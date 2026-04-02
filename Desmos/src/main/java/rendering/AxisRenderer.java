

package rendering;

import model.Coordinate_System;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

public class AxisRenderer {
    private final Font labelFont = Font.font("Arial", 12);
    private final Coordinate_System coordSystem;
    private boolean isDarkMode = false;
    public void setDarkMode(boolean dark) { this.isDarkMode = dark; }

    public AxisRenderer(Coordinate_System coordSystem) {
        this.coordSystem = coordSystem;
    }

    public void drawAxes(GraphicsContext gc, boolean isDark) {
        double width = gc.getCanvas().getWidth();
        double height = gc.getCanvas().getHeight();
        this.isDarkMode = isDark;

        gc.setStroke(isDarkMode ? Color.web("#cccccc") : Color.BLACK);
        gc.setLineWidth(2);


        double y0 = coordSystem.worldToScreenY(0);
        if (y0 >= 0 && y0 <= height) {
            gc.strokeLine(0, y0, width, y0);
        }


        double x0 = coordSystem.worldToScreenX(0);
        if (x0 >= 0 && x0 <= width) {
            gc.strokeLine(x0, 0, x0, height);
        }


        drawLabels(gc, width, height, x0, y0);
    }

    private void drawLabels(GraphicsContext gc, double width, double height, double x0, double y0) {
        gc.setFill(isDarkMode ? Color.web("#cccccc") : Color.BLACK);
        gc.setFont(labelFont);

        double step = computeStep();


        double labelYPos = Math.max(15, Math.min(y0 + 15, height - 5));
        double labelXPos = Math.max(45, Math.min(x0 - 8, width - 5));


        gc.setTextAlign(javafx.scene.text.TextAlignment.CENTER);

        double xMin = coordSystem.getViewport().getXMin();
        double xMax = coordSystem.getViewport().getXMax();
        double xStart = Math.ceil(xMin / step) * step;

        double lastDrawnX = -1000;

        for (double x = xStart; x <= xMax + 1e-10; x += step) {
            if (Math.abs(x) < 1e-10) continue;

            double sx = coordSystem.worldToScreenX(x);


            if (sx - lastDrawnX > 60) {
                gc.fillText(formatNumber(x, step), sx, labelYPos);
                lastDrawnX = sx;
            }
        }


        gc.setTextAlign(javafx.scene.text.TextAlignment.RIGHT);

        double yMin = coordSystem.getViewport().getYMin();
        double yMax = coordSystem.getViewport().getYMax();
        double yStart = Math.ceil(yMin / step) * step;

        double lastDrawnY = height + 1000;

        for (double y = yStart; y <= yMax + 1e-10; y += step) {
            if (Math.abs(y) < 1e-10) continue;

            double sy = coordSystem.worldToScreenY(y);


            if (lastDrawnY - sy > 20) {
                gc.fillText(formatNumber(y, step), labelXPos, sy + 4);
                lastDrawnY = sy;
            }
        }


        gc.fillText("0", labelXPos, labelYPos);


        gc.setTextAlign(javafx.scene.text.TextAlignment.LEFT);
    }


    private double computeStep() {
        double approxStep = coordSystem.getViewport().getWidth() / 10;
        double pow10 = Math.pow(10, Math.floor(Math.log10(approxStep)));
        double step;

        if (approxStep / pow10 < 2) step = pow10 / 2;
        else if (approxStep / pow10 < 5) step = pow10;
        else step = pow10 * 2;

        return step;
    }


    private String formatNumber(double num, double step) {

        if (Math.abs(num) < 1e-10) {
            return "0";
        }


        int decimalPlaces = (int) Math.max(0, -Math.floor(Math.log10(step)));


        String formatString = "%." + decimalPlaces + "f";

        return String.format(formatString, num);
    }
}