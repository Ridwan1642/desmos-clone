package math;

import javafx.scene.paint.Color;
import org.mariuszgromada.math.mxparser.*;

import java.util.ArrayList;
import java.util.List;

public class GraphFunction {

    private final Expression expression;
    private final Argument xArg;
    private final Argument yArg;
    private Color color;
    private boolean isVisible = true;
    private boolean isImplicit = false;
    private List<Argument> parameters = new ArrayList<>();

    public GraphFunction(String formula, Color color) {

        // 1. Bulletproof Argument initialization
        xArg = new Argument("x", 0);
        yArg = new Argument("y", 0);

        String cleanFormula = preprocessFormula(formula);

        isImplicit = cleanFormula.contains("y") || cleanFormula.contains("=");

        String evalFormula = cleanFormula;
        if (cleanFormula.contains("=")) {
            String[] parts = cleanFormula.split("=");
            if (parts.length == 2) {
                evalFormula = "(" + parts[0].trim() + ") - (" + parts[1].trim() + ")";
            }
        }

        // Initialize expression
        expression = new Expression(evalFormula, xArg, yArg);
        this.color = color;

        // 2. THE DIAGNOSTIC CHECK
        if (!expression.checkSyntax()) {
            System.err.println("Error for: '" + evalFormula + "': " + expression.getErrorMessage());
        }

        // Handle missing custom parameters (like a, b, c)
        String[] missingArgs = expression.getMissingUserDefinedArguments();
        if (missingArgs != null) {
            for (String argName : missingArgs) {
                if (!argName.equals("x") && !argName.equals("y")) {
                    Argument newArg = new Argument(argName, 1);
                    parameters.add(newArg);
                    expression.addArguments(newArg);
                }
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

    public void setColor(Color color) {
        this.color = color;
    }

    public boolean isVisible() {
        return isVisible;
    }

    public boolean isImplicit(){
        return isImplicit;
    }

    public void setVisible(boolean v) {
        isVisible = v;
    }

    public double evaluate(double x) {
        xArg.setArgumentValue(x);
        double result = expression.calculate();
        return result;
    }

    public double evaluate(double x,double y){
        xArg.setArgumentValue(x);
        yArg.setArgumentValue(y);
        return expression.calculate();
    }
}