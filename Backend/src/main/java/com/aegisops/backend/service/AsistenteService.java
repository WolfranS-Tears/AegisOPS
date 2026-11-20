package com.aegisops.backend.service;

import com.aegisops.backend.config.AegisProperties;
import com.aegisops.backend.domain.ArticuloConocimiento;
import com.aegisops.backend.domain.Calificacion;
import com.aegisops.backend.domain.ConsultaIA;
import com.aegisops.backend.domain.Enums.EstadoConsulta;
import com.aegisops.backend.domain.FuenteRespuesta;
import com.aegisops.backend.domain.Incidente;
import com.aegisops.backend.domain.RespuestaIA;
import com.aegisops.backend.dto.AsistenteDtos.CalificacionDto;
import com.aegisops.backend.dto.AsistenteDtos.CalificacionRequest;
import com.aegisops.backend.dto.AsistenteDtos.ConsultaRequest;
import com.aegisops.backend.dto.AsistenteDtos.ConsultaResponse;
import com.aegisops.backend.dto.AsistenteDtos.FuenteDto;
import com.aegisops.backend.dto.AsistenteDtos.N8nRagRequest;
import com.aegisops.backend.dto.AsistenteDtos.N8nRagResponse;
import com.aegisops.backend.dto.AsistenteDtos.RespuestaDto;
import com.aegisops.backend.exception.Excepciones;
import com.aegisops.backend.repository.ArticuloRepository;
import com.aegisops.backend.repository.CalificacionRepository;
import com.aegisops.backend.repository.ConsultaRepository;
import com.aegisops.backend.repository.FuenteRepository;
import com.aegisops.backend.repository.IncidenteRepository;
import com.aegisops.backend.repository.RespuestaRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Orquesta el asistente RAG (RF04, RF05, RF08).
 *
 * Flujo: validar entrada -> guardar consulta PENDIENTE -> llamar a n8n ->
 * validar fuentes contra la base relacional -> guardar respuesta y estado.
 * La consulta queda registrada aunque n8n falle (estado ERROR).
 */
@Service
public class AsistenteService {

    private static final Logger log = LoggerFactory.getLogger(AsistenteService.class);

    static final String MENSAJE_SIN_INFORMACION = "No se encontró información suficientemente relevante en la "
            + "base de conocimiento para este problema. Para no generar una solución sin respaldo, el asistente "
            + "no propone pasos. Se recomienda escalar el incidente a un especialista y, una vez resuelto, "
            + "documentar la solución como un nuevo artículo.";

    private final ConsultaRepository consultas;
    private final RespuestaRepository respuestas;
    private final FuenteRepository fuentes;
    private final CalificacionRepository calificaciones;
    private final IncidenteRepository incidentes;
    private final ArticuloRepository articulos;
    private final N8nClient n8n;
    private final AegisProperties.Rag rag;
    private final TransactionTemplate tx;
    private final TransactionTemplate txLectura;

    public AsistenteService(ConsultaRepository consultas, RespuestaRepository respuestas, FuenteRepository fuentes,
                            CalificacionRepository calificaciones, IncidenteRepository incidentes,
                            ArticuloRepository articulos, N8nClient n8n, AegisProperties props,
                            PlatformTransactionManager txManager) {
        this.consultas = consultas;
        this.respuestas = respuestas;
        this.fuentes = fuentes;
        this.calificaciones = calificaciones;
        this.incidentes = incidentes;
        this.articulos = articulos;
        this.n8n = n8n;
        this.rag = props.rag();
        this.tx = new TransactionTemplate(txManager);
        this.txLectura = new TransactionTemplate(txManager);
        this.txLectura.setReadOnly(true);
    }

    public ConsultaResponse consultar(ConsultaRequest req) {
        String pregunta = req.pregunta() == null ? "" : req.pregunta().trim();
        if (pregunta.isEmpty()) {
            // Prueba 7: una descripción vacía nunca ejecuta el flujo inteligente
            throw new Excepciones.ReglaNegocio("La descripción del problema es obligatoria");
        }
        BigDecimal umbral = BigDecimal.valueOf(rag.similarityThreshold()).setScale(3, RoundingMode.HALF_UP);

        Long consultaId = tx.execute(s -> {
            ConsultaIA c = new ConsultaIA();
            c.setPregunta(pregunta);
            c.setUmbral(umbral);
            if (req.incidenteId() != null) {
                Incidente inc = incidentes.findById(req.incidenteId())
                        .orElseThrow(() -> new Excepciones.NoEncontrado("Incidente", req.incidenteId()));
                c.setIncidente(inc);
            }
            return consultas.save(c).getId();
        });

        try {
            N8nRagResponse r = n8n.consultarRag(new N8nRagRequest(consultaId, pregunta,
                    umbral.doubleValue(), rag.topK()));
            tx.executeWithoutResult(s -> procesar(consultaId, r, umbral.doubleValue()));
        } catch (Excepciones.Integracion e) {
            log.warn("Consulta {} terminó en ERROR: {}", consultaId, e.getMessage());
            tx.executeWithoutResult(s -> buscar(consultaId).marcarError(e.getMessage()));
        }
        return obtener(consultaId);
    }

    public ConsultaResponse obtener(Long id) {
        return txLectura.execute(s -> aDto(buscar(id)));
    }

    public List<ConsultaResponse> historial(Long incidenteId) {
        return txLectura.execute(s -> consultas.historial(incidenteId).stream().map(this::aDto).toList());
    }

    public CalificacionDto calificar(Long respuestaId, CalificacionRequest req) {
        return tx.execute(s -> {
            RespuestaIA respuesta = respuestas.findById(respuestaId)
                    .orElseThrow(() -> new Excepciones.NoEncontrado("Respuesta", respuestaId));
            Calificacion c = calificaciones.findByRespuestaId(respuestaId).orElseGet(Calificacion::new);
            c.setRespuesta(respuesta);
            c.setPuntuacion(req.puntuacion().shortValue());
            c.setComentario(req.comentario() == null || req.comentario().isBlank() ? null : req.comentario().trim());
            Calificacion g = calificaciones.saveAndFlush(c);
            return new CalificacionDto(g.getId(), g.getPuntuacion(), g.getComentario(), g.getFecha());
        });
    }

    // -------------------------------------------------------------------------

    private void procesar(Long consultaId, N8nRagResponse r, double umbral) {
        ConsultaIA consulta = buscar(consultaId);
        String estado = r.estado().trim().toUpperCase();

        switch (estado) {
            case "RESPONDIDA" -> {
                Map<Long, Double> candidatas = FiltroFuentes.depurar(r.fuentes(), umbral, rag.topK());
                // Regla 11: las fuentes deben existir en la base relacional y estar activas/verificadas
                Map<Long, ArticuloConocimiento> validas = candidatas.isEmpty() ? Map.of()
                        : articulos.findFuentesValidas(candidatas.keySet()).stream()
                        .collect(Collectors.toMap(ArticuloConocimiento::getId, Function.identity()));

                if (validas.isEmpty()) {
                    log.info("Consulta {}: n8n respondió, pero ninguna fuente superó la validación", consultaId);
                    sinResultados(consulta);
                    return;
                }
                if (r.solucion() == null || r.solucion().isBlank()) {
                    consulta.marcarError("El flujo RAG respondió sin texto de solución");
                    return;
                }
                RespuestaIA respuesta = new RespuestaIA();
                respuesta.setConsulta(consulta);
                respuesta.setRespuesta(r.solucion().trim());
                respuesta.setPasos(r.pasos() == null ? null : String.join("\n", r.pasos()));
                respuesta.setModelo(r.modelo());
                candidatas.forEach((id, sim) -> {
                    ArticuloConocimiento a = validas.get(id);
                    if (a != null) {
                        respuesta.agregarFuente(new FuenteRespuesta(a,
                                BigDecimal.valueOf(sim).setScale(4, RoundingMode.HALF_UP)));
                    }
                });
                respuestas.save(respuesta);
                consulta.setEstado(EstadoConsulta.RESPONDIDA);
            }
            case "SIN_RESULTADOS" -> sinResultados(consulta);
            default -> consulta.marcarError(r.error() != null ? r.error() : "Estado desconocido: " + estado);
        }
    }

    private void sinResultados(ConsultaIA consulta) {
        RespuestaIA respuesta = new RespuestaIA();
        respuesta.setConsulta(consulta);
        respuesta.setRespuesta(MENSAJE_SIN_INFORMACION);
        respuestas.save(respuesta);
        consulta.setEstado(EstadoConsulta.SIN_RESULTADOS);
    }

    private ConsultaIA buscar(Long id) {
        return consultas.findDetalleById(id).orElseThrow(() -> new Excepciones.NoEncontrado("Consulta", id));
    }

    private ConsultaResponse aDto(ConsultaIA c) {
        RespuestaDto respuestaDto = null;
        RespuestaIA r = c.getRespuesta();
        if (r != null) {
            List<FuenteDto> fs = fuentes.findByRespuestaIdOrderBySimilitudDesc(r.getId()).stream()
                    .map(f -> new FuenteDto(f.getArticulo().getId(), f.getArticulo().getTitulo(),
                            f.getArticulo().getCategoria().getNombre(), f.getSimilitud()))
                    .toList();
            List<String> pasos = r.getPasos() == null || r.getPasos().isBlank() ? List.of()
                    : Arrays.stream(r.getPasos().split("\n")).filter(p -> !p.isBlank()).toList();
            Calificacion k = r.getCalificacion();
            CalificacionDto kDto = k == null ? null
                    : new CalificacionDto(k.getId(), k.getPuntuacion(), k.getComentario(), k.getFecha());
            respuestaDto = new RespuestaDto(r.getId(), r.getRespuesta(), pasos, r.getModelo(), r.getFecha(), fs, kDto);
        }
        Incidente inc = c.getIncidente();
        return new ConsultaResponse(c.getId(), inc == null ? null : inc.getId(), inc == null ? null : inc.getTitulo(),
                c.getPregunta(), c.getFecha(), c.getEstado(), c.getUmbral(), c.getMensajeError(), respuestaDto);
    }
}
