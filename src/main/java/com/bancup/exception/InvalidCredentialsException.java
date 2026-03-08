package com.bancup.exception;

/**
 * Se lanza cuando las credenciales del usuario son incorrectas durante el login.
 * Cubre: usuario no encontrado o password incorrecto.
 * Se devuelve el mismo mensaje generico en ambos casos por seguridad
 * (no se revela si el email existe o no en el sistema).
 * Mapea a HTTP 401 Unauthorized.
 */
public class InvalidCredentialsException extends BancupException {

    public InvalidCredentialsException() {
        super(ErrorCode.CREDENCIALES_INVALIDAS, "Email o contrasena incorrectos");
    }

    public InvalidCredentialsException(String message) {
        super(ErrorCode.CREDENCIALES_INVALIDAS, message);
    }
}
