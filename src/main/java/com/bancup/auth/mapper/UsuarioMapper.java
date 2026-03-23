package com.bancup.auth.mapper;

import com.bancup.auth.dto.response.SignupResponse;
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
     * Convierte un Usuario al DTO de respuesta del signup.
     * Garantiza que ningun dato sensible sea incluido en la respuesta.
     *
     * @param usuario Usuario recien creado (con rol cargado)
     * @return SignupResponse listo para el frontend
     */
    public SignupResponse toSignupResponse(Usuario usuario) {
        return SignupResponse.builder()
                .correo(usuario.getEmail())
                .usuario(usuario.getNombre())
                .rol(usuario.getRol() != null ? usuario.getRol().getNombrePerfil() : null)
                .build();
    }
}
