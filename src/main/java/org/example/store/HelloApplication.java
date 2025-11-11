package org.example.store;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import org.example.store.utils.SceneRouter;
import org.example.store.utils.SecurityManager;

import java.io.IOException;
import java.util.Objects;

public class HelloApplication extends Application {

    @Override
    public void start(Stage stage) throws IOException {

        // ✅ تهيئة SceneRouter بالـ Stage
        SceneRouter.setStage(stage);

        Scene scene;
        if (SecurityManager.isActivated()) {
            if (!SecurityManager.userExists()) {
                FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("userManagement.fxml"));
                scene = new Scene(fxmlLoader.load(), 1000, 700);
            } else {
                FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("login.fxml"));
                scene = new Scene(fxmlLoader.load(), 1000, 700);
            }
        } else {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("activation.fxml"));
            scene = new Scene(fxmlLoader.load(), 1000, 700);
        }

        stage.setTitle("مطعم دهب");
        stage.setScene(scene);
        stage.setResizable(true);
        stage.getIcons().add(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/assets/Icon.png"))));
        stage.show();
    }

    public static void main(String[] args) throws Exception {
        launch();

    }

}