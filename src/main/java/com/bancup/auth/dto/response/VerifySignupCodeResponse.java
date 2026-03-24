package com.bancup.auth.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class VerifySignupCodeResponse {

    private String correo;
    private String verificationToken;
}
