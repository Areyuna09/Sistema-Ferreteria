package com.ferreteria.models;

import java.time.LocalDateTime;

/**
 * Modelo que representa la configuración del negocio.
 * Solo debe existir un único registro (id=1) en la base de datos.
 */
public class BusinessConfig {

    private final int id;
    private final String businessName;
    private final String cuit;
    private final String address;
    private final String phone;
    private final String email;
    private final LocalDateTime updatedAt;

    private BusinessConfig(Builder builder) {
        this.id = builder.id;
        this.businessName = builder.businessName;
        this.cuit = builder.cuit;
        this.address = builder.address;
        this.phone = builder.phone;
        this.email = builder.email;
        this.updatedAt = builder.updatedAt;
    }

    // Getters
    public int getId() { return id; }
    public String getBusinessName() { return businessName; }
    public String getCuit() { return cuit; }
    public String getAddress() { return address; }
    public String getPhone() { return phone; }
    public String getEmail() { return email; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    /**
     * Valida el formato del CUIT (XX-XXXXXXXX-X).
     * @param cuit CUIT a validar
     * @return true si el formato es válido
     */
    public static boolean isValidCuit(String cuit) {
        if (cuit == null || cuit.isBlank()) {
            return true; // Campo opcional
        }
        // Formato: XX-XXXXXXXX-X (11 dígitos con guiones)
        return cuit.matches("^\\d{2}-\\d{8}-\\d{1}$");
    }

    /**
     * Valida el formato del email.
     * @param email Email a validar
     * @return true si el formato es válido
     */
    public static boolean isValidEmail(String email) {
        if (email == null || email.isBlank()) {
            return true; // Campo opcional
        }
        // Regex básico para email
        return email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    }

    /**
     * Formatea el CUIT quitando guiones y espacios.
     * @param cuit CUIT a formatear
     * @return CUIT solo con números
     */
    public static String formatCuitDigitsOnly(String cuit) {
        if (cuit == null) return "";
        return cuit.replaceAll("[^0-9]", "");
    }

    /**
     * Formatea el CUIT agregando guiones (XX-XXXXXXXX-X).
     * @param cuit CUIT con solo dígitos
     * @return CUIT formateado
     */
    public static String formatCuitWithDashes(String cuit) {
        String digitsOnly = formatCuitDigitsOnly(cuit);
        if (digitsOnly.length() != 11) {
            return cuit; // Devolver original si no tiene 11 dígitos
        }
        return digitsOnly.substring(0, 2) + "-" + 
               digitsOnly.substring(2, 10) + "-" + 
               digitsOnly.substring(10);
    }

    // Builder Pattern
    public static class Builder {
        private int id = 1; // Siempre ID 1
        private String businessName;
        private String cuit;
        private String address;
        private String phone;
        private String email;
        private LocalDateTime updatedAt = LocalDateTime.now();

        public Builder id(int id) { 
            this.id = id; 
            return this; 
        }

        public Builder businessName(String name) { 
            this.businessName = name; 
            return this; 
        }

        public Builder cuit(String cuit) { 
            this.cuit = cuit; 
            return this; 
        }

        public Builder address(String address) { 
            this.address = address; 
            return this; 
        }

        public Builder phone(String phone) { 
            this.phone = phone; 
            return this; 
        }

        public Builder email(String email) { 
            this.email = email; 
            return this; 
        }

        public Builder updatedAt(LocalDateTime updatedAt) { 
            this.updatedAt = updatedAt; 
            return this; 
        }

        public BusinessConfig build() {
            validate();
            return new BusinessConfig(this);
        }

        private void validate() {
            if (businessName == null || businessName.isBlank()) {
                throw new IllegalArgumentException("El nombre del negocio es requerido");
            }
            if (cuit != null && !cuit.isBlank() && !BusinessConfig.isValidCuit(cuit)) {
                throw new IllegalArgumentException("Formato de CUIT inválido. Use: XX-XXXXXXXX-X");
            }
            if (email != null && !email.isBlank() && !BusinessConfig.isValidEmail(email)) {
                throw new IllegalArgumentException("Formato de email inválido");
            }
        }
    }
}