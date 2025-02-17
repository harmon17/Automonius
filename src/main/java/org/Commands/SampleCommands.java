package org.Commands;

import org.annotations.Action;
import org.annotations.InputType;
import org.annotations.ObjectType;

public class SampleCommands {

    @Action(object = ObjectType.SELENIUM, desc = "Click on a button", input = InputType.NO)
    public void clickButton() {
        // Implementation to click a button
    }

    @Action(object = ObjectType.SELENIUM, desc = "Double click on a button", input = InputType.NO)
    public void doubleClick() {
        // Implementation to click a button
    }

    @Action(object = ObjectType.BROWSER, desc = "Navigate to a URL", input = InputType.YES)
    public void navigateToUrl(String url) {
        // Implementation to navigate to a URL
    }
}
