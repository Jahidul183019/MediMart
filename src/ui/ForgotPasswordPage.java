package ui;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.stage.Stage;
import models.Inventory;
import services.UserService;

public class ForgotPasswordPage {

    private final Inventory inventory;
    private final UserService userService;

    public ForgotPasswordPage(Inventory inventory, UserService userService) {
        this.inventory = inventory;
        this.userService = userService;
    }

    public void show(Stage stage) {
        Label title = new Label("Recover your account");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");

        Label choose = new Label("Choose recovery method:");

        Button byEmail = new Button("Reset via Email");
        Button byPhone = new Button("Reset via Phone");
        Button back = new Button("Back to Login");

        VBox vbox = new VBox(12, title, choose, byEmail, byPhone, back);
        vbox.setAlignment(Pos.CENTER);
        vbox.setPadding(new Insets(20));
        vbox.setStyle(
                "-fx-background-color: #fdfdfd;" +
                        "-fx-border-color: #d0e7df;" +
                        "-fx-border-width: 2px;" +
                        "-fx-border-radius: 8px;"
        );

        // --- Button Actions ---
        byEmail.setOnAction(e -> new ResetByEmailPage(inventory, userService).show(stage));
        byPhone.setOnAction(e -> new ResetByPhonePage(inventory, userService).show(stage));

        back.setOnAction(e -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/LoginSignup.fxml"));
                Pane root = loader.load();

                LoginSignup controller = loader.getController();
                controller.setServices(inventory, userService);

                stage.getScene().setRoot(root);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        Scene scene = new Scene(vbox, 420, 260);
        stage.setScene(scene);
        stage.setTitle("Forgot Password - MediMart");
        stage.show();
    }
}
