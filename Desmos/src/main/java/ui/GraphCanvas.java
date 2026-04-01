package ui;

import javafx.scene.paint.Color;
import model.Coordinate_System;
import javafx.scene.canvas.Canvas;
import model.ShadedRegion;
import rendering.GraphRenderer;
import rendering.GridRenderer;
import rendering.AxisRenderer;
import math.GraphFunction;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.text.Font;

public class GraphCanvas extends Canvas {

    private final Coordinate_System coordSystem;
    private GridRenderer gridRenderer;
    private AxisRenderer axisRenderer;
    private GraphRenderer graphRenderer;
    private double lastMouseX;
    private double lastMouseY;
    private boolean isDarkMode = false;

    public void setDarkMode(boolean dark) {
        this.isDarkMode = dark;
        if (gridRenderer != null) gridRenderer.setDarkMode(dark);
        if (axisRenderer != null) axisRenderer.setDarkMode(dark);
        redraw();
    }
    private double currentMouseX;
    private double currentMouseY;
    private boolean isMouseOnCanvas = false;

    public void setGridRenderer(GridRenderer gridRenderer) {
        this.gridRenderer = gridRenderer;
    }


    public void clearShadedRegions() {
        graphRenderer.clearShadedRegions();
    }

    public void redraw() {
        coordSystem.setScreenSize(getWidth(), getHeight());
        getGraphicsContext2D().clearRect(0, 0, getWidth(), getHeight());
        getGraphicsContext2D().setFill(isDarkMode ? Color.web("#1e1e1e") : Color.WHITE);
        getGraphicsContext2D().fillRect(0, 0, getWidth(), getHeight());

        if (gridRenderer != null) {
            gridRenderer.drawGrid(getGraphicsContext2D());
        }
        if (axisRenderer != null) {
            axisRenderer.drawAxes(getGraphicsContext2D());
        }
        graphRenderer.drawGraph(getGraphicsContext2D());
        if (isMouseOnCanvas) {
            drawMouseCoordinates(getGraphicsContext2D());
        }

    }


    public GraphCanvas(Coordinate_System coordSystem, GridRenderer gridRenderer) {
        this.coordSystem = coordSystem;
        this.gridRenderer = gridRenderer;
        this.axisRenderer = new AxisRenderer(coordSystem);
        this.graphRenderer = new GraphRenderer(coordSystem);
        setOnMousePressed(e -> {
            lastMouseX = e.getX();
            lastMouseY = e.getY();
        });


        widthProperty().addListener(evt -> redraw());
        heightProperty().addListener(evt -> redraw());
        setOnMouseMoved(e -> {
            currentMouseX = e.getX();
            currentMouseY = e.getY();
            isMouseOnCanvas = true;
            redraw();
        });


        setOnMouseExited(e -> {
            isMouseOnCanvas = false;
            redraw();
        });
        setOnMouseDragged(e -> {
            double dx = e.getX() - lastMouseX;
            double dy = e.getY() - lastMouseY;


            double worldDX = dx * coordSystem.getViewport().getWidth() / getWidth();
            double worldDY = -dy * coordSystem.getViewport().getHeight() / getHeight();

            coordSystem.getViewport().pan(-worldDX, -worldDY);

            lastMouseX = e.getX();
            lastMouseY = e.getY();
            currentMouseX = e.getX();
            currentMouseY = e.getY();
            isMouseOnCanvas = true;
            redraw();
        });
        setOnScroll(e -> {
            double factor = 1.1;

            if (e.getDeltaY() > 0) {
                factor = 1 / factor;
            }


            double mouseX = e.getX();
            double mouseY = e.getY();


            double worldX = coordSystem.screenToWorldX(mouseX);
            double worldY = coordSystem.screenToWorldY(mouseY);


            coordSystem.getViewport().zoom(factor, worldX, worldY);

            redraw();
        });

        redraw();

    }

    private void drawMouseCoordinates(GraphicsContext gc) {

        double worldX = coordSystem.screenToWorldX(currentMouseX);
        double worldY = coordSystem.screenToWorldY(currentMouseY);


        String coords = String.format("(%.2f, %.2f)", worldX, worldY);


        gc.setFill(Color.rgb(50, 50, 50, 0.8));
        gc.setFont(Font.font("Arial", 14));


        gc.setFill(isDarkMode ? Color.rgb(40, 40, 40, 0.9) : Color.rgb(255, 255, 255, 0.8));
        gc.fillRoundRect(currentMouseX + 10, currentMouseY + 10, 85, 20, 5, 5);

        gc.setFill(isDarkMode ? Color.WHITE : Color.BLACK);
        gc.fillText(coords, currentMouseX + 15, currentMouseY + 25);
    }

    public void setFunction(int index, GraphFunction function) {
        graphRenderer.updateFunction(index, function);
        redraw();
    }

    public void removeFunction(GraphFunction function) {
        graphRenderer.removeFunction(function);
        redraw();
    }

    public void addShadedRegion(ShadedRegion region) {
        graphRenderer.addShadedRegion(region);
    }
}


