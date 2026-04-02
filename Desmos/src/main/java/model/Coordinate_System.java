package model;

public class Coordinate_System {

    private Viewport viewport;
    private double screenWidth;
    private double screenHeight;

    public Coordinate_System(Viewport viewport) {
        this.viewport = viewport;
    }

    public void setScreenSize(double width, double height) {
        this.screenWidth = width;
        this.screenHeight = height;
    }

    public double worldToScreenX(double x) {
        return (x - viewport.getXMin()) / viewport.getWidth() * screenWidth;
    }

    public double worldToScreenY(double y) {
        return screenHeight
                - (y - viewport.getYMin()) / viewport.getHeight() * screenHeight;
    }

    public double screenToWorldX(double x) {
        return viewport.getXMin() + x / screenWidth * viewport.getWidth();
    }

    public double screenToWorldY(double y) {
        return viewport.getYMin()
                + (screenHeight - y) / screenHeight * viewport.getHeight();
    }

    public Viewport getViewport() {
        return viewport;
    }

    public void enforceAspectRatio(double screenW, double screenH) {
        if (screenW <= 0 || screenH <= 0) return;

        double targetRatio = screenW / screenH;
        double currentHeight = viewport.getHeight();

        // Adjust the Math X-bounds so the ratio perfectly matches the screen pixels
        double newWidth = currentHeight * targetRatio;
        double centerX = (viewport.getXMin() + viewport.getXMax()) / 2.0;

        // We need to add setters to Viewport for this!
        viewport.setXMin(centerX - (newWidth / 2.0));
        viewport.setXMax(centerX + (newWidth / 2.0));
    }
}
