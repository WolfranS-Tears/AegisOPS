package com.aegisops.backend.service;

import com.aegisops.backend.dto.AsistenteDtos.N8nFuente;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Lógica pura (sin base de datos) para depurar las fuentes que devuelve n8n:
 * descarta nulos y similitudes bajo el umbral, elimina duplicados (se queda
 * con la mayor similitud), ordena de mayor a menor y limita a topK.
 */
public final class FiltroFuentes {

    private FiltroFuentes() {
    }

    /** @return mapa ordenado articuloId -> similitud */
    public static Map<Long, Double> depurar(List<N8nFuente> fuentes, double umbral, int topK) {
        Map<Long, Double> mejores = new LinkedHashMap<>();
        if (fuentes == null) {
            return mejores;
        }
        fuentes.stream()
                .filter(Objects::nonNull)
                .filter(f -> f.articuloId() != null && f.similitud() != null)
                .filter(f -> f.similitud() >= umbral && f.similitud() <= 1.0)
                .forEach(f -> mejores.merge(f.articuloId(), f.similitud(), Math::max));

        Map<Long, Double> ordenado = new LinkedHashMap<>();
        mejores.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(topK)
                .forEach(e -> ordenado.put(e.getKey(), e.getValue()));
        return ordenado;
    }
}
