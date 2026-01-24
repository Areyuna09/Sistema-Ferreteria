package com.ferreteria.models.dao;

import org.mindrot.jbcrypt.BCrypt;
import java.sql.*;

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
            applyMigrations(conn);  // Linea agregada para el email
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
      /**
     * Aplica migraciones necesarias a la base de datos.
     * Agrega el campo email a business_config si no existe.
     */
    private void applyMigrations(Connection conn) throws SQLException {
        try {
            // Verificar si existe la columna email en business_config
            DatabaseMetaData meta = conn.getMetaData();
            ResultSet columns = meta.getColumns(null, null, "business_config", "email");
            
            if (!columns.next()) {
                // La columna no existe, agregarla
                Statement stmt = conn.createStatement();
                stmt.execute("ALTER TABLE business_config ADD COLUMN email VARCHAR(100)");
                System.out.println("✓ Columna 'email' agregada a business_config");
            }
            
        } catch (SQLException e) {
            System.err.println("Error aplicando migraciones: " + e.getMessage());
            // No lanzar excepción, continuar
        }
    }
}
