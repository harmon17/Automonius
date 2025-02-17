package org.automonius;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.annotations.Action;
import org.annotations.InputType;

import java.lang.reflect.Method;
import java.util.List;
import java.util.stream.Collectors;

public class TableViewComponent {
    private final TableManager tableManager;
    private final List<Method> actions;

    public TableViewComponent(boolean loadProject, TableManager tableManager) {
        this.tableManager = tableManager;
        this.actions = tableManager.getActions();
    }

    public VBox createTableView2() {
        TableView<ActionData> tableView2 = tableManager.createNewTableView("TableView2", actions);

        Button addRowButton = new Button("Add Row");
        Button deleteRowButton = new Button("Delete Row");
        addRowButton.setOnAction(e -> {
            ActionData newRow = new ActionData("", "", "", InputType.NONE);  // Correct InputType should be set here
            tableView2.getItems().add(newRow);
        });

        deleteRowButton.setOnAction(e -> {
            ActionData selectedItem = tableView2.getSelectionModel().getSelectedItem();
            if (selectedItem != null && tableView2.getItems().size() > 1) {
                tableView2.getItems().remove(selectedItem);
            }
        });

        HBox rowButtons = new HBox(10, addRowButton, deleteRowButton);
        VBox tableView2Box = new VBox(10, new Label("TableView2"), tableView2, rowButtons);
        tableView2Box.setPadding(new Insets(10));
        return tableView2Box;
    }

}
