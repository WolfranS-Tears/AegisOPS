# Documento de pruebas funcionales — AegisOPS

**Entorno:** `docker compose up -d --build` con los datos iniciales, n8n 2.47 con los flujos importados y backend en `http://localhost:8080`.

**Herramientas:**
- Frontend (`http://localhost:8081`).
- Swagger (`http://localhost:8080/swagger-ui.html`).
- Colección Postman (`postman/AegisOPS.postman_collection.json`).
- SQL (`database/consultas_analiticas.sql`, §13).

**Estado de cada prueba:**
- ✅ Ejecutada y aprobada.
- ⏳ Requiere la clave de OpenRouter configurada en n8n. Registrar el resultado al ejecutarla.

| # | Prueba | Requisito | Estado |
|---|---|---|---|
| 1 | Incidente de conectividad con soluciones RAG | RF01, RF04, RF05 | ⏳ |
| 2 | Error de ejecución en una aplicación | RF04 | ⏳ |
| 3 | Incidente sin información en la base: no inventar | RF05, regla 11 | ⏳ |
| 4 | Incidentes por categoría = base relacional | RF01, RF06 | ✅ |
| 5 | Evolución semanal = SQL | RF06, RF07 | ✅ |
| 6 | Artículo desactivado no vuelve a ser fuente | RF02, regla 11 | ✅ (validación en backend) / ⏳ (Qdrant) |
| 7 | Descripción vacía no ejecuta el flujo | Regla 11 | ✅ |
| 8 | Fallo del servicio de embeddings | Estados de consulta | ✅ |
| 9 | Historial de un incidente | RF08 | ✅ |
| 10 | Calificar una respuesta y verificar persistencia | RF08 | ⏳ (necesita una respuesta generada) |

---

## Prueba 1: incidente de conectividad con RAG

1. En **Incidentes → Nuevo incidente** registrar:
   - Título: "Sin internet en sala de juntas".
   - Descripción: "Ningún equipo de la sala obtiene IP, aparece 169.254 y no navegan".
   - Categoría: Conectividad. Prioridad: Alta.
2. En la fila del incidente pulsar el botón del asistente. La pregunta se precarga con la descripción. Pulsar **Consultar asistente**.

**Resultado esperado**
- Estado `RESPONDIDA`.
- Solución con pasos tomados del artículo #1 ("Equipo sin acceso a internet por falla de DHCP"): `ipconfig /release`/`/renew`, `netsh winsock reset`, validar el puerto del switch.
- Fuentes con similitud ≥ umbral, con el artículo #1 en primer lugar.

**Verificación SQL**
```sql
SELECT q.id, q.estado, fr.articulo_id, fr.similitud
FROM consulta_ia q JOIN respuesta_ia r ON r.consulta_id = q.id
JOIN fuente_respuesta fr ON fr.respuesta_id = r.id
WHERE q.incidente_id = <id> ORDER BY fr.similitud DESC;
```
**Resultado obtenido:** _(registrar las similitudes obtenidas)_

## Prueba 2: error de ejecución en una aplicación

**Pregunta:** "Al abrir el programa contable se cierra con el error 0xc000007b y dice que falta VCRUNTIME140.dll".

**Esperado:** `RESPONDIDA`, con el artículo #11 ("Aplicación de escritorio se cierra al iniciar por falta de componentes") como fuente principal y pasos para reinstalar los redistribuibles de Visual C++. Variante: con "java.lang.OutOfMemoryError: Java heap space" debe recuperar el artículo #12.

**Resultado obtenido:** _(registrar)_

## Prueba 3: sin información suficiente, sin inventar

**Pregunta:** "El aire acondicionado del cuarto de servidores gotea agua sobre el rack y la alarma de humedad suena".

**Esperado:**
- Estado `SIN_RESULTADOS`.
- La respuesta es exactamente el mensaje fijo del backend ("No se encontró información suficientemente relevante…").
- Sin pasos y sin fuentes.
- El flujo no llama al LLM o, si lo llama, este responde `suficiente=false`.

**Verificación:** `SELECT estado FROM consulta_ia ORDER BY id DESC LIMIT 1;` → `SIN_RESULTADOS`.

**Resultado obtenido:** _(registrar la similitud máxima observada en la ejecución de n8n)_

## Prueba 4: incidentes por categoría ✅

**Pasos:** `GET /api/incidentes?categoriaId=1&tamano=200`, o en el frontend filtrar por "Conectividad y redes".

**Verificación:** `SELECT COUNT(*) FROM incidente WHERE categoria_id = 1;`

**Resultado obtenido:** la API devolvió `totalElementos = 85` y SQL devolvió `85`. Coinciden. Con `dimension=categoria`, `/api/analitica/distribucion` también reporta 85 (28,33 %).

## Prueba 5: evolución semanal ✅

**Pasos:** abrir **Dashboard** → "Evolución semanal", o `GET /api/analitica/tendencia-semanal`.

**Verificación:** consulta §2 de `consultas_analiticas.sql`.

**Resultado obtenido:** 26 semanas, idénticas en la API y en SQL. Por ejemplo, 2026-05-18: 10 creados y 7 resueltos; 2026-08-24: 21 y 22; 2026-11-09: 12 y 8. El filtro `categoriaId=1` también coincide con la consulta filtrada.

## Prueba 6: desactivar un artículo ✅ / ⏳

**Pasos**
1. **Conocimiento** → artículo #3 → **Desactivar** (`PATCH /api/articulos/3/estado {"estado":"INACTIVO"}`).
2. Preguntar al asistente "Puedo hacer ping a una IP pero no abren páginas por nombre, error DNS".

**Esperado**
- El artículo #3 no aparece como fuente.
- El backend dispara el Flujo 1 para ese artículo y n8n lo retira de Qdrant.
- Aunque Qdrant no se hubiera actualizado, `/api/internal/articulos/fuentes` y la validación final del backend lo excluyen.

**Resultado obtenido (validación en backend)**
- `PATCH` → `INACTIVO, fuenteValida=false`.
- `GET /api/internal/articulos/fuentes?ids=1,2,3` → `[1, 2]`. El artículo 3 queda excluido.

**Verificación SQL:** consulta §13 (prueba 6). Debe devolver 0 filas de fuentes inactivas usadas después de la desactivación.

## Prueba 7: descripción vacía ✅

**Pasos:** `POST /api/asistente/consultas {"incidenteId":301,"pregunta":"   "}`.

**Resultado obtenido**
- HTTP 400 `{"title":"Datos inválidos","errores":{"pregunta":"La descripción del problema es obligatoria"}}`.
- No se creó ninguna fila en `consulta_ia` y n8n no recibió llamada.
- El frontend también bloquea el envío y muestra el error bajo el campo.
- Como tercera barrera, el flujo de n8n responde 400 si recibe una pregunta vacía.

## Prueba 8: fallo en el servicio de embeddings ✅

**Simulación:** cualquiera de estas opciones:
- Detener n8n (`docker stop n8n`).
- Desactivar el flujo 2.
- Poner una clave inválida en la credencial "OpenRouter API".
- Cambiar `modeloEmbedding` en el nodo Config por un modelo inexistente.

**Resultado obtenido (flujo no disponible)**
- La consulta quedó registrada con estado `ERROR` y `mensaje_error = "El flujo RAG respondió HTTP 404: … webhook not registered"`.
- La API respondió 201 con la consulta en estado `ERROR`.
- El frontend mostró "No fue posible generar la orientación" y un aviso de error.

Con OpenRouter fallando, el nodo HTTP de n8n sale por su rama de error y responde `502 {"estado":"ERROR","error":…}`. El backend lo guarda igual.

## Prueba 9: historial de un incidente ✅

**Pasos:** `GET /api/asistente/consultas?incidenteId=301`, o en **Asistente** seleccionar el incidente (también disponible en **Historial IA**).

**Resultado obtenido:** se listan las consultas del incidente, de la más reciente a la más antigua, con pregunta, fecha, estado, umbral, respuesta, fuentes y calificación.

**Verificación SQL:** consulta §13 (prueba 9).

## Prueba 10: calificar una respuesta ⏳

**Pasos**
1. Sobre una consulta `RESPONDIDA` o `SIN_RESULTADOS`, marcar 4 estrellas y escribir un comentario.
2. Equivale a `PUT /api/asistente/respuestas/{id}/calificacion {"puntuacion":4,"comentario":"Sirvió"}`.

**Esperado**
- HTTP 200 con la calificación.
- Si se repite la llamada, se actualiza la misma fila (`uk_calificacion_respuesta`).
- `puntuacion` 0 o 6 → HTTP 400 ("La puntuación mínima es 1" / "La puntuación máxima es 5").

**Verificación:** `SELECT * FROM calificacion WHERE respuesta_id = <id>;`

**Resultado obtenido:** _(registrar)_

---

## Pruebas automatizadas

`FiltroFuentesTest` (JUnit 5, 5 casos) verifica que se descarten fuentes bajo el umbral, que se eliminen duplicados conservando la mayor similitud, el orden y el límite topK, y que se ignoren valores nulos o fuera de rango.

```bash
cd Backend && ./mvnw test
```
