package ui;

import javafx.application.Platform;  // Import Platform class for thread safety
import javafx.fxml.FXMLLoader;       // Import FXMLLoader
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import models.Inventory;
import services.UserService;
import java.io.IOException;          // For handling I/O exceptions

public class SignupForm {

    private Inventory inventory;
    private UserService userService;
    private Stage stage; // Store the stage passed to this form

    // Inject services and stage
    public void setServices(Inventory inventory, UserService userService, Stage stage) {
        this.inventory = inventory;
        this.userService = userService;
        this.stage = stage; // Store the current stage (LoginSignup stage)
    }

    // Create the Signup Form UI elements and return the root Pane
    public VBox getRoot() {
        // ==== Inline brand styles (visual only) ====
        final String inputStyle = """
            -fx-background-color: rgba(255,255,255,0.96);
            -fx-background-radius: 12;
            -fx-border-radius: 12;
            -fx-border-color: rgba(0,0,0,0.12);
            -fx-border-width: 1;
            -fx-font-size: 14px;
            -fx-text-fill: #2b2b2b;
            -fx-prompt-text-fill: rgba(0,0,0,0.45);
            -fx-padding: 10 14;
        """;

        final String titleStyle = """
            -fx-font-size: 30px;
            -fx-font-weight: 800;
            -fx-text-fill: #2f2a1f;
        """;

        final String subtitleStyle = """
            -fx-font-size: 13px;
            -fx-text-fill: rgba(0,0,0,0.65);
        """;

        final String primaryBtn = """
            -fx-background-color: linear-gradient(#ffbb00, #d99a00);
            -fx-text-fill: #1e1a12;
            -fx-font-weight: bold;
            -fx-font-size: 14px;
            -fx-background-radius: 14;
            -fx-padding: 12 28;
            -fx-cursor: hand;
        """;

        final String primaryBtnHover = """
            -fx-background-color: linear-gradient(#ffd24d, #e2a516);
            -fx-text-fill: #1e1a12;
            -fx-font-weight: bold;
            -fx-font-size: 14px;
            -fx-background-radius: 14;
            -fx-padding: 12 28;
            -fx-cursor: hand;
        """;

        final String ghostBtn = """
            -fx-background-color: rgba(255,255,255,0.15);
            -fx-text-fill: #1f1f1f;
            -fx-font-weight: bold;
            -fx-font-size: 14px;
            -fx-border-color: rgba(0,0,0,0.18);
            -fx-border-width: 1;
            -fx-background-radius: 14;
            -fx-border-radius: 14;
            -fx-padding: 12 22;
            -fx-cursor: hand;
        """;

        final String ghostBtnHover = """
            -fx-background-color: rgba(255,255,255,0.28);
            -fx-text-fill: #111;
            -fx-font-weight: bold;
            -fx-font-size: 14px;
            -fx-border-color: rgba(0,0,0,0.28);
            -fx-border-width: 1;
            -fx-background-radius: 14;
            -fx-border-radius: 14;
            -fx-padding: 12 22;
            -fx-cursor: hand;
        """;

        // ==== Components ====
        Label titleLabel = new Label("Sign Up for MediMart");
        titleLabel.setStyle(titleStyle);

        Label subtitle = new Label("Create your account to get started");
        subtitle.setStyle(subtitleStyle);

        TextField emailField = new TextField();
        emailField.setPromptText("Email");
        emailField.setStyle(inputStyle);

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Password");
        passwordField.setStyle(inputStyle);

        TextField passwordTextField = new TextField();
        passwordTextField.setPromptText("Password");
        passwordTextField.setStyle(inputStyle);
        passwordTextField.setVisible(false);
        passwordTextField.setManaged(false);

        PasswordField confirmPasswordField = new PasswordField();
        confirmPasswordField.setPromptText("Confirm Password");
        confirmPasswordField.setStyle(inputStyle);

        TextField confirmPasswordTextField = new TextField();
        confirmPasswordTextField.setPromptText("Confirm Password");
        confirmPasswordTextField.setStyle(inputStyle);
        confirmPasswordTextField.setVisible(false);
        confirmPasswordTextField.setManaged(false);

        passwordTextField.textProperty().bindBidirectional(passwordField.textProperty());
        confirmPasswordTextField.textProperty().bindBidirectional(confirmPasswordField.textProperty());

        CheckBox showPasswordCheck = new CheckBox("Show password");
        showPasswordCheck.setStyle("-fx-font-size: 13px;");

        HBox showPasswordRow = new HBox(showPasswordCheck);
        showPasswordRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        StackPane passwordStack = new StackPane(passwordField, passwordTextField);
        StackPane confirmPasswordStack = new StackPane(confirmPasswordField, confirmPasswordTextField);

        showPasswordCheck.selectedProperty().addListener((obs, oldV, show) -> {
            passwordTextField.setVisible(show);
            passwordTextField.setManaged(show);
            passwordField.setVisible(!show);
            passwordField.setManaged(!show);

            confirmPasswordTextField.setVisible(show);
            confirmPasswordTextField.setManaged(show);
            confirmPasswordField.setVisible(!show);
            confirmPasswordField.setManaged(!show);
        });

        TextField firstNameField = new TextField();
        firstNameField.setPromptText("First Name");
        firstNameField.setStyle(inputStyle);

        TextField lastNameField = new TextField();
        lastNameField.setPromptText("Last Name");
        lastNameField.setStyle(inputStyle);

        TextField phoneField = new TextField();
        phoneField.setPromptText("Phone Number");
        phoneField.setStyle(inputStyle);

        Button signupBtn = new Button("Sign Up");
        Button backBtn = new Button("Back");
        signupBtn.setStyle(primaryBtn);
        backBtn.setStyle(ghostBtn);

        // Hover feedback (visual only)
        signupBtn.setOnMouseEntered(e -> signupBtn.setStyle(primaryBtnHover));
        signupBtn.setOnMouseExited(e -> signupBtn.setStyle(primaryBtn));
        backBtn.setOnMouseEntered(e -> backBtn.setStyle(ghostBtnHover));
        backBtn.setOnMouseExited(e -> backBtn.setStyle(ghostBtn));

        // Keep your existing actions (logic unchanged)
        signupBtn.setOnAction(e -> signup(
                firstNameField, lastNameField, emailField,
                passwordField, passwordTextField,
                confirmPasswordField, confirmPasswordTextField,
                phoneField, showPasswordCheck
        ));
        backBtn.setOnAction(e -> goBack());

        // === BIGGER inner "card" (inside layer) ===
        VBox card = new VBox(
                8,
                titleLabel,
                subtitle,
                new Separator(),
                firstNameField,
                lastNameField,
                emailField,
                passwordStack,
                confirmPasswordStack,
                showPasswordRow,
                phoneField
        );
        card.setSpacing(15);
        card.setAlignment(javafx.geometry.Pos.CENTER);

        // Make the inside layer bigger (wider + taller + roomier)
        card.setMaxWidth(640);  // was ~480
        card.setMinHeight(480);
        card.setPadding(new javafx.geometry.Insets(35, 50, 35, 50));
        card.setStyle("""
            -fx-background-color: rgba(255,255,255,0.40);
            -fx-background-radius: 26;
            -fx-border-radius: 26;
            -fx-border-color: rgba(255,255,255,0.55);
            -fx-border-width: 1.5;
            -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.22), 22, 0.25, 0, 6);
        """);

        // Action row
        HBox buttonRow = new HBox(14, backBtn, signupBtn);
        buttonRow.setAlignment(javafx.geometry.Pos.CENTER);

        // Layout for the form (outer container)
        VBox vbox = new VBox(22, card, buttonRow);
        vbox.setAlignment(javafx.geometry.Pos.CENTER);
        vbox.setPadding(new javafx.geometry.Insets(30));
        vbox.setMaxWidth(720); // expand outer wrapper so the big card breathes

        // Background image (path unchanged, exactly as you had it)
        vbox.setStyle(
                "-fx-background-image: url('file:src/resources/images/signup_background.png');" +
                        "-fx-background-size: cover;" +
                        "-fx-background-position: center center;"
        );

        // Return the root Pane
        return vbox;
    }

    private void signup(
            TextField firstNameField, TextField lastNameField, TextField emailField,
            PasswordField passwordField, TextField passwordTextField,
            PasswordField confirmPasswordField, TextField confirmPasswordTextField,
            TextField phoneField, CheckBox showPasswordCheck
    ) {
        String first = firstNameField.getText().trim();
        String last = lastNameField.getText().trim();
        String email = emailField.getText().trim();
        String pass = (showPasswordCheck.isSelected()
                ? passwordTextField.getText()
                : passwordField.getText()).trim();

        String confirm = (showPasswordCheck.isSelected()
                ? confirmPasswordTextField.getText()
                : confirmPasswordField.getText()).trim();

        String phone = phoneField.getText().trim();

        // Basic Validation
        if (first.isEmpty() || last.isEmpty() || email.isEmpty() || pass.isEmpty() || confirm.isEmpty() || phone.isEmpty()) {
            showMessage("Please fill in all fields!", "red");
            return;
        }

        // Password Match Validation
        if (!pass.equals(confirm)) {
            showMessage("Passwords do not match!", "red");
            return;
        }

        // Email Format Validation
        if (!email.matches("^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$")) {
            showMessage("Invalid email format!", "red");
            return;
        }

        // Phone Number Validation
        if (!phone.matches("\\d{10,15}")) {
            showMessage("Invalid phone number!", "red");
            return;
        }

        // Signup action (pass all required fields to the service)
        boolean success = userService.signup(first, last, phone, email, pass);

        // Handle success or failure
        if (success) {
            showMessage("Sign up successful!", "green");

            // Wait for a moment and go back to the login page
            new Thread(() -> {
                try {
                    Thread.sleep(2000); // Delay for 2 seconds to show the success message
                    Platform.runLater(this::goBack); // Go back to login page
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();
        } else {
            showMessage("Sign up failed. Please try again.", "red");
        }
    }

    private void goBack() {
        try {
            // Load the LoginPage.fxml and set it as the root of the stage
            FXMLLoader loader = new FXMLLoader(getClass().getClassLoader().getResource("resources/fxml/LoginPage.fxml"));

            if (loader.getLocation() == null) {
                System.err.println("FXML not found! Check path: /fxml/LoginPage.fxml");
                showMessage("Login page not found!", "red");
                return;
            }

            // Load the root Pane from LoginPage.fxml
            Pane root = loader.load();

            // Inject services (inventory, userService) into LoginPage controller
            LoginPage controller = loader.getController();
            controller.setServices(inventory, userService);

            // Switch scene to the LoginPage
            stage.getScene().setRoot(root);  // Use existing stage to switch scene

        } catch (IOException ex) {
            ex.printStackTrace();
            showMessage("Failed to open login page!", "red");
        }
    }

    private void showMessage(String text, String color) {
        // Show message to user (you can enhance this with a label or pop-up)
        System.out.println(text); // For debugging purposes
    }
}
