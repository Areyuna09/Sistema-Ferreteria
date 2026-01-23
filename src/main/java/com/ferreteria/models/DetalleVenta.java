package com.ferreteria.models;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Modelo que representa un item/detalle de una venta.
 * Mapea a la tabla 'sale_items' en la base de datos.
 */
public class DetalleVenta {

    private final int id;
    private final int saleId;
    private final int variantId;
    private final int quantity;
    private final BigDecimal unitPrice;
    private final BigDecimal subtotal;
    private final LocalDateTime createdAt;

    // Campos adicionales para mostrar info del producto (no persisten)
    private final String productName;
    private final String variantName;

    private DetalleVenta(Builder builder) {
        this.id = builder.id;
        this.saleId = builder.saleId;
        this.variantId = builder.variantId;
        this.quantity = builder.quantity;
        this.unitPrice = builder.unitPrice;
        this.subtotal = builder.subtotal;
        this.createdAt = builder.createdAt;
        this.productName = builder.productName;
        this.variantName = builder.variantName;
    }

    // Getters
    public int getId() { return id; }
    public int getSaleId() { return saleId; }
    public int getVariantId() { return variantId; }
    public int getQuantity() { return quantity; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public BigDecimal getSubtotal() { return subtotal; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public String getProductName() { return productName; }
    public String getVariantName() { return variantName; }

    /**
     * Obtiene la descripción completa del item para mostrar.
     * @return nombre del producto con su variante
     */
    public String getDisplayName() {
        if (variantName != null && !variantName.isBlank()) {
            return productName + " - " + variantName;
        }
        return productName != null ? productName : "Producto #" + variantId;
    }

    /**
     * Calcula el subtotal basado en cantidad y precio unitario.
     * @return quantity * unitPrice
     */
    public BigDecimal calculateSubtotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }

    // Builder Pattern
    public static class Builder {
        private int id;
        private int saleId;
        private int variantId;
        private int quantity = 1;
        private BigDecimal unitPrice = BigDecimal.ZERO;
        private BigDecimal subtotal = BigDecimal.ZERO;
        private LocalDateTime createdAt = LocalDateTime.now();
        private String productName;
        private String variantName;

        public Builder id(int id) {
            this.id = id;
            return this;
        }

        public Builder saleId(int saleId) {
            this.saleId = saleId;
            return this;
        }

        public Builder variantId(int variantId) {
            this.variantId = variantId;
            return this;
        }

        public Builder quantity(int quantity) {
            this.quantity = quantity;
            return this;
        }

        public Builder unitPrice(BigDecimal unitPrice) {
            this.unitPrice = unitPrice;
            return this;
        }

        public Builder subtotal(BigDecimal subtotal) {
            this.subtotal = subtotal;
            return this;
        }

        public Builder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder productName(String productName) {
            this.productName = productName;
            return this;
        }

        public Builder variantName(String variantName) {
            this.variantName = variantName;
            return this;
        }

        /**
         * Calcula automáticamente el subtotal antes de construir.
         * @return this builder con subtotal calculado
         */
        public Builder calculateSubtotal() {
            this.subtotal = unitPrice.multiply(BigDecimal.valueOf(quantity));
            return this;
        }

        public DetalleVenta build() {
            validate();
            return new DetalleVenta(this);
        }

        private void validate() {
            if (variantId <= 0) {
                throw new IllegalArgumentException("Variante de producto es requerida");
            }
            if (quantity <= 0) {
                throw new IllegalArgumentException("Cantidad debe ser mayor a 0");
            }
            if (unitPrice.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Precio unitario no puede ser negativo");
            }
        }
    }
}
