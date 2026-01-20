package com.ferreteria.models;

/**
 * Roles disponibles en el sistema.
 */
public enum UserRole {
    ADMINISTRADOR("administrador"),
    VENDEDOR("vendedor"),
    SUPERVISOR("supervisor");

    private final String value;

    UserRole(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static UserRole fromString(String text) {
        for (UserRole role : UserRole.values()) {
            if (role.value.equalsIgnoreCase(text)) {
                return role;
            }
        }
        return VENDEDOR; // Default
    }
}
