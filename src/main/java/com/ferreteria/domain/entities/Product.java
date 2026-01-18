package com.ferreteria.domain.entities;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entidad de dominio que representa un producto del inventario.
 */
public class Product {

    private final int id;
    private final String code;
    private final String name;
    private final String description;
    private final String category;
    private final BigDecimal price;
    private final BigDecimal cost;
    private final int stock;
    private final int minStock;
    private final String location;
    private final boolean active;
    private final LocalDateTime createdAt;

    private Product(Builder builder) {
        this.id = builder.id;
        this.code = builder.code;
        this.name = builder.name;
        this.description = builder.description;
        this.category = builder.category;
        this.price = builder.price;
        this.cost = builder.cost;
        this.stock = builder.stock;
        this.minStock = builder.minStock;
        this.location = builder.location;
        this.active = builder.active;
        this.createdAt = builder.createdAt;
    }

    // Getters
    public int getId() { return id; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getCategory() { return category; }
    public BigDecimal getPrice() { return price; }
    public BigDecimal getCost() { return cost; }
    public int getStock() { return stock; }
    public int getMinStock() { return minStock; }
    public String getLocation() { return location; }
    public boolean isActive() { return active; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public boolean isLowStock() {
        return stock <= minStock;
    }

    public BigDecimal getProfit() {
        return price.subtract(cost);
    }

    // Builder Pattern
    public static class Builder {
        private int id;
        private String code;
        private String name;
        private String description;
        private String category;
        private BigDecimal price = BigDecimal.ZERO;
        private BigDecimal cost = BigDecimal.ZERO;
        private int stock = 0;
        private int minStock = 5;
        private String location;
        private boolean active = true;
        private LocalDateTime createdAt = LocalDateTime.now();

        public Builder id(int id) { this.id = id; return this; }
        public Builder code(String code) { this.code = code; return this; }
        public Builder name(String name) { this.name = name; return this; }
        public Builder description(String desc) { this.description = desc; return this; }
        public Builder category(String cat) { this.category = cat; return this; }
        public Builder price(BigDecimal price) { this.price = price; return this; }
        public Builder cost(BigDecimal cost) { this.cost = cost; return this; }
        public Builder stock(int stock) { this.stock = stock; return this; }
        public Builder minStock(int min) { this.minStock = min; return this; }
        public Builder location(String loc) { this.location = loc; return this; }
        public Builder active(boolean active) { this.active = active; return this; }
        public Builder createdAt(LocalDateTime dt) { this.createdAt = dt; return this; }

        public Product build() {
            validate();
            return new Product(this);
        }

        private void validate() {
            if (name == null || name.isBlank()) {
                throw new IllegalArgumentException("Nombre es requerido");
            }
            if (price.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Precio no puede ser negativo");
            }
        }
    }
}
