package com.bookloop.controller;

import com.bookloop.model.BorrowRequest;
import com.bookloop.model.RequestStatus;
import com.bookloop.service.BorrowService;
import com.bookloop.util.AlertUtil;
import com.bookloop.util.SessionManager;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** Controller for the Requests screen (incoming tab for owners, outgoing tab for borrowers). */
public class RequestsController {

    @FXML private VBox  incomingContainer;
    @FXML private VBox  outgoingContainer;
    @FXML private Label incomingEmptyLabel;
    @FXML private Label outgoingEmptyLabel;

    private final BorrowService borrowService = new BorrowService();
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("MMM d, yyyy");

    @FXML
    private void initialize() {
        loadIncoming();
        loadOutgoing();
    }

    private void loadIncoming() {
        incomingContainer.getChildren().clear();
        try {
            List<BorrowRequest> reqs = borrowService.getIncomingRequests(SessionManager.getCurrentUser().getId());
            incomingEmptyLabel.setVisible(reqs.isEmpty());
            for (BorrowRequest r : reqs) incomingContainer.getChildren().add(buildIncoming(r));
        } catch (SQLException e) { AlertUtil.showError("Error", e.getMessage()); }
    }

    private void loadOutgoing() {
        outgoingContainer.getChildren().clear();
        try {
            List<BorrowRequest> reqs = borrowService.getOutgoingRequests(SessionManager.getCurrentUser().getId());
            outgoingEmptyLabel.setVisible(reqs.isEmpty());
            for (BorrowRequest r : reqs) outgoingContainer.getChildren().add(buildOutgoing(r));
        } catch (SQLException e) { AlertUtil.showError("Error", e.getMessage()); }
    }

    private VBox buildIncoming(BorrowRequest req) {
        VBox card = new VBox(8);
        card.getStyleClass().add("request-card");
        card.setPadding(new Insets(16));

        Label title = new Label("\uD83D\uDCD6 " + req.getBookTitle()); title.getStyleClass().add("book-title");
        Label req2  = new Label("From: " + req.getRequesterName());   req2.getStyleClass().add("book-author");
        Label dur   = new Label(req.getDurationDays() + " days");      dur.getStyleClass().add("book-meta");
        Label due   = req.getDueDate() != null
                ? new Label("Due: " + req.getDueDate().format(FMT)) : new Label("");
        due.getStyleClass().add("book-meta");
        Label status = new Label(req.getStatus().name());
        status.getStyleClass().addAll("status-badge", "status-" + req.getStatus().name().toLowerCase());
        card.getChildren().addAll(title, req2, dur, due, status);

        if (req.getStatus() == RequestStatus.PENDING) {
            Button accept  = new Button("\u2713 Accept");  accept.getStyleClass().add("success-button");
            Button decline = new Button("\u2717 Decline"); decline.getStyleClass().add("danger-button");
            accept.setOnAction(e  -> { doAccept(req);  loadIncoming(); });
            decline.setOnAction(e -> { doDecline(req); loadIncoming(); });
            HBox btns = new HBox(8, accept, decline);
            card.getChildren().add(btns);
        }
        return card;
    }

    private HBox buildOutgoing(BorrowRequest req) {
        HBox card = new HBox(16);
        card.getStyleClass().add("request-card");
        card.setPadding(new Insets(16));
        VBox info = new VBox(4);
        HBox.setHgrow(info, Priority.ALWAYS);
        Label title  = new Label("\uD83D\uDCD6 " + req.getBookTitle()); title.getStyleClass().add("book-title");
        Label dur    = new Label(req.getDurationDays() + " days");        dur.getStyleClass().add("book-meta");
        Label due    = req.getDueDate() != null
                ? new Label("Due: " + req.getDueDate().format(FMT)) : new Label("");
        due.getStyleClass().add("book-meta");
        Label status = new Label(req.getStatus().name());
        status.getStyleClass().addAll("status-badge", "status-" + req.getStatus().name().toLowerCase());
        info.getChildren().addAll(title, dur, due, status);
        if (req.getStatus() == RequestStatus.ACCEPTED) {
            Button ret = new Button("Mark as Returned"); ret.getStyleClass().add("secondary-button");
            ret.setOnAction(e -> { doReturn(req); loadOutgoing(); });
            info.getChildren().add(ret);
        }
        card.getChildren().add(info);
        return card;
    }

    private void doAccept(BorrowRequest req) {
        if (!AlertUtil.showConfirm("Accept", "Accept " + req.getRequesterName() + "'s request?")) return;
        try { borrowService.acceptRequest(req.getId(), SessionManager.getCurrentUser().getFullName()); }
        catch (SQLException e) { AlertUtil.showError("Error", e.getMessage()); }
    }

    private void doDecline(BorrowRequest req) {
        if (!AlertUtil.showConfirm("Decline", "Decline " + req.getRequesterName() + "'s request?")) return;
        try { borrowService.declineRequest(req.getId(), SessionManager.getCurrentUser().getFullName()); }
        catch (SQLException e) { AlertUtil.showError("Error", e.getMessage()); }
    }

    private void doReturn(BorrowRequest req) {
        if (!AlertUtil.showConfirm("Return", "Mark \"" + req.getBookTitle() + "\" as returned?")) return;
        try { borrowService.returnBook(req.getId()); }
        catch (SQLException e) { AlertUtil.showError("Error", e.getMessage()); }
    }
}
