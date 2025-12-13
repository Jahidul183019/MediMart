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

/**
 * key: the OTP internal key (e.g. "email:abc@x.com" or "phone:017...")
 * type: "email" or "phone"
 * identifier: the email string or phone string (used when resetting password)
 */
public class VerifyOTPPage {
    private final Inventory inventory;
    private final UserService userService;
    private final String key;
    private final String type;
    private final String identifier;

    public VerifyOTPPage(Inventory inventory, UserService userService, String key, String type, String identifier) {
        this.inventory = inventory;
        this.userService = userService;
        this.key = key;
        this.type = type;
        this.identifier = identifier;
    }

    public void show(Stage stage) {
        Label title = new Label("Enter OTP");
        TextField otpField = new TextField();
        otpField.setPromptText("6-digit OTP");

        Button verify = new Button("Verify");
        Button resend = new Button("Resend OTP");
        Button back = new Button("Back");
        Label msg = new Label();

        HBox buttons = new HBox(10, verify, resend, back);
        buttons.setAlignment(Pos.CENTER);

        VBox vbox = new VBox(12, title, otpField, buttons, msg);
        vbox.setAlignment(Pos.CENTER);
        vbox.setPadding(new Insets(20));

        verify.setOnAction(e -> {
            String otp = otpField.getText().trim();
            if (otp.isEmpty()) { msg.setText("Enter OTP."); return; }
            boolean ok = OTPService.verifyOTP(key, otp);
            if (ok) {
                // success — go to reset password
                new ResetPasswordPage(inventory, userService, type, identifier).show(stage);
            } else {
                msg.setText("Invalid or expired OTP.");
            }
        });

        resend.setOnAction(e -> {
            if (type.equals("email")) OTPService.sendOTPEmail(identifier);
            else OTPService.sendOTPPhone(identifier);
            msg.setText("OTP resent (console).");
        });

        back.setOnAction(e -> {
            if (type.equals("email")) new ResetByEmailPage(inventory, userService).show(stage);
            else new ResetByPhonePage(inventory, userService).show(stage);
        });

        Scene scene = new Scene(vbox, 420, 240);
        stage.setScene(scene);
        stage.setTitle("Verify OTP");
        stage.show();
    }
}