package com.bookloop.controller;

import com.bookloop.service.AuthService;
import com.bookloop.util.NavigationUtil;
import com.bookloop.util.NotificationPoller;
import com.bookloop.util.SessionManager;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;

/** Shell controller for the dashboard — manages sidebar navigation and the content area. */
public class DashboardController {

    @FXML private Label       userNameLabel;
    @FXML private Label       notificationBadge;
    @FXML private AnchorPane  contentArea;
    @FXML private Button      myBooksBtn;
    @FXML private Button      browseBooksBtn;
    @FXML private Button      requestsBtn;
    @FXML private Button      notificationsBtn;

    private final AuthService authService = new AuthService();
    private Button activeButton;

    @FXML
    private void initialize() {
        if (SessionManager.getCurrentUser() != null)
            userNameLabel.setText(SessionManager.getCurrentUser().getFullName());

        int userId = SessionManager.getCurrentUser().getId();
        NotificationPoller.start(userId, count -> {
            if (count > 0) {
                notificationBadge.setText(String.valueOf(count));
                notificationBadge.setVisible(true);
            } else {
                notificationBadge.setVisible(false);
            }
        });

        handleMyBooks(); // default view
    }

    @FXML private void handleMyBooks()      { setActive(myBooksBtn);      NavigationUtil.loadInto(contentArea, "my_books"); }
    @FXML private void handleBrowseBooks()  { setActive(browseBooksBtn);  NavigationUtil.loadInto(contentArea, "browse"); }
    @FXML private void handleRequests()     { setActive(requestsBtn);     NavigationUtil.loadInto(contentArea, "requests"); }

    @FXML
    private void handleNotifications() {
        setActive(notificationsBtn);
        notificationBadge.setVisible(false);
        NavigationUtil.loadInto(contentArea, "notifications");
    }

    @FXML
    private void handleLogout() {
        NotificationPoller.shutdown();
        authService.logout();
        NavigationUtil.navigateTo("login", "BookLoop \u2014 Login");
    }

    private void setActive(Button btn) {
        if (activeButton != null) activeButton.getStyleClass().remove("active");
        activeButton = btn;
        if (!btn.getStyleClass().contains("active")) btn.getStyleClass().add("active");
    }
}
