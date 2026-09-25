package com.bookloop.dao;

import com.bookloop.model.Book;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

/** CRUD and query operations for the books table.
 * Extends {@link AbstractDAO} (abstract class) and implements
 * {@link CrudRepository} (interface) — see those files for the OOP showcase. */
public class BookDAO extends AbstractDAO<Book> implements CrudRepository<Book, Integer> {

    /**
     * Inserts a new book and sets its generated id.
     * @return generated id
     */
    public int save(Book book) throws SQLException {
        String sql = """
            INSERT INTO books(owner_id,title,author,publisher,isbn,description,cover_url,current_address,category,available)
            VALUES(?,?,?,?,?,?,?,?,?,?)
            """;
        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, book.getOwnerId());
            ps.setString(2, book.getTitle());
            ps.setString(3, book.getAuthor());
            ps.setString(4, book.getPublisher());
            ps.setString(5, book.getIsbn());
            ps.setString(6, book.getDescription());
            ps.setString(7, book.getCoverUrl());
            ps.setString(8, book.getCurrentAddress() == null ? "" : book.getCurrentAddress());
            ps.setString(9, book.getCategory() == null ? "Others" : book.getCategory());
            ps.setInt(10, book.isAvailable() ? 1 : 0);
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
     * Searches available books by title or author (case-insensitive LIKE),
     * optionally filtered by category. Excludes the searching user's own books.
     */
    public List<Book> search(String term, int excludeOwnerId) throws SQLException {
        return search(term, excludeOwnerId, "All");
    }

    public List<Book> search(String term, int excludeOwnerId, String category) throws SQLException {
        boolean filterCat = category != null && !category.isBlank() && !"All".equalsIgnoreCase(category);
        boolean filterTerm = term != null && !term.isBlank();
        String sql = """
            SELECT b.*, u.full_name AS owner_name
            FROM books b JOIN users u ON b.owner_id = u.id
            WHERE b.owner_id != ? AND b.available = 1
            """
            + (filterTerm ? "  AND (LOWER(b.title) LIKE ? OR LOWER(b.author) LIKE ?)\n" : "")
            + (filterCat ? "  AND b.category = ?\n" : "")
            + "ORDER BY b.created_at DESC";
        String pat = "%" + (filterTerm ? term.toLowerCase() : "") + "%";
        return query(sql, ps -> {
            int i = 1;
            ps.setInt(i++, excludeOwnerId);
            if (filterTerm) { ps.setString(i++, pat); ps.setString(i++, pat); }
            if (filterCat) ps.setString(i++, category);
        });
    }

    /** Returns available books in a category, excluding the given user's own books. */
    public List<Book> findByCategory(String category, int excludeOwnerId) throws SQLException {
        return search(null, excludeOwnerId, category);
    }

    /** Looks up a single book by primary key (JOINed with user for owner name). */
    @Override
    public Optional<Book> findById(Integer id) throws SQLException {
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

    /**
     * Deletes a book by id (CRUD Delete).
     * Only the owner should call this (enforced in {@link com.bookloop.service.BookService}).
     */
    @Override
    public void deleteById(Integer bookId) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("DELETE FROM books WHERE id=?")) {
            ps.setInt(1, bookId);
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

    @Override
    protected Book map(ResultSet rs) throws SQLException {
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
        try { b.setCurrentAddress(rs.getString("current_address")); } catch (SQLException ignored) { b.setCurrentAddress(""); }
        try { b.setCategory(rs.getString("category")); } catch (SQLException ignored) { b.setCategory("Others"); }
        b.setAvailable(rs.getInt("available") == 1);
        String ts = rs.getString("created_at");
        if (ts != null) { try { b.setCreatedAt(LocalDateTime.parse(ts.replace(" ","T"))); } catch (Exception ignored) {} }
        return b;
    }
}
