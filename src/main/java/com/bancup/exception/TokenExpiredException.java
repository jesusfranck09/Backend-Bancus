package com.bancup.exception;

/**
 * Se lanza cuando el refresh token recibido ha expirado, no existe
 * en base de datos o ya fue utilizado/revocado.
 * Mapea a HTTP 401 Unauthorized.
 */
public class TokenExpiredException extends BancupException {

    public TokenExpiredException() {
        super(ErrorCode.TOKEN_EXPIRADO, "El token ha expirado o no es valido. Inicia sesion nuevamente.");
    }

    public TokenExpiredException(String message) {
        super(ErrorCode.TOKEN_EXPIRADO, message);
    }
}
