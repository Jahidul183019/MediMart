package ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.TextFormatter.Change;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Screen;
import javafx.stage.Stage;
import models.Inventory;
import models.Medicine;
import services.MedicineService;
import utils.ImageStorage;

import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.function.UnaryOperator;

public class EditMedicineForm {
    private final Inventory inventory;
    private final Medicine medicine;
    private final MedicineService medicineService;

    private File chosenImage; // optional new image

    public EditMedicineForm(Inventory inventory, Medicine medicine) {
        this.inventory = inventory;
        this.medicine = medicine;
        this.medicineService = new MedicineService();
    }

    public void show(Stage stage) {
        // --- Responsive sizing ---
        Rectangle2D b = Screen.getPrimary().getVisualBounds();
        double prefW = Math.max(560, Math.min(780, b.getWidth() * 0.54));
        double prefH = Math.max(460, Math.min(620, b.getHeight() * 0.64));

        // --- Form grid ---
        GridPane grid = new GridPane();
        grid.setHgap(14);
        grid.setVgap(12);
        grid.setPadding(new Insets(24));
        grid.setAlignment(Pos.CENTER);
        grid.setStyle("""
            -fx-background-color: linear-gradient(to bottom right, #E0F7FA, #E8F5E9);
            -fx-background-radius: 14;
        """);

        // --- Fields (prefilled) ---
        TextField idField = new TextField(String.valueOf(medicine.getId()));
        idField.setDisable(true);
        styleInput(idField);

        TextField nameField = new TextField(safe(medicine.getName()));
        nameField.setPromptText("Medicine Name");
        styleInput(nameField);

        TextField categoryField = new TextField(safe(medicine.getCategory()));
        categoryField.setPromptText("Category");
        styleInput(categoryField);

        TextField priceField = new TextField(String.format("%.2f", medicine.getPrice()));
        priceField.setPromptText("Price (e.g., 99.50)");
        applyDecimalFormatter(priceField);
        styleInput(priceField);

        TextField qtyField = new TextField(String.valueOf(medicine.getQuantity()));
        qtyField.setPromptText("Quantity");
        applyIntegerFormatter(qtyField, true);
        styleInput(qtyField);

        TextField expiryField = new TextField(safe(medicine.getExpiryDate()));
        expiryField.setPromptText("Expiry Date (YYYY-MM-DD)");
        styleInput(expiryField);

        // --- Image upload + preview (NEW) ---
        ImageView preview = new ImageView();
        preview.setFitWidth(150);
        preview.setFitHeight(120);
        preview.setPreserveRatio(true);
        preview.setSmooth(true);
        preview.setStyle("""
            -fx-background-color: rgba(0,0,0,0.05);
            -fx-background-radius: 10;
            -fx-border-radius: 10;
            -fx-border-color: rgba(0,0,0,0.08);
            -fx-border-width: 1;
        """);

        if (medicine.getImagePath() != null && !medicine.getImagePath().isBlank()) {
            try {
                preview.setImage(new Image("file:" + medicine.getImagePath(), 150, 120, true, true));
            } catch (Exception ignored) { }
        }

        Button uploadBtn = new Button("Change Image");
        uploadBtn.setStyle("-fx-background-color: #607D8B; -fx-text-fill: white; -fx-background-radius: 10; -fx-font-weight: bold; -fx-padding: 6 12;");
        uploadBtn.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.setTitle("Choose New Medicine Image");
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif"));
            File f = fc.showOpenDialog(stage);
            if (f != null) {
                chosenImage = f;
                preview.setImage(new Image(f.toURI().toString(), 150, 120, true, true));
            }
        });

        // --- Buttons + status ---
        Button updateBtn = new Button("Update Medicine");
        Button backBtn   = new Button("Back");

        String btnBase = "-fx-background-radius: 10; -fx-font-weight: bold; -fx-padding: 8 18;";
        updateBtn.setStyle(btnBase + "-fx-background-color: #FFA500; -fx-text-fill: white;");
        backBtn.setStyle(btnBase + "-fx-background-color: #f44336; -fx-text-fill: white;");

        Label status = new Label();
        status.setWrapText(true);

        // --- Actions ---
        updateBtn.setOnAction(e -> {
            String name = safe(nameField.getText());
            String category = safe(categoryField.getText());
            String priceText = safe(priceField.getText());
            String qtyText = safe(qtyField.getText());
            String expiry = safe(expiryField.getText());

            // Required
            if (name.isEmpty() || category.isEmpty() || priceText.isEmpty() || qtyText.isEmpty() || expiry.isEmpty()) {
                setStatus(status, "All fields are required.", false);
                return;
            }

            // Numbers
            double price;
            int qty;
            try {
                price = Double.parseDouble(priceText);
                qty = Integer.parseInt(qtyText);
            } catch (NumberFormatException ex) {
                setStatus(status, "Invalid numbers. Use decimal Price and integer Quantity.", false);
                return;
            }
            if (price < 0) { setStatus(status, "Price cannot be negative.", false); return; }
            if (qty <= 0) { setStatus(status, "Quantity must be greater than 0.", false); return; }

            // Date
            if (!isValidIsoDate(expiry)) {
                setStatus(status, "Invalid date. Use YYYY-MM-DD (e.g., 2026-01-31).", false);
                return;
            }

            try {
                // Save new image if chosen
                String imagePath = medicine.getImagePath();
                if (chosenImage != null) {
                    imagePath = ImageStorage.saveImage(chosenImage);
                }

                // Update model
                medicine.setName(name);
                medicine.setCategory(category);
                medicine.setPrice(price);
                medicine.setQuantity(qty);
                medicine.setExpiryDate(expiry);
                medicine.setImagePath(imagePath);

                // Persist to DB
                boolean ok = medicineService.updateMedicine(medicine);
                if (ok) {
                    setStatus(status, "Medicine updated successfully!", true);
                } else {
                    setStatus(status, "Failed to update medicine.", false);
                }

            } catch (Exception ex) {
                setStatus(status, "Update failed: " + ex.getMessage(), false);
            }
        });

        backBtn.setOnAction(e -> new AdminDashboard(inventory).show(stage));

        // Keyboard: Enter = Update, Esc = Back
        grid.setOnKeyPressed(e -> {
            switch (e.getCode()) {
                case ENTER -> updateBtn.fire();
                case ESCAPE -> backBtn.fire();
            }
        });

        // --- Layout ---
        grid.addRow(0, new Label("ID:"), idField);
        grid.addRow(1, new Label("Name:"), nameField);
        grid.addRow(2, new Label("Category:"), categoryField);
        grid.addRow(3, new Label("Price:"), priceField);
        grid.addRow(4, new Label("Quantity:"), qtyField);
        grid.addRow(5, new Label("Expiry:"), expiryField);
        grid.addRow(6, new Label("Image:"), uploadBtn);
        grid.add(preview, 1, 6);
        grid.add(backBtn, 0, 7);
        grid.add(updateBtn, 1, 7);
        grid.add(status, 1, 8);

        // --- Scene ---
        Scene scene = new Scene(grid, prefW, prefH);
        stage.setScene(scene);
        stage.setTitle("Edit Medicine");
        stage.setMinWidth(560);
        stage.setMinHeight(460);
        // Center
        stage.setX(b.getMinX() + (b.getWidth() - prefW) / 2);
        stage.setY(b.getMinY() + (b.getHeight() - prefH) / 2);
        stage.show();

        nameField.requestFocus();
    }

    // ===== Helpers =====
    private static String safe(String s) { return s == null ? "" : s.trim(); }

    private static void setStatus(Label label, String msg, boolean ok) {
        label.setText(msg);
        label.setTextFill(ok ? Color.web("#2e7d32") : Color.web("#c62828"));
    }

    private static boolean isValidIsoDate(String s) {
        try {
            DateTimeFormatter f = DateTimeFormatter.ISO_LOCAL_DATE;
            LocalDate.parse(s, f);
            return true;
        } catch (DateTimeParseException ex) {
            return false;
        }
    }

    private static void applyIntegerFormatter(TextField field, boolean mustBePositive) {
        UnaryOperator<Change> filter = change -> {
            String text = change.getControlNewText();
            if (text.isEmpty() || text.matches("\\d+")) {
                if (mustBePositive && text.startsWith("0") && text.length() > 1) return null;
                return change;
            }
            return null;
        };
        field.setTextFormatter(new TextFormatter<>(filter));
    }

    private static void applyDecimalFormatter(TextField field) {
        UnaryOperator<Change> filter = change -> {
            String text = change.getControlNewText();
            if (text.isEmpty() || text.matches("\\d{0,9}(\\.\\d{0,2})?")) return change;
            return null;
        };
        field.setTextFormatter(new TextFormatter<>(filter));
    }

    /** Consistent polished input styling (rounded, border, focus glow) */
    private static void styleInput(TextField tf) {
        final String base = """
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
        tf.setStyle(base);
        tf.focusedProperty().addListener((obs, o, f) -> {
            if (f) {
                tf.setStyle(base + """
                    -fx-border-color: #26A69A;
                    -fx-effect: dropshadow(gaussian, rgba(38,166,154,0.35), 12, 0.35, 0, 0);
                """);
            } else {
                tf.setStyle(base + "-fx-effect: null;");
            }
        });
    }
}
