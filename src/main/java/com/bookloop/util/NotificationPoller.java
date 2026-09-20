package com.bookloop.util;

import com.bookloop.dao.NotificationDAO;
import javafx.application.Platform;

import java.sql.SQLException;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.logging.Logger;

/**
 * Background poller that checks for unread notifications every 5 seconds.
 * Uses a daemon thread so it never prevents JVM shutdown.
 * All UI updates are dispatched via Platform.runLater().
 */
public final class NotificationPoller {

    private static final Logger LOGGER = Logger.getLogger(NotificationPoller.class.getName());
    private static final int POLL_INTERVAL_SEC = 5;
    private static ScheduledExecutorService executor;

    private NotificationPoller() {}

    /**
     * Starts (or restarts) the polling loop for the given user.
     * @param userId              logged-in user's id
     * @param onUnreadCountChange called on the JavaFX thread with the new unread count
     */
    public static void start(int userId, Consumer<Integer> onUnreadCountChange) {
        shutdown();
        executor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "notification-poller");
            t.setDaemon(true);
            return t;
        });
        executor.scheduleAtFixedRate(() -> {
            try {
                int count = new NotificationDAO().countUnread(userId);
                Platform.runLater(() -> onUnreadCountChange.accept(count));
            } catch (SQLException e) {
                LOGGER.warning("Notification poll error: " + e.getMessage());
            }
        }, 0, POLL_INTERVAL_SEC, TimeUnit.SECONDS);
        LOGGER.info("Notification poller started (userId=" + userId + ", interval=" + POLL_INTERVAL_SEC + "s)");
    }

    /** Shuts down the background thread gracefully (called on app close). */
    public static void shutdown() {
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(2, TimeUnit.SECONDS)) executor.shutdownNow();
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
            LOGGER.info("Notification poller stopped.");
        }
    }
}
