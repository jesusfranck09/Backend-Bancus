package com.bancup.auth.service.impl;

import com.bancup.auth.service.VerificationCodeDeliveryService;
import com.bancup.exception.BancupException;
import com.bancup.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@ConditionalOnProperty(prefix = "bancup.mail", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class SmtpVerificationCodeDeliveryService implements VerificationCodeDeliveryService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final JavaMailSender mailSender;

    @Value("${bancup.mail.from:${spring.mail.username:}}")
    private String from;

    @Value("${bancup.mail.signup.subject:Bancup - Codigo de verificacion}")
    private String signupSubject;

    @Override
    public void sendSignupCode(String correo, String codigo, LocalDateTime fechaExpiracion) {
        if (from == null || from.isBlank()) {
            throw new BancupException(
                    ErrorCode.ENVIO_CORREO_FALLIDO,
                    "No fue posible enviar el correo de verificacion porque no hay remitente configurado."
            );
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from.trim());
        message.setTo(correo);
        message.setSubject(signupSubject);
        message.setText(buildSignupMessage(correo, codigo, fechaExpiracion));

        try {
            mailSender.send(message);
            log.info("Correo de verificacion enviado correctamente a {}", correo);
        } catch (MailException ex) {
            log.error("Fallo el envio del correo de verificacion a {}", correo, ex);
            throw new BancupException(
                    ErrorCode.ENVIO_CORREO_FALLIDO,
                    "No fue posible enviar el correo de verificacion. Intenta nuevamente.",
                    ex
            );
        }
    }

    private String buildSignupMessage(String correo, String codigo, LocalDateTime fechaExpiracion) {
        return String.format(
                """
                Hola,

                Recibimos una solicitud para registrar el correo %s en Bancup.

                Tu codigo de verificacion es: %s

                Este codigo expira el %s.

                Si tu no solicitaste este registro, puedes ignorar este mensaje.

                Equipo Bancup
                """,
                correo,
                codigo,
                fechaExpiracion.format(DATE_TIME_FORMATTER)
        );
    }
}
