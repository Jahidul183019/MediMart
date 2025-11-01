import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import models.Inventory;
import models.Medicine;
import models.Order;  // ✅ Add this import

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

public class CustomerDashboard {
    private final Inventory inventory;
    private GridPane grid;
    private List<Medicine> displayedMedicines;
    private ComboBox<String> sortBox;
    private TextField searchField;

    public CustomerDashboard(Inventory inventory) {
        this.inventory = inventory;
        this.displayedMedicines = inventory.getAllMedicines();
    }

    public void start(Stage stage) {
        // Title
        Label title = new Label("Customer Dashboard");
        title.setFont(new Font("Arial Bold", 24));
        title.setPadding(new Insets(10));

        // Search bar
        searchField = new TextField();
        searchField.setPromptText("Search medicine...");
        searchField.setMinWidth(200);
        searchField.setOnKeyReleased(this::handleSearch);

        // Sorting ComboBox
        sortBox = new ComboBox<>();
        sortBox.getItems().addAll("Sort by Price: Low → High", "Sort by Price: High → Low");
        sortBox.setValue("Sort by Price: Low → High");
        sortBox.setOnAction(e -> refreshGrid());

        HBox topControls = new HBox(20, searchField, sortBox);
        topControls.setAlignment(Pos.CENTER);
        topControls.setPadding(new Insets(10));

        // Grid for medicines
        grid = new GridPane();
        grid.setHgap(20);
        grid.setVgap(20);
        grid.setPadding(new Insets(20));

        refreshGrid();

        // Back button
        Button back = new Button("Back");
        back.setStyle("-fx-background-color: #f44336; -fx-text-fill: white; -fx-font-size: 14px;");
        back.setOnAction(e -> new Main().start(stage));

        HBox backBox = new HBox(back);
        backBox.setAlignment(Pos.CENTER);
        backBox.setPadding(new Insets(10));

        // Layout
        BorderPane root = new BorderPane();
        root.setTop(new VBox(title, topControls));
        BorderPane.setAlignment(title, Pos.CENTER);
        root.setCenter(grid);
        root.setBottom(backBox);

        // Set background image
        Path bgPath = Paths.get("src/resources/images/background.jpg");
        if (bgPath.toFile().exists()) {
            Image bgImg = new Image(bgPath.toUri().toString(), 1000, 700, false, true);
            BackgroundImage bg = new BackgroundImage(bgImg,
                    BackgroundRepeat.NO_REPEAT,
                    BackgroundRepeat.NO_REPEAT,
                    BackgroundPosition.CENTER,
                    BackgroundSize.DEFAULT);
            root.setBackground(new Background(bg));
        } else {
            root.setStyle("-fx-background-color: #f0f8ff;"); // fallback color
        }

        Scene scene = new Scene(root, 1000, 700);
        stage.setScene(scene);
        stage.setTitle("Customer Dashboard");
        stage.show();
    }

    /** Handle search filter */
    private void handleSearch(KeyEvent e) {
        String query = searchField.getText().toLowerCase().trim();

        displayedMedicines = inventory.getAllMedicines().stream()
                .filter(m -> m.getName().toLowerCase().contains(query) ||
                        m.getCategory().toLowerCase().contains(query))
                .collect(Collectors.toList());

        refreshGrid();
    }

    /** Refresh the grid display */
    private void refreshGrid() {
        grid.getChildren().clear();

        // Sort by price
        if ("Sort by Price: Low → High".equals(sortBox.getValue())) {
            displayedMedicines.sort((a, b) -> Double.compare(a.getPrice(), b.getPrice()));
        } else {
            displayedMedicines.sort((a, b) -> Double.compare(b.getPrice(), a.getPrice()));
        }

        int col = 0, row = 0;
        for (Medicine m : displayedMedicines) {
            VBox card = createMedicineCard(m);
            grid.add(card, col, row);
            col++;
            if (col > 3) col = 0; // 4 per row
            if (col == 0) row++;
        }
    }

    private VBox createMedicineCard(Medicine m) {
        Node imageNode = loadImageNode(m.getName());

        Label name = new Label(m.getName());
        name.setFont(new Font(16));

        Label category = new Label(m.getCategory());
        Label price = new Label("Price: $" + m.getPrice());

        HBox qtyBox = new HBox(5);
        qtyBox.setAlignment(Pos.CENTER);
        Label qtyLabel = new Label("Qty:");
        Spinner<Integer> qtySpinner = new Spinner<>(1, 100, 1);
        qtyBox.getChildren().addAll(qtyLabel, qtySpinner);

        Label expiry = new Label("Expiry: " + m.getExpiryDate());

        Button orderBtn = new Button("Order");
        orderBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");
        orderBtn.setOnAction(e -> {
            int qty = qtySpinner.getValue();
            Order order = new Order(); // ✅ Now recognized
            order.addToCart(m, qty);
            System.out.println("Ordered: " + m.getName() + " x" + qty);
        });

        VBox card = new VBox(5, imageNode, name, category, price, qtyBox, expiry, orderBtn);
        card.setPadding(new Insets(10));
        card.setAlignment(Pos.CENTER);
        card.setStyle("-fx-border-color: lightgray; -fx-border-width: 1px; -fx-background-color: rgba(255,255,255,0.9);");

        // Hover effect
        card.setOnMouseEntered(e -> card.setStyle("-fx-border-color: #4CAF50; -fx-border-width: 2px; -fx-background-color: #e0f2f1;"));
        card.setOnMouseExited(e -> card.setStyle("-fx-border-color: lightgray; -fx-border-width: 1px; -fx-background-color: rgba(255,255,255,0.9);"));

        // Tooltip
        Tooltip tooltip = new Tooltip("Category: " + m.getCategory() + "\nExpiry: " + m.getExpiryDate());
        Tooltip.install(card, tooltip);

        return card;
    }

    /** Load image from project images folder */
    private Node loadImageNode(String medicineName) {
        Image img = loadImageFromFile(medicineName.trim());
        if (img == null) img = loadImageFromFile("Placeholder");
        if (img != null) return buildImageView(img, medicineName);

        Rectangle r = new Rectangle(120, 120);
        r.setFill(Color.LIGHTGRAY);
        VBox box = new VBox(4, r, new Label(medicineName));
        box.setAlignment(Pos.CENTER);
        return box;
    }

    /** Load image from filesystem */
    private Image loadImageFromFile(String name) {
        try {
            Image img = new Image("file:src/resources/images/" + name + ".jpg", 120, 120, true, true);
            if (!img.isError()) return img;
        } catch (Exception e) {
            System.out.println("Failed to load image: " + name);
        }
        return null;
    }

    private ImageView buildImageView(Image img, String tip) {
        ImageView iv = new ImageView(img);
        iv.setFitWidth(120);
        iv.setFitHeight(120);
        iv.setPreserveRatio(true);
        Tooltip.install(iv, new Tooltip(tip));
        return iv;
    }
}
