package org.automonius;

import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public class CanvasController {

    @FXML private Canvas canvas;

    @FXML
    public void initialize() {
        GraphicsContext gc = canvas.getGraphicsContext2D();

        // Fill background
        gc.setFill(Color.DARKGRAY);
        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());

        // Example: draw test result icons
        gc.setFill(Color.LIMEGREEN);
        gc.fillOval(50, 50, 40, 40); // ✔ placeholder

        gc.setFill(Color.RED);
        gc.fillOval(120, 50, 40, 40); // ✘ placeholder

    }


    public void drawPassIcon(double x, double y) {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setFill(Color.LIMEGREEN);
        gc.fillOval(x, y, 40, 40); // green circle
        gc.setStroke(Color.WHITE);
        gc.setLineWidth(3);
        gc.strokeLine(x + 10, y + 20, x + 18, y + 28); // checkmark part 1
        gc.strokeLine(x + 18, y + 28, x + 30, y + 12); // checkmark part 2
    }

    public void drawFailIcon(double x, double y) {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setFill(Color.RED);
        gc.fillOval(x, y, 40, 40); // red circle
        gc.setStroke(Color.WHITE);
        gc.setLineWidth(3);
        gc.strokeLine(x + 12, y + 12, x + 28, y + 28); // X part 1
        gc.strokeLine(x + 28, y + 12, x + 12, y + 28); // X part 2
    }

    public void clearCanvas() {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
    }
}
