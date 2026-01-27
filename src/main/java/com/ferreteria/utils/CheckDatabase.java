package com.ferreteria.utils;

import com.ferreteria.models.dao.DatabaseConfig;
import java.io.File;
import java.sql.*;

/**
 * Verifica la existencia y contenido de la base de datos.
 */
public class CheckDatabase {

    public static void main(String[] args) {
        DatabaseConfig dbConfig = DatabaseConfig.getInstance();
        String dbPath = dbConfig.getDbPath();
        File dbFile = new File(dbPath);

        System.out.println("===========================================");
        System.out.println("  Verificación de Base de Datos");
        System.out.println("===========================================\n");

        System.out.println("Ruta: " + dbPath);
        System.out.println("Existe: " + (dbFile.exists() ? "SÍ" : "NO"));

        if (dbFile.exists()) {
            System.out.println("Tamaño: " + formatFileSize(dbFile.length()));
            System.out.println("Última modificación: " + new java.util.Date(dbFile.lastModified()));
            System.out.println();

            // Verificar contenido
            try (Connection conn = dbConfig.getConnection()) {
                System.out.println("===========================================");
                System.out.println("  Contenido de la Base de Datos");
                System.out.println("===========================================\n");

                checkTable(conn, "users", "Usuarios");
                checkTable(conn, "business_config", "Configuración del negocio");
                checkTable(conn, "categories", "Categorías");
                checkTable(conn, "products", "Productos");
                checkTable(conn, "product_variants", "Variantes de productos");
                checkTable(conn, "sales", "Ventas");
                checkTable(conn, "sale_items", "Items de ventas");
                checkTable(conn, "sale_payments", "Pagos");

                System.out.println("\n===========================================");
                System.out.println("  Resumen de Ventas");
                System.out.println("===========================================\n");

                // Ventas por mes
                String sql = "SELECT " +
                            "strftime('%Y-%m', created_at) as mes, " +
                            "COUNT(*) as cantidad, " +
                            "SUM(total) as total " +
                            "FROM sales " +
                            "GROUP BY mes " +
                            "ORDER BY mes DESC";

                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery(sql)) {

                    if (!rs.next()) {
                        System.out.println("⚠ No hay ventas registradas");
                    } else {
                        System.out.printf("%-10s %10s %15s%n", "Mes", "Ventas", "Total");
                        System.out.println("─────────────────────────────────────────");
                        do {
                            System.out.printf("%-10s %10d $%,14.2f%n",
                                rs.getString("mes"),
                                rs.getInt("cantidad"),
                                rs.getDouble("total"));
                        } while (rs.next());
                    }
                }

                System.out.println("\n===========================================");
                System.out.println("✓ Verificación completada");
                System.out.println("===========================================");

            } catch (SQLException e) {
                System.err.println("\n✗ Error al conectar a la BD: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            System.out.println("\n⚠ La base de datos NO existe");
            System.out.println("\nPara crear la base de datos, ejecuta:");
            System.out.println("  mvn javafx:run");
            System.out.println("\nO para crear con datos de prueba:");
            System.out.println("  reiniciar-bd.bat");
        }
    }

    private static void checkTable(Connection conn, String tableName, String description) {
        try {
            String sql = "SELECT COUNT(*) as count FROM " + tableName;
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                if (rs.next()) {
                    int count = rs.getInt("count");
                    String status = count > 0 ? "✓" : "⚠";
                    System.out.printf("%s %-30s: %d registros%n", status, description, count);
                }
            }
        } catch (SQLException e) {
            System.out.printf("✗ %-30s: Error - %s%n", description, e.getMessage());
        }
    }

    private static String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " bytes";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "";
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }
}
