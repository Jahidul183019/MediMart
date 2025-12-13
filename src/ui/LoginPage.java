package ui;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import models.Inventory;
import services.UserService;
import services.MedicineService;
import utils.Session;

import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;

public class LoginPage {

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private TextField passwordTextField;
    @FXML private CheckBox showPasswordCheck;
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
        setupBackgroundImage();
        setupButtonsAndFields();
        setupShowPassword();
        setupKeyboardShortcutsAndFocus();
    }

    /* ========================
       UI Setup Helpers
       ======================== */

    private void setupShowPassword() {
        if (passwordField == null || passwordTextField == null || showPasswordCheck == null) return;

        // Keep same look (you already use styleClass "password-field" in FXML, so this is optional)
        passwordTextField.getStyleClass().add("password-field");

        // Sync both fields
        passwordTextField.textProperty().bindBidirectional(passwordField.textProperty());

        // Toggle visibility
        passwordTextField.visibleProperty().bind(showPasswordCheck.selectedProperty());
        passwordTextField.managedProperty().bind(showPasswordCheck.selectedProperty());

        passwordField.visibleProperty().bind(showPasswordCheck.selectedProperty().not());
        passwordField.managedProperty().bind(showPasswordCheck.selectedProperty().not());
    }

    private void setupBackgroundImage() {
        try {
            if (glassPane == null) {
                System.err.println("️ LoginPage: glassPane is null – check fx:id=\"glassPane\" in FXML and controller mapping.");
                return;
            }

            // 1) Try to load from classpath: resources/images/home_background.png
            var url = LoginPage.class.getResource("/resources/images/home_background.png");

            if (url != null) {
                glassPane.setStyle(
                        "-fx-background-image: url('" + url.toExternalForm() + "');" +
                                "-fx-background-size: cover;" +
                                "-fx-background-position: center center;"
                );
                System.out.println(" LoginPage: Background loaded from classpath /images/home_background.png");
            } else {
                System.err.println(" LoginPage: /images/home_background.png NOT found on classpath. Using fallback path.");

                // 2) Portable fallback (similar idea as SignupForm)
                String fallbackUrl = Paths
                        .get("src/resources/images/home_background.png")
                        .toUri()
                        .toString();

                glassPane.setStyle(
                        "-fx-background-image: url('" + fallbackUrl + "');" +
                                "-fx-background-size: cover;" +
                                "-fx-background-position: center center;"
                );
            }
        } catch (Exception ex) {
            System.err.println(" LoginPage: Failed to set background image");
            ex.printStackTrace();
        }
    }

    private void setupButtonsAndFields() {
        if (loginBtn != null) {
            loginBtn.getStyleClass().add("btn-login");
            loginBtn.setOnAction(e -> login());
        }

        if (signupBtn != null) {
            signupBtn.getStyleClass().add("btn-signup");
            signupBtn.setOnAction(e -> openSignup());
        }

        if (backBtn != null) {
            backBtn.setStyle(
                    "-fx-background-color: linear-gradient(to right, #f44336, #d32f2f);" +
                            "-fx-text-fill: white; -fx-font-weight: bold;" +
                            "-fx-background-radius: 20; -fx-padding: 8 18; -fx-cursor: hand;"
            );
            backBtn.setOnAction(e -> goBackToMain());
        }

        if (emailField != null)    emailField.getStyleClass().add("text-field");
        if (passwordField != null) passwordField.getStyleClass().add("password-field");
        if (messageLabel != null)  messageLabel.getStyleClass().add("message-label");
    }

    private void setupKeyboardShortcutsAndFocus() {
        Platform.runLater(() -> {
            // Pressing Enter in password field triggers login
            if (passwordField != null) {
                passwordField.setOnAction(e -> login());
            }

            // Global ESC handler to go back to main
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

                emailField.requestFocus();
            }
        });
    }

    private void escHandler(KeyEvent ev) {
        if (ev.getCode() == KeyCode.ESCAPE) {
            goBackToMain();
            ev.consume();
        }
    }

    /* ========================
       Login / Navigation Logic
       ======================== */

    /** Handle login (async to keep UI responsive) */
    private void login() {
        if (loggingIn) return; // debounce rapid clicks

        final String email = (emailField != null && emailField.getText() != null)
                ? emailField.getText().trim() : "";
        final String pass = (showPasswordCheck != null && showPasswordCheck.isSelected())
                ? (passwordTextField != null ? passwordTextField.getText().trim() : "")
                : (passwordField != null ? passwordField.getText().trim() : "");

        if (email.isEmpty() || pass.isEmpty()) {
            setMessage("Please fill in both fields!", "red");
            return;
        }

        // Fallbacks if not injected
        if (userService == null) {
            userService = new UserService();
            System.out.println("ℹ️ LoginPage: userService was null, created new UserService()");
        }
        if (inventory == null) {
            inventory = Inventory.getInstance();
            System.out.println("ℹ️ LoginPage: inventory was null, using Inventory.getInstance()");
        }

        setButtonsDisabled(true);
        loggingIn = true;

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

    /* ========================
       Utility Helpers
       ======================== */

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
