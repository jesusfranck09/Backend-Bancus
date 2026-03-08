package com.bancup.auth.dto.response;

import lombok.Builder;
import lombok.Data;

/**
 * Respuesta del endpoint POST /auth/login.
 *
 * En caso de exito:  success=true, message, token JWT, userId (publicId), email.
 * En caso de error: el GlobalExceptionHandler devuelve ApiResponse con errorCode.
 */
@Data
@Builder
public class LoginResponse {

    private boolean success;
    private String message;

    /** JWT Bearer token. Valido por 24 horas. */
    private String token;

    /** Identificador publico del usuario (UUID). Nunca el ID interno. */
    private String userId;

    private String email;
}
