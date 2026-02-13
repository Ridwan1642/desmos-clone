package rendering;

import math.GraphFunction;
import model.Coordinate_System;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import java.util.ArrayList;
import java.util.List;


public class GraphRenderer {

    private final Coordinate_System coordSystem;
    private List<GraphFunction> functions = new ArrayList<>();
    private double lineWidth = 2;

    public GraphRenderer(Coordinate_System coordSystem) {
        this.coordSystem = coordSystem;
    }

    public void addFunctions(GraphFunction f, Color color){
        functions.add(f);
    }

    public void updateFunction(int index, GraphFunction f) {
        if (index >= 0 && index < functions.size()) {
            functions.set(index, f);
        } else {
            functions.add(f);
        }
    }

    public void drawGraph(GraphicsContext gc) {
        if (functions == null) return;

        for(GraphFunction function : functions)
        {
            if(function.isVisible()) {
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

                    gc.strokeLine(sx1, sy1, sx2, sy2);

                    prevX = x;
                    prevY = y;
                }
            }

        }
    }
}
