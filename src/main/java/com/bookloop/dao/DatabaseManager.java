package com.bookloop.dao;

import java.sql.*;
import java.util.logging.Logger;

/**
 * Singleton responsible for the SQLite connection lifecycle.
 * Creates bookloop.db in the working directory on first run,
 * initialises all tables, and inserts seed data.
 */
public class DatabaseManager {

    private static final Logger LOGGER = Logger.getLogger(DatabaseManager.class.getName());
    private static final String DB_URL  = "jdbc:sqlite:bookloop.db";
    private static DatabaseManager instance;
    private Connection connection;

    private DatabaseManager() {}

    /** Returns the singleton instance (thread-safe). */
    public static synchronized DatabaseManager getInstance() {
        if (instance == null) instance = new DatabaseManager();
        return instance;
    }

    /** Returns the shared SQLite connection. */
    public Connection getConnection() { return connection; }

    /**
     * Opens (or creates) bookloop.db, enables foreign keys,
     * creates all tables, and seeds demo data on first run.
     */
    public void initialize() throws SQLException {
        connection = DriverManager.getConnection(DB_URL);
        connection.setAutoCommit(true);
        try (Statement st = connection.createStatement()) {
            st.execute("PRAGMA foreign_keys = ON");
        }
        createTables();
        seedData();
        LOGGER.info("Database initialised at: bookloop.db");
    }

    private void createTables() throws SQLException {
        String[] ddl = {
            """
            CREATE TABLE IF NOT EXISTS users (
                id            INTEGER PRIMARY KEY AUTOINCREMENT,
                full_name     TEXT    NOT NULL,
                phone         TEXT    NOT NULL,
                email         TEXT    UNIQUE NOT NULL,
                password_hash TEXT    NOT NULL,
                salt          TEXT    NOT NULL,
                reward_points INTEGER NOT NULL DEFAULT 0,
                created_at    TEXT    DEFAULT (datetime('now'))
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS books (
                id          INTEGER PRIMARY KEY AUTOINCREMENT,
                owner_id    INTEGER NOT NULL REFERENCES users(id),
                title       TEXT    NOT NULL,
                author      TEXT    NOT NULL,
                publisher   TEXT,
                isbn        TEXT,
                description TEXT,
                cover_url   TEXT,
                current_address TEXT DEFAULT '',
                category    TEXT DEFAULT 'Others',
                available   INTEGER DEFAULT 1,
                created_at  TEXT    DEFAULT (datetime('now'))
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS borrow_requests (
                id           INTEGER PRIMARY KEY AUTOINCREMENT,
                book_id      INTEGER NOT NULL REFERENCES books(id),
                requester_id INTEGER NOT NULL REFERENCES users(id),
                status       TEXT    DEFAULT 'PENDING',
                duration_days INTEGER NOT NULL,
                request_date TEXT    DEFAULT (datetime('now')),
                due_date     TEXT
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS notifications (
                id         INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id    INTEGER NOT NULL REFERENCES users(id),
                message    TEXT    NOT NULL,
                is_read    INTEGER DEFAULT 0,
                created_at TEXT    DEFAULT (datetime('now'))
            )
            """
        };
        try (Statement st = connection.createStatement()) {
            for (String sql : ddl) st.execute(sql);
            // Migrate existing databases: add reward_points if missing, backfill 50 pts
            try {
                boolean hasCol = false;
                try (ResultSet rs = st.executeQuery("PRAGMA table_info(users)")) {
                    while (rs.next()) {
                        if ("reward_points".equalsIgnoreCase(rs.getString("name"))) { hasCol = true; break; }
                    }
                }
                if (!hasCol) {
                    st.execute("ALTER TABLE users ADD COLUMN reward_points INTEGER NOT NULL DEFAULT 0");
                    st.execute("UPDATE users SET reward_points = 50 WHERE reward_points = 0");
                }
            } catch (SQLException e) {
                LOGGER.warning("Reward points migration: " + e.getMessage());
            }
            // Migrate existing databases: add current_address to books if missing
            try {
                boolean hasAddr = false;
                try (ResultSet rs = st.executeQuery("PRAGMA table_info(books)")) {
                    while (rs.next()) {
                        if ("current_address".equalsIgnoreCase(rs.getString("name"))) { hasAddr = true; break; }
                    }
                }
                if (!hasAddr) st.execute("ALTER TABLE books ADD COLUMN current_address TEXT DEFAULT ''");
                boolean hasCat = false;
                try (ResultSet rs = st.executeQuery("PRAGMA table_info(books)")) {
                    while (rs.next()) {
                        if ("category".equalsIgnoreCase(rs.getString("name"))) { hasCat = true; break; }
                    }
                }
                if (!hasCat) st.execute("ALTER TABLE books ADD COLUMN category TEXT DEFAULT 'Others'");
            } catch (SQLException e) {
                LOGGER.warning("Current address migration: " + e.getMessage());
            }
        }
        LOGGER.info("Schema verified.");
    }

    /** No seed data — the app starts with empty tables. */
    private void seedData() throws SQLException {
    }

    /** Closes the database connection cleanly. */
    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                LOGGER.info("Database connection closed.");
            }
        } catch (SQLException e) {
            LOGGER.warning("Error closing DB: " + e.getMessage());
        }
    }
}
