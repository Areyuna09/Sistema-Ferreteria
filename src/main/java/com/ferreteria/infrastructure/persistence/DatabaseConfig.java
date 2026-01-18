package com.ferreteria.infrastructure.persistence;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Configuración de conexión a la base de datos.
 * Singleton para gestionar la conexión.
 */
public class DatabaseConfig {

    private static DatabaseConfig instance;
    private Connection connection;
    private final String dbPath;

    private DatabaseConfig() {
        String userHome = System.getProperty("user.home");
        String appDataDir = userHome + File.separator + ".ferreteria-java-data";

        File dir = new File(appDataDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        this.dbPath = appDataDir + File.separator + "ferreteria.db";
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
