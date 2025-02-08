package org.automonius;

import javafx.scene.layout.VBox;

public class MainController {

    private final TreeTableViewComponent treeTableViewComponent;
    private final TableViewComponent tableViewComponent;
    private final VBox mainContainer;

    public MainController(boolean loadProject) {
        tableViewComponent = new TableViewComponent(loadProject);
        mainContainer = new VBox();

        treeTableViewComponent = new TreeTableViewComponent(loadProject, tableViewComponent, mainContainer);
    }

    public VBox getMainContainer() {
        return mainContainer;
    }

    public VBox createObjectRepositoryView() {
        return treeTableViewComponent.createTreeTableView();
    }

    public VBox createReusableComponentTableView() {
        return treeTableViewComponent.createTreeTableView();
    }

    public VBox createTestPlanTableView() {
        return treeTableViewComponent.createTreeTableView();
    }

    public VBox createTableView2() {
        return tableViewComponent.createTableView2();
    }

    public VBox createPropertiesView() {
        return tableViewComponent.createPropertiesView();
    }
}
