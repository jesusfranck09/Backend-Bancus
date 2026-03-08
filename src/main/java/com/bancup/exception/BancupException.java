package com.bancup.exception;

import lombok.Getter;

/**
 * Excepcion base del sistema Bancup.
 * Todas las excepciones de negocio deben extender esta clase.
 */
@Getter
public class BancupException extends RuntimeException {

    private final ErrorCode errorCode;

    public BancupException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public BancupException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
}
