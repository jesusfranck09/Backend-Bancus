package com.bancup.auth.dto.response;

import lombok.Builder;
import lombok.Data;

/**
 * Respuesta del endpoint POST /auth/signup.
 *
 * Solo contiene informacion no sensible, lista para el frontend.
 * Nunca incluye: password_hash, password_2hash, id interno, ni datos criticos.
 */
@Data
@Builder
public class SignupResponse {

    private String correo;
    private String usuario;

    /** Nombre del perfil/rol asignado. Ej: "CLIENTE". */
    private String rol;
}
