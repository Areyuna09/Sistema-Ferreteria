package com.ferreteria.utils;

import com.ferreteria.models.*;

import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Formateador de datos para el Debug Panel.
 */
public class DebugFormatter {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public static String formatSales(List<Sale> sales) {
        if (sales.isEmpty()) return "No se encontraron ventas";

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%-8s %-20s %-15s %-12s %-10s\n", "ID", "Fecha", "Total", "Items", "Estado"));
        sb.append("─".repeat(70)).append("\n");

        for (Sale s : sales) {
            sb.append(String.format("%-8d %-20s $%-14s %-12d %-10s\n",
                s.getId(),
                s.getCreatedAt() != null ? s.getCreatedAt().format(DATE_FORMAT) : "-",
                String.format("%,.2f", s.getTotal()),
                s.getTotalItems(),
                s.isCompleted() ? "OK" : "ANULADA"
            ));
        }

        sb.append("─".repeat(70)).append("\n");
        sb.append("Total: ").append(sales.size()).append(" ventas");
        return sb.toString();
    }

    public static String formatSaleDetail(Sale s) {
        StringBuilder sb = new StringBuilder();
        sb.append("VENTA #").append(s.getId()).append("\n");
        sb.append("─".repeat(40)).append("\n");
        sb.append("Fecha: ").append(s.getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"))).append("\n");
        sb.append("Usuario: ").append(s.getUserName() != null ? s.getUserName() : "ID " + s.getUserId()).append("\n");
        sb.append("Estado: ").append(s.isCompleted() ? "Completada" : "Anulada").append("\n");
        sb.append("Notas: ").append(s.getNotes() != null ? s.getNotes() : "-").append("\n\n");

        sb.append("ITEMS:\n");
        for (SaleItem item : s.getItems()) {
            sb.append(String.format("  • %s x%d = $%,.2f\n",
                item.getDisplayName(), item.getQuantity(), item.getSubtotal()));
        }

        sb.append("\nPAGOS:\n");
        for (SalePayment payment : s.getPayments()) {
            sb.append(String.format("  • %s: $%,.2f\n",
                payment.getPaymentMethodDisplayName(), payment.getAmount()));
        }

        sb.append("\n").append("─".repeat(40)).append("\n");
        sb.append("TOTAL: $").append(String.format("%,.2f", s.getTotal()));
        return sb.toString();
    }

    public static String formatVariantes(List<ProductVariant> variantes) {
        if (variantes.isEmpty()) return "No se encontraron productos";

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%-6s %-30s %-15s %-10s %-8s\n", "ID", "Producto", "SKU", "Precio", "Stock"));
        sb.append("─".repeat(75)).append("\n");

        for (ProductVariant v : variantes) {
            String nombre = v.getDisplayName();
            if (nombre.length() > 28) nombre = nombre.substring(0, 25) + "...";

            sb.append(String.format("%-6d %-30s %-15s $%-9s %-8d\n",
                v.getId(), nombre,
                v.getSku() != null ? v.getSku() : "-",
                String.format("%,.2f", v.getSalePrice()),
                v.getStock()
            ));
        }

        sb.append("─".repeat(75)).append("\n");
        sb.append("Total: ").append(variantes.size()).append(" productos");
        return sb.toString();
    }

    public static String formatVarianteDetalle(ProductVariant v) {
        StringBuilder sb = new StringBuilder();
        sb.append("PRODUCTO #").append(v.getId()).append("\n");
        sb.append("─".repeat(40)).append("\n");
        sb.append("Nombre: ").append(v.getDisplayName()).append("\n");
        sb.append("SKU: ").append(v.getSku()).append("\n");
        sb.append("Precio costo: $").append(String.format("%,.2f", v.getCostPrice())).append("\n");
        sb.append("Precio venta: $").append(String.format("%,.2f", v.getSalePrice())).append("\n");
        sb.append("Stock actual: ").append(v.getStock()).append("\n");
        sb.append("Stock mínimo: ").append(v.getMinStock()).append("\n");
        return sb.toString();
    }

    public static String formatUsuarios(List<User> usuarios) {
        if (usuarios.isEmpty()) return "No se encontraron usuarios";

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%-6s %-20s %-25s %-15s %-8s\n", "ID", "Username", "Nombre", "Rol", "Activo"));
        sb.append("─".repeat(80)).append("\n");

        for (User u : usuarios) {
            sb.append(String.format("%-6d %-20s %-25s %-15s %-8s\n",
                u.getId(), u.getUsername(), u.getFullName(),
                u.getRole().getValue(), u.isActive() ? "Sí" : "No"
            ));
        }

        sb.append("─".repeat(80)).append("\n");
        sb.append("Total: ").append(usuarios.size()).append(" usuarios");
        return sb.toString();
    }

    public static String formatUsuarioDetalle(User u) {
        StringBuilder sb = new StringBuilder();
        sb.append("USUARIO #").append(u.getId()).append("\n");
        sb.append("─".repeat(40)).append("\n");
        sb.append("Username: ").append(u.getUsername()).append("\n");
        sb.append("Nombre: ").append(u.getFullName()).append("\n");
        sb.append("Rol: ").append(u.getRole().getValue()).append("\n");
        sb.append("Activo: ").append(u.isActive() ? "Sí" : "No").append("\n");
        sb.append("Creado: ").append(u.getCreatedAt() != null ?
            u.getCreatedAt().format(DATE_FORMAT) : "-").append("\n");
        return sb.toString();
    }
}
