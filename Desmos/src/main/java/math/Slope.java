package math;

public class Slope {
    GraphFunction f;

    public void setGraphFunction(GraphFunction f) {
        this.f = f;
    }

    // --- VALIDITY CHECKS ---

    public boolean isValidPoint(double x) {
        if (f.isImplicit() || f.getType() == GraphFunction.Type.PARAMETRIC || f.getType() == GraphFunction.Type.POLAR) return true;
        double y = f.evaluate(x);
        return !Double.isNaN(y);
    }

    public boolean isValidPoint(double x, double y) {
        if (!f.isImplicit()) return isValidPoint(x);
        double val = f.evaluate(x, y);
        return !Double.isNaN(val);
    }

    public boolean isValidT(double t) {
        if (f.getType() == GraphFunction.Type.PARAMETRIC) {
            return !Double.isNaN(f.evaluateT(t)) && !Double.isNaN(f.evaluateParametricY(t));
        } else if (f.getType() == GraphFunction.Type.POLAR) {
            return !Double.isNaN(f.evaluateT(t));
        }
        return false;
    }

    // --- COORDINATE HELPER FOR UI ---

    // Used to find exactly where to draw the tangent point on the canvas
    public double[] getCoordinatesAtT(double t) {
        if (f.getType() == GraphFunction.Type.PARAMETRIC) {
            return new double[]{f.evaluateT(t), f.evaluateParametricY(t)};
        } else if (f.getType() == GraphFunction.Type.POLAR) {
            double r = f.evaluateT(t);
            return new double[]{r * Math.cos(t), r * Math.sin(t)};
        }
        return null;
    }

    // --- SNAPPING ALGORITHM ---

    public double[] snapToCurve(double x, double yGuess) {
        // Snapping from a pure (x,y) mouse coordinate back to a 't' parameter algebraically
        // is highly complex. For parametric/polar, we return null to bypass implicit snapping.
        if (f.getType() == GraphFunction.Type.PARAMETRIC || f.getType() == GraphFunction.Type.POLAR) {
            return null;
        }

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

    // --- SLOPE: CARTESIAN (y = f(x)) ---

    public double findSlope(double x) {
        double h = 0.0000001;

        if (isValidPoint(x - h) && isValidPoint(x + h)) {
            double y1 = f.evaluate(x - h);
            double y2 = f.evaluate(x + h);
            return (y2 - y1) / (2 * h);
        }

        return Double.NaN;
    }

    // --- SLOPE: IMPLICIT (F(x,y) = 0) ---

    public double findSlope(double x, double y) {
        if (!f.isImplicit()) {
            return findSlope(x);
        }

        double h = 0.0000001;
        double dFdx = (f.evaluate(x + h, y) - f.evaluate(x - h, y)) / (2 * h);
        double dFdy = (f.evaluate(x, y + h) - f.evaluate(x, y - h)) / (2 * h);

        if (Math.abs(dFdy) < 1e-10) {
            return Double.NaN; // Vertical tangent
        }

        return -dFdx / dFdy;
    }

    // --- SLOPE: PARAMETRIC (x(t), y(t)) ---

    public double findParametricSlope(double t) {
        double h = 1e-7;

        // Approximate dx/dt
        double dxdt = (f.evaluateT(t + h) - f.evaluateT(t - h)) / (2 * h);

        // Approximate dy/dt
        double dydt = (f.evaluateParametricY(t + h) - f.evaluateParametricY(t - h)) / (2 * h);

        if (Math.abs(dxdt) < 1e-10) return Double.NaN; // Vertical tangent

        return dydt / dxdt;
    }

    // --- SLOPE: POLAR (r = f(theta)) ---

    public double findPolarSlope(double theta) {
        double h = 1e-7;

        double r = f.evaluateT(theta);
        // Approximate dr/dtheta
        double drdtheta = (f.evaluateT(theta + h) - f.evaluateT(theta - h)) / (2 * h);

        // Chain rule conversions
        double dy = drdtheta * Math.sin(theta) + r * Math.cos(theta);
        double dx = drdtheta * Math.cos(theta) - r * Math.sin(theta);

        if (Math.abs(dx) < 1e-10) return Double.NaN; // Vertical tangent

        return dy / dx;
    }
}