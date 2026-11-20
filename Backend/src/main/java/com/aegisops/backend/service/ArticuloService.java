package com.aegisops.backend.service;

import com.aegisops.backend.domain.ArticuloConocimiento;
import com.aegisops.backend.domain.Categoria;
import com.aegisops.backend.domain.Enums.EstadoArticulo;
import com.aegisops.backend.dto.ArticuloDtos.ArticuloFuente;
import com.aegisops.backend.dto.ArticuloDtos.ArticuloIndexacion;
import com.aegisops.backend.dto.ArticuloDtos.ArticuloRequest;
import com.aegisops.backend.dto.ArticuloDtos.ArticuloResponse;
import com.aegisops.backend.exception.Excepciones;
import com.aegisops.backend.repository.ArticuloRepository;
import com.aegisops.backend.repository.CategoriaRepository;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
@Transactional(readOnly = true)
public class ArticuloService {

    private static final Logger log = LoggerFactory.getLogger(ArticuloService.class);

    private final ArticuloRepository articulos;
    private final CategoriaRepository categorias;
    private final N8nClient n8n;

    public ArticuloService(ArticuloRepository articulos, CategoriaRepository categorias, N8nClient n8n) {
        this.articulos = articulos;
        this.categorias = categorias;
        this.n8n = n8n;
    }

    public List<ArticuloResponse> listar(EstadoArticulo estado, Long categoriaId, String texto) {
        String t = texto == null || texto.isBlank() ? null : texto.trim();
        return articulos.buscar(estado, categoriaId, t).stream().map(ArticuloResponse::de).toList();
    }

    public ArticuloResponse obtener(Long id) {
        return ArticuloResponse.de(buscar(id));
    }

    @Transactional
    public ArticuloResponse crear(ArticuloRequest req) {
        ArticuloConocimiento a = new ArticuloConocimiento();
        aplicar(a, req);
        ArticuloConocimiento guardado = articulos.save(a);
        sincronizarIndiceDespuesDeCommit(guardado.getId());
        return ArticuloResponse.de(guardado);
    }

    @Transactional
    public ArticuloResponse actualizar(Long id, ArticuloRequest req) {
        ArticuloConocimiento a = buscar(id);
        aplicar(a, req);
        ArticuloConocimiento guardado = articulos.saveAndFlush(a);
        sincronizarIndiceDespuesDeCommit(id);
        return ArticuloResponse.de(guardado);
    }

    /**
     * Activa, desactiva o verifica un artículo. Al desactivarlo deja de ser
     * fuente válida de inmediato (el RAG valida contra esta tabla) y además se
     * pide a n8n que lo retire de Qdrant.
     */
    @Transactional
    public ArticuloResponse cambiarEstado(Long id, EstadoArticulo estado, Boolean verificado) {
        ArticuloConocimiento a = buscar(id);
        a.setEstado(estado);
        if (verificado != null) {
            a.setVerificado(verificado);
        }
        ArticuloConocimiento guardado = articulos.saveAndFlush(a);
        sincronizarIndiceDespuesDeCommit(id);
        return ArticuloResponse.de(guardado);
    }

    /** Ejecuta el flujo 1 de n8n para toda la base de conocimiento. */
    public Map<String, Object> reindexarTodo() {
        return n8n.indexar(null);
    }

    // ---- Endpoints internos que consume n8n --------------------------------

    public List<ArticuloIndexacion> paraIndexacion(Long articuloId) {
        if (articuloId != null) {
            return List.of(ArticuloIndexacion.de(buscar(articuloId)));
        }
        return articulos.findAllByOrderByIdAsc().stream().map(ArticuloIndexacion::de).toList();
    }

    public List<ArticuloFuente> fuentesValidas(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return articulos.findFuentesValidas(ids).stream().map(ArticuloFuente::de).toList();
    }

    private ArticuloConocimiento buscar(Long id) {
        return articulos.findWithCategoriaById(id)
                .orElseThrow(() -> new Excepciones.NoEncontrado("Artículo", id));
    }

    private void aplicar(ArticuloConocimiento a, ArticuloRequest req) {
        Categoria categoria = categorias.findById(req.categoriaId())
                .orElseThrow(() -> new Excepciones.NoEncontrado("Categoría", req.categoriaId()));
        a.setTitulo(req.titulo().trim());
        a.setProblema(req.problema().trim());
        a.setSintomas(req.sintomas().trim());
        a.setCausa(req.causa().trim());
        a.setSolucion(req.solucion().trim());
        a.setCategoria(categoria);
        if (req.estado() != null) {
            a.setEstado(req.estado());
        }
        if (req.verificado() != null) {
            a.setVerificado(req.verificado());
        }
    }

    /** Pide a n8n reindexar el artículo cuando la transacción ya se confirmó (mejor esfuerzo). */
    private void sincronizarIndiceDespuesDeCommit(Long articuloId) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                CompletableFuture.runAsync(() -> {
                    try {
                        n8n.indexar(articuloId);
                        log.info("Artículo {} sincronizado con el índice vectorial", articuloId);
                    } catch (RuntimeException e) {
                        log.warn("No se pudo sincronizar el artículo {} con Qdrant: {}. "
                                + "El RAG igual lo excluye si no está activo.", articuloId, e.getMessage());
                    }
                });
            }
        });
    }
}
