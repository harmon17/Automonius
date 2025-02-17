package org.automonius;

import javafx.scene.layout.VBox;

public class TreeTableViewComponent {
    private final TreeTableViewManager treeTableViewManager;
    private final UIManager uiManager;

    public TreeTableViewComponent(boolean loadProject, TableManager tableManager, VBox mainContainer) {
        treeTableViewManager = new TreeTableViewManager(loadProject, tableManager, mainContainer);
        uiManager = new UIManager();
    }

    public VBox createTreeTableView(boolean isForDirectory) {
        return uiManager.createTreeTableView(treeTableViewManager, isForDirectory);
    }
}
