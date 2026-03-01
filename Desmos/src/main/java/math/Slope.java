package math;

public class Slope {
    GraphFunction f;

    public void setGraphFunction(GraphFunction f){
        this.f = f;
    }

    public boolean isValidPoint(double x){
        double y = f.evaluate(x);
        return !Double.isNaN(y);
    }
    public double findSlope(double x){
        double slope;
        double h = 0.0000001;

        if(isValidPoint(x))
        {
            double y1 = f.evaluate(x);
            double y2 = f.evaluate(x+h);
            slope = (y2-y1)/h;
            return slope;
        }

        return Double.NaN;
    }
}
