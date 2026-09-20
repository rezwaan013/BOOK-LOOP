package com.bookloop.service;

import com.bookloop.dao.NotificationDAO;
import com.bookloop.model.Notification;

import java.sql.SQLException;
import java.util.List;

/** Business logic for notification management. */
public class NotificationService {

    private final NotificationDAO dao = new NotificationDAO();

    /** Creates and persists a notification for the given user. */
    public void createNotification(int userId, String message) throws SQLException {
        dao.save(new Notification(userId, message));
    }

    /** Returns all notifications for a user, newest first. */
    public List<Notification> getNotificationsForUser(int userId) throws SQLException {
        return dao.findByUser(userId);
    }

    /** Marks a single notification as read. */
    public void markRead(int notificationId) throws SQLException {
        dao.markRead(notificationId);
    }

    /** Marks all notifications for a user as read. */
    public void markAllRead(int userId) throws SQLException {
        dao.markAllRead(userId);
    }

    /** Returns unread notification count for a user. */
    public int countUnread(int userId) throws SQLException {
        return dao.countUnread(userId);
    }
}
