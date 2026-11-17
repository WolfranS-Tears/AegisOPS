package com.aegisops.backend.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final AegisProperties props;

    public WebConfig(AegisProperties props) {
        this.props = props;
    }

    /** Solo el frontend configurado puede consumir la API desde el navegador. */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(props.cors().allowedOrigins().toArray(String[]::new))
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*");
    }

    @Bean
    OpenAPI aegisOpenApi() {
        return new OpenAPI().info(new Info()
                .title("AegisOPS API")
                .version("1.0.0")
                .description("Gestión de incidentes, base de conocimiento, asistente RAG y analítica. "
                        + "El frontend solo consume esta API; n8n, Qdrant y OpenRouter quedan detrás del backend."));
    }
}
