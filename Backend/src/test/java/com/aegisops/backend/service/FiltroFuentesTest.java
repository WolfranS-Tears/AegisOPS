package com.aegisops.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.aegisops.backend.dto.AsistenteDtos.N8nFuente;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class FiltroFuentesTest {

    private static N8nFuente f(Long id, Double sim) {
        return new N8nFuente(id, "Artículo " + id, sim);
    }

    @Test
    void descartaFuentesBajoElUmbral() {
        Map<Long, Double> r = FiltroFuentes.depurar(List.of(f(1L, 0.81), f(2L, 0.49), f(3L, 0.50)), 0.50, 5);
        assertEquals(List.of(1L, 3L), new ArrayList<>(r.keySet()));
    }

    @Test
    void eliminaDuplicadosConservandoLaMayorSimilitud() {
        Map<Long, Double> r = FiltroFuentes.depurar(List.of(f(7L, 0.60), f(7L, 0.72)), 0.5, 5);
        assertEquals(1, r.size());
        assertEquals(0.72, r.get(7L));
    }

    @Test
    void ordenaDeMayorAMenorYLimitaATopK() {
        Map<Long, Double> r = FiltroFuentes.depurar(List.of(
                f(1L, 0.55), f(2L, 0.91), f(3L, 0.77), f(4L, 0.66), f(5L, 0.88), f(6L, 0.70)), 0.5, 3);
        assertEquals(List.of(2L, 5L, 3L), new ArrayList<>(r.keySet()));
    }

    @Test
    void ignoraNulosYValoresInvalidos() {
        Map<Long, Double> r = FiltroFuentes.depurar(Arrays.asList(null, f(null, 0.9), f(2L, null), f(3L, 1.5)), 0.5, 5);
        assertTrue(r.isEmpty());
    }

    @Test
    void listaNulaDevuelveVacio() {
        assertTrue(FiltroFuentes.depurar(null, 0.5, 5).isEmpty());
    }
}
