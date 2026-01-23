package com.ferreteria.models.dao;

import com.ferreteria.models.DetalleVenta;
import com.ferreteria.models.PagoVenta;
import com.ferreteria.models.Venta;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object para ventas.
 * Maneja transacciones para crear ventas con actualización de stock y pagos.
 */
public class VentaDAO {

    private final DatabaseConfig config;
    private final DetalleVentaDAO detalleDAO;
    private final PagoVentaDAO pagoDAO;

    public VentaDAO(DatabaseConfig config) {
        this.config = config;
        this.detalleDAO = new DetalleVentaDAO(config);
        this.pagoDAO = new PagoVentaDAO(config);
    }

    /**
     * Crea una venta completa con items, pagos y actualiza el stock.
     * Usa transacción para garantizar consistencia.
     *
     * @param venta la venta a crear con sus items y pagos
     * @return la venta creada con su ID asignado
     * @throws RuntimeException si hay error en la transacción
     */
    public Venta crear(Venta venta) {
        Connection conn = null;
        try {
            conn = config.getConnection();
            conn.setAutoCommit(false);

            // 1. Insertar cabecera de venta
            int ventaId = insertarVenta(conn, venta);

            // 2. Insertar items y actualizar stock
            for (DetalleVenta item : venta.getItems()) {
                detalleDAO.crear(conn, ventaId, item);
                actualizarStock(conn, item.getVariantId(), -item.getQuantity());
            }

            // 3. Insertar pagos
            for (PagoVenta pago : venta.getPagos()) {
                pagoDAO.crear(conn, ventaId, pago);
            }

            conn.commit();
            return buscarPorId(ventaId).orElse(venta);

        } catch (SQLException e) {
            rollback(conn);
            throw new RuntimeException("Error creando venta: " + e.getMessage(), e);
        } finally {
            setAutoCommitTrue(conn);
        }
    }

    /**
     * Anula una venta y revierte el stock de todos sus items.
     *
     * @param ventaId ID de la venta a anular
     * @throws RuntimeException si la venta no existe o ya está anulada
     */
    public void anular(int ventaId) {
        Connection conn = null;
        try {
            conn = config.getConnection();
            conn.setAutoCommit(false);

            // Verificar que existe y no está anulada
            Venta venta = buscarPorId(ventaId)
                .orElseThrow(() -> new RuntimeException("Venta no encontrada: " + ventaId));

            if (venta.isCancelled()) {
                throw new RuntimeException("La venta ya está anulada");
            }

            // 1. Cambiar status a cancelled
            String sql = "UPDATE sales SET status = 'cancelled' WHERE id = ?";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, ventaId);
            pstmt.executeUpdate();

            // 2. Revertir stock de cada item
            List<DetalleVenta> items = detalleDAO.listarPorVenta(ventaId);
            for (DetalleVenta item : items) {
                actualizarStock(conn, item.getVariantId(), item.getQuantity());
            }

            conn.commit();

        } catch (SQLException e) {
            rollback(conn);
            throw new RuntimeException("Error anulando venta: " + e.getMessage(), e);
        } finally {
            setAutoCommitTrue(conn);
        }
    }

    /**
     * Busca una venta por su ID con todos sus detalles.
     *
     * @param id ID de la venta
     * @return Optional con la venta completa si existe
     */
    public Optional<Venta> buscarPorId(int id) {
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
                // Cargar items y pagos
                List<DetalleVenta> items = detalleDAO.listarPorVenta(id);
                List<PagoVenta> pagos = pagoDAO.listarPorVenta(id);

                return Optional.of(new Venta.Builder()
                    .id(rs.getInt("id"))
                    .userId(rs.getInt("user_id"))
                    .total(rs.getBigDecimal("total"))
                    .status(rs.getString("status"))
                    .notes(rs.getString("notes"))
                    .createdAt(parseDateTime(rs.getString("created_at")))
                    .userName(rs.getString("user_name"))
                    .items(items)
                    .pagos(pagos)
                    .build());
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error buscando venta por ID", e);
        }
        return Optional.empty();
    }

    /**
     * Lista ventas de una fecha específica.
     *
     * @param fecha la fecha a buscar
     * @return lista de ventas de ese día
     */
    public List<Venta> listarPorFecha(LocalDate fecha) {
        String sql = """
            SELECT * FROM sales
            WHERE DATE(created_at) = ?
            ORDER BY created_at DESC
        """;
        List<Venta> ventas = new ArrayList<>();

        try {
            PreparedStatement pstmt = config.getConnection().prepareStatement(sql);
            pstmt.setString(1, fecha.toString());
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                ventas.add(mapResultSetToVenta(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error listando ventas por fecha", e);
        }
        return ventas;
    }

    /**
     * Lista ventas de un mes específico.
     *
     * @param year año
     * @param month mes (1-12)
     * @return lista de ventas del mes
     */
    public List<Venta> listarPorMes(int year, int month) {
        String sql = """
            SELECT * FROM sales
            WHERE strftime('%Y', created_at) = ?
            AND strftime('%m', created_at) = ?
            ORDER BY created_at DESC
        """;
        List<Venta> ventas = new ArrayList<>();

        try {
            PreparedStatement pstmt = config.getConnection().prepareStatement(sql);
            pstmt.setString(1, String.valueOf(year));
            pstmt.setString(2, String.format("%02d", month));
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                ventas.add(mapResultSetToVenta(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error listando ventas por mes", e);
        }
        return ventas;
    }

    /**
     * Lista todas las ventas completadas.
     *
     * @return lista de ventas con status 'completed'
     */
    public List<Venta> listarCompletadas() {
        return listarPorStatus("completed");
    }

    /**
     * Lista todas las ventas anuladas.
     *
     * @return lista de ventas con status 'cancelled'
     */
    public List<Venta> listarAnuladas() {
        return listarPorStatus("cancelled");
    }

    /**
     * Lista ventas por status.
     *
     * @param status el status a filtrar
     * @return lista de ventas
     */
    public List<Venta> listarPorStatus(String status) {
        String sql = "SELECT * FROM sales WHERE status = ? ORDER BY created_at DESC";
        List<Venta> ventas = new ArrayList<>();

        try {
            PreparedStatement pstmt = config.getConnection().prepareStatement(sql);
            pstmt.setString(1, status);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                ventas.add(mapResultSetToVenta(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error listando ventas por status", e);
        }
        return ventas;
    }

    /**
     * Obtiene el total de ventas del día.
     *
     * @param fecha la fecha a consultar
     * @return suma de totales de ventas completadas
     */
    public BigDecimal totalDelDia(LocalDate fecha) {
        String sql = """
            SELECT COALESCE(SUM(total), 0) as total
            FROM sales
            WHERE DATE(created_at) = ? AND status = 'completed'
        """;
        try {
            PreparedStatement pstmt = config.getConnection().prepareStatement(sql);
            pstmt.setString(1, fecha.toString());
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return rs.getBigDecimal("total");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error calculando total del día", e);
        }
        return BigDecimal.ZERO;
    }

    /**
     * Cuenta las ventas del día.
     *
     * @param fecha la fecha a consultar
     * @return cantidad de ventas completadas
     */
    public int contarDelDia(LocalDate fecha) {
        String sql = """
            SELECT COUNT(*) FROM sales
            WHERE DATE(created_at) = ? AND status = 'completed'
        """;
        try {
            PreparedStatement pstmt = config.getConnection().prepareStatement(sql);
            pstmt.setString(1, fecha.toString());
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error contando ventas del día", e);
        }
        return 0;
    }

    /**
     * Lista todas las ventas ordenadas por fecha descendente.
     *
     * @return lista de todas las ventas
     */
    public List<Venta> listarTodas() {
        String sql = "SELECT * FROM sales ORDER BY created_at DESC";
        List<Venta> ventas = new ArrayList<>();

        try {
            Statement stmt = config.getConnection().createStatement();
            ResultSet rs = stmt.executeQuery(sql);

            while (rs.next()) {
                ventas.add(mapResultSetToVenta(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error listando todas las ventas", e);
        }
        return ventas;
    }

    /**
     * Lista ventas con paginación.
     *
     * @param limit cantidad máxima de resultados
     * @param offset desde qué registro empezar
     * @return lista de ventas paginada
     */
    public List<Venta> listarPaginado(int limit, int offset) {
        String sql = "SELECT * FROM sales ORDER BY created_at DESC LIMIT ? OFFSET ?";
        List<Venta> ventas = new ArrayList<>();

        try {
            PreparedStatement pstmt = config.getConnection().prepareStatement(sql);
            pstmt.setInt(1, limit);
            pstmt.setInt(2, offset);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                ventas.add(mapResultSetToVenta(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error listando ventas paginadas", e);
        }
        return ventas;
    }

    /**
     * Lista ventas de un usuario específico.
     *
     * @param userId ID del usuario/vendedor
     * @return lista de ventas del usuario
     */
    public List<Venta> listarPorUsuario(int userId) {
        String sql = "SELECT * FROM sales WHERE user_id = ? ORDER BY created_at DESC";
        List<Venta> ventas = new ArrayList<>();

        try {
            PreparedStatement pstmt = config.getConnection().prepareStatement(sql);
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                ventas.add(mapResultSetToVenta(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error listando ventas por usuario", e);
        }
        return ventas;
    }

    /**
     * Lista ventas en un rango de fechas.
     *
     * @param desde fecha inicial (inclusive)
     * @param hasta fecha final (inclusive)
     * @return lista de ventas en el rango
     */
    public List<Venta> listarPorRangoFechas(LocalDate desde, LocalDate hasta) {
        String sql = """
            SELECT * FROM sales
            WHERE DATE(created_at) BETWEEN ? AND ?
            ORDER BY created_at DESC
        """;
        List<Venta> ventas = new ArrayList<>();

        try {
            PreparedStatement pstmt = config.getConnection().prepareStatement(sql);
            pstmt.setString(1, desde.toString());
            pstmt.setString(2, hasta.toString());
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                ventas.add(mapResultSetToVenta(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error listando ventas por rango de fechas", e);
        }
        return ventas;
    }

    /**
     * Cuenta el total de ventas.
     *
     * @return cantidad total de ventas
     */
    public int contar() {
        String sql = "SELECT COUNT(*) FROM sales";
        try {
            Statement stmt = config.getConnection().createStatement();
            ResultSet rs = stmt.executeQuery(sql);

            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error contando ventas", e);
        }
        return 0;
    }

    /**
     * Cuenta ventas completadas.
     *
     * @return cantidad de ventas completadas
     */
    public int contarCompletadas() {
        String sql = "SELECT COUNT(*) FROM sales WHERE status = 'completed'";
        try {
            Statement stmt = config.getConnection().createStatement();
            ResultSet rs = stmt.executeQuery(sql);

            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error contando ventas completadas", e);
        }
        return 0;
    }

    /**
     * Obtiene el total de ventas del mes.
     *
     * @param year año
     * @param month mes (1-12)
     * @return suma de totales de ventas completadas del mes
     */
    public BigDecimal totalDelMes(int year, int month) {
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
            throw new RuntimeException("Error calculando total del mes", e);
        }
        return BigDecimal.ZERO;
    }

    /**
     * Obtiene el total general de ventas completadas.
     *
     * @return suma de todos los totales
     */
    public BigDecimal totalGeneral() {
        String sql = "SELECT COALESCE(SUM(total), 0) as total FROM sales WHERE status = 'completed'";
        try {
            Statement stmt = config.getConnection().createStatement();
            ResultSet rs = stmt.executeQuery(sql);

            if (rs.next()) {
                return rs.getBigDecimal("total");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error calculando total general", e);
        }
        return BigDecimal.ZERO;
    }

    /**
     * Obtiene estadísticas de ventas por usuario.
     *
     * @param userId ID del usuario
     * @return estadísticas del vendedor
     */
    public EstadisticasVendedor estadisticasPorUsuario(int userId) {
        String sql = """
            SELECT COUNT(*) as cantidad,
                   COALESCE(SUM(total), 0) as total
            FROM sales
            WHERE user_id = ? AND status = 'completed'
        """;
        try {
            PreparedStatement pstmt = config.getConnection().prepareStatement(sql);
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return new EstadisticasVendedor(
                    userId,
                    rs.getInt("cantidad"),
                    rs.getBigDecimal("total")
                );
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error obteniendo estadísticas de vendedor", e);
        }
        return new EstadisticasVendedor(userId, 0, BigDecimal.ZERO);
    }

    /**
     * Record para estadísticas de vendedor.
     */
    public record EstadisticasVendedor(
        int userId,
        int cantidadVentas,
        BigDecimal totalVentas
    ) {
        public BigDecimal promedioVenta() {
            if (cantidadVentas == 0) return BigDecimal.ZERO;
            return totalVentas.divide(BigDecimal.valueOf(cantidadVentas), 2, java.math.RoundingMode.HALF_UP);
        }
    }

    // Métodos privados auxiliares

    private int insertarVenta(Connection conn, Venta venta) throws SQLException {
        String sql = """
            INSERT INTO sales (user_id, total, status, notes)
            VALUES (?, ?, ?, ?)
        """;
        PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        pstmt.setInt(1, venta.getUserId());
        pstmt.setBigDecimal(2, venta.getTotal());
        pstmt.setString(3, venta.getStatus());
        pstmt.setString(4, venta.getNotes());
        pstmt.executeUpdate();

        ResultSet keys = pstmt.getGeneratedKeys();
        if (keys.next()) {
            return keys.getInt(1);
        }
        throw new SQLException("No se pudo obtener ID de venta creada");
    }

    private void actualizarStock(Connection conn, int variantId, int cantidad) throws SQLException {
        String sql = "UPDATE product_variants SET stock = stock + ? WHERE id = ?";
        PreparedStatement pstmt = conn.prepareStatement(sql);
        pstmt.setInt(1, cantidad);
        pstmt.setInt(2, variantId);
        pstmt.executeUpdate();
    }

    private Venta mapResultSetToVenta(ResultSet rs) throws SQLException {
        return new Venta.Builder()
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
            System.err.println("Error en rollback: " + e.getMessage());
        }
    }

    private void setAutoCommitTrue(Connection conn) {
        try {
            if (conn != null) conn.setAutoCommit(true);
        } catch (SQLException e) {
            System.err.println("Error restaurando autocommit: " + e.getMessage());
        }
    }
}
