package com.ferreteria.models.dao;

import com.ferreteria.models.BusinessConfig;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * DAO para gestionar la configuración del negocio.
 * Solo debe existir un único registro con id=1.
 */
public class BusinessConfigDAO {
    private static final Logger LOGGER = Logger.getLogger(BusinessConfigDAO.class.getName());
    private static final int CONFIG_ID = 1;
    
    private final DatabaseConfig dbConfig;

    public BusinessConfigDAO(DatabaseConfig dbConfig) {
        this.dbConfig = dbConfig;
    }

    /**
     * Obtiene la configuración del negocio.
     * Si no existe, devuelve una configuración vacía.
     * 
     * @return Optional con la configuración si existe
     */
    public Optional<BusinessConfig> getConfig() {
        String sql = "SELECT * FROM business_config WHERE id = ?";
        
        try (Connection conn = dbConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, CONFIG_ID);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                BusinessConfig config = new BusinessConfig.Builder()
                    .id(rs.getInt("id"))
                    .businessName(rs.getString("business_name"))
                    .cuit(rs.getString("cuit"))
                    .address(rs.getString("address"))
                    .phone(rs.getString("phone"))
                    .email(rs.getString("email"))
                    .updatedAt(parseDateTime(rs.getString("updated_at")))
                    .build();
                
                LOGGER.info("Configuración cargada: " + config.getBusinessName());
                return Optional.of(config);
            }
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al obtener configuración", e);
        }
        
        return Optional.empty();
    }

    /**
     * Guarda o actualiza la configuración del negocio.
     * Si existe el registro id=1, hace UPDATE. Si no, hace INSERT.
     * 
     * @param config Configuración a guardar
     * @return true si se guardó correctamente
     */
    public boolean saveConfig(BusinessConfig config) {
        try (Connection conn = dbConfig.getConnection()) {
            conn.setAutoCommit(false);
            
            try {
                // Verificar si existe
                boolean exists = existsConfig(conn);
                
                if (exists) {
                    updateConfig(conn, config);
                    LOGGER.info("Configuración actualizada");
                } else {
                    insertConfig(conn, config);
                    LOGGER.info("Configuración creada");
                }
                
                conn.commit();
                return true;
                
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al guardar configuración", e);
            throw new RuntimeException("Error al guardar configuración: " + e.getMessage(), e);
        }
    }

    /**
     * Verifica si existe el registro de configuración.
     * 
     * @param conn Conexión activa
     * @return true si existe
     */
    private boolean existsConfig(Connection conn) throws SQLException {
        String sql = "SELECT COUNT(*) FROM business_config WHERE id = ?";
        
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, CONFIG_ID);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        }
        
        return false;
    }

    /**
     * Inserta un nuevo registro de configuración.
     * 
     * @param conn Conexión activa
     * @param config Configuración a insertar
     * @throws SQLException si hay error en la base de datos
     */
    private void insertConfig(Connection conn, BusinessConfig config) throws SQLException {
        String sql = """
            INSERT INTO business_config (id, business_name, cuit, address, phone, email, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, datetime('now'))
        """;
        
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, CONFIG_ID);
            pstmt.setString(2, config.getBusinessName());
            pstmt.setString(3, config.getCuit());
            pstmt.setString(4, config.getAddress());
            pstmt.setString(5, config.getPhone());
            pstmt.setString(6, config.getEmail());
            pstmt.executeUpdate();
        }
    }

    /**
     * Actualiza el registro existente de configuración.
     * 
     * @param conn Conexión activa
     * @param config Configuración a actualizar
     * @throws SQLException si hay error en la base de datos
     */
    private void updateConfig(Connection conn, BusinessConfig config) throws SQLException {
        String sql = """
            UPDATE business_config 
            SET business_name = ?, cuit = ?, address = ?, phone = ?, email = ?, updated_at = datetime('now')
            WHERE id = ?
        """;
        
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, config.getBusinessName());
            pstmt.setString(2, config.getCuit());
            pstmt.setString(3, config.getAddress());
            pstmt.setString(4, config.getPhone());
            pstmt.setString(5, config.getEmail());
            pstmt.setInt(6, CONFIG_ID);
            pstmt.executeUpdate();
        }
    }

    // Helper methods

    private LocalDateTime parseDateTime(String dateStr) {
        if (dateStr == null) return LocalDateTime.now();
        try {
            return LocalDateTime.parse(dateStr.replace(" ", "T"));
        } catch (Exception e) {
            return LocalDateTime.now();
        }
    }
}