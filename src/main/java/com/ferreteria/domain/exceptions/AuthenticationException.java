package com.ferreteria.domain.exceptions;

/**
 * Excepción lanzada cuando falla la autenticación.
 */
public class AuthenticationException extends DomainException {

    public AuthenticationException(String message) {
        super(message);
    }

    public static AuthenticationException invalidCredentials() {
        return new AuthenticationException("Usuario o contraseña incorrectos");
    }

    public static AuthenticationException userNotFound() {
        return new AuthenticationException("Usuario no encontrado");
    }

    public static AuthenticationException userInactive() {
        return new AuthenticationException("Usuario inactivo");
    }
}
