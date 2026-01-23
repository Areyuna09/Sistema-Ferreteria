package com.ferreteria.models;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Model representing a payment associated with a sale.
 * Allows combined payments (e.g., part cash, part card).
 * Maps to the 'sale_payments' table in the database.
 */
public class SalePayment {

    private final int id;
    private final int saleId;
    private final PaymentMethod paymentMethod;
    private final BigDecimal amount;
    private final String reference;
    private final LocalDateTime createdAt;

    private SalePayment(Builder builder) {
        this.id = builder.id;
        this.saleId = builder.saleId;
        this.paymentMethod = builder.paymentMethod;
        this.amount = builder.amount;
        this.reference = builder.reference;
        this.createdAt = builder.createdAt;
    }

    // Getters
    public int getId() { return id; }
    public int getSaleId() { return saleId; }
    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public BigDecimal getAmount() { return amount; }
    public String getReference() { return reference; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    /**
     * Gets the payment method name for display.
     * @return readable payment method name
     */
    public String getPaymentMethodDisplayName() {
        return paymentMethod.getDisplayName();
    }

    /**
     * Enum for supported payment methods.
     */
    public enum PaymentMethod {
        CASH("efectivo", "Efectivo"),
        DEBIT_CARD("tarjeta_debito", "Tarjeta de Débito"),
        CREDIT_CARD("tarjeta_credito", "Tarjeta de Crédito"),
        TRANSFER("transferencia", "Transferencia Bancaria"),
        MERCADO_PAGO("mercado_pago", "Mercado Pago"),
        CREDIT_ACCOUNT("cuenta_corriente", "Cuenta Corriente"),
        OTHER("otro", "Otro");

        private final String value;
        private final String displayName;

        PaymentMethod(String value, String displayName) {
            this.value = value;
            this.displayName = displayName;
        }

        public String getValue() { return value; }
        public String getDisplayName() { return displayName; }

        public static PaymentMethod fromValue(String value) {
            for (PaymentMethod m : values()) {
                if (m.value.equalsIgnoreCase(value)) {
                    return m;
                }
            }
            return OTHER;
        }
    }

    // Builder Pattern
    public static class Builder {
        private int id;
        private int saleId;
        private PaymentMethod paymentMethod = PaymentMethod.CASH;
        private BigDecimal amount = BigDecimal.ZERO;
        private String reference;
        private LocalDateTime createdAt = LocalDateTime.now();

        public Builder id(int id) {
            this.id = id;
            return this;
        }

        public Builder saleId(int saleId) {
            this.saleId = saleId;
            return this;
        }

        public Builder paymentMethod(PaymentMethod method) {
            this.paymentMethod = method;
            return this;
        }

        public Builder paymentMethod(String method) {
            this.paymentMethod = PaymentMethod.fromValue(method);
            return this;
        }

        public Builder amount(BigDecimal amount) {
            this.amount = amount;
            return this;
        }

        public Builder reference(String reference) {
            this.reference = reference;
            return this;
        }

        public Builder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public SalePayment build() {
            validate();
            return new SalePayment(this);
        }

        private void validate() {
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Amount must be greater than 0");
            }
            if (paymentMethod == null) {
                throw new IllegalArgumentException("Payment method is required");
            }
        }
    }
}
