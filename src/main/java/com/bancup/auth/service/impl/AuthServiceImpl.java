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
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final CuentaRepository cuentaRepository;
    private final DispositivoRepository dispositivoRepository;
    private final AuditoriaEventoRepository auditoriaEventoRepository;
    private final CatGeneroRepository catGeneroRepository;
    private final CatEstadoRepository catEstadoRepository;
    private final PasswordEncoder passwordEncoder;
    private final UsuarioMapper usuarioMapper;
    private final JwtService jwtService;

    /** Nombre del rol por defecto. Configurable en application.properties. */
    @Value("${bancup.auth.default-role}")
    private String rolDefecto;

    /** Tipo de cuenta inicial. Configurable en application.properties. */
    @Value("${bancup.auth.default-account-type}")
    private String tipoCuentaDefecto;

    @Override
    @Transactional
    public SignupResponse signup(SignupRequest request, String ipOrigen) {
        log.info("Iniciando signup para email: {}", request.getEmail());

        // 1. Validar unicidad de email (case-insensitive: se normaliza a minusculas)
        String emailNormalizado = request.getEmail().toLowerCase().trim();
        if (usuarioRepository.existsByEmail(emailNormalizado)) {
            throw new DuplicateResourceException(
                    ErrorCode.EMAIL_DUPLICADO,
                    "El email '" + emailNormalizado + "' ya esta registrado"
            );
        }

        // 2. Validar unicidad de CURP si se proporciona
        if (StringUtils.hasText(request.getCurp())) {
            String curpNormalizada = request.getCurp().toUpperCase().trim();
            if (usuarioRepository.existsByCurp(curpNormalizada)) {
                throw new DuplicateResourceException(
                        ErrorCode.CURP_DUPLICADO,
                        "La CURP proporcionada ya esta registrada en el sistema"
                );
            }
        }

        // 3. Resolver rol: especificado en el request o por defecto desde ROLES
        Rol rol = resolverRol(request.getIdRol());

        // 4. Validar genero si se proporciona (debe existir en CAT_GENERO)
        CatGenero genero = null;
        if (request.getGeneroId() != null) {
            genero = catGeneroRepository.findById(request.getGeneroId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            ErrorCode.GENERO_NO_ENCONTRADO,
                            "El genero con id " + request.getGeneroId() + " no existe en la tabla CAT_GENERO"
                    ));
        }

        // 5. Validar entidad/estado si se proporciona (debe existir en CAT_ESTADOS)
        CatEstado entidad = null;
        if (request.getEntidadId() != null) {
            entidad = catEstadoRepository.findById(request.getEntidadId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            ErrorCode.ESTADO_NO_ENCONTRADO,
                            "La entidad con id " + request.getEntidadId() + " no existe en la tabla CAT_ESTADOS"
                    ));
        }

        // 6. Hash del password con BCrypt (strength 12). Nunca texto plano.
        String passwordHash = passwordEncoder.encode(request.getPassword());

        // 7. Construir y persistir el usuario
        Usuario usuario = Usuario.builder()
                .rol(rol)
                .publicId(UUID.randomUUID().toString())
                .email(emailNormalizado)
                .passwordHash(passwordHash)
                .password2Hash(null)                          // Reservado para fase futura
                .telefono(request.getTelefono())
                .nombre(request.getNombre().trim())
                .apellidoPaterno(request.getApellidoPaterno().trim())
                .apellidoMaterno(request.getApellidoMaterno() != null
                        ? request.getApellidoMaterno().trim() : null)
                .curp(StringUtils.hasText(request.getCurp())
                        ? request.getCurp().toUpperCase().trim() : null)
                .fechaNacimiento(request.getFechaNacimiento())
                .genero(genero)
                .entidad(entidad)
                .fotoSelfieUrl(null)                          // Se actualiza en flujo KYC
                .fotoIneFrenteUrl(null)
                .fotoIneVueltaUrl(null)
                .kycStatus("PENDIENTE")
                .esBiometriaValida(0)
                .ultimoLogin(null)
                .fechaRegistro(LocalDateTime.now())
                .failedLoginAttempts(0)
                .accountLocked(false)
                .build();

        usuario = usuarioRepository.save(usuario);
        log.info("Usuario creado con id_usuario={}, public_id={}", usuario.getIdUsuario(), usuario.getPublicId());

        // 8. Crear cuenta base para el usuario
        //    saldo inicial = 0, tipo_cuenta configurado en application.properties
        Cuenta cuenta = Cuenta.builder()
                .usuario(usuario)
                .saldo(BigDecimal.ZERO)
                .tipoCuenta(tipoCuentaDefecto)
                .fechaCreacion(LocalDateTime.now())
                .build();
        cuenta = cuentaRepository.save(cuenta);
        log.info("Cuenta base creada id_cuenta={} tipo={} para usuario id={}",
                cuenta.getIdCuenta(), cuenta.getTipoCuenta(), usuario.getIdUsuario());

        // 9. Registrar evento de auditoria: accion SIGNUP
        AuditoriaEvento auditoria = AuditoriaEvento.builder()
                .usuario(usuario)
                .accion("SIGNUP")
                .ipOrigen(ipOrigen)
                .fechaEvento(LocalDateTime.now())
                .build();
        auditoriaEventoRepository.save(auditoria);
        log.debug("Evento de auditoria SIGNUP registrado para usuario id={}", usuario.getIdUsuario());

        // 10. Registrar dispositivo SOLO si el request trae datos reales
        //     No se crean registros vacios/ficticios
        if (StringUtils.hasText(request.getDeviceTipo()) || StringUtils.hasText(request.getDeviceIp())) {
            Dispositivo dispositivo = Dispositivo.builder()
                    .usuario(usuario)
                    .tipo(request.getDeviceTipo())
                    .ip(request.getDeviceIp())
                    .fechaRegistro(LocalDateTime.now())
                    .build();
            dispositivoRepository.save(dispositivo);
            log.debug("Dispositivo registrado tipo={} ip={} para usuario id={}",
                    request.getDeviceTipo(), request.getDeviceIp(), usuario.getIdUsuario());
        }

        log.info("Signup completado exitosamente para email={}", emailNormalizado);
        return usuarioMapper.toSignupResponse(usuario, cuenta);
    }

    @Override
    @Transactional
    public LoginResponse login(LoginRequest request, String ipOrigen) {
        String emailNormalizado = request.getEmail().toLowerCase().trim();
        log.info("Iniciando login para email: {}", emailNormalizado);

        // 1. Buscar usuario por email. Se lanza excepcion generica para no revelar
        //    si el email existe o no (prevencion de enumeracion de usuarios).
        Usuario usuario = usuarioRepository.findByEmail(emailNormalizado)
                .orElseThrow(InvalidCredentialsException::new);

        // 2. Validar password con BCrypt
        if (!passwordEncoder.matches(request.getPassword(), usuario.getPasswordHash())) {
            log.warn("Contrasena incorrecta para email: {}", emailNormalizado);
            throw new InvalidCredentialsException();
        }

        // 3. Generar JWT con publicId, email y rol
        String rol = usuario.getRol() != null ? usuario.getRol().getNombrePerfil() : "CLIENTE";
        String token = jwtService.generateToken(usuario.getPublicId(), emailNormalizado, rol);

        // 4. Registrar evento de auditoria: accion LOGIN
        AuditoriaEvento auditoria = AuditoriaEvento.builder()
                .usuario(usuario)
                .accion("LOGIN")
                .ipOrigen(ipOrigen)
                .fechaEvento(LocalDateTime.now())
                .build();
        auditoriaEventoRepository.save(auditoria);

        log.info("Login exitoso para usuario publicId={}", usuario.getPublicId());

        return LoginResponse.builder()
                .success(true)
                .message("Autenticacion exitosa")
                .token(token)
                .userId(usuario.getPublicId())
                .email(emailNormalizado)
                .build();
    }

    // =========================================================================
    // Metodos privados de soporte
    // =========================================================================

    /**
     * Resuelve el rol a asignar al usuario.
     *
     * Si se especifica idRol en el request: valida existencia en ROLES.
     * Si no se especifica: busca el rol por defecto (bancup.auth.default-role).
     *
     * IMPORTANTE: Si el rol por defecto no existe en la tabla ROLES, se lanza
     * ResourceNotFoundException con ErrorCode.ROL_CLIENTE_NO_CONFIGURADO.
     * Esto indica que los catalogos base no han sido cargados en la BD.
     * NO se inventa ni se crea el rol silenciosamente.
     */
    private Rol resolverRol(Long idRolSolicitado) {
        if (idRolSolicitado != null) {
            return rolRepository.findById(idRolSolicitado)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            ErrorCode.ROL_NO_ENCONTRADO,
                            "El rol con id " + idRolSolicitado + " no existe en la tabla ROLES"
                    ));
        }
        return rolRepository.findByNombrePerfil(rolDefecto)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.ROL_CLIENTE_NO_CONFIGURADO,
                        "El rol por defecto '" + rolDefecto + "' no existe en la tabla ROLES. " +
                        "Es necesario cargar los catalogos base (INSERT en ROLES con nombre_perfil='" + rolDefecto + "') " +
                        "antes de realizar el primer signup."
                ));
    }
}
