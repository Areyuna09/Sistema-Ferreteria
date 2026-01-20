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
            createTables(conn);
            createDefaultAdmin(conn);
            System.out.println("Base de datos inicializada: " + config.getDbPath());
        } catch (SQLException e) {
            throw new RuntimeException("Error inicializando base de datos", e);
        }
    }

    private void createTables(Connection conn) throws SQLException {
        Statement stmt = conn.createStatement();

        stmt.execute("""
            CREATE TABLE IF NOT EXISTS users (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                username VARCHAR(50) UNIQUE NOT NULL,
                password VARCHAR(255) NOT NULL,
                role VARCHAR(20) DEFAULT 'vendedor',
                full_name VARCHAR(100),
                active BOOLEAN DEFAULT 1,
                created_at DATETIME DEFAULT CURRENT_TIMESTAMP
            )
        """);

        stmt.execute("""
            CREATE TABLE IF NOT EXISTS products (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                code VARCHAR(50) UNIQUE,
                name VARCHAR(200) NOT NULL,
                description TEXT,
                category VARCHAR(100),
                price DECIMAL(10,2) NOT NULL DEFAULT 0,
                cost DECIMAL(10,2) DEFAULT 0,
                stock INTEGER DEFAULT 0,
                min_stock INTEGER DEFAULT 5,
                location VARCHAR(50),
                active BOOLEAN DEFAULT 1,
                created_at DATETIME DEFAULT CURRENT_TIMESTAMP
            )
        """);

        stmt.execute("""
            CREATE TABLE IF NOT EXISTS sales (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id INTEGER,
                total DECIMAL(10,2) NOT NULL,
                payment_method VARCHAR(50) DEFAULT 'efectivo',
                created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (user_id) REFERENCES users(id)
            )
        """);
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
}
