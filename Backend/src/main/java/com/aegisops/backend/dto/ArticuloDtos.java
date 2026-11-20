package com.aegisops.backend.dto;

import com.aegisops.backend.domain.ArticuloConocimiento;
import com.aegisops.backend.domain.Enums.EstadoArticulo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

public final class ArticuloDtos {

    private ArticuloDtos() {
    }

    public record ArticuloRequest(
            @NotBlank(message = "El título es obligatorio") @Size(max = 200) String titulo,
            @NotBlank(message = "El problema es obligatorio") String problema,
            @NotBlank(message = "Los síntomas son obligatorios") String sintomas,
            @NotBlank(message = "La causa es obligatoria") String causa,
            @NotBlank(message = "La solución es obligatoria") String solucion,
            @NotNull(message = "La categoría es obligatoria") Long categoriaId,
            EstadoArticulo estado,
            Boolean verificado) {
    }

    public record CambioEstadoArticuloRequest(@NotNull(message = "El estado es obligatorio") EstadoArticulo estado,
                                              Boolean verificado) {
    }

    public record ArticuloResponse(
            Long id, String titulo, String problema, String sintomas, String causa, String solucion,
            Long categoriaId, String categoria, EstadoArticulo estado, boolean verificado,
            boolean fuenteValida, LocalDateTime fechaCreacion, LocalDateTime fechaActualizacion) {

        public static ArticuloResponse de(ArticuloConocimiento a) {
            return new ArticuloResponse(a.getId(), a.getTitulo(), a.getProblema(), a.getSintomas(),
                    a.getCausa(), a.getSolucion(), a.getCategoria().getId(), a.getCategoria().getNombre(),
                    a.getEstado(), a.isVerificado(), a.esFuenteValida(),
                    a.getFechaCreacion(), a.getFechaActualizacion());
        }
    }

    /** Representación que consume el flujo de indexación de n8n. */
    public record ArticuloIndexacion(Long id, String titulo, String categoria, EstadoArticulo estado,
                                     boolean verificado, boolean indexable, String texto,
                                     LocalDateTime fechaActualizacion) {

        public static ArticuloIndexacion de(ArticuloConocimiento a) {
            return new ArticuloIndexacion(a.getId(), a.getTitulo(), a.getCategoria().getNombre(),
                    a.getEstado(), a.isVerificado(), a.esFuenteValida(), a.textoParaEmbedding(),
                    a.getFechaActualizacion());
        }
    }

    /** Contenido de una fuente válida; n8n lo usa para construir el contexto del RAG. */
    public record ArticuloFuente(Long id, String titulo, String categoria, String problema,
                                 String sintomas, String causa, String solucion) {

        public static ArticuloFuente de(ArticuloConocimiento a) {
            return new ArticuloFuente(a.getId(), a.getTitulo(), a.getCategoria().getNombre(),
                    a.getProblema(), a.getSintomas(), a.getCausa(), a.getSolucion());
        }
    }
}
