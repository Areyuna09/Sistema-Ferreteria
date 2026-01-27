package com.ferreteria.models;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Modelo que representa una variante de producto.
 * Cada producto puede tener múltiples variantes (tamaños, colores, etc.).
 * El precio y stock se manejan a nivel de variante.
 */
public class ProductVariant {

    private final int id;
    private final int productId;
    private final String sku;
    private final String variantName;
    private final BigDecimal costPrice;
    private final BigDecimal salePrice;
    private final int stock;
    private final int minStock;
    private final boolean active;
    private final LocalDateTime createdAt;

    // Campos adicionales para mostrar (no persisten)
    private final String productName;
    private final String productCode;
    private final String categoryName;

    private ProductVariant(Builder builder) {
        this.id = builder.id;
        this.productId = builder.productId;
        this.sku = builder.sku;
        this.variantName = builder.variantName;
        this.costPrice = builder.costPrice;
        this.salePrice = builder.salePrice;
        this.stock = builder.stock;
        this.minStock = builder.minStock;
        this.active = builder.active;
        this.createdAt = builder.createdAt;
        this.productName = builder.productName;
        this.productCode = builder.productCode;
        this.categoryName = builder.categoryName;
    }

    // Getters
    public int getId() { return id; }
    public int getProductId() { return productId; }
    public String getSku() { return sku; }
    public String getVariantName() { return variantName; }
    public BigDecimal getCostPrice() { return costPrice; }
    public BigDecimal getSalePrice() { return salePrice; }
    public int getStock() { return stock; }
    public int getMinStock() { return minStock; }
    public boolean isActive() { return active; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public String getProductName() { return productName; }
    public String getProductCode() { return productCode; }
    public String getCategoryName() { return categoryName; }

    /**
     * Obtiene el nombre completo para mostrar.
     * @return "Producto - Variante" o solo "Producto" si no hay variante
     */
    public String getDisplayName() {
        if (variantName != null && !variantName.isBlank() && !variantName.equals("Default")) {
            return productName + " - " + variantName;
        }
        return productName != null ? productName : "Variante #" + id;
    }

    /**
     * Verifica si tiene stock bajo.
     * @return true si stock <= minStock
     */
    public boolean isLowStock() {
        return stock <= minStock;
    }

    /**
     * Verifica si hay stock disponible.
     * @return true si stock > 0
     */
    public boolean hasStock() {
        return stock > 0;
    }

    /**
     * Calcula el margen de ganancia.
     * @return salePrice - costPrice
     */
    public BigDecimal getProfit() {
        return salePrice.subtract(costPrice);
    }

    /**
     * Calcula el porcentaje de margen.
     * @return (profit / costPrice) * 100
     */
    public BigDecimal getProfitPercentage() {
        if (costPrice.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return getProfit()
            .divide(costPrice, 4, java.math.RoundingMode.HALF_UP)
            .multiply(BigDecimal.valueOf(100));
    }

    // Builder Pattern
    public static class Builder {
        private int id;
        private int productId;
        private String sku;
        private String variantName = "Default";
        private BigDecimal costPrice = BigDecimal.ZERO;
        private BigDecimal salePrice = BigDecimal.ZERO;
        private int stock = 0;
        private int minStock = 5;
        private boolean active = true;
        private LocalDateTime createdAt = LocalDateTime.now();
        private String productName;
        private String productCode;
        private String categoryName;

        public Builder id(int id) { this.id = id; return this; }
        public Builder productId(int productId) { this.productId = productId; return this; }
        public Builder sku(String sku) { this.sku = sku; return this; }
        public Builder variantName(String name) { this.variantName = name; return this; }
        public Builder costPrice(BigDecimal price) { this.costPrice = price; return this; }
        public Builder salePrice(BigDecimal price) { this.salePrice = price; return this; }
        public Builder stock(int stock) { this.stock = stock; return this; }
        public Builder minStock(int min) { this.minStock = min; return this; }
        public Builder active(boolean active) { this.active = active; return this; }
        public Builder createdAt(LocalDateTime dt) { this.createdAt = dt; return this; }
        public Builder productName(String name) { this.productName = name; return this; }
        public Builder productCode(String code) { this.productCode = code; return this; }
        public Builder categoryName(String name) { this.categoryName = name; return this; }

        public ProductVariant build() {
            validate();
            return new ProductVariant(this);
        }

        private void validate() {
            if (productId <= 0 && id <= 0) {
                throw new IllegalArgumentException("Producto es requerido");
            }
            if (salePrice.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Precio de venta no puede ser negativo");
            }
        }
    }
}
