package com.bancup.auth.service.impl;

import com.bancup.auth.dto.request.LoginRequest;
import com.bancup.auth.dto.request.RequestSignupCodeRequest;
import com.bancup.auth.dto.request.SignupRequest;
import com.bancup.auth.dto.request.VerifySignupCodeRequest;
import com.bancup.auth.dto.response.LoginResponse;
import com.bancup.auth.dto.response.RequestSignupCodeResponse;
import com.bancup.auth.dto.response.SignupResponse;
import com.bancup.auth.dto.response.VerifySignupCodeResponse;
import com.bancup.auth.mapper.UsuarioMapper;
import com.bancup.auth.service.AuthService;
import com.bancup.auth.service.VerificationCodeDeliveryService;
import com.bancup.entity.*;
import com.bancup.exception.BancupException;
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

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.security.SecureRandom;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private static final String USUARIO_SISTEMA = "AUTH_API";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final AuditoriaEventoRepository auditoriaEventoRepository;
    private final CatGeneroRepository catGeneroRepository;
    private final CodigoVerificacionCorreoRepository codigoVerificacionCorreoRepository;
    private final PasswordEncoder passwordEncoder;
    private final UsuarioMapper usuarioMapper;
    private final JwtService jwtService;
    private final VerificationCodeDeliveryService verificationCodeDeliveryService;

    /** Nombre del rol por defecto. Configurable en application.properties. */
    @Value("${bancup.auth.default-role}")
    private String rolDefecto;

    @Value("${bancup.verification.code-expiration-minutes:10}")
    private long verificationCodeExpirationMinutes;

    @Value("${bancup.verification.max-attempts:5}")
    private int maxVerificationAttempts;

    @Value("${bancup.verification.max-resends:3}")
    private int maxVerificationResends;

    @Value("${bancup.verification.return-code-in-response:${bancup.verification.debug-return-code:false}}")
    private boolean returnCodeInResponse;

    @Value("${bancup.signup.verification-token-expiration-ms:900000}")
    private long signupVerificationExpirationMs;

    @Override
    @Transactional
    public RequestSignupCodeResponse requestSignupCode(RequestSignupCodeRequest request, String ipOrigen) {
        String emailNormalizado = normalizeEmail(request.getCorreo());
        log.info("Generando codigo de verificacion para correo: {}", emailNormalizado);

        if (usuarioRepository.existsByEmail(emailNormalizado)) {
            throw new DuplicateResourceException(
                    ErrorCode.EMAIL_DUPLICADO,
                    "El correo '" + emailNormalizado + "' ya esta registrado"
            );
        }

        LocalDateTime ahora = LocalDateTime.now();
        Optional<CodigoVerificacionCorreo> ultimoCodigo = codigoVerificacionCorreoRepository
                .findTopByCorreoAndTipoOrderByFechaCreaDesc(emailNormalizado, TipoCodigoVerificacion.SIGNUP);

        int reenvios = calculateResends(ultimoCodigo.orElse(null), ahora);
        if (reenvios > maxVerificationResends) {
            throw new BancupException(
                    ErrorCode.LIMITE_REENVIOS_ALCANZADO,
                    "Se alcanzo el limite de reenvios para este correo. Espera a que expire el codigo actual o intenta mas tarde."
            );
        }

        invalidateActiveSignupCodes(emailNormalizado, ahora);

        String codigoPlano = generateVerificationCode();
        LocalDateTime fechaExpiracion = ahora.plusMinutes(verificationCodeExpirationMinutes);

        CodigoVerificacionCorreo codigo = CodigoVerificacionCorreo.builder()
                .correo(emailNormalizado)
                .codigoHash(passwordEncoder.encode(codigoPlano))
                .tipo(TipoCodigoVerificacion.SIGNUP)
                .estatus(EstatusCodigoVerificacion.PENDIENTE)
                .intentosFallidos(0)
                .reenvios(reenvios)
                .fechaExpiracion(fechaExpiracion)
                .ipOrigen(ipOrigen)
                .fechaCrea(ahora)
                .usuarioCrea(USUARIO_SISTEMA)
                .fechaModifica(ahora)
                .usuarioModifica(USUARIO_SISTEMA)
                .build();
        codigoVerificacionCorreoRepository.save(codigo);

        verificationCodeDeliveryService.sendSignupCode(emailNormalizado, codigoPlano, fechaExpiracion);

        return RequestSignupCodeResponse.builder()
                .correo(emailNormalizado)
                .fechaExpiracion(fechaExpiracion)
                .codigo(returnCodeInResponse ? codigoPlano : null)
                .codigoDebug(returnCodeInResponse ? codigoPlano : null)
                .build();
    }

    @Override
    @Transactional(noRollbackFor = BancupException.class)
    public VerifySignupCodeResponse verifySignupCode(VerifySignupCodeRequest request, String ipOrigen) {
        String emailNormalizado = normalizeEmail(request.getCorreo());
        String codigoPlano = request.getCodigo().trim();
        LocalDateTime ahora = LocalDateTime.now();

        CodigoVerificacionCorreo codigo = codigoVerificacionCorreoRepository
                .findTopByCorreoAndTipoOrderByFechaCreaDesc(emailNormalizado, TipoCodigoVerificacion.SIGNUP)
                .orElseThrow(() -> new BancupException(
                        ErrorCode.CODIGO_VERIFICACION_INVALIDO,
                        "El codigo de verificacion es invalido o ya no esta disponible."
                ));

        if (isExpired(codigo, ahora)) {
            markAsExpired(codigo, ahora);
            throw new BancupException(
                    ErrorCode.CODIGO_VERIFICACION_EXPIRADO,
                    "El codigo de verificacion expiro. Solicita uno nuevo."
            );
        }

        if (codigo.getEstatus() == EstatusCodigoVerificacion.BLOQUEADO) {
            throw new BancupException(
                    ErrorCode.LIMITE_INTENTOS_VERIFICACION_ALCANZADO,
                    "Se alcanzo el limite de intentos para este codigo. Solicita uno nuevo."
            );
        }

        if (codigo.getEstatus() == EstatusCodigoVerificacion.CANCELADO) {
            throw new BancupException(
                    ErrorCode.CODIGO_VERIFICACION_INVALIDO,
                    "El codigo de verificacion es invalido o ya no esta disponible."
            );
        }

        if (!passwordEncoder.matches(codigoPlano, codigo.getCodigoHash())) {
            if (codigo.getEstatus() != EstatusCodigoVerificacion.PENDIENTE) {
                throw new BancupException(
                        ErrorCode.CODIGO_VERIFICACION_INVALIDO,
                        "El codigo de verificacion es invalido o ya no esta disponible."
                );
            }

            int intentos = codigo.getIntentosFallidos() != null ? codigo.getIntentosFallidos() + 1 : 1;
            codigo.setIntentosFallidos(intentos);
            codigo.setFechaModifica(ahora);
            codigo.setUsuarioModifica(USUARIO_SISTEMA);

            if (intentos >= maxVerificationAttempts) {
                codigo.setEstatus(EstatusCodigoVerificacion.BLOQUEADO);
                codigoVerificacionCorreoRepository.save(codigo);
                throw new BancupException(
                        ErrorCode.LIMITE_INTENTOS_VERIFICACION_ALCANZADO,
                        "Se alcanzo el limite de intentos para este codigo. Solicita uno nuevo."
                );
            }

            codigoVerificacionCorreoRepository.save(codigo);
            throw new BancupException(
                    ErrorCode.CODIGO_VERIFICACION_INVALIDO,
                    "El codigo de verificacion es incorrecto."
            );
        }

        if (codigo.getEstatus() == EstatusCodigoVerificacion.PENDIENTE) {
            codigo.setEstatus(EstatusCodigoVerificacion.VERIFICADO);
            codigo.setFechaVerificado(ahora);
            // Una vez validado el codigo, la ventana para completar signup queda
            // alineada con la vigencia del verificationToken temporal.
            codigo.setFechaExpiracion(ahora.plus(Duration.ofMillis(signupVerificationExpirationMs)));
            codigo.setIpOrigen(ipOrigen);
            codigo.setFechaModifica(ahora);
            codigo.setUsuarioModifica(USUARIO_SISTEMA);
            codigoVerificacionCorreoRepository.save(codigo);
        }

        String verificationToken = jwtService.generateSignupVerificationToken(emailNormalizado);

        return VerifySignupCodeResponse.builder()
                .correo(emailNormalizado)
                .verificationToken(verificationToken)
                .build();
    }

    @Override
    @Transactional(noRollbackFor = BancupException.class)
    public SignupResponse signup(SignupRequest request, String ipOrigen) {
        String emailNormalizado = normalizeEmail(request.getCorreo());
        log.info("Iniciando signup para correo: {}", emailNormalizado);

        // 1. Validar unicidad de correo
        if (usuarioRepository.existsByEmail(emailNormalizado)) {
            throw new DuplicateResourceException(
                    ErrorCode.EMAIL_DUPLICADO,
                    "El correo '" + emailNormalizado + "' ya esta registrado"
            );
        }

        // 2. Validar token temporal y estado verificado del correo.
        if (!jwtService.validateSignupVerificationToken(request.getVerificationToken(), emailNormalizado)) {
            throw new BancupException(
                    ErrorCode.TOKEN_VERIFICACION_INVALIDO,
                    "El verificationToken es invalido o ya expiro. Verifica tu correo nuevamente."
            );
        }

        LocalDateTime ahora = LocalDateTime.now();
        CodigoVerificacionCorreo verificacion = resolveVerifiedSignupCode(emailNormalizado, ahora);

        // 3. Resolver rol por defecto desde ROLES
        Rol rol = resolverRol();

        // 4. Validar genero obligatorio en CAT_GENERO
        CatGenero genero = catGeneroRepository.findById(request.getGenero())
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.GENERO_NO_ENCONTRADO,
                        "El genero con id " + request.getGenero() + " no existe en la tabla CAT_GENERO"
                ));

        // 5. Hash del password con BCrypt (strength 12). Nunca texto plano.
        String passwordHash = passwordEncoder.encode(request.getContrasena());

        String usuarioNormalizado = request.getUsuario().trim();

        // 6. Construir y persistir el usuario.
        //    Se inicializan los metadatos requeridos por el esquema APP_USER.
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
                .usuarioCrea(USUARIO_SISTEMA)
                .fechaModifica(ahora)
                .usuarioModifica(USUARIO_SISTEMA)
                .build();

        usuario = usuarioRepository.save(usuario);
        log.info("Usuario creado con id_usuario={}", usuario.getIdUsuario());

        // 7. Registrar evento de auditoria: accion SIGNUP
        AuditoriaEvento auditoria = AuditoriaEvento.builder()
                .usuario(usuario)
                .accion("SIGNUP")
                .ipOrigen(ipOrigen)
                .fechaEvento(ahora)
                .build();
        auditoriaEventoRepository.save(auditoria);
        log.debug("Evento de auditoria SIGNUP registrado para usuario id={}", usuario.getIdUsuario());

        // 8. Consumir la verificacion para que el flujo de alta sea de un solo uso.
        verificacion.setEstatus(EstatusCodigoVerificacion.CANCELADO);
        verificacion.setFechaModifica(ahora);
        verificacion.setUsuarioModifica(USUARIO_SISTEMA);
        codigoVerificacionCorreoRepository.save(verificacion);

        log.info("Signup completado exitosamente para correo={}", emailNormalizado);
        return usuarioMapper.toSignupResponse(usuario);
    }

    @Override
    @Transactional(noRollbackFor = BancupException.class)
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
        usuario.setUsuarioModifica(USUARIO_SISTEMA);
        usuarioRepository.save(usuario);

        String rol = usuario.getRol() != null ? usuario.getRol().getNombrePerfil() : "CLIENTE";
        String token = jwtService.generateToken(String.valueOf(usuario.getIdUsuario()), emailNormalizado, rol);

        // 4. Registrar evento de auditoria: accion LOGIN
        AuditoriaEvento auditoria = AuditoriaEvento.builder()
                .usuario(usuario)
                .accion("LOGIN")
                .ipOrigen(ipOrigen)
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
        usuario.setUsuarioModifica(USUARIO_SISTEMA);
        usuarioRepository.save(usuario);
        return Boolean.TRUE.equals(usuario.getAccountLocked());
    }

    private CodigoVerificacionCorreo resolveVerifiedSignupCode(String correo, LocalDateTime ahora) {
        CodigoVerificacionCorreo verificacion = codigoVerificacionCorreoRepository
                .findTopByCorreoAndTipoAndEstatusOrderByFechaCreaDesc(
                        correo,
                        TipoCodigoVerificacion.SIGNUP,
                        EstatusCodigoVerificacion.VERIFICADO
                )
                .orElseThrow(() -> new BancupException(
                        ErrorCode.CORREO_NO_VERIFICADO,
                        "El correo aun no ha sido verificado para completar el registro."
                ));

        if (isExpired(verificacion, ahora)) {
            markAsExpired(verificacion, ahora);
            throw new BancupException(
                    ErrorCode.CODIGO_VERIFICACION_EXPIRADO,
                    "La verificacion del correo expiro. Solicita un codigo nuevo."
            );
        }

        return verificacion;
    }

    private void invalidateActiveSignupCodes(String correo, LocalDateTime ahora) {
        List<CodigoVerificacionCorreo> activos = codigoVerificacionCorreoRepository.findByCorreoAndTipoAndEstatusIn(
                correo,
                TipoCodigoVerificacion.SIGNUP,
                List.of(EstatusCodigoVerificacion.PENDIENTE, EstatusCodigoVerificacion.VERIFICADO)
        );

        for (CodigoVerificacionCorreo codigo : activos) {
            codigo.setEstatus(isExpired(codigo, ahora)
                    ? EstatusCodigoVerificacion.EXPIRADO
                    : EstatusCodigoVerificacion.CANCELADO);
            codigo.setFechaModifica(ahora);
            codigo.setUsuarioModifica(USUARIO_SISTEMA);
        }

        if (!activos.isEmpty()) {
            codigoVerificacionCorreoRepository.saveAll(activos);
        }
    }

    private void markAsExpired(CodigoVerificacionCorreo codigo, LocalDateTime ahora) {
        if (codigo.getEstatus() != EstatusCodigoVerificacion.EXPIRADO) {
            codigo.setEstatus(EstatusCodigoVerificacion.EXPIRADO);
            codigo.setFechaModifica(ahora);
            codigo.setUsuarioModifica(USUARIO_SISTEMA);
            codigoVerificacionCorreoRepository.save(codigo);
        }
    }

    private int calculateResends(CodigoVerificacionCorreo ultimoCodigo, LocalDateTime ahora) {
        if (ultimoCodigo == null || isExpired(ultimoCodigo, ahora)) {
            return 0;
        }
        return (ultimoCodigo.getReenvios() != null ? ultimoCodigo.getReenvios() : 0) + 1;
    }

    private boolean isExpired(CodigoVerificacionCorreo codigo, LocalDateTime ahora) {
        return codigo.getFechaExpiracion() == null || !codigo.getFechaExpiracion().isAfter(ahora);
    }

    private String generateVerificationCode() {
        return String.format("%06d", SECURE_RANDOM.nextInt(1_000_000));
    }

    private String normalizeEmail(String correo) {
        return correo.toLowerCase(Locale.ROOT).trim();
    }
}
