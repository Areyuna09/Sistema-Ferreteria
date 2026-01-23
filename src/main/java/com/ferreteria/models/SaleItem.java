package com.ferreteria.models;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Model representing an item/detail of a sale.
 * Maps to the 'sale_items' table in the database.
 */
public class SaleItem {

    private final int id;
    private final int saleId;
    private final int variantId;
    private final int quantity;
    private final BigDecimal unitPrice;
    private final BigDecimal subtotal;
    private final LocalDateTime createdAt;

    // Additional fields to display product info (not persisted)
    private final String productName;
    private final String variantName;

    private SaleItem(Builder builder) {
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
     * Gets the complete item description for display.
     * @return product name with its variant
     */
    public String getDisplayName() {
        if (variantName != null && !variantName.isBlank()) {
            return productName + " - " + variantName;
        }
        return productName != null ? productName : "Product #" + variantId;
    }

    /**
     * Calculates subtotal based on quantity and unit price.
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
         * Automatically calculates subtotal before building.
         * @return this builder with calculated subtotal
         */
        public Builder calculateSubtotal() {
            this.subtotal = unitPrice.multiply(BigDecimal.valueOf(quantity));
            return this;
        }

        public SaleItem build() {
            validate();
            return new SaleItem(this);
        }

        private void validate() {
            if (variantId <= 0) {
                throw new IllegalArgumentException("Product variant is required");
            }
            if (quantity <= 0) {
                throw new IllegalArgumentException("Quantity must be greater than 0");
            }
            if (unitPrice.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Unit price cannot be negative");
            }
        }
    }
}
