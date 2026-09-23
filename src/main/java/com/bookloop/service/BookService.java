package com.bookloop.service;

import com.bookloop.dao.BookDAO;
import com.bookloop.model.Book;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Business logic for book management.
 * Every user can both add books to lend and browse/borrow others' books.
 */
public class BookService {

    private final BookDAO       bookDAO    = new BookDAO();
    private final BookApiService apiService = new BookApiService();

    /**
     * Adds a new book for the given owner.
     * If an ISBN is provided, attempts to enrich the record from Open Library
     * (cover image, description, publisher). Falls back silently on failure.
     *
     * @param ownerId   the logged-in user's id
     * @param title     book title (required)
     * @param author    author name (required)
     * @param publisher publisher (optional; may be filled in from API)
     * @param isbn      ISBN (optional; triggers API fetch when non-blank)
     */
    public Book addBook(int ownerId, String title, String author,
                        String publisher, String isbn) throws SQLException {
        Book book = new Book();
        book.setOwnerId(ownerId);
        book.setTitle(title.trim());
        book.setAuthor(author.trim());
        book.setPublisher(publisher == null ? "" : publisher.trim());
        book.setIsbn(isbn == null ? "" : isbn.trim());
        book.setAvailable(true);

        if (isbn != null && !isbn.isBlank()) {
            apiService.fetchBookDetails(isbn.trim()).ifPresent(api -> {
                if (api.getDescription() != null && !api.getDescription().isBlank())
                    book.setDescription(api.getDescription());
                if (api.getCoverUrl() != null && !api.getCoverUrl().isBlank())
                    book.setCoverUrl(api.getCoverUrl());
                if ((publisher == null || publisher.isBlank())
                        && api.getPublisher() != null && !api.getPublisher().isBlank())
                    book.setPublisher(api.getPublisher());
            });
        }

        bookDAO.save(book);
        // Reward: +10 pts for contributing a book
        try {
            int updated = new com.bookloop.dao.UserDAO().addPoints(ownerId, 10);
            com.bookloop.model.User current = com.bookloop.util.SessionManager.getCurrentUser();
            if (current != null && current.getId() == ownerId) current.setRewardPoints(updated);
        } catch (SQLException ignored) {}
        return book;
    }

    /** Returns all books owned by this user (My Library). */
    public List<Book> getMyBooks(int ownerId) throws SQLException {
        return bookDAO.findByOwner(ownerId);
    }

    /** Returns all available books NOT owned by the current user (Browse). */
    public List<Book> getBrowseBooks(int currentUserId) throws SQLException {
        return bookDAO.findAvailableExcludingOwner(currentUserId);
    }

    /**
     * Searches available books by title or author, excluding the user's own books.
     * If the query is blank, returns all available books.
     */
    public List<Book> searchBooks(String query, int currentUserId) throws SQLException {
        return (query == null || query.isBlank())
                ? getBrowseBooks(currentUserId)
                : bookDAO.search(query, currentUserId);
    }

    /** Looks up a single book by its id. */
    public Optional<Book> findById(int bookId) throws SQLException {
        return bookDAO.findById(bookId);
    }
}
