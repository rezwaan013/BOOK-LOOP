package com.bookloop.controller;

import com.bookloop.model.Notification;
import com.bookloop.service.NotificationService;
import com.bookloop.util.AlertUtil;
import com.bookloop.util.SessionManager;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** Controller for the Notifications screen. */
public class NotificationsController {

    @FXML private VBox  notificationsContainer;
    @FXML private Label emptyLabel;

    private final NotificationService service = new NotificationService();
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("MMM d, yyyy HH:mm");

    @FXML private void initialize() { loadNotifications(); }

    @FXML
    private void handleMarkAllRead() {
        try {
            service.markAllRead(SessionManager.getCurrentUser().getId());
            loadNotifications();
        } catch (SQLException e) { AlertUtil.showError("Error", e.getMessage()); }
    }

    private void loadNotifications() {
        notificationsContainer.getChildren().clear();
        try {
            List<Notification> ns = service.getNotificationsForUser(SessionManager.getCurrentUser().getId());
            emptyLabel.setVisible(ns.isEmpty());
            for (Notification n : ns) notificationsContainer.getChildren().add(buildCard(n));
        } catch (SQLException e) { AlertUtil.showError("Error", e.getMessage()); }
    }

    private HBox buildCard(Notification n) {
        HBox card = new HBox(12);
        card.getStyleClass().add("notification-card");
        if (!n.isRead()) card.getStyleClass().add("unread");
        card.setPadding(new Insets(12, 16, 12, 16));

        Label dot = new Label(n.isRead() ? "\u25CB" : "\u25CF");
        dot.getStyleClass().add(n.isRead() ? "read-dot" : "unread-dot");

        VBox content = new VBox(4);
        HBox.setHgrow(content, Priority.ALWAYS);
        Label msg  = new Label(n.getMessage()); msg.setWrapText(true);
        msg.getStyleClass().add(n.isRead() ? "notification-text-read" : "notification-text");
        Label time = new Label(n.getCreatedAt() != null ? n.getCreatedAt().format(FMT) : "");
        time.getStyleClass().add("notification-time");
        content.getChildren().addAll(msg, time);

        card.getChildren().addAll(dot, content);
        if (!n.isRead()) {
            Button markRead = new Button("Mark Read"); markRead.getStyleClass().add("mark-read-btn");
            markRead.setOnAction(e -> {
                try { service.markRead(n.getId()); loadNotifications(); }
                catch (SQLException ex) { AlertUtil.showError("Error", ex.getMessage()); }
            });
            card.getChildren().add(markRead);
        }
        return card;
    }
}
