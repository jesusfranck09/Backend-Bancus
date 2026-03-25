package com.bancup.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class VerifySignupCodeRequest {

    @NotBlank(message = "El correo es obligatorio")
    @Email(message = "El formato del correo no es valido")
    private String correo;

    @NotBlank(message = "El codigo es obligatorio")
    @Pattern(regexp = "\\d{6}", message = "El codigo debe tener exactamente 6 digitos")
    private String codigo;

    public void setCorreo(String correo) {
        this.correo = correo != null ? correo.trim() : null;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo != null ? codigo.trim() : null;
    }
}
