package ui;

import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.geometry.Pos;
import javafx.geometry.Insets;
import models.Inventory;
import services.UserService;

public class ResetByEmailPage {

    private final Inventory inventory;
    private final UserService userService;

    public ResetByEmailPage(Inventory inventory, UserService userService) {
        this.inventory = inventory;
        this.userService = userService;
    }

    public void show(Stage stage) {
        Label title = new Label("Reset your password via Email");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        Label emailLabel = new Label("Enter your email:");
        TextField emailField = new TextField();
        emailField.setPromptText("Email");

        Button resetButton = new Button("Send Reset Link");
        Button backButton = new Button("Back");

        VBox vbox = new VBox(12, title, emailLabel, emailField, resetButton, backButton);
        vbox.setAlignment(Pos.CENTER);
        vbox.setPadding(new Insets(20));

        // Button actions
        resetButton.setOnAction(e -> {
            // Implement the email reset logic (e.g., send reset link)
            System.out.println("Email reset link sent!");
        });

        backButton.setOnAction(e -> {
            ForgotPasswordPage forgotPasswordPage = new ForgotPasswordPage(inventory, userService);
            forgotPasswordPage.show(stage);
        });

        Scene scene = new Scene(vbox, 400, 250);
        stage.setScene(scene);
        stage.setTitle("Reset via Email");
        stage.show();
    }
}
