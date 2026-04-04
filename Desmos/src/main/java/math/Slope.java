package math;

public class Slope {
    GraphFunction f;

    public void setGraphFunction(GraphFunction f) {
        this.f = f;
    }

    public boolean isValidPoint(double x) {
        if (f.isImplicit()) return true;
        double y = f.evaluate(x);
        return !Double.isNaN(y);
    }

    public boolean isValidPoint(double x, double y) {
        if (!f.isImplicit()) return isValidPoint(x);
        double val = f.evaluate(x, y);
        return !Double.isNaN(val);
    }

    public double[] snapToCurve(double x, double yGuess) {
        double y = yGuess;
        double h = 1e-6;

        for (int i = 0; i < 20; i++) {
            double val = f.evaluate(x, y);

            if (Math.abs(val) < 1e-7) return new double[]{x, y};

            double dFdy = (f.evaluate(x, y + h) - f.evaluate(x, y - h)) / (2 * h);

            if (Math.abs(dFdy) < 1e-12) break;

            y = y - (val / dFdy);
        }

        if (Math.abs(f.evaluate(x, y)) < 1e-3) return new double[]{x, y};

        return null;
    }

    public double findSlope(double x) {
        double h = 0.0000001;

        if (isValidPoint(x - h) && isValidPoint(x + h)) {
            double y1 = f.evaluate(x - h);
            double y2 = f.evaluate(x + h);
            return (y2 - y1) / (2 * h);
        }

        return Double.NaN;
    }

    public double findSlope(double x, double y) {
        if (!f.isImplicit()) {
            return findSlope(x);
        }

        double h = 0.0000001;

        double dFdx = (f.evaluate(x + h, y) - f.evaluate(x - h, y)) / (2 * h);
        double dFdy = (f.evaluate(x, y + h) - f.evaluate(x, y - h)) / (2 * h);

        if (Math.abs(dFdy) < 1e-10) {
            return Double.NaN;
        }

        return -dFdx / dFdy;
    }
}