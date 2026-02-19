package math;

import javafx.scene.paint.Color;
import org.mariuszgromada.math.mxparser.*;

import java.util.ArrayList;
import java.util.List;

public class GraphFunction {

    private final Expression expression;
    private final Argument xArg;
    private Color color;
    private boolean isVisible = true;
    private List<Argument> parameters = new ArrayList<>();

    public GraphFunction(String formula, Color color) {
        // Create a variable x
        xArg = new Argument("x = 0");
        // Parse the expression with mXparser
        expression = new Expression(formula, xArg);
        this.color = color;

        String[] missingArgs = expression.getMissingUserDefinedArguments();
        if (missingArgs != null) {
            for (String argName : missingArgs) {
                Argument newArg = new Argument(argName + " = 1");
                parameters.add(newArg);
                expression.addArguments(newArg);
            }
        }
    }

    public List<Argument> getParameters(){
        return parameters;
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

