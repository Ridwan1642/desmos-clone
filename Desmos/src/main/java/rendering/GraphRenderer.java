package rendering;

import javafx.application.Platform;
import math.GraphFunction;
import model.Coordinate_System;
import model.ShadedRegion;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.ArrayList;
import java.util.List;

public class GraphRenderer {

    public static class KeyPoint {
        public double x, y;
        public Color color;
        public KeyPoint(double x, double y, Color color) {
            this.x = x; this.y = y; this.color = color;
        }
    }

    private final Coordinate_System coordSystem;
    private List<GraphFunction> functions = new ArrayList<>();
    private List<ShadedRegion> shadedRegions = new ArrayList<>();

    private List<KeyPoint> dynamicIntercepts = new ArrayList<>();
    private List<KeyPoint> tangentPoints = new ArrayList<>();

    private double lineWidth = 2;
    private int res = 1;
    private final AtomicInteger renderVersion = new AtomicInteger(0);

    public GraphRenderer(Coordinate_System coordSystem) {
        this.coordSystem = coordSystem;
    }

    public void addTangentPoint(double x, double y, Color c) {
        tangentPoints.add(new KeyPoint(x, y, c));
    }

    public void clearTangentPoints() {
        tangentPoints.clear();
    }

    public List<KeyPoint> getAllKeyPoints() {
        List<KeyPoint> all = new ArrayList<>(dynamicIntercepts);
        all.addAll(tangentPoints);
        return all;
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
        clearTangentPoints();
    }

    public void drawGraph(GraphicsContext gc, boolean isDarkMode, Runnable onRenderComplete) {
        int currentVersion = renderVersion.incrementAndGet();
        double width = gc.getCanvas().getWidth();
        double height = gc.getCanvas().getHeight();

        if (width <= 0 || height <= 0) return;

        List<GraphFunction> renderFunctions = new ArrayList<>(functions);
        List<ShadedRegion> renderRegions = new ArrayList<>(shadedRegions);

        new Thread(() -> {
            List<Runnable> drawCalls = new ArrayList<>();
            List<KeyPoint> newIntercepts = new ArrayList<>();

            for (ShadedRegion region : renderRegions) {
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

                int finalIndex = index;
                drawCalls.add(() -> {
                    gc.setFill(region.color.deriveColor(1, 1, 1, 0.3));
                    gc.fillPolygon(xPoints, yPoints, finalIndex);
                });
            }

            for (GraphFunction function : renderFunctions) {
                if (!function.isVisible()) continue;

                Color funcColor = function.getColor();

                if (function.isImplicit()) {
                    int cols = (int) (width / res) + 1;
                    int rows = (int) (height / res) + 1;
                    double[][] values = new double[cols + 1][rows + 1];
                    double epsX = 0.0000123;
                    double epsY = 0.0000345;

                    for (int i = 0; i <= cols; i++) {
                        for (int j = 0; j <= rows; j++) {
                            if (renderVersion.get() != currentVersion) return;
                            double worldX = coordSystem.screenToWorldX(i * res) + epsX;
                            double worldY = coordSystem.screenToWorldY(j * res) + epsY;
                            values[i][j] = function.evaluate(worldX, worldY);
                        }
                    }

                    // --- RADAR SWEEP: Implicit X & Y Intercepts ---
                    double prevValX = function.evaluate(coordSystem.screenToWorldX(0), 0);
                    for (int i = 1; i <= width; i += 2) {
                        double wx = coordSystem.screenToWorldX(i);
                        double val = function.evaluate(wx, 0);
                        if (prevValX * val <= 0 && !Double.isNaN(val) && !Double.isNaN(prevValX)) {
                            double prevWx = coordSystem.screenToWorldX(i - 2);
                            double rootX = prevWx - prevValX * (wx - prevWx) / (val - prevValX);
                            newIntercepts.add(new KeyPoint(rootX, 0, funcColor));
                        }
                        prevValX = val;
                    }

                    double prevValY = function.evaluate(0, coordSystem.screenToWorldY(0));
                    for (int j = 1; j <= height; j += 2) {
                        double wy = coordSystem.screenToWorldY(j);
                        double val = function.evaluate(0, wy);
                        if (prevValY * val <= 0 && !Double.isNaN(val) && !Double.isNaN(prevValY)) {
                            double prevWy = coordSystem.screenToWorldY(j - 2);
                            double rootY = prevWy - prevValY * (wy - prevWy) / (val - prevValY);
                            newIntercepts.add(new KeyPoint(0, rootY, funcColor));
                        }
                        prevValY = val;
                    }

                    drawCalls.add(() -> {
                        gc.save();
                        gc.setStroke(funcColor);
                        gc.setLineWidth(lineWidth);
                        gc.setLineCap(StrokeLineCap.ROUND);
                        gc.setLineJoin(StrokeLineJoin.ROUND);

                        for (int i = 0; i < cols; i++) {
                            for (int j = 0; j < rows; j++) {
                                double v0 = values[i][j];
                                double v1 = values[i + 1][j];
                                double v2 = values[i + 1][j + 1];
                                double v3 = values[i][j + 1];

                                if (Double.isNaN(v0) || Double.isNaN(v1) || Double.isNaN(v2) || Double.isNaN(v3)) continue;

                                int state = getState(v0, v1, v2, v3);
                                if (state == 0 || state == 15) continue;

                                double x = i * res;
                                double y = j * res;

                                double aX = x + res * interpolate(v0, v1);
                                double aY = y;
                                double bX = x + res;
                                double bY = y + res * interpolate(v1, v2);
                                double cX = x + res * interpolate(v3, v2);
                                double cY = y + res;
                                double dX = x;
                                double dY = y + res * interpolate(v0, v3);

                                switch (state) {
                                    case 1: gc.strokeLine(cX, cY, dX, dY); break;
                                    case 2: gc.strokeLine(bX, bY, cX, cY); break;
                                    case 3: gc.strokeLine(bX, bY, dX, dY); break;
                                    case 4: gc.strokeLine(aX, aY, bX, bY); break;
                                    case 5: gc.strokeLine(aX, aY, dX, dY); gc.strokeLine(bX, bY, cX, cY); break;
                                    case 6: gc.strokeLine(aX, aY, cX, cY); break;
                                    case 7: gc.strokeLine(aX, aY, dX, dY); break;
                                    case 8: gc.strokeLine(aX, aY, dX, dY); break;
                                    case 9: gc.strokeLine(aX, aY, cX, cY); break;
                                    case 10: gc.strokeLine(aX, aY, bX, bY); gc.strokeLine(cX, cY, dX, dY); break;
                                    case 11: gc.strokeLine(aX, aY, bX, bY); break;
                                    case 12: gc.strokeLine(bX, bY, dX, dY); break;
                                    case 13: gc.strokeLine(bX, bY, cX, cY); break;
                                    case 14: gc.strokeLine(cX, cY, dX, dY); break;
                                }
                            }
                        }
                        gc.restore();
                    });

                } else {
                    int w = (int) width;
                    double[] xVals = new double[w + 1];
                    double[] yVals = new double[w + 1];

                    for (int i = 0; i <= w; i++) {
                        if (renderVersion.get() != currentVersion) return;
                        xVals[i] = coordSystem.screenToWorldX(i);
                        yVals[i] = function.evaluate(xVals[i]);
                    }

                    drawCalls.add(() -> {
                        gc.save();
                        gc.setStroke(funcColor);
                        gc.setLineWidth(lineWidth);

                        double prevX = xVals[0];
                        double prevY = yVals[0];

                        for (int i = 1; i <= w; i++) {
                            double x = xVals[i];
                            double y = yVals[i];

                            double sx1 = coordSystem.worldToScreenX(prevX);
                            double sy1 = coordSystem.worldToScreenY(prevY);
                            double sx2 = coordSystem.worldToScreenX(x);
                            double sy2 = coordSystem.worldToScreenY(y);

                            if (!Double.isNaN(y) && !Double.isNaN(prevY) && Math.abs(sy2 - sy1) < height) {
                                gc.strokeLine(sx1, sy1, sx2, sy2);

                                if (prevY * y <= 0 && Math.abs(y - prevY) > 1e-10) {
                                    double rootX = prevX - prevY * (x - prevX) / (y - prevY);
                                    newIntercepts.add(new KeyPoint(rootX, 0, funcColor));
                                }

                                if (prevX <= 0 && x >= 0 && Math.abs(x - prevX) > 1e-10) {
                                    double rootY = prevY + (0 - prevX) * (y - prevY) / (x - prevX);
                                    newIntercepts.add(new KeyPoint(0, rootY, funcColor));
                                }
                            }

                            prevX = x;
                            prevY = y;
                        }
                        gc.restore();
                    });
                }
            }

            if (renderVersion.get() != currentVersion) return;

            Platform.runLater(() -> {
                if (renderVersion.get() != currentVersion) return;

                dynamicIntercepts = newIntercepts;

                if (onRenderComplete != null) onRenderComplete.run();

                gc.clearRect(0, 0, width, height);

                for (Runnable drawCall : drawCalls) {
                    drawCall.run();
                }

                Color dotInside = isDarkMode ? Color.web("#1e1e1e") : Color.WHITE;
                for (KeyPoint kp : getAllKeyPoints()) {
                    double sx = coordSystem.worldToScreenX(kp.x);
                    double sy = coordSystem.worldToScreenY(kp.y);

                    if (sx >= -10 && sx <= width + 10 && sy >= -10 && sy <= height + 10) {
                        gc.setFill(dotInside);
                        gc.fillOval(sx - 4, sy - 4, 8, 8);
                        gc.setStroke(kp.color);
                        gc.setLineWidth(2);
                        gc.strokeOval(sx - 4, sy - 4, 8, 8);
                    }
                }
            });

        }).start();
    }

    private int getState(double a,double b,double c,double d){
        int state = 0;
        if(a > 0) state |= 8;
        if(b > 0) state |= 4;
        if(c > 0) state |= 2;
        if(d > 0) state |= 1;
        return state;
    }

    private double interpolate(double val1, double val2){
        double diff = val1-val2;
        if(Math.abs(diff) < 1e-9) return 0.5;
        double t = (0-val1)/diff;
        return Math.max(0.0, Math.min(1.0,t));
    }
}