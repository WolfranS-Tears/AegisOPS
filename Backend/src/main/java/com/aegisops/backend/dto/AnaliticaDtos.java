package com.aegisops.backend.dto;

import com.aegisops.backend.domain.Enums.Prioridad;
import java.time.LocalDate;

public final class AnaliticaDtos {

    private AnaliticaDtos() {
    }

    /** Filtros comunes del dashboard. Todos son opcionales. */
    public record FiltroAnalitica(LocalDate desde, LocalDate hasta, Long categoriaId, Prioridad prioridad) {

        public FiltroAnalitica {
            if (desde != null && hasta != null && hasta.isBefore(desde)) {
                throw new IllegalArgumentException("La fecha hasta no puede ser anterior a desde");
            }
        }
    }
}
