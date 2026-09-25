package com.bookloop.dao;

import com.bookloop.util.PasswordUtil;

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

    /** Inserts demo users + books only if the users table is empty. */
    private void seedData() throws SQLException {
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM users")) {
            if (rs.getInt(1) > 0) return;  // already seeded
        }

        // Three demo users — all share password: Password1!
        String[][] userData = {
            {"Alice Johnson",  "+1-555-0101", "alice@bookloop.com"},
            {"Bob Smith",      "+1-555-0102", "bob@bookloop.com"},
            {"Charlie Brown",  "+1-555-0103", "charlie@bookloop.com"}
        };
        int[] ids = new int[3];
        String insertUser = "INSERT INTO users(full_name, phone, email, password_hash, salt) VALUES(?,?,?,?,?)";
        for (int i = 0; i < userData.length; i++) {
            String salt = PasswordUtil.generateSalt();
            String hash = PasswordUtil.hash("Password1!", salt);
            try (PreparedStatement ps = connection.prepareStatement(insertUser, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, userData[i][0]);
                ps.setString(2, userData[i][1]);
                ps.setString(3, userData[i][2]);
                ps.setString(4, hash);
                ps.setString(5, salt);
                ps.executeUpdate();
                try (ResultSet k = ps.getGeneratedKeys()) { ids[i] = k.getInt(1); }
            }
        }

        // Four demo books
        Object[][] books = {
            {ids[0], "The Great Gatsby",        "F. Scott Fitzgerald", "Scribner",              "9780743273565",
             "A story of the fabulously wealthy Jay Gatsby and his love for the beautiful Daisy Buchanan.", null},
            {ids[0], "To Kill a Mockingbird",   "Harper Lee",          "J. B. Lippincott & Co.","9780061935466",
             "An unforgettable novel of a childhood in a sleepy Southern town and the crisis of conscience that rocked it.", null},
            {ids[1], "1984",                    "George Orwell",       "Secker & Warburg",      "9780451524935",
             "A dystopian novel set in a totalitarian state ruled by the omnipresent Big Brother.", null},
            {ids[2], "The Hobbit",              "J.R.R. Tolkien",      "George Allen & Unwin",  "9780618260300",
             "A fantasy novel about the quest of home-loving hobbit Bilbo Baggins.", null}
        };
        String insertBook = "INSERT INTO books(owner_id,title,author,publisher,isbn,description,cover_url) VALUES(?,?,?,?,?,?,?)";
        for (Object[] b : books) {
            try (PreparedStatement ps = connection.prepareStatement(insertBook)) {
                ps.setInt(1,    (int) b[0]);
                ps.setString(2, (String) b[1]);
                ps.setString(3, (String) b[2]);
                ps.setString(4, (String) b[3]);
                ps.setString(5, (String) b[4]);
                ps.setString(6, (String) b[5]);
                ps.setString(7, (String) b[6]);
                ps.executeUpdate();
            }
        }
        LOGGER.info("Seed data inserted (3 users, 4 books).");
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
