package com.ferreteria.models.dao;

import com.ferreteria.models.SalePayment;
import com.ferreteria.models.SalePayment.PaymentMethod;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Data Access Object for sale payments.
 * Allows handling multiple payment methods per sale.
 */
public class SalePaymentDAO {

    private final DatabaseConfig config;

    public SalePaymentDAO(DatabaseConfig config) {
        this.config = config;
    }

    /**
     * Creates a payment using an existing connection (for transactions).
     *
     * @param conn connection with active transaction
     * @param saleId ID of the sale
     * @param payment the payment to create
     * @return the created payment with its ID
     * @throws SQLException if database error occurs
     */
    public SalePayment create(Connection conn, int saleId, SalePayment payment) throws SQLException {
        String sql = """
            INSERT INTO sale_payments (sale_id, payment_method, amount, reference)
            VALUES (?, ?, ?, ?)
        """;
        PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        pstmt.setInt(1, saleId);
        pstmt.setString(2, payment.getPaymentMethod().getValue());
        pstmt.setBigDecimal(3, payment.getAmount());
        pstmt.setString(4, payment.getReference());
        pstmt.executeUpdate();

        ResultSet keys = pstmt.getGeneratedKeys();
        if (keys.next()) {
            return new SalePayment.Builder()
                .id(keys.getInt(1))
                .saleId(saleId)
                .paymentMethod(payment.getPaymentMethod())
                .amount(payment.getAmount())
                .reference(payment.getReference())
                .build();
        }
        return payment;
    }

    /**
     * Creates a payment (without external transaction).
     *
     * @param payment the payment to create
     * @return the created payment with its ID
     */
    public SalePayment create(SalePayment payment) {
        try {
            return create(config.getConnection(), payment.getSaleId(), payment);
        } catch (SQLException e) {
            throw new RuntimeException("Error creating payment", e);
        }
    }

    /**
     * Lists all payments for a sale.
     *
     * @param saleId ID of the sale
     * @return list of payments
     */
    public List<SalePayment> findBySaleId(int saleId) {
        String sql = "SELECT * FROM sale_payments WHERE sale_id = ? ORDER BY id";
        List<SalePayment> payments = new ArrayList<>();

        try {
            PreparedStatement pstmt = config.getConnection().prepareStatement(sql);
            pstmt.setInt(1, saleId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                payments.add(mapResultSet(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error listing sale payments", e);
        }
        return payments;
    }

    /**
     * Gets the total paid for a sale.
     *
     * @param saleId ID of the sale
     * @return sum of all payments
     */
    public BigDecimal totalPaid(int saleId) {
        String sql = "SELECT COALESCE(SUM(amount), 0) as total FROM sale_payments WHERE sale_id = ?";
        try {
            PreparedStatement pstmt = config.getConnection().prepareStatement(sql);
            pstmt.setInt(1, saleId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return rs.getBigDecimal("total");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error calculating total paid", e);
        }
        return BigDecimal.ZERO;
    }

    /**
     * Gets totals grouped by payment method.
     *
     * @return map with payment method and its total
     */
    public Map<PaymentMethod, BigDecimal> totalsByMethod() {
        String sql = """
            SELECT payment_method, SUM(amount) as total
            FROM sale_payments
            GROUP BY payment_method
        """;
        Map<PaymentMethod, BigDecimal> totals = new HashMap<>();

        try {
            Statement stmt = config.getConnection().createStatement();
            ResultSet rs = stmt.executeQuery(sql);

            while (rs.next()) {
                PaymentMethod method = PaymentMethod.fromValue(rs.getString("payment_method"));
                totals.put(method, rs.getBigDecimal("total"));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error getting totals by method", e);
        }
        return totals;
    }

    /**
     * Gets totals by payment method in a date range.
     *
     * @param from start date
     * @param to end date
     * @return map with payment method and its total
     */
    public Map<PaymentMethod, BigDecimal> totalsByMethod(java.time.LocalDate from, java.time.LocalDate to) {
        String sql = """
            SELECT sp.payment_method, SUM(sp.amount) as total
            FROM sale_payments sp
            JOIN sales s ON sp.sale_id = s.id
            WHERE DATE(s.created_at) BETWEEN ? AND ?
            AND s.status = 'completed'
            GROUP BY sp.payment_method
        """;
        Map<PaymentMethod, BigDecimal> totals = new HashMap<>();

        try {
            PreparedStatement pstmt = config.getConnection().prepareStatement(sql);
            pstmt.setString(1, from.toString());
            pstmt.setString(2, to.toString());
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                PaymentMethod method = PaymentMethod.fromValue(rs.getString("payment_method"));
                totals.put(method, rs.getBigDecimal("total"));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error getting totals by method in range", e);
        }
        return totals;
    }

    /**
     * Counts payments by method.
     *
     * @param method the payment method
     * @return count of payments with that method
     */
    public int countByMethod(PaymentMethod method) {
        String sql = "SELECT COUNT(*) FROM sale_payments WHERE payment_method = ?";
        try {
            PreparedStatement pstmt = config.getConnection().prepareStatement(sql);
            pstmt.setString(1, method.getValue());
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error counting payments by method", e);
        }
        return 0;
    }

    /**
     * Deletes all payments for a sale (for cancellation).
     * Should be used within a transaction.
     *
     * @param conn connection with transaction
     * @param saleId ID of the sale
     * @throws SQLException if error occurs
     */
    public void deleteBySaleId(Connection conn, int saleId) throws SQLException {
        String sql = "DELETE FROM sale_payments WHERE sale_id = ?";
        PreparedStatement pstmt = conn.prepareStatement(sql);
        pstmt.setInt(1, saleId);
        pstmt.executeUpdate();
    }

    private SalePayment mapResultSet(ResultSet rs) throws SQLException {
        return new SalePayment.Builder()
            .id(rs.getInt("id"))
            .saleId(rs.getInt("sale_id"))
            .paymentMethod(rs.getString("payment_method"))
            .amount(rs.getBigDecimal("amount"))
            .reference(rs.getString("reference"))
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
}
