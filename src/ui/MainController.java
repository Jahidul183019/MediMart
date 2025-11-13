package ui;

import javafx.animation.*;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import javafx.util.Duration;
import models.Inventory;
import services.UserService;
import services.MedicineService;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class MainController {

    // FXML refs
    @FXML private Parent root;                 // <-- CHANGED from StackPane to Parent
    @FXML private StackPane testimonialCarousel;
    @FXML private FlowPane categoryFlow;
    @FXML private HBox statRow;

    @FXML private Circle orbA, orbB, orbC;

    @FXML private ImageView logoView, p1, p2, p3, p4;

    @FXML private TextField globalSearch;
    @FXML private Button searchBtn, adminBtn, customerBtn;

    @FXML private VBox heroCard;

    // for navigation
    private Stage stage;
    private Inventory inventory;
    private UserService userService;

    // Carousel data (sample)
    private final List<Node> testimonials = new ArrayList<>();
    private int testimonialIndex = 0;

    /** Call this from Main.java after loading the FXML */
    public void wire(Stage stage, Inventory inventory, UserService userService) {
        this.stage = stage;
        this.inventory = (inventory != null) ? inventory : Inventory.getInstance();
        this.userService = (userService != null) ? userService : new UserService();

        // Button actions now navigate to your real screens
        adminBtn.setOnAction(e -> new AdminLogin(this.inventory).show(this.stage));

        customerBtn.setOnAction(e -> openLoginSignup());

        // Search opens CustomerDashboard (you can later pass the query into it)
        searchBtn.setOnAction(e -> openCustomerDashboard());
        globalSearch.setOnAction(e -> openCustomerDashboard());
    }

    private void openLoginSignup() {
        try {
            URL fxmlUrl = getClass().getResource("/fxml/LoginSignup.fxml");
            FXMLLoader loader;
            Parent rootNode;   // <-- CHANGED from StackPane to Parent

            if (fxmlUrl != null) {
                loader = new FXMLLoader(fxmlUrl);
                rootNode = loader.load();
            } else {
                File fxmlFile = new File("src/resources/fxml/LoginSignup.fxml");
                loader = new FXMLLoader(fxmlFile.toURI().toURL());
                rootNode = loader.load();
            }

            LoginSignup controller = loader.getController();
            controller.setServices(inventory, userService);

            // Make sure we have a valid stage
            Stage currentStage = this.stage;
            if (currentStage == null && root != null && root.getScene() != null) {
                currentStage = (Stage) root.getScene().getWindow();
                this.stage = currentStage;
            }

            if (currentStage == null) {
                throw new IllegalStateException("Stage is null. Did you call wire(stage, inventory, userService)?");
            }

            currentStage.getScene().setRoot(rootNode);
            currentStage.setTitle("Login / Sign Up - MediMart");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void openCustomerDashboard() {
        // If you want to inject the search text into the dashboard filter later,
        // add an API on CustomerDashboard (e.g., setInitialQuery) and call it here.
        new CustomerDashboard(inventory, new MedicineService()).show(stage);
    }

    /* ================== UI/FX INITIALIZATION ================== */

    public void initialize() {
        loadImages();
        buildCategoryChips();
        setupCTAInteractions();
        animateStats(13250, 42, 2850); // counts animate in
        setupBackgroundAnimations();
        setupTestimonials();
    }

    /* ---------- Assets ---------- */
    private void loadImages() {
        setImage(logoView, "/images/logo_medimart.png");
        setImage(p1, "/images/partners/p1.png");
        setImage(p2, "/images/partners/p2.png");
        setImage(p3, "/images/partners/p3.png");
        setImage(p4, "/images/partners/p4.png");
    }
    private void setImage(ImageView iv, String classpath) {
        try {
            URL url = getClass().getResource(classpath);
            if (url != null) iv.setImage(new Image(url.toExternalForm(), true));
        } catch (Exception ignored) {}
    }

    /* ---------- Category chips ---------- */
    private void buildCategoryChips() {
        String[] cats = {"Pain Relief", "Diabetes", "Cardiac", "Vitamins", "Baby Care",
                "Dermatology", "Respiratory", "OTC", "Antibiotics", "Women’s Health"};

        for (String c : cats) {
            Label chip = new Label(c);
            chip.setPadding(new Insets(8, 14, 8, 14));
            chip.setStyle("-fx-background-radius: 999; -fx-background-color: linear-gradient(to right,#26A69A,#64B5F6); -fx-text-fill: white; -fx-font-weight: bold;");
            chip.setCursor(Cursor.HAND);

            // hover micro-interaction
            ScaleTransition st = new ScaleTransition(Duration.millis(120), chip);
            chip.setOnMouseEntered(e -> { st.stop(); st.setToX(1.06); st.setToY(1.06); st.playFromStart(); });
            chip.setOnMouseExited(e -> { st.stop(); st.setToX(1.0); st.setToY(1.0); st.playFromStart(); });

            Tooltip.install(chip, new Tooltip("Browse " + c));
            chip.setOnMouseClicked(e -> {
                // Later: open CustomerDashboard filtered to this category
                new CustomerDashboard(inventory, new MedicineService()).show(stage);
            });

            categoryFlow.getChildren().add(chip);
        }
    }

    /* ---------- CTA micro-interactions ---------- */
    private void setupCTAInteractions() {
        pulseOnClick(adminBtn);
        pulseOnClick(customerBtn);
    }
    private void pulseOnClick(Button btn) {
        btn.addEventHandler(MouseEvent.MOUSE_PRESSED, e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(110), btn);
            st.setToX(0.97);
            st.setToY(0.97);
            st.setAutoReverse(true);
            st.setCycleCount(2);
            st.play();
        });
    }

    /* ---------- Stats count-up ---------- */
    private void animateStats(int ordersTarget, int citiesTarget, int productsTarget) {
        countUp(statRow.lookup("#statOrders"), ordersTarget, true);
        countUp(statRow.lookup("#statCities"), citiesTarget, false);
        countUp(statRow.lookup("#statProducts"), productsTarget, true);
    }
    private void countUp(Object node, int target, boolean plusSuffix) {
        if (!(node instanceof Label label)) return;
        int durationMs = 1200;
        Timeline tl = new Timeline();
        final int steps = 30;
        for (int i = 0; i <= steps; i++) {
            int v = (int) Math.round(target * (i / (double) steps));
            String txt = (v >= 1000) ? String.format("%.1fk", v / 1000.0) : String.valueOf(v);
            if (plusSuffix && i == steps) txt += "+";
            tl.getKeyFrames().add(new KeyFrame(Duration.millis(i * (durationMs / (double) steps)),
                    new KeyValue(label.textProperty(), txt)));
        }
        tl.play();
    }

    /* ---------- Background “breathing” ---------- */
    private void setupBackgroundAnimations() {
        animateOrb(orbA, 1.0, 1.06, -12, 10, 0.75, 3400);
        animateOrb(orbB, 1.0, 1.05,  10,-14, 0.65, 3600);
        animateOrb(orbC, 1.0, 1.07,  -8, 12, 0.70, 3800);
    }
    private void animateOrb(Circle orb, double s0, double s1, double dx, double dy, double minOpacity, int ms) {
        if (orb == null) return;
        ScaleTransition scale = new ScaleTransition(Duration.millis(ms), orb);
        scale.setFromX(s0); scale.setFromY(s0);
        scale.setToX(s1);   scale.setToY(s1);
        scale.setAutoReverse(true); scale.setCycleCount(Animation.INDEFINITE);

        TranslateTransition move = new TranslateTransition(Duration.millis(ms), orb);
        move.setFromX(0); move.setFromY(0);
        move.setToX(dx);  move.setToY(dy);
        move.setAutoReverse(true); move.setCycleCount(Animation.INDEFINITE);

        FadeTransition fade = new FadeTransition(Duration.millis(ms), orb);
        fade.setFromValue(1.0); fade.setToValue(minOpacity);
        fade.setAutoReverse(true); fade.setCycleCount(Animation.INDEFINITE);

        scale.play(); move.play(); fade.play();
    }

    /* ---------- Testimonials auto-rotate ---------- */
    private void setupTestimonials() {
        testimonials.add(makeTestimonial("“Great prices and super fast delivery. Love MediMart!” — Farhan"));
        testimonials.add(makeTestimonial("“Customer support is actually 24/7. Impressed.” — Ritu"));
        testimonials.add(makeTestimonial("“Found all my prescribed meds in one place.” — Arafat"));

        if (!testimonials.isEmpty()) {
            testimonialCarousel.getChildren().setAll(testimonials.get(0));
            rotateTestimonials();
        }
    }
    private Node makeTestimonial(String text) {
        Label l = new Label(text);
        l.setWrapText(true);
        l.setPadding(new Insets(18));
        l.setStyle("-fx-font-size:15px; -fx-text-fill:#06363B;");
        StackPane p = new StackPane(l);
        p.setPadding(new Insets(12));
        return p;
    }
    private void rotateTestimonials() {
        Timeline ticker = new Timeline(new KeyFrame(Duration.seconds(4), e -> {
            if (testimonials.isEmpty()) return;
            Node next = testimonials.get((++testimonialIndex) % testimonials.size());
            Node current = testimonialCarousel.getChildren().isEmpty() ? null : testimonialCarousel.getChildren().get(0);

            if (current == null) {
                testimonialCarousel.getChildren().setAll(next);
                return;
            }
            FadeTransition fadeOut = new FadeTransition(Duration.millis(220), current);
            fadeOut.setFromValue(1.0); fadeOut.setToValue(0.0);

            FadeTransition fadeIn = new FadeTransition(Duration.millis(220), next);
            fadeIn.setFromValue(0.0); fadeIn.setToValue(1.0);

            fadeOut.setOnFinished(ev -> {
                testimonialCarousel.getChildren().setAll(next);
                fadeIn.play();
            });
            fadeOut.play();
        }));
        ticker.setCycleCount(Animation.INDEFINITE);
        ticker.play();
    }
}
