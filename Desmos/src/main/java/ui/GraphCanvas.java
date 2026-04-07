package ui;

import javafx.animation.PauseTransition;
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
import javafx.scene.transform.Scale;


public class GraphCanvas extends Canvas {

    private final Coordinate_System coordSystem;
    private GridRenderer gridRenderer;
    private AxisRenderer axisRenderer;
    private GraphRenderer graphRenderer;

    private Canvas mathCanvas;
    private Canvas overlayCanvas;

    private double lastMouseX, lastMouseY;
    private double currentMouseX, currentMouseY;
    private boolean isMouseOnCanvas = false;
    private boolean isDarkMode = false;

    private double totalDragX = 0, totalDragY = 0;
    private ShadedRegion activeRegion = null;
    private String draggingBound = null;
    private java.util.function.BiConsumer<Double, Double> onBoundsChanged = null;

    private double introProgress = 1.0;

    public void setIntroProgress(double progress){
        this.introProgress = progress;
    }

    private PauseTransition zoomTimer = new PauseTransition(javafx.util.Duration.millis(50));
    private PauseTransition dragTimer = new PauseTransition(javafx.util.Duration.millis(30));
    public GraphCanvas(Coordinate_System coordSystem, GridRenderer gridRenderer) {
        this.coordSystem = coordSystem;
        this.gridRenderer = gridRenderer;
        this.axisRenderer = new AxisRenderer(coordSystem);
        this.graphRenderer = new GraphRenderer(coordSystem);

        zoomTimer.setOnFinished(e -> redrawMath());
        dragTimer.setOnFinished(e -> redrawMath());

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
            draggingBound = null;

            if (activeRegion != null) {
                double tolerance = 15;
                if (!activeRegion.isRespectToY) {
                    double sxA = coordSystem.worldToScreenX(activeRegion.a);
                    double sxB = coordSystem.worldToScreenX(activeRegion.b);
                    if (Math.abs(e.getX() - sxA) < tolerance) draggingBound = "a";
                    else if (Math.abs(e.getX() - sxB) < tolerance) draggingBound = "b";
                } else {
                    double syA = coordSystem.worldToScreenY(activeRegion.a);
                    double syB = coordSystem.worldToScreenY(activeRegion.b);
                    if (Math.abs(e.getY() - syA) < tolerance) draggingBound = "a";
                    else if (Math.abs(e.getY() - syB) < tolerance) draggingBound = "b";
                }
            }
        });

        setOnMouseMoved(e -> {
            currentMouseX = e.getX();
            currentMouseY = e.getY();
            isMouseOnCanvas = true;
            drawMouseCoordinates();
        });

        setOnMouseExited(e -> {
            isMouseOnCanvas = false;
            if (overlayCanvas != null) {
                overlayCanvas.getGraphicsContext2D().clearRect(0, 0, getWidth(), getHeight());
            }
        });

        setOnMouseDragged(e -> {
            if (draggingBound != null && activeRegion != null && onBoundsChanged != null) {
                if (!activeRegion.isRespectToY) {
                    double newWorldX = coordSystem.screenToWorldX(e.getX());
                    if (draggingBound.equals("a")) activeRegion.a = newWorldX;
                    else activeRegion.b = newWorldX;
                } else {
                    double newWorldY = coordSystem.screenToWorldY(e.getY());
                    if (draggingBound.equals("a")) activeRegion.a = newWorldY;
                    else activeRegion.b = newWorldY;
                }
                onBoundsChanged.accept(activeRegion.a, activeRegion.b);
                dragTimer.playFromStart();
                return;
            }
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
            redrawMath();
        });

        setOnScroll(e -> {
            double factor = 1.1;
            if (e.getDeltaY() > 0) factor = 1 / factor;

            double worldX = coordSystem.screenToWorldX(e.getX());
            double worldY = coordSystem.screenToWorldY(e.getY());
            coordSystem.getViewport().zoom(factor, worldX, worldY);

            redrawGridOnly();

            if (mathCanvas != null) {
                double visualFactor = 1.0 / factor;
                mathCanvas.getTransforms().add(new Scale(visualFactor, visualFactor, e.getX(), e.getY()));
            }

            drawMouseCoordinates();
            zoomTimer.playFromStart();
        });

        redraw();
    }

    public void setCanvases(Canvas math, Canvas overlay) {
        this.mathCanvas = math;
        this.overlayCanvas = overlay;
    }
    public void setInteractiveIntegration(ShadedRegion region, java.util.function.BiConsumer<Double, Double> callback) {
        this.activeRegion = region;
        this.onBoundsChanged = callback;
        graphRenderer.clearShadedRegions();
        graphRenderer.addShadedRegion(region);
        redrawMath();
    }

    public void clearInteractiveIntegration() {
        this.activeRegion = null;
        this.onBoundsChanged = null;
        graphRenderer.clearShadedRegions();
        redrawMath();
    }

    public void setDarkMode(boolean dark) {
        this.isDarkMode = dark;
        redraw();
    }

    public void addTangentPoint(double x, double y, Color color) {
        graphRenderer.addTangentPoint(x, y, color);
        redrawMath();
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

        if (gridRenderer != null) gridRenderer.drawGrid(gc, isDarkMode);
        if (axisRenderer != null) axisRenderer.drawAxes(gc, isDarkMode);
    }

    private void redrawMath() {
        if (mathCanvas != null) {
            graphRenderer.drawGraph(mathCanvas.getGraphicsContext2D(), isDarkMode, () -> {
                mathCanvas.getTransforms().clear();
                mathCanvas.setTranslateX(0);
                mathCanvas.setTranslateY(0);
                totalDragX = 0;
                totalDragY = 0;
            });
        }
    }
    public void clearScatterPoints() {
        graphRenderer.clearScatterPoints();
    }

    public void addScatterPoint(double x, double y, Color c) {
        graphRenderer.addScatterPoint(x, y, c);
    }

    private void drawMouseCoordinates() {
        if (overlayCanvas == null) return;
        GraphicsContext gc = overlayCanvas.getGraphicsContext2D();
        gc.clearRect(0, 0, getWidth(), getHeight());

        if (!isMouseOnCanvas) return;

        double mouseX = currentMouseX;
        double mouseY = currentMouseY;
        double worldX = coordSystem.screenToWorldX(mouseX);
        double worldY = coordSystem.screenToWorldY(mouseY);

        boolean snapped = false;
        Color snapColor = isDarkMode ? Color.LIGHTGRAY : Color.DARKGRAY;

        for (GraphRenderer.KeyPoint kp : graphRenderer.getAllKeyPoints()) {
            double sx = coordSystem.worldToScreenX(kp.x);
            double sy = coordSystem.worldToScreenY(kp.y);

            if (Math.hypot(mouseX - sx, mouseY - sy) < 12) {
                worldX = kp.x;
                worldY = kp.y;
                mouseX = sx;
                mouseY = sy;
                snapped = true;
                snapColor = kp.color;
                break;
            }
        }

        String coords = String.format("(%.2f, %.2f)", worldX, worldY);

        if (snapped) {
            gc.setFont(Font.font("Arial", javafx.scene.text.FontWeight.BOLD, 14));
            gc.setFill(isDarkMode ? Color.rgb(40, 40, 40, 0.95) : Color.rgb(255, 255, 255, 0.95));
            gc.fillRoundRect(mouseX + 12, mouseY - 25, 95, 25, 5, 5);
            gc.setStroke(snapColor);
            gc.setLineWidth(1);
            gc.strokeRoundRect(mouseX + 12, mouseY - 25, 95, 25, 5, 5);

            gc.setFill(snapColor);
            gc.fillText(coords, mouseX + 17, mouseY - 7);

            gc.setFill(isDarkMode ? Color.web("#1e1e1e") : Color.WHITE);
            gc.fillOval(mouseX - 6, mouseY - 6, 12, 12);
            gc.setStroke(snapColor);
            gc.setLineWidth(3);
            gc.strokeOval(mouseX - 6, mouseY - 6, 12, 12);
        } else {
            gc.setFont(Font.font("Arial", 14));
            gc.setFill(isDarkMode ? Color.rgb(40, 40, 40, 0.8) : Color.rgb(255, 255, 255, 0.8));
            gc.fillRoundRect(mouseX + 10, mouseY + 10, 85, 20, 5, 5);

            gc.setFill(isDarkMode ? Color.WHITE : Color.BLACK);
            gc.fillText(coords, mouseX + 15, mouseY + 25);

            gc.setStroke(snapColor);
            gc.setLineWidth(1);
            gc.strokeLine(mouseX - 5, mouseY, mouseX + 5, mouseY);
            gc.strokeLine(mouseX, mouseY - 5, mouseX, mouseY + 5);
        }
    }

    public void setFunction(int index, GraphFunction function) {
        graphRenderer.updateFunction(index, function);
        redrawMath();
    }

    public void removeFunction(GraphFunction function) {
        graphRenderer.removeFunction(function);
        redrawMath();
    }

    public void addShadedRegion(ShadedRegion region) {
        graphRenderer.addShadedRegion(region);
        redrawMath();
    }
    public void clearTangentPoints() {
        graphRenderer.clearTangentPoints();
        redrawMath();
    }

    public void addTangentLine(double x, double y, double m, Color color) {
        graphRenderer.addTangentLine(x, y, m, color.deriveColor(0, 1, 1, 0.7));
        redrawMath();
    }

    public void clearTangentLines() {
        graphRenderer.clearTangentLines();
        redrawMath();
    }
}