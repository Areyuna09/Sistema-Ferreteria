package com.ferreteria.models.dao;

import com.ferreteria.models.Subcategory;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Data Access Object para operaciones CRUD de subcategorías.
 * Las subcategorías son categorías con parent_id referenciando a otra categoría.
 *
 * @author Sistema Ferretería
 * @version 1.0
 */
public class SubcategoryDAO {

    private static final Logger LOGGER = Logger.getLogger(SubcategoryDAO.class.getName());
    private final DatabaseConfig config;

    public SubcategoryDAO() {
        this.config = DatabaseConfig.getInstance();
    }

    public SubcategoryDAO(DatabaseConfig config) {
        this.config = config;
    }

    // ==================== OPERACIONES DE LECTURA ====================

    /**
     * Busca una subcategoría por su ID.
     *
     * @param id ID de la subcategoría
     * @return Optional con la subcategoría si existe
     */
    public Optional<Subcategory> findById(int id) {
        String sql = """
            SELECT s.*, c.name as category_name
            FROM categories s
            LEFT JOIN categories c ON s.parent_id = c.id
            WHERE s.id = ? AND s.parent_id IS NOT NULL
        """;
        try {
            PreparedStatement pstmt = config.getConnection().prepareStatement(sql);
            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return Optional.of(mapResultSetToSubcategory(rs));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error buscando subcategoría por ID: " + id, e);
            throw new RuntimeException("Error buscando subcategoría", e);
        }
        return Optional.empty();
    }

    /**
     * Lista todas las subcategorías activas.
     *
     * @return Lista de subcategorías
     */
    public List<Subcategory> findAll() {
        String sql = """
            SELECT s.*, c.name as category_name
            FROM categories s
            LEFT JOIN categories c ON s.parent_id = c.id
            WHERE s.parent_id IS NOT NULL AND s.active = 1
            ORDER BY c.name, s.name
        """;
        return executeQuery(sql);
    }

    /**
     * Lista subcategorías por categoría padre.
     *
     * @param categoryId ID de la categoría padre
     * @return Lista de subcategorías de esa categoría
     */
    public List<Subcategory> findByCategoryId(int categoryId) {
        String sql = """
            SELECT s.*, c.name as category_name
            FROM categories s
            LEFT JOIN categories c ON s.parent_id = c.id
            WHERE s.parent_id = ? AND s.active = 1
            ORDER BY s.name
        """;
        List<Subcategory> subcategories = new ArrayList<>();
        try {
            PreparedStatement pstmt = config.getConnection().prepareStatement(sql);
            pstmt.setInt(1, categoryId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                subcategories.add(mapResultSetToSubcategory(rs));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error listando subcategorías por categoría", e);
            throw new RuntimeException("Error listando subcategorías", e);
        }
        return subcategories;
    }

    /**
     * Busca subcategorías por término de búsqueda.
     *
     * @param searchTerm Término a buscar
     * @return Lista de subcategorías que coinciden
     */
    public List<Subcategory> search(String searchTerm) {
        String sql = """
            SELECT s.*, c.name as category_name
            FROM categories s
            LEFT JOIN categories c ON s.parent_id = c.id
            WHERE s.parent_id IS NOT NULL AND s.active = 1
            AND (s.name LIKE ? OR s.description LIKE ? OR c.name LIKE ?)
            ORDER BY c.name, s.name
        """;
        List<Subcategory> subcategories = new ArrayList<>();
        try {
            PreparedStatement pstmt = config.getConnection().prepareStatement(sql);
            String term = "%" + searchTerm + "%";
            pstmt.setString(1, term);
            pstmt.setString(2, term);
            pstmt.setString(3, term);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                subcategories.add(mapResultSetToSubcategory(rs));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error buscando subcategorías", e);
            throw new RuntimeException("Error en búsqueda de subcategorías", e);
        }
        return subcategories;
    }

    /**
     * Busca subcategorías dentro de una categoría específica.
     *
     * @param categoryId ID de la categoría padre
     * @param searchTerm Término a buscar
     * @return Lista de subcategorías que coinciden
     */
    public List<Subcategory> searchInCategory(int categoryId, String searchTerm) {
        String sql = """
            SELECT s.*, c.name as category_name
            FROM categories s
            LEFT JOIN categories c ON s.parent_id = c.id
            WHERE s.parent_id = ? AND s.active = 1
            AND (s.name LIKE ? OR s.description LIKE ?)
            ORDER BY s.name
        """;
        List<Subcategory> subcategories = new ArrayList<>();
        try {
            PreparedStatement pstmt = config.getConnection().prepareStatement(sql);
            pstmt.setInt(1, categoryId);
            String term = "%" + searchTerm + "%";
            pstmt.setString(2, term);
            pstmt.setString(3, term);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                subcategories.add(mapResultSetToSubcategory(rs));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error buscando subcategorías en categoría", e);
            throw new RuntimeException("Error en búsqueda de subcategorías", e);
        }
        return subcategories;
    }

    /**
     * Verifica si existe una subcategoría activa con el nombre dado en una categoría.
     *
     * @param nombre Nombre a verificar
     * @param categoryId ID de la categoría padre
     * @return true si existe
     */
    public boolean existsByNameInCategory(String nombre, int categoryId) {
        String sql = "SELECT COUNT(*) FROM categories WHERE name = ? AND parent_id = ? AND active = 1";
        try {
            PreparedStatement pstmt = config.getConnection().prepareStatement(sql);
            pstmt.setString(1, nombre);
            pstmt.setInt(2, categoryId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error verificando existencia de subcategoría", e);
            throw new RuntimeException("Error verificando subcategoría", e);
        }
        return false;
    }

    /**
     * Busca una subcategoría inactiva por nombre en una categoría para posible reactivación.
     *
     * @param nombre Nombre a buscar
     * @param categoryId ID de la categoría padre
     * @return Optional con la subcategoría inactiva si existe
     */
    public Optional<Subcategory> findInactiveByNameInCategory(String nombre, int categoryId) {
        String sql = """
            SELECT s.*, c.name as category_name
            FROM categories s
            LEFT JOIN categories c ON s.parent_id = c.id
            WHERE s.name = ? AND s.parent_id = ? AND s.active = 0
        """;
        try {
            PreparedStatement pstmt = config.getConnection().prepareStatement(sql);
            pstmt.setString(1, nombre);
            pstmt.setInt(2, categoryId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return Optional.of(mapResultSetToSubcategory(rs));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error buscando subcategoría inactiva", e);
        }
        return Optional.empty();
    }

    /**
     * Verifica si existe otra subcategoría activa con el mismo nombre (excluyendo la actual).
     *
     * @param nombre Nombre a verificar
     * @param categoryId ID de la categoría padre
     * @param excludeId ID a excluir de la búsqueda
     * @return true si existe otra subcategoría con ese nombre
     */
    public boolean existsByNameInCategoryExcludingId(String nombre, int categoryId, int excludeId) {
        String sql = "SELECT COUNT(*) FROM categories WHERE name = ? AND parent_id = ? AND id != ? AND active = 1";
        try {
            PreparedStatement pstmt = config.getConnection().prepareStatement(sql);
            pstmt.setString(1, nombre);
            pstmt.setInt(2, categoryId);
            pstmt.setInt(3, excludeId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error verificando existencia de subcategoría", e);
            throw new RuntimeException("Error verificando subcategoría", e);
        }
        return false;
    }

    // ==================== OPERACIONES DE CONTEO ====================

    /**
     * Cuenta los productos asociados a una subcategoría.
     *
     * @param subcategoryId ID de la subcategoría
     * @return Número de productos
     */
    public int countProducts(int subcategoryId) {
        String sql = "SELECT COUNT(*) FROM products WHERE category_id = ? AND active = 1";
        try {
            PreparedStatement pstmt = config.getConnection().prepareStatement(sql);
            pstmt.setInt(1, subcategoryId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error contando productos de subcategoría", e);
            throw new RuntimeException("Error contando productos", e);
        }
        return 0;
    }

    /**
     * Cuenta el total de subcategorías activas.
     *
     * @return Número total de subcategorías
     */
    public int count() {
        String sql = "SELECT COUNT(*) FROM categories WHERE parent_id IS NOT NULL AND active = 1";
        try {
            Statement stmt = config.getConnection().createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error contando subcategorías", e);
            throw new RuntimeException("Error contando subcategorías", e);
        }
        return 0;
    }

    /**
     * Cuenta subcategorías de una categoría específica.
     *
     * @param categoryId ID de la categoría padre
     * @return Número de subcategorías
     */
    public int countByCategory(int categoryId) {
        String sql = "SELECT COUNT(*) FROM categories WHERE parent_id = ? AND active = 1";
        try {
            PreparedStatement pstmt = config.getConnection().prepareStatement(sql);
            pstmt.setInt(1, categoryId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error contando subcategorías de categoría", e);
            throw new RuntimeException("Error contando subcategorías", e);
        }
        return 0;
    }

    // ==================== OPERACIONES DE ESCRITURA ====================

    /**
     * Guarda una subcategoría (inserta o actualiza según el ID).
     *
     * @param subcategory Subcategoría a guardar
     * @return Subcategoría guardada con ID actualizado
     */
    public Subcategory save(Subcategory subcategory) {
        // Validar que la categoría padre exista
        if (!categoryExists(subcategory.getCategoryId())) {
            throw new IllegalArgumentException("La categoría padre no existe");
        }

        if (subcategory.getId() > 0) {
            return update(subcategory);
        }
        return insert(subcategory);
    }

    /**
     * Verifica si una categoría padre existe.
     */
    private boolean categoryExists(int categoryId) {
        String sql = "SELECT COUNT(*) FROM categories WHERE id = ? AND parent_id IS NULL AND active = 1";
        try {
            PreparedStatement pstmt = config.getConnection().prepareStatement(sql);
            pstmt.setInt(1, categoryId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error verificando categoría padre", e);
        }
        return false;
    }

    /**
     * Inserta una nueva subcategoría.
     */
    private Subcategory insert(Subcategory subcategory) {
        String sql = "INSERT INTO categories (name, description, parent_id, active) VALUES (?, ?, ?, ?)";
        try {
            PreparedStatement pstmt = config.getConnection().prepareStatement(sql,
                Statement.RETURN_GENERATED_KEYS);
            pstmt.setString(1, subcategory.getNombre());
            pstmt.setString(2, subcategory.getDescripcion());
            pstmt.setInt(3, subcategory.getCategoryId());
            pstmt.setBoolean(4, subcategory.isActive());
            pstmt.executeUpdate();

            ResultSet keys = pstmt.getGeneratedKeys();
            if (keys.next()) {
                int newId = keys.getInt(1);
                LOGGER.info("Subcategoría creada con ID: " + newId);
                return findById(newId).orElse(subcategory);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error insertando subcategoría", e);
            throw new RuntimeException("Error al crear subcategoría", e);
        }
        return subcategory;
    }

    /**
     * Actualiza una subcategoría existente.
     */
    private Subcategory update(Subcategory subcategory) {
        String sql = "UPDATE categories SET name = ?, description = ?, parent_id = ?, active = ? WHERE id = ?";
        try {
            PreparedStatement pstmt = config.getConnection().prepareStatement(sql);
            pstmt.setString(1, subcategory.getNombre());
            pstmt.setString(2, subcategory.getDescripcion());
            pstmt.setInt(3, subcategory.getCategoryId());
            pstmt.setBoolean(4, subcategory.isActive());
            pstmt.setInt(5, subcategory.getId());
            pstmt.executeUpdate();
            LOGGER.info("Subcategoría actualizada: " + subcategory.getId());
            return findById(subcategory.getId()).orElse(subcategory);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error actualizando subcategoría", e);
            throw new RuntimeException("Error al actualizar subcategoría", e);
        }
    }

    /**
     * Elimina una subcategoría (soft delete).
     * Valida que no tenga productos asociados.
     *
     * @param id ID de la subcategoría a eliminar
     * @throws IllegalStateException si tiene productos asociados
     */
    public void delete(int id) {
        // Validar que no tenga productos
        int productCount = countProducts(id);
        if (productCount > 0) {
            throw new IllegalStateException(
                "No se puede eliminar: la subcategoría tiene " + productCount + " producto(s) asociado(s)");
        }

        // Soft delete
        String sql = "UPDATE categories SET active = 0 WHERE id = ?";
        try {
            PreparedStatement pstmt = config.getConnection().prepareStatement(sql);
            pstmt.setInt(1, id);
            int affected = pstmt.executeUpdate();
            if (affected > 0) {
                LOGGER.info("Subcategoría eliminada (soft delete): " + id);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error eliminando subcategoría", e);
            throw new RuntimeException("Error al eliminar subcategoría", e);
        }
    }

    /**
     * Reactiva una subcategoría previamente eliminada.
     *
     * @param id ID de la subcategoría
     */
    public void reactivate(int id) {
        String sql = "UPDATE categories SET active = 1 WHERE id = ?";
        try {
            PreparedStatement pstmt = config.getConnection().prepareStatement(sql);
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
            LOGGER.info("Subcategoría reactivada: " + id);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error reactivando subcategoría", e);
            throw new RuntimeException("Error al reactivar subcategoría", e);
        }
    }

    // ==================== MÉTODOS AUXILIARES ====================

    /**
     * Ejecuta una consulta SQL y retorna lista de subcategorías.
     */
    private List<Subcategory> executeQuery(String sql) {
        List<Subcategory> subcategories = new ArrayList<>();
        try {
            Statement stmt = config.getConnection().createStatement();
            ResultSet rs = stmt.executeQuery(sql);

            while (rs.next()) {
                subcategories.add(mapResultSetToSubcategory(rs));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error ejecutando consulta de subcategorías", e);
            throw new RuntimeException("Error listando subcategorías", e);
        }
        return subcategories;
    }

    /**
     * Mapea un ResultSet a un objeto Subcategory.
     */
    private Subcategory mapResultSetToSubcategory(ResultSet rs) throws SQLException {
        LocalDateTime createdAt = null;
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) {
            createdAt = ts.toLocalDateTime();
        }

        return new Subcategory.Builder()
            .id(rs.getInt("id"))
            .categoryId(rs.getInt("parent_id"))
            .categoryName(rs.getString("category_name"))
            .nombre(rs.getString("name"))
            .descripcion(rs.getString("description"))
            .active(rs.getBoolean("active"))
            .createdAt(createdAt != null ? createdAt : LocalDateTime.now())
            .build();
    }
}
