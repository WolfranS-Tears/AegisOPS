package com.aegisops.backend.dto;

import com.aegisops.backend.domain.Enums.EstadoConsulta;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public final class AsistenteDtos {

    private AsistenteDtos() {
    }

    public record ConsultaRequest(
            Long incidenteId,
            @NotBlank(message = "La descripción del problema es obligatoria")
            @Size(min = 10, max = 2000, message = "La descripción debe tener entre 10 y 2000 caracteres")
            String pregunta) {
    }

    public record CalificacionRequest(
            @NotNull(message = "La puntuación es obligatoria")
            @Min(value = 1, message = "La puntuación mínima es 1")
            @Max(value = 5, message = "La puntuación máxima es 5") Integer puntuacion,
            @Size(max = 500) String comentario) {
    }

    public record FuenteDto(Long articuloId, String titulo, String categoria, BigDecimal similitud) {
    }

    public record CalificacionDto(Long id, Short puntuacion, String comentario, LocalDateTime fecha) {
    }

    public record RespuestaDto(Long id, String respuesta, List<String> pasos, String modelo,
                               LocalDateTime fecha, List<FuenteDto> fuentes, CalificacionDto calificacion) {
    }

    public record ConsultaResponse(Long id, Long incidenteId, String incidente, String pregunta,
                                   LocalDateTime fecha, EstadoConsulta estado, BigDecimal umbral,
                                   String mensajeError, RespuestaDto respuesta) {
    }

    // ---- Contrato con el flujo RAG de n8n -----------------------------------

    /** Petición que el backend envía al webhook RAG de n8n. */
    public record N8nRagRequest(Long consultaId, String pregunta, double umbral, int topK) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record N8nFuente(Long articuloId, String titulo, Double similitud) {
    }

    /** Respuesta del webhook RAG: estado RESPONDIDA, SIN_RESULTADOS o ERROR. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record N8nRagResponse(String estado, String solucion, List<String> pasos,
                                 List<N8nFuente> fuentes, String modelo, String error) {
    }
}
