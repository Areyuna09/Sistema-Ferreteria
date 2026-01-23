package com.ferreteria.models.dao;

import com.ferreteria.models.DetalleVenta;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object para items/detalles de venta.
 * Trabaja en conjunto con VentaDAO para transacciones.
 */
public class DetalleVentaDAO {

    private final DatabaseConfig config;

    public DetalleVentaDAO(DatabaseConfig config) {
        this.config = config;
    }

    /**
     * Crea un item de venta usando una conexión existente (para transacciones).
     *
     * @param conn conexión con transacción activa
     * @param saleId ID de la venta
     * @param item el detalle a crear
     * @return el item creado con su ID
     * @throws SQLException si hay error de base de datos
     */
    public DetalleVenta crear(Connection conn, int saleId, DetalleVenta item) throws SQLException {
        String sql = """
            INSERT INTO sale_items (sale_id, variant_id, quantity, unit_price, subtotal)
            VALUES (?, ?, ?, ?, ?)
        """;
        PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        pstmt.setInt(1, saleId);
        pstmt.setInt(2, item.getVariantId());
        pstmt.setInt(3, item.getQuantity());
        pstmt.setBigDecimal(4, item.getUnitPrice());
        pstmt.setBigDecimal(5, item.getSubtotal());
        pstmt.executeUpdate();

        ResultSet keys = pstmt.getGeneratedKeys();
        if (keys.next()) {
            return new DetalleVenta.Builder()
                .id(keys.getInt(1))
                .saleId(saleId)
                .variantId(item.getVariantId())
                .quantity(item.getQuantity())
                .unitPrice(item.getUnitPrice())
                .subtotal(item.getSubtotal())
                .productName(item.getProductName())
                .variantName(item.getVariantName())
                .build();
        }
        return item;
    }

    /**
     * Lista todos los items de una venta con información del producto.
     *
     * @param saleId ID de la venta
     * @return lista de detalles de venta
     */
    public List<DetalleVenta> listarPorVenta(int saleId) {
        String sql = """
            SELECT si.*, p.name as product_name, pv.variant_name
            FROM sale_items si
            JOIN product_variants pv ON si.variant_id = pv.id
            JOIN products p ON pv.product_id = p.id
            WHERE si.sale_id = ?
            ORDER BY si.id
        """;
        List<DetalleVenta> items = new ArrayList<>();

        try {
            PreparedStatement pstmt = config.getConnection().prepareStatement(sql);
            pstmt.setInt(1, saleId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                items.add(mapResultSetToDetalle(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error listando items de venta", e);
        }
        return items;
    }

    /**
     * Busca un item por su ID.
     *
     * @param id ID del item
     * @return el detalle si existe
     */
    public DetalleVenta buscarPorId(int id) {
        String sql = """
            SELECT si.*, p.name as product_name, pv.variant_name
            FROM sale_items si
            JOIN product_variants pv ON si.variant_id = pv.id
            JOIN products p ON pv.product_id = p.id
            WHERE si.id = ?
        """;
        try {
            PreparedStatement pstmt = config.getConnection().prepareStatement(sql);
            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return mapResultSetToDetalle(rs);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error buscando item de venta", e);
        }
        return null;
    }

    /**
     * Calcula el total vendido de una variante específica.
     *
     * @param variantId ID de la variante
     * @return cantidad total vendida
     */
    public int totalVendidoPorVariante(int variantId) {
        String sql = """
            SELECT COALESCE(SUM(si.quantity), 0) as total
            FROM sale_items si
            JOIN sales s ON si.sale_id = s.id
            WHERE si.variant_id = ? AND s.status = 'completed'
        """;
        try {
            PreparedStatement pstmt = config.getConnection().prepareStatement(sql);
            pstmt.setInt(1, variantId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return rs.getInt("total");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error calculando total vendido", e);
        }
        return 0;
    }

    /**
     * Obtiene los productos más vendidos.
     *
     * @param limit cantidad máxima de resultados
     * @return lista de detalles agrupados por producto
     */
    public List<ProductoVendido> productosMasVendidos(int limit) {
        String sql = """
            SELECT p.id, p.name, pv.variant_name,
                   SUM(si.quantity) as total_cantidad,
                   SUM(si.subtotal) as total_monto
            FROM sale_items si
            JOIN product_variants pv ON si.variant_id = pv.id
            JOIN products p ON pv.product_id = p.id
            JOIN sales s ON si.sale_id = s.id
            WHERE s.status = 'completed'
            GROUP BY pv.id
            ORDER BY total_cantidad DESC
            LIMIT ?
        """;
        List<ProductoVendido> productos = new ArrayList<>();

        try {
            PreparedStatement pstmt = config.getConnection().prepareStatement(sql);
            pstmt.setInt(1, limit);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                productos.add(new ProductoVendido(
                    rs.getInt("id"),
                    rs.getString("name"),
                    rs.getString("variant_name"),
                    rs.getInt("total_cantidad"),
                    rs.getBigDecimal("total_monto")
                ));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error obteniendo productos más vendidos", e);
        }
        return productos;
    }

    private DetalleVenta mapResultSetToDetalle(ResultSet rs) throws SQLException {
        return new DetalleVenta.Builder()
            .id(rs.getInt("id"))
            .saleId(rs.getInt("sale_id"))
            .variantId(rs.getInt("variant_id"))
            .quantity(rs.getInt("quantity"))
            .unitPrice(rs.getBigDecimal("unit_price"))
            .subtotal(rs.getBigDecimal("subtotal"))
            .productName(rs.getString("product_name"))
            .variantName(rs.getString("variant_name"))
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

    /**
     * Record para representar un producto vendido con estadísticas.
     */
    public record ProductoVendido(
        int productId,
        String productName,
        String variantName,
        int totalCantidad,
        BigDecimal totalMonto
    ) {
        public String getDisplayName() {
            if (variantName != null && !variantName.isBlank()) {
                return productName + " - " + variantName;
            }
            return productName;
        }
    }
}
