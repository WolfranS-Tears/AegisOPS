package com.aegisops.backend.repository;

import com.aegisops.backend.dto.AnaliticaDtos.FiltroAnalitica;
import com.aegisops.backend.exception.Excepciones;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Consultas analíticas con SQL nativo. Cada indicador del dashboard sale de
 * una de estas consultas, de modo que los valores pueden verificarse
 * ejecutando el mismo SQL (ver database/consultas_analiticas.sql).
 * Los nombres de columna son las claves del JSON que recibe el frontend.
 */
@Repository
public class AnaliticaRepository {

    /** Horas entre creación y resolución. NULL si el incidente sigue abierto. */
    private static final String HORAS = "(EXTRACT(EPOCH FROM (i.fecha_resolucion - i.fecha_creacion)) / 3600.0)";
    private static final String ES_RESUELTO = "i.estado IN ('RESUELTO', 'CERRADO')";
    private static final String ES_ABIERTO = "i.estado IN ('ABIERTO', 'EN_PROGRESO')";

    /** Objetivos de atención (SLA) en horas por prioridad. */
    private static final String SLA_OBJETIVO = """
            CASE i.prioridad WHEN 'CRITICA' THEN 4 WHEN 'ALTA' THEN 8 WHEN 'MEDIA' THEN 24 ELSE 72 END""";

    /** Dimensiones permitidas para agrupar (lista blanca: evita inyección SQL). */
    private static final Map<String, String> DIMENSIONES = Map.of(
            "categoria", "c.nombre",
            "prioridad", "i.prioridad",
            "estado", "i.estado");

    private final NamedParameterJdbcTemplate jdbc;

    public AnaliticaRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Map<String, Object> resumen(FiltroAnalitica f) {
        Where w = where(f);
        String sql = """
                SELECT COUNT(*)                                             AS total,
                       COUNT(*) FILTER (WHERE %2$s)                         AS abiertos,
                       COUNT(*) FILTER (WHERE %3$s)                         AS resueltos,
                       ROUND(100.0 * COUNT(*) FILTER (WHERE %3$s) / NULLIF(COUNT(*), 0), 2) AS porcentaje_resolucion,
                       ROUND(AVG(%1$s)::numeric, 2)                         AS tiempo_promedio_horas,
                       ROUND((PERCENTILE_CONT(0.5) WITHIN GROUP (ORDER BY %1$s))::numeric, 2) AS tiempo_mediana_horas,
                       COUNT(*) FILTER (WHERE i.prioridad = 'CRITICA' AND %2$s) AS criticos_abiertos
                FROM incidente i
                %4$s
                """.formatted(HORAS, ES_ABIERTO, ES_RESUELTO, w.sql);
        return jdbc.queryForMap(sql, w.params);
    }

    /** Incidentes creados y resueltos por semana (semana ISO, inicia lunes). */
    public List<Map<String, Object>> tendenciaSemanal(FiltroAnalitica f) {
        Where w = where(f);
        String sql = """
                WITH base AS (
                    SELECT i.fecha_creacion, i.fecha_resolucion
                    FROM incidente i
                    %s
                ),
                semanas AS (
                    SELECT generate_series(DATE_TRUNC('week', MIN(fecha_creacion)),
                                           DATE_TRUNC('week', MAX(fecha_creacion)),
                                           INTERVAL '1 week') AS semana
                    FROM base
                )
                SELECT TO_CHAR(s.semana, 'YYYY-MM-DD') AS semana,
                       (SELECT COUNT(*) FROM base b WHERE DATE_TRUNC('week', b.fecha_creacion) = s.semana)   AS creados,
                       (SELECT COUNT(*) FROM base b WHERE DATE_TRUNC('week', b.fecha_resolucion) = s.semana) AS resueltos
                FROM semanas s
                ORDER BY s.semana
                """.formatted(w.sql);
        return jdbc.queryForList(sql, w.params);
    }

    public List<Map<String, Object>> distribucion(String dimension, FiltroAnalitica f) {
        String columna = columnaDimension(dimension);
        Where w = where(f);
        String sql = """
                SELECT %1$s AS etiqueta,
                       COUNT(*) AS cantidad,
                       ROUND(100.0 * COUNT(*) / SUM(COUNT(*)) OVER (), 2) AS porcentaje
                FROM incidente i
                JOIN categoria c ON c.id = i.categoria_id
                %2$s
                GROUP BY %1$s
                ORDER BY cantidad DESC, etiqueta
                """.formatted(columna, w.sql);
        return jdbc.queryForList(sql, w.params);
    }

    public List<Map<String, Object>> tiemposResolucion(String dimension, FiltroAnalitica f) {
        String columna = columnaDimension(dimension);
        Where w = where(f);
        String sql = """
                SELECT %1$s AS etiqueta,
                       COUNT(*) FILTER (WHERE %3$s)     AS resueltos,
                       ROUND(AVG(%2$s)::numeric, 2)     AS promedio_horas,
                       ROUND(MIN(%2$s)::numeric, 2)     AS minimo_horas,
                       ROUND(MAX(%2$s)::numeric, 2)     AS maximo_horas
                FROM incidente i
                JOIN categoria c ON c.id = i.categoria_id
                %4$s
                GROUP BY %1$s
                ORDER BY promedio_horas DESC NULLS LAST
                """.formatted(columna, HORAS, ES_RESUELTO, w.sql);
        return jdbc.queryForList(sql, w.params);
    }

    /** Ranking de categorías con mayores problemas (volumen, severidad y backlog). */
    public List<Map<String, Object>> categoriasCriticas(FiltroAnalitica f) {
        Where w = where(f);
        String sql = """
                SELECT c.nombre AS categoria,
                       COUNT(*) AS total,
                       COUNT(*) FILTER (WHERE %2$s) AS abiertos,
                       COUNT(*) FILTER (WHERE i.prioridad IN ('ALTA', 'CRITICA')) AS alta_o_critica,
                       ROUND(100.0 * COUNT(*) FILTER (WHERE i.prioridad IN ('ALTA', 'CRITICA')) / COUNT(*), 2) AS porcentaje_alta_critica,
                       ROUND(100.0 * COUNT(*) FILTER (WHERE %3$s) / COUNT(*), 2) AS porcentaje_resolucion,
                       ROUND(AVG(%1$s)::numeric, 2) AS promedio_horas
                FROM incidente i
                JOIN categoria c ON c.id = i.categoria_id
                %4$s
                GROUP BY c.nombre
                ORDER BY total DESC, alta_o_critica DESC
                """.formatted(HORAS, ES_ABIERTO, ES_RESUELTO, w.sql);
        return jdbc.queryForList(sql, w.params);
    }

    /** Cumplimiento del objetivo de atención por prioridad. */
    public List<Map<String, Object>> cumplimientoSla(FiltroAnalitica f) {
        Where w = where(f);
        String sql = """
                SELECT i.prioridad AS prioridad,
                       MIN(%2$s) AS objetivo_horas,
                       COUNT(*) FILTER (WHERE %3$s) AS resueltos,
                       COUNT(*) FILTER (WHERE %3$s AND %1$s <= %2$s) AS dentro_objetivo,
                       ROUND(100.0 * COUNT(*) FILTER (WHERE %3$s AND %1$s <= %2$s)
                             / NULLIF(COUNT(*) FILTER (WHERE %3$s), 0), 2) AS porcentaje_cumplimiento
                FROM incidente i
                %4$s
                GROUP BY i.prioridad
                ORDER BY objetivo_horas
                """.formatted(HORAS, SLA_OBJETIVO, ES_RESUELTO, w.sql);
        return jdbc.queryForList(sql, w.params);
    }

    public List<Map<String, Object>> cargaResponsables(FiltroAnalitica f) {
        Where w = where(f);
        String sql = """
                SELECT u.nombre AS responsable,
                       u.area   AS area,
                       COUNT(*) AS asignados,
                       COUNT(*) FILTER (WHERE %2$s) AS abiertos,
                       COUNT(*) FILTER (WHERE %3$s) AS resueltos,
                       ROUND(AVG(%1$s)::numeric, 2) AS promedio_horas
                FROM incidente i
                JOIN usuario u ON u.id = i.usuario_id
                %4$s
                GROUP BY u.nombre, u.area
                ORDER BY asignados DESC, responsable
                """.formatted(HORAS, ES_ABIERTO, ES_RESUELTO, w.sql);
        return jdbc.queryForList(sql, w.params);
    }

    /** Indicadores del asistente RAG (consultas, estados, calificaciones, artículos más usados). */
    public Map<String, Object> asistente(FiltroAnalitica f) {
        MapSqlParameterSource p = new MapSqlParameterSource();
        StringBuilder w = new StringBuilder(" WHERE 1 = 1");
        if (f.desde() != null) {
            w.append(" AND q.fecha >= :desde");
            p.addValue("desde", Timestamp.valueOf(f.desde().atStartOfDay()));
        }
        if (f.hasta() != null) {
            w.append(" AND q.fecha < :hasta");
            p.addValue("hasta", Timestamp.valueOf(f.hasta().plusDays(1).atStartOfDay()));
        }
        String filtro = w.toString();

        List<Map<String, Object>> porEstado = jdbc.queryForList("""
                SELECT q.estado AS estado, COUNT(*) AS cantidad
                FROM consulta_ia q %s
                GROUP BY q.estado ORDER BY cantidad DESC
                """.formatted(filtro), p);

        Map<String, Object> calificaciones = jdbc.queryForMap("""
                SELECT COUNT(k.id) AS calificadas,
                       ROUND(AVG(k.puntuacion)::numeric, 2) AS promedio
                FROM consulta_ia q
                JOIN respuesta_ia r ON r.consulta_id = q.id
                JOIN calificacion k ON k.respuesta_id = r.id
                %s
                """.formatted(filtro), p);

        List<Map<String, Object>> articulosMasUsados = jdbc.queryForList("""
                SELECT a.id AS articulo_id, a.titulo AS titulo,
                       COUNT(*) AS usos, ROUND(AVG(fr.similitud)::numeric, 4) AS similitud_promedio
                FROM consulta_ia q
                JOIN respuesta_ia r ON r.consulta_id = q.id
                JOIN fuente_respuesta fr ON fr.respuesta_id = r.id
                JOIN articulo_conocimiento a ON a.id = fr.articulo_id
                %s
                GROUP BY a.id, a.titulo
                ORDER BY usos DESC, a.id
                LIMIT 5
                """.formatted(filtro), p);

        return Map.of(
                "consultasPorEstado", porEstado,
                "calificaciones", calificaciones,
                "articulosMasUsados", articulosMasUsados);
    }

    private static String columnaDimension(String dimension) {
        String columna = DIMENSIONES.get(dimension == null ? "" : dimension.toLowerCase());
        if (columna == null) {
            throw new Excepciones.ReglaNegocio("Dimensión no válida: " + dimension
                    + ". Valores permitidos: " + DIMENSIONES.keySet());
        }
        return columna;
    }

    private static Where where(FiltroAnalitica f) {
        MapSqlParameterSource p = new MapSqlParameterSource();
        StringBuilder sql = new StringBuilder("WHERE 1 = 1");
        if (f.desde() != null) {
            sql.append(" AND i.fecha_creacion >= :desde");
            p.addValue("desde", Timestamp.valueOf(f.desde().atStartOfDay()));
        }
        if (f.hasta() != null) {
            sql.append(" AND i.fecha_creacion < :hasta");
            p.addValue("hasta", Timestamp.valueOf(f.hasta().plusDays(1).atStartOfDay()));
        }
        if (f.categoriaId() != null) {
            sql.append(" AND i.categoria_id = :categoriaId");
            p.addValue("categoriaId", f.categoriaId());
        }
        if (f.prioridad() != null) {
            sql.append(" AND i.prioridad = :prioridad");
            p.addValue("prioridad", f.prioridad().name());
        }
        return new Where(sql.toString(), p);
    }

    private record Where(String sql, MapSqlParameterSource params) {
    }
}
