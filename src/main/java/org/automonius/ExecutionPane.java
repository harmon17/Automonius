package org.automonius;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;

public class ExecutionPane extends BorderPane {

    public ExecutionPane(Runnable onBackButtonPressed) {
        // Create the additional buttons with images
        Button button1 = createButtonWithImage("IMAGE/bank.png", 60, 60);
        button1.setOnAction(e -> onBackButtonPressed.run());

        Button button2 = createButtonWithImage("IMAGE/presentation.png", 60, 60);

        Button button3 = createButtonWithImage("IMAGE/settings.png", 60, 60);

        // Layout for the additional buttons
        VBox buttonLayout = new VBox(10, button1, button2, button3);
        buttonLayout.setAlignment(Pos.CENTER_LEFT);
        buttonLayout.setPadding(new Insets(10));
        setLeft(buttonLayout);

        // Directory tree on the left
        TreeItem<String> rootItem = new TreeItem<>("Root");
        TreeItem<String> directoryItem = new TreeItem<>("Directory");
        rootItem.getChildren().add(directoryItem);
        TreeView<String> directoryTree = new TreeView<>(rootItem);
        VBox leftDirectory = new VBox(new Label("ExecutionPane Directory"), directoryTree);
        leftDirectory.setPadding(new Insets(10));

        // Table view in the middle
        TableView<String> tableView = new TableView<>();
        VBox middleTableView = new VBox(new Label("ExecutionPane TableView"), tableView);
        middleTableView.setPadding(new Insets(10));

        // Directory tree on the right
        TreeItem<String> rightRootItem = new TreeItem<>("Right Root");
        TreeItem<String> rightDirectoryItem = new TreeItem<>("Right Directory");
        rightRootItem.getChildren().add(rightDirectoryItem);
        TreeView<String> rightDirectoryTree = new TreeView<>(rightRootItem);
        VBox rightDirectory = new VBox(new Label("ExecutionPane Right Directory"), rightDirectoryTree);
        rightDirectory.setPadding(new Insets(10));

        // Assemble the execution pane layout with button layout
        BorderPane executionLayout = new BorderPane();
        executionLayout.setLeft(buttonLayout);
        executionLayout.setCenter(middleTableView);
        executionLayout.setRight(rightDirectory);

        setCenter(executionLayout);
    }

    private Button createButtonWithImage(String imagePath, double width, double height) {
        Button button = new Button();
        try {
            // Log the image path
            System.out.println("Loading image from: " + getClass().getClassLoader().getResource(imagePath));

            // Use ClassLoader to load the image resource
            Image icon = new Image(getClass().getClassLoader().getResourceAsStream(imagePath));
            ImageView imageView = new ImageView(icon);
            imageView.setFitWidth(width);
            imageView.setFitHeight(height);
            button.setGraphic(imageView);
        } catch (Exception e) {
            System.out.println("Error loading image: " + e.getMessage());
        }
        return button;
    }
}
