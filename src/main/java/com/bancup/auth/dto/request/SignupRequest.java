package com.bancup.auth.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Request para el endpoint POST /auth/signup.
 *
 * Campos obligatorios: usuario, correo, contrasena, confirmarContrasena, genero.
 */
@Data
public class SignupRequest {

    @NotBlank(message = "El usuario es obligatorio")
    @Size(max = 100, message = "El usuario no debe exceder 100 caracteres")
    private String usuario;

    @NotBlank(message = "El correo es obligatorio")
    @Email(message = "El formato del correo no es valido")
    private String correo;

    @NotBlank(message = "La contrasena es obligatoria")
    @Size(min = 8, message = "La contrasena debe tener al menos 8 caracteres")
    private String contrasena;

    @NotBlank(message = "La confirmacion de contrasena es obligatoria")
    private String confirmarContrasena;

    /**
     * FK a CAT_GENERO.GENERO_ID.
     */
    @NotNull(message = "El genero es obligatorio")
    private Long genero;

    @AssertTrue(message = "La contrasena y su confirmacion no coinciden")
    public boolean isConfirmacionContrasenaValida() {
        return contrasena != null && contrasena.equals(confirmarContrasena);
    }
}
