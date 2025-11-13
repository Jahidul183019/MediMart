package ui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;
import models.Inventory;
import services.UserService;

import java.io.File;
import java.net.URL;

public class Main extends Application {
    private final Inventory inventory = Inventory.getInstance();
    private final UserService userService = new UserService();

    @Override
    public void start(Stage stage) throws Exception {
        // Try classpath first
        URL cpUrl = Main.class.getResource("/fxml/main.fxml");

        Parent root;
        FXMLLoader loader;

        if (cpUrl != null) {
            loader = new FXMLLoader(cpUrl);
            root = loader.load();
        } else {
            // Fallback to source tree during development
            File fxmlFile = new File("src/resources/fxml/main.fxml"); // <- adjust if your tree differs
            if (!fxmlFile.exists()) {
                System.err.println("[Main] FXML not found at classpath:/fxml/main.fxml");
                System.err.println("[Main] Also missing on disk: " + fxmlFile.getAbsolutePath());
                throw new IllegalStateException("main.fxml not found. Check resource paths.");
            }
            loader = new FXMLLoader(fxmlFile.toURI().toURL());
            root = loader.load();
            System.out.println("[Main] Loaded FXML from file: " + fxmlFile.getAbsolutePath());
        }

        MainController controller = loader.getController();
        controller.wire(stage, Inventory.getInstance(), new UserService());

        double w = Math.max(1000, Math.min(1280, Screen.getPrimary().getBounds().getWidth() * 0.84));
        double h = Math.max(680, Math.min(860, Screen.getPrimary().getBounds().getHeight() * 0.86));
        Scene scene = new Scene(root, w, h);

        stage.setScene(scene);
        stage.setTitle("MediMart");
        stage.show();
    }


    // ✅ This is your original openCustomerLogin(...) method merged here directly
    public void openCustomerLogin(Stage stage) {
        try {
            URL fxmlUrl = getClass().getResource("/fxml/LoginSignup.fxml");
            javafx.scene.layout.Pane root;
            FXMLLoader loader;
            if (fxmlUrl != null) {
                loader = new FXMLLoader(fxmlUrl);
                root = loader.load();
            } else {
                File fxmlFile = new File("src/resources/fxml/LoginSignup.fxml");
                if (!fxmlFile.exists()) {
                    System.err.println("FXML not found! Check path: " + fxmlFile.getAbsolutePath());
                    return;
                }
                loader = new FXMLLoader(fxmlFile.toURI().toURL());
                root = loader.load();
            }

            LoginSignup controller = loader.getController();
            controller.setServices(inventory, userService);

            if (stage.getScene() == null) {
                Scene scene = new Scene(root, 500, 600);
                File cssFile = new File("src/resources/css/style.css");
                if (cssFile.exists()) scene.getStylesheets().add(cssFile.toURI().toString());
                stage.setScene(scene);
            } else {
                stage.getScene().setRoot(root);
            }

            stage.setTitle("Login / Sign Up - MediMart");
            stage.show();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
