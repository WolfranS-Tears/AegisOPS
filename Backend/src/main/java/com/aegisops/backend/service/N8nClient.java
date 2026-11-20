package com.aegisops.backend.service;

import com.aegisops.backend.config.AegisProperties;
import com.aegisops.backend.dto.AsistenteDtos.N8nRagRequest;
import com.aegisops.backend.dto.AsistenteDtos.N8nRagResponse;
import com.aegisops.backend.exception.Excepciones;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/**
 * Único punto de contacto del backend con n8n. El frontend nunca llama a n8n,
 * Qdrant ni OpenRouter: todo pasa por aquí (regla de negocio 11).
 */
@Component
public class N8nClient {

    private final RestClient http;
    private final AegisProperties.N8n config;

    public N8nClient(AegisProperties props) {
        this.config = props.n8n();
        // HTTP/1.1 explícito: el upgrade h2c por defecto del cliente JDK no lo acepta n8n
        HttpClient jdk = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(jdk);
        factory.setReadTimeout(config.timeout());
        this.http = RestClient.builder()
                .baseUrl(config.baseUrl())
                .requestFactory(factory)
                .build();
    }

    /** Ejecuta el flujo 2 (asistencia RAG). Lanza Integracion si n8n falla o no responde. */
    public N8nRagResponse consultarRag(N8nRagRequest request) {
        try {
            N8nRagResponse r = http.post()
                    .uri(config.ragWebhook())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(N8nRagResponse.class);
            if (r == null || r.estado() == null) {
                throw new Excepciones.Integracion("n8n devolvió una respuesta vacía o sin estado", null);
            }
            return r;
        } catch (RestClientResponseException e) {
            throw new Excepciones.Integracion("El flujo RAG respondió HTTP " + e.getStatusCode().value()
                    + ": " + recortar(e.getResponseBodyAsString()), e);
        } catch (ResourceAccessException e) {
            throw new Excepciones.Integracion("No fue posible conectar con n8n (" + config.baseUrl()
                    + "): " + e.getMostSpecificCause().getMessage(), e);
        } catch (RestClientException e) {
            throw new Excepciones.Integracion("Respuesta inválida del flujo RAG: " + e.getMessage(), e);
        }
    }

    /** Ejecuta el flujo 1 (indexación). articuloId null = reindexar toda la base. */
    public Map<String, Object> indexar(Long articuloId) {
        Map<String, Object> body = new HashMap<>();
        body.put("articuloId", articuloId);
        try {
            return http.post()
                    .uri(config.indexWebhook())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() { });
        } catch (RestClientResponseException e) {
            throw new Excepciones.Integracion("El flujo de indexación respondió HTTP " + e.getStatusCode().value()
                    + ": " + recortar(e.getResponseBodyAsString()), e);
        } catch (RestClientException e) {
            throw new Excepciones.Integracion("No fue posible ejecutar la indexación en n8n: "
                    + e.getMessage(), e);
        }
    }

    private static String recortar(String texto) {
        if (texto == null) {
            return "";
        }
        return texto.length() > 300 ? texto.substring(0, 300) + "..." : texto;
    }
}
