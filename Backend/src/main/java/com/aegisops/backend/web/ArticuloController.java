package com.aegisops.backend.web;

import com.aegisops.backend.domain.Enums.EstadoArticulo;
import com.aegisops.backend.dto.ArticuloDtos.ArticuloRequest;
import com.aegisops.backend.dto.ArticuloDtos.ArticuloResponse;
import com.aegisops.backend.dto.ArticuloDtos.CambioEstadoArticuloRequest;
import com.aegisops.backend.service.ArticuloService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/articulos")
@Tag(name = "Base de conocimiento", description = "RF02 - Artículos técnicos y RF03 - Indexación")
public class ArticuloController {

    private final ArticuloService service;

    public ArticuloController(ArticuloService service) {
        this.service = service;
    }

    @GetMapping
    public List<ArticuloResponse> listar(@RequestParam(required = false) EstadoArticulo estado,
                                         @RequestParam(required = false) Long categoriaId,
                                         @RequestParam(required = false) String texto) {
        return service.listar(estado, categoriaId, texto);
    }

    @GetMapping("/{id}")
    public ArticuloResponse obtener(@PathVariable Long id) {
        return service.obtener(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ArticuloResponse crear(@Valid @RequestBody ArticuloRequest req) {
        return service.crear(req);
    }

    @PutMapping("/{id}")
    public ArticuloResponse actualizar(@PathVariable Long id, @Valid @RequestBody ArticuloRequest req) {
        return service.actualizar(id, req);
    }

    @Operation(summary = "Activa, desactiva o verifica un artículo (un artículo inactivo deja de ser fuente)")
    @PatchMapping("/{id}/estado")
    public ArticuloResponse cambiarEstado(@PathVariable Long id,
                                          @Valid @RequestBody CambioEstadoArticuloRequest req) {
        return service.cambiarEstado(id, req.estado(), req.verificado());
    }

    @Operation(summary = "Ejecuta el flujo 1 de n8n: genera embeddings y sincroniza Qdrant")
    @PostMapping("/indexar")
    public Map<String, Object> indexar() {
        return service.reindexarTodo();
    }
}
