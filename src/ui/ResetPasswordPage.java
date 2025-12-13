package ui;

import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import models.Inventory;
import services.UserService;

/**
 * After OTP verified — allows user to set a new password.
 * type = "email" or "phone"
 * identifier = the email or phone string
 */
public class ResetPasswordPage {
    private final Inventory inventory;
    private final UserService userService;
    private final String type;
    private final String identifier;

    public ResetPasswordPage(Inventory inventory, UserService userService, String type, String identifier) {
        this.inventory = inventory;
        this.userService = userService;
        this.type = type;
        this.identifier = identifier;
    }

    public void show(Stage stage) {
        Label title = new Label("Set New Password");
        PasswordField pass1 = new PasswordField();
        PasswordField pass2 = new PasswordField();
        pass1.setPromptText("New password");
        pass2.setPromptText("Confirm password");

        Button save = new Button("Save");
        Button back = new Button("Back");
        Label msg = new Label();
        msg.setTextFill(Color.RED);

        HBox btns = new HBox(10, save, back);
        btns.setAlignment(Pos.CENTER);

        VBox vbox = new VBox(12, title, pass1, pass2, btns, msg);
        vbox.setAlignment(Pos.CENTER);
        vbox.setPadding(new Insets(20));

        // --- Save Action ---
        save.setOnAction(e -> {
            String p1 = pass1.getText();
            String p2 = pass2.getText();
            if (p1.isEmpty() || p2.isEmpty()) {
                msg.setText("Fill both fields.");
                return;
            }
            if (!p1.equals(p2)) {
                msg.setText("Passwords do not match.");
                return;
            }

            boolean ok = type.equals("email")
                    ? userService.updatePasswordByEmail(identifier, p1)
                    : userService.updatePasswordByPhone(identifier, p1);

            if (ok) {
                msg.setTextFill(Color.GREEN);
                msg.setText("Password updated. You can login now.");
                goToLogin(stage);
            } else {
                msg.setTextFill(Color.RED);
                msg.setText("Failed to update password.");
            }
        });

        // --- Back Action ---
        back.setOnAction(e -> goToLogin(stage));

        Scene scene = new Scene(vbox, 450, 300);
        stage.setScene(scene);
        stage.setTitle("Reset Password");
        stage.show();
    }

    // --- Helper method to go back to login FXML ---
    private void goToLogin(Stage stage) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/LoginSignup.fxml"));
            Pane root = loader.load();

            LoginSignup controller = loader.getController();
            controller.setServices(inventory, userService);

            stage.getScene().setRoot(root);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}