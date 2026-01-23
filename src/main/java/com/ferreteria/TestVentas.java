package com.ferreteria;

import com.ferreteria.models.*;
import com.ferreteria.models.dao.*;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.List;

/**
 * Clase de prueba para verificar el módulo de ventas.
 * Similar a probar endpoints - verificamos cada operación del DAO.
 */
public class TestVentas {

    private static DatabaseConfig dbConfig;
    private static SaleDAO saleDAO;
    private static ProductVariantDAO variantDAO;

    public static void main(String[] args) {
        System.out.println("===========================================");
        System.out.println("   TEST DEL MÓDULO DE VENTAS");
        System.out.println("===========================================\n");

        try {
            // Inicializar
            setup();

            // Ejecutar tests
            testCrearProductosDemo();
            testListarVariantes();
            testBuscarVariantes();
            testCrearVenta();
            testListarVentas();
            testAnularVenta();
            testEstadisticas();

            System.out.println("\n===========================================");
            System.out.println("   TODOS LOS TESTS PASARON ✓");
            System.out.println("===========================================");

        } catch (Exception e) {
            System.err.println("\n❌ ERROR EN TEST: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (dbConfig != null) {
                dbConfig.close();
            }
        }
    }

    private static void setup() throws SQLException {
        System.out.println("► Inicializando base de datos...");
        dbConfig = DatabaseConfig.getInstance();

        // Inicializar tablas
        DatabaseInitializer initializer = new DatabaseInitializer(dbConfig);
        initializer.initialize();

        saleDAO = new SaleDAO(dbConfig);
        variantDAO = new ProductVariantDAO(dbConfig);

        System.out.println("  ✓ Base de datos inicializada\n");
    }

    private static void testCrearProductosDemo() throws SQLException {
        System.out.println("► TEST: Crear productos de demo...");

        Connection conn = dbConfig.getConnection();

        // Verificar si ya hay productos
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM products");
        rs.next();
        int count = rs.getInt(1);

        if (count == 0) {
            // Crear categoría
            stmt.execute("INSERT INTO categories (name, description) VALUES ('Herramientas', 'Herramientas manuales')");

            // Crear productos
            String[] productos = {
                "INSERT INTO products (code, name, category_id) VALUES ('MART001', 'Martillo de Carpintero', 1)",
                "INSERT INTO products (code, name, category_id) VALUES ('DEST002', 'Destornillador Phillips', 1)",
                "INSERT INTO products (code, name, category_id) VALUES ('LLAV003', 'Llave Francesa 10\"', 1)",
                "INSERT INTO products (code, name, category_id) VALUES ('TALA004', 'Taladro Percutor', 1)",
                "INSERT INTO products (code, name, category_id) VALUES ('SIER005', 'Sierra Circular', 1)"
            };

            for (String sql : productos) {
                stmt.execute(sql);
            }

            // Crear variantes con precios y stock
            String[] variantes = {
                "INSERT INTO product_variants (product_id, sku, variant_name, cost_price, sale_price, stock, min_stock) VALUES (1, 'MART001-STD', 'Estándar', 1500, 2500, 50, 5)",
                "INSERT INTO product_variants (product_id, sku, variant_name, cost_price, sale_price, stock, min_stock) VALUES (1, 'MART001-PRO', 'Profesional', 2500, 4500, 25, 3)",
                "INSERT INTO product_variants (product_id, sku, variant_name, cost_price, sale_price, stock, min_stock) VALUES (2, 'DEST002-PH1', 'PH1', 300, 550, 100, 10)",
                "INSERT INTO product_variants (product_id, sku, variant_name, cost_price, sale_price, stock, min_stock) VALUES (2, 'DEST002-PH2', 'PH2', 350, 600, 100, 10)",
                "INSERT INTO product_variants (product_id, sku, variant_name, cost_price, sale_price, stock, min_stock) VALUES (3, 'LLAV003-10', '10 pulgadas', 2000, 3500, 30, 5)",
                "INSERT INTO product_variants (product_id, sku, variant_name, cost_price, sale_price, stock, min_stock) VALUES (4, 'TALA004-500W', '500W', 15000, 25000, 10, 2)",
                "INSERT INTO product_variants (product_id, sku, variant_name, cost_price, sale_price, stock, min_stock) VALUES (5, 'SIER005-7', '7 1/4\"', 25000, 42000, 8, 2)"
            };

            for (String sql : variantes) {
                stmt.execute(sql);
            }

            System.out.println("  ✓ Productos de demo creados");
        } else {
            System.out.println("  ✓ Ya existen " + count + " productos");
        }
        System.out.println();
    }

    private static void testListarVariantes() {
        System.out.println("► TEST: Listar variantes disponibles...");

        List<ProductVariant> variantes = variantDAO.listarDisponibles();

        System.out.println("  Variantes encontradas: " + variantes.size());
        for (ProductVariant v : variantes) {
            System.out.printf("    - [%d] %s | SKU: %s | Precio: $%.2f | Stock: %d%n",
                v.getId(),
                v.getDisplayName(),
                v.getSku(),
                v.getSalePrice(),
                v.getStock());
        }

        assert !variantes.isEmpty() : "Debería haber variantes";
        System.out.println("  ✓ Test pasado\n");
    }

    private static void testBuscarVariantes() {
        System.out.println("► TEST: Buscar variantes...");

        // Buscar por nombre
        List<ProductVariant> resultados = variantDAO.buscar("martillo", 10);
        System.out.println("  Búsqueda 'martillo': " + resultados.size() + " resultados");

        // Buscar por SKU
        resultados = variantDAO.buscar("DEST", 10);
        System.out.println("  Búsqueda 'DEST': " + resultados.size() + " resultados");

        // Buscar por ID
        var variante = variantDAO.buscarPorId(1);
        System.out.println("  Buscar ID 1: " + (variante.isPresent() ? variante.get().getDisplayName() : "No encontrado"));

        System.out.println("  ✓ Test pasado\n");
    }

    private static void testCrearVenta() {
        System.out.println("► TEST: Crear venta...");

        // Obtener variantes para la venta
        List<ProductVariant> variantes = variantDAO.listarDisponibles();
        if (variantes.size() < 2) {
            System.out.println("  ⚠ No hay suficientes variantes para el test");
            return;
        }

        ProductVariant v1 = variantes.get(0);
        ProductVariant v2 = variantes.get(1);

        int stockAntes1 = v1.getStock();
        int stockAntes2 = v2.getStock();

        System.out.printf("  Stock antes - %s: %d, %s: %d%n",
            v1.getDisplayName(), stockAntes1,
            v2.getDisplayName(), stockAntes2);

        // Crear items
        SaleItem item1 = new SaleItem.Builder()
            .variantId(v1.getId())
            .quantity(2)
            .unitPrice(v1.getSalePrice())
            .subtotal(v1.getSalePrice().multiply(BigDecimal.valueOf(2)))
            .productName(v1.getProductName())
            .variantName(v1.getVariantName())
            .build();

        SaleItem item2 = new SaleItem.Builder()
            .variantId(v2.getId())
            .quantity(3)
            .unitPrice(v2.getSalePrice())
            .subtotal(v2.getSalePrice().multiply(BigDecimal.valueOf(3)))
            .productName(v2.getProductName())
            .variantName(v2.getVariantName())
            .build();

        BigDecimal total = item1.getSubtotal().add(item2.getSubtotal());

        // Crear pago
        SalePayment payment = new SalePayment.Builder()
            .paymentMethod(SalePayment.PaymentMethod.CASH)
            .amount(total)
            .build();

        // Crear venta
        Sale sale = new Sale.Builder()
            .userId(1) // Admin
            .total(total)
            .status("completed")
            .notes("Venta de prueba")
            .addItem(item1)
            .addItem(item2)
            .addPayment(payment)
            .build();

        System.out.printf("  Creando venta por $%.2f...%n", total);

        Sale createdSale = saleDAO.create(sale);

        System.out.println("  ✓ Venta creada con ID: " + createdSale.getId());

        // Verificar stock actualizado
        ProductVariant v1Despues = variantDAO.buscarPorId(v1.getId()).orElseThrow();
        ProductVariant v2Despues = variantDAO.buscarPorId(v2.getId()).orElseThrow();

        System.out.printf("  Stock después - %s: %d (-%d), %s: %d (-%d)%n",
            v1.getDisplayName(), v1Despues.getStock(), stockAntes1 - v1Despues.getStock(),
            v2.getDisplayName(), v2Despues.getStock(), stockAntes2 - v2Despues.getStock());

        assert v1Despues.getStock() == stockAntes1 - 2 : "Stock no se actualizó correctamente";
        assert v2Despues.getStock() == stockAntes2 - 3 : "Stock no se actualizó correctamente";

        System.out.println("  ✓ Stock actualizado correctamente\n");
    }

    private static void testListarVentas() {
        System.out.println("► TEST: Listar ventas...");

        // Listar todas
        List<Sale> todas = saleDAO.findAll();
        System.out.println("  Total ventas: " + todas.size());

        // Listar por fecha
        List<Sale> hoy = saleDAO.findByDate(LocalDate.now());
        System.out.println("  Ventas de hoy: " + hoy.size());

        // Listar completadas
        List<Sale> completadas = saleDAO.findCompleted();
        System.out.println("  Ventas completadas: " + completadas.size());

        // Mostrar detalle de última venta
        if (!todas.isEmpty()) {
            Sale ultima = saleDAO.findById(todas.get(0).getId()).orElseThrow();
            System.out.println("\n  Última venta (#" + ultima.getId() + "):");
            System.out.printf("    Total: $%.2f%n", ultima.getTotal());
            System.out.println("    Items: " + ultima.getItems().size());
            for (SaleItem item : ultima.getItems()) {
                System.out.printf("      - %s x%d = $%.2f%n",
                    item.getDisplayName(),
                    item.getQuantity(),
                    item.getSubtotal());
            }
            System.out.println("    Pagos: " + ultima.getPayments().size());
            for (SalePayment payment : ultima.getPayments()) {
                System.out.printf("      - %s: $%.2f%n",
                    payment.getPaymentMethodDisplayName(),
                    payment.getAmount());
            }
        }

        System.out.println("  ✓ Test pasado\n");
    }

    private static void testAnularVenta() {
        System.out.println("► TEST: Anular venta...");

        List<Sale> completadas = saleDAO.findCompleted();
        if (completadas.isEmpty()) {
            System.out.println("  ⚠ No hay ventas para anular");
            return;
        }

        Sale saleToCancel = saleDAO.findById(completadas.get(0).getId()).orElseThrow();

        // Guardar stock antes
        int variantId = saleToCancel.getItems().get(0).getVariantId();
        int cantidad = saleToCancel.getItems().get(0).getQuantity();
        ProductVariant antes = variantDAO.buscarPorId(variantId).orElseThrow();
        int stockAntes = antes.getStock();

        System.out.printf("  Anulando venta #%d (stock %s antes: %d)...%n",
            saleToCancel.getId(), antes.getDisplayName(), stockAntes);

        saleDAO.cancel(saleToCancel.getId());

        // Verificar stock revertido
        ProductVariant despues = variantDAO.buscarPorId(variantId).orElseThrow();
        System.out.printf("  Stock después: %d (+%d revertido)%n",
            despues.getStock(), despues.getStock() - stockAntes);

        assert despues.getStock() == stockAntes + cantidad : "Stock no se revirtió correctamente";

        // Verificar estado
        Sale cancelledSale = saleDAO.findById(saleToCancel.getId()).orElseThrow();
        assert cancelledSale.isCancelled() : "Venta no quedó como anulada";

        System.out.println("  ✓ Venta anulada y stock revertido\n");
    }

    private static void testEstadisticas() {
        System.out.println("► TEST: Estadísticas...");

        LocalDate hoy = LocalDate.now();
        int year = hoy.getYear();
        int month = hoy.getMonthValue();

        BigDecimal totalHoy = saleDAO.dailyTotal(hoy);
        BigDecimal totalMes = saleDAO.monthlyTotal(year, month);
        BigDecimal totalGeneral = saleDAO.overallTotal();
        int cantidadHoy = saleDAO.dailyCount(hoy);
        int totalVentas = saleDAO.count();
        int completadas = saleDAO.countCompleted();

        System.out.printf("  Ventas de hoy: %d por $%.2f%n", cantidadHoy, totalHoy);
        System.out.printf("  Ventas del mes: $%.2f%n", totalMes);
        System.out.printf("  Total histórico: $%.2f%n", totalGeneral);
        System.out.printf("  Total ventas: %d (completadas: %d)%n", totalVentas, completadas);

        // Estadísticas por usuario
        var stats = saleDAO.statsByUser(1);
        System.out.printf("  Usuario #1: %d ventas, $%.2f total, $%.2f promedio%n",
            stats.salesCount(),
            stats.totalSales(),
            stats.averageSale());

        System.out.println("  ✓ Test pasado\n");
    }
}
