package com.ferreteria.models.dao;

import org.mindrot.jbcrypt.BCrypt;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Inicializa la base de datos con las tablas necesarias.
 */
public class DatabaseInitializer {

    private final DatabaseConfig config;

    public DatabaseInitializer(DatabaseConfig config) {
        this.config = config;
    }

    public void initialize() {
        try {
            Connection conn = config.getConnection();

            // Activar foreign keys en SQLite
            Statement pragma = conn.createStatement();
            pragma.execute("PRAGMA foreign_keys = ON");

            createTables(conn);
            createIndexes(conn);
            createDefaultAdmin(conn);
            createInitialCategories(conn);
            createInitialProducts(conn);
            fixProductCategories(conn); // Reparar categorías faltantes
            System.out.println("Base de datos inicializada: " + config.getDbPath());
        } catch (SQLException e) {
            throw new RuntimeException("Error inicializando base de datos", e);
        }
    }

    private void createTables(Connection conn) throws SQLException {
        Statement stmt = conn.createStatement();

        // =============================================
        // CONFIGURACIÓN DEL NEGOCIO
        // =============================================
        stmt.execute("""
            CREATE TABLE IF NOT EXISTS business_config (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                business_name VARCHAR(200),
                address VARCHAR(300),
                phone VARCHAR(50),
                cuit VARCHAR(20),
                logo_path VARCHAR(500),
                created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
            )
        """);

        // =============================================
        // USUARIOS
        // =============================================
        stmt.execute("""
            CREATE TABLE IF NOT EXISTS users (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                username VARCHAR(50) UNIQUE NOT NULL,
                password VARCHAR(255) NOT NULL,
                role VARCHAR(20) DEFAULT 'vendedor',
                full_name VARCHAR(100),
                active BOOLEAN DEFAULT 1,
                created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
            )
        """);

        // =============================================
        // CATEGORÍAS (con subcategorías)
        // =============================================
        stmt.execute("""
            CREATE TABLE IF NOT EXISTS categories (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name VARCHAR(100) NOT NULL,
                description TEXT,
                parent_id INTEGER,
                active BOOLEAN DEFAULT 1,
                created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (parent_id) REFERENCES categories(id),
                UNIQUE(name, parent_id)
            )
        """);

        // =============================================
        // PRODUCTOS (producto base)
        // =============================================
        stmt.execute("""
            CREATE TABLE IF NOT EXISTS products (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                code VARCHAR(50) UNIQUE,
                name VARCHAR(200) NOT NULL,
                description TEXT,
                category_id INTEGER,
                brand VARCHAR(100),
                location VARCHAR(100),
                image_path VARCHAR(500),
                active BOOLEAN DEFAULT 1,
                created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (category_id) REFERENCES categories(id)
            )
        """);

        // =============================================
        // VARIANTES DE PRODUCTO (precio y stock aquí)
        // =============================================
        stmt.execute("""
            CREATE TABLE IF NOT EXISTS product_variants (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                product_id INTEGER NOT NULL,
                sku VARCHAR(50) UNIQUE,
                variant_name VARCHAR(100) NOT NULL,
                cost_price DECIMAL(10,2) DEFAULT 0,
                sale_price DECIMAL(10,2) NOT NULL DEFAULT 0,
                stock INTEGER DEFAULT 0 CHECK(stock >= 0),
                min_stock INTEGER DEFAULT 5,
                active BOOLEAN DEFAULT 1,
                created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (product_id) REFERENCES products(id)
            )
        """);

        // =============================================
        // VENTAS (cabecera)
        // =============================================
        stmt.execute("""
            CREATE TABLE IF NOT EXISTS sales (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id INTEGER NOT NULL,
                total DECIMAL(10,2) NOT NULL DEFAULT 0,
                status VARCHAR(20) DEFAULT 'completed',
                notes TEXT,
                created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (user_id) REFERENCES users(id)
            )
        """);

        // =============================================
        // DETALLE DE VENTA (items)
        // =============================================
        stmt.execute("""
            CREATE TABLE IF NOT EXISTS sale_items (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                sale_id INTEGER NOT NULL,
                variant_id INTEGER NOT NULL,
                quantity INTEGER NOT NULL DEFAULT 1,
                unit_price DECIMAL(10,2) NOT NULL,
                subtotal DECIMAL(10,2) NOT NULL,
                created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (sale_id) REFERENCES sales(id),
                FOREIGN KEY (variant_id) REFERENCES product_variants(id)
            )
        """);

        // =============================================
        // PAGOS DE VENTA (permite pago combinado)
        // =============================================
        stmt.execute("""
            CREATE TABLE IF NOT EXISTS sale_payments (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                sale_id INTEGER NOT NULL,
                payment_method VARCHAR(50) NOT NULL,
                amount DECIMAL(10,2) NOT NULL,
                reference VARCHAR(100),
                created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (sale_id) REFERENCES sales(id)
            )
        """);
    }

    private void createIndexes(Connection conn) throws SQLException {
        Statement stmt = conn.createStatement();

        // Índices para búsquedas rápidas
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_products_name ON products(name)");
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_products_category ON products(category_id)");
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_variants_product ON product_variants(product_id)");
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_variants_sku ON product_variants(sku)");
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_sales_date ON sales(created_at)");
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_sales_user ON sales(user_id)");
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_sales_status ON sales(status)");
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_sale_items_sale ON sale_items(sale_id)");
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_sale_payments_sale ON sale_payments(sale_id)");
    }

    private void createDefaultAdmin(Connection conn) throws SQLException {
        String checkSql = "SELECT COUNT(*) FROM users WHERE username = 'admin'";
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery(checkSql);

        if (rs.next() && rs.getInt(1) == 0) {
            String insertSql = "INSERT INTO users (username, password, role, full_name) VALUES (?, ?, ?, ?)";
            PreparedStatement pstmt = conn.prepareStatement(insertSql);
            pstmt.setString(1, "admin");
            pstmt.setString(2, BCrypt.hashpw("admin123", BCrypt.gensalt(10)));
            pstmt.setString(3, "administrador");
            pstmt.setString(4, "Administrador");
            pstmt.executeUpdate();
            System.out.println("Usuario admin creado");
        }
    }

    private void createInitialCategories(Connection conn) throws SQLException {
        try {
            System.out.println("Verificando categorías necesarias para productos...");
            
            // Verificar si ya hay categorías
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM categories");
            
            if (rs.next() && rs.getInt(1) > 0) {
                System.out.println("Categorías ya existen, verificando categorías necesarias...");
                
                // Verificar categorías específicas que necesitan los productos
                String[] requiredCategories = {
                    "Martillos", "Destornilladores", "Alicates", "Llaves", 
                    "Serruchos y Sierras", "Herramientas de Carpintería", "Herramientas de Corte",
                    "Cintas Métricas", "Niveles y Plomadas", "Calibres y Micrómetros",
                    "Taladros y Atornilladores", "Amoladoras y Sierras", "Lijadoras y Pulidoras", "Herramientas Especializadas",
                    "Mechas y Brocas", "Discos y Hojas", "Lijas y Abrasivos", "Puntas y Adaptadores",
                    "Materiales Secos", "Mampostería", "Hierros y Metales", "Maderas y Derivados",
                    "Pinturas", "Accesorios de Pintura",
                    "Caños PVC y Accesorios", "Griferías", "Agua Caliente y Gas", "Tanques y Depósitos",
                    "Cables y Conductores", "Instalación Eléctrica", "Tableros y Protecciones", "Iluminación",
                    "Tornillos y Bulones", "Fijaciones Especiales",
                    "Colas y Pegamentos", "Siliconas y Selladores",
                    "Cerraduras y Candados", "Herrajes para Muebles",
                    "Vidrios y Accesorios", "Sanitarios",
                    "Ventilación", "Climatización",
                    "Seguridad", "Protección Personal",
                    "Herramientas de Jardín", "Riego y Accesorios",
                    "Escaleras", "Andamios y Accesorios",
                    "Limpieza", "Mantenimiento",
                    "Lubricantes y Fluidos", "Repuestos y Accesorios",
                    "Equipos de Soldadura", "Consumibles de Soldadura",
                    "Piscinas", "Náutica",
                    "Camping", "Outdoor",
                    "Organización", "Eléctricos Varios", "Herramientas Varias"
                };
                
                // Insertar categorías faltantes
                for (String category : requiredCategories) {
                    ResultSet checkRs = stmt.executeQuery("SELECT COUNT(*) FROM categories WHERE name = '" + category + "'");
                    if (checkRs.next() && checkRs.getInt(1) == 0) {
                        System.out.println("Creando categoría faltante: " + category);
                        String insertSql = "INSERT INTO categories (name, description, active, created_at) VALUES (?, ?, 1, datetime('now'))";
                        PreparedStatement pstmt = conn.prepareStatement(insertSql);
                        pstmt.setString(1, category);
                        pstmt.setString(2, "Categoría: " + category);
                        pstmt.executeUpdate();
                    }
                }
                return;
            }

            // Insertar categorías principales
            String[] mainCategories = {
                "HERRAMIENTAS MANUALES", "INSTRUMENTOS DE MEDICIÓN", "HERRAMIENTAS ELÉCTRICAS",
                "ACCESORIOS ELÉCTRICOS", "MATERIALES DE CONSTRUCCIÓN", "PINTURAS",
                "PLOMERÍA", "ELECTRICIDAD", "TORNILLERÍA", "ADHESIVOS Y SELLADORES",
                "CERRAJERÍA", "VIDRIOS Y SANITARIOS", "VENTILACIÓN Y CLIMATIZACIÓN",
                "SEGURIDAD Y PROTECCIÓN", "JARDÍN Y EXTERIOR", "ESCALERAS Y ANDAMIOS",
                "LIMPIEZA Y MANTENIMIENTO", "AUTOMOTRIZ", "SOLDADURA",
                "NAVAL Y PISCINAS", "CAMPING Y OUTDOOR", "VARIOS"
            };

            for (String category : mainCategories) {
                String insertSql = "INSERT OR IGNORE INTO categories (name, description, active, created_at) VALUES (?, ?, 1, datetime('now'))";
                PreparedStatement pstmt = conn.prepareStatement(insertSql);
                pstmt.setString(1, category);
                pstmt.setString(2, "Categoría: " + category);
                pstmt.executeUpdate();
            }

            // Insertar subcategorías clave
            String[][] subCategories = {
                {"Martillos", "Martillos varios tipos", "HERRAMIENTAS MANUALES"},
                {"Destornilladores", "Destornilladores varios tipos", "HERRAMIENTAS MANUALES"},
                {"Alicates", "Alicates y pinzas", "HERRAMIENTAS MANUALES"},
                {"Llaves", "Llaves varias", "HERRAMIENTAS MANUALES"},
                {"Taladros y Atornilladores", "Taladros eléctricos", "HERRAMIENTAS ELÉCTRICAS"},
                {"Amoladoras y Sierras", "Amoladoras y sierras eléctricas", "HERRAMIENTAS ELÉCTRICAS"},
                {"Materiales Secos", "Cementos, cal, yeso", "MATERIALES DE CONSTRUCCIÓN"},
                {"Mampostería", "Ladrillos, bloques", "MATERIALES DE CONSTRUCCIÓN"},
                {"Pinturas", "Látex, esmaltes", "PINTURAS"},
                {"Accesorios de Pintura", "Pinceles, rodillos", "PINTURAS"},
                {"Caños PVC y Accesorios", "Caños PVC", "PLOMERÍA"},
                {"Griferías", "Canillas, mezcladoras", "PLOMERÍA"},
                {"Cables y Conductores", "Cables eléctricos", "ELECTRICIDAD"},
                {"Tableros y Protecciones", "Térmicas, disyuntores", "ELECTRICIDAD"}
            };

            for (String[] subCat : subCategories) {
                String insertSql = """
                    INSERT OR IGNORE INTO categories (name, description, parent_id, active, created_at) 
                    VALUES (?, ?, (SELECT id FROM categories WHERE name = ?), 1, datetime('now'))
                    """;
                PreparedStatement pstmt = conn.prepareStatement(insertSql);
                pstmt.setString(1, subCat[0]);
                pstmt.setString(2, subCat[1]);
                pstmt.setString(3, subCat[2]);
                pstmt.executeUpdate();
            }

            System.out.println("Categorías iniciales creadas");
        } catch (Exception e) {
            System.err.println("Error creando categorías: " + e.getMessage());
        }
    }

    private void createInitialProducts(Connection conn) throws SQLException {
        try {
            System.out.println("Cargando productos desde archivos SQL...");
            
            // Limpiar completamente la base de datos para reemplazar con nuevos productos
            Statement stmt = conn.createStatement();
            System.out.println("Limpiando base de datos existente...");
            
            // Desactivar foreign keys temporalmente
            stmt.execute("PRAGMA foreign_keys = OFF");
            
            // Limpiar tablas en orden correcto
            stmt.execute("DELETE FROM product_variants");
            stmt.execute("DELETE FROM products");
            
            // Resetear autoincrement
            stmt.execute("DELETE FROM sqlite_sequence WHERE name = 'products'");
            stmt.execute("DELETE FROM sqlite_sequence WHERE name = 'product_variants'");
            
            // Reactivar foreign keys
            stmt.execute("PRAGMA foreign_keys = ON");
            
            System.out.println("Base de datos limpiada. Cargando nuevos productos...");
            
            // Leer y ejecutar el archivo de productos
            executeSqlFile(conn, "/sql/products_part1.sql");
            
            System.out.println("Generando variantes para todos los productos...");
            
            // Generar variantes automáticamente para todos los productos
            generateProductVariants(conn);
            
            System.out.println("Nuevos productos y variantes cargados desde archivos SQL");
        } catch (Exception e) {
            System.err.println("Error creando productos: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void generateProductVariants(Connection conn) throws SQLException {
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT id, code, name FROM products");
        
        PreparedStatement pstmtVariant = conn.prepareStatement("""
            INSERT INTO product_variants (product_id, sku, variant_name, cost_price, sale_price, stock, min_stock, active, created_at, updated_at) 
            VALUES (?, ?, 'Estándar', ?, ?, ?, ?, 1, datetime('now'), datetime('now'))
            """);
        
        int variantsGenerated = 0;
        while (rs.next()) {
            int productId = rs.getInt("id");
            String code = rs.getString("code");
            String name = rs.getString("name");
            
            // Calcular precios según tipo de producto
            double costPrice = calculateCostPrice(code, name);
            double salePrice = costPrice * 1.4; // 40% de margen
            
            // Configurar parámetros
            pstmtVariant.setInt(1, productId);
            pstmtVariant.setString(2, code + "-STD");
            pstmtVariant.setDouble(3, costPrice);
            pstmtVariant.setDouble(4, salePrice);
            pstmtVariant.setInt(5, calculateStock(code, name));
            pstmtVariant.setInt(6, calculateMinStock(code, name));
            
            pstmtVariant.executeUpdate();
            variantsGenerated++;
        }
        
        System.out.println("Se generaron " + variantsGenerated + " variantes de productos");
    }
    
    private double calculateCostPrice(String code, String name) {
        // Precios de costo según tipo de producto (Argentina 2026)
        if (code.startsWith("MART")) {
            return switch (code) {
                case "MART-001" -> 1800.0;
                case "MART-002" -> 1200.0;
                case "MART-003" -> 3500.0;
                case "MART-004" -> 8000.0;
                case "MART-005" -> 6000.0;
                default -> 2000.0;
            };
        } else if (code.startsWith("DEST")) {
            return switch (code) {
                case "DEST-010" -> 3000.0;
                case "DEST-011" -> 4000.0;
                default -> 800.0;
            };
        } else if (code.startsWith("TAL")) {
            return switch (code) {
                case "TAL-001" -> 15000.0;
                case "TAL-002" -> 25000.0;
                case "TAL-003" -> 35000.0;
                case "TAL-004" -> 45000.0;
                default -> 20000.0;
            };
        } else if (code.startsWith("AMO")) {
            return switch (code) {
                case "AMO-001" -> 18000.0;
                case "AMO-002" -> 25000.0;
                case "AMO-003" -> 32000.0;
                default -> 20000.0;
            };
        } else if (code.startsWith("ESP")) {
            return switch (code) {
                case "ESP-001" -> 30000.0;
                case "ESP-006" -> 40000.0;
                case "ESP-007" -> 35000.0;
                case "ESP-008" -> 45000.0;
                default -> 20000.0;
            };
        } else if (code.startsWith("CAL")) {
            return 3000.0;
        } else if (code.startsWith("NIV")) {
            return switch (code) {
                case "NIV-004" -> 8000.0;
                default -> 2000.0;
            };
        } else if (code.startsWith("MED")) {
            return 800.0;
        } else if (code.startsWith("MAT") || code.startsWith("MAM") || code.startsWith("HIE")) {
            return 500.0; // Materiales de construcción
        } else if (code.startsWith("PIN")) {
            return switch (code) {
                case "PIN-001", "PIN-004", "PIN-006", "PIN-015" -> 8000.0; // Látex grandes
                case "PIN-016" -> 12000.0; // Membrana líquida
                default -> 3000.0; // Pinturas pequeñas
            };
        } else if (code.startsWith("MEC") || code.startsWith("DIS") || code.startsWith("LIJ")) {
            return 1500.0; // Accesorios eléctricos
        } else {
            return 1500.0; // Precio por defecto
        }
    }
    
    private int calculateStock(String code, String name) {
        // Stock según tipo de producto
        if (code.startsWith("TAL") || code.startsWith("AMO") || code.startsWith("ESP")) {
            return 5; // Herramientas eléctricas: poco stock
        } else if (code.startsWith("CAL")) {
            return 8; // Instrumentos de precisión: stock bajo
        } else if (code.startsWith("NIV") && code.equals("NIV-004")) {
            return 6; // Nivel láser: stock bajo
        } else if (code.startsWith("SET")) {
            return 4; // Sets: stock bajo
        } else if (code.startsWith("MAT") || code.startsWith("MAM") || code.startsWith("HIE")) {
            return 20; // Materiales de construcción: stock normal
        } else if (code.startsWith("PIN")) {
            return 15; // Pinturas: stock normal
        } else {
            return 15; // Herramientas manuales: stock normal
        }
    }
    
    private int calculateMinStock(String code, String name) {
        int stock = calculateStock(code, name);
        return Math.max(2, stock / 3); // Mínimo stock = 33% del stock normal
    }
    
    private void executeSqlFile(Connection conn, String resourcePath) throws SQLException, IOException {
        System.out.println("Intentando cargar archivo: " + resourcePath);
        try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
            if (is == null) {
                System.err.println("ERROR: No se encontró el recurso: " + resourcePath);
                return;
            }
            
            System.out.println("Archivo encontrado, leyendo contenido...");
            String sqlContent = new String(is.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            String[] sqlStatements = sqlContent.split(";");
            System.out.println("Se encontraron " + sqlStatements.length + " sentencias SQL");
            
            Statement stmt = conn.createStatement();
            int executedCount = 0;
            for (String sql : sqlStatements) {
                String trimmedSql = sql.trim();
                if (!trimmedSql.isEmpty()) {
                    try {
                        stmt.execute(trimmedSql);
                        executedCount++;
                    } catch (Exception e) {
                        System.err.println("Error ejecutando SQL: " + trimmedSql);
                        System.err.println("Error: " + e.getMessage());
                    }
                }
            }
            System.out.println("Se ejecutaron " + executedCount + " sentencias SQL exitosamente");
        }
    }
    
    private void fixProductCategories(Connection conn) throws SQLException {
        System.out.println("Reparando categorías de productos...");
        
        try {
            Statement stmt = conn.createStatement();
            
            // Mapeo de códigos de productos a categorías
            String[][] productCategories = {
                {"MART", "Martillos"},
                {"DEST", "Destornilladores"},
                {"ALIC", "Alicates"},
                {"PINZ", "Alicates"},
                {"LLAV", "Llaves"},
                {"SERR", "Serruchos y Sierras"},
                {"SIER", "Serruchos y Sierras"},
                {"CARP", "Herramientas de Carpintería"},
                {"CORT", "Herramientas de Corte"},
                {"CINT", "Cintas Métricas"},
                {"NIV", "Niveles y Plomadas"},
                {"MED", "Calibres y Micrómetros"},
                {"TAL", "Taladros y Atornilladores"},
                {"AMO", "Amoladoras y Sierras"},
                {"ESP", "Herramientas Especializadas"},
                {"MEC", "Mechas y Brocas"},
                {"DIS", "Discos y Hojas"},
                {"LIJ", "Lijas y Abrasivos"},
                {"PUNT", "Puntas y Adaptadores"},
                {"MAT", "Materiales Secos"},
                {"MAM", "Mampostería"},
                {"HIE", "Hierros y Metales"},
                {"MAD", "Maderas y Derivados"},
                {"PIN", "Pinturas"},
                {"PCE", "Accesorios de Pintura"},
                {"CAÑ", "Caños PVC y Accesorios"},
                {"GRI", "Griferías"},
                {"AGU", "Agua Caliente y Gas"},
                {"TAN", "Tanques y Depósitos"},
                {"CAB", "Cables y Conductores"},
                {"INS", "Instalación Eléctrica"},
                {"TAB", "Tableros y Protecciones"},
                {"ILU", "Iluminación"},
                {"TOR", "Tornillos y Bulones"},
                {"FIJ", "Fijaciones Especiales"},
                {"COL", "Colas y Pegamentos"},
                {"SIL", "Siliconas y Selladores"},
                {"CER", "Cerraduras y Candados"},
                {"HERR", "Herrajes para Muebles"},
                {"VID", "Vidrios y Accesorios"},
                {"SAN", "Sanitarios"},
                {"VEN", "Ventilación"},
                {"CLI", "Climatización"},
                {"SEG", "Seguridad"},
                {"PRO", "Protección Personal"},
                {"JAR", "Herramientas de Jardín"},
                {"RIE", "Riego y Accesorios"},
                {"ESC", "Escaleras"},
                {"AND", "Andamios y Accesorios"},
                {"LIM", "Limpieza"},
                {"MAN", "Mantenimiento"},
                {"LUB", "Lubricantes y Fluidos"},
                {"REP", "Repuestos y Accesorios"},
                {"EQU", "Equipos de Soldadura"},
                {"CON", "Consumibles de Soldadura"},
                {"PIS", "Piscinas"},
                {"NAU", "Náutica"},
                {"CAM", "Camping"},
                {"OUT", "Outdoor"},
                {"ORG", "Organización"},
                {"ELE", "Eléctricos Varios"},
                {"HVA", "Herramientas Varias"}
            };
            
            int updatedCount = 0;
            for (String[] mapping : productCategories) {
                String codePrefix = mapping[0];
                String categoryName = mapping[1];
                
                // Actualizar productos que no tienen categoría asignada
                String updateSql = String.format("""
                    UPDATE products 
                    SET category_id = (SELECT id FROM categories WHERE name = '%s')
                    WHERE code LIKE '%s%%' AND (category_id IS NULL OR category_id = 0)
                    """, categoryName, codePrefix);
                
                int rowsUpdated = stmt.executeUpdate(updateSql);
                if (rowsUpdated > 0) {
                    System.out.println("Actualizados " + rowsUpdated + " productos con prefijo " + codePrefix + " a categoría " + categoryName);
                    updatedCount += rowsUpdated;
                }
            }
            
            System.out.println("Total de productos actualizados: " + updatedCount);
            
        } catch (Exception e) {
            System.err.println("Error reparando categorías: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
