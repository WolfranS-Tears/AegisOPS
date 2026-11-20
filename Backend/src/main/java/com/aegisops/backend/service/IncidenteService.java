package com.aegisops.backend.service;

import com.aegisops.backend.domain.Categoria;
import com.aegisops.backend.domain.Enums.EstadoIncidente;
import com.aegisops.backend.domain.Incidente;
import com.aegisops.backend.domain.Usuario;
import com.aegisops.backend.dto.IncidenteDtos.FiltroIncidente;
import com.aegisops.backend.dto.IncidenteDtos.IncidenteRequest;
import com.aegisops.backend.dto.IncidenteDtos.IncidenteResponse;
import com.aegisops.backend.dto.Pagina;
import com.aegisops.backend.exception.Excepciones;
import com.aegisops.backend.repository.CategoriaRepository;
import com.aegisops.backend.repository.IncidenteRepository;
import com.aegisops.backend.repository.UsuarioRepository;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class IncidenteService {

    private final IncidenteRepository incidentes;
    private final CategoriaRepository categorias;
    private final UsuarioRepository usuarios;

    public IncidenteService(IncidenteRepository incidentes, CategoriaRepository categorias,
                            UsuarioRepository usuarios) {
        this.incidentes = incidentes;
        this.categorias = categorias;
        this.usuarios = usuarios;
    }

    public Pagina<IncidenteResponse> listar(FiltroIncidente filtro, Pageable pageable) {
        return Pagina.de(incidentes.findAll(especificacion(filtro), pageable), IncidenteResponse::de);
    }

    public IncidenteResponse obtener(Long id) {
        return IncidenteResponse.de(buscar(id));
    }

    @Transactional
    public IncidenteResponse crear(IncidenteRequest req) {
        Incidente i = new Incidente();
        aplicar(i, req);
        return IncidenteResponse.de(incidentes.save(i));
    }

    @Transactional
    public IncidenteResponse actualizar(Long id, IncidenteRequest req) {
        Incidente i = buscar(id);
        aplicar(i, req);
        return IncidenteResponse.de(incidentes.saveAndFlush(i));
    }

    @Transactional
    public IncidenteResponse cambiarEstado(Long id, EstadoIncidente estado) {
        Incidente i = buscar(id);
        i.cambiarEstado(estado);
        return IncidenteResponse.de(incidentes.saveAndFlush(i));
    }

    private Incidente buscar(Long id) {
        return incidentes.findWithRelacionesById(id)
                .orElseThrow(() -> new Excepciones.NoEncontrado("Incidente", id));
    }

    private void aplicar(Incidente i, IncidenteRequest req) {
        Categoria categoria = categorias.findById(req.categoriaId())
                .orElseThrow(() -> new Excepciones.NoEncontrado("Categoría", req.categoriaId()));
        Usuario usuario = usuarios.findById(req.usuarioId())
                .orElseThrow(() -> new Excepciones.NoEncontrado("Usuario", req.usuarioId()));
        i.setTitulo(req.titulo().trim());
        i.setDescripcion(req.descripcion().trim());
        i.setCategoria(categoria);
        i.setPrioridad(req.prioridad());
        i.setUsuario(usuario);
    }

    /** Filtros combinables de RF01 (categoría, prioridad, estado, responsable, texto y fechas). */
    private static Specification<Incidente> especificacion(FiltroIncidente f) {
        return (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            if (f.categoriaId() != null) {
                p.add(cb.equal(root.get("categoria").get("id"), f.categoriaId()));
            }
            if (f.prioridad() != null) {
                p.add(cb.equal(root.get("prioridad"), f.prioridad()));
            }
            if (f.estado() != null) {
                p.add(cb.equal(root.get("estado"), f.estado()));
            }
            if (f.usuarioId() != null) {
                p.add(cb.equal(root.get("usuario").get("id"), f.usuarioId()));
            }
            if (f.texto() != null && !f.texto().isBlank()) {
                String patron = "%" + f.texto().trim().toLowerCase() + "%";
                p.add(cb.or(cb.like(cb.lower(root.get("titulo")), patron),
                        cb.like(cb.lower(root.get("descripcion")), patron)));
            }
            if (f.desde() != null) {
                p.add(cb.greaterThanOrEqualTo(root.get("fechaCreacion"), f.desde().atStartOfDay()));
            }
            if (f.hasta() != null) {
                p.add(cb.lessThan(root.get("fechaCreacion"), f.hasta().plusDays(1).atStartOfDay()));
            }
            return cb.and(p.toArray(new Predicate[0]));
        };
    }
}
