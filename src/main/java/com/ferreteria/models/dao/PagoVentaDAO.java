package com.ferreteria.models.dao;

import com.ferreteria.models.PagoVenta;
import com.ferreteria.models.PagoVenta.MetodoPago;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Data Access Object para pagos de venta.
 * Permite manejar múltiples métodos de pago por venta.
 */
public class PagoVentaDAO {

    private final DatabaseConfig config;

    public PagoVentaDAO(DatabaseConfig config) {
        this.config = config;
    }

    /**
     * Crea un pago usando una conexión existente (para transacciones).
     *
     * @param conn conexión con transacción activa
     * @param saleId ID de la venta
     * @param pago el pago a crear
     * @return el pago creado con su ID
     * @throws SQLException si hay error de base de datos
     */
    public PagoVenta crear(Connection conn, int saleId, PagoVenta pago) throws SQLException {
        String sql = """
            INSERT INTO sale_payments (sale_id, payment_method, amount, reference)
            VALUES (?, ?, ?, ?)
        """;
        PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        pstmt.setInt(1, saleId);
        pstmt.setString(2, pago.getPaymentMethod().getValue());
        pstmt.setBigDecimal(3, pago.getAmount());
        pstmt.setString(4, pago.getReference());
        pstmt.executeUpdate();

        ResultSet keys = pstmt.getGeneratedKeys();
        if (keys.next()) {
            return new PagoVenta.Builder()
                .id(keys.getInt(1))
                .saleId(saleId)
                .paymentMethod(pago.getPaymentMethod())
                .amount(pago.getAmount())
                .reference(pago.getReference())
                .build();
        }
        return pago;
    }

    /**
     * Crea un pago (sin transacción externa).
     *
     * @param pago el pago a crear
     * @return el pago creado con su ID
     */
    public PagoVenta crear(PagoVenta pago) {
        try {
            return crear(config.getConnection(), pago.getSaleId(), pago);
        } catch (SQLException e) {
            throw new RuntimeException("Error creando pago", e);
        }
    }

    /**
     * Lista todos los pagos de una venta.
     *
     * @param saleId ID de la venta
     * @return lista de pagos
     */
    public List<PagoVenta> listarPorVenta(int saleId) {
        String sql = "SELECT * FROM sale_payments WHERE sale_id = ? ORDER BY id";
        List<PagoVenta> pagos = new ArrayList<>();

        try {
            PreparedStatement pstmt = config.getConnection().prepareStatement(sql);
            pstmt.setInt(1, saleId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                pagos.add(mapResultSetToPago(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error listando pagos de venta", e);
        }
        return pagos;
    }

    /**
     * Obtiene el total pagado en una venta.
     *
     * @param saleId ID de la venta
     * @return suma de todos los pagos
     */
    public BigDecimal totalPagado(int saleId) {
        String sql = "SELECT COALESCE(SUM(amount), 0) as total FROM sale_payments WHERE sale_id = ?";
        try {
            PreparedStatement pstmt = config.getConnection().prepareStatement(sql);
            pstmt.setInt(1, saleId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return rs.getBigDecimal("total");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error calculando total pagado", e);
        }
        return BigDecimal.ZERO;
    }

    /**
     * Obtiene totales agrupados por método de pago.
     *
     * @return mapa con método de pago y su total
     */
    public Map<MetodoPago, BigDecimal> totalesPorMetodo() {
        String sql = """
            SELECT payment_method, SUM(amount) as total
            FROM sale_payments
            GROUP BY payment_method
        """;
        Map<MetodoPago, BigDecimal> totales = new HashMap<>();

        try {
            Statement stmt = config.getConnection().createStatement();
            ResultSet rs = stmt.executeQuery(sql);

            while (rs.next()) {
                MetodoPago metodo = MetodoPago.fromValue(rs.getString("payment_method"));
                totales.put(metodo, rs.getBigDecimal("total"));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error obteniendo totales por método", e);
        }
        return totales;
    }

    /**
     * Obtiene totales por método de pago en un rango de fechas.
     *
     * @param desde fecha inicial
     * @param hasta fecha final
     * @return mapa con método de pago y su total
     */
    public Map<MetodoPago, BigDecimal> totalesPorMetodo(java.time.LocalDate desde, java.time.LocalDate hasta) {
        String sql = """
            SELECT sp.payment_method, SUM(sp.amount) as total
            FROM sale_payments sp
            JOIN sales s ON sp.sale_id = s.id
            WHERE DATE(s.created_at) BETWEEN ? AND ?
            AND s.status = 'completed'
            GROUP BY sp.payment_method
        """;
        Map<MetodoPago, BigDecimal> totales = new HashMap<>();

        try {
            PreparedStatement pstmt = config.getConnection().prepareStatement(sql);
            pstmt.setString(1, desde.toString());
            pstmt.setString(2, hasta.toString());
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                MetodoPago metodo = MetodoPago.fromValue(rs.getString("payment_method"));
                totales.put(metodo, rs.getBigDecimal("total"));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error obteniendo totales por método en rango", e);
        }
        return totales;
    }

    /**
     * Cuenta pagos por método.
     *
     * @param metodo el método de pago
     * @return cantidad de pagos con ese método
     */
    public int contarPorMetodo(MetodoPago metodo) {
        String sql = "SELECT COUNT(*) FROM sale_payments WHERE payment_method = ?";
        try {
            PreparedStatement pstmt = config.getConnection().prepareStatement(sql);
            pstmt.setString(1, metodo.getValue());
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error contando pagos por método", e);
        }
        return 0;
    }

    /**
     * Elimina todos los pagos de una venta (para anulación).
     * Debe usarse dentro de una transacción.
     *
     * @param conn conexión con transacción
     * @param saleId ID de la venta
     * @throws SQLException si hay error
     */
    public void eliminarPorVenta(Connection conn, int saleId) throws SQLException {
        String sql = "DELETE FROM sale_payments WHERE sale_id = ?";
        PreparedStatement pstmt = conn.prepareStatement(sql);
        pstmt.setInt(1, saleId);
        pstmt.executeUpdate();
    }

    private PagoVenta mapResultSetToPago(ResultSet rs) throws SQLException {
        return new PagoVenta.Builder()
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
