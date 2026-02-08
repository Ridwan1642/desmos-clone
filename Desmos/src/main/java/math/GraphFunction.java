package math;

import org.mariuszgromada.math.mxparser.*;

public class GraphFunction {

    private final Expression expression;
    private final Argument xArg;

    public GraphFunction(String formula) {
        // Create a variable x
        xArg = new Argument("x = 0");
        // Parse the expression with mXparser
        expression = new Expression(formula, xArg);
    }

    public double evaluate(double x) {
        xArg.setArgumentValue(x);
        return expression.calculate();
    }
}

