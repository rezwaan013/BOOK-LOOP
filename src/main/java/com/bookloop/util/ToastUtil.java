package com.bookloop.util;

import javafx.animation.PauseTransition;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/**
 * Small toast-style popup notifications shown inside the dashboard
 * while the user keeps working (e.g. browsing books when a new
 * notification arrives, or when reward points change).
 * Must be called on the JavaFX thread.
 */
public final class ToastUtil {

    private ToastUtil() {}

    /**
     * Shows a toast in the given container; auto-hides after ~4 seconds.
     * @param container the toast overlay box from dashboard.fxml
     * @param title     bold header line
     * @param message   body text
     * @param styleClass extra CSS class ("toast-info", "toast-points", ...)
     */
    public static void show(VBox container, String title, String message, String styleClass) {
        container.getChildren().clear();
        Label titleLbl = new Label(title);
        titleLbl.getStyleClass().add("toast-title");
        Label msgLbl = new Label(message);
        msgLbl.getStyleClass().add("toast-message");
        msgLbl.setWrapText(true);
        VBox toast = new VBox(2, titleLbl, msgLbl);
        toast.getStyleClass().addAll("toast", styleClass);
        container.getChildren().add(toast);
        container.setVisible(true);
        container.toFront();
        PauseTransition hide = new PauseTransition(Duration.seconds(4));
        hide.setOnFinished(e -> {
            container.getChildren().clear();
            container.setVisible(false);
        });
        hide.play();
    }

    public static void showNotification(VBox container, String message) {
        show(container, "🔔 New notification", message, "toast-info");
    }

    public static void showPoints(VBox container, int delta, int balance) {
        String sign = delta >= 0 ? "+" : "";
        show(container, "★ " + sign + delta + " pts",
                "Balance: " + balance + " pts", "toast-points");
    }
}
