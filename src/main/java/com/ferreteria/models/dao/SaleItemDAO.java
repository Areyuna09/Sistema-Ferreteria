package com.ferreteria.models.dao;

import com.ferreteria.models.SaleItem;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for sale items/details.
 * Works together with SaleDAO for transactions.
 */
public class SaleItemDAO {

    private final DatabaseConfig config;

    public SaleItemDAO(DatabaseConfig config) {
        this.config = config;
    }

    /**
     * Creates a sale item using an existing connection (for transactions).
     *
     * @param conn connection with active transaction
     * @param saleId ID of the sale
     * @param item the item to create
     * @return the created item with its ID
     * @throws SQLException if database error occurs
     */
    public SaleItem create(Connection conn, int saleId, SaleItem item) throws SQLException {
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
            return new SaleItem.Builder()
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
     * Lists all items for a sale with product information.
     *
     * @param saleId ID of the sale
     * @return list of sale items
     */
    public List<SaleItem> findBySaleId(int saleId) {
        String sql = """
            SELECT si.*, p.name as product_name, pv.variant_name
            FROM sale_items si
            JOIN product_variants pv ON si.variant_id = pv.id
            JOIN products p ON pv.product_id = p.id
            WHERE si.sale_id = ?
            ORDER BY si.id
        """;
        List<SaleItem> items = new ArrayList<>();

        try {
            PreparedStatement pstmt = config.getConnection().prepareStatement(sql);
            pstmt.setInt(1, saleId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                items.add(mapResultSet(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error listing sale items", e);
        }
        return items;
    }

    /**
     * Finds an item by its ID.
     *
     * @param id item ID
     * @return the item if exists
     */
    public SaleItem findById(int id) {
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
                return mapResultSet(rs);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding sale item", e);
        }
        return null;
    }

    /**
     * Calculates total sold for a specific variant.
     *
     * @param variantId variant ID
     * @return total quantity sold
     */
    public int totalSoldByVariant(int variantId) {
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
            throw new RuntimeException("Error calculating total sold", e);
        }
        return 0;
    }

    /**
     * Gets the best-selling products.
     *
     * @param limit maximum number of results
     * @return list of sold products grouped by product
     */
    public List<SoldProduct> getBestSellers(int limit) {
        String sql = """
            SELECT p.id, p.name, pv.variant_name,
                   SUM(si.quantity) as total_quantity,
                   SUM(si.subtotal) as total_amount
            FROM sale_items si
            JOIN product_variants pv ON si.variant_id = pv.id
            JOIN products p ON pv.product_id = p.id
            JOIN sales s ON si.sale_id = s.id
            WHERE s.status = 'completed'
            GROUP BY pv.id
            ORDER BY total_quantity DESC
            LIMIT ?
        """;
        List<SoldProduct> products = new ArrayList<>();

        try {
            PreparedStatement pstmt = config.getConnection().prepareStatement(sql);
            pstmt.setInt(1, limit);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                products.add(new SoldProduct(
                    rs.getInt("id"),
                    rs.getString("name"),
                    rs.getString("variant_name"),
                    rs.getInt("total_quantity"),
                    rs.getBigDecimal("total_amount")
                ));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error getting best sellers", e);
        }
        return products;
    }

    private SaleItem mapResultSet(ResultSet rs) throws SQLException {
        return new SaleItem.Builder()
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
     * Record representing a sold product with statistics.
     */
    public record SoldProduct(
        int productId,
        String productName,
        String variantName,
        int totalQuantity,
        BigDecimal totalAmount
    ) {
        public String getDisplayName() {
            if (variantName != null && !variantName.isBlank()) {
                return productName + " - " + variantName;
            }
            return productName;
        }
    }
}
