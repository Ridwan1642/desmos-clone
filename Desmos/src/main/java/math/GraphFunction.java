package math;

import javafx.scene.paint.Color;
import org.mariuszgromada.math.mxparser.*;

import java.util.ArrayList;
import java.util.List;

public class GraphFunction {

    public enum Type { CARTESIAN, IMPLICIT, PARAMETRIC, POLAR }

    private static final String NORMAL_CHARS = "0123456789xyn+-().t";
    private static final String SUPER_CHARS  = "⁰¹²³⁴⁵⁶⁷⁸⁹ˣʸⁿ⁺⁻⁽⁾·ᵗ";

    private Expression expression;
    private Expression exprY;

    private final Argument xArg = new Argument("x", 0);
    private final Argument yArg = new Argument("y", 0);
    private final Argument tArg = new Argument("t", 0);
    private final Argument thetaArg = new Argument("theta", 0);

    private Color color;
    private boolean isVisible = true;
    private boolean isImplicit = false;
    private Type type = Type.CARTESIAN;
    private List<Argument> parameters = new ArrayList<>();
    private double tMin =0;
    private double tMax = 1;

    public GraphFunction(String formula, Color color) {
        this.color = color;
        String cleanFormula = preprocessFormula(formula.trim());

        if (cleanFormula.startsWith("r=")) {
            type = Type.POLAR;
            isImplicit = false;
            tMin = 0;
            tMax = 12*Math.PI;
            String rightSide = cleanFormula.substring(cleanFormula.indexOf("=") + 1).trim();
            expression = new Expression(rightSide, tArg, thetaArg);
        }
        else if (cleanFormula.startsWith("(") && cleanFormula.endsWith(")") && cleanFormula.contains(",")) {
            type = Type.PARAMETRIC;
            isImplicit = false;
            tMin = 0;
            tMax = 1;
            String inside = cleanFormula.substring(1, cleanFormula.length() - 1);
            String[] parts = inside.split(",", 2);
            expression = new Expression(parts[0].trim(), tArg, thetaArg);
            exprY = new Expression(parts[1].trim(), tArg, thetaArg);
        }
        else if (cleanFormula.contains("y") || cleanFormula.contains("=")) {
            type = Type.IMPLICIT;
            isImplicit = true;
            String evalFormula = cleanFormula;
            if (cleanFormula.contains("=")) {
                String[] parts = cleanFormula.split("=");
                if (parts.length == 2) {
                    evalFormula = "(" + parts[0].trim() + ") - (" + parts[1].trim() + ")";
                }
            }
            expression = new Expression(evalFormula, xArg, yArg);
        }
        else {
            type = Type.CARTESIAN;
            isImplicit = false;
            expression = new Expression(cleanFormula, xArg);
        }

        if (type == Type.PARAMETRIC) {
            extractParameters(expression);
            extractParameters(exprY);

        } else {
            extractParameters(expression);
        }
    }

    private void extractParameters(Expression expr) {
        String[] missingArgs = expr.getMissingUserDefinedArguments();

        if (missingArgs != null) {
            for (String argName : missingArgs) {
                if (!argName.matches("x|y|t|theta")) {
                    boolean exists = parameters.stream().anyMatch(a -> a.getArgumentName().equals(argName));
                    if (!exists) {
                        Argument newArg = new Argument(argName, 1);
                        parameters.add(newArg);
                        expr.addArguments(newArg);
                    }
                }
            }
        }

        if (!expr.checkSyntax()) {
            System.err.println("Syntax Error: " + expr.getErrorMessage());
        }
    }

    private String preprocessFormula(String formula) {
        if (formula == null || formula.isEmpty()) return "0";

        String stripped = formula.replace("\u200B", "");
        StringBuilder sb = new StringBuilder();
        boolean inSuperBlock = false;

        for (int i = 0; i < stripped.length(); i++) {
            char c = stripped.charAt(i);
            int superIdx = SUPER_CHARS.indexOf(c);

            if (superIdx != -1) {
                if (!inSuperBlock) { sb.append("^("); inSuperBlock = true; }
                sb.append(NORMAL_CHARS.charAt(superIdx));
            } else {
                if (inSuperBlock) { sb.append(")"); inSuperBlock = false; }
                sb.append(c);
            }
        }
        if (inSuperBlock) sb.append(")");

        String safeMath = sb.toString();
        int openParen = 0, closeParen = 0;

        for (char c : safeMath.toCharArray()) {
            if (c == '(') openParen++; else if (c == ')') closeParen++;
        }

        StringBuilder processed = new StringBuilder(safeMath);
        while (openParen > closeParen) { processed.append(")"); closeParen++; }

        return processed.toString();
    }
    public double getTMin() { return tMin; }
    public void setTMin(double tMin) { this.tMin = tMin; }
    public double getTMax() { return tMax; }
    public void setTMax(double tMax) { this.tMax = tMax; }
    public Type getType() { return type; }
    public boolean isImplicit() { return isImplicit; }
    public Color getColor() { return color; }
    public void setColor(Color color) { this.color = color; }
    public boolean isVisible() { return isVisible; }
    public void setVisible(boolean v) { isVisible = v; }
    public List<Argument> getParameters() { return parameters; }

    public double evaluate(double x) {
        xArg.setArgumentValue(x);
        return expression.calculate();
    }

    public double evaluate(double x, double y) {
        xArg.setArgumentValue(x);
        yArg.setArgumentValue(y);
        return expression.calculate();
    }

    public double evaluateT(double t) {
        tArg.setArgumentValue(t);
        thetaArg.setArgumentValue(t);
        return expression.calculate();
    }

    public double evaluateParametricY(double t) {
        tArg.setArgumentValue(t);
        thetaArg.setArgumentValue(t);
        return exprY.calculate();
    }

    public double inverseEvaluate(double targetY, double guessX) {
        double x = guessX;
        for (int i = 0; i < 30; i++) {
            double currentY = evaluate(x);
            double diff = currentY - targetY;

            if (Math.abs(diff) < 1e-6) return x;

            double h = 1e-5;
            double derivative = (evaluate(x + h) - evaluate(x - h)) / (2 * h);

            if (Math.abs(derivative) < 1e-12) {
                x += 0.5;
                continue;
            }
            x = x - (diff / derivative);
        }

        if (Math.abs(evaluate(x) - targetY) > 1e-2) return Double.NaN;
        return x;
    }
}