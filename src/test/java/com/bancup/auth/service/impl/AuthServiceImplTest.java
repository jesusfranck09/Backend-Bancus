package com.bancup.auth.service.impl;

import com.bancup.auth.dto.request.RequestSignupCodeRequest;
import com.bancup.auth.dto.request.VerifySignupCodeRequest;
import com.bancup.auth.dto.response.RequestSignupCodeResponse;
import com.bancup.auth.dto.response.VerifySignupCodeResponse;
import com.bancup.auth.mapper.UsuarioMapper;
import com.bancup.auth.service.VerificationCodeDeliveryService;
import com.bancup.entity.CodigoVerificacionCorreo;
import com.bancup.entity.EstatusCodigoVerificacion;
import com.bancup.entity.TipoCodigoVerificacion;
import com.bancup.repository.AuditoriaEventoRepository;
import com.bancup.repository.CatGeneroRepository;
import com.bancup.repository.CodigoVerificacionCorreoRepository;
import com.bancup.repository.RolRepository;
import com.bancup.repository.UsuarioRepository;
import com.bancup.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private RolRepository rolRepository;

    @Mock
    private AuditoriaEventoRepository auditoriaEventoRepository;

    @Mock
    private CatGeneroRepository catGeneroRepository;

    @Mock
    private CodigoVerificacionCorreoRepository codigoVerificacionCorreoRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UsuarioMapper usuarioMapper;

    @Mock
    private JwtService jwtService;

    @Mock
    private VerificationCodeDeliveryService verificationCodeDeliveryService;

    @InjectMocks
    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "rolDefecto", "CLIENTE");
        ReflectionTestUtils.setField(authService, "verificationCodeExpirationMinutes", 10L);
        ReflectionTestUtils.setField(authService, "maxVerificationAttempts", 5);
        ReflectionTestUtils.setField(authService, "maxVerificationResends", 3);
        ReflectionTestUtils.setField(authService, "returnCodeInResponse", true);
        ReflectionTestUtils.setField(authService, "signupVerificationExpirationMs", 900_000L);
    }

    @Test
    void requestSignupCodeShouldGenerateSixDigitCodeAndReturnItInDebugMode() {
        RequestSignupCodeRequest request = new RequestSignupCodeRequest();
        request.setCorreo(" Usuario@Correo.com ");

        when(usuarioRepository.existsByEmail("usuario@correo.com")).thenReturn(false);
        when(codigoVerificacionCorreoRepository.findTopByCorreoAndTipoOrderByFechaCreaDesc(
                "usuario@correo.com",
                TipoCodigoVerificacion.SIGNUP
        )).thenReturn(Optional.empty());
        when(codigoVerificacionCorreoRepository.findByCorreoAndTipoAndEstatusIn(
                eq("usuario@correo.com"),
                eq(TipoCodigoVerificacion.SIGNUP),
                any()
        )).thenReturn(java.util.Collections.emptyList());
        when(passwordEncoder.encode(anyString())).thenAnswer(invocation -> "hash:" + invocation.getArgument(0, String.class));
        when(codigoVerificacionCorreoRepository.save(any(CodigoVerificacionCorreo.class)))
                .thenAnswer(invocation -> invocation.getArgument(0, CodigoVerificacionCorreo.class));

        ArgumentCaptor<String> codeCaptor = ArgumentCaptor.forClass(String.class);
        doNothing().when(verificationCodeDeliveryService)
                .sendSignupCode(eq("usuario@correo.com"), codeCaptor.capture(), any(LocalDateTime.class));

        RequestSignupCodeResponse response = authService.requestSignupCode(request, "127.0.0.1");

        assertThat(codeCaptor.getValue()).matches("\\d{6}");
        assertThat(response.getCorreo()).isEqualTo("usuario@correo.com");
        assertThat(response.getCodigo()).isEqualTo(codeCaptor.getValue());
        assertThat(response.getCodigoDebug()).isEqualTo(codeCaptor.getValue());
        assertThat(Duration.between(LocalDateTime.now(), response.getFechaExpiracion())).isBetween(
                Duration.ofMinutes(9),
                Duration.ofMinutes(10).plusSeconds(1)
        );
        verify(passwordEncoder).encode(codeCaptor.getValue());
    }

    @Test
    void verifySignupCodeShouldTrimWhitespaceReturnTokenAndExtendVerificationWindow() {
        VerifySignupCodeRequest request = new VerifySignupCodeRequest();
        request.setCorreo(" Usuario@Correo.com ");
        request.setCodigo(" 123456 ");

        CodigoVerificacionCorreo codigo = CodigoVerificacionCorreo.builder()
                .correo("usuario@correo.com")
                .codigoHash("hash")
                .tipo(TipoCodigoVerificacion.SIGNUP)
                .estatus(EstatusCodigoVerificacion.PENDIENTE)
                .intentosFallidos(0)
                .reenvios(0)
                .fechaExpiracion(LocalDateTime.now().plusMinutes(10))
                .fechaCrea(LocalDateTime.now().minusMinutes(1))
                .usuarioCrea("AUTH_API")
                .build();

        when(codigoVerificacionCorreoRepository.findTopByCorreoAndTipoOrderByFechaCreaDesc(
                "usuario@correo.com",
                TipoCodigoVerificacion.SIGNUP
        )).thenReturn(Optional.of(codigo));
        when(passwordEncoder.matches("123456", "hash")).thenReturn(true);
        when(jwtService.generateSignupVerificationToken("usuario@correo.com")).thenReturn("jwt-temporal");
        when(codigoVerificacionCorreoRepository.save(any(CodigoVerificacionCorreo.class)))
                .thenAnswer(invocation -> invocation.getArgument(0, CodigoVerificacionCorreo.class));

        VerifySignupCodeResponse response = authService.verifySignupCode(request, "127.0.0.1");

        assertThat(request.getCodigo()).isEqualTo("123456");
        assertThat(response.getCorreo()).isEqualTo("usuario@correo.com");
        assertThat(response.getVerificationToken()).isEqualTo("jwt-temporal");
        assertThat(codigo.getEstatus()).isEqualTo(EstatusCodigoVerificacion.VERIFICADO);
        assertThat(codigo.getFechaVerificado()).isNotNull();
        assertThat(Duration.between(codigo.getFechaVerificado(), codigo.getFechaExpiracion()))
                .isEqualTo(Duration.ofMinutes(15));
    }
}
