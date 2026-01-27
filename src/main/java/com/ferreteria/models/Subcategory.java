package com.ferreteria.models;

import java.time.LocalDateTime;

/**
 * Modelo que representa una subcategoría de productos.
 * Cada subcategoría pertenece a una categoría padre.
 * Utiliza el patrón Builder para construcción inmutable.
 *
 * @author Sistema Ferretería
 * @version 1.0
 */
public class Subcategory {

    private final int id;
    private final int categoryId;
    private final String categoryName;
    private final String nombre;
    private final String descripcion;
    private final boolean active;
    private final LocalDateTime createdAt;

    /**
     * Constructor privado - usar Builder para crear instancias
     */
    private Subcategory(Builder builder) {
        this.id = builder.id;
        this.categoryId = builder.categoryId;
        this.categoryName = builder.categoryName;
        this.nombre = builder.nombre;
        this.descripcion = builder.descripcion;
        this.active = builder.active;
        this.createdAt = builder.createdAt;
    }

    // ==================== GETTERS ====================

    public int getId() {
        return id;
    }

    public int getCategoryId() {
        return categoryId;
    }

    public String getCategoryName() {
        return categoryName;
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
     * Verifica si la subcategoría tiene descripción
     */
    public boolean hasDescripcion() {
        return descripcion != null && !descripcion.isBlank();
    }

    /**
     * Retorna el nombre completo incluyendo la categoría padre
     */
    public String getFullName() {
        if (categoryName != null && !categoryName.isBlank()) {
            return categoryName + " > " + nombre;
        }
        return nombre;
    }

    @Override
    public String toString() {
        return nombre;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Subcategory that = (Subcategory) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }

    // ==================== BUILDER ====================

    /**
     * Builder para construir instancias de Subcategory de forma fluida.
     */
    public static class Builder {
        private int id;
        private int categoryId;
        private String categoryName;
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

        public Builder categoryId(int categoryId) {
            this.categoryId = categoryId;
            return this;
        }

        public Builder categoryName(String categoryName) {
            this.categoryName = categoryName;
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
         * Construye la instancia de Subcategory después de validar los campos requeridos.
         *
         * @return nueva instancia de Subcategory
         * @throws IllegalArgumentException si la validación falla
         */
        public Subcategory build() {
            validate();
            return new Subcategory(this);
        }

        /**
         * Valida que los campos requeridos estén presentes
         */
        private void validate() {
            if (nombre == null || nombre.isBlank()) {
                throw new IllegalArgumentException("El nombre de la subcategoría es requerido");
            }
            if (nombre.length() > 100) {
                throw new IllegalArgumentException("El nombre no puede exceder 100 caracteres");
            }
            if (categoryId <= 0) {
                throw new IllegalArgumentException("La subcategoría debe tener una categoría padre válida");
            }
        }
    }
}
