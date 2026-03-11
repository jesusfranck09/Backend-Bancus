package com.bancup.auth.controller;

import com.bancup.auth.dto.request.LoginRequest;
import com.bancup.auth.dto.request.SignupRequest;
import com.bancup.auth.dto.response.LoginResponse;
import com.bancup.auth.dto.response.SignupResponse;
import com.bancup.auth.service.AuthService;
import com.bancup.repository.UsuarioRepository;
import com.bancup.common.dto.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Controlador de autenticacion.
 *
 * Fase 1: POST /auth/signup
 * Fase 2 (pendiente): POST /auth/login, POST /auth/refresh-token, POST /auth/logout
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;
    private final UsuarioRepository usuarioRepository;

    /**
     * POST /auth/signup
     *
     * Registra un nuevo usuario en el sistema Bancup.
     * Crea la cuenta base con saldo 0 y registra el evento de auditoria.
     *
     * @param request     Body con los datos del nuevo usuario
     * @param httpRequest Request HTTP para extraer la IP de origen
     * @return HTTP 201 Created con datos no sensibles del usuario creado
     */
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<SignupResponse>> signup(
            @Valid @RequestBody SignupRequest request,
            HttpServletRequest httpRequest) {

        String ipOrigen = httpRequest.getRemoteAddr();
        log.info("POST /auth/signup recibido desde IP: {}", ipOrigen);

        SignupResponse response = authService.signup(request, ipOrigen);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Usuario registrado exitosamente", response));
    }

    /**
     * POST /auth/login
     *
     * Autentica un usuario existente y devuelve un JWT Bearer token.
     *
     * @param request     Body con email y password
     * @param httpRequest Request HTTP para extraer la IP de origen
     * @return HTTP 200 OK con token JWT y datos basicos del usuario
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {

        String ipOrigen = httpRequest.getRemoteAddr();
        log.info("POST /auth/login recibido desde IP: {}", ipOrigen);

        LoginResponse response = authService.login(request, ipOrigen);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/test-db")
    public String testDb() {
        usuarioRepository.count();
        return "Conexion a Oracle OK";
    }

    // TODO Fase 3: POST /auth/refresh-token
    // TODO Fase 3: POST /auth/logout
}