import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import models.Inventory;

public class Main extends Application {
    private Inventory inventory = new Inventory();

    @Override
    public void start(Stage stage) {
        // Title
        Label title = new Label("Welcome to MediMart");
        title.setFont(new Font("Arial Bold", 28));
        title.setStyle("-fx-text-fill: #333;");

        // Buttons
        Button adminBtn = new Button("Login as Admin");
        Button customerBtn = new Button("Login as Customer");

        adminBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-size: 16px; -fx-padding: 10;");
        customerBtn.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white; -fx-font-size: 16px; -fx-padding: 10;");

        adminBtn.setOnAction(e -> new AdminDashboard(inventory).start(stage));
        customerBtn.setOnAction(e -> new CustomerDashboard(inventory).start(stage));

        // Icons
        ImageView adminIcon = new ImageView(new Image("file:resources/admin.png"));
        adminIcon.setFitHeight(50);
        adminIcon.setFitWidth(50);

        ImageView customerIcon = new ImageView(new Image("file:resources/customer.png"));
        customerIcon.setFitHeight(50);
        customerIcon.setFitWidth(50);

        VBox adminBox = new VBox(10, adminIcon, adminBtn);
        adminBox.setAlignment(Pos.CENTER);

        VBox customerBox = new VBox(10, customerIcon, customerBtn);
        customerBox.setAlignment(Pos.CENTER);

        HBox buttonsBox = new HBox(40, adminBox, customerBox);
        buttonsBox.setAlignment(Pos.CENTER);

        BorderPane root = new BorderPane();
        root.setTop(title);
        BorderPane.setAlignment(title, Pos.CENTER);
        root.setCenter(buttonsBox);
        root.setPadding(new Insets(50));
        root.setStyle("-fx-background-color: #f9f9f9;");

        Scene scene = new Scene(root, 600, 450);

        stage.setScene(scene);
        stage.setTitle("MediMart App");
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
