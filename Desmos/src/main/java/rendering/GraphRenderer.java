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

    public static class TangentLine {
        public double px, py, m;
        public Color color;
        public TangentLine(double px, double py, double m, Color color) {
            this.px = px; this.py = py; this.m = m; this.color = color;
        }
    }

    private final Coordinate_System coordSystem;
    private List<GraphFunction> functions = new ArrayList<>();
    private List<ShadedRegion> shadedRegions = new ArrayList<>();
    private List<KeyPoint> dynamicIntercepts = new ArrayList<>();
    private List<KeyPoint> tangentPoints = new ArrayList<>();
    private List<TangentLine> tangentLines = new ArrayList<>();

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

    public void addTangentLine(double x, double y, double m, Color c) {
        tangentLines.add(new TangentLine(x, y, m, c));
    }

    public void clearTangentLines() {
        tangentLines.clear();
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
        while (functions.size() <= index) {
            functions.add(null);
        }
        functions.set(index, f);
    }

    public void removeFunction(GraphFunction f) {
        if (f != null) {
            functions.remove(f);
        }
        clearTangentPoints();
        clearTangentLines();
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

            // --- ADVANCED SHADING & INTERACTIVE BOUNDS ---
            for (ShadedRegion region : renderRegions) {
                double min = Math.min(region.a, region.b);
                double max = Math.max(region.a, region.b);
                int numSteps = 500;
                double step = (max - min) / numSteps;

                double[] xPoints = new double[numSteps * 2 + 2];
                double[] yPoints = new double[numSteps * 2 + 2];
                int index = 0;

                // 1. Forward Path (Primary Function)
                for (int i = 0; i <= numSteps; i++) {
                    double val = min + (i * step);
                    if (region.f1.getType() == GraphFunction.Type.PARAMETRIC) {
                        xPoints[index] = coordSystem.worldToScreenX(region.f1.evaluateT(val));
                        yPoints[index] = coordSystem.worldToScreenY(region.f1.evaluateParametricY(val));
                    } else if (region.f1.getType() == GraphFunction.Type.POLAR) {
                        double r = region.f1.evaluateT(val);
                        xPoints[index] = coordSystem.worldToScreenX(r * Math.cos(val));
                        yPoints[index] = coordSystem.worldToScreenY(r * Math.sin(val));
                    } else if (!region.isRespectToY) {
                        xPoints[index] = coordSystem.worldToScreenX(val);
                        yPoints[index] = coordSystem.worldToScreenY(region.f1.evaluate(val));
                    } else {
                        yPoints[index] = coordSystem.worldToScreenY(val);
                        xPoints[index] = coordSystem.worldToScreenX(region.f1.evaluate(val));
                    }
                    index++;
                }

                // 2. Return Path (Secondary Function, Origin, or Axis)
                for (int i = numSteps; i >= 0; i--) {
                    double val = min + (i * step);
                    if (region.f1.getType() == GraphFunction.Type.PARAMETRIC) {
                        xPoints[index] = coordSystem.worldToScreenX(region.f1.evaluateT(val));
                        yPoints[index] = coordSystem.worldToScreenY(0);
                    } else if (region.f1.getType() == GraphFunction.Type.POLAR) {
                        xPoints[index] = coordSystem.worldToScreenX(0);
                        yPoints[index] = coordSystem.worldToScreenY(0);
                    } else if (!region.isRespectToY) {
                        xPoints[index] = coordSystem.worldToScreenX(val);
                        double yVal = (region.f2 != null) ? region.f2.evaluate(val) : 0;
                        yPoints[index] = coordSystem.worldToScreenY(yVal);
                    } else {
                        yPoints[index] = coordSystem.worldToScreenY(val);
                        double xVal = (region.f2 != null) ? region.f2.evaluate(val) : 0;
                        xPoints[index] = coordSystem.worldToScreenX(xVal);
                    }
                    index++;
                }

                int finalIndex = index;
                drawCalls.add(() -> {
                    // Fill Shading
                    gc.setFill(region.color.deriveColor(1, 1, 1, 0.3));
                    gc.fillPolygon(xPoints, yPoints, finalIndex);

                    // Draw Dotted Bounds & Hollow Intersections (Cartesian Only)
                    if (region.f1.getType() != GraphFunction.Type.PARAMETRIC && region.f1.getType() != GraphFunction.Type.POLAR) {
                        gc.setStroke(region.color);
                        gc.setLineWidth(1.5);
                        gc.setLineDashes(5, 5);

                        if (!region.isRespectToY) {
                            double sxA = coordSystem.worldToScreenX(region.a);
                            double sxB = coordSystem.worldToScreenX(region.b);
                            gc.strokeLine(sxA, 0, sxA, height);
                            gc.strokeLine(sxB, 0, sxB, height);

                            drawHollowDot(gc, sxA, coordSystem.worldToScreenY(region.f1.evaluate(region.a)), region.color, isDarkMode);
                            drawHollowDot(gc, sxB, coordSystem.worldToScreenY(region.f1.evaluate(region.b)), region.color, isDarkMode);
                            if (region.f2 != null) {
                                drawHollowDot(gc, sxA, coordSystem.worldToScreenY(region.f2.evaluate(region.a)), region.color, isDarkMode);
                                drawHollowDot(gc, sxB, coordSystem.worldToScreenY(region.f2.evaluate(region.b)), region.color, isDarkMode);
                            } else {
                                drawHollowDot(gc, sxA, coordSystem.worldToScreenY(0), region.color, isDarkMode);
                                drawHollowDot(gc, sxB, coordSystem.worldToScreenY(0), region.color, isDarkMode);
                            }
                        } else {
                            double syA = coordSystem.worldToScreenY(region.a);
                            double syB = coordSystem.worldToScreenY(region.b);
                            gc.strokeLine(0, syA, width, syA);
                            gc.strokeLine(0, syB, width, syB);

                            drawHollowDot(gc, coordSystem.worldToScreenX(region.f1.evaluate(region.a)), syA, region.color, isDarkMode);
                            drawHollowDot(gc, coordSystem.worldToScreenX(region.f1.evaluate(region.b)), syB, region.color, isDarkMode);
                            if (region.f2 != null) {
                                drawHollowDot(gc, coordSystem.worldToScreenX(region.f2.evaluate(region.a)), syA, region.color, isDarkMode);
                                drawHollowDot(gc, coordSystem.worldToScreenX(region.f2.evaluate(region.b)), syB, region.color, isDarkMode);
                            } else {
                                drawHollowDot(gc, coordSystem.worldToScreenX(0), syA, region.color, isDarkMode);
                                drawHollowDot(gc, coordSystem.worldToScreenX(0), syB, region.color, isDarkMode);
                            }
                        }
                        gc.setLineDashes(null);
                    }
                });
            }

            for (GraphFunction function : renderFunctions) {
                if (function == null || !function.isVisible()) continue;
                Color funcColor = function.getColor();

                if (function.getType() == GraphFunction.Type.IMPLICIT) {
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

                } else if (function.getType() == GraphFunction.Type.PARAMETRIC || function.getType() == GraphFunction.Type.POLAR) {
                    drawCalls.add(() -> {
                        gc.save();
                        gc.setStroke(funcColor);
                        gc.setLineWidth(lineWidth);
                        gc.setLineCap(StrokeLineCap.ROUND);
                        gc.setLineJoin(StrokeLineJoin.ROUND);
                        gc.beginPath();

                        double tMin = 0;
                        double tMax = 12 * Math.PI;
                        int steps = 2000;
                        double dt = (tMax - tMin) / steps;
                        boolean isFirstPoint = true;

                        for (int i = 0; i <= steps; i++) {
                            if (renderVersion.get() != currentVersion) return;

                            double t = tMin + (i * dt);
                            double x, y;

                            if (function.getType() == GraphFunction.Type.PARAMETRIC) {
                                x = function.evaluateT(t);
                                y = function.evaluateParametricY(t);
                            } else {
                                double r = function.evaluateT(t);
                                x = r * Math.cos(t);
                                y = r * Math.sin(t);
                            }

                            if (Double.isNaN(x) || Double.isNaN(y)) {
                                isFirstPoint = true;
                                continue;
                            }

                            double sx = coordSystem.worldToScreenX(x);
                            double sy = coordSystem.worldToScreenY(y);

                            if (isFirstPoint) {
                                gc.moveTo(sx, sy);
                                isFirstPoint = false;
                            } else {
                                gc.lineTo(sx, sy);
                            }
                        }
                        gc.stroke();
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

                // Draw Tangent Lines
                for (TangentLine line : tangentLines) {
                    gc.setStroke(line.color);
                    gc.setLineWidth(1.5);
                    gc.setLineDashes(8, 4);

                    double x1 = coordSystem.getViewport().getXMin();
                    double y1 = line.m * (x1 - line.px) + line.py;

                    double x2 = coordSystem.getViewport().getXMax();
                    double y2 = line.m * (x2 - line.px) + line.py;

                    gc.strokeLine(
                            coordSystem.worldToScreenX(x1), coordSystem.worldToScreenY(y1),
                            coordSystem.worldToScreenX(x2), coordSystem.worldToScreenY(y2)
                    );
                    gc.setLineDashes(null);
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

    private void drawHollowDot(GraphicsContext gc, double x, double y, Color color, boolean isDarkMode) {
        gc.setFill(isDarkMode ? Color.web("#1e1e1e") : Color.WHITE);
        gc.fillOval(x - 5, y - 5, 10, 10);
        gc.setStroke(color);
        gc.setLineWidth(2);
        gc.setLineDashes(null);
        gc.strokeOval(x - 5, y - 5, 10, 10);
        gc.setLineDashes(5, 5);
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