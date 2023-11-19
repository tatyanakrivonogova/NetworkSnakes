package app;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import lab4.gui.controller.FXMLController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;

public class App extends Application {
    Logger logger = LoggerFactory.getLogger(App.class);
    FXMLController controller;
    @Override
    public void start(Stage stage) throws Exception {
        logger.info("Application start");
        controller = new FXMLController();

        FXMLLoader loader = new FXMLLoader();
        loader.setController(controller);
        URL xmlUrl = getClass().getClassLoader().getResource("layout.fxml");
        loader.setLocation(xmlUrl);

        Parent root = loader.load();
        Scene scene = new Scene(root);

        controller.start();

        stage.setScene(scene);
        stage.setTitle("Snakes");
        stage.show();
        stage.setOnCloseRequest((ignored) -> {
            stop();
            Platform.exit();
        });
    }

    @Override
    public void stop() {
        controller.stop();
    }
}