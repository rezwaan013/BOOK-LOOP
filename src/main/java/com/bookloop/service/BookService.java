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

    private final BookDAO bookDAO = new BookDAO();
    /** Reward economy, referenced through the interface (polymorphism). */
    private final RewardPolicy rewards = new StandardRewardPolicy();

    /** Book categories shown in the Add form and Browse filter. */
    public static final List<String> CATEGORIES = List.of(
            "Academic", "Fiction", "Non Fiction", "History", "Others");

    /**
     * Adds a new book for the given owner.
     *
     * @param ownerId   the logged-in user's id
     * @param title     book title (required)
     * @param author    author name (required)
     * @param publisher publisher (optional)
     * @param currentAddress where the book is currently available (e.g. hall name)
     * @param category  one of {@link #CATEGORIES}
     */
    public Book addBook(int ownerId, String title, String author,
                        String publisher, String currentAddress, String category) throws SQLException {
        return addBook(ownerId, title, author, publisher, currentAddress, category, null, null);
    }

    /**
     * Full variant carrying optional cover/description metadata.
     */
    public Book addBook(int ownerId, String title, String author,
                        String publisher, String currentAddress, String category,
                        String coverUrl, String description) throws SQLException {
        Book book = new Book();
        book.setOwnerId(ownerId);
        book.setTitle(title.trim());
        book.setAuthor(author.trim());
        book.setPublisher(publisher == null ? "" : publisher.trim());
        book.setIsbn("");
        book.setCurrentAddress(currentAddress == null ? "" : currentAddress.trim());
        book.setCategory(category);
        book.setCoverUrl(coverUrl);
        book.setDescription(description);
        book.setAvailable(true);

        bookDAO.save(book);
        // Reward: +10 pts for contributing a book
        try {
            int updated = new com.bookloop.dao.UserDAO().addPoints(ownerId, rewards.pointsForAddingBook());
            com.bookloop.model.User current = com.bookloop.util.SessionManager.getCurrentUser();
            if (current != null && current.getId() == ownerId) current.setRewardPoints(updated);
        } catch (SQLException ignored) {}
        return book;
    }

    /** Legacy overload (ISBN removed) — delegates to the new signature. */
    public Book addBook(int ownerId, String title, String author,
                        String publisher, String isbn) throws SQLException {
        return addBook(ownerId, title, author, publisher, "", "Others");
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
     * Searches available books by title/author and category, excluding the user's own books.
     * Pass "All" (or blank) as category to disable the category filter.
     * If the query is blank, returns all available books (in the category).
     */
    public List<Book> searchBooks(String query, int currentUserId) throws SQLException {
        return searchBooks(query, currentUserId, "All");
    }

    public List<Book> searchBooks(String query, int currentUserId, String category) throws SQLException {
        boolean noQuery = (query == null || query.isBlank());
        boolean noCat = (category == null || category.isBlank() || "All".equalsIgnoreCase(category));
        if (noQuery && noCat) return getBrowseBooks(currentUserId);
        return bookDAO.search(noQuery ? null : query, currentUserId, noCat ? "All" : category);
    }

    /** Looks up a single book by its id. */
    public Optional<Book> findById(int bookId) throws SQLException {
        return bookDAO.findById(bookId);
    }

    /**
     * Deletes a book (CRUD Delete). Only the owner may delete, and only
     * while the book is not currently borrowed.
     */
    public void deleteBook(int bookId, int requesterId) throws SQLException {
        Book book = bookDAO.findById(bookId)
                .orElseThrow(() -> new IllegalArgumentException("Book not found."));
        if (book.getOwnerId() != requesterId)
            throw new IllegalArgumentException("You can only delete your own books.");
        if (!book.isAvailable())
            throw new IllegalStateException("Cannot delete: this book is currently borrowed.");
        bookDAO.deleteById(bookId);
    }
}
