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

    // The Glass Panes
    private Canvas mathCanvas;
    private Canvas overlayCanvas;

    private double lastMouseX, lastMouseY;
    private double currentMouseX, currentMouseY;
    private boolean isMouseOnCanvas = false;
    private boolean isDarkMode = false;

    private double totalDragX = 0, totalDragY = 0;
    private javafx.animation.PauseTransition zoomTimer = new javafx.animation.PauseTransition(javafx.util.Duration.millis(150));

    public GraphCanvas(Coordinate_System coordSystem, GridRenderer gridRenderer) {
        this.coordSystem = coordSystem;
        this.gridRenderer = gridRenderer;
        this.axisRenderer = new AxisRenderer(coordSystem);
        this.graphRenderer = new GraphRenderer(coordSystem);

        zoomTimer.setOnFinished(e -> redrawMath());

        widthProperty().addListener(evt -> {
            coordSystem.enforceAspectRatio(getWidth(), getHeight());
            redraw();
        });
        heightProperty().addListener(evt -> {
            coordSystem.enforceAspectRatio(getWidth(), getHeight());
            redraw();
        });

        setOnMousePressed(e -> {
            lastMouseX = e.getX();
            lastMouseY = e.getY();
            totalDragX = 0;
            totalDragY = 0;
        });

        setOnMouseMoved(e -> {
            currentMouseX = e.getX();
            currentMouseY = e.getY();
            isMouseOnCanvas = true;
            drawMouseCoordinates(); // Draw to glass pane, NOT full redraw!
        });

        setOnMouseExited(e -> {
            isMouseOnCanvas = false;
            if (overlayCanvas != null) {
                overlayCanvas.getGraphicsContext2D().clearRect(0, 0, getWidth(), getHeight());
            }
        });

        setOnMouseDragged(e -> {
            double dx = e.getX() - lastMouseX;
            double dy = e.getY() - lastMouseY;

            totalDragX += dx;
            totalDragY += dy;
            if (mathCanvas != null) {
                mathCanvas.setTranslateX(totalDragX);
                mathCanvas.setTranslateY(totalDragY);
            }

            double worldDX = dx * coordSystem.getViewport().getWidth() / getWidth();
            double worldDY = -dy * coordSystem.getViewport().getHeight() / getHeight();
            coordSystem.getViewport().pan(-worldDX, -worldDY);

            lastMouseX = e.getX();
            lastMouseY = e.getY();
            currentMouseX = e.getX();
            currentMouseY = e.getY();

            redrawGridOnly();
            drawMouseCoordinates();
        });

        setOnMouseReleased(e -> {
            if (mathCanvas != null) {
                mathCanvas.setTranslateX(0);
                mathCanvas.setTranslateY(0);
            }
            redrawMath();
        });

        setOnScroll(e -> {
            double factor = 1.1;
            if (e.getDeltaY() > 0) factor = 1 / factor;

            double worldX = coordSystem.screenToWorldX(e.getX());
            double worldY = coordSystem.screenToWorldY(e.getY());
            coordSystem.getViewport().zoom(factor, worldX, worldY);

            redrawGridOnly();
            if(mathCanvas != null) mathCanvas.getGraphicsContext2D().clearRect(0,0,getWidth(),getHeight());
            drawMouseCoordinates();
            zoomTimer.playFromStart();
        });

        redraw();
    }

    public void setCanvases(Canvas math, Canvas overlay) {
        this.mathCanvas = math;
        this.overlayCanvas = overlay;
    }

    public void setDarkMode(boolean dark) {
        this.isDarkMode = dark;
        redraw();
    }

    public void setGridRenderer(GridRenderer gridRenderer) {
        this.gridRenderer = gridRenderer;
    }

    public void clearShadedRegions() {
        graphRenderer.clearShadedRegions();
    }

    public void redraw() {
        redrawGridOnly();
        redrawMath();
    }

    private void redrawGridOnly() {
        coordSystem.setScreenSize(getWidth(), getHeight());
        GraphicsContext gc = getGraphicsContext2D();

        gc.clearRect(0, 0, getWidth(), getHeight());
        gc.setFill(isDarkMode ? Color.web("#1e1e1e") : Color.WHITE);
        gc.fillRect(0, 0, getWidth(), getHeight());

        if (gridRenderer != null) gridRenderer.drawGrid(gc, isDarkMode); // Pass dark mode flag
        if (axisRenderer != null) axisRenderer.drawAxes(gc, isDarkMode); // Pass dark mode flag
    }

    private void redrawMath() {
        if (mathCanvas != null) {
            graphRenderer.drawGraph(mathCanvas.getGraphicsContext2D());
        }
    }

    private void drawMouseCoordinates() {
        if (overlayCanvas == null) return;
        GraphicsContext gc = overlayCanvas.getGraphicsContext2D();
        gc.clearRect(0, 0, getWidth(), getHeight());

        if (!isMouseOnCanvas) return;

        double worldX = coordSystem.screenToWorldX(currentMouseX);
        double worldY = coordSystem.screenToWorldY(currentMouseY);
        String coords = String.format("(%.2f, %.2f)", worldX, worldY);

        gc.setFont(Font.font("Arial", 14));
        gc.setFill(isDarkMode ? Color.rgb(40, 40, 40, 0.9) : Color.rgb(255, 255, 255, 0.8));
        gc.fillRoundRect(currentMouseX + 10, currentMouseY + 10, 85, 20, 5, 5);

        gc.setFill(isDarkMode ? Color.WHITE : Color.BLACK);
        gc.fillText(coords, currentMouseX + 15, currentMouseY + 25);
    }

    public void setFunction(int index, GraphFunction function) {
        graphRenderer.updateFunction(index, function);
        redrawMath(); // Only redraw math!
    }

    public void removeFunction(GraphFunction function) {
        graphRenderer.removeFunction(function);
        redrawMath();
    }

    public void addShadedRegion(ShadedRegion region) {
        graphRenderer.addShadedRegion(region);
        redrawMath();
    }
}