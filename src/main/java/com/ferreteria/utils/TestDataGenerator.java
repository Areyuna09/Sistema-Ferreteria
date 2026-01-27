package com.ferreteria.utils;

import com.ferreteria.models.dao.DatabaseConfig;
import org.mindrot.jbcrypt.BCrypt;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.Random;
import java.util.logging.Logger;

/**
 * Generador de datos de prueba para el sistema.
 * Inserta categorías, productos, variantes y ventas de ejemplo.
 *
 * @author Sistema Ferretería
 * @version 1.0
 */
public class TestDataGenerator {
    private static final Logger LOGGER = Logger.getLogger(TestDataGenerator.class.getName());
    private final DatabaseConfig dbConfig;
    private final Random random;

    public TestDataGenerator() {
        this.dbConfig = DatabaseConfig.getInstance();
        this.random = new Random();
    }

    /**
     * Genera todos los datos de prueba.
     */
    public void generateAllTestData() {
        Connection conn = null;
        try {
            // Cerrar cualquier conexión existente primero
            dbConfig.close();

            // Obtener nueva conexión
            conn = dbConfig.getConnection();

            // Configurar timeout de SQLite
            Statement stmt = conn.createStatement();
            stmt.execute("PRAGMA busy_timeout = 10000"); // 10 segundos de timeout
            stmt.close();

            LOGGER.info("=== Iniciando generación de datos de prueba ===");

            // 1. Configuración del negocio
            insertBusinessConfig(conn);

            // 2. Usuarios adicionales
            insertUsers(conn);

            // 3. Categorías
            int[] categoryIds = insertCategories(conn);

            // 4. Productos y variantes
            int[] variantIds = insertProductsAndVariants(conn, categoryIds);

            // 5. Ventas del mes actual y anterior
            insertSales(conn, variantIds);

            LOGGER.info("=== Datos de prueba generados exitosamente ===");

        } catch (SQLException e) {
            LOGGER.severe("Error al generar datos de prueba: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                if (conn != null && !conn.isClosed()) {
                    conn.close();
                }
            } catch (SQLException e) {
                LOGGER.warning("Error cerrando conexión: " + e.getMessage());
            }
        }
    }

    /**
     * Inserta configuración del negocio.
     */
    private void insertBusinessConfig(Connection conn) throws SQLException {
        String checkSql = "SELECT COUNT(*) FROM business_config";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(checkSql)) {

            if (rs.next() && rs.getInt(1) > 0) {
                LOGGER.info("Configuración del negocio ya existe");
                return;
            }
        }

        String sql = "INSERT INTO business_config (business_name, address, phone, cuit) VALUES (?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, "Ferretería El Tornillo Feliz");
            pstmt.setString(2, "Av. San Martin 1234, Ciudad de Buenos Aires");
            pstmt.setString(3, "+54 11 4567-8900");
            pstmt.setString(4, "20-12345678-9");
            pstmt.executeUpdate();
            LOGGER.info("✓ Configuración del negocio insertada");
        }
    }

    /**
     * Inserta usuarios adicionales.
     */
    private void insertUsers(Connection conn) throws SQLException {
        String[][] users = {
            {"vendedor1", "vendedor123", "vendedor", "Juan Pérez"},
            {"vendedor2", "vendedor123", "vendedor", "María González"},
            {"supervisor", "supervisor123", "administrador", "Carlos Rodríguez"}
        };

        String sql = "INSERT OR IGNORE INTO users (username, password, role, full_name) VALUES (?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            for (String[] user : users) {
                pstmt.setString(1, user[0]);
                pstmt.setString(2, BCrypt.hashpw(user[1], BCrypt.gensalt(10)));
                pstmt.setString(3, user[2]);
                pstmt.setString(4, user[3]);
                pstmt.executeUpdate();
            }
            LOGGER.info("✓ " + users.length + " usuarios insertados");
        }
    }

    /**
     * Inserta categorías de productos.
     */
    private int[] insertCategories(Connection conn) throws SQLException {
        String[] categories = {
            "Herramientas Manuales",
            "Herramientas Eléctricas",
            "Tornillería",
            "Pinturería",
            "Electricidad",
            "Plomería",
            "Construcción",
            "Jardín"
        };

        String sql = "INSERT OR IGNORE INTO categories (name, description) VALUES (?, ?)";
        int[] ids = new int[categories.length];

        try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            for (int i = 0; i < categories.length; i++) {
                pstmt.setString(1, categories[i]);
                pstmt.setString(2, "Categoría de " + categories[i].toLowerCase());
                pstmt.executeUpdate();

                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        ids[i] = rs.getInt(1);
                    }
                }
            }
            LOGGER.info("✓ " + categories.length + " categorías insertadas");
        }

        return ids;
    }

    /**
     * Inserta productos y sus variantes.
     */
    private int[] insertProductsAndVariants(Connection conn, int[] categoryIds) throws SQLException {
        // Definir productos
        Object[][] products = {
            // {nombre, descripción, categoría_index, marca, ubicación}
            {"Martillo", "Martillo de carpintero", 0, "Stanley", "Estante A1"},
            {"Destornillador", "Destornillador de precisión", 0, "Bosch", "Estante A2"},
            {"Taladro", "Taladro eléctrico inalámbrico", 1, "Makita", "Estante B1"},
            {"Sierra Circular", "Sierra circular 7 pulgadas", 1, "DeWalt", "Estante B2"},
            {"Tornillos", "Tornillos para madera", 2, "Fischer", "Cajón C1"},
            {"Tuercas", "Tuercas hexagonales", 2, "Fischer", "Cajón C2"},
            {"Pintura Latex", "Pintura látex interior", 3, "Alba", "Depósito D1"},
            {"Rodillo", "Rodillo para pintura", 3, "Tigre", "Estante D2"},
            {"Cable", "Cable eléctrico", 4, "Pirelli", "Estante E1"},
            {"Interruptor", "Interruptor simple", 4, "Cambre", "Cajón E2"},
            {"Caño PVC", "Caño PVC para desagüe", 5, "Nicoll", "Depósito F1"},
            {"Llave Inglesa", "Llave inglesa ajustable", 5, "Bahco", "Estante F2"},
            {"Cemento", "Cemento Portland", 6, "Loma Negra", "Depósito G1"},
            {"Arena", "Arena fina para construcción", 6, "Canteras", "Depósito G2"},
            {"Pala", "Pala punta cuadrada", 7, "Tramontina", "Estante H1"},
            {"Manguera", "Manguera para riego", 7, "Aquaflex", "Estante H2"}
        };

        // Variantes por producto (nombre, precio_costo, precio_venta, stock)
        Object[][][] variants = {
            // Martillo
            {{"500g", 1200, 2400, 15}, {"750g", 1500, 3000, 12}, {"1kg", 1800, 3600, 8}},
            // Destornillador
            {{"Plano 5mm", 300, 600, 25}, {"Phillips #1", 350, 700, 20}, {"Phillips #2", 400, 800, 18}},
            // Taladro
            {{"12V", 15000, 30000, 5}, {"18V", 20000, 40000, 3}},
            // Sierra Circular
            {{"1200W", 18000, 36000, 4}, {"1400W", 22000, 44000, 2}},
            // Tornillos
            {{"4x40mm (100u)", 200, 400, 50}, {"6x50mm (100u)", 250, 500, 40}, {"8x60mm (50u)", 300, 600, 30}},
            // Tuercas
            {{"M6 (50u)", 150, 300, 60}, {"M8 (50u)", 180, 360, 50}, {"M10 (50u)", 220, 440, 40}},
            // Pintura Latex
            {{"1L Blanco", 800, 1600, 30}, {"4L Blanco", 2800, 5600, 20}, {"10L Blanco", 6500, 13000, 10}},
            // Rodillo
            {{"10cm", 250, 500, 35}, {"15cm", 350, 700, 28}, {"20cm", 450, 900, 22}},
            // Cable
            {{"2.5mm (metro)", 180, 360, 100}, {"4mm (metro)", 280, 560, 80}},
            // Interruptor
            {{"10A", 200, 400, 45}, {"16A", 280, 560, 35}},
            // Caño PVC
            {{"40mm (3m)", 450, 900, 25}, {"63mm (3m)", 650, 1300, 18}, {"110mm (3m)", 1200, 2400, 12}},
            // Llave Inglesa
            {{"8 pulgadas", 800, 1600, 15}, {"10 pulgadas", 1000, 2000, 12}, {"12 pulgadas", 1300, 2600, 8}},
            // Cemento
            {{"50kg", 1800, 3600, 40}, {"25kg", 1000, 2000, 30}},
            // Arena
            {{"Bolsa 25kg", 500, 1000, 50}},
            // Pala
            {{"Mango madera", 1200, 2400, 18}, {"Mango fibra", 1500, 3000, 12}},
            // Manguera
            {{"1/2 pulgada (15m)", 1500, 3000, 20}, {"3/4 pulgada (15m)", 2000, 4000, 15}}
        };

        int totalVariants = 0;
        int[] allVariantIds = new int[100]; // Array para almacenar IDs
        int variantIndex = 0;

        String productSql = "INSERT INTO products (code, name, description, category_id, brand, location) VALUES (?, ?, ?, ?, ?, ?)";
        String variantSql = "INSERT INTO product_variants (product_id, sku, variant_name, cost_price, sale_price, stock, min_stock) VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement productStmt = conn.prepareStatement(productSql, Statement.RETURN_GENERATED_KEYS);
             PreparedStatement variantStmt = conn.prepareStatement(variantSql, Statement.RETURN_GENERATED_KEYS)) {

            for (int i = 0; i < products.length; i++) {
                Object[] product = products[i];

                // Insertar producto
                String productCode = "PROD" + String.format("%04d", i + 1);
                productStmt.setString(1, productCode);
                productStmt.setString(2, (String) product[0]);
                productStmt.setString(3, (String) product[1]);
                productStmt.setInt(4, categoryIds[(int) product[2]]);
                productStmt.setString(5, (String) product[3]);
                productStmt.setString(6, (String) product[4]);
                productStmt.executeUpdate();

                int productId;
                try (ResultSet rs = productStmt.getGeneratedKeys()) {
                    rs.next();
                    productId = rs.getInt(1);
                }

                // Insertar variantes
                Object[][] productVariants = variants[i];
                for (int j = 0; j < productVariants.length; j++) {
                    Object[] variant = productVariants[j];
                    String sku = productCode + "-V" + (j + 1);

                    variantStmt.setInt(1, productId);
                    variantStmt.setString(2, sku);
                    variantStmt.setString(3, (String) variant[0]);
                    variantStmt.setBigDecimal(4, BigDecimal.valueOf((int) variant[1]));
                    variantStmt.setBigDecimal(5, BigDecimal.valueOf((int) variant[2]));
                    variantStmt.setInt(6, (int) variant[3]);
                    variantStmt.setInt(7, 5);
                    variantStmt.executeUpdate();

                    try (ResultSet rs = variantStmt.getGeneratedKeys()) {
                        if (rs.next()) {
                            allVariantIds[variantIndex++] = rs.getInt(1);
                        }
                    }

                    totalVariants++;
                }
            }

            LOGGER.info("✓ " + products.length + " productos insertados");
            LOGGER.info("✓ " + totalVariants + " variantes insertadas");
        }

        // Retornar solo los IDs válidos
        int[] result = new int[variantIndex];
        System.arraycopy(allVariantIds, 0, result, 0, variantIndex);
        return result;
    }

    /**
     * Inserta ventas de prueba para el mes actual y el anterior.
     */
    private void insertSales(Connection conn, int[] variantIds) throws SQLException {
        if (variantIds.length == 0) {
            LOGGER.warning("No hay variantes disponibles para generar ventas");
            return;
        }

        // Obtener IDs de usuarios
        int[] userIds = getUserIds(conn);
        if (userIds.length == 0) {
            LOGGER.warning("No hay usuarios disponibles para generar ventas");
            return;
        }

        YearMonth currentMonth = YearMonth.now();
        YearMonth previousMonth = currentMonth.minusMonths(1);

        int totalSales = 0;

        // Generar ventas para el mes anterior (15-25 ventas)
        totalSales += generateMonthSales(conn, previousMonth, variantIds, userIds, 15 + random.nextInt(11));

        // Generar ventas para el mes actual (20-35 ventas)
        totalSales += generateMonthSales(conn, currentMonth, variantIds, userIds, 20 + random.nextInt(16));

        LOGGER.info("✓ " + totalSales + " ventas insertadas");
    }

    /**
     * Genera ventas para un mes específico.
     */
    private int generateMonthSales(Connection conn, YearMonth month, int[] variantIds,
                                   int[] userIds, int salesCount) throws SQLException {
        String saleSql = "INSERT INTO sales (user_id, total, status, created_at) VALUES (?, ?, ?, ?)";
        String itemSql = "INSERT INTO sale_items (sale_id, variant_id, quantity, unit_price, subtotal) VALUES (?, ?, ?, ?, ?)";
        String paymentSql = "INSERT INTO sale_payments (sale_id, payment_method, amount) VALUES (?, ?, ?)";

        String[] paymentMethods = {"efectivo", "tarjeta_debito", "tarjeta_credito", "transferencia"};

        try (PreparedStatement saleStmt = conn.prepareStatement(saleSql, Statement.RETURN_GENERATED_KEYS);
             PreparedStatement itemStmt = conn.prepareStatement(itemSql);
             PreparedStatement paymentStmt = conn.prepareStatement(paymentSql)) {

            for (int i = 0; i < salesCount; i++) {
                // Fecha aleatoria dentro del mes
                int day = 1 + random.nextInt(month.lengthOfMonth());
                int hour = 8 + random.nextInt(12); // Entre 8:00 y 20:00
                int minute = random.nextInt(60);
                LocalDateTime saleDate = month.atDay(day).atTime(hour, minute);

                // Usuario aleatorio
                int userId = userIds[random.nextInt(userIds.length)];

                // Cantidad de items en la venta (1-5)
                int itemCount = 1 + random.nextInt(5);
                BigDecimal totalSale = BigDecimal.ZERO;

                // Calcular total de la venta
                for (int j = 0; j < itemCount; j++) {
                    int variantId = variantIds[random.nextInt(variantIds.length)];
                    BigDecimal price = getVariantPrice(conn, variantId);
                    int quantity = 1 + random.nextInt(5);
                    BigDecimal subtotal = price.multiply(BigDecimal.valueOf(quantity));
                    totalSale = totalSale.add(subtotal);
                }

                // Insertar venta
                saleStmt.setInt(1, userId);
                saleStmt.setBigDecimal(2, totalSale);
                saleStmt.setString(3, "completed");
                saleStmt.setString(4, saleDate.toString());
                saleStmt.executeUpdate();

                int saleId;
                try (ResultSet rs = saleStmt.getGeneratedKeys()) {
                    rs.next();
                    saleId = rs.getInt(1);
                }

                // Insertar items
                for (int j = 0; j < itemCount; j++) {
                    int variantId = variantIds[random.nextInt(variantIds.length)];
                    BigDecimal price = getVariantPrice(conn, variantId);
                    int quantity = 1 + random.nextInt(5);
                    BigDecimal subtotal = price.multiply(BigDecimal.valueOf(quantity));

                    itemStmt.setInt(1, saleId);
                    itemStmt.setInt(2, variantId);
                    itemStmt.setInt(3, quantity);
                    itemStmt.setBigDecimal(4, price);
                    itemStmt.setBigDecimal(5, subtotal);
                    itemStmt.executeUpdate();
                }

                // Insertar pago (80% un solo método, 20% pago combinado)
                if (random.nextDouble() < 0.8) {
                    // Pago con un solo método
                    String method = paymentMethods[random.nextInt(paymentMethods.length)];
                    paymentStmt.setInt(1, saleId);
                    paymentStmt.setString(2, method);
                    paymentStmt.setBigDecimal(3, totalSale);
                    paymentStmt.executeUpdate();
                } else {
                    // Pago combinado (2 métodos)
                    BigDecimal amount1 = totalSale.multiply(BigDecimal.valueOf(0.5 + random.nextDouble() * 0.3));
                    BigDecimal amount2 = totalSale.subtract(amount1);

                    paymentStmt.setInt(1, saleId);
                    paymentStmt.setString(2, paymentMethods[random.nextInt(paymentMethods.length)]);
                    paymentStmt.setBigDecimal(3, amount1);
                    paymentStmt.executeUpdate();

                    paymentStmt.setInt(1, saleId);
                    paymentStmt.setString(2, paymentMethods[random.nextInt(paymentMethods.length)]);
                    paymentStmt.setBigDecimal(3, amount2);
                    paymentStmt.executeUpdate();
                }
            }
        }

        return salesCount;
    }

    /**
     * Obtiene el precio de una variante.
     */
    private BigDecimal getVariantPrice(Connection conn, int variantId) throws SQLException {
        String sql = "SELECT sale_price FROM product_variants WHERE id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, variantId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getBigDecimal("sale_price");
                }
            }
        }
        return BigDecimal.valueOf(1000); // Precio por defecto
    }

    /**
     * Obtiene los IDs de usuarios disponibles.
     */
    private int[] getUserIds(Connection conn) throws SQLException {
        String sql = "SELECT id FROM users WHERE active = 1";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            int[] ids = new int[10];
            int count = 0;

            while (rs.next() && count < ids.length) {
                ids[count++] = rs.getInt("id");
            }

            int[] result = new int[count];
            System.arraycopy(ids, 0, result, 0, count);
            return result;
        }
    }

    /**
     * Método principal para ejecutar el generador.
     */
    public static void main(String[] args) {
        System.out.println("===========================================");
        System.out.println("  Generador de Datos de Prueba");
        System.out.println("  Sistema de Ferretería");
        System.out.println("===========================================\n");

        TestDataGenerator generator = new TestDataGenerator();

        try {
            generator.generateAllTestData();
            System.out.println("\n===========================================");
            System.out.println("  ✓ Datos generados exitosamente");
            System.out.println("===========================================");
            System.out.println("\nAhora puedes:");
            System.out.println("1. Iniciar sesión con: admin / admin123");
            System.out.println("2. O con: vendedor1 / vendedor123");
            System.out.println("3. Ir a la sección de Reportes");
            System.out.println("4. Generar reportes para el mes actual o anterior");
            System.out.println("5. Probar las exportaciones PDF y Excel");

        } catch (Exception e) {
            System.err.println("\n✗ Error al generar datos: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
