package math;

public class Slope {
    GraphFunction f;

    public void setGraphFunction(GraphFunction f) {
        this.f = f;
    }

    public boolean isValidPoint(double x) {
        double y = f.evaluate(x);
        return !Double.isNaN(y);
    }

    public double findSlope(double x) {
        double slope;
        double h = 0.0000001;

        if (isValidPoint(x - h) && isValidPoint(x + h)) {
            double y1 = f.evaluate(x - h);
            double y2 = f.evaluate(x + h);
            return (y2 - y1) / (2 * h);
        }

        return Double.NaN;
    }
}
