package com.aegisops.backend.domain;

/**
 * Enumeraciones del dominio. Se guardan como texto (EnumType.STRING) y la base
 * de datos las restringe con CHECK constraints (ver database/01_schema.sql).
 */
public final class Enums {

    private Enums() {
    }

    public enum Prioridad { BAJA, MEDIA, ALTA, CRITICA }

    public enum EstadoIncidente {
        ABIERTO, EN_PROGRESO, RESUELTO, CERRADO;

        public boolean esFinal() {
            return this == RESUELTO || this == CERRADO;
        }
    }

    public enum EstadoArticulo { BORRADOR, ACTIVO, INACTIVO }

    public enum EstadoConsulta { PENDIENTE, RESPONDIDA, SIN_RESULTADOS, ERROR }
}
