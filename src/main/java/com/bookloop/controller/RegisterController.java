package com.bookloop.controller;

import com.bookloop.service.AuthService;
import com.bookloop.util.NavigationUtil;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.sql.SQLException;

/** Controller for the registration screen. */
public class RegisterController {

    @FXML private TextField     fullNameField;
    @FXML private TextField     phoneField;
    @FXML private TextField     emailField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Label         errorLabel;

    private final AuthService authService = new AuthService();

    @FXML
    private void initialize() {
        errorLabel.setVisible(false);
    }

    @FXML
    private void handleRegister() {
        String fullName = fullNameField.getText().trim();
        String phone    = phoneField.getText().trim();
        String email    = emailField.getText().trim();
        String password = passwordField.getText();
        String confirm  = confirmPasswordField.getText();

        if (fullName.isEmpty() || phone.isEmpty() || email.isEmpty() || password.isEmpty()) {
            showError("Please fill in all required fields."); return;
        }
        if (!email.contains("@")) { showError("Please enter a valid email address."); return; }
        if (password.length() < 6) { showError("Password must be at least 6 characters."); return; }
        if (!password.equals(confirm)) { showError("Passwords do not match."); return; }

        try {
            authService.register(fullName, phone, email, password);
            NavigationUtil.navigateTo("dashboard", "BookLoop \u2014 Dashboard");
        } catch (IllegalArgumentException e) {
            showError(e.getMessage());
        } catch (SQLException e) {
            showError("Database error: " + e.getMessage());
        }
    }

    @FXML
    private void handleBackToLogin() {
        NavigationUtil.navigateTo("login", "BookLoop \u2014 Login");
    }

    private void showError(String msg) {
        errorLabel.setText(msg);
        errorLabel.setVisible(true);
    }
}
