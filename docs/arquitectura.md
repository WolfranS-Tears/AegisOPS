# Arquitectura — AegisOPS

## Componentes

```mermaid
flowchart LR
    U[Usuario<br/>navegador] -->|HTTP| FE[Frontend<br/>HTML + CSS + JS vanilla<br/>nginx :8081]
    FE -->|REST JSON<br/>/api/**| BE[Backend<br/>Spring Boot 4 · Java 21<br/>:8080]
    BE -->|JPA / JDBC| DB[(PostgreSQL 17<br/>:5433)]
    BE -->|Webhooks| N8N[n8n<br/>:5678]
    N8N -->|/api/internal/**<br/>X-Internal-Token| BE
    N8N -->|embeddings · chat| OR[OpenRouter]
    N8N -->|upsert · search · delete| QD[(Qdrant 1.15<br/>:6333)]

    classDef ext fill:#1e293b,stroke:#8b5cf6,color:#f8fafc
    class OR ext
```

**Regla de negocio 11:** el frontend solo habla con Spring Boot. n8n, Qdrant y OpenRouter quedan detrás del backend. Las claves viven en variables de entorno (`.env`) y en las credenciales de n8n, nunca en el código.

## Capas del backend

```
com.aegisops.backend
├── config      AegisProperties (variables de entorno), CORS, OpenAPI, filtro X-Internal-Token
├── domain      Entidades JPA y enums (Usuario, Categoria, Incidente, ArticuloConocimiento,
│               ConsultaIA, RespuestaIA, FuenteRespuesta, Calificacion)
├── repository  Repositorios JPA + AnaliticaRepository (SQL nativo verificable)
├── service     Reglas de negocio, N8nClient (único cliente hacia n8n), FiltroFuentes
├── dto         Records de entrada/salida y contrato con n8n
├── web         Controladores REST
└── exception   Errores RFC 9457 (ProblemDetail)
```

## Flujo 1 — Indexación de la base de conocimiento (RF03)

```mermaid
sequenceDiagram
    participant FE as Frontend
    participant BE as Spring Boot
    participant N as n8n (Flujo 1)
    participant OR as OpenRouter
    participant Q as Qdrant
    FE->>BE: POST /api/articulos/indexar
    BE->>N: POST /webhook/aegisops-indexar {articuloId|null}
    N->>Q: PUT /collections/articulos_conocimiento (si no existe)
    N->>BE: GET /api/internal/articulos/indexacion
    BE-->>N: artículos + indexable (ACTIVO y verificado)
    N->>OR: POST /embeddings (lote de textos)
    OR-->>N: vectores 1536-d
    N->>Q: PUT points (id = articulo.id) → upsert sin duplicados
    N->>Q: POST points/delete (no indexables)
    N-->>BE: {indexados, retirados}
    BE-->>FE: resumen
```

Además, al crear, editar, activar o desactivar un artículo, el backend dispara el Flujo 1 solo para ese artículo después de confirmar la transacción. Lo hace en modo de mejor esfuerzo: si n8n no responde, el artículo igual queda excluido como fuente, porque el RAG valida contra PostgreSQL.

## Flujo 2 — Asistencia RAG (RF04, RF05, RF08)

```mermaid
sequenceDiagram
    participant FE as Frontend
    participant BE as Spring Boot
    participant DB as PostgreSQL
    participant N as n8n (Flujo 2)
    participant OR as OpenRouter
    participant Q as Qdrant
    FE->>BE: POST /api/asistente/consultas {incidenteId, pregunta}
    BE->>BE: @NotBlank: si la pregunta está vacía → 400, no se llama a n8n
    BE->>DB: INSERT consulta_ia (PENDIENTE, umbral)
    BE->>N: POST /webhook/aegisops-rag {consultaId, pregunta, umbral, topK}
    N->>OR: embedding de la pregunta
    N->>Q: search (limit=topK, score_threshold=umbral, indexable=true)
    alt sin candidatas
        N-->>BE: {estado: SIN_RESULTADOS}
    else hay candidatas
        N->>BE: GET /api/internal/articulos/fuentes?ids=…
        BE-->>N: solo ACTIVO + verificado (contenido desde PostgreSQL)
        N->>OR: chat/completions (solo con ese contexto, JSON)
        N-->>BE: {estado: RESPONDIDA, solucion, pasos, fuentes[similitud]}
    end
    BE->>BE: FiltroFuentes: umbral, duplicados, topK
    BE->>DB: valida que las fuentes existan y estén activas
    BE->>DB: INSERT respuesta_ia + fuente_respuesta, estado final
    BE-->>FE: consulta con respuesta, pasos, fuentes y similitudes
    Note over BE,N: Si n8n, OpenRouter o Qdrant fallan → estado ERROR con el mensaje
```

Estados de la consulta: `PENDIENTE` → `RESPONDIDA` | `SIN_RESULTADOS` | `ERROR`.

## Despliegue (Docker Compose)

| Servicio | Imagen | Puerto host |
|---|---|---|
| postgres | postgres:17-alpine | 5433 |
| qdrant | qdrant/qdrant:v1.15.4 | 6333 / 6334 |
| backend | build `./Backend` (Temurin 21) | 8080 |
| frontend | nginx:1.27-alpine | 8081 |
| n8n (perfil `n8n`) | n8nio/n8n | 5678 |
