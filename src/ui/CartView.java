package ui;

import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import models.OrderItem;

public class CartView {
    private ObservableList<OrderItem> cartData;  // Observable list to track cart items

    public CartView(ObservableList<OrderItem> cartData) {
        this.cartData = cartData;  // Initialize cartData with passed ObservableList
    }

    public void show(Stage stage) {
        // Title for Cart View
        Label title = new Label("Shopping Cart");
        title.setStyle("-fx-font-size: 36px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        // Create a table to display cart items
        TableView<OrderItem> table = new TableView<>();
        table.setStyle("-fx-border-color: #BDC3C7; -fx-border-width: 1px; -fx-font-size: 14px;");

        // Medicine Name Column
        TableColumn<OrderItem, String> nameCol = new TableColumn<>("Medicine Name");
        nameCol.setCellValueFactory(c -> c.getValue().medicineProperty().get().nameProperty());
        nameCol.setStyle("-fx-alignment: center; -fx-font-weight: bold;");

        // Quantity Column
        TableColumn<OrderItem, Integer> qtyCol = new TableColumn<>("Quantity");
        qtyCol.setCellValueFactory(c -> c.getValue().quantityProperty().asObject());
        qtyCol.setStyle("-fx-alignment: center;");

        // Price Column
        TableColumn<OrderItem, Double> priceCol = new TableColumn<>("Price");
        priceCol.setCellValueFactory(c -> c.getValue().medicineProperty().get().priceProperty().asObject());
        priceCol.setStyle("-fx-alignment: center;");

        // Total Price Column
        TableColumn<OrderItem, Double> totalPriceCol = new TableColumn<>("Total Price");
        totalPriceCol.setCellValueFactory(c -> c.getValue().totalPriceProperty().asObject());
        totalPriceCol.setStyle("-fx-alignment: center;");

        // Add columns to table
        table.getColumns().addAll(nameCol, qtyCol, priceCol, totalPriceCol);
        table.setItems(cartData);

        // Calculate total cart price
        double totalPrice = cartData.stream().mapToDouble(item -> item.getTotalPrice()).sum();

        // Total Price Label
        Label totalPriceLabel = new Label("Total: $" + String.format("%.2f", totalPrice));
        totalPriceLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #E74C3C;");

        // Button for checkout (Proceed to Payment)
        Button checkoutBtn = new Button("Proceed to Payment");
        checkoutBtn.setStyle("-fx-background-color: #3498DB; -fx-text-fill: white; -fx-font-size: 16px; -fx-padding: 10px 20px;");
        checkoutBtn.setOnAction(e -> {
            // Proceed with payment logic here
            System.out.println("Proceeding to payment...");
            // You can add logic here to navigate to a payment screen or show a payment form.
        });

        // Back button to go back to the customer dashboard
        Button backBtn = new Button("Back to Dashboard");
        backBtn.setStyle("-fx-background-color: #f44336; -fx-text-fill: white; -fx-font-size: 16px; -fx-padding: 10px 20px;");
        backBtn.setOnAction(e -> {
            new CustomerDashboard().show(stage);  // Go back to customer dashboard
        });

        // Layout
        VBox vbox = new VBox(20, title, table, totalPriceLabel, checkoutBtn, backBtn);
        vbox.setPadding(new Insets(20));
        vbox.setAlignment(Pos.CENTER);
        vbox.setStyle("-fx-background-color: #ECF0F1;");

        // Set scene and show stage
        Scene scene = new Scene(vbox, 800, 500);
        stage.setScene(scene);
        stage.setTitle("Cart View");
        stage.show();
    }
}
