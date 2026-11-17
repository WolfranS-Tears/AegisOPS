package com.aegisops.backend.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Protege los endpoints /api/internal/** (los consume n8n) con un token
 * compartido enviado en la cabecera X-Internal-Token.
 */
@Component
public class InternalTokenFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Internal-Token";

    private final AegisProperties props;

    public InternalTokenFilter(AegisProperties props) {
        this.props = props;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/internal/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String esperado = props.internal().token();
        if (esperado == null || esperado.isBlank()) {
            chain.doFilter(request, response);
            return;
        }
        String recibido = request.getHeader(HEADER);
        boolean valido = recibido != null && MessageDigest.isEqual(
                esperado.getBytes(StandardCharsets.UTF_8), recibido.getBytes(StandardCharsets.UTF_8));
        if (!valido) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("{\"title\":\"No autorizado\",\"detail\":\"Token interno inválido\"}");
            return;
        }
        chain.doFilter(request, response);
    }
}
