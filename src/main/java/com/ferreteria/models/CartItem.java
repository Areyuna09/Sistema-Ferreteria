package com.ferreteria.models;

import java.math.BigDecimal;

/**
 * Represents an item in the shopping cart.
 */
public class CartItem {
    private ProductVariant variant;
    private int quantity;

    public CartItem(ProductVariant variant, int quantity) {
        this.variant = variant;
        this.quantity = quantity;
    }

    public ProductVariant getVariant() {
        return variant;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public void incrementQuantity(int amount) {
        this.quantity += amount;
    }

    public void decrementQuantity() {
        if (this.quantity > 1) {
            this.quantity--;
        }
    }

    public BigDecimal getSubtotal() {
        return variant.getSalePrice().multiply(BigDecimal.valueOf(quantity));
    }

    public int getVariantId() {
        return variant.getId();
    }

    public int getStock() {
        return variant.getStock();
    }
}
