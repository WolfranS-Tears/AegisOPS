package com.aegisops.backend.web;

import com.aegisops.backend.domain.Enums.Prioridad;
import com.aegisops.backend.dto.AnaliticaDtos.FiltroAnalitica;
import com.aegisops.backend.service.AnaliticaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * RF06/RF07. Todos los endpoints aceptan los filtros opcionales
 * desde, hasta (yyyy-MM-dd), categoriaId y prioridad.
 */
@RestController
@RequestMapping("/api/analitica")
@Tag(name = "Analítica", description = "RF06/RF07 - Indicadores calculados con SQL sobre datos reales")
public class AnaliticaController {

    private final AnaliticaService service;

    public AnaliticaController(AnaliticaService service) {
        this.service = service;
    }

    @ModelAttribute
    FiltroAnalitica filtro(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) Long categoriaId,
            @RequestParam(required = false) Prioridad prioridad) {
        return new FiltroAnalitica(desde, hasta, categoriaId, prioridad);
    }

    @Operation(summary = "Todos los indicadores del dashboard en una sola respuesta")
    @GetMapping("/dashboard")
    public Map<String, Object> dashboard(@ModelAttribute FiltroAnalitica f) {
        return service.dashboard(f);
    }

    @Operation(summary = "Totales, % de resolución, tiempo promedio y mediana de resolución")
    @GetMapping("/resumen")
    public Map<String, Object> resumen(@ModelAttribute FiltroAnalitica f) {
        return service.resumen(f);
    }

    @Operation(summary = "Incidentes creados y resueltos por semana")
    @GetMapping("/tendencia-semanal")
    public List<Map<String, Object>> tendencia(@ModelAttribute FiltroAnalitica f) {
        return service.tendenciaSemanal(f);
    }

    @Operation(summary = "Distribución por categoria, prioridad o estado (cantidad y porcentaje)")
    @GetMapping("/distribucion")
    public List<Map<String, Object>> distribucion(@RequestParam String dimension, @ModelAttribute FiltroAnalitica f) {
        return service.distribucion(dimension, f);
    }

    @Operation(summary = "Tiempo de resolución (promedio, mínimo, máximo) por categoria o prioridad")
    @GetMapping("/tiempos-resolucion")
    public List<Map<String, Object>> tiempos(@RequestParam(defaultValue = "categoria") String dimension,
                                             @ModelAttribute FiltroAnalitica f) {
        return service.tiempos(dimension, f);
    }

    @Operation(summary = "Ranking de categorías con mayores problemas")
    @GetMapping("/categorias-criticas")
    public List<Map<String, Object>> categoriasCriticas(@ModelAttribute FiltroAnalitica f) {
        return service.categoriasCriticas(f);
    }

    @Operation(summary = "Cumplimiento de objetivos de atención (SLA) por prioridad")
    @GetMapping("/sla")
    public List<Map<String, Object>> sla(@ModelAttribute FiltroAnalitica f) {
        return service.sla(f);
    }

    @Operation(summary = "Carga de trabajo y desempeño por responsable")
    @GetMapping("/responsables")
    public List<Map<String, Object>> responsables(@ModelAttribute FiltroAnalitica f) {
        return service.responsables(f);
    }

    @Operation(summary = "Indicadores del asistente RAG: estados, calificación promedio y artículos más usados")
    @GetMapping("/asistente")
    public Map<String, Object> asistente(@ModelAttribute FiltroAnalitica f) {
        return service.asistente(f);
    }
}
