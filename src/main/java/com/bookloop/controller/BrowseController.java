package com.bookloop.controller;

import com.bookloop.model.Book;
import com.bookloop.service.BookService;
import com.bookloop.service.BorrowService;
import com.bookloop.util.AlertUtil;
import com.bookloop.util.SessionManager;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;

import java.sql.SQLException;
import java.util.List;

/** Controller for the Browse Books screen — list available books and send borrow requests. */
public class BrowseController {

    @FXML private VBox                booksContainer;
    @FXML private TextField           searchField;
    @FXML private ComboBox<Integer>   durationComboBox;
    @FXML private Label               emptyLabel;

    private final BookService   bookService   = new BookService();
    private final BorrowService borrowService = new BorrowService();

    @FXML
    private void initialize() {
        durationComboBox.getItems().addAll(7, 14, 21);
        durationComboBox.setValue(7);
        loadBooks(null);
    }

    @FXML private void handleSearch()      { loadBooks(searchField.getText().trim()); }
    @FXML private void handleClearSearch() { searchField.clear(); loadBooks(null); }

    private void loadBooks(String query) {
        booksContainer.getChildren().clear();
        try {
            int userId = SessionManager.getCurrentUser().getId();
            List<Book> books = (query == null || query.isBlank())
                    ? bookService.getBrowseBooks(userId)
                    : bookService.searchBooks(query, userId);
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
        Label titleLbl  = new Label(book.getTitle()); titleLbl.getStyleClass().add("book-title");
        Label authorLbl = new Label("by " + book.getAuthor()); authorLbl.getStyleClass().add("book-author");
        Label ownerLbl  = new Label("Owner: " + book.getOwnerName()); ownerLbl.getStyleClass().add("book-meta");
        info.getChildren().addAll(titleLbl, authorLbl, ownerLbl);
        if (book.getDescription() != null && !book.getDescription().isBlank()) {
            Label desc = new Label(book.getDescription().length() > 120
                    ? book.getDescription().substring(0, 120) + "..." : book.getDescription());
            desc.setWrapText(true); desc.getStyleClass().add("book-description");
            info.getChildren().add(desc);
        }

        Button borrowBtn = new Button("Request to Borrow");
        borrowBtn.getStyleClass().add("primary-button");
        borrowBtn.setOnAction(e -> handleBorrowRequest(book));

        VBox actions = new VBox();
        actions.setAlignment(javafx.geometry.Pos.CENTER);
        actions.getChildren().add(borrowBtn);
        card.getChildren().addAll(cover, info, actions);
        return card;
    }

    private void handleBorrowRequest(Book book) {
        int duration = durationComboBox.getValue();
        if (!AlertUtil.showConfirm("Borrow Request",
                "Request to borrow \"" + book.getTitle() + "\" for " + duration + " days?")) return;
        try {
            String name = SessionManager.getCurrentUser().getFullName();
            int    uid  = SessionManager.getCurrentUser().getId();
            borrowService.requestBorrow(book.getId(), uid, duration, name);
            AlertUtil.showInfo("Request Sent",
                    "Your request has been sent to " + book.getOwnerName() + "!");
            loadBooks(searchField.getText().trim());
        } catch (Exception e) {
            AlertUtil.showError("Error", e.getMessage());
        }
    }
}
