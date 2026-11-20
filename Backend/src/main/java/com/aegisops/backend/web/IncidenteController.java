package com.aegisops.backend.web;

import com.aegisops.backend.domain.Enums.EstadoIncidente;
import com.aegisops.backend.domain.Enums.Prioridad;
import com.aegisops.backend.dto.IncidenteDtos.CambioEstadoRequest;
import com.aegisops.backend.dto.IncidenteDtos.FiltroIncidente;
import com.aegisops.backend.dto.IncidenteDtos.IncidenteRequest;
import com.aegisops.backend.dto.IncidenteDtos.IncidenteResponse;
import com.aegisops.backend.dto.Pagina;
import com.aegisops.backend.service.IncidenteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
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
@RequestMapping("/api/incidentes")
@Tag(name = "Incidentes", description = "RF01 - Gestión de incidentes")
public class IncidenteController {

    private final IncidenteService service;

    public IncidenteController(IncidenteService service) {
        this.service = service;
    }

    @Operation(summary = "Lista incidentes con filtros combinables y paginación")
    @GetMapping
    public Pagina<IncidenteResponse> listar(
            @RequestParam(required = false) Long categoriaId,
            @RequestParam(required = false) Prioridad prioridad,
            @RequestParam(required = false) EstadoIncidente estado,
            @RequestParam(required = false) Long usuarioId,
            @RequestParam(required = false) String texto,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamano) {
        FiltroIncidente filtro = new FiltroIncidente(categoriaId, prioridad, estado, usuarioId, texto, desde, hasta);
        PageRequest page = PageRequest.of(Math.max(pagina, 0), Math.clamp(tamano, 1, 200),
                Sort.by(Sort.Direction.DESC, "fechaCreacion").and(Sort.by(Sort.Direction.DESC, "id")));
        return service.listar(filtro, page);
    }

    @GetMapping("/{id}")
    public IncidenteResponse obtener(@PathVariable Long id) {
        return service.obtener(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public IncidenteResponse crear(@Valid @RequestBody IncidenteRequest req) {
        return service.crear(req);
    }

    @PutMapping("/{id}")
    public IncidenteResponse actualizar(@PathVariable Long id, @Valid @RequestBody IncidenteRequest req) {
        return service.actualizar(id, req);
    }

    @Operation(summary = "Cambia el estado. RESUELTO/CERRADO registra la fecha de resolución")
    @PatchMapping("/{id}/estado")
    public IncidenteResponse cambiarEstado(@PathVariable Long id, @Valid @RequestBody CambioEstadoRequest req) {
        return service.cambiarEstado(id, req.estado());
    }
}
