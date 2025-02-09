package org.automonius;

import javafx.application.Application;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Project Manager");

        // Create the main layout
        BorderPane mainLayout = new BorderPane();

        // ImageView to display the loaded image
        ImageView imageView = new ImageView();
        imageView.setPreserveRatio(true);
        imageView.fitWidthProperty().bind(mainLayout.widthProperty());
        imageView.fitHeightProperty().bind(mainLayout.heightProperty().subtract(100)); // Subtracting 100 to accommodate the progress bar and buttons

        // Add the ImageView to the main layout
        mainLayout.setCenter(imageView);

        // Task to load the image
        Task<Image> loadImageTask = new Task<Image>() {
            @Override
            protected Image call() throws Exception {
                // Load the actual image from the classpath
                String imagePath = "/IMAGE/Mr..png"; // Ensure the path is correct
                System.out.println("Loading image from classpath: " + imagePath);
                Image image = new Image(getClass().getResourceAsStream(imagePath));

                // Check if image loaded correctly
                if (image.isError()) {
                    System.out.println("Error loading image: " + image.getException().getMessage());
                }
                return image;
            }
        };

        // Set the loaded image in the ImageView
        loadImageTask.setOnSucceeded(event -> {
            Image loadedImage = loadImageTask.getValue();
            imageView.setImage(loadedImage);
            runProgressBar(mainLayout);
        });

        loadImageTask.setOnFailed(event -> {
            Throwable exception = loadImageTask.getException();
            if (exception != null) {
                exception.printStackTrace();
            }
        });

        // Start the background task
        new Thread(loadImageTask).start();

        Scene scene = new Scene(mainLayout, 800, 600);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void runProgressBar(BorderPane mainLayout) {
        // Create and configure the ProgressBar (minimal style) at the bottom left corner
        ProgressBar progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(150); // Set minimal width for the progress bar

        HBox progressBarContainer = new HBox(progressBar);
        progressBarContainer.setAlignment(Pos.BOTTOM_LEFT);
        progressBarContainer.setPadding(new Insets(10)); // Add padding for better visibility
        mainLayout.setBottom(progressBarContainer);

        // Task to run the progress bar
        Task<Void> progressTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                // Simulate a faster loading process
                for (int i = 0; i <= 100; i++) {
                    updateProgress(i, 100);
                    Thread.sleep(25); // Simulate faster loading time
                }
                return null;
            }
        };

        // Update the ProgressBar as it runs
        progressBar.progressProperty().bind(progressTask.progressProperty());

        // Show buttons after progress bar completes
        progressTask.setOnSucceeded(event -> {
            mainLayout.getChildren().remove(progressBarContainer);

            // Create buttons for "Create New Project" and "Load Existing Project"
            Button newProjectBtn = new Button("Create New Project");
            Button loadProjectBtn = new Button("Load Existing Project");

            // Close the current window and open MainUI when a button is clicked
            newProjectBtn.setOnAction(e -> {
                System.out.println("Create New Project button clicked.");
                openMainUI((Stage) mainLayout.getScene().getWindow(), false);
            });
            loadProjectBtn.setOnAction(e -> {
                System.out.println("Load Existing Project button clicked.");
                openMainUI((Stage) mainLayout.getScene().getWindow(), true);
            });

            VBox buttonBox = new VBox(10, newProjectBtn, loadProjectBtn);
            buttonBox.setAlignment(Pos.CENTER);
            buttonBox.setPadding(new Insets(10)); // Add padding for better visibility and margins
            mainLayout.setBottom(buttonBox);
        });

        new Thread(progressTask).start();
    }

    private void openMainUI(Stage primaryStage, boolean loadProject) {
        primaryStage.close(); // Close the current window
        new MainUI().start(new Stage(), loadProject);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
