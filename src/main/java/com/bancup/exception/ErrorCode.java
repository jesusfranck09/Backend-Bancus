package com.bancup.exception;

/**
 * Codigos de error estructurados del sistema Bancup.
 * Se usan en ApiResponse.errorCode para que el frontend pueda
 * manejar errores de forma precisa sin depender del mensaje de texto.
 */
public enum ErrorCode {

    // Auth - Signup
    EMAIL_DUPLICADO,
    CURP_DUPLICADO,
    ROL_NO_ENCONTRADO,
    ROL_CLIENTE_NO_CONFIGURADO,
    GENERO_NO_ENCONTRADO,
    ESTADO_NO_ENCONTRADO,
    CODIGO_VERIFICACION_INVALIDO,
    CODIGO_VERIFICACION_EXPIRADO,
    CORREO_NO_VERIFICADO,
    TOKEN_VERIFICACION_INVALIDO,
    LIMITE_REENVIOS_ALCANZADO,
    LIMITE_INTENTOS_VERIFICACION_ALCANZADO,

    // Auth - Login
    USUARIO_NO_ENCONTRADO,
    CREDENCIALES_INVALIDAS,
    CUENTA_BLOQUEADA,

    // Auth - Tokens
    TOKEN_EXPIRADO,
    TOKEN_INVALIDO,

    // Validacion de request
    VALIDACION_FALLIDA,

    // Genericos
    RECURSO_NO_ENCONTRADO,
    ERROR_INTERNO
}
