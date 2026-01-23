package com.ferreteria.models;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Modelo que representa un pago asociado a una venta.
 * Permite pagos combinados (ej: parte efectivo, parte tarjeta).
 * Mapea a la tabla 'sale_payments' en la base de datos.
 */
public class PagoVenta {

    private final int id;
    private final int saleId;
    private final MetodoPago paymentMethod;
    private final BigDecimal amount;
    private final String reference;
    private final LocalDateTime createdAt;

    private PagoVenta(Builder builder) {
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
    public MetodoPago getPaymentMethod() { return paymentMethod; }
    public BigDecimal getAmount() { return amount; }
    public String getReference() { return reference; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    /**
     * Obtiene el nombre del método de pago para mostrar.
     * @return nombre legible del método de pago
     */
    public String getPaymentMethodDisplayName() {
        return paymentMethod.getDisplayName();
    }

    /**
     * Enum para los métodos de pago soportados.
     */
    public enum MetodoPago {
        EFECTIVO("efectivo", "Efectivo"),
        TARJETA_DEBITO("tarjeta_debito", "Tarjeta de Débito"),
        TARJETA_CREDITO("tarjeta_credito", "Tarjeta de Crédito"),
        TRANSFERENCIA("transferencia", "Transferencia Bancaria"),
        MERCADO_PAGO("mercado_pago", "Mercado Pago"),
        CUENTA_CORRIENTE("cuenta_corriente", "Cuenta Corriente"),
        OTRO("otro", "Otro");

        private final String value;
        private final String displayName;

        MetodoPago(String value, String displayName) {
            this.value = value;
            this.displayName = displayName;
        }

        public String getValue() { return value; }
        public String getDisplayName() { return displayName; }

        public static MetodoPago fromValue(String value) {
            for (MetodoPago m : values()) {
                if (m.value.equalsIgnoreCase(value)) {
                    return m;
                }
            }
            return OTRO;
        }
    }

    // Builder Pattern
    public static class Builder {
        private int id;
        private int saleId;
        private MetodoPago paymentMethod = MetodoPago.EFECTIVO;
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

        public Builder paymentMethod(MetodoPago method) {
            this.paymentMethod = method;
            return this;
        }

        public Builder paymentMethod(String method) {
            this.paymentMethod = MetodoPago.fromValue(method);
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

        public PagoVenta build() {
            validate();
            return new PagoVenta(this);
        }

        private void validate() {
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Monto debe ser mayor a 0");
            }
            if (paymentMethod == null) {
                throw new IllegalArgumentException("Método de pago es requerido");
            }
        }
    }
}
