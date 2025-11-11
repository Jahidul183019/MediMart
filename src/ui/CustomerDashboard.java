package ui;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import models.Inventory;
import models.Medicine;
import models.OrderItem;
import services.MedicineService;
import utils.AppException;
import utils.FileLogger;

import java.net.URL;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class CustomerDashboard {

    private final Inventory inventory;
    private final MedicineService medicineService;
    private final boolean guestMode;

    private final ObservableList<Medicine> medicinesData;
    private final ObservableList<OrderItem> cartData;

    // UI members
    private VBox categoryContainer;
    private TextField searchField;
    private ComboBox<String> sortBox;
    private Label statusBar;
    private Label cartBadge;
    private Button cartButton;
    private Button profileButton;

    private final Map<String, Image> imageCache = new ConcurrentHashMap<>();
    private ScheduledExecutorService poller;
    private static final int POLL_SECS = 0;

    private volatile long lastRefreshMs = 0L;
    private String lastDataSignature = "";

    // --- Constructors ---
    public CustomerDashboard() { this(Inventory.getInstance(), new MedicineService(), false); }
    public CustomerDashboard(Inventory inventory, MedicineService medicineService) { this(inventory, medicineService, false); }
    public CustomerDashboard(Inventory inventory, MedicineService medicineService, boolean guestMode) {
        this.inventory = inventory;
        this.medicineService = medicineService;
        this.guestMode = guestMode;
        this.medicinesData = FXCollections.observableArrayList();
        this.cartData = FXCollections.observableArrayList();
    }

    public void show(Stage stage) {
        Label title = new Label("Customer Dashboard");
        title.setStyle("-fx-font-size: 30px; -fx-font-weight: bold; "
                + "-fx-text-fill: linear-gradient(to right, #26A69A, #2196F3);");
        title.setEffect(new DropShadow(3, Color.color(0, 0, 0, 0.3)));

        Node profileNode = buildIconButton("/images/profile.png", "Profile", "#2196F3", e -> viewProfile(stage));
        Node cartNode = buildCartButton("/images/cart.png", "Cart", "#2196F3", e -> viewCart(stage));
        HBox topButtons = new HBox(12, profileNode, cartNode);
        topButtons.setAlignment(Pos.CENTER_RIGHT);

        searchField = new TextField();
        searchField.setPromptText("Search medicine...");
        styleSearch(searchField);
        searchField.setMinWidth(260);
        searchField.setOnKeyReleased(e -> refreshCategoryView());

        sortBox = new ComboBox<>();
        sortBox.getItems().addAll("Sort by Price: Low → High", "Sort by Price: High → Low");
        sortBox.setValue("Sort by Price: Low → High");
        sortBox.setOnAction(e -> refreshCategoryView());

        HBox controls = new HBox(14, searchField, sortBox);
        controls.setAlignment(Pos.CENTER_LEFT);

        statusBar = new Label("Ready");
        statusBar.setStyle("-fx-text-fill: rgba(0,0,0,0.65);");
        VBox header = new VBox(4, title, topButtons, controls, statusBar);
        header.setPadding(new Insets(10));

        // === Category container ===
        categoryContainer = new VBox(20);
        categoryContainer.setPadding(new Insets(20, 20, 40, 20));

        ScrollPane scroller = new ScrollPane(categoryContainer);
        scroller.setFitToWidth(true);
        scroller.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroller.setStyle("-fx-background-color: transparent; -fx-background-insets: 0;");

        Button back = new Button("Back");
        back.setStyle("-fx-background-color: linear-gradient(to right, #f44336, #e53935); "
                + "-fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 20; -fx-padding: 6 14;");
        back.setOnAction(e -> new Main().start(stage));
        HBox backBox = new HBox(back);
        backBox.setAlignment(Pos.CENTER);
        backBox.setPadding(new Insets(10));

        BorderPane root = new BorderPane();
        root.setTop(header);
        root.setCenter(scroller);
        root.setBottom(backBox);
        root.setStyle("-fx-background-color: linear-gradient(to bottom right, #E0F7FA, #E8F5E9);");

        Scene scene = new Scene(root, 1000, 700);
        stage.setScene(scene);
        stage.setTitle("Customer Dashboard");
        stage.show();

        refreshMedicines();
        try { medicineService.addChangeListener(this::refreshMedicines); } catch (Throwable ignored) {}

        if (POLL_SECS > 0) {
            poller = Executors.newSingleThreadScheduledExecutor();
            poller.scheduleAtFixedRate(() -> Platform.runLater(this::refreshMedicines),
                    POLL_SECS, POLL_SECS, TimeUnit.SECONDS);
        }

        stage.setOnCloseRequest(e -> {
            if (poller != null) poller.shutdownNow();
        });

        updateCartBadge();
    }

    private void viewProfile(Stage stage) {
        new ProfileView().show(stage);
    }

    private void refreshCategoryView() {
        categoryContainer.getChildren().clear();
        List<Medicine> filtered = getFilteredSorted();
        Map<String, List<Medicine>> grouped = filtered.stream()
                .collect(Collectors.groupingBy(m -> safe(m.getCategory()), TreeMap::new, Collectors.toList()));

        for (var entry : grouped.entrySet()) {
            String category = entry.getKey();
            List<Medicine> meds = entry.getValue();

            if (meds.isEmpty()) continue;

            Label header = new Label(category);
            header.setStyle("""
                -fx-font-size: 20px;
                -fx-font-weight: bold;
                -fx-text-fill: white;
                -fx-padding: 6 14;
                -fx-background-radius: 10;
                -fx-background-color: linear-gradient(to right, #26A69A, #2196F3);
            """);

            GridPane grid = new GridPane();
            grid.setHgap(22);
            grid.setVgap(22);
            grid.setPadding(new Insets(10, 0, 0, 0));

            int col = 0, row = 0;
            for (Medicine m : meds) {
                VBox card = createMedicineCard(m);
                grid.add(card, col, row);
                col++;
                if (col > 3) { col = 0; row++; }
            }

            VBox section = new VBox(10, header, grid);
            section.setPadding(new Insets(8, 0, 12, 0));
            categoryContainer.getChildren().add(section);
        }

        if (filtered.isEmpty()) {
            Label empty = new Label("No medicines found.");
            empty.setStyle("-fx-text-fill: rgba(0,0,0,0.6); -fx-font-size: 15px;");
            categoryContainer.getChildren().add(empty);
        }
    }

    private List<Medicine> getFilteredSorted() {
        String q = (searchField == null || searchField.getText() == null) ? "" : searchField.getText().trim().toLowerCase();

        List<Medicine> filtered = medicinesData.stream()
                .filter(m -> q.isBlank()
                        || (m.getName() != null && m.getName().toLowerCase().contains(q))
                        || (m.getCategory() != null && m.getCategory().toLowerCase().contains(q)))
                .collect(Collectors.toList());

        Comparator<Medicine> cmp;
        String sortOpt = sortBox != null ? sortBox.getValue() : "Sort by Price: Low → High";
        if ("Sort by Price: High → Low".equals(sortOpt)) {
            cmp = Comparator.comparingDouble(Medicine::getPrice).reversed();
        } else {
            cmp = Comparator.comparingDouble(Medicine::getPrice);
        }
        filtered.sort(cmp);
        return filtered;
    }

    private VBox createMedicineCard(Medicine m) {
        Node imageNode = loadImageNodeForMedicine(m);
        Label name = new Label(safe(m.getName()));
        name.setStyle("-fx-font-size: 15px; -fx-font-weight: bold;");

        Label price = new Label("Price: $" + m.getPrice());
        Label expiry = new Label("Expiry: " + safe(m.getExpiryDate()));

        Label stock = new Label(m.getQuantity() > 0 ? "In Stock: " + m.getQuantity() : "Out of Stock");
        stock.setStyle(m.getQuantity() > 0
                ? "-fx-text-fill: #2e7d32; -fx-font-weight: bold;"
                : "-fx-text-fill: #c62828; -fx-font-weight: bold;");

        Spinner<Integer> qtySpinner = new Spinner<>(1, Math.max(1, m.getQuantity()), 1);
        qtySpinner.setDisable(m.getQuantity() <= 0);

        Button addBtn = new Button("Add to Cart");
        addBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-background-radius: 18;");
        addBtn.setDisable(m.getQuantity() <= 0);
        addBtn.setOnAction(e -> {
            int qty = qtySpinner.getValue();
            addToCart(m, qty);
            updateCartBadge();
        });

        VBox card = new VBox(8, imageNode, name, price, expiry, stock, qtySpinner, addBtn);
        card.setPadding(new Insets(12));
        card.setAlignment(Pos.CENTER);
        card.setStyle("""
            -fx-border-color: lightgray;
            -fx-border-width: 1px;
            -fx-background-color: rgba(255,255,255,0.96);
            -fx-background-radius: 14;
            -fx-border-radius: 14;
        """);

        card.setOnMouseEntered(e -> card.setStyle("""
            -fx-border-color: #26A69A;
            -fx-border-width: 2px;
            -fx-background-color: #E0F2F1;
            -fx-background-radius: 14;
            -fx-border-radius: 14;
        """));
        card.setOnMouseExited(e -> card.setStyle("""
            -fx-border-color: lightgray;
            -fx-border-width: 1px;
            -fx-background-color: rgba(255,255,255,0.96);
            -fx-background-radius: 14;
            -fx-border-radius: 14;
        """));

        return card;
    }

    private Node loadImageNodeForMedicine(Medicine m) {
        Image img = null;
        String p = (m.getImagePath() == null || m.getImagePath().isBlank()) ? null : m.getImagePath().trim();
        if (p != null) img = loadImageFile(p, 140, 120);
        if (img == null && m.getName() != null)
            img = loadImageFromClasspath("/images/" + m.getName().trim() + ".jpg", 140, 120);
        if (img == null)
            img = loadImageFromClasspath("/images/Placeholder.jpg", 140, 120);

        ImageView iv = new ImageView(img);
        iv.setFitWidth(140);
        iv.setFitHeight(120);
        iv.setPreserveRatio(true);

        Rectangle clip = new Rectangle(140, 120);
        clip.setArcWidth(16);
        clip.setArcHeight(16);
        iv.setClip(clip);
        iv.setEffect(new DropShadow(6, Color.rgb(0, 0, 0, 0.25)));

        return iv;
    }

    private Image loadImageFromClasspath(String classpath, int w, int h) {
        if (classpath == null) return null;
        return imageCache.computeIfAbsent(classpath, k -> {
            try {
                URL url = getClass().getResource(k);
                if (url != null) return new Image(url.toExternalForm(), w, h, true, true, true);
            } catch (Exception ignored) {}
            return new Image("file:src/resources/images/Placeholder.jpg", w, h, true, true, true);
        });
    }

    private Image loadImageFile(String path, int w, int h) {
        return imageCache.computeIfAbsent(path, k ->
                new Image("file:" + k, w, h, true, true, true));
    }

    private void addToCart(Medicine medicine, int quantity) {
        cartData.add(new OrderItem(medicine, quantity));
    }

    private void viewCart(Stage stage) {
        new CartView(cartData).show(stage);
    }

    public void refreshMedicines() {
        long now = System.currentTimeMillis();
        if (now - lastRefreshMs < 1500) return;
        lastRefreshMs = now;
        setStatus("Refreshing...");
        CompletableFuture.supplyAsync(() -> {
            try {
                return medicineService.getAllMedicines();
            } catch (Exception e) {
                FileLogger.error("Refresh failed: " + e.getMessage(), e);
                return null;
            }
        }).thenAccept(list -> Platform.runLater(() -> {
            if (list != null) {
                String sig = signature(list);
                if (!sig.equals(lastDataSignature)) {
                    lastDataSignature = sig;
                    medicinesData.setAll(list);
                    refreshCategoryView();
                }
                setStatus("Refreshed");
            } else setStatus("Refresh failed");
        }));
    }

    private static String signature(List<Medicine> list) {
        StringBuilder sb = new StringBuilder();
        for (Medicine m : list) {
            sb.append(m.getId()).append('|')
                    .append(m.getName()).append('|')
                    .append(m.getCategory()).append('|')
                    .append(m.getPrice()).append('|')
                    .append(m.getQuantity()).append('\n');
        }
        return sb.toString();
    }

    private static String safe(String s) { return s == null ? "" : s; }
    private void setStatus(String text) { if (statusBar != null) statusBar.setText(text); }

    private Node buildIconButton(String icon, String tooltip, String color, javafx.event.EventHandler<javafx.event.ActionEvent> click) {
        profileButton = new Button();
        profileButton.setStyle("-fx-background-color: " + color + "; -fx-background-radius: 999; -fx-padding: 8 12;");
        profileButton.setOnAction(click);
        ImageView iv = loadIcon(icon);
        Label lbl = new Label(tooltip);
        lbl.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");
        HBox box = new HBox(6, iv, lbl);
        profileButton.setGraphic(box);
        return profileButton;
    }

    private Node buildCartButton(String icon, String tooltip, String color, javafx.event.EventHandler<javafx.event.ActionEvent> click) {
        cartButton = new Button();
        cartButton.setStyle("-fx-background-color: " + color + "; -fx-background-radius: 999; -fx-padding: 8 14;");
        cartButton.setOnAction(click);
        ImageView iv = loadIcon(icon);
        cartBadge = new Label();
        cartBadge.setStyle("""
            -fx-background-color: #E53935; -fx-text-fill: white;
            -fx-font-size: 10px; -fx-font-weight: bold;
            -fx-background-radius: 999; -fx-padding: 1 4;
        """);
        cartBadge.setVisible(false);
        StackPane iconWithBadge = new StackPane(iv, cartBadge);
        StackPane.setAlignment(cartBadge, Pos.TOP_RIGHT);
        HBox box = new HBox(6, iconWithBadge, new Label(tooltip){{
            setStyle("-fx-text-fill:white;-fx-font-weight:bold;");
        }});
        cartButton.setGraphic(box);
        return cartButton;
    }

    private ImageView loadIcon(String path) {
        URL url = getClass().getResource(path);
        Image img = (url != null) ? new Image(url.toExternalForm(), 24, 24, true, true, true)
                : new Image("file:src/resources/images/Placeholder.jpg", 24, 24, true, true, true);
        ImageView iv = new ImageView(img);
        iv.setFitWidth(24);
        iv.setFitHeight(24);
        iv.setPreserveRatio(true);
        return iv;
    }

    private void updateCartBadge() {
        int count = cartData.size();
        if (count > 0) {
            cartBadge.setText(String.valueOf(count));
            cartBadge.setVisible(true);
        } else {
            cartBadge.setVisible(false);
        }
    }

    private static void styleSearch(TextField tf) {
        final String base = """
            -fx-background-color: rgba(255,255,255,0.96);
            -fx-background-insets: 0;
            -fx-background-radius: 10;
            -fx-border-color: #B2DFDB;
            -fx-border-radius: 10;
            -fx-border-width: 1.2;
            -fx-padding: 8 12;
            -fx-font-size: 13px;
            -fx-prompt-text-fill: rgba(0,0,0,0.45);
        """;
        tf.setStyle(base);
        tf.focusedProperty().addListener((obs, o, f) -> {
            if (f) {
                tf.setStyle(base + """
                    -fx-border-color: #26A69A;
                    -fx-effect: dropshadow(gaussian, rgba(38,166,154,0.3), 10, 0.35, 0, 0);
                """);
            } else tf.setStyle(base + "-fx-effect: null;");
        });
    }
}
