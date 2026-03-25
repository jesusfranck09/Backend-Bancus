package com.bancup.auth.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RequestSignupCodeResponse {

    private String correo;
    private LocalDateTime fechaExpiracion;
    private String codigo;
    private String codigoDebug;
}
