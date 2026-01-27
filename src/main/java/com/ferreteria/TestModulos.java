package com.ferreteria;

import com.ferreteria.models.*;
import com.ferreteria.models.dao.*;

import java.math.BigDecimal;
import java.sql.*;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

/**
 * Test rápido para verificar módulos de Productos y Reportes.
 */
public class TestModulos {

    public static void main(String[] args) {
        System.out.println("===========================================");
        System.out.println("   TEST DE MÓDULOS: PRODUCTOS Y REPORTES");
        System.out.println("===========================================\n");

        try {
            // Inicializar
            DatabaseConfig dbConfig = DatabaseConfig.getInstance();
            DatabaseInitializer initializer = new DatabaseInitializer(dbConfig);
            initializer.initialize();

            testProductos(dbConfig);
            testReportes();

            System.out.println("\n===========================================");
            System.out.println("   TODOS LOS TESTS PASARON ✓");
            System.out.println("===========================================");

        } catch (Exception e) {
            System.err.println("\n❌ ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void testProductos(DatabaseConfig dbConfig) throws SQLException {
        System.out.println("► TEST: Módulo de Productos...\n");

        ProductVariantDAO variantDAO = new ProductVariantDAO(dbConfig);

        // Test listar
        List<ProductVariant> disponibles = variantDAO.listarDisponibles();
        System.out.println("  Productos disponibles: " + disponibles.size());

        // Test consulta directa (como hace ProductsController)
        Connection conn = dbConfig.getConnection();
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery("""
            SELECT p.id, p.code, p.name, c.name as category,
                   COALESCE(pv.sale_price, 0) as price,
                   COALESCE(pv.stock, 0) as stock
            FROM products p
            LEFT JOIN categories c ON p.category_id = c.id
            LEFT JOIN product_variants pv ON p.id = pv.product_id AND pv.active = 1
            WHERE p.active = 1
            ORDER BY p.name
            """);

        int count = 0;
        while (rs.next()) {
            count++;
            if (count <= 3) {
                System.out.printf("    [%d] %s - %s | $%.2f | Stock: %d%n",
                    rs.getInt("id"),
                    rs.getString("code"),
                    rs.getString("name"),
                    rs.getBigDecimal("price"),
                    rs.getInt("stock"));
            }
        }
        if (count > 3) {
            System.out.println("    ... y " + (count - 3) + " más");
        }

        System.out.println("  ✓ Productos cargados correctamente: " + count + "\n");
    }

    private static void testReportes() {
        System.out.println("► TEST: Módulo de Reportes...\n");

        ReportDAO reportDAO = new ReportDAO();
        YearMonth mesActual = YearMonth.now();

        // Test estadísticas mensuales
        Map<String, Object> stats = reportDAO.getMonthlyStats(mesActual);
        System.out.println("  Estadísticas de " + mesActual + ":");
        System.out.printf("    - Total ventas: %d%n", stats.get("totalVentas"));
        System.out.printf("    - Total recaudado: $%s%n", stats.get("totalRecaudado"));
        System.out.printf("    - Promedio: $%s%n", stats.get("promedioVenta"));
        System.out.printf("    - Venta máxima: $%s%n", stats.get("ventaMaxima"));

        // Test métodos de pago
        Map<String, BigDecimal> pagos = reportDAO.getPaymentMethodTotals(mesActual);
        System.out.println("\n  Métodos de pago:");
        if (pagos.isEmpty()) {
            System.out.println("    (sin datos este mes)");
        } else {
            for (Map.Entry<String, BigDecimal> entry : pagos.entrySet()) {
                System.out.printf("    - %s: $%s%n", entry.getKey(), entry.getValue());
            }
        }

        // Test productos vendidos
        List<Map<String, Object>> productos = reportDAO.getProductSalesSummary(mesActual);
        System.out.println("\n  Productos vendidos este mes: " + productos.size());
        for (int i = 0; i < Math.min(3, productos.size()); i++) {
            Map<String, Object> p = productos.get(i);
            System.out.printf("    - %s (%s) x%d = $%s%n",
                p.get("producto"), p.get("variante"), p.get("cantidad"), p.get("total"));
        }

        // Test ventas diarias
        Map<Integer, BigDecimal> diarias = reportDAO.getDailySales(mesActual);
        System.out.println("\n  Días con ventas: " + diarias.size());

        System.out.println("  ✓ Reportes funcionando correctamente\n");
    }
}
