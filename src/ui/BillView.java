package ui;

import javafx.animation.ScaleTransition;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import models.Medicine;
import models.OrderItem;

// PDFBox 3 imports
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts.FontName;

import java.io.File;
import java.io.IOException;

public class BillView {

    // Using the main Stage (no static Stage here)
    private final ObservableList<OrderItem> items;
    private final double total;
    private final String orderDate;

    public BillView(ObservableList<OrderItem> items, double total, String orderDate) {
        // copy so later cartData.clear() doesn’t affect the bill table
        this.items = FXCollections.observableArrayList(items);
        this.total = total;
        this.orderDate = orderDate;
    }

    public void show(Stage stage) {
        stage.setTitle("Order Bill");

        /* ---------- Header ---------- */
        Label title = new Label("Order Bill / Invoice");
        title.setStyle(
                "-fx-font-size: 24px;" +
                        "-fx-font-weight: 700;" +
                        "-fx-text-fill: #111827;" +
                        "-fx-letter-spacing: 0.5px;"
        );

        Label subtitle = new Label("Thank you for shopping with MediMart.");
        subtitle.setStyle(
                "-fx-font-size: 13px;" +
                        "-fx-text-fill: #6B7280;"
        );

        VBox textHeader = new VBox(4, title, subtitle);
        textHeader.setAlignment(Pos.CENTER_LEFT);

        Label dateLabel = new Label("Order Date: " + orderDate);
        dateLabel.setStyle(
                "-fx-font-size: 13px;" +
                        "-fx-text-fill: #4B5563;"
        );

        HBox headerRow = new HBox(10, textHeader, dateLabel);
        headerRow.setAlignment(Pos.CENTER);
        HBox.setHgrow(textHeader, Priority.ALWAYS);
        HBox.setMargin(dateLabel, new Insets(4, 0, 0, 20));

        /* ---------- Table Setup ---------- */
        TableView<OrderItem> table = new TableView<>(items);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        table.setPrefHeight(280);
        table.setFixedCellSize(34);
        table.setPlaceholder(new Label("No items found.\nYour purchased medicines will appear here."));

        TableColumn<OrderItem, String> nameCol = new TableColumn<>("Medicine");
        nameCol.setCellValueFactory(cd -> {
            Medicine m = cd.getValue().getMedicine();
            return (m != null) ? m.nameProperty() : new SimpleStringProperty("");
        });
        nameCol.setStyle("-fx-alignment: CENTER-LEFT;");

        TableColumn<OrderItem, Integer> qtyCol = new TableColumn<>("Qty");
        qtyCol.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        qtyCol.setStyle("-fx-alignment: CENTER;");

        TableColumn<OrderItem, Double> priceCol = new TableColumn<>("Unit Price");
        priceCol.setCellValueFactory(cd -> {
            Medicine m = cd.getValue().getMedicine();
            return (m != null) ? m.priceProperty().asObject() : null;
        });
        priceCol.setStyle("-fx-alignment: CENTER-RIGHT;");

        TableColumn<OrderItem, Double> lineTotalCol = new TableColumn<>("Line Total");
        lineTotalCol.setCellValueFactory(new PropertyValueFactory<>("totalPrice"));
        lineTotalCol.setStyle("-fx-alignment: CENTER-RIGHT;");

        table.getColumns().addAll(nameCol, qtyCol, priceCol, lineTotalCol);

        table.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-border-color: transparent;" +
                        "-fx-table-cell-border-color: rgba(148,163,184,0.35);" +
                        "-fx-padding: 4;"
        );
        VBox.setVgrow(table, Priority.ALWAYS); // 🔹 let table grow with window

        /* ---------- Bottom Summary Row ---------- */
        Label totalLabel = new Label("Grand Total: " + String.format("%.2f", total) + " BDT");
        totalLabel.setStyle(
                "-fx-font-size: 18px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-text-fill: #16a34a;"
        );

        // Download PDF button
        Button pdfBtn = new Button("⬇ Download PDF");
        pdfBtn.getStyleClass().add("primary-button");
        pdfBtn.setStyle(
                "-fx-background-radius: 999;" +
                        "-fx-font-size: 13px;" +
                        "-fx-font-weight: 600;" +
                        "-fx-padding: 8 18;"
        );
        pdfBtn.setOnAction(e -> generatePdfInvoice(stage));

        // Back to dashboard button
        Button backBtn = new Button("← Back to Dashboard");
        backBtn.getStyleClass().add("secondary-button");
        backBtn.setStyle(
                "-fx-background-radius: 999;" +
                        "-fx-font-size: 13px;" +
                        "-fx-font-weight: 500;" +
                        "-fx-padding: 8 18;"
        );
        backBtn.setOnAction(e -> {
            CustomerDashboard dashboard = new CustomerDashboard();
            dashboard.show(stage);
        });

        HBox buttonRow = new HBox(10, pdfBtn, backBtn);
        buttonRow.setAlignment(Pos.CENTER_RIGHT);

        HBox bottomRow = new HBox(20, totalLabel, buttonRow);
        bottomRow.setAlignment(Pos.CENTER_RIGHT);
        HBox.setHgrow(buttonRow, Priority.ALWAYS);
        HBox.setMargin(totalLabel, new Insets(0, 0, 0, 4));

        /* ---------- Card Container (glassmorphism style) ---------- */
        VBox card = new VBox(18, headerRow, table, bottomRow);
        card.setPadding(new Insets(20));
        card.setAlignment(Pos.TOP_CENTER);
        card.setStyle(
                "-fx-background-color: rgba(255,255,255,0.95);" +
                        "-fx-background-radius: 26;" +
                        "-fx-effect: dropshadow(gaussian, rgba(15,23,42,0.18), 28, 0.25, 0, 8);"
        );

        /* ---------- Root with soft gradient background & responsive card ---------- */
        StackPane root = new StackPane(card);
        root.setPadding(new Insets(24));
        root.setAlignment(Pos.CENTER);
        root.setStyle(
                "-fx-background-color: linear-gradient(to bottom right, #E0F2FE, #F9FAFB);"
        );

        // 🔹 Make it fit nicely on different screen sizes
        card.maxWidthProperty().bind(root.widthProperty().multiply(0.75));
        card.maxHeightProperty().bind(root.heightProperty().multiply(0.90));
        card.setMinWidth(520);

        Scene scene = new Scene(root, 820, 520); // bigger default window

        try {
            scene.getStylesheets().add(
                    getClass().getResource("/resources/css/theme.css").toExternalForm()
            );
        } catch (Exception ignore) {}

        // Scale-in animation on the card (feels more “iOS”)
        card.setScaleX(0.9);
        card.setScaleY(0.9);
        ScaleTransition st = new ScaleTransition(Duration.millis(220), card);
        st.setFromX(0.9);
        st.setFromY(0.9);
        st.setToX(1.0);
        st.setToY(1.0);
        st.play();

        stage.setScene(scene);
        stage.show();
    }

    /**
     * Generate a simple PDF invoice for this bill and let user download it.
     */
    private void generatePdfInvoice(Stage owner) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save Invoice as PDF");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("PDF Files", "*.pdf")
        );
        chooser.setInitialFileName("invoice_" + orderDate.replace(" ", "_").replace(":", "-") + ".pdf");
        File file = chooser.showSaveDialog(owner);
        if (file == null) {
            return; // user cancelled
        }

        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);

            PDType1Font headerFont = new PDType1Font(FontName.HELVETICA_BOLD);
            PDType1Font boldFont   = new PDType1Font(FontName.HELVETICA_BOLD);
            PDType1Font normalFont = new PDType1Font(FontName.HELVETICA);

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {

                float margin = 50;
                float y = page.getMediaBox().getHeight() - margin;

                // Header
                cs.beginText();
                cs.setFont(headerFont, 20);
                cs.newLineAtOffset(margin, y);
                cs.showText("MediMart Invoice");
                cs.endText();

                y -= 30;

                // Date
                cs.beginText();
                cs.setFont(normalFont, 12);
                cs.newLineAtOffset(margin, y);
                cs.showText("Order Date: " + orderDate);
                cs.endText();

                y -= 25;

                // Table header
                cs.beginText();
                cs.setFont(boldFont, 12);
                cs.newLineAtOffset(margin, y);
                cs.showText(String.format("%-30s %-8s %-10s %-10s",
                        "Medicine", "Qty", "Unit", "Total"));
                cs.endText();

                y -= 18;

                // Items
                cs.setFont(normalFont, 12);
                for (OrderItem item : items) {
                    if (y < margin + 40) {
                        break; // not handling multi-page in this simple version
                    }
                    Medicine m = item.getMedicine();
                    String name = (m != null ? m.getName() : "");
                    String line = String.format("%-30.30s %-8d %-10.2f %-10.2f",
                            name, item.getQuantity(),
                            (m != null ? m.getPrice() : 0.0),
                            item.getTotalPrice());

                    cs.beginText();
                    cs.newLineAtOffset(margin, y);
                    cs.showText(line);
                    cs.endText();

                    y -= 16;
                }

                y -= 20;

                // Total
                cs.beginText();
                cs.setFont(boldFont, 14);
                cs.newLineAtOffset(margin, y);
                cs.showText("Grand Total: " + String.format("%.2f", total) + " BDT");
                cs.endText();
            }

            doc.save(file);

            Alert a = new Alert(Alert.AlertType.INFORMATION,
                    "Invoice saved to:\n" + file.getAbsolutePath(),
                    ButtonType.OK);
            a.setHeaderText("PDF Downloaded");
            a.showAndWait();

        } catch (IOException ex) {
            ex.printStackTrace();
            Alert a = new Alert(Alert.AlertType.ERROR,
                    "Failed to generate PDF: " + ex.getMessage(),
                    ButtonType.OK);
            a.setHeaderText("Error");
            a.showAndWait();
        }
    }
}
