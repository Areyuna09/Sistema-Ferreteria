package com.ferreteria.models.dao;

import com.ferreteria.models.Sale;
import com.ferreteria.models.SaleItem;
import com.ferreteria.models.SalePayment;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * DAO para consultas de reportes y estadísticas de ventas.
 * Proporciona métodos para obtener datos agregados por período.
 * 
 * @author Sistema Ferretería
 * @version 1.0
 */
public class ReportDAO {
    private static final Logger LOGGER = Logger.getLogger(ReportDAO.class.getName());
    private final DatabaseConfig dbConfig;

    public ReportDAO() {
        this.dbConfig = DatabaseConfig.getInstance();
    }

    /**
     * Obtiene todas las ventas de un mes específico
     * 
     * @param yearMonth Mes y año a consultar (formato: YYYY-MM)
     * @return Lista de ventas del mes
     */
    public List<Sale> getSalesByMonth(YearMonth yearMonth) {
        List<Sale> sales = new ArrayList<>();
        String query = "SELECT s.id, s.user_id, u.full_name, s.total, s.status, " +
                      "s.notes, s.created_at " +
                      "FROM sales s " +
                      "LEFT JOIN users u ON s.user_id = u.id " +
                      "WHERE strftime('%Y-%m', s.created_at) = ? " +
                      "ORDER BY s.created_at DESC";

        try (Connection conn = dbConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setString(1, yearMonth.toString());
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Sale sale = new Sale.Builder()
                        .id(rs.getInt("id"))
                        .userId(rs.getInt("user_id"))
                        .userName(rs.getString("full_name"))
                        .total(rs.getBigDecimal("total"))
                        .status(rs.getString("status"))
                        .notes(rs.getString("notes"))
                        .createdAt(rs.getTimestamp("created_at").toLocalDateTime())
                        .build();
                    
                    sales.add(sale);
                }
            }
            
            LOGGER.info("Ventas encontradas para " + yearMonth + ": " + sales.size());
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al obtener ventas por mes", e);
        }

        return sales;
    }

    /**
     * Obtiene resumen de productos vendidos en un mes
     * Agrupa por producto y variante con cantidades totales
     * 
     * @param yearMonth Mes a consultar
     * @return Mapa con producto como clave y datos como valor
     */
    public List<Map<String, Object>> getProductSalesSummary(YearMonth yearMonth) {
        List<Map<String, Object>> summary = new ArrayList<>();
        String query = "SELECT " +
                      "p.name as producto, " +
                      "pv.variant_name as variante, " +
                      "SUM(si.quantity) as cantidad_total, " +
                      "si.unit_price as precio_unitario, " +
                      "SUM(si.subtotal) as total_vendido " +
                      "FROM sale_items si " +
                      "INNER JOIN sales s ON si.sale_id = s.id " +
                      "INNER JOIN product_variants pv ON si.variant_id = pv.id " +
                      "INNER JOIN products p ON pv.product_id = p.id " +
                      "WHERE strftime('%Y-%m', s.created_at) = ? " +
                      "GROUP BY p.id, pv.id " +
                      "ORDER BY total_vendido DESC";

        try (Connection conn = dbConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setString(1, yearMonth.toString());
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new HashMap<>();
                    row.put("producto", rs.getString("producto"));
                    row.put("variante", rs.getString("variante"));
                    row.put("cantidad", rs.getInt("cantidad_total"));
                    row.put("precio", rs.getBigDecimal("precio_unitario"));
                    row.put("total", rs.getBigDecimal("total_vendido"));
                    
                    summary.add(row);
                }
            }
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al obtener resumen de productos", e);
        }

        return summary;
    }

    /**
     * Obtiene totales por método de pago en un mes específico
     * 
     * @param yearMonth Mes a consultar
     * @return Mapa con método de pago y total recaudado
     */
    public Map<String, BigDecimal> getPaymentMethodTotals(YearMonth yearMonth) {
        Map<String, BigDecimal> totals = new LinkedHashMap<>();
        String query = "SELECT " +
                      "sp.payment_method, " +
                      "SUM(sp.amount) as total " +
                      "FROM sale_payments sp " +
                      "INNER JOIN sales s ON sp.sale_id = s.id " +
                      "WHERE strftime('%Y-%m', s.created_at) = ? " +
                      "GROUP BY sp.payment_method " +
                      "ORDER BY total DESC";

        try (Connection conn = dbConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setString(1, yearMonth.toString());
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String method = rs.getString("payment_method");
                    BigDecimal total = rs.getBigDecimal("total");
                    totals.put(method, total);
                }
            }
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al obtener totales por método de pago", e);
        }

        return totals;
    }

    /**
     * Obtiene ventas agrupadas por día del mes (para gráficos)
     * 
     * @param yearMonth Mes a consultar
     * @return Mapa con día del mes y total vendido
     */
    public Map<Integer, BigDecimal> getDailySales(YearMonth yearMonth) {
        Map<Integer, BigDecimal> dailySales = new TreeMap<>();
        
        // Agregar log para debugging
        LOGGER.info("Consultando ventas diarias para: " + yearMonth.toString());
        
        String query = "SELECT " +
                      "CAST(strftime('%d', s.created_at) AS INTEGER) as dia, " +
                      "SUM(s.total) as total " +
                      "FROM sales s " +
                      "WHERE strftime('%Y-%m', s.created_at) = ? " +
                      "AND s.status = 'completed' " +
                      "GROUP BY dia " +
                      "ORDER BY dia";

        try (Connection conn = dbConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setString(1, yearMonth.toString());
            
            LOGGER.info("Ejecutando query con periodo: " + yearMonth.toString());
            
            try (ResultSet rs = stmt.executeQuery()) {
                int rowCount = 0;
                while (rs.next()) {
                    int day = rs.getInt("dia");
                    BigDecimal total = rs.getBigDecimal("total");
                    dailySales.put(day, total);
                    rowCount++;
                    LOGGER.info("Dia " + day + ": $" + total);
                }
                
                if (rowCount == 0) {
                    LOGGER.warning("No se encontraron ventas diarias para " + yearMonth);
                    
                    // Query de debug para ver que hay en la tabla
                    String debugQuery = "SELECT COUNT(*) as total, " +
                                       "MIN(created_at) as primera, " +
                                       "MAX(created_at) as ultima " +
                                       "FROM sales WHERE status = 'completed'";
                    
                    try (Statement debugStmt = conn.createStatement();
                         ResultSet debugRs = debugStmt.executeQuery(debugQuery)) {
                        if (debugRs.next()) {
                            LOGGER.info("Total ventas en BD: " + debugRs.getInt("total"));
                            LOGGER.info("Primera venta: " + debugRs.getString("primera"));
                            LOGGER.info("Ultima venta: " + debugRs.getString("ultima"));
                        }
                    }
                }
            }
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al obtener ventas diarias", e);
        }

        return dailySales;
    }

    /**
     * Calcula el total general de ventas de un mes
     * 
     * @param yearMonth Mes a consultar
     * @return Total en BigDecimal
     */
    public BigDecimal getMonthlyTotal(YearMonth yearMonth) {
        String query = "SELECT COALESCE(SUM(total), 0) as total_mes " +
                      "FROM sales " +
                      "WHERE strftime('%Y-%m', created_at) = ?";

        try (Connection conn = dbConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setString(1, yearMonth.toString());
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getBigDecimal("total_mes");
                }
            }
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al calcular total mensual", e);
        }

        return BigDecimal.ZERO;
    }

    /**
     * Obtiene estadísticas generales del mes
     * 
     * @param yearMonth Mes a consultar
     * @return Mapa con diferentes métricas
     */
    public Map<String, Object> getMonthlyStats(YearMonth yearMonth) {
        Map<String, Object> stats = new HashMap<>();
        String query = "SELECT " +
                      "COUNT(*) as total_ventas, " +
                      "COALESCE(SUM(total), 0) as total_recaudado, " +
                      "COALESCE(AVG(total), 0) as promedio_venta, " +
                      "COALESCE(MAX(total), 0) as venta_maxima, " +
                      "COALESCE(MIN(total), 0) as venta_minima " +
                      "FROM sales " +
                      "WHERE strftime('%Y-%m', created_at) = ?";

        try (Connection conn = dbConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setString(1, yearMonth.toString());
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    stats.put("totalVentas", rs.getInt("total_ventas"));
                    stats.put("totalRecaudado", rs.getBigDecimal("total_recaudado"));
                    stats.put("promedioVenta", rs.getBigDecimal("promedio_venta"));
                    stats.put("ventaMaxima", rs.getBigDecimal("venta_maxima"));
                    stats.put("ventaMinima", rs.getBigDecimal("venta_minima"));
                }
            }
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al obtener estadísticas mensuales", e);
            stats.put("totalVentas", 0);
            stats.put("totalRecaudado", BigDecimal.ZERO);
            stats.put("promedioVenta", BigDecimal.ZERO);
            stats.put("ventaMaxima", BigDecimal.ZERO);
            stats.put("ventaMinima", BigDecimal.ZERO);
        }

        return stats;
    }
}