package com.ferreteria.domain.entities;

import java.time.LocalDateTime;

/**
 * Entidad de dominio que representa un usuario del sistema.
 * Inmutable después de creación para garantizar integridad.
 */
public class User {

    private final int id;
    private final String username;
    private final String passwordHash;
    private final UserRole role;
    private final String fullName;
    private final boolean active;
    private final LocalDateTime createdAt;

    private User(Builder builder) {
        this.id = builder.id;
        this.username = builder.username;
        this.passwordHash = builder.passwordHash;
        this.role = builder.role;
        this.fullName = builder.fullName;
        this.active = builder.active;
        this.createdAt = builder.createdAt;
    }

    // Getters
    public int getId() { return id; }
    public String getUsername() { return username; }
    public String getPasswordHash() { return passwordHash; }
    public UserRole getRole() { return role; }
    public String getFullName() { return fullName; }
    public boolean isActive() { return active; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public boolean isAdmin() {
        return role == UserRole.ADMINISTRADOR;
    }

    // Builder Pattern
    public static class Builder {
        private int id;
        private String username;
        private String passwordHash;
        private UserRole role = UserRole.VENDEDOR;
        private String fullName;
        private boolean active = true;
        private LocalDateTime createdAt = LocalDateTime.now();

        public Builder id(int id) { this.id = id; return this; }
        public Builder username(String username) { this.username = username; return this; }
        public Builder passwordHash(String hash) { this.passwordHash = hash; return this; }
        public Builder role(UserRole role) { this.role = role; return this; }
        public Builder role(String role) {
            this.role = UserRole.fromString(role);
            return this;
        }
        public Builder fullName(String name) { this.fullName = name; return this; }
        public Builder active(boolean active) { this.active = active; return this; }
        public Builder createdAt(LocalDateTime dt) { this.createdAt = dt; return this; }

        public User build() {
            validate();
            return new User(this);
        }

        private void validate() {
            if (username == null || username.isBlank()) {
                throw new IllegalArgumentException("Username es requerido");
            }
        }
    }
}
