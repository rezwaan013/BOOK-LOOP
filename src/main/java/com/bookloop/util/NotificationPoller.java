package com.bookloop.util;

import com.bookloop.dao.NotificationDAO;
import javafx.application.Platform;

import java.sql.SQLException;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.logging.Logger;

/**
 * Background poller that checks for unread notifications every 5 seconds.
 * Demonstrates <b>concurrency</b>: a single-threaded
 * {@link ScheduledExecutorService} (thread pool) running a daemon thread,
 * with results marshalled back to the UI via
 * {@code Platform.runLater()} so the JavaFX thread is never blocked.
 * The polling thread shuts down cleanly via {@link #shutdown()}.
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
        start(userId, onUnreadCountChange, msg -> {}, (delta, balance) -> {});
    }

    /**
     * Full variant used by the dashboard: badge counts plus live toast popups.
     * Runs two lightweight DB reads per tick on the background thread:
     * unread count (+ newest message for toasts) and the user's points balance.
     * A toast fires only when something actually <i>changed</i> (new unread id
     * or a different points balance), so the user can keep browsing
     * uninterrupted in one window while events pop up in another layer.
     *
     * @param onNewNotification fired once per new unread notification message
     * @param onPointsChanged   fired as (delta, newBalance) when points change
     */
    public static void start(int userId,
                             Consumer<Integer> onUnreadCountChange,
                             Consumer<String> onNewNotification,
                             java.util.function.BiConsumer<Integer, Integer> onPointsChanged) {
        shutdown();
        executor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "notification-poller");
            t.setDaemon(true);
            return t;
        });
        final int[] lastSeenId = {-1};
        final int[] lastPoints = {Integer.MIN_VALUE};
        final boolean[] firstTick = {true};
        executor.scheduleAtFixedRate(() -> {
            try {
                com.bookloop.dao.NotificationDAO dao = new com.bookloop.dao.NotificationDAO();
                int count = dao.countUnread(userId);
                var latest = dao.findLatestUnread(userId);
                int points = new com.bookloop.dao.UserDAO().findById(userId)
                        .map(com.bookloop.model.User::getRewardPoints).orElse(0);
                Platform.runLater(() -> {
                    onUnreadCountChange.accept(count);
                    if (latest.isPresent()) {
                        int id = latest.get().getId();
                        if (!firstTick[0] && id != lastSeenId[0]) {
                            lastSeenId[0] = id;
                            onNewNotification.accept(latest.get().getMessage());
                        } else if (firstTick[0]) {
                            lastSeenId[0] = id;
                        }
                    }
                    if (lastPoints[0] == Integer.MIN_VALUE) {
                        lastPoints[0] = points;
                    } else if (points != lastPoints[0]) {
                        int delta = points - lastPoints[0];
                        lastPoints[0] = points;
                        onPointsChanged.accept(delta, points);
                    }
                    firstTick[0] = false;
                });
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
