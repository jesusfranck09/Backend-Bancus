package com.bancup.auth.service.impl;

import com.bancup.auth.service.VerificationCodeDeliveryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@Slf4j
public class LoggingVerificationCodeDeliveryService implements VerificationCodeDeliveryService {

    @Override
    public void sendSignupCode(String correo, String codigo, LocalDateTime fechaExpiracion) {
        log.info(
                "Codigo de verificacion SIGNUP para correo={} codigo={} expira={}",
                correo,
                codigo,
                fechaExpiracion
        );
    }
}
