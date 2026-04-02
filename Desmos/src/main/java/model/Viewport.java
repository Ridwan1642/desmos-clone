package model;

public class Viewport {

    private double xMin;
    private double xMax;
    private double yMin;
    private double yMax;

    public Viewport(double xMin, double xMax, double yMin, double yMax) {
        this.xMin = xMin;
        this.xMax = xMax;
        this.yMin = yMin;
        this.yMax = yMax;
    }

    public void setXMin(double x){
        xMin = x;
    }

    public void setXMax(double xmax){
        xMax = xmax;
    }
    public double getXMin() {
        return xMin;
    }

    public double getXMax() {
        return xMax;
    }

    public double getYMin() {
        return yMin;
    }

    public double getYMax() {
        return yMax;
    }


    public double getWidth() {
        return xMax - xMin;
    }

    public double getHeight() {
        return yMax - yMin;
    }

    public void pan(double dx, double dy) {
        xMin += dx;
        xMax += dx;
        yMin += dy;
        yMax += dy;
    }

    public void zoom(double factor, double centerX, double centerY) {
        double oldWidth = getWidth();
        double oldHeight = getHeight();

        double newWidth = oldWidth * factor;
        double newHeight = oldHeight * factor;

        xMin = centerX - (centerX - xMin) * (newWidth / oldWidth);
        xMax = xMin + newWidth;

        yMin = centerY - (centerY - yMin) * (newHeight / oldHeight);
        yMax = yMin + newHeight;
    }

    public void reset() {
        this.xMin = -10;
        this.xMax = 10;
        this.yMin = -10;
        this.yMax = 10;
    }

}






