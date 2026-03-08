package com.bancup.exception;

/**
 * Se lanza cuando un recurso requerido no existe en la base de datos.
 * Ejemplos: rol no encontrado, genero inexistente, entidad/estado inexistente.
 * Mapea a HTTP 404 Not Found.
 */
public class ResourceNotFoundException extends BancupException {

    public ResourceNotFoundException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
