package com.aegisops.backend.service;

import com.aegisops.backend.dto.AnaliticaDtos.FiltroAnalitica;
import com.aegisops.backend.repository.AnaliticaRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** RF06: indicadores calculados con SQL real sobre los datos almacenados. */
@Service
@Transactional(readOnly = true)
public class AnaliticaService {

    private final AnaliticaRepository repo;

    public AnaliticaService(AnaliticaRepository repo) {
        this.repo = repo;
    }

    public Map<String, Object> resumen(FiltroAnalitica f) {
        return repo.resumen(f);
    }

    public List<Map<String, Object>> tendenciaSemanal(FiltroAnalitica f) {
        return repo.tendenciaSemanal(f);
    }

    public List<Map<String, Object>> distribucion(String dimension, FiltroAnalitica f) {
        return repo.distribucion(dimension, f);
    }

    public List<Map<String, Object>> tiempos(String dimension, FiltroAnalitica f) {
        return repo.tiemposResolucion(dimension, f);
    }

    public List<Map<String, Object>> categoriasCriticas(FiltroAnalitica f) {
        return repo.categoriasCriticas(f);
    }

    public List<Map<String, Object>> sla(FiltroAnalitica f) {
        return repo.cumplimientoSla(f);
    }

    public List<Map<String, Object>> responsables(FiltroAnalitica f) {
        return repo.cargaResponsables(f);
    }

    public Map<String, Object> asistente(FiltroAnalitica f) {
        return repo.asistente(f);
    }

    /** Todos los indicadores en una sola llamada (lo usa el dashboard). */
    public Map<String, Object> dashboard(FiltroAnalitica f) {
        Map<String, Object> d = new LinkedHashMap<>();
        d.put("filtros", f);
        d.put("resumen", repo.resumen(f));
        d.put("tendenciaSemanal", repo.tendenciaSemanal(f));
        d.put("porCategoria", repo.distribucion("categoria", f));
        d.put("porPrioridad", repo.distribucion("prioridad", f));
        d.put("porEstado", repo.distribucion("estado", f));
        d.put("tiemposPorCategoria", repo.tiemposResolucion("categoria", f));
        d.put("tiemposPorPrioridad", repo.tiemposResolucion("prioridad", f));
        d.put("categoriasCriticas", repo.categoriasCriticas(f));
        d.put("sla", repo.cumplimientoSla(f));
        d.put("responsables", repo.cargaResponsables(f));
        d.put("asistente", repo.asistente(f));
        return d;
    }
}
