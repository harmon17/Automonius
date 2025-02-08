package org.automonius;

import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;

public class MenuBarComponent {

    public MenuBar createMenuBar() {
        MenuBar menuBar = new MenuBar();
        Menu fileMenu = new Menu("Menu Here");
        MenuItem addButton = new MenuItem("Add Button");
        fileMenu.getItems().add(addButton);
        menuBar.getMenus().add(fileMenu);

        return menuBar;
    }
}
