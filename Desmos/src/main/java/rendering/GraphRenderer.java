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

    private final Coordinate_System coordSystem;
    private List<GraphFunction> functions = new ArrayList<>();
    private List<ShadedRegion> shadedRegions = new ArrayList<>();
    private double lineWidth = 2;
    private int res = 1;
    private final AtomicInteger renderVersion = new AtomicInteger(0);
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
        int currentVersion = renderVersion.incrementAndGet();
        gc.clearRect(0, 0, gc.getCanvas().getWidth(), gc.getCanvas().getHeight());
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

        double width = gc.getCanvas().getWidth();
        double height = gc.getCanvas().getHeight();
        for (GraphFunction function : functions) {
            if(!function.isVisible()) continue;
            gc.setStroke(function.getColor());
            gc.setLineWidth(lineWidth);

            if(function.isImplicit())
                drawImplicit(gc,function,width,height,currentVersion);
            else
                drawExplicit(gc,function,width,height);
                }
            }

            private void drawExplicit(GraphicsContext gc, GraphFunction function, double width, double height){
                double prevX = coordSystem.getViewport().getXMin();
                double prevY = function.evaluate(prevX);

                for (int i = 1; i <= width; i++) {
                    double x = coordSystem.screenToWorldX(i);
                    double y = function.evaluate(x);

                    double sx1 = coordSystem.worldToScreenX(prevX);
                    double sy1 = coordSystem.worldToScreenY(prevY);
                    double sx2 = coordSystem.worldToScreenX(x);
                    double sy2 = coordSystem.worldToScreenY(y);

                    // Prevent drawing massive lines across vertical asymptotes
                    if (!Double.isNaN(y) && !Double.isNaN(prevY) && Math.abs(sy2 - sy1) < height) {
                        gc.strokeLine(sx1, sy1, sx2, sy2);
                    }

                    prevX = x;
                    prevY = y;
                }
        }

    private void drawImplicit(GraphicsContext gc, GraphFunction function, double width, double height, int version) {
        int cols = (int) (width / res) + 1;
        int rows = (int) (height / res) + 1;

        // Capture the unique color of THIS function before the thread starts
        Color funcColor = function.getColor();

        new Thread(() -> {
            double[][] values = new double[cols + 1][rows + 1];
            double epsX = 0.0000123;
            double epsY = 0.0000345;

            // Step 1: Background Math
            for (int i = 0; i <= cols; i++) {
                for (int j = 0; j <= rows; j++) {
                    if (renderVersion.get() != version) return;

                    double worldX = coordSystem.screenToWorldX(i * res) + epsX;
                    double worldY = coordSystem.screenToWorldY(j * res) + epsY;
                    values[i][j] = function.evaluate(worldX, worldY);
                }
            }

            // Step 2: Draw to Screen
            Platform.runLater(() -> {
                if (renderVersion.get() != version) return;

                // --- ISOLATE THE GRAPHICS STATE ---
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

                        // Use strokeLine for instant, un-batchable drawing
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

                // --- RESTORE THE GRAPHICS STATE ---
                gc.restore();
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