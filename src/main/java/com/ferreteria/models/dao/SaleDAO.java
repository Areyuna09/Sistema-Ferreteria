package com.ferreteria.models.dao;

import com.ferreteria.models.Sale;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Data Access Object para operaciones CRUD de ventas.
 * Implementado desde cero basado en la estructura de Alan.
 */
public class SaleDAO {
    
    private static final Logger LOGGER = Logger.getLogger(SaleDAO.class.getName());
    private final DatabaseConfig config;
    
    public SaleDAO() {
        this.config = DatabaseConfig.getInstance();
    }
    
    public SaleDAO(DatabaseConfig config) {
        this.config = config;
    }
    
    // ==================== OPERACIONES DE LECTURA ====================
    
    /**
     * Busca una venta por su ID.
     */
    public Optional<Sale> findById(int id) {
        String sql = "SELECT * FROM sales WHERE id = ?";
        try {
            PreparedStatement pstmt = config.getConnection().prepareStatement(sql);
            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return Optional.of(mapResultSetToSale(rs));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al buscar venta por ID: " + id, e);
        }
        return Optional.empty();
    }
    
    /**
     * Obtiene todas las ventas.
     */
    public List<Sale> findAll() {
        List<Sale> sales = new ArrayList<>();
        String sql = "SELECT * FROM sales ORDER BY created_at DESC";
        try {
            Statement stmt = config.getConnection().createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            
            while (rs.next()) {
                sales.add(mapResultSetToSale(rs));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al obtener todas las ventas", e);
        }
        return sales;
    }
    
    /**
     * Busca ventas por rango de fechas.
     */
    public List<Sale> findByDateRange(LocalDate startDate, LocalDate endDate) {
        List<Sale> sales = new ArrayList<>();
        String sql = "SELECT * FROM sales WHERE DATE(created_at) BETWEEN ? AND ? ORDER BY created_at DESC";
        try {
            PreparedStatement pstmt = config.getConnection().prepareStatement(sql);
            pstmt.setDate(1, Date.valueOf(startDate));
            pstmt.setDate(2, Date.valueOf(endDate));
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                sales.add(mapResultSetToSale(rs));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al buscar ventas por rango de fechas", e);
        }
        return sales;
    }
    
    /**
     * Busca ventas por fecha específica.
     */
    public List<Sale> findByDate(LocalDate date) {
        List<Sale> sales = new ArrayList<>();
        String sql = "SELECT * FROM sales WHERE DATE(created_at) = ? ORDER BY created_at DESC";
        try {
            PreparedStatement pstmt = config.getConnection().prepareStatement(sql);
            pstmt.setDate(1, Date.valueOf(date));
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                sales.add(mapResultSetToSale(rs));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al buscar ventas por fecha: " + date, e);
        }
        return sales;
    }
    
    /**
     * Busca ventas por rango de monto.
     */
    public List<Sale> findByAmountRange(BigDecimal minAmount, BigDecimal maxAmount) {
        List<Sale> sales = new ArrayList<>();
        String sql = "SELECT * FROM sales WHERE total BETWEEN ? AND ? ORDER BY created_at DESC";
        try {
            PreparedStatement pstmt = config.getConnection().prepareStatement(sql);
            pstmt.setBigDecimal(1, minAmount);
            pstmt.setBigDecimal(2, maxAmount);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                sales.add(mapResultSetToSale(rs));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al buscar ventas por rango de monto", e);
        }
        return sales;
    }
    
    /**
     * Obtiene ventas completadas.
     */
    public List<Sale> findCompleted() {
        List<Sale> sales = new ArrayList<>();
        String sql = "SELECT * FROM sales WHERE status = 'completed' ORDER BY created_at DESC";
        try {
            Statement stmt = config.getConnection().createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            
            while (rs.next()) {
                sales.add(mapResultSetToSale(rs));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al obtener ventas completadas", e);
        }
        return sales;
    }
    
    /**
     * Obtiene ventas anuladas.
     */
    public List<Sale> findCancelled() {
        List<Sale> sales = new ArrayList<>();
        String sql = "SELECT * FROM sales WHERE status = 'cancelled' ORDER BY created_at DESC";
        try {
            Statement stmt = config.getConnection().createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            
            while (rs.next()) {
                sales.add(mapResultSetToSale(rs));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al obtener ventas anuladas", e);
        }
        return sales;
    }
    
    // ==================== OPERACIONES DE ACTUALIZACIÓN ====================
    
    /**
     * Anula una venta.
     */
    public void cancel(int saleId) throws SQLException {
        String sql = "UPDATE sales SET status = 'cancelled', cancelled_at = NOW() WHERE id = ?";
        try {
            PreparedStatement pstmt = config.getConnection().prepareStatement(sql);
            pstmt.setInt(1, saleId);
            pstmt.executeUpdate();
            LOGGER.info("Venta #" + saleId + " anulada correctamente");
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al anular venta: " + saleId, e);
            throw e;
        }
    }
    
    // ==================== ESTADÍSTICAS ====================
    
    /**
     * Obtiene el total de ventas del día.
     */
    public BigDecimal dailyTotal(LocalDate date) {
        String sql = "SELECT COALESCE(SUM(total), 0) as total FROM sales WHERE DATE(created_at) = ? AND status = 'completed'";
        try {
            PreparedStatement pstmt = config.getConnection().prepareStatement(sql);
            pstmt.setDate(1, Date.valueOf(date));
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return rs.getBigDecimal("total");
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al calcular total diario", e);
        }
        return BigDecimal.ZERO;
    }
    
    /**
     * Obtiene el total de ventas del mes.
     */
    public BigDecimal monthlyTotal(int year, int month) {
        String sql = "SELECT COALESCE(SUM(total), 0) as total FROM sales WHERE YEAR(created_at) = ? AND MONTH(created_at) = ? AND status = 'completed'";
        try {
            PreparedStatement pstmt = config.getConnection().prepareStatement(sql);
            pstmt.setInt(1, year);
            pstmt.setInt(2, month);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return rs.getBigDecimal("total");
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al calcular total mensual", e);
        }
        return BigDecimal.ZERO;
    }
    
    /**
     * Obtiene el número de ventas del día.
     */
    public int dailyCount(LocalDate date) {
        String sql = "SELECT COUNT(*) as count FROM sales WHERE DATE(created_at) = ? AND status = 'completed'";
        try {
            PreparedStatement pstmt = config.getConnection().prepareStatement(sql);
            pstmt.setDate(1, Date.valueOf(date));
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt("count");
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al contar ventas diarias", e);
        }
        return 0;
    }
    
    // ==================== MÉTODOS AUXILIARES ====================
    
    /**
     * Mapea un ResultSet a un objeto Sale.
     */
    private Sale mapResultSetToSale(ResultSet rs) throws SQLException {
        Sale sale = new Sale();
        sale.setId(rs.getInt("id"));
        sale.setTotal(rs.getBigDecimal("total"));
        sale.setStatus(rs.getString("status"));
        sale.setUserName(rs.getString("user_name"));
        sale.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        
        // Campos opcionales
        Timestamp cancelledAt = rs.getTimestamp("cancelled_at");
        if (cancelledAt != null) {
            // Simplificado: no usar setCancelledAt si no existe
        }
        
        return sale;
    }
}
