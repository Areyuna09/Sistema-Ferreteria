package com.ferreteria.models;

import java.time.LocalDateTime;

/**
 * Modelo que representa una categoría de productos.
 * Utiliza el patrón Builder para construcción inmutable.
 *
 * @author Sistema Ferretería
 * @version 1.0
 */
public class Category {

    private final int id;
    private final String nombre;
    private final String descripcion;
    private final boolean active;
    private final LocalDateTime createdAt;

    /**
     * Constructor privado - usar Builder para crear instancias
     */
    private Category(Builder builder) {
        this.id = builder.id;
        this.nombre = builder.nombre;
        this.descripcion = builder.descripcion;
        this.active = builder.active;
        this.createdAt = builder.createdAt;
    }

    // ==================== GETTERS ====================

    public int getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public boolean isActive() {
        return active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    // ==================== MÉTODOS DE UTILIDAD ====================

    /**
     * Verifica si la categoría tiene descripción
     */
    public boolean hasDescripcion() {
        return descripcion != null && !descripcion.isBlank();
    }

    @Override
    public String toString() {
        return nombre;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Category category = (Category) o;
        return id == category.id;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }

    // ==================== BUILDER ====================

    /**
     * Builder para construir instancias de Category de forma fluida.
     */
    public static class Builder {
        private int id;
        private String nombre;
        private String descripcion;
        private boolean active = true;
        private LocalDateTime createdAt = LocalDateTime.now();

        public Builder() {
        }

        public Builder id(int id) {
            this.id = id;
            return this;
        }

        public Builder nombre(String nombre) {
            this.nombre = nombre;
            return this;
        }

        public Builder descripcion(String descripcion) {
            this.descripcion = descripcion;
            return this;
        }

        public Builder active(boolean active) {
            this.active = active;
            return this;
        }

        public Builder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        /**
         * Construye la instancia de Category después de validar los campos requeridos.
         *
         * @return nueva instancia de Category
         * @throws IllegalArgumentException si la validación falla
         */
        public Category build() {
            validate();
            return new Category(this);
        }

        /**
         * Valida que los campos requeridos estén presentes
         */
        private void validate() {
            if (nombre == null || nombre.isBlank()) {
                throw new IllegalArgumentException("El nombre de la categoría es requerido");
            }
            if (nombre.length() > 100) {
                throw new IllegalArgumentException("El nombre no puede exceder 100 caracteres");
            }
        }
    }
}
