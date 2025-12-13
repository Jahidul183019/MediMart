package ui;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;
import models.Inventory;
import services.UserService;

import java.io.File;
import java.net.URL;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Main extends Application {

    private static final Logger LOG = Logger.getLogger(Main.class.getName());

    // shared services
    private final Inventory inventory = Inventory.getInstance();
    private final UserService userService = new UserService();

    @Override
    public void start(Stage stage) {
        try {
            Rectangle2D b = Screen.getPrimary().getVisualBounds();
            double prefW = Math.max(900, Math.min(1200, b.getWidth() * 0.78));
            double prefH = Math.max(620, Math.min(800, b.getHeight() * 0.82));

            Parent root;
            FXMLLoader loader;

            // 1) Try from jar/classpath
            URL url = Main.class.getResource("/fxml/main.fxml");
            if (url != null) {
                loader = new FXMLLoader(url);
                root = loader.load();
            } else {
                // 2) Dev fallback from source tree
                File fxmlFile = new File("src/resources/fxml/main.fxml");
                if (!fxmlFile.exists()) {
                    LOG.severe("FXML not found: " + fxmlFile.getAbsolutePath());
                    return;
                }
                loader = new FXMLLoader(fxmlFile.toURI().toURL());
                root = loader.load();
            }

            // 🔗 VERY IMPORTANT: wire the controller so buttons get actions
            MainController controller = Objects.requireNonNull(
                    loader.getController(),
                    "MainController missing in main.fxml"
            );
            controller.wire(stage, inventory, userService);

            Scene scene = new Scene(root, prefW, prefH);

            // optional stylesheet (classpath first, then src/)
            URL cssUrl = Main.class.getResource("/css/style.css");
            if (cssUrl != null) {
                scene.getStylesheets().add(cssUrl.toExternalForm());
            } else {
                File cssFile = new File("src/resources/css/style.css");
                if (cssFile.exists()) {
                    scene.getStylesheets().add(cssFile.toURI().toString());
                }
            }

            stage.setTitle("MediMart App");
            stage.setScene(scene);
            stage.setMinWidth(820);
            stage.setMinHeight(560);
            stage.setX(b.getMinX() + (b.getWidth() - prefW) / 2);
            stage.setY(b.getMinY() + (b.getHeight() - prefH) / 2);
            stage.show();

            stage.setOnCloseRequest(e -> Platform.exit());

        } catch (Exception ex) {
            LOG.log(Level.SEVERE, "Failed to start application", ex);
            Platform.exit();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
