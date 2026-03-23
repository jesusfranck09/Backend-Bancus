package com.bancup.auth.service;

import com.bancup.auth.dto.request.LoginRequest;
import com.bancup.auth.dto.request.SignupRequest;
import com.bancup.auth.dto.response.LoginResponse;
import com.bancup.auth.dto.response.SignupResponse;

/**
 * Contrato del servicio de autenticacion Bancup.
 *
 * Fase 1: signup.
 * Fase 2: login con JWT.
 */
public interface AuthService {

    /**
     * Registra un nuevo usuario en el sistema.
     * Crea el usuario usando el contrato minimo de registro y registra el evento de auditoria.
     *
     * @param request   Datos del registro
     * @param ipOrigen  IP desde donde se realiza el registro (para auditoria)
     * @return          Datos no sensibles del usuario creado
     */
    SignupResponse signup(SignupRequest request, String ipOrigen);

    /**
     * Autentica a un usuario existente y genera un JWT.
     *
     * @param request   Correo y contrasena del usuario
     * @param ipOrigen  IP de origen (para auditoria)
     * @return          LoginResponse con token JWT y nombre del usuario
     */
    LoginResponse login(LoginRequest request, String ipOrigen);

    // TODO Fase 3: TokenRefreshResponse refreshToken(String refreshToken);
    // TODO Fase 3: void logout(String publicId);
}
