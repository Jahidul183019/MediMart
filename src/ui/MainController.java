package ui;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
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
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import javafx.util.Duration;
import models.Inventory;
import services.MedicineService;
import services.UserService;

import java.io.File;
import java.net.URL;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MainController {
    private static final Logger LOG = Logger.getLogger(MainController.class.getName());

    // FXML refs
    @FXML private StackPane root;
    @FXML private StackPane bgStack;
    @FXML private ImageView bg1, bg2, bg3, bg4;

    @FXML private FlowPane categoryFlow;
    @FXML private ImageView logoView, p1, p2, p3, p4;
    @FXML private ImageView icoSupport, icoFast, icoGenuine;

    @FXML private TextField globalSearch;
    @FXML private Button searchBtn, adminBtn, customerBtn;
    @FXML private VBox heroCard;

    // NEW: make the ScrollPane’s viewport fully transparent
    @FXML private ScrollPane scroller;

    @FXML private Circle dot1, dot2, dot3, dot4;

    // navigation
    private Stage stage;
    private Inventory inventory;
    private UserService userService;

    // background carousel
    private final List<ImageView> slides = new ArrayList<>();
    private int slideIndex = 0;
    private Timeline autoPlay;

    /** Call from Main.start() after loading the FXML */
    public void wire(Stage stage, Inventory inventory, UserService userService) {
        this.stage = Objects.requireNonNull(stage, "stage must not be null");
        this.inventory = (inventory != null) ? inventory : Inventory.getInstance();
        this.userService = (userService != null) ? userService : new UserService();

        if (adminBtn != null) adminBtn.setOnAction(e -> new AdminLogin(this.inventory).show(this.stage));
        if (customerBtn != null) customerBtn.setOnAction(e -> openLoginSignup());
        if (searchBtn != null)  searchBtn.setOnAction(e -> openCustomerDashboard());
        if (globalSearch != null) globalSearch.setOnAction(e -> openCustomerDashboard());

        // stop animations when window hides
        this.stage.sceneProperty().addListener((obs, oldSc, newSc) -> {
            if (newSc != null) {
                newSc.windowProperty().addListener((o2, oldW, newW) -> {
                    if (newW != null) newW.setOnHidden(ev -> stopAutoPlay());
                });
            }
        });
    }

    /* ============== initialize UI ============== */
    public void initialize() {
        try {
            // bind background to root size (perfect fit on resize)
            bindBgToRoot(bg1);
            bindBgToRoot(bg2);
            bindBgToRoot(bg3);
            bindBgToRoot(bg4);

            // ensure the ScrollPane & its viewport are fully transparent
            makeScrollPaneTransparent(scroller);

            loadImages();
            buildCategoryChips();
            setupCTAInteractions();
            setupFeatureTooltips();
            setupCarousel();
            setupDots();

        } catch (Exception ex) {
            LOG.log(Level.SEVERE, "initialize() failed", ex);
        }
    }

    /* ---------- Make ScrollPane truly transparent ---------- */
    private void makeScrollPaneTransparent(ScrollPane sp) {
        if (sp == null) return;
        sp.setStyle("-fx-background-color: transparent;");
        // viewport may be created after skin loads; set twice
        Runnable apply = () -> {
            Node vp = sp.lookup(".viewport");
            if (vp != null) vp.setStyle("-fx-background-color: transparent;");
        };
        sp.skinProperty().addListener((o, oldSkin, newSkin) -> Platform.runLater(apply));
        Platform.runLater(apply);
    }

    /* ---------- BG fit ---------- */
    private void bindBgToRoot(ImageView iv) {
        if (iv == null) return;
        iv.fitWidthProperty().bind(root.widthProperty());
        iv.fitHeightProperty().bind(root.heightProperty());
    }

    /* ---------- Assets ---------- */
    private void loadImages() {

        setImage(logoView, "/images/logo_medimart.png", "src/resources/images/logo_medimart.png");

        // feature icons
        setImage(icoSupport, "/images/icons/support.png", "src/resources/images/support.png");
        setImage(icoFast,    "/images/icons/fast.png",    "src/resources/images/fast.png");
        setImage(icoGenuine, "/images/icons/genuine.png", "src/resources/images/genuine.png");

        // background slides (use p1..p4 as vivid backgrounds)
        setImage(bg1, "/images/partners/p1.png", "src/resources/images/p1.png");
        setImage(bg2, "/images/partners/p2.png", "src/resources/images/p2.png");
        setImage(bg3, "/images/partners/p3.png", "src/resources/images/p3.png");
        setImage(bg4, "/images/partners/p4.png", "src/resources/images/p4.png");
    }

    private void setImage(ImageView iv, String classpath, String fsFallback) {
        if (iv == null) return;
        try {
            URL url = getClass().getResource(classpath);
            if (url != null) { iv.setImage(new Image(url.toExternalForm(), true)); return; }
            File f = new File(fsFallback);
            if (f.exists()) { iv.setImage(new Image(f.toURI().toString(), true)); return; }
            LOG.warning("Image not found: " + classpath + " (fallback: " + fsFallback + ")");
        } catch (Exception ex) {
            LOG.log(Level.WARNING, "Failed to set image: " + classpath, ex);
        }
    }

    /* ---------- Categories ---------- */
    private void buildCategoryChips() {
        if (categoryFlow == null) return;
        String[] cats = {"Pain Relief", "Diabetes", "Cardiac", "Vitamins", "Baby Care",
                "Dermatology", "Respiratory", "OTC", "Antibiotics", "Women’s Health"};

        for (String c : cats) {
            Label chip = new Label(c);
            chip.setPadding(new Insets(8, 14, 8, 14));
            chip.setStyle("-fx-background-radius: 999; -fx-background-color: linear-gradient(to right,#26A69A,#64B5F6); -fx-text-fill: white; -fx-font-weight: bold;");
            chip.setCursor(Cursor.HAND);

            ScaleTransition st = new ScaleTransition(Duration.millis(120), chip);
            chip.setOnMouseEntered(e -> { st.stop(); st.setToX(1.06); st.setToY(1.06); st.playFromStart(); });
            chip.setOnMouseExited(e -> { st.stop(); st.setToX(1.0); st.setToY(1.0); st.playFromStart(); });

            Tooltip.install(chip, new Tooltip("Browse " + c));
            chip.setOnMouseClicked(e -> {
                try { new CustomerDashboard(inventory, new MedicineService()).show(stage); }
                catch (Exception ex) { LOG.log(Level.SEVERE, "Open CustomerDashboard (category=" + c + ") failed", ex); }
            });

            categoryFlow.getChildren().add(chip);
        }
    }

    /* ---------- Buttons micro-interactions ---------- */
    private void setupCTAInteractions() {
        pulseOnClick(adminBtn);
        pulseOnClick(customerBtn);
        pulseOnClick(searchBtn);
    }
    private void pulseOnClick(Button btn) {
        if (btn == null) return;
        btn.addEventHandler(MouseEvent.MOUSE_PRESSED, e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(110), btn);
            st.setToX(0.97); st.setToY(0.97);
            st.setAutoReverse(true); st.setCycleCount(2);
            st.play();
        });
    }

    /* ---------- Feature image tooltips ---------- */
    private void setupFeatureTooltips() {
        installImageTooltip(icoSupport, "/images/tooltips/support_tip.png", "src/resources/images/tooltips/support_tip.png",
                "Real agents 24/7");
        installImageTooltip(icoFast, "/images/tooltips/fast_tip.png", "src/resources/images/tooltips/fast_tip.png",
                "Under 24 hours in major cities");
        installImageTooltip(icoGenuine, "/images/tooltips/genuine_tip.png", "src/resources/images/tooltips/genuine_tip.png",
                "Sourced directly from verified distributors");
    }
    private void installImageTooltip(ImageView anchor, String cp, String fs, String fallbackText) {
        if (anchor == null) return;
        Tooltip tip = new Tooltip(fallbackText);
        try {
            URL u = getClass().getResource(cp);
            Image img = null;
            if (u != null) img = new Image(u.toExternalForm(), true);
            else {
                File f = new File(fs);
                if (f.exists()) img = new Image(f.toURI().toString(), true);
            }
            if (img != null) {
                ImageView iv = new ImageView(img);
                iv.setPreserveRatio(true);
                iv.setFitHeight(140);
                tip.setGraphic(iv);
            }
        } catch (Exception ignored) {}
        Tooltip.install(anchor, tip);
    }

    /* ---------- Background carousel ---------- */
    private void setupCarousel() {
        slides.clear();
        Collections.addAll(slides, bg1, bg2, bg3, bg4);
        for (int i = 0; i < slides.size(); i++) {
            ImageView iv = slides.get(i);
            if (iv != null) iv.setOpacity(i == 0 ? 1.0 : 0.0);
        }

        autoPlay = new Timeline(
                new KeyFrame(Duration.seconds(6), e -> showSlide((slideIndex + 1) % slides.size()))
        );
        autoPlay.setCycleCount(Animation.INDEFINITE);
        autoPlay.play();
    }

    private void showSlide(int nextIndex) {
        if (slides.isEmpty()) return;
        ImageView cur = slides.get(slideIndex);
        ImageView nxt = slides.get(nextIndex);
        if (cur == null || nxt == null || cur == nxt) { slideIndex = nextIndex; return; }

        FadeTransition out = new FadeTransition(Duration.millis(600), cur);
        out.setToValue(0.0);
        FadeTransition in = new FadeTransition(Duration.millis(600), nxt);
        in.setToValue(1.0);

        ParallelTransition pt = new ParallelTransition(out, in);
        pt.play();

        slideIndex = nextIndex;
        updateDots();
    }

    private void setupDots() {
        dot1.setOnMouseClicked(e -> { stopAutoPlay(); showSlide(0); resumeAutoPlay(); });
        dot2.setOnMouseClicked(e -> { stopAutoPlay(); showSlide(1); resumeAutoPlay(); });
        dot3.setOnMouseClicked(e -> { stopAutoPlay(); showSlide(2); resumeAutoPlay(); });
        dot4.setOnMouseClicked(e -> { stopAutoPlay(); showSlide(3); resumeAutoPlay(); });
        updateDots();
    }

    private void updateDots() {
        setDot(dot1, slideIndex == 0);
        setDot(dot2, slideIndex == 1);
        setDot(dot3, slideIndex == 2);
        setDot(dot4, slideIndex == 3);
    }
    private void setDot(Circle c, boolean active) {
        if (c == null) return;
        c.setOpacity(active ? 0.95 : 0.5);
        c.setRadius(active ? 6 : 5);
    }
    private void stopAutoPlay() { if (autoPlay != null) autoPlay.stop(); }
    private void resumeAutoPlay() { if (autoPlay != null) autoPlay.playFromStart(); }

    /* ---------- Navigation ---------- */
    private void openLoginSignup() {
        try {
            Parent rootNode = loadFXML("/fxml/LoginSignup.fxml", "src/resources/fxml/LoginSignup.fxml");
            if (rootNode == null) {
                LOG.severe("LoginSignup.fxml not found");
                return;
            }
            replaceSceneRootWithFade(rootNode, "Login / Sign Up - MediMart");
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, "openLoginSignup() failed", ex);
        }
    }

    private void openCustomerDashboard() {
        try {
            new CustomerDashboard(inventory, new MedicineService()).show(stage);
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, "openCustomerDashboard() failed", ex);
        }
    }

    /* ---------- FXML helpers ---------- */
    private Parent loadFXML(String classpath, String fsFallback) {
        try {
            URL cp = getClass().getResource(classpath);
            javafx.fxml.FXMLLoader loader;
            Parent rootNode;
            if (cp != null) {
                loader = new javafx.fxml.FXMLLoader(cp);
                rootNode = loader.load();
            } else {
                File f = new File(fsFallback);
                if (!f.exists()) return null;
                loader = new javafx.fxml.FXMLLoader(f.toURI().toURL());
                rootNode = loader.load();
            }
            return rootNode;
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, "Failed to load FXML: " + classpath + " (fallback: " + fsFallback + ")", ex);
            return null;
        }
    }

    private void replaceSceneRootWithFade(Parent newRoot, String newTitle) {
        if (stage == null) { LOG.warning("Stage is null; cannot replace scene root"); return; }
        Scene scene = stage.getScene();
        if (scene == null) {
            stage.setScene(new Scene(newRoot));
            stage.setTitle(newTitle);
            return;
        }
        Parent oldRoot = scene.getRoot();
        Duration d = Duration.millis(180);
        FadeTransition fadeOut = new FadeTransition(d, oldRoot);
        fadeOut.setFromValue(1.0); fadeOut.setToValue(0.0);
        FadeTransition fadeIn = new FadeTransition(d, newRoot);
        fadeIn.setFromValue(0.0); fadeIn.setToValue(1.0);
        fadeOut.setOnFinished(ev -> { scene.setRoot(newRoot); stage.setTitle(newTitle); fadeIn.play(); });
        fadeOut.play();
    }
}
