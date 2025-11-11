package ui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Screen;
import javafx.stage.Stage;
import models.Inventory;
import services.UserService;

import java.io.File;
import java.net.URL;

public class Main extends Application {

    Inventory inventory = Inventory.getInstance();
    private final UserService userService = new UserService();

    // ---- Brand constants (keep in sync with AdminLogin) ----
    private static final String ACCENT_GRADIENT = """
        -fx-background-color: linear-gradient(to bottom right, #B2EBF2, #C8E6C9);
    """;
    private static final String GLASS_STYLE = """
        -fx-background-color: rgba(255,255,255,0.16);
        -fx-background-radius: 28;
        -fx-border-radius: 28;
        -fx-border-color: rgba(255,255,255,0.35);
        -fx-border-width: 2;
        -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.28), 22, 0.2, 0, 6);
    """;
    private static final String BTN_BASE = """
        -fx-background-radius: 28;
        -fx-font-size: 16px;
        -fx-font-weight: bold;
        -fx-padding: 12 26;
        -fx-text-fill: white;
        -fx-cursor: hand;
    """;
    private static final String BTN_PRIMARY = "-fx-background-color: #26A69A;"; // teal
    private static final String BTN_PRIMARY_HOVER = "-fx-background-color: #1F8F85;";
    private static final String BTN_SECONDARY = "-fx-background-color: #2196F3;"; // blue
    private static final String BTN_SECONDARY_HOVER = "-fx-background-color: #1E88E5;";

    @Override
    public void start(Stage stage) {
        // ===== Responsive sizing =====
        Rectangle2D b = Screen.getPrimary().getVisualBounds();
        double prefW = Math.max(900, Math.min(1200, b.getWidth() * 0.78));
        double prefH = Math.max(620, Math.min(800, b.getHeight() * 0.82));

        // ===== Logo (classpath first; fallback to file:) =====
        ImageView logo = new ImageView();
        try {
            URL logoUrl = getClass().getResource("/images/logo_medimart.png");
            if (logoUrl != null) {
                logo.setImage(new Image(logoUrl.toExternalForm(), true));
            } else {
                logo.setImage(new Image("file:src/resources/images/logo_medimart.png", true));
            }
        } catch (Exception ignored) {
            logo.setImage(new Image("file:src/resources/images/logo_medimart.png", true));
        }
        logo.setPreserveRatio(true);
        logo.setFitHeight(160);
        logo.setSmooth(true);
        logo.setEffect(new DropShadow(18, Color.rgb(0, 0, 0, 0.35)));

        VBox header = new VBox(8, logo);
        header.setAlignment(Pos.CENTER);

        // ===== Buttons (Admin / Customer) =====
        Button adminBtn    = new Button("Login as Admin");
        Button customerBtn = new Button("Login / Sign Up");

        adminBtn.setStyle(BTN_BASE + BTN_PRIMARY);
        customerBtn.setStyle(BTN_BASE + BTN_SECONDARY);

        // Hover effects
        adminBtn.setOnMouseEntered(e -> adminBtn.setStyle(BTN_BASE + BTN_PRIMARY_HOVER));
        adminBtn.setOnMouseExited (e -> adminBtn.setStyle(BTN_BASE + BTN_PRIMARY));
        customerBtn.setOnMouseEntered(e -> customerBtn.setStyle(BTN_BASE + BTN_SECONDARY_HOVER));
        customerBtn.setOnMouseExited (e -> customerBtn.setStyle(BTN_BASE + BTN_SECONDARY));

        // Icons
        ImageView adminIcon = new ImageView();
        ImageView customerIcon = new ImageView();
        try {
            adminIcon.setImage(new Image(getClass().getResource("/images/admin.png").toExternalForm()));
        } catch (Exception ignored) { adminIcon.setImage(new Image("file:src/resources/images/admin.png", true)); }
        try {
            customerIcon.setImage(new Image(getClass().getResource("/images/customer.png").toExternalForm()));
        } catch (Exception ignored) { customerIcon.setImage(new Image("file:src/resources/images/customer.png", true)); }
        adminIcon.setFitHeight(46); adminIcon.setFitWidth(46); adminIcon.setPreserveRatio(true);
        customerIcon.setFitHeight(46); customerIcon.setFitWidth(46); customerIcon.setPreserveRatio(true);

        VBox adminBox = new VBox(10, adminIcon, adminBtn);     adminBox.setAlignment(Pos.CENTER);
        VBox customerBox = new VBox(10, customerIcon, customerBtn); customerBox.setAlignment(Pos.CENTER);

        HBox buttonsRow = new HBox(48, adminBox, customerBox);
        buttonsRow.setAlignment(Pos.CENTER);

        VBox actions = new VBox(28, buttonsRow);
        actions.setAlignment(Pos.CENTER);

        // ===== Glassmorphism panel =====
        VBox glassPane = new VBox(24, header, actions);
        glassPane.setAlignment(Pos.CENTER);
        glassPane.setPadding(new Insets(36));
        glassPane.setMaxWidth(780);
        glassPane.setStyle(GLASS_STYLE);
        // lift panel a bit for nicer composition
        StackPane.setMargin(glassPane, new Insets(-20, 0, 0, 0));

        // ===== Root with brand gradient + (optional) background image blend =====
        StackPane root = new StackPane();
        root.setPadding(new Insets(26));

        // If you want to blend image with brand gradient, keep the image; else comment out block to show gradient only
        String bgCss;
        try {
            URL bgUrl = getClass().getResource("/images/home_background.png");
            if (bgUrl != null) {
                bgCss = """
                    -fx-background-image: url('%s');
                    -fx-background-size: cover;
                    -fx-background-position: center center;
                    -fx-background-repeat: no-repeat;
                """.formatted(bgUrl.toExternalForm()) + ACCENT_GRADIENT;
            } else {
                bgCss = ACCENT_GRADIENT;
            }
        } catch (Exception ex) {
            bgCss = ACCENT_GRADIENT;
        }

        root.setStyle(bgCss);
        root.getChildren().add(glassPane);

        // ===== Button actions =====
        adminBtn.setOnAction(e -> new AdminLogin(inventory).show(stage));
        customerBtn.setOnAction(e -> openCustomerLogin(stage));

        // ===== Scene =====
        Scene scene = new Scene(root, prefW, prefH);

        File cssFile = new File("src/resources/css/style.css");
        if (cssFile.exists()) scene.getStylesheets().add(cssFile.toURI().toString());

        stage.setScene(scene);
        stage.setTitle("MediMart App");
        stage.setMinWidth(820);
        stage.setMinHeight(560);
        stage.setX(b.getMinX() + (b.getWidth() - prefW) / 2);
        stage.setY(b.getMinY() + (b.getHeight() - prefH) / 2);
        stage.show();
    }

    private void openCustomerLogin(Stage stage) {
        try {
            URL fxmlUrl = getClass().getResource("/fxml/LoginSignup.fxml");
            Pane root;
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
