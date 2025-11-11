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
import models.User;
import models.Inventory;
import services.UserService;
import services.MedicineService;
import utils.Session; // <-- IMPORTANT

import java.util.concurrent.CompletableFuture;

public class LoginPage {

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Button loginBtn;
    @FXML private Button signupBtn;
    @FXML private Button backBtn;
    @FXML private Label messageLabel;
    @FXML private Pane glassPane;

    private Inventory inventory;
    private UserService userService;

    // Performance / UX guards
    private boolean escFilterAdded = false;
    private volatile boolean loggingIn = false;

    // Inject Inventory and UserService after loading FXML
    public void setServices(Inventory inventory, UserService userService) {
        this.inventory = inventory;
        this.userService = userService;
    }

    @FXML
    private void initialize() {
        // Background image (classpath first, fallback to file:)
        try {
            var url = getClass().getResource("/images/home_background.png");
            if (glassPane != null) {
                if (url != null) {
                    glassPane.setStyle("""
                        -fx-background-image: url('%s');
                        -fx-background-size: cover;
                        -fx-background-position: center center;
                    """.formatted(url.toExternalForm()));
                } else {
                    glassPane.setStyle("""
                        -fx-background-image: url('file:src/resources/images/home_background.png');
                        -fx-background-size: cover;
                        -fx-background-position: center center;
                    """);
                }
            }
        } catch (Exception ignored) {}

        if (loginBtn != null) {
            loginBtn.getStyleClass().add("btn-login");
            loginBtn.setOnAction(e -> login());
        }

        if (signupBtn != null) {
            signupBtn.getStyleClass().add("btn-signup");
            signupBtn.setOnAction(e -> openSignup());
        }

        if (backBtn != null) {
            backBtn.setStyle("-fx-background-color: linear-gradient(to right, #f44336, #d32f2f);"
                    + "-fx-text-fill: white; -fx-font-weight: bold;"
                    + "-fx-background-radius: 20; -fx-padding: 8 18; -fx-cursor: hand;");
            backBtn.setOnAction(e -> goBackToMain());
        }

        if (emailField != null) emailField.getStyleClass().add("text-field");
        if (passwordField != null) passwordField.getStyleClass().add("password-field");
        if (messageLabel != null) messageLabel.getStyleClass().add("message-label");

        // Keyboard shortcuts / focus; attach ESC filter once even if Scene appears later
        Platform.runLater(() -> {
            if (passwordField != null) passwordField.setOnAction(e -> login()); // Enter triggers login

            if (emailField != null) {
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

            if (emailField != null) emailField.requestFocus();
        });
    }

    private void escHandler(KeyEvent ev) {
        if (ev.getCode() == KeyCode.ESCAPE) {
            goBackToMain();
            ev.consume();
        }
    }

    /** Handle login (async to keep UI responsive) */
    private void login() {
        if (loggingIn) return; // debounce rapid clicks

        final String email = (emailField != null && emailField.getText() != null) ? emailField.getText().trim() : "";
        final String pass  = (passwordField != null && passwordField.getText() != null) ? passwordField.getText().trim() : "";

        if (email.isEmpty() || pass.isEmpty()) {
            setMessage("Please fill in both fields!", "red");
            return;
        }

        // Fallbacks if not injected
        if (userService == null) userService = new UserService();
        if (inventory == null) inventory = Inventory.getInstance();

        setButtonsDisabled(true);
        loggingIn = true;

        // Do DB work off the FX thread
        CompletableFuture
                .supplyAsync(() -> userService.login(email, pass))
                .thenAccept(user -> Platform.runLater(() -> {
                    try {
                        if (user != null) {
                            Session.setCurrentUser(user); // important for ProfileView etc.
                            setMessage("Login successful!", "green");

                            // Navigate to CustomerDashboard (reuse same Stage)
                            Stage stage = (Stage) loginBtn.getScene().getWindow();
                            new CustomerDashboard(inventory, new MedicineService()).show(stage);
                        } else {
                            setMessage("Invalid email or password!", "red");
                        }
                    } finally {
                        // re-enable only if we stayed on this screen (safe even if scene changed)
                        setButtonsDisabled(false);
                        loggingIn = false;
                    }
                }));
    }

    /** Open signup form */
    private void openSignup() {
        try {
            if (inventory == null) inventory = Inventory.getInstance();
            if (userService == null) userService = new UserService();

            SignupForm signupForm = new SignupForm();
            Stage stage = (Stage) signupBtn.getScene().getWindow();
            signupForm.setServices(inventory, userService, stage);
            Pane root = signupForm.getRoot();
            stage.getScene().setRoot(root);
        } catch (Exception ex) {
            ex.printStackTrace();
            setMessage("Unable to open signup form!", "red");
        }
    }

    /** Back to main */
    private void goBackToMain() {
        try {
            Stage stage = (Stage) backBtn.getScene().getWindow();
            new Main().start(stage);
        } catch (Exception ex) {
            ex.printStackTrace();
            setMessage("Failed to return to main page!", "red");
        }
    }

    /** Status text helper */
    private void setMessage(String text, String color) {
        if (messageLabel != null) {
            messageLabel.setText(text);
            messageLabel.setStyle("-fx-text-fill: " + color + "; -fx-font-weight: bold;");
        }
    }

    private void setButtonsDisabled(boolean disabled) {
        if (loginBtn != null)  loginBtn.setDisable(disabled);
        if (signupBtn != null) signupBtn.setDisable(disabled);
        if (backBtn != null)   backBtn.setDisable(disabled);
        // keep inputs enabled so users can correct typos while login runs
    }
}
