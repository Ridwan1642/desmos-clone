package rendering;

import model.Coordinate_System;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public class GridRenderer {

    private final Coordinate_System coordSystem;

    public GridRenderer(Coordinate_System coordSystem) {
        this.coordSystem = coordSystem;
    }

    // Compute a “nice” grid step depending on zoom level
    private double computeGridStep() {
        double approxStep = coordSystem.getViewport().getWidth() / 10; // ~10 lines
        double pow10 = Math.pow(10, Math.floor(Math.log10(approxStep)));
        double step;

        if (approxStep / pow10 < 2) step = pow10 / 2;
        else if (approxStep / pow10 < 5) step = pow10;
        else step = pow10 * 2;

        return step;
    }

    public void drawGrid(GraphicsContext gc) {
        double width = gc.getCanvas().getWidth();
        double height = gc.getCanvas().getHeight();
        double step = computeGridStep();

        gc.setStroke(Color.LIGHTGRAY);
        gc.setLineWidth(1);

        double xMin = coordSystem.getViewport().getXMin();
        double xMax = coordSystem.getViewport().getXMax();
        double yMin = coordSystem.getViewport().getYMin();
        double yMax = coordSystem.getViewport().getYMax();

        // --- Vertical lines ---
        double xStart = Math.ceil(xMin / step) * step;
        for (double x = xStart; x <= xMax + 1e-10; x += step) { // small epsilon
            double sx = coordSystem.worldToScreenX(x);
            gc.strokeLine(sx, 0, sx, height);
        }


        // --- Horizontal lines ---
        double yStart = Math.ceil(yMin / step) * step;
        for (double y = yStart; y <= yMax; y += step) {
            double sy = coordSystem.worldToScreenY(y);
            gc.strokeLine(0, sy, width, sy);
        }
    }
}
