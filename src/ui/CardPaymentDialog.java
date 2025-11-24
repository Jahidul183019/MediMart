package ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.animation.FadeTransition;

import java.util.function.Consumer;

public class CardPaymentDialog {

    /**
     * Show payment UI on the SAME Stage (no extra window).
     */
    public static void show(Stage stage, Consumer<Boolean> onResult, double totalAmount) {

        Scene previousScene = stage.getScene(); // return-to cart

        /* ----------------------------------------------------
         * Header
         * ---------------------------------------------------- */
        Label title = new Label("Secure Payment");
        title.setStyle(
                "-fx-font-size: 22px;" +
                        "-fx-font-weight: 700;" +
                        "-fx-text-fill: #111827;"
        );

        Label subtitle = new Label("Enter your card details to complete your purchase.");
        subtitle.setStyle(
                "-fx-font-size: 13px;" +
                        "-fx-text-fill: #6B7280;"
        );

        VBox headerBox = new VBox(4, title, subtitle);
        headerBox.setAlignment(Pos.CENTER_LEFT);

        /* ----------------------------------------------------
         * Billing Summary
         * ---------------------------------------------------- */
        Label billingTitle = new Label("Billing Summary");
        billingTitle.setStyle(
                "-fx-font-size: 15px;" +
                        "-fx-font-weight: 600;"
        );

        Label totalLabel = new Label("Total Amount: " +
                String.format("%.2f", totalAmount) + " BDT");
        totalLabel.setStyle(
                "-fx-font-size: 16px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-text-fill: #16a34a;"
        );

        VBox billingBox = new VBox(4, billingTitle, totalLabel);
        billingBox.setAlignment(Pos.CENTER_LEFT);

        /* ----------------------------------------------------
         * Card Form
         * ---------------------------------------------------- */
        Label cardDetailsLabel = new Label("Card Details");
        cardDetailsLabel.setStyle(
                "-fx-font-size: 15px;" +
                        "-fx-font-weight: 600;"
        );

        TextField nameField = new TextField();
        nameField.setPromptText("Name on Card");

        TextField cardNumberField = new TextField();
        cardNumberField.setPromptText("XXXX XXXX XXXX XXXX");

        TextField expiryField = new TextField();
        expiryField.setPromptText("MM/YY");

        PasswordField cvvField = new PasswordField();
        cvvField.setPromptText("3 or 4 digits");

        // 🌈 Modern textbox look (rounded, padded, subtle border)
        String fieldStyle =
                "-fx-background-color: rgba(248,250,252,0.95);" +   // soft off-white
                        "-fx-background-radius: 10;" +
                        "-fx-border-radius: 10;" +
                        "-fx-border-color: #CBD5F5;" +
                        "-fx-border-width: 1;" +
                        "-fx-padding: 8 10;" +
                        "-fx-font-size: 13px;";

        nameField.setStyle(fieldStyle);
        cardNumberField.setStyle(fieldStyle);
        expiryField.setStyle(fieldStyle);
        cvvField.setStyle(fieldStyle);

        GridPane form = new GridPane();
        form.setHgap(14);
        form.setVgap(12);

        form.add(new Label("Name on Card"), 0, 0);
        form.add(nameField, 1, 0);

        form.add(new Label("Card Number"), 0, 1);
        form.add(cardNumberField, 1, 1);

        form.add(new Label("Expiry (MM/YY)"), 0, 2);
        form.add(expiryField, 1, 2);

        form.add(new Label("CVV"), 0, 3);
        form.add(cvvField, 1, 3);

        ColumnConstraints colLabel = new ColumnConstraints(130);
        ColumnConstraints colField = new ColumnConstraints();
        colField.setHgrow(Priority.ALWAYS);
        form.getColumnConstraints().addAll(colLabel, colField);

        /* ----------------------------------------------------
         * Buttons
         * ---------------------------------------------------- */
        Button cancelBtn = new Button("Cancel");
        cancelBtn.getStyleClass().add("danger-button");

        cancelBtn.setOnAction(e -> {
            stage.setScene(previousScene);
            if (onResult != null) onResult.accept(false);
        });

        Button payBtn = new Button("Pay Now");
        payBtn.getStyleClass().add("primary-button");

        payBtn.setOnAction(e -> {
            if (!validate(nameField, cardNumberField, expiryField, cvvField)) return;

            if (onResult != null) onResult.accept(true);
        });

        HBox buttonRow = new HBox(10, cancelBtn, payBtn);
        buttonRow.setAlignment(Pos.CENTER_RIGHT);

        /* ----------------------------------------------------
         * Card Container (Glassmorphism)
         * ---------------------------------------------------- */
        VBox card = new VBox(
                20,
                headerBox,
                new Separator(),
                billingBox,
                new Separator(),
                cardDetailsLabel,
                form,
                new Separator(),
                buttonRow
        );

        card.setPadding(new Insets(22));
        card.setAlignment(Pos.TOP_CENTER);
        card.setStyle(
                "-fx-background-color: rgba(255,255,255,0.90);" +
                        "-fx-background-radius: 22;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.18), 25, 0.22, 0, 6);"
        );

        /* ----------------------------------------------------
         * Root (Adaptive gradient background)
         * ---------------------------------------------------- */
        StackPane root = new StackPane(card);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(34));
        root.setStyle(
                "-fx-background-color: linear-gradient(to bottom right, #E0F2FE, #F9FAFB);"
        );

        // Make it look good on bigger screens too
        card.maxWidthProperty().bind(root.widthProperty().multiply(0.65));
        card.maxHeightProperty().bind(root.heightProperty().multiply(0.90));
        card.setMinWidth(480);

        // Slightly larger default window so it breathes on big displays
        Scene scene = new Scene(root, 780, 520);

        try {
            scene.getStylesheets().add(
                    CardPaymentDialog.class.getResource("/resources/css/theme.css").toExternalForm()
            );
        } catch (Exception ignored) {}

        /* Fade-in animation */
        card.setOpacity(0);
        FadeTransition ft = new FadeTransition(Duration.millis(280), card);
        ft.setFromValue(0);
        ft.setToValue(1);
        ft.play();

        stage.setScene(scene);
        stage.setTitle("Secure Payment");
        stage.show();
    }

    /* ----------------------------------------------------
     * Validation (unchanged)
     * ---------------------------------------------------- */
    private static boolean validate(TextField nameField,
                                    TextField cardNumberField,
                                    TextField expiryField,
                                    PasswordField cvvField) {

        if (nameField.getText().trim().isEmpty()
                || cardNumberField.getText().trim().isEmpty()
                || expiryField.getText().trim().isEmpty()
                || cvvField.getText().trim().isEmpty()) {

            new Alert(Alert.AlertType.ERROR,
                    "Please fill in all card details.").showAndWait();
            return false;
        }

        if (cardNumberField.getText().replaceAll("\\s+", "").length() < 12) {
            new Alert(Alert.AlertType.ERROR,
                    "Card number looks too short.").showAndWait();
            return false;
        }

        if (cvvField.getText().trim().length() < 3) {
            new Alert(Alert.AlertType.ERROR,
                    "CVV must be at least 3 digits.").showAndWait();
            return false;
        }

        return true;
    }
}
