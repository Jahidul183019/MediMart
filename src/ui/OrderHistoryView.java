package ui;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import models.OrderHistoryRow;
import utils.DBHelper;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class OrderHistoryView {

    private final int userId;

    public OrderHistoryView(int userId) {
        this.userId = userId;
    }

    /**
     * Show order history on the SAME Stage (CustomerDashboard is replaced),
     * with a Back button to return to the dashboard.
     */
    public void show(Stage stage) {
        stage.setTitle("My Order History");

        /* ---------- Title & Subtitle ---------- */
        Label title = new Label("Order History");
        title.setStyle(
                "-fx-font-size: 24px;" +
                        "-fx-font-weight: 700;" +
                        "-fx-text-fill: #111827;" +
                        "-fx-letter-spacing: 0.5px;"
        );

        Label subtitle = new Label("See your past purchases and track your spending.");
        subtitle.setStyle(
                "-fx-font-size: 13px;" +
                        "-fx-text-fill: #6B7280;"
        );

        VBox headerBox = new VBox(4, title, subtitle);
        headerBox.setAlignment(Pos.CENTER_LEFT);

        /* ---------- Table Setup ---------- */
        TableView<OrderHistoryRow> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        table.setPrefHeight(380);
        table.setFixedCellSize(36);
        table.setPlaceholder(new Label("No orders yet.\nYour completed orders will appear here."));

        TableColumn<OrderHistoryRow, Integer> orderIdCol = new TableColumn<>("Order");
        orderIdCol.setCellValueFactory(new PropertyValueFactory<>("orderId"));
        orderIdCol.setStyle("-fx-alignment: CENTER;");

        TableColumn<OrderHistoryRow, String> dateCol = new TableColumn<>("Date");
        dateCol.setCellValueFactory(new PropertyValueFactory<>("orderDate"));
        dateCol.setStyle("-fx-alignment: CENTER;");

        TableColumn<OrderHistoryRow, String> medCol = new TableColumn<>("Medicine");
        medCol.setCellValueFactory(new PropertyValueFactory<>("medicineName"));
        medCol.setStyle("-fx-alignment: CENTER-LEFT;");

        TableColumn<OrderHistoryRow, Integer> qtyCol = new TableColumn<>("Qty");
        qtyCol.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        qtyCol.setStyle("-fx-alignment: CENTER;");

        TableColumn<OrderHistoryRow, Double> totalCol = new TableColumn<>("Total (BDT)");
        totalCol.setCellValueFactory(new PropertyValueFactory<>("totalPrice"));
        totalCol.setStyle("-fx-alignment: CENTER-RIGHT;");

        table.getColumns().addAll(orderIdCol, dateCol, medCol, qtyCol, totalCol);

        // Slightly rounded / flat header look
        table.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-border-color: transparent;" +
                        "-fx-table-cell-border-color: rgba(148,163,184,0.35);" +
                        "-fx-padding: 4;"
        );

        /* ---------- Load data from DB ---------- */
        ObservableList<OrderHistoryRow> rows = FXCollections.observableArrayList();
        String sql = """
            SELECT oi.order_id,
                   oi.order_date,
                   oi.quantity,
                   oi.total_price,
                   m.name AS medicine_name
            FROM order_items oi
            JOIN medicines m ON oi.medicine_id = m.serial_number
            WHERE oi.user_id = ?
            ORDER BY oi.order_date DESC, oi.order_id DESC
        """;

        try (Connection conn = DBHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rows.add(new OrderHistoryRow(
                            rs.getInt("order_id"),
                            rs.getString("order_date"),
                            rs.getString("medicine_name"),
                            rs.getInt("quantity"),
                            rs.getDouble("total_price")
                    ));
                }
            }

        } catch (SQLException e) {
            new Alert(Alert.AlertType.ERROR,
                    "Failed to load order history: " + e.getMessage()
            ).showAndWait();
        }

        table.setItems(rows);

        // Simple summary pill (orders count + total spent)
        int orderCount = (int) rows.stream()
                .map(OrderHistoryRow::getOrderId)
                .distinct()
                .count();

        double totalSpent = rows.stream()
                .mapToDouble(OrderHistoryRow::getTotalPrice)
                .sum();

        Label summaryLabel = new Label(
                "Orders: " + orderCount +
                        "   ·   Total spent: " + String.format("%.2f", totalSpent) + " BDT"
        );
        summaryLabel.setStyle(
                "-fx-font-size: 12px;" +
                        "-fx-text-fill: #4B5563;"
        );

        HBox summaryBox = new HBox(summaryLabel);
        summaryBox.setAlignment(Pos.CENTER_RIGHT);

        /* ---------- Back Button (top, iOS-style pill) ---------- */
        Button backBtn = new Button("← Back");
        backBtn.setOnAction(e -> {
            CustomerDashboard dashboard = new CustomerDashboard();
            dashboard.show(stage);
        });
        backBtn.setStyle(
                "-fx-background-radius: 999;" +
                        "-fx-background-color: rgba(15,23,42,0.07);" +
                        "-fx-text-fill: #111827;" +
                        "-fx-font-size: 13px;" +
                        "-fx-font-weight: 500;" +
                        "-fx-padding: 8 16;"
        );

        HBox topBar = new HBox(backBtn);
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(0, 0, 6, 0));

        /* ---------- Card Container (glassmorphism style) ---------- */
        VBox card = new VBox(16, topBar, headerBox, table, summaryBox);
        card.setPadding(new Insets(18));
        card.setAlignment(Pos.TOP_CENTER);
        card.setStyle(
                "-fx-background-color: rgba(255,255,255,0.92);" +
                        "-fx-background-radius: 26;" +
                        "-fx-effect: dropshadow(gaussian, rgba(15,23,42,0.18), 28, 0.25, 0, 8);"
        );

        /* ---------- Root with gradient background ---------- */
        VBox root = new VBox(card);
        root.setPadding(new Insets(24));
        root.setAlignment(Pos.CENTER);
        root.setStyle(
                "-fx-background-color: linear-gradient(to bottom right, #E0F2FE, #F9FAFB);"
        );

        Scene scene = new Scene(root, 800, 520);
        try {
            scene.getStylesheets().add(
                    getClass().getResource("/resources/css/theme.css").toExternalForm()
            );
        } catch (Exception ignored) {}

        stage.setScene(scene);
        stage.show();
    }
}
