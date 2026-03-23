package com.bancup.exception.handler;

import com.bancup.common.dto.ApiResponse;
import com.bancup.exception.AccountLockedException;
import com.bancup.exception.DuplicateResourceException;
import com.bancup.exception.ErrorCode;
import com.bancup.exception.InvalidCredentialsException;
import com.bancup.exception.ResourceNotFoundException;
import com.bancup.exception.TokenExpiredException;
import com.bancup.exception.UserNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;
import java.util.stream.Stream;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * Credenciales incorrectas durante el login.
     * HTTP 401 Unauthorized.
     */
    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ApiResponse<?>> handleInvalidCredentials(InvalidCredentialsException ex) {
        log.warn("Intento de login fallido: {}", ex.getMessage());
        return buildSimpleError(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }

    /**
     * Cuenta bloqueada por multiples intentos fallidos.
     * HTTP 423 Locked.
     */
    @ExceptionHandler(AccountLockedException.class)
    public ResponseEntity<ApiResponse<?>> handleAccountLocked(AccountLockedException ex) {
        log.warn("Cuenta bloqueada: {}", ex.getMessage());
        return buildSimpleError(HttpStatus.LOCKED, ex.getMessage());
    }

    /**
     * Usuario no encontrado.
     * HTTP 404 Not Found.
     */
    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ApiResponse<?>> handleUserNotFound(UserNotFoundException ex) {
        log.warn("Usuario no encontrado: {}", ex.getMessage());
        return buildSimpleError(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    /**
     * Token expirado o invalido.
     * HTTP 401 Unauthorized.
     */
    @ExceptionHandler(TokenExpiredException.class)
    public ResponseEntity<ApiResponse<?>> handleTokenExpired(TokenExpiredException ex) {
        log.warn("Token invalido o expirado: {}", ex.getMessage());
        return buildSimpleError(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }

    /**
     * Email duplicado, CURP duplicada u otro recurso duplicado.
     * HTTP 409 Conflict.
     */
    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ApiResponse<?>> handleDuplicateResource(DuplicateResourceException ex) {
        log.warn("Recurso duplicado [{}]: {}", ex.getErrorCode(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(ex.getMessage(), ex.getErrorCode().name()));
    }

    /**
     * Rol, genero, entidad u otro catalogo no encontrado.
     * HTTP 404 Not Found.
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<?>> handleResourceNotFound(ResourceNotFoundException ex) {
        log.warn("Recurso no encontrado [{}]: {}", ex.getErrorCode(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(ex.getMessage(), ex.getErrorCode().name()));
    }

    /**
     * Errores de validacion de Jakarta Validation (@Valid).
     * HTTP 400 Bad Request.
     * El mensaje concatena todos los campos con error.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<?>> handleValidationErrors(MethodArgumentNotValidException ex) {
        String mensajes = Stream.concat(
                        ex.getBindingResult().getFieldErrors()
                                .stream()
                                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage()),
                        ex.getBindingResult().getGlobalErrors()
                                .stream()
                                .map(ge -> ge.getDefaultMessage())
                )
                .collect(Collectors.joining("; "));
        log.warn("Error de validacion en request: {}", mensajes);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(mensajes, ErrorCode.VALIDACION_FALLIDA.name()));
    }

    /**
     * JSON malformado o tipo de dato incorrecto en el request body.
     * HTTP 400 Bad Request.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<?>> handleMessageNotReadable(HttpMessageNotReadableException ex) {
        log.warn("Request body invalido: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("El cuerpo de la peticion tiene un formato invalido.", ErrorCode.VALIDACION_FALLIDA.name()));
    }

    /**
     * Error interno no controlado.
     * HTTP 500 Internal Server Error.
     * No se expone el detalle del error al cliente por seguridad.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<?>> handleGenericException(Exception ex) {
        log.error("Error interno del servidor no controlado", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Error interno del servidor. Por favor intente de nuevo.", ErrorCode.ERROR_INTERNO.name()));
    }

    private ResponseEntity<ApiResponse<?>> buildSimpleError(HttpStatus status, String message) {
        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .success(false)
                .message(message)
                .build();
        return ResponseEntity.status(status).body(response);
    }
}
