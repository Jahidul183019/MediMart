package ui;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Cursor;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import javafx.util.Duration;
import models.Inventory;
import services.MedicineService;
import services.UserService;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class MainController {

    // Root & background
    @FXML private StackPane root;
    @FXML private ImageView bgView;
    @FXML private ScrollPane pageScroll;
    @FXML private Circle orbA, orbB, orbC;

    // UI sections
    @FXML private FlowPane categoryFlow;

    @FXML private ImageView logoView;

    @FXML private TextField globalSearch;
    @FXML private Button searchBtn, adminBtn, customerBtn;

    // In FXML heroCard is a StackPane, so:
    @FXML private StackPane heroCard;

    // navigation
    private Stage stage;
    private Inventory inventory;
    private UserService userService;

    // Background slideshow
    private final List<Image> bgImages = new ArrayList<>();
    private int bgIndex = 0;
    private Timeline bgTicker;

    /** Called from Main.start() */
    public void wire(Stage stage, Inventory inventory, UserService userService) {
        this.stage = stage;
        this.inventory = (inventory != null) ? inventory : Inventory.getInstance();
        this.userService = (userService != null) ? userService : new UserService();

        // Button actions – same behavior as before
        adminBtn.setOnAction(e -> new AdminLogin(this.inventory).show(this.stage));
        customerBtn.setOnAction(e -> openLoginSignup());
        searchBtn.setOnAction(e -> openCustomerDashboard());
        globalSearch.setOnAction(e -> openCustomerDashboard());
    }

    /* ========== Navigation ========== */

    private void openLoginSignup() {
        try {
            URL fxmlUrl = getClass().getResource("/fxml/LoginSignup.fxml");
            FXMLLoader loader;
            Parent rootNode;

            if (fxmlUrl != null) {
                loader = new FXMLLoader(fxmlUrl);
                rootNode = loader.load();
            } else {
                File fxmlFile = new File("src/resources/fxml/LoginSignup.fxml");
                if (!fxmlFile.exists()) {
                    System.err.println("LoginSignup.fxml not found: " + fxmlFile.getAbsolutePath());
                    return;
                }
                loader = new FXMLLoader(fxmlFile.toURI().toURL());
                rootNode = loader.load();
            }

            LoginSignup controller = loader.getController();
            controller.setServices(inventory, userService);

            Stage currentStage = this.stage;
            if (currentStage == null && root != null && root.getScene() != null) {
                currentStage = (Stage) root.getScene().getWindow();
                this.stage = currentStage;
            }
            if (currentStage == null) {
                throw new IllegalStateException("Stage is null. Did you call wire()?");
            }

            currentStage.getScene().setRoot(rootNode);
            currentStage.setTitle("Login / Sign Up - MediMart");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void openCustomerDashboard() {
        new CustomerDashboard(inventory, new MedicineService()).show(stage);
    }

    /* ========== initialize() ========== */

    public void initialize() {
        makeScrollPaneTransparent(pageScroll);
        loadImages();               // logo + p1..p4
        setupBackgroundSlideshow();  // use p1..p4
        buildCategoryChips();
        setupCTAInteractions();
        setupBackgroundAnimations();
    }

    /* ---------- Make ScrollPane viewport transparent (remove white layer) ---------- */
    private void makeScrollPaneTransparent(ScrollPane sp) {
        if (sp == null) return;
        sp.setStyle("-fx-background-color: transparent;");
        Runnable apply = () -> {
            var vp = sp.lookup(".viewport");
            if (vp != null) {
                vp.setStyle("-fx-background-color: transparent;");
            }
        };
        sp.skinProperty().addListener((o, oldSkin, newSkin) -> Platform.runLater(apply));
        Platform.runLater(apply);
    }

    /* ---------- Assets ---------- */
    private void loadImages() {
        // IMPORTANT: these paths must match how resources are on the classpath.
        // With src/resources marked as Resources Root, images live at /images/*.png

        // Logo
        setImage(logoView, "/resources/images/logo_medimart.png");

        // Background slides: p1..p4
        addBgImage("/resources/images/p1.png");
        addBgImage("/resources/images/p2.png");
        addBgImage("/resources/images/p3.png");
        addBgImage("/resources/images/p4.png");
    }

    private void setImage(ImageView iv, String cp) {
        if (iv == null) return;
        try {
            URL url = getClass().getResource(cp);
            if (url != null) {
                iv.setImage(new Image(url.toExternalForm(), true));
            } else {
                System.err.println("Image not found on classpath: " + cp);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void addBgImage(String cp) {
        try {
            URL url = getClass().getResource(cp);
            if (url != null) {
                bgImages.add(new Image(url.toExternalForm(), true));
            } else {
                System.err.println("Background image not found: " + cp);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /* ---------- Background slideshow using p1–p4 ---------- */
    private void setupBackgroundSlideshow() {
        if (bgView == null || bgImages.isEmpty() || root == null) return;

        bgView.setPreserveRatio(false);
        bgView.fitWidthProperty().bind(root.widthProperty());
        bgView.fitHeightProperty().bind(root.heightProperty());
        bgView.setOpacity(1.0);
        bgView.setImage(bgImages.get(0));

        bgTicker = new Timeline(new KeyFrame(Duration.seconds(7), e -> {
            int next = (bgIndex + 1) % bgImages.size();
            Image nextImg = bgImages.get(next);

            FadeTransition fadeOut = new FadeTransition(Duration.millis(600), bgView);
            fadeOut.setToValue(0.0);
            fadeOut.setOnFinished(ev -> {
                bgView.setImage(nextImg);
                FadeTransition fadeIn = new FadeTransition(Duration.millis(600), bgView);
                fadeIn.setToValue(1.0);
                fadeIn.play();
            });
            fadeOut.play();
            bgIndex = next;
        }));
        bgTicker.setCycleCount(Animation.INDEFINITE);
        bgTicker.play();
    }

    /* ---------- Category chips ---------- */
    private void buildCategoryChips() {
        String[] cats = {"Pain Relief", "Diabetes", "Cardiac", "Vitamins", "Baby Care",
                "Dermatology", "Respiratory", "OTC", "Antibiotics", "Women’s Health"};

        for (String c : cats) {
            Label chip = new Label(c);
            chip.setPadding(new Insets(8, 14, 8, 14));
            chip.setStyle(
                    "-fx-background-radius: 999;" +
                            "-fx-background-color: linear-gradient(to right,#26A69A,#64B5F6);" +
                            "-fx-text-fill: white;" +
                            "-fx-font-weight: bold;"
            );
            chip.setCursor(Cursor.HAND);

            ScaleTransition st = new ScaleTransition(Duration.millis(120), chip);
            chip.setOnMouseEntered(e -> {
                st.stop();
                st.setToX(1.06);
                st.setToY(1.06);
                st.playFromStart();
            });
            chip.setOnMouseExited(e -> {
                st.stop();
                st.setToX(1.0);
                st.setToY(1.0);
                st.playFromStart();
            });

            Tooltip.install(chip, new Tooltip("Browse " + c));

            chip.setOnMouseClicked(e -> new CustomerDashboard(inventory, new MedicineService()).show(stage));

            categoryFlow.getChildren().add(chip);
        }
    }

    /* ---------- CTA micro-interactions ---------- */
    private void setupCTAInteractions() {
        pulseOnClick(adminBtn);
        pulseOnClick(customerBtn);
        pulseOnClick(searchBtn);
    }

    private void pulseOnClick(Button btn) {
        if (btn == null) return;
        btn.addEventHandler(MouseEvent.MOUSE_PRESSED, e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(110), btn);
            st.setToX(0.97);
            st.setToY(0.97);
            st.setAutoReverse(true);
            st.setCycleCount(2);
            st.play();
        });
    }

    /* ---------- Background orbs breathing ---------- */
    private void setupBackgroundAnimations() {
        animateOrb(orbA, 1.0, 1.06, -12, 10, 0.75, 3400);
        animateOrb(orbB, 1.0, 1.05, 10, -14, 0.65, 3600);
        animateOrb(orbC, 1.0, 1.07, -8, 12, 0.70, 3800);
    }

    private void animateOrb(Circle orb, double s0, double s1, double dx, double dy,
                            double minOpacity, int ms) {
        if (orb == null) return;

        ScaleTransition scale = new ScaleTransition(Duration.millis(ms), orb);
        scale.setFromX(s0);
        scale.setFromY(s0);
        scale.setToX(s1);
        scale.setToY(s1);
        scale.setAutoReverse(true);
        scale.setCycleCount(Animation.INDEFINITE);

        TranslateTransition move = new TranslateTransition(Duration.millis(ms), orb);
        move.setFromX(0);
        move.setFromY(0);
        move.setToX(dx);
        move.setToY(dy);
        move.setAutoReverse(true);
        move.setCycleCount(Animation.INDEFINITE);

        FadeTransition fade = new FadeTransition(Duration.millis(ms), orb);
        fade.setFromValue(1.0);
        fade.setToValue(minOpacity);
        fade.setAutoReverse(true);
        fade.setCycleCount(Animation.INDEFINITE);

        scale.play();
        move.play();
        fade.play();
    }
}
