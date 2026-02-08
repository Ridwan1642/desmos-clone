package rendering;


import model.Coordinate_System;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public class AxisRenderer {

    private final Coordinate_System coordSystem;

    public AxisRenderer(Coordinate_System coordSystem) {
        this.coordSystem = coordSystem;
    }

    public void drawAxes(GraphicsContext gc) {
        double width = gc.getCanvas().getWidth();
        double height = gc.getCanvas().getHeight();

        gc.setStroke(Color.BLACK);
        gc.setLineWidth(2);

        // X axis
        double y0 = coordSystem.worldToScreenY(0);
        if (y0 >= 0 && y0 <= height) {
            gc.strokeLine(0, y0, width, y0);
        }

        // Y axis
        double x0 = coordSystem.worldToScreenX(0);
        if (x0 >= 0 && x0 <= width) {
            gc.strokeLine(x0, 0, x0, height);
        }
    }
}
