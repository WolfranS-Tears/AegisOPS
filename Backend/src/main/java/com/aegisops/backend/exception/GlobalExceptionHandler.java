package com.aegisops.backend.exception;

import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/** Respuestas de error uniformes en formato RFC 9457 (ProblemDetail). */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail validacion(MethodArgumentNotValidException ex) {
        Map<String, String> errores = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(e -> errores.putIfAbsent(e.getField(), e.getDefaultMessage()));
        ProblemDetail pd = problema(HttpStatus.BAD_REQUEST, "Datos inválidos", "Revise los campos enviados");
        pd.setProperty("errores", errores);
        return pd;
    }

    @ExceptionHandler(Excepciones.NoEncontrado.class)
    ProblemDetail noEncontrado(Excepciones.NoEncontrado ex) {
        return problema(HttpStatus.NOT_FOUND, "No encontrado", ex.getMessage());
    }

    @ExceptionHandler({Excepciones.ReglaNegocio.class, IllegalArgumentException.class})
    ProblemDetail reglaNegocio(RuntimeException ex) {
        return problema(HttpStatus.BAD_REQUEST, "Solicitud no válida", ex.getMessage());
    }

    @ExceptionHandler({MethodArgumentTypeMismatchException.class, HttpMessageNotReadableException.class})
    ProblemDetail formato(Exception ex) {
        return problema(HttpStatus.BAD_REQUEST, "Formato inválido",
                "Uno de los valores enviados no tiene el formato esperado");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail integridad(DataIntegrityViolationException ex) {
        log.warn("Violación de integridad: {}", ex.getMostSpecificCause().getMessage());
        return problema(HttpStatus.CONFLICT, "Conflicto de datos",
                "La operación viola una restricción de integridad (registro duplicado o referencia inválida)");
    }

    @ExceptionHandler(Excepciones.Integracion.class)
    ProblemDetail integracion(Excepciones.Integracion ex) {
        log.error("Fallo de integración: {}", ex.getMessage());
        return problema(HttpStatus.BAD_GATEWAY, "Servicio externo no disponible", ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail general(Exception ex) {
        log.error("Error no controlado", ex);
        return problema(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno", "Ocurrió un error inesperado");
    }

    private static ProblemDetail problema(HttpStatus status, String titulo, String detalle) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detalle);
        pd.setTitle(titulo);
        return pd;
    }
}
