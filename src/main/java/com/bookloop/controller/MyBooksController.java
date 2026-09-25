package com.bookloop.controller;

import com.bookloop.model.Book;
import com.bookloop.service.BookApiService;
import com.bookloop.service.BookService;
import com.bookloop.util.AlertUtil;
import com.bookloop.util.SessionManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;

import java.sql.SQLException;
import java.util.List;

/** Controller for the My Library screen (view + add books). */
public class MyBooksController {

    @FXML private VBox      booksContainer;
    @FXML private TextField titleField;
    @FXML private TextField authorField;
    @FXML private TextField publisherField;
    @FXML private TextField isbnField;
    @FXML private TextField addressField;
    @FXML private Label     statusLabel;
    @FXML private VBox      addBookForm;
    @FXML private Label     emptyLabel;

    private final BookService    bookService = new BookService();
    private final BookApiService apiService  = new BookApiService();

    @FXML
    private void initialize() {
        statusLabel.setVisible(false);
        addBookForm.setVisible(false);
        addBookForm.setManaged(false);
        loadBooks();
    }

    @FXML
    private void toggleAddForm() {
        boolean show = !addBookForm.isVisible();
        addBookForm.setVisible(show);
        addBookForm.setManaged(show);
        if (!show) clearForm();
    }

    @FXML
    private void handleFetchDetails() {
        String isbn = isbnField.getText().trim();
        if (isbn.isEmpty()) { statusLabel.setText("Enter an ISBN first."); statusLabel.setVisible(true); return; }
        statusLabel.setText("Fetching from Open Library...");
        statusLabel.setVisible(true);
        new Thread(() -> {
            var opt = apiService.fetchBookDetails(isbn);
            Platform.runLater(() -> {
                if (opt.isPresent()) {
                    Book api = opt.get();
                    if (api.getPublisher() != null && !api.getPublisher().isBlank())
                        publisherField.setText(api.getPublisher());
                    statusLabel.setText("\u2713 Details fetched from Open Library!");
                } else {
                    statusLabel.setText("No data found for this ISBN. Fill in manually.");
                }
            });
        }).start();
    }

    @FXML
    private void handleAddBook() {
        String title     = titleField.getText().trim();
        String author    = authorField.getText().trim();
        String publisher = publisherField.getText().trim();
        String isbn      = isbnField.getText().trim();
        String address   = addressField.getText().trim();
        if (title.isEmpty() || author.isEmpty()) {
            statusLabel.setText("Title and Author are required.");
            statusLabel.setVisible(true);
            return;
        }
        try {
            int ownerId = SessionManager.getCurrentUser().getId();
            bookService.addBook(ownerId, title, author, publisher, isbn, address);
            clearForm();
            addBookForm.setVisible(false);
            addBookForm.setManaged(false);
            loadBooks();
            AlertUtil.showInfo("Book Added", "\"" + title + "\" has been added to your library!");
        } catch (SQLException e) {
            AlertUtil.showError("Error", "Failed to add book: " + e.getMessage());
        }
    }

    private void loadBooks() {
        booksContainer.getChildren().clear();
        try {
            List<Book> books = bookService.getMyBooks(SessionManager.getCurrentUser().getId());
            emptyLabel.setVisible(books.isEmpty());
            for (Book b : books) booksContainer.getChildren().add(buildCard(b));
        } catch (SQLException e) {
            AlertUtil.showError("Error", "Failed to load books: " + e.getMessage());
        }
    }

    private HBox buildCard(Book book) {
        HBox card = new HBox(16);
        card.getStyleClass().add("book-card");
        card.setPadding(new Insets(16));

        ImageView cover = new ImageView();
        cover.setFitWidth(60); cover.setFitHeight(80); cover.setPreserveRatio(true);
        if (book.getCoverUrl() != null && !book.getCoverUrl().isBlank()) {
            try { cover.setImage(new Image(book.getCoverUrl(), true)); } catch (Exception ignored) {}
        }

        VBox info = new VBox(4);
        HBox.setHgrow(info, Priority.ALWAYS);
        Label titleLbl  = new Label(book.getTitle());   titleLbl.getStyleClass().add("book-title");
        Label authorLbl = new Label("by " + book.getAuthor()); authorLbl.getStyleClass().add("book-author");
        Label pubLbl    = new Label(book.getPublisher() != null ? book.getPublisher() : "");
        pubLbl.getStyleClass().add("book-meta");
        Label addrLbl = null;
        if (book.getCurrentAddress() != null && !book.getCurrentAddress().isBlank()) {
            addrLbl = new Label("📍 " + book.getCurrentAddress());
            addrLbl.getStyleClass().add("book-location");
        }
        Label avail = new Label(book.isAvailable() ? "\u2713 Available" : "\u23f3 Currently Borrowed");
        avail.getStyleClass().add(book.isAvailable() ? "status-available" : "status-borrowed");
        info.getChildren().addAll(titleLbl, authorLbl, pubLbl);
        if (addrLbl != null) info.getChildren().add(addrLbl);
        if (book.getDescription() != null && !book.getDescription().isBlank()) {
            Label desc = new Label(book.getDescription().length() > 120
                    ? book.getDescription().substring(0, 120) + "..." : book.getDescription());
            desc.getStyleClass().add("book-description");
            desc.setWrapText(true);
            info.getChildren().add(desc);
        }
        info.getChildren().add(avail);
        card.getChildren().addAll(cover, info);
        return card;
    }

    private void clearForm() {
        titleField.clear(); authorField.clear(); publisherField.clear(); isbnField.clear(); addressField.clear();
        statusLabel.setVisible(false);
    }
}
