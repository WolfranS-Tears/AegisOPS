package com.aegisops.backend.web;

import com.aegisops.backend.dto.ArticuloDtos.ArticuloFuente;
import com.aegisops.backend.dto.ArticuloDtos.ArticuloIndexacion;
import com.aegisops.backend.service.ArticuloService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints que consume n8n (no el frontend). Protegidos con X-Internal-Token
 * cuando INTERNAL_API_TOKEN está configurado.
 */
@RestController
@RequestMapping("/api/internal/articulos")
@Tag(name = "Interno (n8n)", description = "Consumidos por los flujos de n8n")
public class InternalController {

    private final ArticuloService service;

    public InternalController(ArticuloService service) {
        this.service = service;
    }

    @Operation(summary = "Artículos para indexar. indexable=false indica que debe retirarse de Qdrant")
    @GetMapping("/indexacion")
    public List<ArticuloIndexacion> indexacion(@RequestParam(required = false) Long articuloId) {
        return service.paraIndexacion(articuloId);
    }

    @Operation(summary = "Devuelve solo los artículos que existen, están activos y verificados")
    @GetMapping("/fuentes")
    public List<ArticuloFuente> fuentes(@RequestParam(required = false) List<Long> ids) {
        return service.fuentesValidas(ids);
    }
}
