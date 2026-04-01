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

        xArg = new Argument("x = 0");


        String cleanFormula = preprocessFormula(formula);


        expression = new Expression(cleanFormula, xArg);
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


    private String preprocessFormula(String formula) {
        if (formula == null || formula.isEmpty()) {
            return "0";
        }


        int openParen = 0;
        int closeParen = 0;
        for (char c : formula.toCharArray()) {
            if (c == '(') openParen++;
            else if (c == ')') closeParen++;
        }

        StringBuilder processed = new StringBuilder(formula);


        while (openParen > closeParen) {
            processed.append(")");
            closeParen++;
        }


        return processed.toString();
    }

    public List<Argument> getParameters() {
        return parameters;
    }

    public Color getColor() {
        return color;
    }

    public boolean isVisible() {
        return isVisible;
    }

    public void setVisible(boolean v) {
        isVisible = v;
    }

    public double evaluate(double x) {
        xArg.setArgumentValue(x);
        double result = expression.calculate();


        return result;
    }
}