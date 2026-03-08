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

    /** Identificador publico del usuario (UUID). Usar este en lugar del ID interno. */
    private String publicId;

    private String email;
    private String nombre;
    private String apellidoPaterno;
    private String apellidoMaterno;

    /** Nombre del perfil/rol asignado. Ej: "CLIENTE". */
    private String rol;

    /** Estado KYC inicial. Siempre "PENDIENTE" en el signup. */
    private String kycStatus;

    /** ID de la cuenta base creada automaticamente. */
    private Long idCuenta;

    /** Tipo de cuenta creada. Ej: "AHORRO". */
    private String tipoCuenta;
}
