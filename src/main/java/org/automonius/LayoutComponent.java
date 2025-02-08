package org.automonius;

import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.VBox;

public class LayoutComponent {

    public BorderPane createLayout(boolean loadProject) {
        BorderPane layout = new BorderPane();

        MenuBarComponent menuBarComponent = new MenuBarComponent();
        layout.setTop(menuBarComponent.createMenuBar());

        MainController mainController = new MainController(loadProject);
        VBox mainContainer = mainController.getMainContainer();
        VBox objectRepositoryView = mainController.createObjectRepositoryView();
        VBox reusableComponentView = mainController.createReusableComponentTableView();
        VBox testPlanView = mainController.createTestPlanTableView();
        VBox tableView2Box = mainController.createTableView2();
        VBox propertiesBox = mainController.createPropertiesView();

        ColumnConstraints cc1 = new ColumnConstraints();
        cc1.setPercentWidth(20);
        ColumnConstraints cc2 = new ColumnConstraints();
        cc2.setPercentWidth(60);
        ColumnConstraints cc3 = new ColumnConstraints();
        cc3.setPercentWidth(20);

        GridPane topArea = new GridPane();
        topArea.getColumnConstraints().addAll(cc1, cc2, cc3);
        topArea.add(testPlanView, 0, 0);
        topArea.add(mainContainer, 1, 0);
        topArea.add(objectRepositoryView, 2, 0);

        GridPane bottomArea = new GridPane();
        bottomArea.getColumnConstraints().addAll(cc1, cc2, cc3);
        bottomArea.add(reusableComponentView, 0, 0);
        bottomArea.add(tableView2Box, 1, 0);
        bottomArea.add(propertiesBox, 2, 0);

        RowConstraints rc1 = new RowConstraints();
        rc1.setPercentHeight(50);
        RowConstraints rc2 = new RowConstraints();
        rc2.setPercentHeight(50);

        GridPane mainArea = new GridPane();
        mainArea.getRowConstraints().addAll(rc1, rc2);
        mainArea.add(topArea, 0, 0);
        mainArea.add(bottomArea, 0, 1);

        layout.setCenter(mainArea);

        return layout;
    }
}
