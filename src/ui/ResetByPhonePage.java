package ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import models.Inventory;
import services.OTPService;
import services.UserService;

public class ResetByPhonePage {
    private final Inventory inventory;
    private final UserService userService;

    public ResetByPhonePage(Inventory inventory, UserService userService) {
        this.inventory = inventory;
        this.userService = userService;
    }

    public void show(Stage stage) {
        Label title = new Label("Reset via Phone");
        TextField phoneField = new TextField();
        phoneField.setPromptText("Enter your phone number");

        Button sendBtn = new Button("Send OTP");
        Button back = new Button("Back");
        Label msg = new Label();

        VBox vbox = new VBox(10, title, phoneField, sendBtn, back, msg);
        vbox.setAlignment(Pos.CENTER);
        vbox.setPadding(new Insets(20));

        sendBtn.setOnAction(e -> {
            String phone = phoneField.getText().trim();
            if (phone.isEmpty()) {
                msg.setText("Please enter phone");
                return;
            }
            // optional: verify user exists by phone
            if (!userService.isPhoneRegistered(phone)) {
                msg.setText("Phone not found.");
                return;
            }
            OTPService.sendOTPPhone(phone);
            msg.setText("OTP sent to phone (check console for test OTP).");
            new VerifyOTPPage(inventory, userService, OTPService.keyForPhone(phone), "phone", phone).show(stage);
        });

        back.setOnAction(e -> new ForgotPasswordPage(inventory, userService).show(stage));

        Scene scene = new Scene(vbox, 450, 260);
        stage.setScene(scene);
        stage.setTitle("Reset via Phone");
        stage.show();
    }
}