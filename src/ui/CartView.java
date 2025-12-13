package ui;

import javafx.animation.FadeTransition;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;
import models.Medicine;
import models.OrderItem;
import services.OrderService;
import utils.Session;
import utils.DBHelper;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class CartView {

    private final ObservableList<OrderItem> cartData;
    private final OrderService orderService;

    // single-window: we just keep reference to the SAME Stage
    private Stage stage;

    public CartView(ObservableList<OrderItem> cartData, OrderService orderService) {
        this.cartData = cartData;
        this.orderService = orderService;
    }

    public void show(Stage stage) {
        this.stage = stage;  // remember primary stage
        stage.setTitle("Cart");

        /* ---------- Title & Subtitle ---------- */
        Label title = new Label("Your Cart");
        title.setStyle(
                "-fx-font-size: 24px;" +
                        "-fx-font-weight: 700;" +
                        "-fx-text-fill: #111827;" +
                        "-fx-letter-spacing: 0.5px;"
        );

        Label subtitle = new Label("Review your selected medicines before checkout.");
        subtitle.setStyle(
                "-fx-font-size: 13px;" +
                        "-fx-text-fill: #6B7280;"
        );

        VBox headerText = new VBox(4, title, subtitle);
        headerText.setAlignment(Pos.CENTER_LEFT);

        /* ---------- Back button (pill style) ---------- */
        Button backBtn = new Button("← Back");
        backBtn.getStyleClass().add("warning-button");
        backBtn.setStyle(
                "-fx-background-radius: 999;" +
                        "-fx-font-size: 13px;" +
                        "-fx-font-weight: 500;" +
                        "-fx-padding: 6 16;"
        );
        backBtn.setOnAction(e -> {
            CustomerDashboard dashboard = new CustomerDashboard();
            dashboard.show(stage);
        });

        HBox headerRow = new HBox(12, backBtn, headerText);
        headerRow.setAlignment(Pos.CENTER_LEFT);

        /* ---------- Table ---------- */
        TableView<OrderItem> table = new TableView<>(cartData);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        table.setPrefHeight(280);
        table.setFixedCellSize(34);
        table.setPlaceholder(new Label("Your cart is empty.\nAdd medicines from the dashboard."));

        TableColumn<OrderItem, String> nameCol = new TableColumn<>("Medicine");
        nameCol.setCellValueFactory(cellData -> {
            Medicine m = cellData.getValue().getMedicine();
            return (m != null) ? m.nameProperty() : new SimpleStringProperty("");
        });
        nameCol.setStyle("-fx-alignment: CENTER-LEFT;");

        TableColumn<OrderItem, String> categoryCol = new TableColumn<>("Category");
        categoryCol.setCellValueFactory(cellData -> {
            Medicine m = cellData.getValue().getMedicine();
            return (m != null) ? m.categoryProperty() : new SimpleStringProperty("");
        });
        categoryCol.setStyle("-fx-alignment: CENTER;");

        TableColumn<OrderItem, Double> priceCol = new TableColumn<>("Unit Price (BDT)");
        priceCol.setCellValueFactory(cellData -> {
            Medicine m = cellData.getValue().getMedicine();
            return (m != null) ? m.priceProperty().asObject() : null;
        });
        priceCol.setStyle("-fx-alignment: CENTER-RIGHT;");

        TableColumn<OrderItem, Integer> qtyCol = new TableColumn<>("Qty");
        qtyCol.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        qtyCol.setStyle("-fx-alignment: CENTER;");

        TableColumn<OrderItem, Double> totalCol = new TableColumn<>("Line Total");
        totalCol.setCellValueFactory(new PropertyValueFactory<>("totalPrice"));
        totalCol.setStyle("-fx-alignment: CENTER-RIGHT;");

        table.getColumns().addAll(nameCol, categoryCol, priceCol, qtyCol, totalCol);

        table.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-border-color: transparent;" +
                        "-fx-table-cell-border-color: rgba(148,163,184,0.35);" +
                        "-fx-padding: 4;"
        );

        /* ---------- Total + item count ---------- */
        Label totalPriceLabel = new Label();
        totalPriceLabel.setStyle(
                "-fx-font-size: 17px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-text-fill: #16a34a;"
        );

        Label countLabel = new Label();
        countLabel.setStyle(
                "-fx-font-size: 12px;" +
                        "-fx-text-fill: #6B7280;"
        );

        updateTotalLabel(totalPriceLabel, countLabel);
        cartData.addListener((ListChangeListener<OrderItem>) change -> updateTotalLabel(totalPriceLabel, countLabel));

        VBox totalBox = new VBox(2, totalPriceLabel, countLabel);
        totalBox.setAlignment(Pos.CENTER_LEFT);

        /* ---------- Buttons: Remove + Checkout ---------- */
        Button removeBtn = new Button("Remove Selected");
        removeBtn.getStyleClass().add("danger-button");
        removeBtn.setStyle(
                "-fx-background-radius: 999;" +
                        "-fx-font-size: 13px;" +
                        "-fx-font-weight: 500;" +
                        "-fx-padding: 6 16;"
        );
        removeBtn.setOnAction(e -> {
            OrderItem selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) {
                new Alert(Alert.AlertType.INFORMATION, "Please select an item to remove.").showAndWait();
                return;
            }
            cartData.remove(selected);
        });

        Button checkoutBtn = new Button("Proceed to Payment");
        checkoutBtn.getStyleClass().add("primary-button");
        checkoutBtn.setStyle(
                "-fx-background-radius: 999;" +
                        "-fx-font-size: 13px;" +
                        "-fx-font-weight: 600;" +
                        "-fx-padding: 8 18;"
        );
        checkoutBtn.setOnAction(e -> {
            if (cartData == null || cartData.isEmpty()) {
                new Alert(Alert.AlertType.INFORMATION, "Your cart is empty. Add some items first.").showAndWait();
                return;
            }

            if (!Session.isLoggedIn()) {
                new Alert(Alert.AlertType.ERROR, "You must be logged in to place an order.").showAndWait();
                return;
            }

            // Snapshot for DB + Bill
            ObservableList<OrderItem> billItemsSnapshot =
                    FXCollections.observableArrayList(cartData);

            double total = billItemsSnapshot.stream()
                    .mapToDouble(OrderItem::getTotalPrice)
                    .sum();

            // Use same stage as owner (single window)
            CardPaymentDialog.show(stage, success -> {
                if (success) {
                    storeOrderInDatabase(billItemsSnapshot);
                }
            }, total);
        });

        HBox actionsRight = new HBox(10, removeBtn, checkoutBtn);
        actionsRight.setAlignment(Pos.CENTER_RIGHT);

        HBox bottomRow = new HBox(20, totalBox, actionsRight);
        bottomRow.setAlignment(Pos.CENTER);
        HBox.setHgrow(actionsRight, Priority.ALWAYS);

        /* ---------- Card container (glass look) ---------- */
        VBox card = new VBox(18,
                headerRow,
                table,
                bottomRow
        );
        card.setPadding(new Insets(20));
        card.setAlignment(Pos.TOP_CENTER);
        card.setStyle(
                "-fx-background-color: rgba(255,255,255,0.95);" +
                        "-fx-background-radius: 26;" +
                        "-fx-effect: dropshadow(gaussian, rgba(15,23,42,0.18), 28, 0.25, 0, 8);"
        );

        /* ---------- Root with soft gradient background ---------- */
        VBox root = new VBox(card);
        root.setPadding(new Insets(24));
        root.setAlignment(Pos.CENTER);
        root.setStyle(
                "-fx-background-color: linear-gradient(to bottom right, #E0F2FE, #F9FAFB);"
        );

        Scene scene = new Scene(root, 820, 520);
        try {
            scene.getStylesheets().add(
                    getClass().getResource("/resources/css/theme.css").toExternalForm()
            );
        } catch (Exception ignore) {}

        // Fade-in animation on the card
        card.setOpacity(0);
        FadeTransition ft = new FadeTransition(Duration.millis(260), card);
        ft.setFromValue(0);
        ft.setToValue(1);
        ft.play();

        stage.setScene(scene);
        stage.show();
    }

    private void updateTotalLabel(Label totalPriceLabel, Label countLabel) {
        double total = cartData.stream()
                .mapToDouble(OrderItem::getTotalPrice)
                .sum();
        int count = cartData.size();

        totalPriceLabel.setText("Total: " + String.format("%.2f", total) + " BDT");
        countLabel.setText(count == 0
                ? "No items in cart."
                : count + (count == 1 ? " item" : " items") + " selected.");
    }

    /**
     * Store the order in the database after successful payment,
     * reduce stock in medicines, then open BillView on same Stage.
     */
    private void storeOrderInDatabase(ObservableList<OrderItem> orderItems) {
        if (!Session.isLoggedIn()) {
            new Alert(Alert.AlertType.ERROR, "You must be logged in to place an order.").showAndWait();
            return;
        }

        for (OrderItem item : orderItems) {
            if (item.getMedicine() == null || item.getMedicine().getId() == 0) {
                new Alert(Alert.AlertType.ERROR, "Invalid item in cart. Please check your selection.").showAndWait();
                return;
            }
        }

        int userId = Session.getCurrentUserId();

        double total = orderItems.stream().mapToDouble(OrderItem::getTotalPrice).sum();
        String orderDate = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                .format(new java.util.Date());

        String orderSql =
                "INSERT INTO order_items(user_id, medicine_id, quantity, total_price, order_date) " +
                        "VALUES(?, ?, ?, ?, ?)";

        // use serial_number (med.getId())
        String stockSql =
                "UPDATE medicines " +
                        "SET quantity = quantity - ?, " +
                        "    last_updated = strftime('%s','now') " +
                        "WHERE serial_number = ? AND quantity >= ?";

        try (Connection conn = DBHelper.getConnection()) {
            conn.setAutoCommit(false);

            try (PreparedStatement orderStmt = conn.prepareStatement(orderSql);
                 PreparedStatement stockStmt = conn.prepareStatement(stockSql)) {

                for (OrderItem item : orderItems) {
                    Medicine med = item.getMedicine();

                    // ---- INSERT INTO order_items ----
                    orderStmt.setInt(1, userId);
                    orderStmt.setInt(2, med.getId());
                    orderStmt.setInt(3, item.getQuantity());
                    orderStmt.setDouble(4, item.getTotalPrice());
                    orderStmt.setString(5, orderDate);
                    orderStmt.addBatch();

                    // ---- UPDATE medicines.quantity ----
                    stockStmt.setInt(1, item.getQuantity());
                    stockStmt.setInt(2, med.getId());
                    stockStmt.setInt(3, item.getQuantity());
                    stockStmt.addBatch();
                }

                orderStmt.executeBatch();
                int[] stockResults = stockStmt.executeBatch();

                for (int res : stockResults) {
                    if (res == 0) {
                        conn.rollback();
                        new Alert(Alert.AlertType.ERROR,
                                "Stock update failed for one of the medicines. Please try again.")
                                .showAndWait();
                        return;
                    }
                }

                conn.commit();
            } catch (SQLException inner) {
                conn.rollback();
                throw inner;
            }

            // Update in-memory quantities
            for (OrderItem item : orderItems) {
                Medicine med = item.getMedicine();
                if (med != null) {
                    int newQty = med.getQuantity() - item.getQuantity();
                    med.setQuantity(Math.max(newQty, 0));
                }
            }

            // Clear real cart
            cartData.clear();

            // Show BillView on same Stage
            BillView billView = new BillView(orderItems, total, orderDate);
            billView.show(stage);

            new Alert(Alert.AlertType.INFORMATION, "Order placed successfully!").showAndWait();

        } catch (SQLException e) {
            new Alert(Alert.AlertType.ERROR,
                    "Database error while placing order: " + e.getMessage()).showAndWait();
        }
    }
}
