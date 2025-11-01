import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class AdminDashboard {
    private Inventory inventory;

    public AdminDashboard(Inventory inventory) {
        this.inventory = inventory;
    }

    public void start(Stage stage) {
        Label title = new Label("Admin Dashboard");

        TableView<Medicine> table = new TableView<>();
        ObservableList<Medicine> data = FXCollections.observableArrayList(inventory.getAllMedicines());

        TableColumn<Medicine, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getName()));

        TableColumn<Medicine, Number> priceCol = new TableColumn<>("Price");
        priceCol.setCellValueFactory(c -> new javafx.beans.property.SimpleDoubleProperty(c.getValue().getPrice()));

        TableColumn<Medicine, Number> qtyCol = new TableColumn<>("Quantity");
        qtyCol.setCellValueFactory(c -> new javafx.beans.property.SimpleIntegerProperty(c.getValue().getQuantity()));

        table.getColumns().addAll(nameCol, priceCol, qtyCol);
        table.setItems(data);

        TextField nameField = new TextField();
        nameField.setPromptText("Name");
        TextField priceField = new TextField();
        priceField.setPromptText("Price");
        TextField qtyField = new TextField();
        qtyField.setPromptText("Quantity");

        Button addBtn = new Button("Add Medicine");
        addBtn.setOnAction(e -> {
            try {
                if (nameField.getText().isEmpty() || priceField.getText().isEmpty() || qtyField.getText().isEmpty())
                    return;

                double price = Double.parseDouble(priceField.getText());
                int qty = Integer.parseInt(qtyField.getText());

                Medicine m = new Medicine(data.size() + 1, nameField.getText(), "General", price, qty, "2026-01-01");
                inventory.addMedicine(m);
                data.setAll(inventory.getAllMedicines());
            } catch (NumberFormatException ex) {
                Alert alert = new Alert(Alert.AlertType.ERROR, "Price and Quantity must be numbers");
                alert.showAndWait();
            }
        });

        Button backBtn = new Button("Back");
        backBtn.setOnAction(e -> new Main().start(stage));

        VBox root = new VBox(10, title, table, nameField, priceField, qtyField, addBtn, backBtn);
        root.setStyle("-fx-padding: 20;");
        stage.setScene(new Scene(root, 600, 400));
        stage.show();
    }
}
