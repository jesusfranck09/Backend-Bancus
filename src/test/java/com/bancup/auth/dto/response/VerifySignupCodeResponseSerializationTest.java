package com.bancup.auth.dto.response;

import com.bancup.common.dto.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class VerifySignupCodeResponseSerializationTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldSerializeVerificationTokenInsideData() throws Exception {
        ApiResponse<VerifySignupCodeResponse> response = ApiResponse.success(
                "Correo verificado correctamente",
                VerifySignupCodeResponse.builder()
                        .correo("usuario@correo.com")
                        .verificationToken("jwt-temporal")
                        .build()
        );

        String json = objectMapper.writeValueAsString(response);

        assertThat(json).contains("\"data\"");
        assertThat(json).contains("\"verificationToken\":\"jwt-temporal\"");
    }
}
