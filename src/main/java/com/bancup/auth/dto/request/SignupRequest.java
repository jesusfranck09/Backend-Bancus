package com.bancup.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

/**
 * Request para el endpoint POST /auth/signup.
 *
 * Campos obligatorios: email, password, nombre, apellidoPaterno.
 * Campos opcionales: apellidoMaterno, telefono, curp, fechaNacimiento,
 *                    generoId, entidadId, idRol, deviceTipo, deviceIp.
 */
@Data
public class SignupRequest {

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El formato del email no es valido")
    private String email;

    @NotBlank(message = "La contrasena es obligatoria")
    @Size(min = 8, message = "La contrasena debe tener al menos 8 caracteres")
    private String password;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 100, message = "El nombre no debe exceder 100 caracteres")
    private String nombre;

    @NotBlank(message = "El apellido paterno es obligatorio")
    @Size(max = 100, message = "El apellido paterno no debe exceder 100 caracteres")
    private String apellidoPaterno;

    @Size(max = 100, message = "El apellido materno no debe exceder 100 caracteres")
    private String apellidoMaterno;

    @Size(max = 20, message = "El telefono no debe exceder 20 caracteres")
    private String telefono;

    /**
     * CURP: si se envia debe tener exactamente 18 caracteres.
     * Si se envia, se valida unicidad contra la tabla USUARIOS.
     */
    @Size(min = 18, max = 18, message = "La CURP debe tener exactamente 18 caracteres")
    private String curp;

    /**
     * Formato esperado: "yyyy-MM-dd" (ISO 8601).
     * Jackson lo deserializa correctamente con write-dates-as-timestamps=false.
     */
    private LocalDate fechaNacimiento;

    /**
     * FK a CAT_GENERO.GENERO_ID. Opcional.
     * Si se envia, se valida existencia en el catalogo.
     */
    private Long generoId;

    /**
     * FK a CAT_ESTADOS.ENTIDAD_ID. Opcional.
     * Si se envia, se valida existencia en el catalogo.
     */
    private Long entidadId;

    /**
     * FK a ROLES.ID_ROL. Opcional.
     * Si no se envia, se asigna el rol configurado en bancup.auth.default-role (CLIENTE).
     * Si se envia, se valida existencia en la tabla ROLES.
     */
    private Long idRol;

    /**
     * Tipo de dispositivo desde el cual se realiza el registro.
     * Ej: "MOVIL", "WEB", "TABLET".
     * Si se envia (junto con deviceIp o solo), se registra en DISPOSITIVOS.
     */
    @Size(max = 50, message = "El tipo de dispositivo no debe exceder 50 caracteres")
    private String deviceTipo;

    /**
     * IP del dispositivo. Si se envia (junto con deviceTipo o solo), se registra en DISPOSITIVOS.
     */
    @Size(max = 50, message = "La IP del dispositivo no debe exceder 50 caracteres")
    private String deviceIp;
}
