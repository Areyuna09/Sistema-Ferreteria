package com.ferreteria.models.dao;

import com.ferreteria.models.Category;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Data Access Object para operaciones CRUD de categorías.
 * Gestiona la persistencia de categorías en la base de datos.
 *
 * @author Sistema Ferretería
 * @version 1.0
 */
public class CategoryDAO {

    private static final Logger LOGGER = Logger.getLogger(CategoryDAO.class.getName());
    private final DatabaseConfig config;

    public CategoryDAO() {
        this.config = DatabaseConfig.getInstance();
    }

    public CategoryDAO(DatabaseConfig config) {
        this.config = config;
    }

    // ==================== OPERACIONES DE LECTURA ====================

    /**
     * Busca una categoría por su ID.
     *
     * @param id ID de la categoría
     * @return Optional con la categoría si existe
     */
    public Optional<Category> findById(int id) {
        String sql = "SELECT * FROM categories WHERE id = ? AND parent_id IS NULL";
        try {
            PreparedStatement pstmt = config.getConnection().prepareStatement(sql);
            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return Optional.of(mapResultSetToCategory(rs));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error buscando categoría por ID: " + id, e);
            throw new RuntimeException("Error buscando categoría", e);
        }
        return Optional.empty();
    }

    /**
     * Lista todas las categorías activas (no subcategorías).
     *
     * @return Lista de categorías
     */
    public List<Category> findAll() {
        String sql = "SELECT * FROM categories WHERE parent_id IS NULL AND active = 1 ORDER BY name";
        return executeQuery(sql);
    }

    /**
     * Lista todas las categorías incluyendo inactivas.
     *
     * @return Lista de todas las categorías
     */
    public List<Category> findAllIncludingInactive() {
        String sql = "SELECT * FROM categories WHERE parent_id IS NULL ORDER BY name";
        return executeQuery(sql);
    }

    /**
     * Busca categorías por nombre (búsqueda parcial).
     *
     * @param searchTerm Término de búsqueda
     * @return Lista de categorías que coinciden
     */
    public List<Category> search(String searchTerm) {
        String sql = """
            SELECT * FROM categories
            WHERE parent_id IS NULL AND active = 1
            AND (name LIKE ? OR description LIKE ?)
            ORDER BY name
        """;
        List<Category> categories = new ArrayList<>();
        try {
            PreparedStatement pstmt = config.getConnection().prepareStatement(sql);
            String term = "%" + searchTerm + "%";
            pstmt.setString(1, term);
            pstmt.setString(2, term);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                categories.add(mapResultSetToCategory(rs));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error buscando categorías", e);
            throw new RuntimeException("Error en búsqueda de categorías", e);
        }
        return categories;
    }

    /**
     * Verifica si existe una categoría activa con el nombre dado.
     *
     * @param nombre Nombre a verificar
     * @return true si existe
     */
    public boolean existsByName(String nombre) {
        String sql = "SELECT COUNT(*) FROM categories WHERE name = ? AND parent_id IS NULL AND active = 1";
        try {
            PreparedStatement pstmt = config.getConnection().prepareStatement(sql);
            pstmt.setString(1, nombre);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error verificando existencia de categoría", e);
            throw new RuntimeException("Error verificando categoría", e);
        }
        return false;
    }

    /**
     * Busca una categoría inactiva por nombre para posible reactivación.
     *
     * @param nombre Nombre a buscar
     * @return Optional con la categoría inactiva si existe
     */
    public Optional<Category> findInactiveByName(String nombre) {
        String sql = "SELECT * FROM categories WHERE name = ? AND parent_id IS NULL AND active = 0";
        try {
            PreparedStatement pstmt = config.getConnection().prepareStatement(sql);
            pstmt.setString(1, nombre);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return Optional.of(mapResultSetToCategory(rs));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error buscando categoría inactiva", e);
        }
        return Optional.empty();
    }

    /**
     * Verifica si existe otra categoría activa con el mismo nombre (excluyendo la actual).
     *
     * @param nombre Nombre a verificar
     * @param excludeId ID a excluir de la búsqueda
     * @return true si existe otra categoría con ese nombre
     */
    public boolean existsByNameExcludingId(String nombre, int excludeId) {
        String sql = "SELECT COUNT(*) FROM categories WHERE name = ? AND parent_id IS NULL AND id != ? AND active = 1";
        try {
            PreparedStatement pstmt = config.getConnection().prepareStatement(sql);
            pstmt.setString(1, nombre);
            pstmt.setInt(2, excludeId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error verificando existencia de categoría", e);
            throw new RuntimeException("Error verificando categoría", e);
        }
        return false;
    }

    // ==================== OPERACIONES DE CONTEO ====================

    /**
     * Cuenta las subcategorías asociadas a una categoría.
     *
     * @param categoryId ID de la categoría
     * @return Número de subcategorías
     */
    public int countSubcategories(int categoryId) {
        String sql = "SELECT COUNT(*) FROM categories WHERE parent_id = ? AND active = 1";
        try {
            PreparedStatement pstmt = config.getConnection().prepareStatement(sql);
            pstmt.setInt(1, categoryId);
            ResultSet rs = pstmt.executeQuery();
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
     * Cuenta los productos asociados a una categoría.
     *
     * @param categoryId ID de la categoría
     * @return Número de productos
     */
    public int countProducts(int categoryId) {
        String sql = "SELECT COUNT(*) FROM products WHERE category_id = ? AND active = 1";
        try {
            PreparedStatement pstmt = config.getConnection().prepareStatement(sql);
            pstmt.setInt(1, categoryId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error contando productos de categoría", e);
            throw new RuntimeException("Error contando productos", e);
        }
        return 0;
    }

    /**
     * Cuenta el total de categorías activas.
     *
     * @return Número total de categorías
     */
    public int count() {
        String sql = "SELECT COUNT(*) FROM categories WHERE parent_id IS NULL AND active = 1";
        try {
            Statement stmt = config.getConnection().createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error contando categorías", e);
            throw new RuntimeException("Error contando categorías", e);
        }
        return 0;
    }

    // ==================== OPERACIONES DE ESCRITURA ====================

    /**
     * Guarda una categoría (inserta o actualiza según el ID).
     *
     * @param category Categoría a guardar
     * @return Categoría guardada con ID actualizado
     */
    public Category save(Category category) {
        if (category.getId() > 0) {
            return update(category);
        }
        return insert(category);
    }

    /**
     * Inserta una nueva categoría.
     */
    private Category insert(Category category) {
        String sql = "INSERT INTO categories (name, description, parent_id, active) VALUES (?, ?, NULL, ?)";
        try {
            PreparedStatement pstmt = config.getConnection().prepareStatement(sql,
                Statement.RETURN_GENERATED_KEYS);
            pstmt.setString(1, category.getNombre());
            pstmt.setString(2, category.getDescripcion());
            pstmt.setBoolean(3, category.isActive());
            pstmt.executeUpdate();

            ResultSet keys = pstmt.getGeneratedKeys();
            if (keys.next()) {
                int newId = keys.getInt(1);
                LOGGER.info("Categoría creada con ID: " + newId);
                return findById(newId).orElse(category);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error insertando categoría", e);
            throw new RuntimeException("Error al crear categoría", e);
        }
        return category;
    }

    /**
     * Actualiza una categoría existente.
     */
    private Category update(Category category) {
        String sql = "UPDATE categories SET name = ?, description = ?, active = ? WHERE id = ?";
        try {
            PreparedStatement pstmt = config.getConnection().prepareStatement(sql);
            pstmt.setString(1, category.getNombre());
            pstmt.setString(2, category.getDescripcion());
            pstmt.setBoolean(3, category.isActive());
            pstmt.setInt(4, category.getId());
            pstmt.executeUpdate();
            LOGGER.info("Categoría actualizada: " + category.getId());
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error actualizando categoría", e);
            throw new RuntimeException("Error al actualizar categoría", e);
        }
        return category;
    }

    /**
     * Elimina una categoría (soft delete).
     * Valida que no tenga subcategorías ni productos asociados.
     *
     * @param id ID de la categoría a eliminar
     * @throws IllegalStateException si tiene dependencias
     */
    public void delete(int id) {
        // Validar que no tenga subcategorías
        int subcatCount = countSubcategories(id);
        if (subcatCount > 0) {
            throw new IllegalStateException(
                "No se puede eliminar: la categoría tiene " + subcatCount + " subcategoría(s) asociada(s)");
        }

        // Validar que no tenga productos
        int productCount = countProducts(id);
        if (productCount > 0) {
            throw new IllegalStateException(
                "No se puede eliminar: la categoría tiene " + productCount + " producto(s) asociado(s)");
        }

        // Soft delete
        String sql = "UPDATE categories SET active = 0 WHERE id = ?";
        try {
            PreparedStatement pstmt = config.getConnection().prepareStatement(sql);
            pstmt.setInt(1, id);
            int affected = pstmt.executeUpdate();
            if (affected > 0) {
                LOGGER.info("Categoría eliminada (soft delete): " + id);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error eliminando categoría", e);
            throw new RuntimeException("Error al eliminar categoría", e);
        }
    }

    /**
     * Elimina permanentemente una categoría (hard delete).
     * Solo usar con precaución.
     *
     * @param id ID de la categoría
     */
    public void hardDelete(int id) {
        String sql = "DELETE FROM categories WHERE id = ?";
        try {
            PreparedStatement pstmt = config.getConnection().prepareStatement(sql);
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
            LOGGER.info("Categoría eliminada permanentemente: " + id);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error en hard delete de categoría", e);
            throw new RuntimeException("Error al eliminar categoría permanentemente", e);
        }
    }

    /**
     * Reactiva una categoría previamente eliminada.
     *
     * @param id ID de la categoría
     */
    public void reactivate(int id) {
        String sql = "UPDATE categories SET active = 1 WHERE id = ?";
        try {
            PreparedStatement pstmt = config.getConnection().prepareStatement(sql);
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
            LOGGER.info("Categoría reactivada: " + id);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error reactivando categoría", e);
            throw new RuntimeException("Error al reactivar categoría", e);
        }
    }

    // ==================== MÉTODOS AUXILIARES ====================

    /**
     * Ejecuta una consulta SQL y retorna lista de categorías.
     */
    private List<Category> executeQuery(String sql) {
        List<Category> categories = new ArrayList<>();
        try {
            Statement stmt = config.getConnection().createStatement();
            ResultSet rs = stmt.executeQuery(sql);

            while (rs.next()) {
                categories.add(mapResultSetToCategory(rs));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error ejecutando consulta de categorías", e);
            throw new RuntimeException("Error listando categorías", e);
        }
        return categories;
    }

    /**
     * Mapea un ResultSet a un objeto Category.
     */
    private Category mapResultSetToCategory(ResultSet rs) throws SQLException {
        LocalDateTime createdAt = null;
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) {
            createdAt = ts.toLocalDateTime();
        }

        return new Category.Builder()
            .id(rs.getInt("id"))
            .nombre(rs.getString("name"))
            .descripcion(rs.getString("description"))
            .active(rs.getBoolean("active"))
            .createdAt(createdAt != null ? createdAt : LocalDateTime.now())
            .build();
    }
}
