package org.automonius;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class UIManager {
    public Button createButtonWithImage(String imagePath) {
        Button button = new Button();
        Image icon = new Image(getClass().getClassLoader().getResourceAsStream(imagePath));
        ImageView imageView = new ImageView(icon);
        imageView.setFitHeight(30);
        imageView.setFitWidth(30);
        button.setGraphic(imageView);
        return button;
    }

    public VBox createTreeTableView(TreeTableViewManager treeTableViewManager, boolean isForDirectory) {
        VBox vBox = new VBox();

        if (isForDirectory) {
            Button addDirectoryButton = createButtonWithImage("IMAGE/MD.png");
            Button addSubDirectoryButton = createButtonWithImage("IMAGE/SD.png");
            Button addTableViewButton = createButtonWithImage("IMAGE/table.png");
            Button deleteButton = createButtonWithImage("IMAGE/delete.png");

            addDirectoryButton.setOnAction(e -> treeTableViewManager.addMainDirectory());
            addSubDirectoryButton.setOnAction(e -> treeTableViewManager.addSubDirectory());
            addTableViewButton.setOnAction(e -> treeTableViewManager.addTableView());
            deleteButton.setOnAction(e -> treeTableViewManager.deleteItem());

            HBox buttonBox = new HBox(10, addDirectoryButton, addSubDirectoryButton, addTableViewButton, deleteButton);
            buttonBox.setAlignment(Pos.CENTER_LEFT);
            buttonBox.setPadding(new Insets(10));

            vBox.getChildren().addAll(buttonBox, treeTableViewManager.getTreeTableView());
        } else {
            vBox.getChildren().add(treeTableViewManager.getTreeTableView());
        }

        return vBox;
    }
}
