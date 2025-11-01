import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

public class CustomerDashboard {
    private final Inventory inventory;

    public CustomerDashboard(Inventory inventory) {
        this.inventory = inventory;
    }

    public void start(Stage stage) {
        // Title
        Label title = new Label("Customer Dashboard");
        title.setFont(new Font("Arial Bold", 24));
        title.setPadding(new Insets(10));

        // Grid for medicines
        GridPane grid = new GridPane();
        grid.setHgap(20);
        grid.setVgap(20);
        grid.setPadding(new Insets(20));

        int col = 0, row = 0;

        for (Medicine m : inventory.getAllMedicines()) {
            VBox card = createMedicineCard(m);
            grid.add(card, col, row);

            col++;
            if (col > 2) { // 3 items per row
                col = 0;
                row++;
            }
        }

        // Back button
        Button back = new Button("Back");
        back.setStyle("-fx-background-color: #f44336; -fx-text-fill: white; -fx-font-size: 14px;");
        back.setOnAction(e -> new Main().start(stage));

        HBox backBox = new HBox(back);
        backBox.setAlignment(Pos.CENTER);
        backBox.setPadding(new Insets(10));

        // Layout
        BorderPane root = new BorderPane();
        root.setTop(title);
        BorderPane.setAlignment(title, Pos.CENTER);
        root.setCenter(grid);
        root.setBottom(backBox);
        root.setStyle("-fx-background-color: #f0f8ff;");

        Scene scene = new Scene(root, 900, 650);
        stage.setScene(scene);
        stage.setTitle("Customer Dashboard");
        stage.show();
    }

    private VBox createMedicineCard(Medicine m) {
        Node imageNode = loadImageNode(m.getName());

        Label name = new Label(m.getName());
        name.setFont(new Font(16));
        name.setWrapText(true);

        Label category = new Label(m.getCategory());
        Label price = new Label("Price: $" + m.getPrice());
        Label quantity = new Label("Qty: " + m.getQuantity());
        Label expiry = new Label("Expiry: " + m.getExpiryDate());

        Button orderBtn = new Button("Order");
        orderBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");
        orderBtn.setOnAction(e -> {
            // NOTE: প্রতি ক্লিকে নতুন Order করলে কার্ট রিসেট হবে; প্র্যাকটিসে শেয়ার্ড কার্ট ব্যবহার করো
            Order order = new Order();
            order.addToCart(m, 1);
            System.out.println("Ordered: " + m.getName());
        });

        VBox card = new VBox(5, imageNode, name, category, price, quantity, expiry, orderBtn);
        card.setPadding(new Insets(10));
        card.setAlignment(Pos.CENTER);
        card.setStyle("-fx-border-color: lightgray; -fx-border-width: 1px; -fx-background-color: white;");
        return card;
    }

    // ---- Image loader: classpath → project /images → placeholder; দৃশ্যমান fallback সহ ----
    private Node loadImageNode(String medicineName) {
        String normalized = medicineName.trim().replaceAll("\\s+", "");

        // 1) Classpath candidates
        String[] classpathCandidates = new String[] {
                "/images/" + normalized + ".png",
                "/images/" + medicineName + ".png",
                // কিছু IDE-তে images-কে সরাসরি Resources Root করলে লিডিং স্ল্যাশে ফাইল রুটে থাকে
                "/" + normalized + ".png",
                "/" + medicineName + ".png"
        };

        // 2) Filesystem candidates (project root/working dir)
        Path wd = Paths.get(System.getProperty("user.dir")); // IntelliJ default: project root
        String[] fileCandidates = new String[] {
                wd.resolve(Paths.get("images", normalized + ".png")).toUri().toString(),
                wd.resolve(Paths.get("images", medicineName + ".png")).toUri().toString()
        };

        // Try classpath
        for (String p : classpathCandidates) {
            URL url = getClass().getResource(p);
            System.out.println("Loading (cp): " + p + " -> " + (url == null ? "null" : "found"));
            if (url != null) {
                Image img = new Image(url.toExternalForm(), 120, 120, true, true);
                if (!img.isError()) return buildImageView(img, medicineName);
            }
        }

        // Try filesystem
        for (String fileUri : fileCandidates) {
            System.out.println("Loading (fs): " + fileUri);
            Image img = new Image(fileUri, 120, 120, true, true);
            if (!img.isError()) return buildImageView(img, medicineName);
        }

        // Placeholder (classpath → filesystem)
        Image ph = tryPlaceholder();
        if (ph != null) return buildImageView(ph, medicineName);

        // Final visible fallback
        Rectangle boxPh = new Rectangle(120, 120);
        boxPh.setStyle("-fx-fill: #eef2f7; -fx-stroke: #b0b7c3;");
        VBox box = new VBox(4, boxPh, new Label(medicineName));
        box.setAlignment(Pos.CENTER);
        System.out.println("⚠️ Placeholder also missing!");
        return box;
    }

    private ImageView buildImageView(Image img, String tip) {
        ImageView iv = new ImageView(img);
        iv.setFitWidth(120);
        iv.setFitHeight(120);
        iv.setPreserveRatio(true);
        Tooltip.install(iv, new Tooltip(tip));
        return iv;
    }

    private Image tryPlaceholder() {
        // classpath → filesystem
        String[] phCp = new String[] { "/images/Placeholder.png", "/Placeholder.png" };
        for (String p : phCp) {
            URL url = getClass().getResource(p);
            System.out.println("Placeholder (cp): " + p + " -> " + (url == null ? "null" : "found"));
            if (url != null) {
                Image img = new Image(url.toExternalForm(), 120, 120, true, true);
                if (!img.isError()) return img;
            }
        }
        Path wd = Paths.get(System.getProperty("user.dir"));
        String phFs = wd.resolve(Paths.get("images", "Placeholder.png")).toUri().toString();
        System.out.println("Placeholder (fs): " + phFs);
        Image img = new Image(phFs, 120, 120, true, true);
        return img.isError() ? null : img;
    }
}
