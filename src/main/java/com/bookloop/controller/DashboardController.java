package com.bookloop.controller;

import com.bookloop.service.AuthService;
import com.bookloop.util.NavigationUtil;
import com.bookloop.util.NotificationPoller;
import com.bookloop.util.SessionManager;
import com.bookloop.util.ToastUtil;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;

/** Shell controller for the dashboard — manages sidebar navigation and the content area. */
public class DashboardController {

    @FXML private Label       userNameLabel;
    @FXML private Label       notificationBadge;
    @FXML private Label       pointsLabel;
    @FXML private AnchorPane  contentArea;
    @FXML private VBox        toastBox;
    @FXML private Button      myBooksBtn;
    @FXML private Button      browseBooksBtn;
    @FXML private Button      requestsBtn;
    @FXML private Button      notificationsBtn;

    private final AuthService authService = new AuthService();
    private Button activeButton;

    @FXML
    private void initialize() {
        if (SessionManager.getCurrentUser() != null) {
            userNameLabel.setText(SessionManager.getCurrentUser().getFullName());
            refreshPoints();
        }

        int userId = SessionManager.getCurrentUser().getId();
        // Live badge + toast popups: the user keeps browsing/working while
        // notifications and points changes pop up in the overlay layer.
        NotificationPoller.start(userId, count -> {
            if (count > 0) {
                notificationBadge.setText(String.valueOf(count));
                notificationBadge.setVisible(true);
            } else {
                notificationBadge.setVisible(false);
            }
        }, message -> ToastUtil.showNotification(toastBox, message),
        (delta, balance) -> {
            if (SessionManager.getCurrentUser() != null)
                SessionManager.getCurrentUser().setRewardPoints(balance);
            refreshPoints();
            ToastUtil.showPoints(toastBox, delta, balance);
        });

        // ---- Layout responsiveness: toast width follows the window ----
        // Caps at 320px but shrinks to 45% of the content width on narrow
        // windows, via a live property binding (no hardcoded sizes).
        toastBox.maxWidthProperty().bind(javafx.beans.binding.Bindings.min(
                320, contentArea.widthProperty().multiply(0.45)));

        handleMyBooks(); // default view
    }

    @FXML private void handleMyBooks()      { setActive(myBooksBtn);      NavigationUtil.loadInto(contentArea, "my_books"); refreshPoints(); }
    @FXML private void handleBrowseBooks()  { setActive(browseBooksBtn);  NavigationUtil.loadInto(contentArea, "browse"); refreshPoints(); }
    @FXML private void handleRequests()     { setActive(requestsBtn);     NavigationUtil.loadInto(contentArea, "requests"); refreshPoints(); }

    /** Refreshes the reward-points badge from the session user. */
    public void refreshPoints() {
        if (pointsLabel != null && SessionManager.getCurrentUser() != null)
            pointsLabel.setText("★ " + SessionManager.getCurrentUser().getRewardPoints() + " pts");
    }

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
