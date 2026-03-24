package com.bancup.auth.service;

import java.time.LocalDateTime;

public interface VerificationCodeDeliveryService {

    void sendSignupCode(String correo, String codigo, LocalDateTime fechaExpiracion);
}
