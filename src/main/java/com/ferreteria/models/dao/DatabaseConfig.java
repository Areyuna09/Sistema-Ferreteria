package com.ferreteria.models.dao;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Configuración de conexión a la base de datos.
 * Singleton para gestionar la conexión.
 */
public class DatabaseConfig {

    private static DatabaseConfig instance;
    private Connection connection;
    private static final String DB_PATH = "ferreteria.db";
    private final String dbPath;

    private DatabaseConfig() {
        String appDataDir = System.getProperty("user.dir");

        File dir = new File(appDataDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        this.dbPath = appDataDir + File.separator + DB_PATH;
    }

    public static synchronized DatabaseConfig getInstance() {
        if (instance == null) {
            instance = new DatabaseConfig();
        }
        return instance;
    }

    public Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
            // Activar foreign keys en SQLite
            Statement stmt = connection.createStatement();
            stmt.execute("PRAGMA foreign_keys = ON");
        }
        return connection;
    }

    public String getDbPath() {
        return dbPath;
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            System.err.println("Error cerrando conexión: " + e.getMessage());
        }
    }
}
