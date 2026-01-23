package com.ferreteria.models;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Modelo que representa una venta en el sistema.
 * Mapea a la tabla 'sales' en la base de datos.
 * Incluye items (detalles) y pagos asociados.
 */
public class Venta {

    private final int id;
    private final int userId;
    private final BigDecimal total;
    private final String status;
    private final String notes;
    private final LocalDateTime createdAt;
    private final List<DetalleVenta> items;
    private final List<PagoVenta> pagos;

    // Campo adicional para mostrar info del usuario (no persiste)
    private final String userName;

    private Venta(Builder builder) {
        this.id = builder.id;
        this.userId = builder.userId;
        this.total = builder.total;
        this.status = builder.status;
        this.notes = builder.notes;
        this.createdAt = builder.createdAt;
        this.items = new ArrayList<>(builder.items);
        this.pagos = new ArrayList<>(builder.pagos);
        this.userName = builder.userName;
    }

    // Getters
    public int getId() { return id; }
    public int getUserId() { return userId; }
    public BigDecimal getTotal() { return total; }
    public String getStatus() { return status; }
    public String getNotes() { return notes; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public List<DetalleVenta> getItems() { return Collections.unmodifiableList(items); }
    public List<PagoVenta> getPagos() { return Collections.unmodifiableList(pagos); }
    public String getUserName() { return userName; }

    /**
     * Verifica si la venta está completada.
     * @return true si el status es 'completed'
     */
    public boolean isCompleted() {
        return "completed".equals(status);
    }

    /**
     * Verifica si la venta fue anulada.
     * @return true si el status es 'cancelled'
     */
    public boolean isCancelled() {
        return "cancelled".equals(status);
    }

    /**
     * Calcula el total basado en los items.
     * @return suma de subtotales de todos los items
     */
    public BigDecimal calculateTotal() {
        return items.stream()
            .map(DetalleVenta::getSubtotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Obtiene la cantidad total de productos vendidos.
     * @return suma de cantidades de todos los items
     */
    public int getTotalItems() {
        return items.stream()
            .mapToInt(DetalleVenta::getQuantity)
            .sum();
    }

    /**
     * Calcula el total pagado.
     * @return suma de todos los pagos
     */
    public BigDecimal getTotalPagado() {
        return pagos.stream()
            .map(PagoVenta::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Verifica si la venta está completamente pagada.
     * @return true si el total pagado >= total de la venta
     */
    public boolean estaPagada() {
        return getTotalPagado().compareTo(total) >= 0;
    }

    /**
     * Obtiene el saldo pendiente.
     * @return total - totalPagado
     */
    public BigDecimal getSaldoPendiente() {
        BigDecimal pagado = getTotalPagado();
        BigDecimal pendiente = total.subtract(pagado);
        return pendiente.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : pendiente;
    }

    // Builder Pattern
    public static class Builder {
        private int id;
        private int userId;
        private BigDecimal total = BigDecimal.ZERO;
        private String status = "completed";
        private String notes;
        private LocalDateTime createdAt = LocalDateTime.now();
        private List<DetalleVenta> items = new ArrayList<>();
        private List<PagoVenta> pagos = new ArrayList<>();
        private String userName;

        public Builder id(int id) {
            this.id = id;
            return this;
        }

        public Builder userId(int userId) {
            this.userId = userId;
            return this;
        }

        public Builder total(BigDecimal total) {
            this.total = total;
            return this;
        }

        public Builder status(String status) {
            this.status = status;
            return this;
        }

        public Builder notes(String notes) {
            this.notes = notes;
            return this;
        }

        public Builder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder items(List<DetalleVenta> items) {
            this.items = new ArrayList<>(items);
            return this;
        }

        public Builder addItem(DetalleVenta item) {
            this.items.add(item);
            return this;
        }

        public Builder pagos(List<PagoVenta> pagos) {
            this.pagos = new ArrayList<>(pagos);
            return this;
        }

        public Builder addPago(PagoVenta pago) {
            this.pagos.add(pago);
            return this;
        }

        public Builder userName(String userName) {
            this.userName = userName;
            return this;
        }

        public Venta build() {
            validate();
            return new Venta(this);
        }

        private void validate() {
            if (userId <= 0) {
                throw new IllegalArgumentException("Usuario es requerido");
            }
            if (total.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Total no puede ser negativo");
            }
        }
    }
}
