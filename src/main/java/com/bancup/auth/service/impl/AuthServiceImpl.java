package com.bancup.auth.service.impl;

import com.bancup.auth.dto.request.LoginRequest;
import com.bancup.auth.dto.request.SignupRequest;
import com.bancup.auth.dto.response.LoginResponse;
import com.bancup.auth.dto.response.SignupResponse;
import com.bancup.auth.mapper.UsuarioMapper;
import com.bancup.auth.service.AuthService;
import com.bancup.entity.*;
import com.bancup.exception.DuplicateResourceException;
import com.bancup.exception.ErrorCode;
import com.bancup.exception.AccountLockedException;
import com.bancup.exception.InvalidCredentialsException;
import com.bancup.exception.ResourceNotFoundException;
import com.bancup.repository.*;
import com.bancup.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final AuditoriaEventoRepository auditoriaEventoRepository;
    private final CatGeneroRepository catGeneroRepository;
    private final PasswordEncoder passwordEncoder;
    private final UsuarioMapper usuarioMapper;
    private final JwtService jwtService;

    /** Nombre del rol por defecto. Configurable en application.properties. */
    @Value("${bancup.auth.default-role}")
    private String rolDefecto;

    @Override
    @Transactional
    public SignupResponse signup(SignupRequest request, String ipOrigen) {
        log.info("Iniciando signup para correo: {}", request.getCorreo());

        // 1. Validar unicidad de correo (case-insensitive: se normaliza a minusculas)
        String emailNormalizado = request.getCorreo().toLowerCase().trim();
        if (usuarioRepository.existsByEmail(emailNormalizado)) {
            throw new DuplicateResourceException(
                    ErrorCode.EMAIL_DUPLICADO,
                    "El correo '" + emailNormalizado + "' ya esta registrado"
            );
        }

        // 2. Resolver rol por defecto desde ROLES
        Rol rol = resolverRol();

        // 3. Validar genero obligatorio en CAT_GENERO
        CatGenero genero = catGeneroRepository.findById(request.getGenero())
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.GENERO_NO_ENCONTRADO,
                        "El genero con id " + request.getGenero() + " no existe en la tabla CAT_GENERO"
                ));

        // 4. Hash del password con BCrypt (strength 12). Nunca texto plano.
        String passwordHash = passwordEncoder.encode(request.getContrasena());

        String usuarioNormalizado = request.getUsuario().trim();

        // 5. Construir y persistir el usuario.
        //    Se inicializan los metadatos requeridos por el esquema APP_USER.
        LocalDateTime ahora = LocalDateTime.now();
        Usuario usuario = Usuario.builder()
                .rol(rol)
                .genero(genero)
                .nombre(usuarioNormalizado)
                .email(emailNormalizado)
                .passwordHash(passwordHash)
                .failedLoginAttempts(0)
                .accountLocked(false)
                .ultimoLogin(null)
                .fechaRegistro(ahora)
                .estatus(1)
                .fechaCrea(ahora)
                .usuarioCrea("AUTH_API")
                .fechaModifica(ahora)
                .usuarioModifica("AUTH_API")
                .build();

        usuario = usuarioRepository.save(usuario);
        log.info("Usuario creado con id_usuario={}", usuario.getIdUsuario());

        // 6. Registrar evento de auditoria: accion SIGNUP
        AuditoriaEvento auditoria = AuditoriaEvento.builder()
                .usuario(usuario)
                .accion("SIGNUP")
                .correo(emailNormalizado)
                .fechaEvento(ahora)
                .build();
        auditoriaEventoRepository.save(auditoria);
        log.debug("Evento de auditoria SIGNUP registrado para usuario id={}", usuario.getIdUsuario());

        log.info("Signup completado exitosamente para correo={}", emailNormalizado);
        return usuarioMapper.toSignupResponse(usuario);
    }

    @Override
    @Transactional
    public LoginResponse login(LoginRequest request, String ipOrigen) {
        String emailNormalizado = request.getCorreo().toLowerCase().trim();
        log.info("Iniciando login para correo: {}", emailNormalizado);

        // 1. Buscar usuario por correo. Se lanza excepcion generica para no revelar
        //    si el correo existe o no (prevencion de enumeracion de usuarios).
        Usuario usuario = usuarioRepository.findByEmail(emailNormalizado)
                .orElseThrow(InvalidCredentialsException::new);

        if (Boolean.TRUE.equals(usuario.getAccountLocked())) {
            throw new AccountLockedException();
        }

        // 2. Validar password con BCrypt
        if (!passwordEncoder.matches(request.getContrasena(), usuario.getPasswordHash())) {
            log.warn("Contrasena incorrecta para correo: {}", emailNormalizado);
            boolean cuentaBloqueada = registrarIntentoFallido(usuario);
            if (cuentaBloqueada) {
                throw new AccountLockedException();
            }
            throw new InvalidCredentialsException();
        }

        // 3. Actualizar metadata de login y generar JWT con el ID interno del usuario.
        LocalDateTime ahora = LocalDateTime.now();
        usuario.setFailedLoginAttempts(0);
        usuario.setAccountLocked(false);
        usuario.setUltimoLogin(ahora);
        usuario.setFechaModifica(ahora);
        usuario.setUsuarioModifica("AUTH_API");
        usuarioRepository.save(usuario);

        String rol = usuario.getRol() != null ? usuario.getRol().getNombrePerfil() : "CLIENTE";
        String token = jwtService.generateToken(String.valueOf(usuario.getIdUsuario()), emailNormalizado, rol);

        // 4. Registrar evento de auditoria: accion LOGIN
        AuditoriaEvento auditoria = AuditoriaEvento.builder()
                .usuario(usuario)
                .accion("LOGIN")
                .correo(emailNormalizado)
                .fechaEvento(ahora)
                .build();
        auditoriaEventoRepository.save(auditoria);

        log.info("Login exitoso para usuario id={}", usuario.getIdUsuario());

        return LoginResponse.builder()
                .success(true)
                .message("Autenticacion exitosa")
                .token(token)
                .nombre(usuario.getNombre())
                .build();
    }

    // =========================================================================
    // Metodos privados de soporte
    // =========================================================================

    /**
     * Resuelve el rol a asignar al usuario.
     *
     * Busca el rol por defecto configurado en bancup.auth.default-role.
     *
     * IMPORTANTE: Si el rol por defecto no existe en la tabla ROLES, se lanza
     * ResourceNotFoundException con ErrorCode.ROL_CLIENTE_NO_CONFIGURADO.
     * Esto indica que los catalogos base no han sido cargados en la BD.
     * NO se inventa ni se crea el rol silenciosamente.
     */
    private Rol resolverRol() {
        return rolRepository.findByNombrePerfil(rolDefecto)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.ROL_CLIENTE_NO_CONFIGURADO,
                        "El rol por defecto '" + rolDefecto + "' no existe en la tabla ROLES. " +
                        "Es necesario cargar los catalogos base (INSERT en ROLES con nombre_perfil='" + rolDefecto + "') " +
                        "antes de realizar el primer signup."
                ));
    }

    private boolean registrarIntentoFallido(Usuario usuario) {
        int intentos = usuario.getFailedLoginAttempts() != null ? usuario.getFailedLoginAttempts() + 1 : 1;
        usuario.setFailedLoginAttempts(intentos);
        usuario.setAccountLocked(intentos >= 5);
        usuario.setFechaModifica(LocalDateTime.now());
        usuario.setUsuarioModifica("AUTH_API");
        usuarioRepository.save(usuario);
        return Boolean.TRUE.equals(usuario.getAccountLocked());
    }
}
