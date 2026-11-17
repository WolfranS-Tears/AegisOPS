package com.aegisops.backend.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Configuración propia de AegisOPS (prefijo "aegisops" en application.yml).
 * Todos los valores sensibles o de entorno llegan por variables de entorno.
 */
@Validated
@ConfigurationProperties(prefix = "aegisops")
public record AegisProperties(@Valid @NotNull N8n n8n,
                              @Valid @NotNull Rag rag,
                              @Valid @NotNull Cors cors,
                              @Valid @NotNull Internal internal) {

    public record N8n(@NotBlank String baseUrl,
                      @NotBlank String ragWebhook,
                      @NotBlank String indexWebhook,
                      @NotNull Duration timeout) {
    }

    /**
     * Umbral de similitud coseno (0 a 1) y número de fuentes a recuperar.
     * El enunciado pide entre 3 y 5 fuentes, por eso topK se limita a ese rango.
     */
    public record Rag(@DecimalMin("0.0") @DecimalMax("1.0") double similarityThreshold,
                      @Min(3) @Max(5) int topK) {
    }

    public record Cors(@NotNull List<String> allowedOrigins) {
    }

    /** Token compartido con n8n para los endpoints /api/internal. Vacío = sin protección (solo desarrollo). */
    public record Internal(String token) {
    }
}
