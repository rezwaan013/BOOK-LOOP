package com.bookloop.dao;

import com.bookloop.model.User;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.Optional;

/** CRUD operations for the users table. Uses PreparedStatements throughout. */
public class UserDAO {

    private final Connection con = DatabaseManager.getInstance().getConnection();

    /**
     * Persists a new user and sets the generated id back on the object.
     * @return generated database id
     */
    public int save(User user) throws SQLException {
        String sql = "INSERT INTO users(full_name,phone,email,password_hash,salt) VALUES(?,?,?,?,?)";
        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, user.getFullName());
            ps.setString(2, user.getPhone());
            ps.setString(3, user.getEmail());
            ps.setString(4, user.getPasswordHash());
            ps.setString(5, user.getSalt());
            ps.executeUpdate();
            try (ResultSet k = ps.getGeneratedKeys()) {
                int id = k.getInt(1);
                user.setId(id);
                return id;
            }
        }
    }

    /** Finds a user by email (used for login). */
    public Optional<User> findByEmail(String email) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("SELECT * FROM users WHERE email=?")) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        }
    }

    /** Finds a user by primary key. */
    public Optional<User> findById(int id) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("SELECT * FROM users WHERE id=?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        }
    }

    private User map(ResultSet rs) throws SQLException {
        User u = new User();
        u.setId(rs.getInt("id"));
        u.setFullName(rs.getString("full_name"));
        u.setPhone(rs.getString("phone"));
        u.setEmail(rs.getString("email"));
        u.setPasswordHash(rs.getString("password_hash"));
        u.setSalt(rs.getString("salt"));
        String ts = rs.getString("created_at");
        if (ts != null) { try { u.setCreatedAt(LocalDateTime.parse(ts.replace(" ","T"))); } catch (Exception ignored) {} }
        return u;
    }
}
