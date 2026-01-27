package com.ferreteria.models;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Model representing a sale in the system.
 * Maps to the 'sales' table in the database.
 * Includes items (details) and associated payments.
 */
public class Sale {

    private final int id;
    private final int userId;
    private final BigDecimal total;
    private final String status;
    private final String notes;
    private final LocalDateTime createdAt;
    private final List<SaleItem> items;
    private final List<SalePayment> payments;

    // Additional field to display user info (not persisted)
    private final String userName;

    private Sale(Builder builder) {
        this.id = builder.id;
        this.userId = builder.userId;
        this.total = builder.total;
        this.status = builder.status;
        this.notes = builder.notes;
        this.createdAt = builder.createdAt;
        this.items = new ArrayList<>(builder.items);
        this.payments = new ArrayList<>(builder.payments);
        this.userName = builder.userName;
    }

    // Getters
    public int getId() { return id; }
    public int getUserId() { return userId; }
    public BigDecimal getTotal() { return total; }
    public String getStatus() { return status; }
    public String getNotes() { return notes; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public List<SaleItem> getItems() { return Collections.unmodifiableList(items); }
    public List<SalePayment> getPayments() { return Collections.unmodifiableList(payments); }
    public String getUserName() { return userName; }

    /**
     * Checks if the sale is completed.
     * @return true if status is 'completed'
     */
    public boolean isCompleted() {
        return "completed".equals(status);
    }

    /**
     * Checks if the sale was cancelled.
     * @return true if status is 'cancelled'
     */
    public boolean isCancelled() {
        return "cancelled".equals(status);
    }

    /**
     * Calculates total based on items.
     * @return sum of subtotals of all items
     */
    public BigDecimal calculateTotal() {
        return items.stream()
            .map(SaleItem::getSubtotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Gets total quantity of products sold.
     * @return sum of quantities of all items
     */
    public int getTotalItems() {
        return items.stream()
            .mapToInt(SaleItem::getQuantity)
            .sum();
    }

    /**
     * Calculates total paid amount.
     * @return sum of all payments
     */
    public BigDecimal getTotalPaid() {
        return payments.stream()
            .map(SalePayment::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Checks if the sale is fully paid.
     * @return true if total paid >= sale total
     */
    public boolean isPaid() {
        return getTotalPaid().compareTo(total) >= 0;
    }

    /**
     * Gets pending balance.
     * @return total - totalPaid
     */
    public BigDecimal getPendingBalance() {
        BigDecimal paid = getTotalPaid();
        BigDecimal pending = total.subtract(paid);
        return pending.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : pending;
    }

    // Builder Pattern
    public static class Builder {
        private int id;
        private int userId;
        private BigDecimal total = BigDecimal.ZERO;
        private String status = "completed";
        private String notes;
        private LocalDateTime createdAt = LocalDateTime.now();
        private List<SaleItem> items = new ArrayList<>();
        private List<SalePayment> payments = new ArrayList<>();
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

        public Builder items(List<SaleItem> items) {
            this.items = new ArrayList<>(items);
            return this;
        }

        public Builder addItem(SaleItem item) {
            this.items.add(item);
            return this;
        }

        public Builder payments(List<SalePayment> payments) {
            this.payments = new ArrayList<>(payments);
            return this;
        }

        public Builder addPayment(SalePayment payment) {
            this.payments.add(payment);
            return this;
        }

        public Builder userName(String userName) {
            this.userName = userName;
            return this;
        }

        public Sale build() {
            validate();
            return new Sale(this);
        }

        private void validate() {
            if (userId <= 0) {
                throw new IllegalArgumentException("User is required");
            }
            if (total.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Total cannot be negative");
            }
        }
    }
}
