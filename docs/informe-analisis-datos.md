# Informe de análisis de datos — AegisOPS

Este informe presenta los indicadores calculados sobre el histórico de incidentes, las consultas SQL que los producen, la interpretación de los resultados y las conclusiones respaldadas por evidencia.

> **Reproducibilidad.** Todos los valores salen de `database/consultas_analiticas.sql`, ejecutado sobre los datos iniciales (`database/02_seed.sql`). Esos datos los genera `database/generador/generar_datos.py` con la semilla fija `2026`. El dashboard (`GET /api/analitica/dashboard`) ejecuta las mismas consultas desde `AnaliticaRepository.java`. Ninguna cifra la produce la IA.
>
> ```bash
> docker exec -i aegisops-postgres psql -U aegisops -d aegisops < database/consultas_analiticas.sql
> ```

## 1. Conjunto de datos

| Entidad | Registros | Observación |
|---|---:|---|
| Categorías | 8 | Conectividad, Hardware, Aplicaciones, Sistema operativo, Correo, Seguridad, Impresión, Accesos |
| Usuarios (técnicos) | 12 | 6 áreas |
| Artículos de conocimiento | 30 | 26 activos y verificados, 2 inactivos, 2 borradores sin verificar |
| Incidentes | 300 | 26 semanas: 2026-05-18 a 2026-11-12 (fecha de corte) |

**Datos sintéticos documentados.** El generador asigna fechas en horario laboral. Calcula el tiempo de resolución con una distribución log-normal según la prioridad (mediana: crítica 3 h, alta 8 h, media 22 h, baja 52 h) y un factor por categoría (hardware ×1,6, accesos ×0,6). También hace que Conectividad crezca 4,5 % por semana.

Restricciones que la base garantiza:
- Un incidente `RESUELTO`/`CERRADO` siempre tiene `fecha_resolucion >= fecha_creacion`.
- Un incidente abierto no tiene `fecha_resolucion` (CHECK `ck_incidente_resolucion`).

## 2. Indicadores implementados

| # | Indicador | Endpoint | Consulta (en `consultas_analiticas.sql`) |
|---|---|---|---|
| 1 | Total, abiertos, resueltos, % de resolución | `/api/analitica/resumen` | §1 |
| 2 | Tiempo promedio y mediana de resolución | `/api/analitica/resumen` | §1 |
| 3 | Evolución semanal (creados vs. resueltos) | `/api/analitica/tendencia-semanal` | §2 |
| 4 | Distribución por categoría / prioridad / estado | `/api/analitica/distribucion?dimension=` | §3, §4, §5 |
| 5 | Tiempo de resolución por categoría y prioridad | `/api/analitica/tiempos-resolucion?dimension=` | §6, §7 |
| 6 | Categorías con mayores problemas | `/api/analitica/categorias-criticas` | §8 |
| 7 | Cumplimiento de objetivos de atención (SLA) | `/api/analitica/sla` | §9 |
| 8 | Carga por responsable | `/api/analitica/responsables` | §10 |
| 9 | Indicadores del asistente (estados, calificación, artículos más usados) | `/api/analitica/asistente` | §12 |

Todos aceptan los filtros `desde`, `hasta` (yyyy-MM-dd), `categoriaId` y `prioridad`.

**Definiciones**
- *Tiempo de resolución* = `fecha_resolucion − fecha_creacion`, en horas. Solo cuenta incidentes resueltos o cerrados.
- *% de resolución* = (resueltos + cerrados) / total × 100.
- *Objetivos SLA*: Crítica 4 h, Alta 8 h, Media 24 h, Baja 72 h.

## 3. Resultados

### 3.1 Resumen general

| Total | Abiertos | Resueltos | % resolución | Promedio (h) | Mediana (h) | Críticos abiertos |
|---:|---:|---:|---:|---:|---:|---:|
| 300 | 14 | 286 | **95,33 %** | **31,11** | **19,58** | 0 |

### 3.2 Distribución por categoría

| Categoría | Incidentes | % |
|---|---:|---:|
| Conectividad y redes | 85 | 28,33 |
| Aplicaciones y software | 51 | 17,00 |
| Hardware | 41 | 13,67 |
| Sistema operativo | 34 | 11,33 |
| Accesos y cuentas | 28 | 9,33 |
| Correo y colaboración | 28 | 9,33 |
| Seguridad | 20 | 6,67 |
| Impresión y periféricos | 13 | 4,33 |

### 3.3 Distribución por prioridad y estado

| Prioridad | Cantidad | % | | Estado | Cantidad | % |
|---|---:|---:|---|---|---:|---:|
| Media | 137 | 45,67 | | Cerrado | 175 | 58,33 |
| Baja | 70 | 23,33 | | Resuelto | 111 | 37,00 |
| Alta | 69 | 23,00 | | Abierto | 10 | 3,33 |
| Crítica | 24 | 8,00 | | En progreso | 4 | 1,33 |

### 3.4 Tiempo promedio de resolución

| Categoría | Resueltos | Promedio (h) | Mín (h) | Máx (h) |
|---|---:|---:|---:|---:|
| Hardware | 39 | 43,61 | 2,46 | 264,03 |
| Aplicaciones y software | 48 | 41,25 | 1,55 | 192,97 |
| Sistema operativo | 34 | 35,77 | 1,74 | 169,16 |
| Impresión y periféricos | 12 | 31,51 | 3,80 | 118,97 |
| Correo y colaboración | 25 | 28,79 | 1,98 | 97,88 |
| Conectividad y redes | 82 | 25,95 | 0,84 | 130,70 |
| Seguridad | 18 | 16,94 | 3,76 | 43,20 |
| Accesos y cuentas | 28 | 16,73 | 1,75 | 59,13 |

| Prioridad | Resueltos | Promedio (h) |
|---|---:|---:|
| Baja | 65 | 72,34 |
| Media | 130 | 26,32 |
| Alta | 67 | 10,53 |
| Crítica | 24 | 2,81 |

### 3.5 Categorías con mayores problemas

| Categoría | Total | Abiertos | Alta/Crítica | % Alta/Crítica | % Resolución | Promedio (h) |
|---|---:|---:|---:|---:|---:|---:|
| Conectividad y redes | 85 | 3 | 33 | 38,82 | 96,47 | 25,95 |
| Aplicaciones y software | 51 | 3 | 17 | 33,33 | 94,12 | 41,25 |
| Hardware | 41 | 2 | 8 | 19,51 | 95,12 | 43,61 |
| Sistema operativo | 34 | 0 | 13 | 38,24 | 100,00 | 35,77 |

### 3.6 Cumplimiento de objetivos (SLA)

| Prioridad | Objetivo | Resueltos | Dentro del objetivo | % cumplimiento |
|---|---:|---:|---:|---:|
| Crítica | 4 h | 24 | 21 | **87,50** |
| Alta | 8 h | 67 | 31 | **46,27** |
| Media | 24 h | 130 | 70 | 53,85 |
| Baja | 72 h | 65 | 38 | 58,46 |

### 3.7 Tendencia de Conectividad

| Periodo | Conectividad | Total | % Conectividad |
|---|---:|---:|---:|
| 18-may a 16-ago (13 semanas) | 28 | 116 | 24,14 |
| 17-ago a 12-nov (13 semanas) | 57 | 184 | **30,98** |

### 3.8 Evolución semanal

El volumen semanal pasó de 4–12 incidentes en mayo y julio a 10–21 desde mediados de agosto. Los picos están en las semanas del 24-ago (21), 12-oct (20) y 08-jun (18). La serie de resueltos sigue de cerca a la de creados. La semana del 09-nov está incompleta (corte el 12-nov) y por eso muestra 12 creados y 8 resueltos.

## 4. Interpretación

1. **El volumen crece, sobre todo en Conectividad.** En la segunda mitad del periodo hay 59 % más incidentes (184 frente a 116). Conectividad casi duplica su cantidad (28 → 57) y pasa de 24 % a 31 % del total, así que su crecimiento supera al general.
2. **La resolución es alta, pero lenta en prioridad Alta.** El 95,33 % de los incidentes está resuelto o cerrado y no hay críticos abiertos. Sin embargo, solo el 46,27 % de los incidentes de prioridad Alta se resolvió dentro de las 8 h objetivo: tardan en promedio 10,53 h.
3. **Hardware y Aplicaciones son las categorías más lentas.** Promedian 43,61 h y 41,25 h, con máximos de más de 190 h. En cambio, Accesos y Seguridad se resuelven en unas 17 h.
4. **El promedio está sesgado por casos extremos.** La mediana (19,58 h) es bastante menor que el promedio (31,11 h). Por eso el dashboard muestra ambas.
5. **La carga no está equilibrada.** Mesa de ayuda (Carlos Rincón, 42 incidentes) y Soporte en sitio (Andrés Torres, 37 con 50,61 h de promedio) concentran el trabajo. Las personas de Seguridad atienden menos casos y más rápido.

## 5. Conclusiones respaldadas por evidencia

1. **Conectividad debe ser la primera prioridad de la base de conocimiento y de la inversión en red.** Es la categoría con más incidentes (85; 28,33 %), la que más alta/crítica acumula (33) y la que más crece (de 24,14 % a 30,98 % del total entre los dos periodos de 13 semanas). Evidencia: consultas §3, §8 y §11.
2. **El objetivo de 8 h para prioridad Alta no se cumple y requiere acción.** Solo 31 de 67 casos (46,27 %) se resolvieron a tiempo, con un promedio de 10,53 h. Para Crítica el cumplimiento es 87,50 %. Se recomienda escalar los casos de Alta a las 6 h y usar el asistente RAG en la primera atención. Evidencia: consultas §7 y §9.
3. **Hardware necesita un stock de repuestos o equipos de respaldo.** Tiene el mayor tiempo promedio de resolución (43,61 h, máximo 264 h), aunque solo el 19,51 % de sus casos es alta/crítica. La demora se explica por la logística, no por la urgencia. Evidencia: consultas §6 y §8.
4. **La carga de Mesa de ayuda y Soporte en sitio debería redistribuirse.** Los dos técnicos con más asignaciones tienen 42 y 37 casos, contra 13 a 18 en otras áreas. Soporte en sitio tiene el promedio más alto (50,61 h). Evidencia: consulta §10.

## 6. Umbral de similitud del asistente RAG (RF04)

- **Valor por defecto:** `RAG_SIMILARITY_THRESHOLD=0.50` (similitud coseno, de 0 a 1), con `RAG_TOP_K=5`.
- **Justificación:**
  - El modelo `openai/text-embedding-3-small` produce similitudes coseno de aproximadamente 0,1 a 0,35 entre textos técnicos de temas distintos.
  - Para descripciones del mismo problema con otras palabras, las similitudes suelen estar entre 0,5 y 0,75.
  - Con 0,50 se descartan coincidencias temáticas débiles, por ejemplo una pregunta de impresoras que recupere un artículo de red solo por compartir vocabulario técnico. Al mismo tiempo se aceptan paráfrasis reales del mismo problema.
  - Un umbral más alto (≥ 0,7) dejaría sin respuesta consultas válidas redactadas por usuarios no técnicos.
- **Calibración:** el valor es configurable por variable de entorno y queda guardado en cada consulta (`consulta_ia.umbral`) para trazabilidad. El procedimiento para validarlo con las pruebas funcionales 1 a 3 es:
  1. Ejecutar las consultas.
  2. Revisar en el historial la similitud de cada fuente recuperada.
  3. Ajustar el umbral si una fuente pertinente queda por debajo o una no pertinente queda por encima.

  Las similitudes medidas se registran en `docs/pruebas-funcionales.md`.
- **Doble control:**
  - Qdrant aplica `score_threshold`.
  - El backend vuelve a filtrar por umbral y confirma en PostgreSQL que cada fuente esté `ACTIVO` y verificada antes de guardarla.
  - Si no queda ninguna fuente, la consulta termina en `SIN_RESULTADOS` y no se genera ninguna solución.
