package com.aegisops.backend.exception;

/** Excepciones de la aplicación. Cada una se traduce a un código HTTP en {@link GlobalExceptionHandler}. */
public final class Excepciones {

    private Excepciones() {
    }

    /** 404: el recurso no existe. */
    public static class NoEncontrado extends RuntimeException {
        public NoEncontrado(String recurso, Object id) {
            super(recurso + " con id " + id + " no existe");
        }
    }

    /** 400: se viola una regla de negocio. */
    public static class ReglaNegocio extends RuntimeException {
        public ReglaNegocio(String mensaje) {
            super(mensaje);
        }
    }

    /** 502: falló un servicio externo (n8n, embeddings, LLM). */
    public static class Integracion extends RuntimeException {
        public Integracion(String mensaje, Throwable causa) {
            super(mensaje, causa);
        }
    }
}
