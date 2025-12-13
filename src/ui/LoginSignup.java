package ui;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import models.Inventory;
import services.UserService;
import services.MedicineService;
import models.User;
import utils.Session;

import java.util.concurrent.*;

public class LoginSignup {

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private TextField passwordTextField;
    @FXML private CheckBox showPasswordCheck;
    @FXML private Button loginBtn;
    @FXML private Button signupBtn;
    @FXML private Button backBtn;
    @FXML private Label messageLabel;

    private Inventory inventory;
    private UserService userService;

    // Reusable single-thread executor (avoids new thread startup cost every login)
    private static final ExecutorService EXECUTOR =
            Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "LoginSignup-Worker");
                t.setDaemon(true);
                return t;
            });

    // ---- Brand constants ----
    private static final String BTN_BASE = """
        -fx-background-radius: 10;
        -fx-font-weight: bold;
        -fx-padding: 8 18;
        -fx-cursor: hand;
        -fx-text-fill: white;
    """;
    private static final String BTN_PRIMARY = "-fx-background-color: #26A69A;";
    private static final String BTN_PRIMARY_HOVER = "-fx-background-color: #1F8F85;";
    private static final String BTN_SECONDARY = "-fx-background-color: #2196F3;";
    private static final String BTN_SECONDARY_HOVER = "-fx-background-color: #1E88E5;";
    private static final String BTN_DANGER = "-fx-background-color: #f44336;";
    private static final String BTN_DANGER_HOVER = "-fx-background-color: #e53935;";

    private static final String INPUT_BASE = """
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

    // Performance guards
    private boolean escFilterAdded = false;
    private boolean polished = false;
    private volatile boolean loggingIn = false;

    public void setServices(Inventory inventory, UserService userService) {
        this.inventory = inventory;
        this.userService = userService;
    }

    @FXML
    private void initialize() {
        assert loginBtn != null : "Login button is not properly initialized!";
        assert signupBtn != null : "Signup button is not properly initialized!";
        assert messageLabel != null : "Message label is not properly initialized!";

        loginBtn.setOnAction(e -> login());
        signupBtn.setOnAction(e -> openSignup());
        setupShowPassword();

        if (backBtn != null) backBtn.setOnAction(e -> goBackToMain());
        else Platform.runLater(this::ensureBackButtonExists);

        if (!polished) { applyPolish(); polished = true; }

        Platform.runLater(() -> {
            if (passwordField != null) passwordField.setOnAction(e -> login());
            setupEscHandler();
            if (emailField != null) emailField.requestFocus();
        });
    }
    private void setupShowPassword() {
        if (passwordField == null || passwordTextField == null || showPasswordCheck == null) return;

        // same style as password field
        styleInput(passwordTextField);

        // sync values
        passwordTextField.textProperty().bindBidirectional(passwordField.textProperty());

        // toggle visibility
        passwordTextField.visibleProperty().bind(showPasswordCheck.selectedProperty());
        passwordTextField.managedProperty().bind(showPasswordCheck.selectedProperty());

        passwordField.visibleProperty().bind(showPasswordCheck.selectedProperty().not());
        passwordField.managedProperty().bind(showPasswordCheck.selectedProperty().not());
    }
    private void setupEscHandler() {
        if (emailField == null) return;
        if (!escFilterAdded && emailField.getScene() != null) {
            emailField.getScene().addEventFilter(KeyEvent.KEY_PRESSED, this::escHandler);
            escFilterAdded = true;
        }
        emailField.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (!escFilterAdded && newScene != null) {
                newScene.addEventFilter(KeyEvent.KEY_PRESSED, this::escHandler);
                escFilterAdded = true;
            }
        });
    }

    private void escHandler(KeyEvent ev) {
        if (ev.getCode() == KeyCode.ESCAPE) {
            goBackToMain();
            ev.consume();
        }
    }

    private void ensureBackButtonExists() {
        try {
            Parent node = emailField;
            VBox vbox = null;
            while (node != null) {
                if (node instanceof VBox) { vbox = (VBox) node; break; }
                node = node.getParent();
            }
            if (vbox == null) return;

            if (vbox.getChildren().isEmpty() || !(vbox.getChildren().get(0) instanceof Button)) {
                Button b = new Button("← Back");
                b.setOnAction(e -> goBackToMain());
                b.setStyle(BTN_BASE + BTN_DANGER);
                b.setOnMouseEntered(e -> b.setStyle(BTN_BASE + BTN_DANGER_HOVER));
                b.setOnMouseExited(e -> b.setStyle(BTN_BASE + BTN_DANGER));
                vbox.getChildren().add(0, b);
            }
        } catch (Exception ignored) {}
    }

    // -------- Optimized async login --------
    private void login() {
        if (loggingIn) return;

        final String email = emailField.getText() != null ? emailField.getText().trim() : "";
        final String password = (showPasswordCheck != null && showPasswordCheck.isSelected()
                ? passwordTextField.getText()
                : passwordField.getText());

        final String passwordTrim = password != null ? password.trim() : "";


        if (email.isEmpty() || password.isEmpty()) {
            setMessage("Please enter email and password!", false);
            return;
        }

        if (userService == null) userService = new UserService();

        loggingIn = true;
        setButtonsDisabled(true);
        setMessage("Logging in...", "#555");

        CompletableFuture
                .supplyAsync(() -> userService.login(email, password), EXECUTOR)
                .whenCompleteAsync((user, ex) -> {
                    Platform.runLater(() -> {
                        try {
                            if (ex != null) {
                                setMessage("Error: " + ex.getMessage(), false);
                                ex.printStackTrace();
                                return;
                            }
                            if (user != null) {
                                Session.setCurrentUser(user);
                                setMessage("Login successful!", true);

                                Stage currentStage = (Stage) loginBtn.getScene().getWindow();
                                new CustomerDashboard(
                                        inventory != null ? inventory : Inventory.getInstance(),
                                        new MedicineService()
                                ).show(currentStage);
                            } else {
                                setMessage("Invalid email or password!", false);
                            }
                        } finally {
                            loggingIn = false;
                            setButtonsDisabled(false);
                        }
                    });
                }, Platform::runLater);
    }

    private void openSignup() {
        try {
            SignupForm signupForm = new SignupForm();
            Stage stage = (Stage) signupBtn.getScene().getWindow();
            signupForm.setServices(
                    inventory != null ? inventory : Inventory.getInstance(),
                    userService != null ? userService : new UserService(),
                    stage
            );
            Pane root = signupForm.getRoot();
            stage.getScene().setRoot(root);
        } catch (Exception ex) {
            ex.printStackTrace();
            setMessage("Unable to open signup form!", false);
        }
    }

    private void goBackToMain() {
        try {
            Stage stage = (Stage) loginBtn.getScene().getWindow();
            new Main().start(stage);
        } catch (Exception ex) {
            ex.printStackTrace();
            setMessage("Failed to return to main page!", false);
        }
    }

    private void setMessage(String text, boolean ok) {
        if (messageLabel != null) {
            messageLabel.setText(text);
            messageLabel.setStyle(ok
                    ? "-fx-text-fill: #2e7d32; -fx-font-weight: bold;"
                    : "-fx-text-fill: #c62828; -fx-font-weight: bold;");
        }
    }

    private void setMessage(String text, String color) {
        if (messageLabel != null) {
            messageLabel.setText(text);
            messageLabel.setStyle("-fx-text-fill: " + color + "; -fx-font-weight: bold;");
        }
    }

    // ---------- UI polish ----------
    private void applyPolish() {
        if (emailField != null) styleInput(emailField);
        if (passwordField != null) styleInput(passwordField);

        if (loginBtn != null) {
            loginBtn.setStyle(BTN_BASE + BTN_PRIMARY);
            loginBtn.setOnMouseEntered(e -> loginBtn.setStyle(BTN_BASE + BTN_PRIMARY_HOVER));
            loginBtn.setOnMouseExited(e -> loginBtn.setStyle(BTN_BASE + BTN_PRIMARY));
        }
        if (signupBtn != null) {
            signupBtn.setStyle(BTN_BASE + BTN_SECONDARY);
            signupBtn.setOnMouseEntered(e -> signupBtn.setStyle(BTN_BASE + BTN_SECONDARY_HOVER));
            signupBtn.setOnMouseExited(e -> signupBtn.setStyle(BTN_BASE + BTN_SECONDARY));
        }
        if (backBtn != null) {
            backBtn.setStyle(BTN_BASE + BTN_DANGER);
            backBtn.setOnMouseEntered(e -> backBtn.setStyle(BTN_BASE + BTN_DANGER_HOVER));
            backBtn.setOnMouseExited(e -> backBtn.setStyle(BTN_BASE + BTN_DANGER));
        }

        try {
            if (emailField != null && emailField.getParent() instanceof VBox v1 && v1.getChildren().get(0) instanceof Label l1) {
                l1.setStyle("-fx-text-fill: rgba(0,0,0,0.72); -fx-font-size: 12.5px;");
            }
            if (passwordField != null && passwordField.getParent() instanceof VBox v2 && v2.getChildren().get(0) instanceof Label l2) {
                l2.setStyle("-fx-text-fill: rgba(0,0,0,0.72); -fx-font-size: 12.5px;");
            }
        } catch (Exception ignored) {}
    }

    private void styleInput(TextField tf) {
        tf.setStyle(INPUT_BASE);
        tf.focusedProperty().addListener((obs, o, focused) -> {
            if (focused) {
                tf.setStyle(INPUT_BASE + """
                    -fx-border-color: #26A69A;
                    -fx-effect: dropshadow(gaussian, rgba(38,166,154,0.35), 12, 0.35, 0, 0);
                """);
            } else {
                tf.setStyle(INPUT_BASE + "-fx-effect: null;");
            }
        });
    }

    private void setButtonsDisabled(boolean disabled) {
        if (loginBtn != null)  loginBtn.setDisable(disabled);
        if (signupBtn != null) signupBtn.setDisable(disabled);
        if (backBtn != null)   backBtn.setDisable(disabled);
        // keep text fields enabled for better UX
    }
}
