package org.automonius;

import javafx.scene.layout.VBox;

public class TreeTableViewComponent {
    private TreeTableViewManager treeTableViewManager;
    private UIManager uiManager;

    private TreeTableViewComponent() {}

    public static TreeTableViewComponent getInstance() {
        return new TreeTableViewComponent();
    }

    public void init(boolean loadProject, TableManager tableManager, VBox mainContainer) {
        treeTableViewManager = new TreeTableViewManager(loadProject, tableManager, mainContainer);
        uiManager = new UIManager();
    }

    public VBox createTreeTableView(boolean isForDirectory) {
        return uiManager.createTreeTableView(treeTableViewManager, isForDirectory);
    }

    public TreeTableViewManager getTreeTableViewManager() {
        return treeTableViewManager;
    }

    public UIManager getUiManager() {
        return uiManager;
    }

}