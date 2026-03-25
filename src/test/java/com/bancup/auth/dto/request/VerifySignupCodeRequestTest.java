package com.bancup.auth.dto.request;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class VerifySignupCodeRequestTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void shouldTrimCodeBeforeValidation() {
        VerifySignupCodeRequest request = new VerifySignupCodeRequest();
        request.setCorreo(" usuario@correo.com ");
        request.setCodigo(" 123456 ");

        assertThat(request.getCorreo()).isEqualTo("usuario@correo.com");
        assertThat(request.getCodigo()).isEqualTo("123456");
        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void shouldRejectCodesThatAreNotExactlySixDigits() {
        assertThat(hasCodigoViolation("12345")).isTrue();
        assertThat(hasCodigoViolation("1234567")).isTrue();
        assertThat(hasCodigoViolation("12a456")).isTrue();
        assertThat(hasCodigoViolation("12 456")).isTrue();
    }

    private boolean hasCodigoViolation(String codigo) {
        VerifySignupCodeRequest request = new VerifySignupCodeRequest();
        request.setCorreo("usuario@correo.com");
        request.setCodigo(codigo);

        return validator.validate(request).stream()
                .anyMatch(violation -> "codigo".equals(violation.getPropertyPath().toString()));
    }
}
