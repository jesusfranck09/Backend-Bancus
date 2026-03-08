package com.bancup.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Request para el endpoint POST /auth/logout.
 * El refresh token actua como credencial para invalidar la sesion.
 */
@Data
public class LogoutRequest {

    @NotBlank(message = "El refresh token es obligatorio")
    private String refreshToken;
}
