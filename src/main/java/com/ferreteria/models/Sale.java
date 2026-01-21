package com.ferreteria.models;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Modelo que representa una venta en el sistema.
 * Incluye items vendidos, pagos y totales.
 * 
 * @author Sistema Ferreteria
 * @version 1.0
 */
public class Sale {
    private Integer id;
    private Integer userId;
    private String userName;
    private BigDecimal total;
    private String status;
    private String notes;
    private LocalDateTime createdAt;
    private List<SaleItem> items;
    private List<SalePayment> payments;

    public Sale() {
        this.items = new ArrayList<>();
        this.payments = new ArrayList<>();
        this.total = BigDecimal.ZERO;
        this.status = "completed";
    }

    public static class Builder {
        private Sale sale;

        public Builder() {
            this.sale = new Sale();
        }

        public Builder id(Integer id) {
            sale.id = id;
            return this;
        }

        public Builder userId(Integer userId) {
            sale.userId = userId;
            return this;
        }

        public Builder userName(String userName) {
            sale.userName = userName;
            return this;
        }

        public Builder total(BigDecimal total) {
            sale.total = total;
            return this;
        }

        public Builder status(String status) {
            sale.status = status;
            return this;
        }

        public Builder notes(String notes) {
            sale.notes = notes;
            return this;
        }

        public Builder createdAt(LocalDateTime createdAt) {
            sale.createdAt = createdAt;
            return this;
        }

        public Builder items(List<SaleItem> items) {
            sale.items = items;
            return this;
        }

        public Builder payments(List<SalePayment> payments) {
            sale.payments = payments;
            return this;
        }

        public Sale build() {
            return sale;
        }
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public List<SaleItem> getItems() {
        return items;
    }

    public void setItems(List<SaleItem> items) {
        this.items = items;
    }

    public List<SalePayment> getPayments() {
        return payments;
    }

    public void setPayments(List<SalePayment> payments) {
        this.payments = payments;
    }

    public void addItem(SaleItem item) {
        this.items.add(item);
    }

    public void addPayment(SalePayment payment) {
        this.payments.add(payment);
    }

    @Override
    public String toString() {
        return "Sale{" +
                "id=" + id +
                ", userId=" + userId +
                ", total=" + total +
                ", status='" + status + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}