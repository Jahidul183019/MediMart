package ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.TextFormatter.Change;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
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

public class AddMedicineForm {
    private final Inventory inventory;
    private final MedicineService medicineService;

    // hold user-chosen file for this form session
    private File chosenImage;

    public AddMedicineForm(Inventory inventory) {
        this.inventory = inventory;
        this.medicineService = new MedicineService();
    }

    public void show(Stage stage) {
        // --- Responsive sizing (fit laptop screen neatly) ---
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

        // Make right column grow (labels left, fields right)
        ColumnConstraints c0 = new ColumnConstraints();
        ColumnConstraints c1 = new ColumnConstraints();
        c1.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(c0, c1);

        // --- Fields ---
        TextField idField = new TextField();
        idField.setPromptText("ID (integer)");
        applyIntegerFormatter(idField, false);
        styleInput(idField);

        TextField nameField = new TextField();
        nameField.setPromptText("Medicine Name");
        styleInput(nameField);

        TextField categoryField = new TextField();
        categoryField.setPromptText("Category");
        styleInput(categoryField);

        TextField priceField = new TextField();
        priceField.setPromptText("Price (e.g., 99.50)");
        applyDecimalFormatter(priceField);
        styleInput(priceField);

        TextField qtyField = new TextField();
        qtyField.setPromptText("Quantity");
        applyIntegerFormatter(qtyField, true);
        styleInput(qtyField);

        TextField expiryField = new TextField();
        expiryField.setPromptText("Expiry Date (YYYY-MM-DD)");
        styleInput(expiryField);

        // --- Image upload (NEW) ---
        Button uploadBtn = new Button("Upload Image");
        uploadBtn.setStyle("-fx-background-color: #607D8B; -fx-text-fill: white; -fx-background-radius: 10; -fx-font-weight: bold; -fx-padding: 6 12;");
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

        uploadBtn.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.setTitle("Choose Medicine Image");
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif"));
            File f = fc.showOpenDialog(stage);
            if (f != null) {
                chosenImage = f;
                try {
                    preview.setImage(new Image(f.toURI().toString(), 150, 120, true, true));
                } catch (Exception ignored) { /* keep UI calm */ }
            }
        });

        HBox imageRow = new HBox(10, uploadBtn, preview);
        imageRow.setAlignment(Pos.CENTER_LEFT);

        Button addBtn = new Button("Add Medicine");
        addBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-background-radius: 10; -fx-font-weight: bold; -fx-padding: 8 16;");
        Button backBtn = new Button("Back");
        backBtn.setStyle("-fx-background-color: #f44336; -fx-text-fill: white; -fx-background-radius: 10; -fx-font-weight: bold; -fx-padding: 8 16;");

        Label status = new Label();
        status.setWrapText(true);

        // --- Actions ---
        addBtn.setOnAction(e -> {
            String idText = safeTrim(idField.getText());
            String name = safeTrim(nameField.getText());
            String category = safeTrim(categoryField.getText());
            String priceText = safeTrim(priceField.getText());
            String qtyText = safeTrim(qtyField.getText());
            String expiry = safeTrim(expiryField.getText());

            if (idText.isEmpty() || name.isEmpty() || category.isEmpty() || priceText.isEmpty() || qtyText.isEmpty() || expiry.isEmpty()) {
                setStatus(status, "All fields are required.", false);
                return;
            }

            int id, quantity;
            double price;
            try {
                id = Integer.parseInt(idText);
                price = Double.parseDouble(priceText);
                quantity = Integer.parseInt(qtyText);
            } catch (NumberFormatException ex) {
                setStatus(status, "Invalid numbers. Use integer ID/Quantity and decimal Price.", false);
                return;
            }

            if (id < 0) { setStatus(status, "ID must be a non-negative integer.", false); return; }
            if (price < 0) { setStatus(status, "Price cannot be negative.", false); return; }
            if (quantity <= 0) { setStatus(status, "Quantity must be greater than 0.", false); return; }
            if (!isValidIsoDate(expiry)) { setStatus(status, "Invalid date. Use YYYY-MM-DD (e.g., 2026-01-31).", false); return; }

            // Save image (optional)
            String imagePath = null;
            if (chosenImage != null) {
                try {
                    imagePath = ImageStorage.saveImage(chosenImage); // stores in medimart_data/images and returns absolute path
                } catch (Exception ex) {
                    // non-fatal; just skip image if copy fails
                    System.err.println("Image save failed: " + ex.getMessage());
                }
            }

            // Build model (uses your updated Medicine that supports imagePath)
            Medicine medicine = new Medicine(id, name, category, price, quantity, expiry, imagePath);

            boolean success;
            try {
                success = medicineService.addMedicine(medicine);
            } catch (Exception ex) {
                setStatus(status, "Failed to add medicine: " + ex.getMessage(), false);
                return;
            }

            if (success) {
                setStatus(status, "Medicine added successfully!", true);
                clearFields(idField, nameField, categoryField, priceField, qtyField, expiryField);
                preview.setImage(null);
                chosenImage = null;
                idField.requestFocus();
            } else {
                setStatus(status, "Error adding medicine. (Check duplicates or DB constraints.)", false);
            }
        });

        backBtn.setOnAction(e -> new AdminDashboard(inventory).show(stage));

        // Keyboard: Enter to add, Esc to back
        grid.setOnKeyPressed(e -> {
            switch (e.getCode()) {
                case ENTER -> addBtn.fire();
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
        grid.addRow(6, new Label("Image:"), imageRow); // NEW row

        GridPane.setMargin(backBtn, new Insets(8, 8, 0, 0));
        GridPane.setMargin(addBtn, new Insets(8, 0, 0, 8));
        grid.add(backBtn, 0, 7);
        grid.add(addBtn, 1, 7);
        grid.add(status, 1, 8);

        // --- Scene & Stage (responsive) ---
        Scene scene = new Scene(grid, prefW, prefH);
        stage.setScene(scene);
        stage.setTitle("Add New Medicine");
        stage.setMinWidth(560);
        stage.setMinHeight(460);
        // Center on screen
        stage.setX(b.getMinX() + (b.getWidth() - prefW) / 2);
        stage.setY(b.getMinY() + (b.getHeight() - prefH) / 2);
        stage.show();
    }

    // ---------- Helpers ----------
    private static String safeTrim(String s) { return s == null ? "" : s.trim(); }

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

    /** Apply polished styling and focus glow to inputs */
    private static void styleInput(TextField tf) {
        final String base = """
            -fx-background-color: rgba(255,255,255,0.92);
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
        tf.focusedProperty().addListener((obs, oldV, focused) -> {
            if (focused) {
                tf.setStyle(base + """
                    -fx-border-color: #26A69A;
                    -fx-effect: dropshadow(gaussian, rgba(38,166,154,0.35), 12, 0.35, 0, 0);
                """);
            } else {
                tf.setStyle(base + "-fx-effect: null;");
            }
        });
    }

    private void clearFields(TextField idField, TextField nameField, TextField categoryField,
                             TextField priceField, TextField qtyField, TextField expiryField) {
        idField.clear(); nameField.clear(); categoryField.clear();
        priceField.clear(); qtyField.clear(); expiryField.clear();
    }
}
