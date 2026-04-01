package rendering;

import math.GraphFunction;
import model.Coordinate_System;
import model.ShadedRegion;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.List;

public class GraphRenderer {

    private final Coordinate_System coordSystem;
    private List<GraphFunction> functions = new ArrayList<>();
    private List<ShadedRegion> shadedRegions = new ArrayList<>();
    private double lineWidth = 2;

    public GraphRenderer(Coordinate_System coordSystem) {
        this.coordSystem = coordSystem;
    }

    public void addShadedRegion(ShadedRegion region) {
        shadedRegions.add(region);
    }

    public void clearShadedRegions() {
        shadedRegions.clear();
    }

    public void addFunctions(GraphFunction f, Color color) {
        functions.add(f);
    }

    public void updateFunction(int index, GraphFunction f) {
        if (index >= 0 && index < functions.size()) {
            functions.set(index, f);
        } else {
            functions.add(f);
        }
    }

    public void removeFunction(GraphFunction f) {
        if (f != null) {
            functions.remove(f);
        }
    }

    public void drawGraph(GraphicsContext gc) {
        
        for (ShadedRegion region : shadedRegions) {
            double screenStartX = coordSystem.worldToScreenX(region.a);
            double screenEndX = coordSystem.worldToScreenX(region.b);
            int numSteps = (int) Math.max(10, Math.abs(screenEndX - screenStartX));
            double step = (region.b - region.a) / numSteps;

            double[] xPoints = new double[numSteps * 2 + 2];
            double[] yPoints = new double[numSteps * 2 + 2];

            int index = 0;

            
            for (int i = 0; i <= numSteps; i++) {
                double worldX = region.a + (i * step);
                xPoints[index] = coordSystem.worldToScreenX(worldX);
                yPoints[index] = coordSystem.worldToScreenY(region.f1.evaluate(worldX));
                index++;
            }

            
            for (int i = numSteps; i >= 0; i--) {
                double worldX = region.a + (i * step);
                xPoints[index] = coordSystem.worldToScreenX(worldX);
                double yVal = (region.f2 != null) ? region.f2.evaluate(worldX) : 0;
                yPoints[index] = coordSystem.worldToScreenY(yVal);
                index++;
            }

            gc.setFill(region.color.deriveColor(1, 1, 1, 0.3));
            gc.fillPolygon(xPoints, yPoints, index);
        }

        
        if (functions == null) return;

        for (GraphFunction function : functions) {
            if (function.isVisible()) {
                gc.setStroke(function.getColor());
                gc.setLineWidth(lineWidth);

                double prevX = coordSystem.getViewport().getXMin();
                double prevY = function.evaluate(prevX);

                for (int i = 1; i <= gc.getCanvas().getWidth(); i++) {
                    double x = coordSystem.screenToWorldX(i);
                    double y = function.evaluate(x);

                    double sx1 = coordSystem.worldToScreenX(prevX);
                    double sy1 = coordSystem.worldToScreenY(prevY);
                    double sx2 = coordSystem.worldToScreenX(x);
                    double sy2 = coordSystem.worldToScreenY(y);

                    
                    if (!Double.isNaN(y) && !Double.isNaN(prevY) && Math.abs(sy2 - sy1) < gc.getCanvas().getHeight()) {
                        gc.strokeLine(sx1, sy1, sx2, sy2);
                    }

                    
                    prevX = x;
                    prevY = y;
                }
            }
        }
    }
}