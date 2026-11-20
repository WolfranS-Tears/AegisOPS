-- =====================================================================
-- AegisOPS - Consultas analíticas (RF06)
-- Son las mismas consultas que ejecuta AnaliticaRepository.java, sin filtros.
-- Sirven para verificar que el dashboard muestra exactamente estos valores.
-- Para filtrar, agregar al WHERE por ejemplo:
--   AND i.fecha_creacion >= '2026-09-01' AND i.fecha_creacion < '2026-11-01'
--   AND i.categoria_id = 1 AND i.prioridad = 'ALTA'
-- Ejecutar:  docker exec -i aegisops-postgres psql -U aegisops -d aegisops < database/consultas_analiticas.sql
-- =====================================================================

-- 1. Resumen: totales, % de resolución, tiempo promedio y mediana (horas)
SELECT COUNT(*)                                                         AS total,
       COUNT(*) FILTER (WHERE i.estado IN ('ABIERTO', 'EN_PROGRESO'))   AS abiertos,
       COUNT(*) FILTER (WHERE i.estado IN ('RESUELTO', 'CERRADO'))      AS resueltos,
       ROUND(100.0 * COUNT(*) FILTER (WHERE i.estado IN ('RESUELTO', 'CERRADO')) / NULLIF(COUNT(*), 0), 2) AS porcentaje_resolucion,
       ROUND(AVG(EXTRACT(EPOCH FROM (i.fecha_resolucion - i.fecha_creacion)) / 3600.0)::numeric, 2) AS tiempo_promedio_horas,
       ROUND((PERCENTILE_CONT(0.5) WITHIN GROUP (ORDER BY EXTRACT(EPOCH FROM (i.fecha_resolucion - i.fecha_creacion)) / 3600.0))::numeric, 2) AS tiempo_mediana_horas,
       COUNT(*) FILTER (WHERE i.prioridad = 'CRITICA' AND i.estado IN ('ABIERTO', 'EN_PROGRESO')) AS criticos_abiertos
FROM incidente i;

-- 2. Evolución semanal: creados y resueltos por semana (lunes)
WITH base AS (
    SELECT i.fecha_creacion, i.fecha_resolucion FROM incidente i
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
ORDER BY s.semana;

-- 3. Distribución por categoría (cantidad y porcentaje)
SELECT c.nombre AS categoria, COUNT(*) AS cantidad,
       ROUND(100.0 * COUNT(*) / SUM(COUNT(*)) OVER (), 2) AS porcentaje
FROM incidente i JOIN categoria c ON c.id = i.categoria_id
GROUP BY c.nombre
ORDER BY cantidad DESC;

-- 4. Distribución por prioridad
SELECT i.prioridad, COUNT(*) AS cantidad,
       ROUND(100.0 * COUNT(*) / SUM(COUNT(*)) OVER (), 2) AS porcentaje
FROM incidente i
GROUP BY i.prioridad
ORDER BY cantidad DESC;

-- 5. Distribución por estado
SELECT i.estado, COUNT(*) AS cantidad,
       ROUND(100.0 * COUNT(*) / SUM(COUNT(*)) OVER (), 2) AS porcentaje
FROM incidente i
GROUP BY i.estado
ORDER BY cantidad DESC;

-- 6. Tiempo de resolución por categoría (horas)
SELECT c.nombre AS categoria,
       COUNT(*) FILTER (WHERE i.estado IN ('RESUELTO', 'CERRADO')) AS resueltos,
       ROUND(AVG(EXTRACT(EPOCH FROM (i.fecha_resolucion - i.fecha_creacion)) / 3600.0)::numeric, 2) AS promedio_horas,
       ROUND(MIN(EXTRACT(EPOCH FROM (i.fecha_resolucion - i.fecha_creacion)) / 3600.0)::numeric, 2) AS minimo_horas,
       ROUND(MAX(EXTRACT(EPOCH FROM (i.fecha_resolucion - i.fecha_creacion)) / 3600.0)::numeric, 2) AS maximo_horas
FROM incidente i JOIN categoria c ON c.id = i.categoria_id
GROUP BY c.nombre
ORDER BY promedio_horas DESC NULLS LAST;

-- 7. Tiempo de resolución por prioridad (horas)
SELECT i.prioridad,
       COUNT(*) FILTER (WHERE i.estado IN ('RESUELTO', 'CERRADO')) AS resueltos,
       ROUND(AVG(EXTRACT(EPOCH FROM (i.fecha_resolucion - i.fecha_creacion)) / 3600.0)::numeric, 2) AS promedio_horas
FROM incidente i
GROUP BY i.prioridad
ORDER BY promedio_horas DESC NULLS LAST;

-- 8. Categorías con mayores problemas
SELECT c.nombre AS categoria,
       COUNT(*) AS total,
       COUNT(*) FILTER (WHERE i.estado IN ('ABIERTO', 'EN_PROGRESO')) AS abiertos,
       COUNT(*) FILTER (WHERE i.prioridad IN ('ALTA', 'CRITICA')) AS alta_o_critica,
       ROUND(100.0 * COUNT(*) FILTER (WHERE i.prioridad IN ('ALTA', 'CRITICA')) / COUNT(*), 2) AS porcentaje_alta_critica,
       ROUND(100.0 * COUNT(*) FILTER (WHERE i.estado IN ('RESUELTO', 'CERRADO')) / COUNT(*), 2) AS porcentaje_resolucion,
       ROUND(AVG(EXTRACT(EPOCH FROM (i.fecha_resolucion - i.fecha_creacion)) / 3600.0)::numeric, 2) AS promedio_horas
FROM incidente i JOIN categoria c ON c.id = i.categoria_id
GROUP BY c.nombre
ORDER BY total DESC, alta_o_critica DESC;

-- 9. Cumplimiento de objetivos de atención (SLA) por prioridad
SELECT i.prioridad,
       MIN(CASE i.prioridad WHEN 'CRITICA' THEN 4 WHEN 'ALTA' THEN 8 WHEN 'MEDIA' THEN 24 ELSE 72 END) AS objetivo_horas,
       COUNT(*) FILTER (WHERE i.estado IN ('RESUELTO', 'CERRADO')) AS resueltos,
       COUNT(*) FILTER (WHERE i.estado IN ('RESUELTO', 'CERRADO')
                          AND EXTRACT(EPOCH FROM (i.fecha_resolucion - i.fecha_creacion)) / 3600.0
                              <= CASE i.prioridad WHEN 'CRITICA' THEN 4 WHEN 'ALTA' THEN 8 WHEN 'MEDIA' THEN 24 ELSE 72 END) AS dentro_objetivo,
       ROUND(100.0 * COUNT(*) FILTER (WHERE i.estado IN ('RESUELTO', 'CERRADO')
                          AND EXTRACT(EPOCH FROM (i.fecha_resolucion - i.fecha_creacion)) / 3600.0
                              <= CASE i.prioridad WHEN 'CRITICA' THEN 4 WHEN 'ALTA' THEN 8 WHEN 'MEDIA' THEN 24 ELSE 72 END)
             / NULLIF(COUNT(*) FILTER (WHERE i.estado IN ('RESUELTO', 'CERRADO')), 0), 2) AS porcentaje_cumplimiento
FROM incidente i
GROUP BY i.prioridad
ORDER BY objetivo_horas;

-- 10. Carga por responsable
SELECT u.nombre AS responsable, u.area,
       COUNT(*) AS asignados,
       COUNT(*) FILTER (WHERE i.estado IN ('ABIERTO', 'EN_PROGRESO')) AS abiertos,
       COUNT(*) FILTER (WHERE i.estado IN ('RESUELTO', 'CERRADO')) AS resueltos,
       ROUND(AVG(EXTRACT(EPOCH FROM (i.fecha_resolucion - i.fecha_creacion)) / 3600.0)::numeric, 2) AS promedio_horas
FROM incidente i JOIN usuario u ON u.id = i.usuario_id
GROUP BY u.nombre, u.area
ORDER BY asignados DESC, responsable;

-- 11. Tendencia de Conectividad: primeras 13 semanas vs. últimas 13 semanas
SELECT CASE WHEN i.fecha_creacion < '2026-08-17' THEN '1. may-ago' ELSE '2. ago-nov' END AS periodo,
       COUNT(*) FILTER (WHERE i.categoria_id = 1) AS conectividad,
       COUNT(*) AS total,
       ROUND(100.0 * COUNT(*) FILTER (WHERE i.categoria_id = 1) / COUNT(*), 2) AS porcentaje_conectividad
FROM incidente i
GROUP BY 1
ORDER BY 1;

-- 12. Asistente RAG: consultas por estado y calificación promedio
SELECT q.estado, COUNT(*) AS cantidad FROM consulta_ia q GROUP BY q.estado ORDER BY cantidad DESC;

SELECT COUNT(k.id) AS calificadas, ROUND(AVG(k.puntuacion)::numeric, 2) AS promedio
FROM consulta_ia q
JOIN respuesta_ia r ON r.consulta_id = q.id
JOIN calificacion k ON k.respuesta_id = r.id;

-- 13. Verificaciones de las pruebas funcionales
-- Prueba 4: incidentes de una categoría (comparar con GET /api/incidentes?categoriaId=1)
SELECT COUNT(*) FROM incidente WHERE categoria_id = 1;
-- Prueba 6: un artículo inactivo no aparece como fuente en respuestas posteriores a su desactivación
SELECT fr.respuesta_id, fr.articulo_id, a.estado, r.fecha
FROM fuente_respuesta fr JOIN articulo_conocimiento a ON a.id = fr.articulo_id
JOIN respuesta_ia r ON r.id = fr.respuesta_id
WHERE a.estado <> 'ACTIVO' OR a.verificado = FALSE;
-- Prueba 9: historial de un incidente
SELECT q.id, q.fecha, q.estado, r.id AS respuesta_id FROM consulta_ia q
LEFT JOIN respuesta_ia r ON r.consulta_id = q.id
WHERE q.incidente_id = 301 ORDER BY q.fecha DESC;
-- Prueba 10: calificación persistida
SELECT * FROM calificacion ORDER BY fecha DESC LIMIT 5;
