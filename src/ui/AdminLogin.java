package ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Screen;
import javafx.stage.Stage;
import models.Inventory;

import java.io.File;
import java.net.URL;

public class AdminLogin {
    private final Inventory inventory;

    // ---- Brand constants (same as Main) ----
    private static final String ACCENT_GRADIENT = """
        -fx-background-color: linear-gradient(to bottom right, #B2EBF2, #C8E6C9);
    """;
    private static final String BTN_BASE = """
        -fx-background-radius: 10;
        -fx-font-weight: bold;
        -fx-padding: 8 18;
        -fx-cursor: hand;
    """;
    private static final String BTN_PRIMARY = "-fx-background-color: #26A69A; -fx-text-fill: white;";
    private static final String BTN_DANGER  = "-fx-background-color: #f44336; -fx-text-fill: white;";

    public AdminLogin(Inventory inventory) {
        this.inventory = inventory;
    }

    public void show(Stage stage) {
        // ===== Responsive sizing (laptop friendly) =====
        Rectangle2D screen = Screen.getPrimary().getVisualBounds();
        double prefW = Math.max(640, Math.min(880, screen.getWidth() * 0.55));
        double prefH = Math.max(520, Math.min(700, screen.getHeight() * 0.60));

        // ===== Title =====
        Label title = new Label("Admin Login");
        title.setFont(Font.font("Sans-serif", 28));
        title.setStyle("-fx-font-weight: bold; -fx-text-fill: #1f6f64;");

        // ===== Fields (polished) =====
        TextField emailField = new TextField();
        emailField.setPromptText("Email");
        styleInput(emailField);

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Password");
        styleInput(passwordField);

        CheckBox showPw = new CheckBox("Show password");
        showPw.setStyle("-fx-text-fill: rgba(0,0,0,0.72);");
        TextField passwordMirror = new TextField();
        passwordMirror.managedProperty().bind(showPw.selectedProperty());
        passwordMirror.visibleProperty().bind(showPw.selectedProperty());
        passwordField.managedProperty().bind(showPw.selectedProperty().not());
        passwordField.visibleProperty().bind(showPw.selectedProperty().not());
        passwordMirror.textProperty().bindBidirectional(passwordField.textProperty());
        styleInput(passwordMirror);

        // ===== Buttons (brand colors) =====
        Button loginBtn = new Button("Login");
        Button backBtn  = new Button("Back");
        loginBtn.setStyle(BTN_BASE + BTN_PRIMARY);
        backBtn.setStyle(BTN_BASE + BTN_DANGER);

        HBox btnBox = new HBox(14, loginBtn, backBtn);
        btnBox.setAlignment(Pos.CENTER);

        // ===== Feedback =====
        Label message = new Label();
        message.setTextFill(Color.RED);
        message.setFont(Font.font(14));

        // ===== Form =====
        VBox fields = new VBox(12,
                new VBox(6, new Label("Email"), emailField),
                new VBox(6, new Label("Password"), passwordField, passwordMirror, showPw)
        );
        fields.setFillWidth(true);
        fields.lookupAll(".label").forEach(n -> n.setStyle("-fx-text-fill: rgba(0,0,0,0.72); -fx-font-size: 12.5px;"));

        VBox formBox = new VBox(18, title, fields, btnBox, message);
        formBox.setAlignment(Pos.CENTER);
        formBox.setPadding(new Insets(28));
        formBox.setMaxWidth(460);

        // ===== Card with background image INSIDE =====
        StackPane card = new StackPane();
        card.setMaxWidth(520);
        card.setPadding(new Insets(10));
        card.setStyle("""
            -fx-background-radius: 20;
            -fx-border-color: rgba(255,255,255,0.55);
            -fx-border-width: 1.2;
            -fx-border-radius: 20;
            -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.18), 16, 0.25, 0, 4);
        """);

        URL cardBgUrl = getClass().getResource("/images/login_background.png");
        if (cardBgUrl == null) {
            File f = new File("/Users/md.jahidulislam/Documents/Pre/MediMart/src/resources/images/login_background.png");
            if (f.exists()) {
                try { cardBgUrl = f.toURI().toURL(); } catch (Exception ignored) {}
            }
        }
        if (cardBgUrl != null) {
            Image bgImg = new Image(cardBgUrl.toExternalForm(), 900, 700, true, true);
            BackgroundImage bg = new BackgroundImage(
                    bgImg,
                    BackgroundRepeat.NO_REPEAT,
                    BackgroundRepeat.NO_REPEAT,
                    BackgroundPosition.CENTER,
                    new BackgroundSize(100, 100, true, true, true, false)
            );
            card.setBackground(new Background(bg));
        } else {
            // If missing, still brand it with the accent gradient
            card.setStyle(card.getStyle() + "-fx-background-color: linear-gradient(to bottom right, #B2EBF2, #C8E6C9);");
        }

        // Frosted overlay for readability
        formBox.setStyle("""
            -fx-background-color: rgba(255,255,255,0.88);
            -fx-background-radius: 16;
            -fx-border-radius: 16;
        """);
        card.getChildren().add(formBox);

        // ===== Root background uses the SAME brand gradient =====
        StackPane root = new StackPane(card);
        root.setPadding(new Insets(28));
        root.setStyle(ACCENT_GRADIENT);

        // ===== Actions =====
        loginBtn.setOnAction(e -> {
            String email = safe(emailField.getText());
            String pass  = safe(passwordField.getText());
            if (email.equals("admin@medimart.com") && pass.equals("admin123")) {
                message.setTextFill(Color.web("#2e7d32"));
                message.setText("Login successful!");
                new AdminDashboard(inventory).show(stage);
            } else {
                message.setTextFill(Color.web("#c62828"));
                message.setText("Invalid email or password!");
            }
        });

        backBtn.setOnAction(e -> {
            try { new Main().start(stage); } catch (Exception ex) { ex.printStackTrace(); }
        });

        // Keyboard shortcuts
        root.setOnKeyPressed(e -> {
            switch (e.getCode()) {
                case ENTER -> loginBtn.fire();
                case ESCAPE -> backBtn.fire();
            }
        });

        // ===== Scene & Stage =====
        Scene scene = new Scene(root, prefW, prefH);
        stage.setScene(scene);
        stage.setTitle("Admin Login - MediMart");
        stage.setMinWidth(600);
        stage.setMinHeight(500);
        stage.setX(screen.getMinX() + (screen.getWidth() - prefW) / 2);
        stage.setY(screen.getMinY() + (screen.getHeight() - prefH) / 2);
        stage.show();

        emailField.requestFocus();
    }

    // ===== Helpers =====
    private static String safe(String s) { return s == null ? "" : s.trim(); }

    /** Polished text field styling to match app */
    private static void styleInput(TextField tf) {
        final String base = """
            -fx-background-color: rgba(255,255,255,0.96);
            -fx-background-insets: 0;
            -fx-background-radius: 10;
            -fx-border-color: #B2DFDB;
            -fx-border-radius: 10;
            -fx-border-width: 1.2;
            -fx-padding: 10 12;
            -fx-font-size: 13px;
            -fx-prompt-text-fill: rgba(0,0,0,0.45);
        """;
        tf.setStyle(base);
        tf.focusedProperty().addListener((obs, o, f) -> {
            if (f) {
                tf.setStyle(base + """
                    -fx-border-color: #26A69A;
                    -fx-effect: dropshadow(gaussian, rgba(38,166,154,0.35), 12, 0.35, 0, 0);
                """);
            } else {
                tf.setStyle(base + "-fx-effect: null;");
            }
        });
    }
}
