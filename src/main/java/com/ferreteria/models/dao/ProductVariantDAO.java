package com.ferreteria.models.dao;

import com.ferreteria.models.ProductVariant;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object para variantes de producto.
 * Incluye información del producto padre para mostrar.
 */
public class ProductVariantDAO {

    private final DatabaseConfig config;

    public ProductVariantDAO(DatabaseConfig config) {
        this.config = config;
    }

    /**
     * Busca una variante por su ID.
     *
     * @param id ID de la variante
     * @return Optional con la variante si existe
     */
    public Optional<ProductVariant> buscarPorId(int id) {
        String sql = """
            SELECT pv.*, p.name as product_name, p.code as product_code, c.name as category_name
            FROM product_variants pv
            JOIN products p ON pv.product_id = p.id
            LEFT JOIN categories c ON p.category_id = c.id
            WHERE pv.id = ?
        """;
        try {
            PreparedStatement pstmt = config.getConnection().prepareStatement(sql);
            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return Optional.of(mapResultSetToVariant(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error buscando variante por ID", e);
        }
        return Optional.empty();
    }

    /**
     * Busca una variante por SKU.
     *
     * @param sku el SKU a buscar
     * @return Optional con la variante si existe
     */
    public Optional<ProductVariant> buscarPorSku(String sku) {
        String sql = """
            SELECT pv.*, p.name as product_name, p.code as product_code, c.name as category_name
            FROM product_variants pv
            JOIN products p ON pv.product_id = p.id
            LEFT JOIN categories c ON p.category_id = c.id
            WHERE pv.sku = ? AND pv.active = 1
        """;
        try {
            PreparedStatement pstmt = config.getConnection().prepareStatement(sql);
            pstmt.setString(1, sku);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return Optional.of(mapResultSetToVariant(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error buscando variante por SKU", e);
        }
        return Optional.empty();
    }

    /**
     * Busca variantes por nombre de producto o variante.
     * Útil para el buscador del POS.
     *
     * @param query texto a buscar
     * @param limit máximo de resultados
     * @return lista de variantes que coinciden
     */
    public List<ProductVariant> buscar(String query, int limit) {
        String sql = """
            SELECT pv.*, p.name as product_name, p.code as product_code, c.name as category_name
            FROM product_variants pv
            JOIN products p ON pv.product_id = p.id
            LEFT JOIN categories c ON p.category_id = c.id
            WHERE pv.active = 1 AND p.active = 1
            AND (
                p.name LIKE ? OR
                p.code LIKE ? OR
                pv.sku LIKE ? OR
                pv.variant_name LIKE ?
            )
            ORDER BY p.name, pv.variant_name
            LIMIT ?
        """;
        List<ProductVariant> results = new ArrayList<>();
        String searchPattern = "%" + query + "%";

        try {
            PreparedStatement pstmt = config.getConnection().prepareStatement(sql);
            pstmt.setString(1, searchPattern);
            pstmt.setString(2, searchPattern);
            pstmt.setString(3, searchPattern);
            pstmt.setString(4, searchPattern);
            pstmt.setInt(5, limit);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                results.add(mapResultSetToVariant(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error buscando variantes", e);
        }
        return results;
    }

    /**
     * Lista todas las variantes activas con stock.
     *
     * @return lista de variantes disponibles para venta
     */
    public List<ProductVariant> listarDisponibles() {
        String sql = """
            SELECT pv.*, p.name as product_name, p.code as product_code, c.name as category_name
            FROM product_variants pv
            JOIN products p ON pv.product_id = p.id
            LEFT JOIN categories c ON p.category_id = c.id
            WHERE pv.active = 1 AND p.active = 1 AND pv.stock > 0
            ORDER BY p.name, pv.variant_name
        """;
        return executeListQuery(sql);
    }

    /**
     * Lista variantes de un producto específico.
     *
     * @param productId ID del producto
     * @return lista de variantes del producto
     */
    public List<ProductVariant> listarPorProducto(int productId) {
        String sql = """
            SELECT pv.*, p.name as product_name, p.code as product_code, c.name as category_name
            FROM product_variants pv
            JOIN products p ON pv.product_id = p.id
            LEFT JOIN categories c ON p.category_id = c.id
            WHERE pv.product_id = ? AND pv.active = 1
            ORDER BY pv.variant_name
        """;
        List<ProductVariant> results = new ArrayList<>();

        try {
            PreparedStatement pstmt = config.getConnection().prepareStatement(sql);
            pstmt.setInt(1, productId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                results.add(mapResultSetToVariant(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error listando variantes por producto", e);
        }
        return results;
    }

    /**
     * Lista variantes con stock bajo.
     *
     * @return lista de variantes con stock <= minStock
     */
    public List<ProductVariant> listarStockBajo() {
        String sql = """
            SELECT pv.*, p.name as product_name, p.code as product_code, c.name as category_name
            FROM product_variants pv
            JOIN products p ON pv.product_id = p.id
            LEFT JOIN categories c ON p.category_id = c.id
            WHERE pv.active = 1 AND p.active = 1 AND pv.stock <= pv.min_stock
            ORDER BY pv.stock ASC, p.name
        """;
        return executeListQuery(sql);
    }

    /**
     * Lista todas las variantes activas.
     *
     * @return lista de todas las variantes
     */
    public List<ProductVariant> listarTodas() {
        String sql = """
            SELECT pv.*, p.name as product_name, p.code as product_code, c.name as category_name
            FROM product_variants pv
            JOIN products p ON pv.product_id = p.id
            LEFT JOIN categories c ON p.category_id = c.id
            WHERE pv.active = 1 AND p.active = 1
            ORDER BY p.name, pv.variant_name
        """;
        return executeListQuery(sql);
    }

    /**
     * Actualiza el stock de una variante.
     *
     * @param variantId ID de la variante
     * @param cantidad cantidad a sumar (negativo para restar)
     */
    public void actualizarStock(int variantId, int cantidad) {
        String sql = "UPDATE product_variants SET stock = stock + ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        try {
            PreparedStatement pstmt = config.getConnection().prepareStatement(sql);
            pstmt.setInt(1, cantidad);
            pstmt.setInt(2, variantId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error actualizando stock", e);
        }
    }

    /**
     * Cuenta variantes activas.
     *
     * @return total de variantes activas
     */
    public int contar() {
        String sql = "SELECT COUNT(*) FROM product_variants WHERE active = 1";
        try {
            Statement stmt = config.getConnection().createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error contando variantes", e);
        }
        return 0;
    }

    /**
     * Cuenta variantes con stock bajo.
     *
     * @return total con stock <= minStock
     */
    public int contarStockBajo() {
        String sql = "SELECT COUNT(*) FROM product_variants WHERE active = 1 AND stock <= min_stock";
        try {
            Statement stmt = config.getConnection().createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error contando stock bajo", e);
        }
        return 0;
    }

    private List<ProductVariant> executeListQuery(String sql) {
        List<ProductVariant> results = new ArrayList<>();
        try {
            Statement stmt = config.getConnection().createStatement();
            ResultSet rs = stmt.executeQuery(sql);

            while (rs.next()) {
                results.add(mapResultSetToVariant(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error ejecutando consulta de variantes", e);
        }
        return results;
    }

    private ProductVariant mapResultSetToVariant(ResultSet rs) throws SQLException {
        return new ProductVariant.Builder()
            .id(rs.getInt("id"))
            .productId(rs.getInt("product_id"))
            .sku(rs.getString("sku"))
            .variantName(rs.getString("variant_name"))
            .costPrice(rs.getBigDecimal("cost_price"))
            .salePrice(rs.getBigDecimal("sale_price"))
            .stock(rs.getInt("stock"))
            .minStock(rs.getInt("min_stock"))
            .active(rs.getBoolean("active"))
            .createdAt(parseDateTime(rs.getString("created_at")))
            .productName(rs.getString("product_name"))
            .productCode(rs.getString("product_code"))
            .categoryName(rs.getString("category_name"))
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
