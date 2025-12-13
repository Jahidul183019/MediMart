package ui;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.function.Predicate;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Insets;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.stage.Screen;
import javafx.stage.Stage;
import models.Inventory;
import models.Medicine;
import services.MedicineService;
import net.MedicineSync;
import utils.FileLogger;
import utils.ReportExporter;

public class AdminDashboard {

    private final Inventory inventory;
    private final CustomerDashboard customerDashboard; // optional reference for live refresh
    private final MedicineService medicineService;     // DB service

    // Background executor for DB I/O (keeps UI responsive)
    private final ExecutorService io = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "AdminDashboard-IO");
        t.setDaemon(true);
        return t;
    });

    private ScheduledExecutorService poller;

    // --- Constructors ---
    public AdminDashboard(Inventory inventory, CustomerDashboard customerDashboard) {
        this.inventory = inventory;
        this.customerDashboard = customerDashboard;
        this.medicineService = new MedicineService();
    }

    public AdminDashboard(Inventory inventory) {
        this.inventory = inventory;
        this.customerDashboard = null;
        this.medicineService = new MedicineService();
    }

    public AdminDashboard(Inventory inventory, MedicineService service) {
        this.inventory = inventory;
        this.customerDashboard = null;
        this.medicineService = service;
    }

    public AdminDashboard(Inventory inventory, CustomerDashboard customerDashboard, MedicineService service) {
        this.inventory = inventory;
        this.customerDashboard = customerDashboard;
        this.medicineService = service;
    }

    public void show(Stage stage) {
        // ===== Table =====
        TableView<Medicine> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        table.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        table.setEditable(false);
        table.setPlaceholder(new Label("No medicines found."));
        table.setTableMenuButtonVisible(true); // column chooser

        // Backing data (start with in-memory list; async refresh will pull from DB)
        ObservableList<Medicine> baseData = FXCollections.observableArrayList(inventory.getAllMedicines());

        // Quick filter box
        TextField searchField = new TextField();
        searchField.setPromptText("Search by ID, Name, Category…");
        styleSearch(searchField);

        // Filtered & sorted views (so search just works)
        FilteredList<Medicine> filtered = new FilteredList<>(baseData, m -> true);
        searchField.textProperty().addListener((obs, oldV, q) -> filtered.setPredicate(buildPredicate(q)));

        SortedList<Medicine> sorted = new SortedList<>(filtered);
        sorted.comparatorProperty().bind(table.comparatorProperty());
        table.setItems(sorted);

        // Columns
        TableColumn<Medicine, Integer> idCol = new TableColumn<>("ID");
        idCol.setMinWidth(70);
        idCol.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().getId()));
        idCol.setStyle("-fx-alignment: CENTER-RIGHT;");

        TableColumn<Medicine, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(c -> new SimpleStringProperty(nullSafe(c.getValue().getName())));
        nameCol.setCellFactory(TextFieldTableCell.forTableColumn());

        TableColumn<Medicine, String> catCol = new TableColumn<>("Category");
        catCol.setCellValueFactory(c -> new SimpleStringProperty(nullSafe(c.getValue().getCategory())));

        TableColumn<Medicine, String> priceCol = new TableColumn<>("Price");
        priceCol.setMinWidth(100);
        priceCol.setCellValueFactory(c -> new SimpleStringProperty(String.format("%.2f", c.getValue().getPrice())));
        priceCol.setStyle("-fx-alignment: CENTER-RIGHT;");

        TableColumn<Medicine, Integer> qtyCol = new TableColumn<>("Quantity");
        qtyCol.setMinWidth(100);
        qtyCol.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().getQuantity()));
        qtyCol.setStyle("-fx-alignment: CENTER-RIGHT;");

        TableColumn<Medicine, String> expCol = new TableColumn<>("Expiry");
        expCol.setCellValueFactory(c -> new SimpleStringProperty(nullSafe(c.getValue().getExpiryDate())));

        table.getColumns().setAll(idCol, nameCol, catCol, priceCol, qtyCol, expCol);

        // Row UX
        table.setRowFactory(tv -> {
            TableRow<Medicine> row = new TableRow<>();
            row.itemProperty().addListener((obs, oldV, newV) -> {
                if (!row.isEmpty() && row.getIndex() % 2 == 0) row.setStyle("-fx-background-color: rgba(0,0,0,0.02);");
                else row.setStyle("");
            });
            row.setOnMouseClicked(ev -> {
                if (ev.getClickCount() == 2 && !row.isEmpty()) {
                    openEdit(stage, row.getItem(), baseData);
                }
            });

            MenuItem edit = new MenuItem("Edit");
            edit.setOnAction(e -> openEdit(stage, row.getItem(), baseData));
            MenuItem del = new MenuItem("Delete");
            del.setOnAction(e -> attemptDelete(row.getItem(), baseData));
            MenuItem copy = new MenuItem("Copy Cell");
            copy.setOnAction(e -> {
                TablePosition<?, ?> pos = table.getFocusModel().getFocusedCell();
                if (pos != null) {
                    Object val = pos.getTableColumn().getCellObservableValue(pos.getRow()).getValue();
                    ClipboardContent cc = new ClipboardContent();
                    cc.putString(val == null ? "" : val.toString());
                    Clipboard.getSystemClipboard().setContent(cc);
                }
            });
            ContextMenu cm = new ContextMenu(edit, del, new SeparatorMenuItem(), copy);
            row.contextMenuProperty().bind(Bindings.when(Bindings.isNotNull(row.itemProperty()))
                    .then(cm).otherwise((ContextMenu) null));
            return row;
        });

        // ===== Buttons & status =====
        Button addBtn = new Button("Add Medicine");
        Button editBtn = new Button("Edit Selected");
        Button deleteBtn = new Button("Delete Selected");
        Button refreshBtn = new Button("Refresh");
        Button backBtn = new Button("Back to Home");

        // NEW: Export CSV button
        Button exportBtn = new Button("Export CSV");
        exportBtn.setTooltip(new Tooltip("Export all medicines to a CSV file"));

        String btnBase = "-fx-background-radius: 8; -fx-font-weight: bold; -fx-padding: 7 14;";
        addBtn.setStyle(btnBase + "-fx-background-color: #4CAF50; -fx-text-fill: white;");
        editBtn.setStyle(btnBase + "-fx-background-color: #FFA500; -fx-text-fill: white;");
        deleteBtn.setStyle(btnBase + "-fx-background-color: #f44336; -fx-text-fill: white;");
        refreshBtn.setStyle(btnBase + "-fx-background-color: #26A69A; -fx-text-fill: white;");
        backBtn.setStyle(btnBase + "-fx-background-color: #2196F3; -fx-text-fill: white;");
        exportBtn.setStyle(btnBase + "-fx-background-color: #03A9F4; -fx-text-fill: white;");

        addBtn.setTooltip(new Tooltip("Add a new medicine"));
        editBtn.setTooltip(new Tooltip("Edit the selected medicine (Enter)"));
        deleteBtn.setTooltip(new Tooltip("Delete the selected medicine (Delete)"));
        refreshBtn.setTooltip(new Tooltip("Reload from database"));
        backBtn.setTooltip(new Tooltip("Return to Home"));

        // ⬅️ add exportBtn here with other left controls
        HBox leftBar = new HBox(10, addBtn, editBtn, deleteBtn, exportBtn);
        HBox rightBar = new HBox(10, refreshBtn, backBtn);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox actionsBar = new HBox(12, leftBar, spacer, rightBar);
        actionsBar.setPadding(new Insets(8, 10, 8, 10));

        Label status = new Label("Ready");
        status.setStyle("-fx-text-fill: rgba(0,0,0,0.65);");

        // OPTIONAL: socket sync status
        Label syncLabel = new Label();
        syncLabel.setStyle("-fx-text-fill: rgba(0,0,0,0.65);");

        HBox statusBar = new HBox(20, status, new Separator(), new Label("Sync:"), syncLabel);
        statusBar.setPadding(new Insets(0, 10, 10, 10));

        // ===== Actions =====
        addBtn.setOnAction(e -> {
            new AddMedicineForm(inventory).show(stage);
            asyncRefresh(baseData, status);
            try { MedicineSync.getInstance().broadcastRefresh(); } catch (Throwable ignored) {}
        });

        editBtn.setOnAction(e -> {
            Medicine selected = table.getSelectionModel().getSelectedItem();
            if (selected != null) {
                openEdit(stage, selected, baseData);
                try { MedicineSync.getInstance().broadcastRefresh(); } catch (Throwable ignored) {}
            } else {
                warn("Please select a medicine to edit.");
            }
        });

        deleteBtn.setOnAction(e -> {
            Medicine selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) {
                warn("Please select a medicine to delete.");
                return;
            }
            attemptDelete(selected, baseData);
        });

        refreshBtn.setOnAction(e -> asyncRefresh(baseData, status));

        backBtn.setOnAction(e -> {
            try {
                new Main().start(stage);
            } catch (Exception ex) {
                ex.printStackTrace();
                error("Unable to return to Home.");
            }
        });

        //  Export CSV action (your snippet)
        exportBtn.setOnAction(e -> {
            try {
                var list = new MedicineService().getAllMedicines(); // or reuse existing
                var path = ReportExporter.exportMedicinesCsv(list);
                new Alert(Alert.AlertType.INFORMATION,
                        "Exported to:\n" + path.toAbsolutePath()).showAndWait();
            } catch (Exception ex) {
                FileLogger.error("Export failed: " + ex.getMessage(), ex);
                new Alert(Alert.AlertType.ERROR,
                        "Export failed: " + ex.getMessage()).showAndWait();
            }
        });

        // Keyboard shortcuts
        table.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.DELETE) deleteBtn.fire();
            else if (e.getCode() == KeyCode.ENTER) editBtn.fire();
        });

        // ===== Top bar (Search + Actions) =====
        HBox searchBar = new HBox(8, new Label("Search:"), searchField);
        searchBar.setPadding(new Insets(12, 10, 0, 10));
        HBox.setHgrow(searchField, Priority.ALWAYS);

        VBox content = new VBox(8, searchBar, table, actionsBar, statusBar);
        content.setPadding(new Insets(8, 8, 12, 8));

        // ===== Root & Scene =====
        BorderPane root = new BorderPane(content);
        root.setPadding(new Insets(10));
        root.setStyle("""
            -fx-background-color: linear-gradient(to bottom right, #E0F7FA, #E8F5E9);
        """);

        // Responsive sizing & centering
        Rectangle2D b = Screen.getPrimary().getVisualBounds();
        double prefW = Math.max(960, Math.min(1200, b.getWidth() * 0.78));
        double prefH = Math.max(600, Math.min(760, b.getHeight() * 0.80));

        Scene scene = new Scene(root, prefW, prefH);
        stage.setScene(scene);
        stage.setTitle("Admin Dashboard");
        stage.setMinWidth(880);
        stage.setMinHeight(560);
        stage.setX(b.getMinX() + (b.getWidth() - prefW) / 2);
        stage.setY(b.getMinY() + (b.getHeight() - prefH) / 2);
        stage.show();

        // ===== Live updates =====
        try {
            medicineService.addChangeListener(() -> asyncRefresh(baseData, status));
        } catch (Throwable ignored) {}

        // OPTIONAL: socket sync server
        try {
            MedicineSync.getInstance().start();
            syncLabel.setText("Running (broadcasts on change)");
        } catch (Throwable t) {
            syncLabel.setText("Failed to start");
        }

        // Initial DB sync
        asyncRefresh(baseData, status);

        // Cleanup
        stage.setOnCloseRequest(ev -> {
            io.shutdownNow();
            stopPolling();
            try { MedicineSync.getInstance().stop(); } catch (Throwable ignored) {}
        });
    }

    private static Predicate<Medicine> buildPredicate(String q) {
        if (q == null || q.isBlank()) return m -> true;
        String s = q.trim().toLowerCase();
        return m -> {
            if (String.valueOf(m.getId()).contains(s)) return true;
            String n = m.getName() == null ? "" : m.getName().toLowerCase();
            String c = m.getCategory() == null ? "" : m.getCategory().toLowerCase();
            return n.contains(s) || c.contains(s);
        };
    }

    private void openEdit(Stage stage, Medicine selected, ObservableList<Medicine> baseData) {
        new EditMedicineForm(inventory, selected).show(stage);
        asyncRefresh(baseData, null);
    }

    private void attemptDelete(Medicine selected, ObservableList<Medicine> baseData) {
        Optional<ButtonType> res = new Alert(
                Alert.AlertType.CONFIRMATION,
                "Delete \"" + nullSafe(selected.getName()) + "\" (ID " + selected.getId() + ")?",
                ButtonType.YES, ButtonType.NO
        ).showAndWait();
        if (res.isPresent() && res.get() == ButtonType.YES) {
            boolean success = medicineService.deleteMedicine(selected.getId());
            if (success) {
                asyncRefresh(baseData, null);
                try { MedicineSync.getInstance().broadcastRefresh(); } catch (Throwable ignored) {}
            } else {
                error("Failed to delete the medicine.");
            }
        }
    }

    private void asyncRefresh(ObservableList<Medicine> baseData, Label status) {
        if (status != null) status.setText("Refreshing…");
        CompletableFuture
                .supplyAsync(() -> {
                    try {
                        return medicineService.getAllMedicines();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        return null;
                    }
                }, io)
                .thenAccept(list -> Platform.runLater(() -> {
                    try {
                        if (list != null) baseData.setAll(list);
                        if (status != null) status.setText("Refreshed");
                    } finally {
                        if (customerDashboard != null) {
                            try { customerDashboard.refreshMedicines(); } catch (Exception ex) { ex.printStackTrace(); }
                        }
                    }
                }));
    }

    private void startPolling(ObservableList<Medicine> baseData, Label status, int seconds) {
        stopPolling();
        poller = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "AdminDashboard-Poller");
            t.setDaemon(true);
            return t;
        });
        poller.scheduleAtFixedRate(() -> asyncRefresh(baseData, status), seconds, seconds, TimeUnit.SECONDS);
    }

    private void stopPolling() {
        if (poller != null) {
            poller.shutdownNow();
            poller = null;
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
            } else {
                tf.setStyle(base + "-fx-effect: null;");
            }
        });
    }

    private static String nullSafe(String s) { return (s == null) ? "" : s; }

    private static void warn(String msg) {
        Alert a = new Alert(Alert.AlertType.WARNING, msg, ButtonType.OK);
        a.showAndWait();
    }

    private static void error(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK);
        a.showAndWait();
    }
}
