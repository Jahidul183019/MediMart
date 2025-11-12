package ui;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
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

    private final Inventory inventory = Inventory.getInstance();
    private final UserService userService = new UserService();

    @Override
    public void start(Stage stage) {
        Thread.setDefaultUncaughtExceptionHandler((t, e) ->
                LOG.log(Level.SEVERE, "Uncaught exception on thread " + t.getName(), e));

        try {
            Parent root;
            FXMLLoader loader;

            // Prefer classpath resource
            URL cp = Main.class.getResource("/fxml/main.fxml");
            if (cp != null) {
                loader = new FXMLLoader(cp);
                root = loader.load();
            } else {
                // Dev fallback
                File f = new File("src/resources/fxml/main.fxml");
                if (!f.exists()) throw new IllegalStateException("main.fxml not found (classpath or src/resources/fxml).");
                loader = new FXMLLoader(f.toURI().toURL());
                root = loader.load();
            }

            MainController controller = Objects.requireNonNull(loader.getController(), "MainController missing in FXML");
            controller.wire(stage, inventory, userService);

            double sw = Screen.getPrimary().getBounds().getWidth();
            double sh = Screen.getPrimary().getBounds().getHeight();
            double w = clamp(sw * 0.84, 1000, 1280);
            double h = clamp(sh * 0.86,  680,  860);

            Scene scene = new Scene(root, w, h);

            // Optional stylesheet
            URL css = Main.class.getResource("/css/style.css");
            if (css != null) scene.getStylesheets().add(css.toExternalForm());
            else {
                File cssFile = new File("src/resources/css/style.css");
                if (cssFile.exists()) scene.getStylesheets().add(cssFile.toURI().toString());
            }

            // Optional icon
            URL iconUrl = Main.class.getResource("/images/logo_medimart.png");
            if (iconUrl == null) {
                File iconFile = new File("src/resources/images/logo_medimart.png");
                if (iconFile.exists()) iconUrl = iconFile.toURI().toURL();
            }
            if (iconUrl != null) stage.getIcons().add(new Image(iconUrl.toExternalForm()));

            stage.setMinWidth(960);
            stage.setMinHeight(640);
            stage.setTitle("MediMart");
            stage.setScene(scene);
            stage.show();

            stage.setOnCloseRequest(e -> Platform.exit());

        } catch (Exception ex) {
            LOG.log(Level.SEVERE, "Failed to start application", ex);
            Platform.exit();
        }
    }

    // Public helper (used by controller navigation)
    public void openCustomerLogin(Stage stage) {
        try {
            FXMLLoader loader;
            Parent root;

            URL fxmlUrl = Main.class.getResource("/fxml/LoginSignup.fxml");
            if (fxmlUrl != null) {
                loader = new FXMLLoader(fxmlUrl);
                root = loader.load();
            } else {
                File fxmlFile = new File("src/resources/fxml/LoginSignup.fxml");
                if (!fxmlFile.exists()) {
                    LOG.severe("FXML not found: " + fxmlFile.getAbsolutePath());
                    return;
                }
                loader = new FXMLLoader(fxmlFile.toURI().toURL());
                root = loader.load();
            }

            LoginSignup controller = loader.getController();
            if (controller != null) controller.setServices(inventory, userService);

            if (stage.getScene() == null) {
                Scene scene = new Scene(root, 500, 600);
                URL css = Main.class.getResource("/css/style.css");
                if (css != null) scene.getStylesheets().add(css.toExternalForm());
                else {
                    File cssFile = new File("src/resources/css/style.css");
                    if (cssFile.exists()) scene.getStylesheets().add(cssFile.toURI().toString());
                }
                stage.setScene(scene);
            } else {
                stage.getScene().setRoot(root);
            }

            stage.setTitle("Login / Sign Up - MediMart");
            stage.show();
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, "openCustomerLogin() failed", ex);
        }
    }

    private static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    public static void main(String[] args) {
        launch(args);
    }
}
