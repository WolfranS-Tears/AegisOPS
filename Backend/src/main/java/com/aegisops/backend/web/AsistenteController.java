package com.aegisops.backend.web;

import com.aegisops.backend.dto.AsistenteDtos.CalificacionDto;
import com.aegisops.backend.dto.AsistenteDtos.CalificacionRequest;
import com.aegisops.backend.dto.AsistenteDtos.ConsultaRequest;
import com.aegisops.backend.dto.AsistenteDtos.ConsultaResponse;
import com.aegisops.backend.service.AsistenteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/asistente")
@Tag(name = "Asistente RAG", description = "RF04/RF05 búsqueda semántica y RAG, RF08 historial y calificación")
public class AsistenteController {

    private final AsistenteService service;

    public AsistenteController(AsistenteService service) {
        this.service = service;
    }

    @Operation(summary = "Ejecuta una consulta inteligente. El estado final es RESPONDIDA, SIN_RESULTADOS o ERROR")
    @PostMapping("/consultas")
    @ResponseStatus(HttpStatus.CREATED)
    public ConsultaResponse consultar(@Valid @RequestBody ConsultaRequest req) {
        return service.consultar(req);
    }

    @Operation(summary = "Historial de consultas (opcionalmente de un incidente)")
    @GetMapping("/consultas")
    public List<ConsultaResponse> historial(@RequestParam(required = false) Long incidenteId) {
        return service.historial(incidenteId);
    }

    @GetMapping("/consultas/{id}")
    public ConsultaResponse obtener(@PathVariable Long id) {
        return service.obtener(id);
    }

    @Operation(summary = "Califica la utilidad de una respuesta (1 a 5). Repetir la llamada actualiza la calificación")
    @PutMapping("/respuestas/{respuestaId}/calificacion")
    public CalificacionDto calificar(@PathVariable Long respuestaId, @Valid @RequestBody CalificacionRequest req) {
        return service.calificar(respuestaId, req);
    }
}
