package com.bancup.exception;

/**
 * Se lanza cuando el usuario intenta autenticarse en una cuenta bloqueada.
 * La cuenta se bloquea automaticamente tras 5 intentos fallidos consecutivos.
 * Mapea a HTTP 423 Locked.
 */
public class AccountLockedException extends BancupException {

    public AccountLockedException() {
        super(ErrorCode.CUENTA_BLOQUEADA,
                "Tu cuenta ha sido bloqueada temporalmente por multiples intentos fallidos. " +
                "Contacta a soporte para desbloquearla.");
    }

    public AccountLockedException(String message) {
        super(ErrorCode.CUENTA_BLOQUEADA, message);
    }
}
