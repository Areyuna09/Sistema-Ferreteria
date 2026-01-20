package com.ferreteria.models.dao;

import com.ferreteria.models.User;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object para usuarios.
 */
public class UserDAO {

    private final DatabaseConfig config;

    public UserDAO(DatabaseConfig config) {
        this.config = config;
    }

    public Optional<User> findById(int id) {
        String sql = "SELECT * FROM users WHERE id = ?";
        try {
            PreparedStatement pstmt = config.getConnection().prepareStatement(sql);
            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return Optional.of(mapResultSetToUser(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error buscando usuario por ID", e);
        }
        return Optional.empty();
    }

    public Optional<User> findByUsername(String username) {
        String sql = "SELECT * FROM users WHERE username = ?";
        try {
            PreparedStatement pstmt = config.getConnection().prepareStatement(sql);
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return Optional.of(mapResultSetToUser(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error buscando usuario", e);
        }
        return Optional.empty();
    }

    public List<User> findAll() {
        String sql = "SELECT * FROM users ORDER BY username";
        List<User> users = new ArrayList<>();

        try {
            Statement stmt = config.getConnection().createStatement();
            ResultSet rs = stmt.executeQuery(sql);

            while (rs.next()) {
                users.add(mapResultSetToUser(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error listando usuarios", e);
        }
        return users;
    }

    public List<User> findAllActive() {
        String sql = "SELECT * FROM users WHERE active = 1 ORDER BY username";
        List<User> users = new ArrayList<>();

        try {
            Statement stmt = config.getConnection().createStatement();
            ResultSet rs = stmt.executeQuery(sql);

            while (rs.next()) {
                users.add(mapResultSetToUser(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error listando usuarios activos", e);
        }
        return users;
    }

    public User save(User user) {
        if (user.getId() > 0) {
            return update(user);
        }
        return insert(user);
    }

    private User insert(User user) {
        String sql = "INSERT INTO users (username, password, role, full_name, active) VALUES (?, ?, ?, ?, ?)";
        try {
            PreparedStatement pstmt = config.getConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            pstmt.setString(1, user.getUsername());
            pstmt.setString(2, user.getPasswordHash());
            pstmt.setString(3, user.getRole().getValue());
            pstmt.setString(4, user.getFullName());
            pstmt.setBoolean(5, user.isActive());
            pstmt.executeUpdate();

            ResultSet keys = pstmt.getGeneratedKeys();
            if (keys.next()) {
                return findById(keys.getInt(1)).orElse(user);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error guardando usuario", e);
        }
        return user;
    }

    private User update(User user) {
        String sql = "UPDATE users SET username=?, role=?, full_name=?, active=? WHERE id=?";
        try {
            PreparedStatement pstmt = config.getConnection().prepareStatement(sql);
            pstmt.setString(1, user.getUsername());
            pstmt.setString(2, user.getRole().getValue());
            pstmt.setString(3, user.getFullName());
            pstmt.setBoolean(4, user.isActive());
            pstmt.setInt(5, user.getId());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error actualizando usuario", e);
        }
        return user;
    }

    public void delete(int id) {
        String sql = "UPDATE users SET active = 0 WHERE id = ?";
        try {
            PreparedStatement pstmt = config.getConnection().prepareStatement(sql);
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error eliminando usuario", e);
        }
    }

    public boolean existsByUsername(String username) {
        return findByUsername(username).isPresent();
    }

    public int count() {
        String sql = "SELECT COUNT(*) FROM users WHERE active = 1";
        try {
            Statement stmt = config.getConnection().createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error contando usuarios", e);
        }
        return 0;
    }

    private User mapResultSetToUser(ResultSet rs) throws SQLException {
        return new User.Builder()
            .id(rs.getInt("id"))
            .username(rs.getString("username"))
            .passwordHash(rs.getString("password"))
            .role(rs.getString("role"))
            .fullName(rs.getString("full_name"))
            .active(rs.getBoolean("active"))
            .build();
    }
}
