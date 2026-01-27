package com.ferreteria.models;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Representa un pago asociado a una venta.
 * Soporta multiples metodos de pago por venta.
 * 
 * @author Sistema Ferreteria
 * @version 1.0
 */
public class SalePayment {
    private Integer id;
    private Integer saleId;
    private String paymentMethod;
    private BigDecimal amount;
    private String reference;
    private LocalDateTime createdAt;

    public SalePayment() {
        this.amount = BigDecimal.ZERO;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getSaleId() {
        return saleId;
    }

    public void setSaleId(Integer saleId) {
        this.saleId = saleId;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getFormattedPaymentMethod() {
        if (paymentMethod == null) {
            return "";
        }
        
        switch (paymentMethod.toLowerCase()) {
            case "efectivo":
                return "Efectivo";
            case "tarjeta_debito":
                return "Tarjeta Debito";
            case "tarjeta_credito":
                return "Tarjeta Credito";
            case "transferencia":
                return "Transferencia";
            default:
                return paymentMethod;
        }
    }

    @Override
    public String toString() {
        return getFormattedPaymentMethod() + ": $" + amount;
    }
}