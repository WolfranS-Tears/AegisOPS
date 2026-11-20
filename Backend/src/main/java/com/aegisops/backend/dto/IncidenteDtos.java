package com.aegisops.backend.dto;

import com.aegisops.backend.domain.Enums.EstadoIncidente;
import com.aegisops.backend.domain.Enums.Prioridad;
import com.aegisops.backend.domain.Incidente;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;

public final class IncidenteDtos {

    private IncidenteDtos() {
    }

    public record IncidenteRequest(
            @NotBlank(message = "El título es obligatorio") @Size(max = 200) String titulo,
            @NotBlank(message = "La descripción es obligatoria") @Size(max = 5000) String descripcion,
            @NotNull(message = "La categoría es obligatoria") Long categoriaId,
            @NotNull(message = "La prioridad es obligatoria") Prioridad prioridad,
            @NotNull(message = "El usuario responsable es obligatorio") Long usuarioId) {
    }

    public record CambioEstadoRequest(@NotNull(message = "El estado es obligatorio") EstadoIncidente estado) {
    }

    public record FiltroIncidente(Long categoriaId, Prioridad prioridad, EstadoIncidente estado,
                                  Long usuarioId, String texto, LocalDate desde, LocalDate hasta) {
    }

    public record IncidenteResponse(
            Long id, String titulo, String descripcion,
            Long categoriaId, String categoria,
            Prioridad prioridad, EstadoIncidente estado,
            LocalDateTime fechaCreacion, LocalDateTime fechaActualizacion, LocalDateTime fechaResolucion,
            Double horasResolucion,
            Long usuarioId, String usuario) {

        public static IncidenteResponse de(Incidente i) {
            Double horas = null;
            if (i.getFechaResolucion() != null) {
                long minutos = Duration.between(i.getFechaCreacion(), i.getFechaResolucion()).toMinutes();
                horas = Math.round(minutos / 60.0 * 100) / 100.0;
            }
            return new IncidenteResponse(i.getId(), i.getTitulo(), i.getDescripcion(),
                    i.getCategoria().getId(), i.getCategoria().getNombre(),
                    i.getPrioridad(), i.getEstado(),
                    i.getFechaCreacion(), i.getFechaActualizacion(), i.getFechaResolucion(), horas,
                    i.getUsuario().getId(), i.getUsuario().getNombre());
        }
    }
}
