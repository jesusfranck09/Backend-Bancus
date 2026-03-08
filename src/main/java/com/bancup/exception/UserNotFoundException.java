package com.bancup.exception;

/**
 * Se lanza cuando se busca un usuario por publicId u otro identificador
 * y no existe en la base de datos.
 * Mapea a HTTP 404 Not Found.
 *
 * NOTA: En el flujo de LOGIN no se usa esta excepcion por seguridad
 * (se usa InvalidCredentialsException para no revelar si el email existe).
 * Esta excepcion es para operaciones internas o de administracion.
 */
public class UserNotFoundException extends BancupException {

    public UserNotFoundException(String identifier) {
        super(ErrorCode.USUARIO_NO_ENCONTRADO,
                "Usuario no encontrado: " + identifier);
    }
}
