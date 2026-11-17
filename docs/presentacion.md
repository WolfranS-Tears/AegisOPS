# Guion de la presentación final — AegisOPS

Duración sugerida: 15 minutos. Cada diapositiva indica qué mostrar y de dónde sale.

1. **Problema** (1 min): soporte que resuelve una y otra vez los mismos problemas, sin aprovechar el histórico y sin búsqueda inteligente.
2. **Objetivo y alcance** (1 min): RF01 a RF08 y las reglas de negocio clave (el frontend solo habla con Spring Boot; sin fuentes no hay solución).
3. **Arquitectura** (2 min): diagrama de componentes de `docs/arquitectura.md`. Recorrer el camino de una consulta.
4. **Modelo de datos** (1 min): diagrama E-R de `docs/modelo-entidad-relacion.md`. Destacar `ck_incidente_resolucion` y las fuentes con similitud.
5. **Flujos n8n** (2 min): capturas de los dos flujos. Explicar el upsert con `id = articulo.id`, el umbral y la triple validación de fuentes.
6. **Demo en vivo** (4 min):
   - Registrar un incidente de conectividad → asistente → respuesta con fuentes y similitudes (prueba 1).
   - Pregunta sin respaldo → `SIN_RESULTADOS` (prueba 3).
   - Desactivar un artículo y repetir la consulta (prueba 6).
   - Calificar la respuesta (prueba 10).
7. **Dashboard** (2 min): filtros por fechas, categoría y prioridad. Comparar un valor con su consulta SQL en `psql` (prueba 5).
8. **Hallazgos del análisis** (1,5 min): las 4 conclusiones de `docs/informe-analisis-datos.md` §5, con sus cifras.
9. **Pruebas y calidad** (0,5 min): tabla de estado de `docs/pruebas-funcionales.md`, pruebas unitarias y Swagger.
10. **Uso de IA y trabajo en equipo** (1 min): `AI_USAGE.md`, ramas y pull requests, aporte de cada integrante.
11. **Cierre** (0,5 min): mejoras futuras (autenticación real, recalibración del umbral con datos reales, alertas SLA).
