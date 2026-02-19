package ui;
import javafx.scene.paint.Color;
import model.Coordinate_System;
import javafx.scene.canvas.Canvas;
import rendering.GraphRenderer;
import rendering.GridRenderer;
import rendering.AxisRenderer;
import rendering.GraphRenderer;
import math.GraphFunction;
import java.util.ArrayList;

public class GraphCanvas extends Canvas {

    private final Coordinate_System coordSystem;
    private GridRenderer gridRenderer;
    private AxisRenderer axisRenderer;
    private GraphRenderer graphRenderer;
    private double lastMouseX;
    private double lastMouseY;

    public void setGridRenderer(GridRenderer gridRenderer) {
        this.gridRenderer = gridRenderer;
    }

    public void redraw() {
        coordSystem.setScreenSize(getWidth(), getHeight());
        getGraphicsContext2D().clearRect(0, 0, getWidth(), getHeight());

        if (gridRenderer != null) {
            gridRenderer.drawGrid(getGraphicsContext2D());
        }
        if (axisRenderer!=null)
        {
            axisRenderer.drawAxes(getGraphicsContext2D());
        }
        graphRenderer.drawGraph(getGraphicsContext2D());

    }


    public GraphCanvas(Coordinate_System coordSystem,GridRenderer gridRenderer) {
        this.coordSystem = coordSystem;
        this.gridRenderer = gridRenderer;
        this.axisRenderer = new AxisRenderer(coordSystem);
        this.graphRenderer = new GraphRenderer(coordSystem);
        setOnMousePressed(e -> {
            lastMouseX = e.getX();
            lastMouseY = e.getY();
        });


        // Make the canvas auto-resize with its parent
        widthProperty().addListener(evt -> redraw());
        heightProperty().addListener(evt -> redraw());
        setOnMouseDragged(e -> {
                    double dx = e.getX() - lastMouseX;
                    double dy = e.getY() - lastMouseY;

                    // convert pixels → world units
                    double worldDX = dx * coordSystem.getViewport().getWidth() / getWidth();
                    double worldDY = -dy * coordSystem.getViewport().getHeight() / getHeight(); // negative because screen y is down

                    coordSystem.getViewport().pan(-worldDX, -worldDY); // negative to match drag direction

                    lastMouseX = e.getX();
                    lastMouseY = e.getY();
                    redraw();
                });
        setOnScroll(e -> {
            double factor = 1.1; // zoom factor per scroll step

            if (e.getDeltaY() > 0) {
                factor = 1 / factor; // scroll up -> zoom in
            }

            // mouse position in pixels
            double mouseX = e.getX();
            double mouseY = e.getY();

            // convert to world coordinates
            double worldX = coordSystem.screenToWorldX(mouseX);
            double worldY = coordSystem.screenToWorldY(mouseY);

            // zoom viewport
            coordSystem.getViewport().zoom(factor, worldX, worldY);

            redraw();
        });

        redraw();

    }
    public void setFunction(int index,GraphFunction function) {
        graphRenderer.updateFunction(index, function);
        redraw();
    }

    public void removeFunction(GraphFunction function){
        graphRenderer.removeFunction(function);
        redraw();
    }
}


