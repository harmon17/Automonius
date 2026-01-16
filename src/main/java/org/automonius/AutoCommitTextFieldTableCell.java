package org.automonius;

import javafx.scene.control.TextField;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.util.converter.DefaultStringConverter;

public class AutoCommitTextFieldTableCell<S> extends TextFieldTableCell<S, String> {
    public AutoCommitTextFieldTableCell() {
        super(new DefaultStringConverter());
    }

    @Override
    public void startEdit() {
        super.startEdit();
        if (getGraphic() instanceof TextField textField) {
            // Commit automatically when focus is lost
            textField.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
                if (!isNowFocused) {
                    commitEdit(textField.getText());
                }
            });
        }
    }
}
