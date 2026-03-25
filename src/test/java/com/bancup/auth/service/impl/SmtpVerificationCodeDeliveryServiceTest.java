package com.bancup.auth.service.impl;

import com.bancup.exception.BancupException;
import com.bancup.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SmtpVerificationCodeDeliveryServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private SmtpVerificationCodeDeliveryService service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "from", "no-reply@bancup.test");
        ReflectionTestUtils.setField(service, "signupSubject", "Bancup - Codigo de verificacion");
    }

    @Test
    void shouldSendVerificationEmailWithCode() {
        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        doNothing().when(mailSender).send(messageCaptor.capture());

        service.sendSignupCode("usuario@correo.com", "123456", LocalDateTime.of(2026, 3, 25, 11, 35));

        verify(mailSender).send(messageCaptor.getValue());
        assertThat(messageCaptor.getValue().getTo()).containsExactly("usuario@correo.com");
        assertThat(messageCaptor.getValue().getFrom()).isEqualTo("no-reply@bancup.test");
        assertThat(messageCaptor.getValue().getSubject()).isEqualTo("Bancup - Codigo de verificacion");
        assertThat(messageCaptor.getValue().getText()).contains("123456");
    }

    @Test
    void shouldFailWhenMailSenderThrows() {
        doThrow(new MailSendException("smtp down")).when(mailSender).send(org.mockito.ArgumentMatchers.any(SimpleMailMessage.class));

        assertThatThrownBy(() -> service.sendSignupCode(
                "usuario@correo.com",
                "123456",
                LocalDateTime.of(2026, 3, 25, 11, 35)
        ))
                .isInstanceOf(BancupException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ENVIO_CORREO_FALLIDO);
    }
}
