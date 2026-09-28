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
    @FXML private TextField addressField;
    @FXML private ComboBox<String> categoryComboBox;
    @FXML private Label     statusLabel;
    @FXML private VBox      addBookForm;
    @FXML private Label     emptyLabel;
    @FXML private ProgressIndicator fetchSpinner;
    @FXML private Label     fetchLabel;

    private final BookService bookService = new BookService();
    private final BookApiService apiService = new BookApiService();
    /** Thread pool for the auto-fill HTTP call; results return via Platform.runLater(). */
    private final java.util.concurrent.ExecutorService networkPool =
            java.util.concurrent.Executors.newCachedThreadPool(r -> {
                Thread t = new Thread(r, "openlibrary-fetch");
                t.setDaemon(true);
                return t;
            });
    private String fetchedCoverUrl;
    private String fetchedDescription;

    @FXML
    private void initialize() {
        statusLabel.setVisible(false);
        addBookForm.setVisible(false);
        addBookForm.setManaged(false);
        categoryComboBox.getItems().addAll(BookService.CATEGORIES);
        categoryComboBox.setValue("Others");
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
    private void handleAddBook() {
        String title     = titleField.getText().trim();
        String author    = authorField.getText().trim();
        String publisher = publisherField.getText().trim();
        String address   = addressField.getText().trim();
        String category  = categoryComboBox.getValue() == null ? "Others" : categoryComboBox.getValue();
        if (title.isEmpty() || author.isEmpty()) {
            statusLabel.setText("Title and Author are required.");
            statusLabel.setVisible(true);
            return;
        }
        try {
            int ownerId = SessionManager.getCurrentUser().getId();
            int before = SessionManager.getCurrentUser().getRewardPoints();
            bookService.addBook(ownerId, title, author, publisher, address, category,
                    fetchedCoverUrl, fetchedDescription);
            int after = SessionManager.getCurrentUser().getRewardPoints();
            clearForm();
            addBookForm.setVisible(false);
            addBookForm.setManaged(false);
            loadBooks();
            AlertUtil.showInfo("Book Added",
                    "\"" + title + "\" added to your library!\n★ +" + (after - before)
                    + " pts (balance: " + after + " pts)");
        } catch (SQLException e) {
            AlertUtil.showError("Error", "Failed to add book: " + e.getMessage());
        }
    }

    /** Auto-fills publisher/cover: local catalog first, else Open Library API. */
    @FXML
    private void handleAutoFill() {
        String title = titleField.getText().trim();
        String author = authorField.getText().trim();
        if (title.isEmpty()) {
            statusLabel.setText("Enter a Title first, then Auto-fill.");
            statusLabel.setVisible(true);
            return;
        }
        fetchSpinner.setVisible(true);
        fetchLabel.setText("Looking up book details...");
        networkPool.submit(() -> {
            var opt = apiService.fetchByTitle(title, author);
            Platform.runLater(() -> {
                fetchSpinner.setVisible(false);
                if (opt.isPresent()) {
                    Book api = opt.get();
                    if (api.getPublisher() != null && !api.getPublisher().isBlank()
                            && publisherField.getText().isBlank())
                        publisherField.setText(api.getPublisher());
                    fetchedCoverUrl = api.getCoverUrl();
                    fetchedDescription = api.getDescription();
                    String source = api.getDescription() != null
                            && api.getDescription().contains("local catalog")
                            ? "local catalog" : "Open Library";
                    String found = api.getPublisher() != null && !api.getPublisher().isBlank()
                            ? "Publisher: " + api.getPublisher() : "Details found";
                    fetchLabel.setText("Found via " + source + " (" + found + ") — will be saved with the book.");
                    statusLabel.setVisible(false);
                } else {
                    fetchLabel.setText("No match found — fill in manually.");
                }
            });
        });
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
        Label catLbl = new Label(book.getCategory() == null ? "Others" : book.getCategory());
        catLbl.getStyleClass().add("category-badge");
        info.getChildren().add(catLbl);
        if (addrLbl != null) info.getChildren().add(addrLbl);
        if (book.getDescription() != null && !book.getDescription().isBlank()) {
            Label desc = new Label(book.getDescription().length() > 120
                    ? book.getDescription().substring(0, 120) + "..." : book.getDescription());
            desc.getStyleClass().add("book-description");
            desc.setWrapText(true);
            info.getChildren().add(desc);
        }
        info.getChildren().add(avail);
        Button deleteBtn = new Button("Delete");
        deleteBtn.getStyleClass().add("danger-button");
        deleteBtn.setOnAction(e -> {
            if (!AlertUtil.showConfirm("Delete Book",
                    "Delete \"" + book.getTitle() + "\" from your library?")) return;
            try {
                bookService.deleteBook(book.getId(), SessionManager.getCurrentUser().getId());
                loadBooks();
            } catch (Exception ex) {
                AlertUtil.showError("Cannot delete", ex.getMessage());
            }
        });
        VBox actions = new VBox(deleteBtn);
        actions.setAlignment(javafx.geometry.Pos.CENTER);
        card.getChildren().addAll(cover, info, actions);
        return card;
    }

    private void clearForm() {
        titleField.clear(); authorField.clear(); publisherField.clear(); addressField.clear();
        categoryComboBox.setValue("Others");
        fetchedCoverUrl = null;
        fetchedDescription = null;
        fetchLabel.setText("");
        fetchSpinner.setVisible(false);
        statusLabel.setVisible(false);
    }
}
