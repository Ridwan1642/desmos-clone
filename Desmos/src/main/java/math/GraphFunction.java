package math;

import javafx.scene.paint.Color;
import org.mariuszgromada.math.mxparser.*;

public class GraphFunction {

    private final Expression expression;
    private final Argument xArg;
    private Color color;
    private boolean isVisible = true;

    public GraphFunction(String formula, Color color) {
        // Create a variable x
        xArg = new Argument("x = 0");
        // Parse the expression with mXparser
        expression = new Expression(formula, xArg);
        this.color = color;
    }

    public Color getColor(){
        return color;
    }

    public boolean isVisible(){
        return isVisible;
    }

    public void setVisible(boolean v){
        isVisible = v;
    }

    public double evaluate(double x) {
        xArg.setArgumentValue(x);
        return expression.calculate();
    }
}

