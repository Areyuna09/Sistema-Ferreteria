package com.ferreteria.models.dao;

import com.ferreteria.models.Sale;
import com.ferreteria.models.SaleItem;
import com.ferreteria.models.SalePayment;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object for sales.
 * Handles transactions for creating sales with stock updates and payments.
 */
public class SaleDAO {

    private final DatabaseConfig config;
    private final SaleItemDAO itemDAO;
    private final SalePaymentDAO paymentDAO;

    public SaleDAO(DatabaseConfig config) {
        this.config = config;
        this.itemDAO = new SaleItemDAO(config);
        this.paymentDAO = new SalePaymentDAO(config);
    }

    /**
     * Creates a complete sale with items, payments and updates stock.
     * Uses transaction to guarantee consistency.
     *
     * @param sale the sale to create with its items and payments
     * @return the created sale with its assigned ID
     * @throws RuntimeException if transaction error occurs
     */
    public Sale create(Sale sale) {
        Connection conn = null;
        try {
            conn = config.getConnection();
            conn.setAutoCommit(false);

            // 1. Insert sale header
            int saleId = insertSale(conn, sale);

            // 2. Insert items and update stock
            for (SaleItem item : sale.getItems()) {
                itemDAO.create(conn, saleId, item);
                updateStock(conn, item.getVariantId(), -item.getQuantity());
            }

            // 3. Insert payments
            for (SalePayment payment : sale.getPayments()) {
                paymentDAO.create(conn, saleId, payment);
            }

            conn.commit();
            return findById(saleId).orElse(sale);

        } catch (SQLException e) {
            rollback(conn);
            throw new RuntimeException("Error creating sale: " + e.getMessage(), e);
        } finally {
            setAutoCommitTrue(conn);
        }
    }

    /**
     * Cancels a sale and reverts stock for all its items.
     *
     * @param saleId ID of the sale to cancel
     * @throws RuntimeException if sale doesn't exist or is already cancelled
     */
    public void cancel(int saleId) {
        Connection conn = null;
        try {
            conn = config.getConnection();
            conn.setAutoCommit(false);

            // Verify it exists and is not cancelled
            Sale sale = findById(saleId)
                .orElseThrow(() -> new RuntimeException("Sale not found: " + saleId));

            if (sale.isCancelled()) {
                throw new RuntimeException("Sale is already cancelled");
            }

            // 1. Change status to cancelled
            String sql = "UPDATE sales SET status = 'cancelled' WHERE id = ?";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, saleId);
            pstmt.executeUpdate();

            // 2. Revert stock for each item
            List<SaleItem> items = itemDAO.findBySaleId(saleId);
            for (SaleItem item : items) {
                updateStock(conn, item.getVariantId(), item.getQuantity());
            }

            conn.commit();

        } catch (SQLException e) {
            rollback(conn);
            throw new RuntimeException("Error cancelling sale: " + e.getMessage(), e);
        } finally {
            setAutoCommitTrue(conn);
        }
    }

    /**
     * Deletes a cancelled sale permanently.
     * Only cancelled sales can be deleted.
     *
     * @param saleId ID of the sale to delete
     * @throws RuntimeException if sale doesn't exist or is not cancelled
     */
    public void delete(int saleId) {
        Connection conn = null;
        try {
            conn = config.getConnection();
            conn.setAutoCommit(false);

            // Verify it exists and is cancelled
            Sale sale = findById(saleId)
                .orElseThrow(() -> new RuntimeException("Sale not found: " + saleId));

            if (!sale.isCancelled()) {
                throw new RuntimeException("Only cancelled sales can be deleted");
            }

            // Delete in order: payments, items, then sale
            PreparedStatement pstmt = conn.prepareStatement("DELETE FROM sale_payments WHERE sale_id = ?");
            pstmt.setInt(1, saleId);
            pstmt.executeUpdate();

            pstmt = conn.prepareStatement("DELETE FROM sale_items WHERE sale_id = ?");
            pstmt.setInt(1, saleId);
            pstmt.executeUpdate();

            pstmt = conn.prepareStatement("DELETE FROM sales WHERE id = ?");
            pstmt.setInt(1, saleId);
            pstmt.executeUpdate();

            conn.commit();

        } catch (SQLException e) {
            rollback(conn);
            throw new RuntimeException("Error deleting sale: " + e.getMessage(), e);
        } finally {
            setAutoCommitTrue(conn);
        }
    }

    /**
     * Updates a sale's date/time.
     *
     * @param saleId ID of the sale to update
     * @param newDateTime new date/time for the sale
     * @throws RuntimeException if sale doesn't exist
     */
    public void updateDateTime(int saleId, LocalDateTime newDateTime) {
        String sql = "UPDATE sales SET created_at = ? WHERE id = ?";
        try {
            PreparedStatement pstmt = config.getConnection().prepareStatement(sql);
            pstmt.setString(1, newDateTime.toString().replace("T", " "));
            pstmt.setInt(2, saleId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error updating sale date: " + e.getMessage(), e);
        }
    }

    /**
     * Updates a sale's notes.
     *
     * @param saleId ID of the sale to update
     * @param notes new notes for the sale
     */
    public void updateNotes(int saleId, String notes) {
        String sql = "UPDATE sales SET notes = ? WHERE id = ?";
        try {
            PreparedStatement pstmt = config.getConnection().prepareStatement(sql);
            pstmt.setString(1, notes);
            pstmt.setInt(2, saleId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error updating sale notes: " + e.getMessage(), e);
        }
    }

    /**
     * Updates a sale's total.
     *
     * @param saleId ID of the sale to update
     * @param newTotal new total amount
     */
    public void updateTotal(int saleId, BigDecimal newTotal) {
        String sql = "UPDATE sales SET total = ? WHERE id = ?";
        try {
            PreparedStatement pstmt = config.getConnection().prepareStatement(sql);
            pstmt.setBigDecimal(1, newTotal);
            pstmt.setInt(2, saleId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error updating sale total: " + e.getMessage(), e);
        }
    }

    /**
     * Gets the SaleItemDAO for direct item operations.
     */
    public SaleItemDAO getItemDAO() {
        return itemDAO;
    }

    /**
     * Gets the SalePaymentDAO for direct payment operations.
     */
    public SalePaymentDAO getPaymentDAO() {
        return paymentDAO;
    }

    /**
     * Finds a sale by its ID with all its details.
     *
     * @param id ID of the sale
     * @return Optional with the complete sale if exists
     */
    public Optional<Sale> findById(int id) {
        String sql = """
            SELECT s.*, u.full_name as user_name
            FROM sales s
            LEFT JOIN users u ON s.user_id = u.id
            WHERE s.id = ?
        """;
        try {
            PreparedStatement pstmt = config.getConnection().prepareStatement(sql);
            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                // Load items and payments
                List<SaleItem> items = itemDAO.findBySaleId(id);
                List<SalePayment> payments = paymentDAO.findBySaleId(id);

                return Optional.of(new Sale.Builder()
                    .id(rs.getInt("id"))
                    .userId(rs.getInt("user_id"))
                    .total(rs.getBigDecimal("total"))
                    .status(rs.getString("status"))
                    .notes(rs.getString("notes"))
                    .createdAt(parseDateTime(rs.getString("created_at")))
                    .userName(rs.getString("user_name"))
                    .items(items)
                    .payments(payments)
                    .build());
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding sale by ID", e);
        }
        return Optional.empty();
    }

    /**
     * Lists sales for a specific date.
     *
     * @param date the date to search
     * @return list of sales for that day
     */
    public List<Sale> findByDate(LocalDate date) {
        String sql = """
            SELECT * FROM sales
            WHERE DATE(created_at) = ?
            ORDER BY created_at DESC
        """;
        List<Sale> sales = new ArrayList<>();

        try {
            PreparedStatement pstmt = config.getConnection().prepareStatement(sql);
            pstmt.setString(1, date.toString());
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                sales.add(mapResultSet(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error listing sales by date", e);
        }
        return sales;
    }

    /**
     * Lists sales for a specific month.
     *
     * @param year year
     * @param month month (1-12)
     * @return list of sales for the month
     */
    public List<Sale> findByMonth(int year, int month) {
        String sql = """
            SELECT * FROM sales
            WHERE strftime('%Y', created_at) = ?
            AND strftime('%m', created_at) = ?
            ORDER BY created_at DESC
        """;
        List<Sale> sales = new ArrayList<>();

        try {
            PreparedStatement pstmt = config.getConnection().prepareStatement(sql);
            pstmt.setString(1, String.valueOf(year));
            pstmt.setString(2, String.format("%02d", month));
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                sales.add(mapResultSet(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error listing sales by month", e);
        }
        return sales;
    }

    /**
     * Lists all completed sales.
     *
     * @return list of sales with status 'completed'
     */
    public List<Sale> findCompleted() {
        return findByStatus("completed");
    }

    /**
     * Lists all cancelled sales.
     *
     * @return list of sales with status 'cancelled'
     */
    public List<Sale> findCancelled() {
        return findByStatus("cancelled");
    }

    /**
     * Lists sales by status.
     *
     * @param status the status to filter
     * @return list of sales
     */
    public List<Sale> findByStatus(String status) {
        String sql = "SELECT * FROM sales WHERE status = ? ORDER BY created_at DESC";
        List<Sale> sales = new ArrayList<>();

        try {
            PreparedStatement pstmt = config.getConnection().prepareStatement(sql);
            pstmt.setString(1, status);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                sales.add(mapResultSet(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error listing sales by status", e);
        }
        return sales;
    }

    /**
     * Gets the total sales for a day.
     *
     * @param date the date to query
     * @return sum of completed sales totals
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
            throw new RuntimeException("Error calculating daily total", e);
        }
        return BigDecimal.ZERO;
    }

    /**
     * Counts sales for a day.
     *
     * @param date the date to query
     * @return count of completed sales
     */
    public int dailyCount(LocalDate date) {
        String sql = """
            SELECT COUNT(*) FROM sales
            WHERE DATE(created_at) = ? AND status = 'completed'
        """;
        try {
            PreparedStatement pstmt = config.getConnection().prepareStatement(sql);
            pstmt.setString(1, date.toString());
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error counting daily sales", e);
        }
        return 0;
    }

    /**
     * Lists all sales ordered by date descending.
     *
     * @return list of all sales
     */
    public List<Sale> findAll() {
        String sql = "SELECT * FROM sales ORDER BY created_at DESC";
        List<Sale> sales = new ArrayList<>();

        try {
            Statement stmt = config.getConnection().createStatement();
            ResultSet rs = stmt.executeQuery(sql);

            while (rs.next()) {
                sales.add(mapResultSet(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error listing all sales", e);
        }
        return sales;
    }

    /**
     * Lists sales with pagination.
     *
     * @param limit maximum number of results
     * @param offset starting record
     * @return paginated list of sales
     */
    public List<Sale> findPaginated(int limit, int offset) {
        String sql = "SELECT * FROM sales ORDER BY created_at DESC LIMIT ? OFFSET ?";
        List<Sale> sales = new ArrayList<>();

        try {
            PreparedStatement pstmt = config.getConnection().prepareStatement(sql);
            pstmt.setInt(1, limit);
            pstmt.setInt(2, offset);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                sales.add(mapResultSet(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error listing paginated sales", e);
        }
        return sales;
    }

    /**
     * Lists sales for a specific user.
     *
     * @param userId ID of the user/seller
     * @return list of user's sales
     */
    public List<Sale> findByUserId(int userId) {
        String sql = "SELECT * FROM sales WHERE user_id = ? ORDER BY created_at DESC";
        List<Sale> sales = new ArrayList<>();

        try {
            PreparedStatement pstmt = config.getConnection().prepareStatement(sql);
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                sales.add(mapResultSet(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error listing sales by user", e);
        }
        return sales;
    }

    /**
     * Lists sales in a date range.
     *
     * @param from start date (inclusive)
     * @param to end date (inclusive)
     * @return list of sales in range
     */
    public List<Sale> findByDateRange(LocalDate from, LocalDate to) {
        String sql = """
            SELECT * FROM sales
            WHERE DATE(created_at) BETWEEN ? AND ?
            ORDER BY created_at DESC
        """;
        List<Sale> sales = new ArrayList<>();

        try {
            PreparedStatement pstmt = config.getConnection().prepareStatement(sql);
            pstmt.setString(1, from.toString());
            pstmt.setString(2, to.toString());
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                sales.add(mapResultSet(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error listing sales by date range", e);
        }
        return sales;
    }

    /**
     * Counts total sales.
     *
     * @return total count of sales
     */
    public int count() {
        String sql = "SELECT COUNT(*) FROM sales";
        try {
            Statement stmt = config.getConnection().createStatement();
            ResultSet rs = stmt.executeQuery(sql);

            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error counting sales", e);
        }
        return 0;
    }

    /**
     * Counts completed sales.
     *
     * @return count of completed sales
     */
    public int countCompleted() {
        String sql = "SELECT COUNT(*) FROM sales WHERE status = 'completed'";
        try {
            Statement stmt = config.getConnection().createStatement();
            ResultSet rs = stmt.executeQuery(sql);

            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error counting completed sales", e);
        }
        return 0;
    }

    /**
     * Gets the monthly sales total.
     *
     * @param year year
     * @param month month (1-12)
     * @return sum of completed sales totals for the month
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
            throw new RuntimeException("Error calculating monthly total", e);
        }
        return BigDecimal.ZERO;
    }

    /**
     * Gets the overall total of completed sales.
     *
     * @return sum of all totals
     */
    public BigDecimal overallTotal() {
        String sql = "SELECT COALESCE(SUM(total), 0) as total FROM sales WHERE status = 'completed'";
        try {
            Statement stmt = config.getConnection().createStatement();
            ResultSet rs = stmt.executeQuery(sql);

            if (rs.next()) {
                return rs.getBigDecimal("total");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error calculating overall total", e);
        }
        return BigDecimal.ZERO;
    }

    /**
     * Gets sales statistics for a user.
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
            return totalSales.divide(BigDecimal.valueOf(salesCount), 2, java.math.RoundingMode.HALF_UP);
        }
    }

    // Private helper methods

    private int insertSale(Connection conn, Sale sale) throws SQLException {
        String sql = """
            INSERT INTO sales (user_id, total, status, notes, created_at)
            VALUES (?, ?, ?, ?, datetime('now', 'localtime'))
        """;
        PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        pstmt.setInt(1, sale.getUserId());
        pstmt.setBigDecimal(2, sale.getTotal());
        pstmt.setString(3, sale.getStatus());
        pstmt.setString(4, sale.getNotes());
        pstmt.executeUpdate();

        ResultSet keys = pstmt.getGeneratedKeys();
        if (keys.next()) {
            return keys.getInt(1);
        }
        throw new SQLException("Could not get created sale ID");
    }

    private void updateStock(Connection conn, int variantId, int quantity) throws SQLException {
        String sql = "UPDATE product_variants SET stock = stock + ? WHERE id = ?";
        PreparedStatement pstmt = conn.prepareStatement(sql);
        pstmt.setInt(1, quantity);
        pstmt.setInt(2, variantId);
        pstmt.executeUpdate();
    }

    private Sale mapResultSet(ResultSet rs) throws SQLException {
        return new Sale.Builder()
            .id(rs.getInt("id"))
            .userId(rs.getInt("user_id"))
            .total(rs.getBigDecimal("total"))
            .status(rs.getString("status"))
            .notes(rs.getString("notes"))
            .createdAt(parseDateTime(rs.getString("created_at")))
            .build();
    }

    private LocalDateTime parseDateTime(String dateStr) {
        if (dateStr == null) return LocalDateTime.now();
        try {
            return LocalDateTime.parse(dateStr.replace(" ", "T"));
        } catch (Exception e) {
            return LocalDateTime.now();
        }
    }

    private void rollback(Connection conn) {
        try {
            if (conn != null) conn.rollback();
        } catch (SQLException e) {
            System.err.println("Rollback error: " + e.getMessage());
        }
    }

    private void setAutoCommitTrue(Connection conn) {
        try {
            if (conn != null) conn.setAutoCommit(true);
        } catch (SQLException e) {
            System.err.println("Error restoring autocommit: " + e.getMessage());
        }
    }
}
