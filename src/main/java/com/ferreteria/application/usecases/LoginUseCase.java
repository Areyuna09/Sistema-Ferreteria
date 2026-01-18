package com.ferreteria.application.usecases;

import com.ferreteria.domain.entities.User;
import com.ferreteria.domain.exceptions.AuthenticationException;
import com.ferreteria.domain.repositories.UserRepository;
import org.mindrot.jbcrypt.BCrypt;

/**
 * Caso de uso: Iniciar sesión.
 * Aplica Single Responsibility Principle.
 */
public class LoginUseCase {

    private final UserRepository userRepository;

    public LoginUseCase(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User execute(String username, String password) {
        validateInput(username, password);

        User user = userRepository.findByUsername(username)
            .orElseThrow(AuthenticationException::userNotFound);

        if (!user.isActive()) {
            throw AuthenticationException.userInactive();
        }

        if (!BCrypt.checkpw(password, user.getPasswordHash())) {
            throw AuthenticationException.invalidCredentials();
        }

        return user;
    }

    private void validateInput(String username, String password) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Usuario es requerido");
        }
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("Contraseña es requerida");
        }
    }
}
