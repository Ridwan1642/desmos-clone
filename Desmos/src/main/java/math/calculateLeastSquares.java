package math;

import java.util.List;
public class calculateLeastSquares {
    public static String calculateBestFitLine(List<Double> xValues, List<Double> yValues) {
        if (xValues == null || yValues == null || xValues.size() != yValues.size() || xValues.size() < 2) {
            return null;
        }

        int n = xValues.size();
        double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0;

        for (int i = 0; i < n; i++) {
            double x = xValues.get(i);
            double y = yValues.get(i);

            sumX += x;
            sumY += y;
            sumXY += (x * y);
            sumX2 += (x * x);
        }

        double denominator = (n * sumX2) - (sumX * sumX);
        if (denominator == 0) return null;

        double m = ((n * sumXY) - (sumX * sumY)) / denominator;
        double b = (sumY - m * sumX) / n;

        String operator = (b < 0) ? " - " : " + ";
        return String.format(java.util.Locale.US, "%.4f*x%s%.4f", m, operator, Math.abs(b));
    }
}
