package com.bookloop.util;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * Central navigation helper.
 * Call navigateTo() to swap the primary scene,
 * or loadInto() to embed a view inside a container pane.
 */
public final class NavigationUtil {

    private static final Logger LOGGER = Logger.getLogger(NavigationUtil.class.getName());
    private static Stage primaryStage;

    private NavigationUtil() {}

    public static void setPrimaryStage(Stage stage) { primaryStage = stage; }
    public static Stage getPrimaryStage()            { return primaryStage; }

    /**
     * Loads /fxml/{name}.fxml and replaces the primary scene.
     * Applies the shared stylesheet automatically.
     *
     * @param fxmlName FXML file name without the .fxml extension
     * @param title    window title to set
     */
    public static void navigateTo(String fxmlName, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    NavigationUtil.class.getResource("/fxml/" + fxmlName + ".fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root);
            scene.getStylesheets().add(
                    NavigationUtil.class.getResource("/css/styles.css").toExternalForm());
            primaryStage.setScene(scene);
            primaryStage.setTitle(title);
        } catch (IOException e) {
            LOGGER.severe("Navigation failed [" + fxmlName + "]: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Loads /fxml/{name}.fxml and sets its root as the sole child of the container.
     * If the container is an AnchorPane the view is pinned to all four edges.
     *
     * @return the FXMLLoader so the caller can retrieve the controller if needed
     */
    public static FXMLLoader loadInto(Pane container, String fxmlName) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    NavigationUtil.class.getResource("/fxml/" + fxmlName + ".fxml"));
            Parent root = loader.load();
            container.getChildren().setAll(root);
            if (container instanceof AnchorPane ap) {
                AnchorPane.setTopAnchor(root,    0.0);
                AnchorPane.setRightAnchor(root,  0.0);
                AnchorPane.setBottomAnchor(root, 0.0);
                AnchorPane.setLeftAnchor(root,   0.0);
            } else if (root instanceof Region region) {
                region.prefWidthProperty().bind(container.widthProperty());
                region.prefHeightProperty().bind(container.heightProperty());
            }
            return loader;
        } catch (IOException e) {
            LOGGER.severe("loadInto failed [" + fxmlName + "]: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
}
