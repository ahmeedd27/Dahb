package org.example.store.utils;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class SceneRouter {

    private static Stage mainStage;

    public static void setStage(Stage stage) {
        mainStage = stage;
    }

    public static void switchTo(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(SceneRouter.class.getResource(fxmlPath));
            Parent newRoot = loader.load();

            Scene currentScene = mainStage.getScene();
            if (currentScene == null) {
                // لأول مرة
                mainStage.setScene(new Scene(newRoot));
            } else {
                currentScene.setRoot(newRoot);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * مكسّر (يملي الشاشة) بشكل آمن على JavaFX Application Thread
     */
    public static void maximize() {
        if (mainStage != null) {
            Platform.runLater(() -> mainStage.setMaximized(true));
        }
    }

    /**
     * لو حابب وضع full-screen (يخفي شريط المهام)
     */
    public static void fullScreen() {
        if (mainStage != null) {
            Platform.runLater(() -> mainStage.setFullScreen(true));
        }
    }
}
