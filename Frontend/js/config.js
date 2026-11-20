/**
 * AegisOps — Configuración del frontend.
 * El frontend SOLO consume la API de Spring Boot. Nunca llama a n8n, Qdrant ni OpenRouter.
 */
globalThis.AEGIS_CONFIG = {
    apiBase: 'http://localhost:8082/api',
};
