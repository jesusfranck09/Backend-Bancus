package com.bancup.auth.mapper;

import com.bancup.auth.dto.response.SignupResponse;
import com.bancup.entity.Cuenta;
import com.bancup.entity.Usuario;
import org.springframework.stereotype.Component;

/**
 * Mapper manual de entidades de usuario a DTOs de respuesta.
 * No usa MapStruct para mantener el stack simple.
 * Se puede reemplazar por MapStruct en fases posteriores si la complejidad lo justifica.
 */
@Component
public class UsuarioMapper {

    /**
     * Convierte un Usuario y su Cuenta base al DTO de respuesta del signup.
     * Garantiza que ningun dato sensible sea incluido en la respuesta.
     *
     * @param usuario Usuario recien creado (con rol cargado)
     * @param cuenta  Cuenta base creada para el usuario
     * @return SignupResponse listo para el frontend
     */
    public SignupResponse toSignupResponse(Usuario usuario, Cuenta cuenta) {
        return SignupResponse.builder()
                .publicId(usuario.getPublicId())
                .email(usuario.getEmail())
                .nombre(usuario.getNombre())
                .apellidoPaterno(usuario.getApellidoPaterno())
                .apellidoMaterno(usuario.getApellidoMaterno())
                .rol(usuario.getRol() != null ? usuario.getRol().getNombrePerfil() : null)
                .kycStatus(usuario.getKycStatus())
                .idCuenta(cuenta != null ? cuenta.getIdCuenta() : null)
                .tipoCuenta(cuenta != null ? cuenta.getTipoCuenta() : null)
                .build();
    }
}
