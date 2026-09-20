package com.bookloop;

import com.bookloop.dao.DatabaseManager;
import com.bookloop.util.NavigationUtil;
import com.bookloop.util.NotificationPoller;
import javafx.application.Application;
import javafx.stage.Stage;

import java.util.logging.Logger;

/**
 * Main JavaFX application entry point for BookLoop.
 * Initializes the SQLite database (creates bookloop.db on first run),
 * sets up scene navigation, and launches the login screen.
 */
public class App extends Application {

    private static final Logger LOGGER = Logger.getLogger(App.class.getName());

    @Override
    public void start(Stage primaryStage) {
        try {
            DatabaseManager.getInstance().initialize();
            NavigationUtil.setPrimaryStage(primaryStage);
            primaryStage.setMinWidth(1100);
            primaryStage.setMinHeight(720);
            NavigationUtil.navigateTo("login", "BookLoop \u2014 Login");
            primaryStage.show();
        } catch (Exception e) {
            LOGGER.severe("Failed to start application: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void stop() {
        LOGGER.info("Application shutting down...");
        NotificationPoller.shutdown();
        DatabaseManager.getInstance().close();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
