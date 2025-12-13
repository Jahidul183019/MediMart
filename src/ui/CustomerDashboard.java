package ui;

import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
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
import javafx.util.Duration;
import models.Inventory;
import models.Medicine;
import models.OrderItem;
import services.MedicineService;
import services.OrderService;
import utils.AppException;
import utils.FileLogger;
import utils.Session;

import java.net.URL;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class CustomerDashboard {

    private final Inventory inventory;
    private final MedicineService medicineService;
    private final OrderService orderService;

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
    private ScrollPane scroller;

    private final Map<String, Image> imageCache = new ConcurrentHashMap<>();
    private ScheduledExecutorService poller;
    private static final int POLL_SECS = 0;

    private volatile long lastRefreshMs = 0L;
    private String lastDataSignature = "";

    // ====== Responsive layout tuning ======
    private static final double CARD_PREF_WIDTH = 220;
    private static final double GRID_HGAP = 22;
    private static final int MIN_COLS = 1;
    private static final int MAX_COLS = 6;
    private int lastCols = -1;

    // --- Constructors ---
    public CustomerDashboard() {
        this(Inventory.getInstance(), new MedicineService(), new OrderService(), false);
    }

    public CustomerDashboard(Inventory inventory, MedicineService medicineService) {
        this(inventory, medicineService, new OrderService(), false);
    }

    public CustomerDashboard(Inventory inventory, MedicineService medicineService,
                             OrderService orderService, boolean guestMode) {
        this.inventory = inventory;
        this.medicineService = medicineService;
        this.orderService = orderService;
        this.guestMode = guestMode;
        this.medicinesData = FXCollections.observableArrayList();
        // ✅ Use per-user (or guest) cart from Session instead of a fresh list
        this.cartData = Session.getCart();
    }

    public void show(Stage stage) {
        Label title = new Label("Customer Dashboard");
        title.setStyle("-fx-font-size: 30px; -fx-font-weight: bold; "
                + "-fx-text-fill: linear-gradient(to right, #26A69A, #2196F3);");
        title.setEffect(new DropShadow(3, Color.color(0, 0, 0, 0.3)));

        // Top-right buttons: Profile, Order History, Cart
        String topBtnColor = "linear-gradient(to right, #26A69A, #2196F3)";

        Node profileNode = buildIconButton("profile.png", "Profile", topBtnColor,
                e -> viewProfile(stage));

        Node historyNode = buildIconButton("order_history.png", "Order History", topBtnColor,
                e -> openOrderHistory(stage));

        Node cartNode = buildCartButton("cart.png", "Cart", topBtnColor,
                e -> viewCart(stage));

        HBox topButtons = new HBox(12, profileNode, historyNode, cartNode);
        topButtons.setAlignment(Pos.CENTER_RIGHT);

        // Search + Sort
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

        // Status
        statusBar = new Label("Ready");
        statusBar.setStyle("-fx-text-fill: rgba(0,0,0,0.65);");

        VBox header = new VBox(4, title, topButtons, controls, statusBar);
        header.setPadding(new Insets(10));

        // Category container
        categoryContainer = new VBox(20);
        categoryContainer.setPadding(new Insets(20, 20, 40, 20));

        scroller = new ScrollPane(categoryContainer);
        scroller.setFitToWidth(true);
        scroller.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroller.setStyle("-fx-background-color: transparent; -fx-background-insets: 0;");

        scroller.viewportBoundsProperty().addListener((obs, ov, nv) -> {
            categoryContainer.setPrefWidth(nv.getWidth());
            int cols = computeCols(nv.getWidth());
            if (cols != lastCols) {
                lastCols = cols;
                refreshCategoryView();
            }
        });

        Button back = new Button("Back");
        back.setStyle("-fx-background-color: linear-gradient(to right, #f44336, #e53935); "
                + "-fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 20; -fx-padding: 6 14;");
        back.setOnAction(e -> goBackToHome(stage));

        HBox backBox = new HBox(back);
        backBox.setAlignment(Pos.CENTER);
        backBox.setPadding(new Insets(10));

        BorderPane root = new BorderPane();
        root.setTop(header);
        root.setCenter(scroller);
        root.setBottom(backBox);
        root.setStyle("-fx-background-color: linear-gradient(to bottom right, #E0F7FA, #E8F5E9);");

        Scene scene = new Scene(root, 1000, 700);
        scene.widthProperty().addListener((o, ov, nv) -> requestRelayout());
        scene.heightProperty().addListener((o, ov, nv) -> requestRelayout());

        stage.setScene(scene);
        stage.setTitle("Customer Dashboard");
        stage.show();

        // Small fade-in animation
        FadeTransition ft = new FadeTransition(Duration.millis(300), root);
        ft.setFromValue(0);
        ft.setToValue(1);
        ft.play();

        // Initial population
        refreshMedicines();
        try {
            medicineService.addChangeListener(this::refreshMedicines);
        } catch (Throwable ignored) {}

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

    // === Order history ===
    private void openOrderHistory(Stage stage) {
        if (!Session.isLoggedIn()) {
            Alert a = new Alert(Alert.AlertType.ERROR,
                    "You must be logged in to view your order history.",
                    ButtonType.OK);
            a.setHeaderText("Not logged in");
            a.showAndWait();
            return;
        }

        int userId = Session.getCurrentUserId();
        OrderHistoryView historyView = new OrderHistoryView(userId);
        historyView.show(stage); // single-window style
    }

    private void requestRelayout() {
        double w = (scroller != null && scroller.getViewportBounds() != null)
                ? scroller.getViewportBounds().getWidth()
                : categoryContainer.getWidth();

        int cols = computeCols(w);
        if (cols != lastCols) {
            lastCols = cols;
            refreshCategoryView();
        }
    }

    private int computeCols(double availableWidth) {
        if (availableWidth <= 0) return (lastCols > 0 ? lastCols : 3);
        double effective = Math.max(availableWidth - 40, CARD_PREF_WIDTH);
        int cols = (int) Math.floor((effective + GRID_HGAP) / (CARD_PREF_WIDTH + GRID_HGAP));
        cols = Math.max(MIN_COLS, Math.min(MAX_COLS, cols));
        return cols;
    }

    private void goBackToHome(Stage stage) {
        try {
            new Main().start(stage);
        } catch (Exception ex) {
            ex.printStackTrace();
            showError("Failed to open Home: " + ex.getMessage());
        }
    }

    private void viewProfile(Stage stage) {
        new ProfileView().show(stage);
    }

    // === Category View ===
    private void refreshCategoryView() {
        categoryContainer.getChildren().clear();
        java.util.List<Medicine> filtered = getFilteredSorted();

        Map<String, java.util.List<Medicine>> grouped = filtered.stream()
                .collect(Collectors.groupingBy(m -> safe(m.getCategory()), TreeMap::new, Collectors.toList()));

        int cols = (lastCols > 0) ? lastCols
                : computeCols(scroller != null && scroller.getViewportBounds() != null
                ? scroller.getViewportBounds().getWidth()
                : categoryContainer.getWidth());

        for (var entry : grouped.entrySet()) {
            String category = entry.getKey();
            java.util.List<Medicine> meds = entry.getValue();
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
            grid.setHgap(GRID_HGAP);
            grid.setVgap(22);
            grid.setPadding(new Insets(10, 0, 0, 0));

            int col = 0, row = 0;
            for (Medicine m : meds) {
                VBox card = createMedicineCard(m);
                card.setPrefWidth(CARD_PREF_WIDTH);
                grid.add(card, col, row);
                col++;
                if (col >= cols) {
                    col = 0;
                    row++;
                }
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

    private java.util.List<Medicine> getFilteredSorted() {
        String q = (searchField == null || searchField.getText() == null)
                ? ""
                : searchField.getText().trim().toLowerCase();

        java.util.List<Medicine> filtered = medicinesData.stream()
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

    // === CARD with live stock bindings ===
    private VBox createMedicineCard(Medicine m) {
        Node imageNode = loadImageNodeForMedicine(m);

        Label name = new Label(safe(m.getName()));
        name.setStyle("-fx-font-size: 15px; -fx-font-weight: bold;");

        Label price = new Label("Price : " + m.getPrice());
        Label expiry = new Label("Expiry: " + safe(m.getExpiryDate()));

        Label stock = new Label();
        stock.textProperty().bind(
                Bindings.createStringBinding(
                        () -> m.getQuantity() > 0
                                ? "In Stock: " + m.getQuantity()
                                : "Out of Stock",
                        m.quantityProperty()
                )
        );
        stock.styleProperty().bind(
                Bindings.createStringBinding(
                        () -> m.getQuantity() > 0
                                ? "-fx-text-fill: #2e7d32; -fx-font-weight: bold;"
                                : "-fx-text-fill: #c62828; -fx-font-weight: bold;",
                        m.quantityProperty()
                )
        );

        int initialQty = Math.max(1, m.getQuantity());
        Spinner<Integer> qtySpinner = new Spinner<>(1, initialQty, 1);
        qtySpinner.setEditable(false);
        qtySpinner.setDisable(m.getQuantity() <= 0);

        Button addBtn = new Button("Add to Cart");
        addBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-background-radius: 18;");
        addBtn.setDisable(m.getQuantity() <= 0);

        m.quantityProperty().addListener((obs, oldVal, newVal) -> {
            int q = newVal.intValue();
            boolean noStock = q <= 0;

            qtySpinner.setDisable(noStock);
            addBtn.setDisable(noStock);

            SpinnerValueFactory<Integer> vf = qtySpinner.getValueFactory();
            if (vf instanceof SpinnerValueFactory.IntegerSpinnerValueFactory isf) {
                int newMax = Math.max(1, q);
                isf.setMax(newMax);

                int current = qtySpinner.getValue();
                if (current > newMax) {
                    qtySpinner.getValueFactory().setValue(newMax);
                }
            }
        });

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

    // === Image loader ===
    private Node loadImageNodeForMedicine(Medicine m) {
        Image img = null;
        String p = (m.getImagePath() == null || m.getImagePath().isBlank())
                ? null
                : m.getImagePath().trim();

        if (p != null) img = loadImageFile(p, 140, 120);
        if (img == null && m.getName() != null)
            img = loadImageFromClasspath("/images/" + m.getName().trim() + ".jpg", 140, 120);
        if (img == null)
            img = loadImageFromClasspath("/images/Placeholder.jpg", 140, 120);
        if (img == null)
            img = new Image("file:src/resources/images/Placeholder.jpg", 140, 120, true, true, true);

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
            } catch (Exception ignored) { }
            return null;
        });
    }

    private Image loadImageFile(String path, int w, int h) {
        return imageCache.computeIfAbsent(path, k ->
                new Image("file:" + k, w, h, true, true, true));
    }

    // === Cart ===
    private void addToCart(Medicine medicine, int quantity) {
        int alreadyInCart = cartData.stream()
                .filter(oi -> oi.getMedicine() != null
                        && oi.getMedicine().getId() == medicine.getId())
                .mapToInt(OrderItem::getQuantity)
                .sum();

        int available = medicine.getQuantity() - alreadyInCart;
        if (available <= 0) {
            Alert a = new Alert(Alert.AlertType.WARNING,
                    "This item is out of stock.");
            a.setHeaderText("Out of Stock");
            a.showAndWait();
            return;
        }

        if (quantity > available) {
            Alert a = new Alert(Alert.AlertType.WARNING,
                    "Only " + available + " units available. Adjust your quantity.");
            a.setHeaderText("Not enough stock");
            a.showAndWait();
            return;
        }

        cartData.add(new OrderItem(medicine, quantity));
    }

    private void viewCart(Stage stage) {
        new CartView(cartData, orderService).show(stage);
    }

    // === Data refresh (async, throttled) ===
    public void refreshMedicines() {
        long now = System.currentTimeMillis();
        if (now - lastRefreshMs < 1500) return;
        lastRefreshMs = now;

        setStatus("Refreshing...");
        CompletableFuture.supplyAsync(() -> {
            try {
                return medicineService.getAllMedicines();
            } catch (AppException ex) {
                FileLogger.error("Refresh failed: " + ex.getMessage(), ex);
                return null;
            } catch (Throwable t) {
                FileLogger.error("Unexpected error: " + t.getMessage(), t);
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
            } else {
                setStatus("Refresh failed");
            }
        }));
    }

    private static String signature(java.util.List<Medicine> list) {
        StringBuilder sb = new StringBuilder();
        for (Medicine m : list) {
            sb.append(m.getId()).append('|')
                    .append(nz(m.getName())).append('|')
                    .append(nz(m.getCategory())).append('|')
                    .append(m.getPrice()).append('|')
                    .append(m.getQuantity()).append('|')
                    .append(nz(m.getExpiryDate())).append('|')
                    .append(nz(m.getImagePath())).append('\n');
        }
        return sb.toString();
    }

    private static String nz(String s) { return s == null ? "" : s; }
    private static String safe(String s) { return s == null ? "" : s; }

    private void setStatus(String text) {
        if (statusBar != null) statusBar.setText(text);
    }

    // === Buttons ===
    private Node buildIconButton(String fileName, String tooltip, String color,
                                 javafx.event.EventHandler<javafx.event.ActionEvent> click) {

        Button btn = new Button();
        btn.setOnAction(click);

        // Use same gradient as category header
        String baseStyle =
                "-fx-background-color: linear-gradient(to right, #26A69A, #2196F3);" +
                        "-fx-background-radius: 999;" +
                        "-fx-padding: 6 16;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-weight: bold;" +
                        "-fx-font-size: 13px;" +
                        "-fx-border-width: 0;" +
                        "-fx-cursor: hand;";

        String hoverStyle =
                "-fx-background-color: linear-gradient(to right, #2BBBAD, #42A5F5);" +
                        "-fx-background-radius: 999;" +
                        "-fx-padding: 6 16;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-weight: bold;" +
                        "-fx-font-size: 13px;" +
                        "-fx-border-width: 0;" +
                        "-fx-cursor: hand;";

        btn.setStyle(baseStyle);

        ImageView iv = loadIcon(fileName);
        iv.setFitWidth(18);
        iv.setFitHeight(18);

        Label lbl = new Label(tooltip);
        lbl.setStyle("-fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: bold;");

        HBox box = new HBox(8, iv, lbl);
        box.setAlignment(Pos.CENTER);

        btn.setGraphic(box);

        btn.setOnMouseEntered(e -> btn.setStyle(hoverStyle));
        btn.setOnMouseExited(e -> btn.setStyle(baseStyle));

        return btn;
    }


    private Node buildCartButton(String fileName, String tooltip, String color,
                                 javafx.event.EventHandler<javafx.event.ActionEvent> click) {

        cartButton = new Button();
        cartButton.setOnAction(click);

        String baseStyle =
                "-fx-background-color: linear-gradient(to right, #26A69A, #2196F3);" +
                        "-fx-background-radius: 999;" +
                        "-fx-padding: 6 18;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-weight: bold;" +
                        "-fx-font-size: 13px;" +
                        "-fx-border-width: 0;" +
                        "-fx-cursor: hand;";

        String hoverStyle =
                "-fx-background-color: linear-gradient(to right, #2BBBAD, #42A5F5);" +
                        "-fx-background-radius: 999;" +
                        "-fx-padding: 6 18;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-weight: bold;" +
                        "-fx-font-size: 13px;" +
                        "-fx-border-width: 0;" +
                        "-fx-cursor: hand;";

        cartButton.setStyle(baseStyle);

        ImageView iv = loadIcon(fileName);
        iv.setFitWidth(18);
        iv.setFitHeight(18);

        cartBadge = new Label();
        cartBadge.setStyle("""
        -fx-background-color: #E53935; 
        -fx-text-fill: white;
        -fx-font-size: 10px; 
        -fx-font-weight: bold;
        -fx-background-radius: 999;
        -fx-padding: 1 4;
    """);
        cartBadge.setVisible(false);

        StackPane iconWithBadge = new StackPane(iv, cartBadge);
        StackPane.setAlignment(cartBadge, Pos.TOP_RIGHT);
        cartBadge.setTranslateX(8);
        cartBadge.setTranslateY(-8);

        Label lbl = new Label(tooltip);
        lbl.setStyle("-fx-text-fill:white; -fx-font-weight:bold; -fx-font-size: 13px;");

        HBox box = new HBox(8, iconWithBadge, lbl);
        box.setAlignment(Pos.CENTER);

        cartButton.setGraphic(box);

        cartButton.setOnMouseEntered(e -> cartButton.setStyle(hoverStyle));
        cartButton.setOnMouseExited(e -> cartButton.setStyle(baseStyle));

        return cartButton;
    }


    private ImageView loadIcon(String fileName) {
        Image img = null;

        // 1) Try from classpath: /images/<fileName>
        try {
            URL url = getClass().getResource("/images/" + fileName);
            if (url != null) {
                img = new Image(url.toExternalForm(), 24, 24, true, true, true);
            }
        } catch (Exception ignored) {}

        // 2) Fallback: direct file path for IDE runs: src/resources/images/<fileName>
        if (img == null || img.isError()) {
            try {
                img = new Image("file:src/resources/images/" + fileName, 24, 24, true, true, true);
            } catch (Exception ignored) {}
        }

        // 3) Fallback placeholder
        if (img == null || img.isError()) {
            img = new Image("file:src/resources/images/Placeholder.jpg", 24, 24, true, true, true);
        }

        ImageView iv = new ImageView(img);
        iv.setFitWidth(24);
        iv.setFitHeight(24);
        iv.setPreserveRatio(true);
        return iv;
    }

    private void updateCartBadge() {
        int count = cartData.size();
        if (cartBadge == null) return;
        if (count > 0) {
            cartBadge.setText(String.valueOf(count));
            cartBadge.setVisible(true);
        } else {
            cartBadge.setVisible(false);
        }
    }

    private void showError(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK);
        a.setHeaderText("Operation failed");
        a.showAndWait();
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
