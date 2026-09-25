package com.bookloop.dao;

import com.bookloop.model.Notification;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

/** CRUD and query operations for the notifications table. */
public class NotificationDAO {

    private final Connection con = DatabaseManager.getInstance().getConnection();

    /** Inserts a notification and sets its generated id. */
    public void save(Notification n) throws SQLException {
        String sql = "INSERT INTO notifications(user_id,message,is_read) VALUES(?,?,0)";
        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, n.getUserId());
            ps.setString(2, n.getMessage());
            ps.executeUpdate();
            try (ResultSet k = ps.getGeneratedKeys()) { n.setId(k.getInt(1)); }
        }
    }

    /** Returns all notifications for a user, newest first. */
    public List<Notification> findByUser(int userId) throws SQLException {
        List<Notification> list = new ArrayList<>();
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT * FROM notifications WHERE user_id=? ORDER BY created_at DESC")) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        }
        return list;
    }

    /** Marks a single notification as read. */
    public void markRead(int notificationId) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("UPDATE notifications SET is_read=1 WHERE id=?")) {
            ps.setInt(1, notificationId);
            ps.executeUpdate();
        }
    }

    /** Marks all notifications for a user as read. */
    public void markAllRead(int userId) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("UPDATE notifications SET is_read=1 WHERE user_id=?")) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        }
    }

    /** Returns the count of unread notifications for a user. */
    public int countUnread(int userId) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT COUNT(*) FROM notifications WHERE user_id=? AND is_read=0")) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) { return rs.getInt(1); }
        }
    }

    /** Returns the newest unread notification, if any (for live toast popups). */
    public Optional<Notification> findLatestUnread(int userId) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT * FROM notifications WHERE user_id=? AND is_read=0 ORDER BY id DESC LIMIT 1")) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        }
    }

    private Notification map(ResultSet rs) throws SQLException {
        Notification n = new Notification();
        n.setId(rs.getInt("id"));
        n.setUserId(rs.getInt("user_id"));
        n.setMessage(rs.getString("message"));
        n.setRead(rs.getInt("is_read") == 1);
        String ts = rs.getString("created_at");
        if (ts != null) { try { n.setCreatedAt(LocalDateTime.parse(ts.replace(" ","T"))); } catch (Exception ignored) {} }
        return n;
    }
}
