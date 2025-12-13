package ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import models.User;
import services.UserService;
import utils.FileLogger;
import utils.Session;

import java.io.File;
import javafx.scene.shape.Circle; // added for circular layer clip

public class ProfileView {
    private final UserService userService = new UserService();

    public void show(Stage stage) {
        User u = Session.getCurrentUser();
        if (u == null) {
            new Alert(Alert.AlertType.WARNING, "No user is logged in.").showAndWait();
            return;
        }

        // Reload latest from DB
        User fresh = userService.getUserById(u.getId());
        if (fresh != null) u = fresh;

        /* ==== Title/Header ==== */
        Label title = new Label("My Profile");
        title.setStyle("""
            -fx-font-size: 26px;
            -fx-font-weight: 800;
            -fx-text-fill: linear-gradient(to right, #1e88e5, #26a69a);
        """);

        Label subtitle = new Label("Update your personal details and password");
        subtitle.setStyle("-fx-text-fill: rgba(0,0,0,0.65); -fx-font-size: 13px;");
        VBox header = new VBox(2, title, subtitle);
        header.setPadding(new Insets(8, 8, 0, 8));

        /* ==== Avatar (full image; circular layer mask, not image) ==== */
        ImageView avatar = new ImageView();
        avatar.setFitWidth(120);
        avatar.setFitHeight(120);
        avatar.setPreserveRatio(true);  // keep full image visible
        avatar.setSmooth(true);
        avatar.setCache(true);
        setAvatarImage(avatar, u.getAvatarPath());

        // Inner container that will be clipped to a circle (so the layer is circular, not the image)
        StackPane clippedLayer = new StackPane(avatar);
        clippedLayer.setMinSize(120, 120);
        clippedLayer.setPrefSize(120, 120);
        clippedLayer.setMaxSize(120, 120);
        clippedLayer.setClip(new Circle(60, 60, 60)); // circular mask applied to the LAYER

        // Decorative ring (not clipped)
        Circle ring = new Circle(60);
        ring.setFill(Color.TRANSPARENT);
        ring.setStroke(Color.web("#90CAF9"));
        ring.setStrokeWidth(3);

        // Outer holder shows the circular layer + the ring
        StackPane avatarHolder = new StackPane(clippedLayer, ring);
        avatarHolder.setMinSize(120, 120);
        avatarHolder.setPrefSize(120, 120);
        avatarHolder.setMaxSize(120, 120);
        avatarHolder.setStyle("""
            -fx-background-color: rgba(255,255,255,0.0);
            -fx-background-radius: 60;
            -fx-border-radius: 60;
            -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.22), 12, 0.3, 0, 1);
        """);

        Button changeAvatar = new Button("Change Avatar");
        changeAvatar.setStyle("""
            -fx-background-color: linear-gradient(to right, #1e88e5, #42a5f5);
            -fx-text-fill: white; -fx-font-weight: bold;
            -fx-background-radius: 999; -fx-padding: 8 16;
        """);
        changeAvatar.setOnMouseEntered(e -> changeAvatar.setStyle("""
            -fx-background-color: linear-gradient(to right, #1976d2, #1e88e5);
            -fx-text-fill: white; -fx-font-weight: bold;
            -fx-background-radius: 999; -fx-padding: 8 16;
        """));
        changeAvatar.setOnMouseExited(e -> changeAvatar.setStyle("""
            -fx-background-color: linear-gradient(to right, #1e88e5, #42a5f5);
            -fx-text-fill: white; -fx-font-weight: bold;
            -fx-background-radius: 999; -fx-padding: 8 16;
        """));

        VBox leftCard = new VBox(12, avatarHolder, changeAvatar);
        leftCard.setAlignment(Pos.TOP_CENTER);
        leftCard.setPadding(new Insets(18));
        leftCard.setStyle("""
            -fx-background-color: rgba(255,255,255,0.85);
            -fx-background-radius: 16;
            -fx-border-radius: 16;
            -fx-border-color: rgba(30,136,229,0.15);
            -fx-border-width: 1;
        """);

        /* ==== Profile fields (styled inputs) ==== */
        TextField firstName = makeInput(nz(u.getFirstName()), "First name");
        TextField lastName  = makeInput(nz(u.getLastName()), "Last name");
        TextField phone     = makeInput(nz(u.getPhone()), "Phone");
        TextField email     = makeInput(nz(u.getEmail()), "Email"); email.setDisable(true);
        TextArea  address   = new TextArea(nz(u.getAddress()));
        styleTextArea(address, "Address");
        address.setPrefRowCount(3);

        GridPane form = new GridPane();
        form.setHgap(14); form.setVgap(12); form.setPadding(new Insets(4));
        form.add(rowLabel("First Name"), 0, 0); form.add(firstName, 1, 0);
        form.add(rowLabel("Last Name"),  0, 1); form.add(lastName,  1, 1);
        form.add(rowLabel("Phone"),      0, 2); form.add(phone,     1, 2);
        form.add(rowLabel("Email"),      0, 3); form.add(email,     1, 3);
        form.add(rowLabel("Address"),    0, 4); form.add(address,   1, 4);

        Button save = new Button("Save Profile");
        save.setStyle("""
            -fx-background-color: linear-gradient(to right, #26a69a, #4db6ac);
            -fx-text-fill: white; -fx-font-weight: bold;
            -fx-background-radius: 10; -fx-padding: 9 18;
        """);
        save.setOnMouseEntered(e -> save.setStyle("""
            -fx-background-color: linear-gradient(to right, #1f8f85, #26a69a);
            -fx-text-fill: white; -fx-font-weight: bold;
            -fx-background-radius: 10; -fx-padding: 9 18;
        """));
        save.setOnMouseExited(e -> save.setStyle("""
            -fx-background-color: linear-gradient(to right, #26a69a, #4db6ac);
            -fx-text-fill: white; -fx-font-weight: bold;
            -fx-background-radius: 10; -fx-padding: 9 18;
        """));

        VBox profileCard = new VBox(14,
                sectionTitle("Profile Information"),
                form,
                save,
                new Separator(),
                sectionTitle("Change Password")
        );
        profileCard.setPadding(new Insets(18));
        profileCard.setStyle("""
            -fx-background-color: rgba(255,255,255,0.92);
            -fx-background-radius: 16;
            -fx-border-radius: 16;
            -fx-border-color: rgba(38,166,154,0.15);
            -fx-border-width: 1;
        """);

        /* ==== Change password ==== */
        PasswordField oldPass = makePassword("Current password");
        PasswordField newPass = makePassword("New password");
        PasswordField cnfPass = makePassword("Confirm new password");

        Button changePass = new Button("Change Password");
        changePass.setStyle("""
            -fx-background-color: linear-gradient(to right, #ffa000, #ffb300);
            -fx-text-fill: #263238; -fx-font-weight: bold;
            -fx-background-radius: 10; -fx-padding: 9 18;
        """);
        changePass.setOnMouseEntered(e -> changePass.setStyle("""
            -fx-background-color: linear-gradient(to right, #ff8f00, #ffa000);
            -fx-text-fill: #263238; -fx-font-weight: bold;
            -fx-background-radius: 10; -fx-padding: 9 18;
        """));
        changePass.setOnMouseExited(e -> changePass.setStyle("""
            -fx-background-color: linear-gradient(to right, #ffa000, #ffb300);
            -fx-text-fill: #263238; -fx-font-weight: bold;
            -fx-background-radius: 10; -fx-padding: 9 18;
        """));

        VBox passBox = new VBox(10, oldPass, newPass, cnfPass, changePass);
        passBox.setPadding(new Insets(0, 4, 4, 4));
        profileCard.getChildren().add(passBox);

        /* ==== Body (scrollable content) ==== */
        HBox body = new HBox(16, leftCard, profileCard);
        body.setPadding(new Insets(14));
        HBox.setHgrow(profileCard, Priority.ALWAYS);

        VBox scrollContent = new VBox(10, header, body);
        scrollContent.setPadding(new Insets(12, 16, 12, 16));

        ScrollPane scroller = new ScrollPane(scrollContent);
        scroller.setFitToWidth(true);
        scroller.setPannable(true);
        scroller.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroller.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroller.setStyle("""
            -fx-background-color: transparent;
            -fx-background-insets: 0;
            -fx-padding: 0;
        """);

        BorderPane root = new BorderPane();
        root.setCenter(scroller);
        root.setStyle("-fx-background-color: linear-gradient(to bottom right, #E3F2FD, #E8F5E9);");

        Button back = new Button("Back");
        back.setStyle("""
            -fx-background-color: linear-gradient(to right, #ef5350, #e53935);
            -fx-text-fill: white; -fx-font-weight: bold;
            -fx-background-radius: 999; -fx-padding: 7 14;
        """);
        back.setOnMouseEntered(e -> back.setStyle("""
            -fx-background-color: linear-gradient(to right, #e53935, #d32f2f);
            -fx-text-fill: white; -fx-font-weight: bold;
            -fx-background-radius: 999; -fx-padding: 7 14;
        """));
        back.setOnMouseExited(e -> back.setStyle("""
            -fx-background-color: linear-gradient(to right, #ef5350, #e53935);
            -fx-text-fill: white; -fx-font-weight: bold;
            -fx-background-radius: 999; -fx-padding: 7 14;
        """));
        back.setOnAction(e -> new CustomerDashboard().show(stage));

        HBox bottom = new HBox(back);
        bottom.setAlignment(Pos.CENTER_RIGHT);
        bottom.setPadding(new Insets(10,16,16,16));
        root.setBottom(bottom);

        Scene scene = new Scene(root, 880, 600);
        stage.setScene(scene);
        stage.setTitle("My Profile");
        stage.show();

        /* ==== Actions (unchanged logic) ==== */
        final User[] currentRef = { u };

        changeAvatar.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.setTitle("Select Avatar Image");
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg"));
            File f = fc.showOpenDialog(stage);
            if (f == null) return;
            try {
                String saved = userService.saveAvatarToAppStorage(currentRef[0].getId(), f.toPath()).toString();
                if (userService.updateAvatar(currentRef[0].getId(), saved)) {
                    setAvatarImage(avatar, saved);
                    currentRef[0].setAvatarPath(saved);
                    Session.setCurrentUser(currentRef[0]);
                    info("Avatar updated.");
                } else {
                    error("Failed to update avatar.");
                }
            } catch (Exception ex) {
                FileLogger.error("Avatar change failed: " + ex.getMessage(), ex);
                error("Avatar change failed: " + ex.getMessage());
            }
        });

        save.setOnAction(e -> {
            if (firstName.getText().trim().isEmpty()) { warn("First name is required."); return; }
            if (phone.getText().trim().isEmpty())     { warn("Phone is required."); return; }

            User cur = currentRef[0];
            cur.setFirstName(firstName.getText().trim());
            cur.setLastName(lastName.getText().trim());
            cur.setPhone(phone.getText().trim());
            cur.setAddress(address.getText().trim());

            try {
                if (userService.updateProfile(cur)) {
                    Session.setCurrentUser(cur);
                    info("Profile saved.");
                } else {
                    error("Could not save profile.");
                }
            } catch (Exception ex) {
                FileLogger.error("Profile save failed: " + ex.getMessage(), ex);
                error("Profile save failed: " + ex.getMessage());
            }
        });

        changePass.setOnAction(e -> {
            String op = oldPass.getText();
            String np = newPass.getText();
            String cp = cnfPass.getText();

            if (op == null || op.isBlank() || np == null || np.isBlank()) {
                warn("Please fill current and new password.");
                return;
            }
            if (!np.equals(cp)) {
                warn("New password and confirm do not match.");
                return;
            }
            if (np.length() < 6) {
                warn("New password must be at least 6 characters.");
                return;
            }

            boolean ok = userService.changePassword(currentRef[0].getId(), op, np);
            if (ok) {
                info("Password changed.");
                oldPass.clear(); newPass.clear(); cnfPass.clear();
            } else {
                warn("Password change failed. Check your current password.");
            }
        });
    }

    /* ================= Helpers & styling ================= */

    private static Label rowLabel(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-text-fill: rgba(0,0,0,0.72); -fx-font-size: 12.8px; -fx-font-weight: 700;");
        return l;
    }

    private static Label sectionTitle(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-text-fill: rgba(0,0,0,0.80); -fx-font-size: 14.5px; -fx-font-weight: 800;");
        return l;
    }

    private static TextField makeInput(String initialText, String placeholder) {
        TextField tf = new TextField(initialText);
        tf.setPromptText(placeholder);
        styleTextField(tf);
        return tf;
    }

    private static PasswordField makePassword(String placeholder) {
        PasswordField pf = new PasswordField();
        pf.setPromptText(placeholder);
        styleTextField(pf);
        return pf;
    }

    private static void styleTextField(TextInputControl tf) {
        final String base = """
            -fx-background-color: rgba(255,255,255,0.98);
            -fx-background-insets: 0;
            -fx-background-radius: 12;
            -fx-border-color: #B2DFDB;
            -fx-border-radius: 12;
            -fx-border-width: 1.2;
            -fx-padding: 10 12;
            -fx-font-size: 13.2px;
            -fx-prompt-text-fill: rgba(0,0,0,0.45);
        """;
        tf.setStyle(base);
        tf.focusedProperty().addListener((obs, old, f) -> {
            if (f) {
                tf.setStyle(base + """
                    -fx-border-color: #26A69A;
                    -fx-effect: dropshadow(gaussian, rgba(38,166,154,0.35), 12, 0.35, 0, 0);
                """);
            } else {
                tf.setStyle(base + "-fx-effect: null;");
            }
        });
    }

    private static void styleTextArea(TextArea ta, String placeholder) {
        ta.setPromptText(placeholder);
        ta.setWrapText(true);
        styleTextField(ta);
    }

    private static void setAvatarImage(ImageView view, String path) {
        try {
            if (path != null && !path.isBlank()) {
                Image img = new Image("file:" + path, 120, 120, true, true);
                if (!img.isError()) {
                    SnapshotParameters sp = new SnapshotParameters();
                    sp.setFill(Color.TRANSPARENT);
                    view.setImage(img);
                    return;
                }
            }
        } catch (Exception ignored) { }
        try {
            var res = ProfileView.class.getResource("/images/customer.png");
            if (res != null) {
                Image fallback = new Image(res.toExternalForm(), 120, 120, true, true);
                view.setImage(fallback);
                return;
            }
        } catch (Exception ignored) { }
        view.setImage(null);
    }

    private static void info(String m){ new Alert(Alert.AlertType.INFORMATION, m, ButtonType.OK).showAndWait(); }
    private static void warn(String m){ new Alert(Alert.AlertType.WARNING, m, ButtonType.OK).showAndWait(); }
    private static void error(String m){ new Alert(Alert.AlertType.ERROR, m, ButtonType.OK).showAndWait(); }
    private static String nz(String s){ return (s == null) ? "" : s; }
}
