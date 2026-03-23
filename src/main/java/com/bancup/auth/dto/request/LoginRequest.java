package com.bancup.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Request para el endpoint POST /auth/login.
 */
@Data
public class LoginRequest {

    @NotBlank(message = "El correo es obligatorio")
    @Email(message = "El formato del correo no es valido")
    private String correo;

    @NotBlank(message = "La contrasena es obligatoria")
    private String contrasena;
}
