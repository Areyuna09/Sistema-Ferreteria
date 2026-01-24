package com.ferreteria.models.dao;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;

/**
 * Data Access Object for sale statistics.
 * Extracted from SaleDAO following Single Responsibility Principle.
 */
public class SaleStatsDAO {

    private final DatabaseConfig config;

    public SaleStatsDAO(DatabaseConfig config) {
        this.config = config;
    }

    /**
     * Gets the total sales amount for a specific day.
     *
     * @param date the date to query
     * @return total sales amount
     */
    public BigDecimal dailyTotal(LocalDate date) {
        String sql = """
            SELECT COALESCE(SUM(total), 0) as total
            FROM sales
            WHERE DATE(created_at) = ? AND status = 'completed'
        """;
        try {
            PreparedStatement pstmt = config.getConnection().prepareStatement(sql);
            pstmt.setString(1, date.toString());
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getBigDecimal("total");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error getting daily total", e);
        }
        return BigDecimal.ZERO;
    }

    /**
     * Gets the count of sales for a specific day.
     *
     * @param date the date to query
     * @return number of sales
     */
    public int dailyCount(LocalDate date) {
        String sql = """
            SELECT COUNT(*) as count
            FROM sales
            WHERE DATE(created_at) = ? AND status = 'completed'
        """;
        try {
            PreparedStatement pstmt = config.getConnection().prepareStatement(sql);
            pstmt.setString(1, date.toString());
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("count");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error getting daily count", e);
        }
        return 0;
    }

    /**
     * Gets the total sales amount for a specific month.
     *
     * @param year the year
     * @param month the month (1-12)
     * @return total sales amount
     */
    public BigDecimal monthlyTotal(int year, int month) {
        String sql = """
            SELECT COALESCE(SUM(total), 0) as total
            FROM sales
            WHERE strftime('%Y', created_at) = ?
            AND strftime('%m', created_at) = ?
            AND status = 'completed'
        """;
        try {
            PreparedStatement pstmt = config.getConnection().prepareStatement(sql);
            pstmt.setString(1, String.valueOf(year));
            pstmt.setString(2, String.format("%02d", month));
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getBigDecimal("total");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error getting monthly total", e);
        }
        return BigDecimal.ZERO;
    }

    /**
     * Gets the count of sales for a specific month.
     *
     * @param year the year
     * @param month the month (1-12)
     * @return number of sales
     */
    public int monthlyCount(int year, int month) {
        String sql = """
            SELECT COUNT(*) as count
            FROM sales
            WHERE strftime('%Y', created_at) = ?
            AND strftime('%m', created_at) = ?
            AND status = 'completed'
        """;
        try {
            PreparedStatement pstmt = config.getConnection().prepareStatement(sql);
            pstmt.setString(1, String.valueOf(year));
            pstmt.setString(2, String.format("%02d", month));
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("count");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error getting monthly count", e);
        }
        return 0;
    }

    /**
     * Gets statistics for a specific seller.
     *
     * @param userId ID of the user
     * @return seller statistics
     */
    public SellerStats statsByUser(int userId) {
        String sql = """
            SELECT COUNT(*) as quantity,
                   COALESCE(SUM(total), 0) as total
            FROM sales
            WHERE user_id = ? AND status = 'completed'
        """;
        try {
            PreparedStatement pstmt = config.getConnection().prepareStatement(sql);
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return new SellerStats(
                    userId,
                    rs.getInt("quantity"),
                    rs.getBigDecimal("total")
                );
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error getting seller statistics", e);
        }
        return new SellerStats(userId, 0, BigDecimal.ZERO);
    }

    /**
     * Record for seller statistics.
     */
    public record SellerStats(
        int userId,
        int salesCount,
        BigDecimal totalSales
    ) {
        public BigDecimal averageSale() {
            if (salesCount == 0) return BigDecimal.ZERO;
            return totalSales.divide(BigDecimal.valueOf(salesCount), 2, RoundingMode.HALF_UP);
        }
    }
}
