package com.ferreteria.utils;

import com.ferreteria.models.dao.DatabaseConfig;
import java.sql.*;

/**
 * Verifica que los datos de prueba se hayan insertado correctamente.
 */
public class VerifyTestData {

    public static void main(String[] args) {
        DatabaseConfig dbConfig = DatabaseConfig.getInstance();

        System.out.println("===========================================");
        System.out.println("  Verificación de Datos de Prueba");
        System.out.println("===========================================\n");
        System.out.println("Base de datos: " + dbConfig.getDbPath() + "\n");

        try (Connection conn = dbConfig.getConnection()) {

            // Verificar conteo de registros
            System.out.println("CONTEO DE REGISTROS:");
            System.out.println("─────────────────────────────────────────");
            printCount(conn, "users", "Usuarios");
            printCount(conn, "categories", "Categorías");
            printCount(conn, "products", "Productos");
            printCount(conn, "product_variants", "Variantes");
            printCount(conn, "sales", "Ventas");
            printCount(conn, "sale_items", "Items de venta");
            printCount(conn, "sale_payments", "Pagos");

            // Verificar ventas por fecha
            System.out.println("\nVENTAS RECIENTES:");
            System.out.println("─────────────────────────────────────────");
            String sql = "SELECT " +
                        "DATE(created_at) as fecha, " +
                        "COUNT(*) as cantidad, " +
                        "SUM(total) as total " +
                        "FROM sales " +
                        "GROUP BY DATE(created_at) " +
                        "ORDER BY fecha DESC " +
                        "LIMIT 10";

            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {

                System.out.printf("%-15s %-10s %15s%n", "Fecha", "Ventas", "Total");
                System.out.println("─────────────────────────────────────────");

                while (rs.next()) {
                    System.out.printf("%-15s %-10d $%,14.2f%n",
                        rs.getString("fecha"),
                        rs.getInt("cantidad"),
                        rs.getDouble("total"));
                }
            }

            // Verificar métodos de pago
            System.out.println("\nMÉTODOS DE PAGO:");
            System.out.println("─────────────────────────────────────────");
            sql = "SELECT " +
                  "payment_method, " +
                  "COUNT(*) as cantidad, " +
                  "SUM(amount) as total " +
                  "FROM sale_payments " +
                  "GROUP BY payment_method";

            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {

                System.out.printf("%-20s %-10s %15s%n", "Método", "Usos", "Total");
                System.out.println("─────────────────────────────────────────");

                while (rs.next()) {
                    System.out.printf("%-20s %-10d $%,14.2f%n",
                        rs.getString("payment_method"),
                        rs.getInt("cantidad"),
                        rs.getDouble("total"));
                }
            }

            System.out.println("\n===========================================");
            System.out.println("✓ Verificación completada");
            System.out.println("===========================================");

        } catch (SQLException e) {
            System.err.println("Error al verificar datos: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void printCount(Connection conn, String table, String label) throws SQLException {
        String sql = "SELECT COUNT(*) as count FROM " + table;
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                System.out.printf("%-20s: %d%n", label, rs.getInt("count"));
            }
        }
    }
}
