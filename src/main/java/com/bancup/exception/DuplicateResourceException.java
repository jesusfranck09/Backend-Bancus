package com.bancup.exception;

/**
 * Se lanza cuando se intenta registrar un recurso que ya existe.
 * Ejemplos: email duplicado, CURP duplicada.
 * Mapea a HTTP 409 Conflict.
 */
public class DuplicateResourceException extends BancupException {

    public DuplicateResourceException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
