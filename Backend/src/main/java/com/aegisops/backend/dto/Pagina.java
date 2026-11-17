package com.aegisops.backend.dto;

import java.util.List;
import java.util.function.Function;
import org.springframework.data.domain.Page;

/** Página estable para el JSON de la API (evita serializar PageImpl). */
public record Pagina<T>(List<T> contenido, int pagina, int tamano, long totalElementos, int totalPaginas) {

    public static <E, T> Pagina<T> de(Page<E> page, Function<E, T> mapper) {
        return new Pagina<>(page.getContent().stream().map(mapper).toList(),
                page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }
}
