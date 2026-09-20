package com.bookloop.dao;

import com.bookloop.model.Book;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

/** CRUD and query operations for the books table. */
public class BookDAO {

    private final Connection con = DatabaseManager.getInstance().getConnection();

    /**
     * Inserts a new book and sets its generated id.
     * @return generated id
     */
    public int save(Book book) throws SQLException {
        String sql = """
            INSERT INTO books(owner_id,title,author,publisher,isbn,description,cover_url,available)
            VALUES(?,?,?,?,?,?,?,?)
            """;
        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, book.getOwnerId());
            ps.setString(2, book.getTitle());
            ps.setString(3, book.getAuthor());
            ps.setString(4, book.getPublisher());
            ps.setString(5, book.getIsbn());
            ps.setString(6, book.getDescription());
            ps.setString(7, book.getCoverUrl());
            ps.setInt(8, book.isAvailable() ? 1 : 0);
            ps.executeUpdate();
            try (ResultSet k = ps.getGeneratedKeys()) {
                int id = k.getInt(1);
                book.setId(id);
                return id;
            }
        }
    }

    /** Returns all books owned by the given user (for My Library view). */
    public List<Book> findByOwner(int ownerId) throws SQLException {
        String sql = """
            SELECT b.*, u.full_name AS owner_name
            FROM books b JOIN users u ON b.owner_id = u.id
            WHERE b.owner_id = ?
            ORDER BY b.created_at DESC
            """;
        return query(sql, ps -> ps.setInt(1, ownerId));
    }

    /** Returns available books NOT owned by the given user (for Browse view). */
    public List<Book> findAvailableExcludingOwner(int ownerId) throws SQLException {
        String sql = """
            SELECT b.*, u.full_name AS owner_name
            FROM books b JOIN users u ON b.owner_id = u.id
            WHERE b.owner_id != ? AND b.available = 1
            ORDER BY b.created_at DESC
            """;
        return query(sql, ps -> ps.setInt(1, ownerId));
    }

    /**
     * Searches available books by title or author (case-insensitive LIKE).
     * Excludes the searching user's own books.
     */
    public List<Book> search(String term, int excludeOwnerId) throws SQLException {
        String sql = """
            SELECT b.*, u.full_name AS owner_name
            FROM books b JOIN users u ON b.owner_id = u.id
            WHERE b.owner_id != ? AND b.available = 1
              AND (LOWER(b.title) LIKE ? OR LOWER(b.author) LIKE ?)
            ORDER BY b.created_at DESC
            """;
        String pat = "%" + term.toLowerCase() + "%";
        return query(sql, ps -> { ps.setInt(1, excludeOwnerId); ps.setString(2, pat); ps.setString(3, pat); });
    }

    /** Looks up a single book by primary key (JOINed with user for owner name). */
    public Optional<Book> findById(int id) throws SQLException {
        String sql = """
            SELECT b.*, u.full_name AS owner_name
            FROM books b JOIN users u ON b.owner_id = u.id
            WHERE b.id = ?
            """;
        List<Book> r = query(sql, ps -> ps.setInt(1, id));
        return r.isEmpty() ? Optional.empty() : Optional.of(r.get(0));
    }

    /** Flips the available flag (called when a borrow request is accepted / returned). */
    public void updateAvailability(int bookId, boolean available) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("UPDATE books SET available=? WHERE id=?")) {
            ps.setInt(1, available ? 1 : 0);
            ps.setInt(2, bookId);
            ps.executeUpdate();
        }
    }

    @FunctionalInterface
    private interface ParamSetter { void set(PreparedStatement ps) throws SQLException; }

    private List<Book> query(String sql, ParamSetter setter) throws SQLException {
        List<Book> list = new ArrayList<>();
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            setter.set(ps);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        }
        return list;
    }

    private Book map(ResultSet rs) throws SQLException {
        Book b = new Book();
        b.setId(rs.getInt("id"));
        b.setOwnerId(rs.getInt("owner_id"));
        b.setOwnerName(rs.getString("owner_name"));
        b.setTitle(rs.getString("title"));
        b.setAuthor(rs.getString("author"));
        b.setPublisher(rs.getString("publisher"));
        b.setIsbn(rs.getString("isbn"));
        b.setDescription(rs.getString("description"));
        b.setCoverUrl(rs.getString("cover_url"));
        b.setAvailable(rs.getInt("available") == 1);
        String ts = rs.getString("created_at");
        if (ts != null) { try { b.setCreatedAt(LocalDateTime.parse(ts.replace(" ","T"))); } catch (Exception ignored) {} }
        return b;
    }
}
