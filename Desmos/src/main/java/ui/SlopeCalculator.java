package ui;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.control.Label;
import math.GraphFunction;
import javafx.scene.layout.Region;
import javafx.scene.control.CheckBox;
import javafx.event.ActionEvent;

public class SlopeCalculator extends VBox {
    private Menu menu;
    private GraphCanvas canvas;
    private Label error;
    private boolean isDarkMode = false;

    public SlopeCalculator(Menu menu, GraphCanvas canvas) {
        this.menu = menu;
        this.canvas = canvas;
        this.error = new Label();
        this.error.setMinHeight(30);
        this.error.setPrefHeight(30);
        this.error.setAlignment(Pos.BOTTOM_CENTER);

        this.setSpacing(15);
        this.setPrefWidth(350);
        this.setPrefHeight(250);

        setDarkMode(false);
        setFunctionState();
    }

    public void setDarkMode(boolean dark) {
        this.isDarkMode = dark;
        this.setStyle(dark ? "-fx-background-color: #404040; -fx-padding: 25;" : "-fx-background-color: white; -fx-padding: 25;");
        applyTheme(this);
    }

    private void applyTheme(javafx.scene.Parent parent) {
        String textFill = isDarkMode ? "white" : "black";
        String fieldBg = isDarkMode ? "black" : "white";
        String border = isDarkMode ? "-fx-border-color: #555; -fx-border-radius: 3;" : "-fx-border-color: #ccc; -fx-border-radius: 3;";

        for (javafx.scene.Node node : parent.getChildrenUnmodifiable()) {
            if (node instanceof Label) {
                Label l = (Label) node;
                if (l == error) {
                    if (l.getText().contains("Snapped") || l.getText().contains("Warning")) {
                        l.setStyle("-fx-text-fill: " + (isDarkMode ? "#FFB300" : "#F57C00") + "; -fx-font-size: 14px;");
                    } else {
                        l.setStyle("-fx-text-fill: " + (isDarkMode ? "#ff6b6b" : "red") + "; -fx-font-size: 15px;");
                    }
                } else if (l.getText().startsWith("Slope:")) {
                    l.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: " + (isDarkMode ? "#00d2ff" : "#2196F3") + ";");
                } else {
                    l.setStyle("-fx-font-weight: bold; -fx-font-size: 16px; -fx-text-fill: " + textFill + ";");
                }
            } else if (node instanceof TextField) {
                ((TextField) node).setStyle("-fx-font-size: 18px; -fx-background-color: " + fieldBg + "; -fx-text-fill: " + textFill + "; " + border);
            } else if (node instanceof CheckBox) {
                ((CheckBox) node).setStyle("-fx-font-size: 15px; -fx-text-fill: " + textFill + ";");
            } else if (node instanceof javafx.scene.Parent) {
                applyTheme((javafx.scene.Parent) node);
            }
        }
    }

    public void seterrLabel(String message) {
        error.setText(message);
        applyTheme(this);
    }

    public void seterrLabel() {
        seterrLabel(" ");
    }

    private void setFunctionState() {
        this.getChildren().clear();
        seterrLabel();

        Label newLabel = new Label("Enter function number:");
        HBox topRow = new HBox(newLabel);
        topRow.setAlignment(Pos.CENTER_LEFT);
        topRow.setPadding(new javafx.geometry.Insets(0, 0, 15, 0));

        TextField input = new TextField();
        input.setPrefWidth(90);

        Button nextBtn = new Button("Next");
        nextBtn.setStyle("-fx-font-size: 16px; -fx-padding: 5 15 5 15;");

        HBox inputRow = new HBox(15, input, nextBtn);
        inputRow.setAlignment(Pos.CENTER_LEFT);

        javafx.event.EventHandler<javafx.event.ActionEvent> processInput = e -> {
            try {
                int funcNo = Integer.parseInt(input.getText().trim()) - 1;

                if (funcNo >= 0 && funcNo < menu.getTextFields().size()) {
                    Object data = menu.getTextFields().get(funcNo).getUserData();
                    if (data instanceof math.GraphFunction) {
                        showCalculationState(funcNo);
                    } else {
                        seterrLabel("Function " + (funcNo + 1) + " is empty.");
                    }
                } else {
                    seterrLabel("Function " + (funcNo + 1) + " doesn't exist.");
                }
            } catch (NumberFormatException ex) {
                seterrLabel("Please enter a valid number.");
            }
        };

        input.setOnAction(processInput);
        nextBtn.setOnAction(processInput);

        this.getChildren().addAll(topRow, inputRow, error);
        applyTheme(this);
    }

    private void showCalculationState(int idx) {
        this.getChildren().clear();
        seterrLabel();

        GraphFunction function = (GraphFunction) menu.getTextFields().get(idx).getUserData();
        boolean isImplicit = function.isImplicit();

        Label instruction = new Label(isImplicit ? "Enter x and y guess:" : "Enter x value:");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button backBtn = new Button("Back");
        backBtn.setStyle("-fx-cursor: hand; -fx-font-size: 14px;");
        backBtn.setOnAction(e -> setFunctionState());

        HBox topRow = new HBox(instruction, spacer, backBtn);
        topRow.setAlignment(Pos.CENTER_LEFT);
        topRow.setPadding(new javafx.geometry.Insets(0, 0, 15, 0));

        TextField xInput = new TextField();
        xInput.setPromptText("x");
        xInput.setPrefWidth(isImplicit ? 60 : 90);

        TextField yInput = new TextField();
        yInput.setPromptText("y");
        yInput.setPrefWidth(60);

        Button calcBtn = new Button("Calculate");
        calcBtn.setStyle("-fx-font-size: 16px; -fx-padding: 5 10 5 10;");

        CheckBox drawTangentBox = new CheckBox("Tangent");

        HBox inputRow = new HBox(10);
        if (isImplicit) {
            inputRow.getChildren().addAll(xInput, yInput, calcBtn, drawTangentBox);
        } else {
            inputRow.getChildren().addAll(xInput, calcBtn, drawTangentBox);
        }
        inputRow.setAlignment(Pos.CENTER_LEFT);

        Label resultLabel = new Label("Slope: ");

        javafx.event.EventHandler<javafx.event.ActionEvent> calculateLogic = e -> {
            try {
                double x = Double.parseDouble(xInput.getText().trim());
                double y = 0;

                if (isImplicit) {
                    y = Double.parseDouble(yInput.getText().trim());
                }

                math.Slope slopeCalculator = new math.Slope();
                slopeCalculator.setGraphFunction(function);

                if (isImplicit) {
                    double[] snappedCoords = slopeCalculator.snapToCurve(x, y);

                    if (snappedCoords == null) {
                        resultLabel.setText("Slope: Undefined");
                        seterrLabel("Point too far from curve to snap.");
                        return;
                    }

                    x = snappedCoords[0];
                    y = snappedCoords[1];

                    java.text.DecimalFormat dfCoord = new java.text.DecimalFormat("0.####");
                    yInput.setText(dfCoord.format(y));

                    seterrLabel("Snapped Y to nearest valid point.");
                } else {
                    if (!slopeCalculator.isValidPoint(x)) {
                        resultLabel.setText("Slope: Undefined");
                        seterrLabel("Point is outside the domain.");
                        return;
                    } else {
                        seterrLabel();
                    }
                }

                double calculatedSlope = isImplicit ? slopeCalculator.findSlope(x, y) : slopeCalculator.findSlope(x);

                if (Double.isNaN(calculatedSlope)) {
                    resultLabel.setText("Slope: Undefined (Vertical)");
                } else if (Math.abs(calculatedSlope) >= 10000) {
                    resultLabel.setText("Slope: Infinity (Vertical)");
                } else {
                    java.text.DecimalFormat df = new java.text.DecimalFormat("0.####");
                    resultLabel.setText("Slope: " + df.format(calculatedSlope));

                    if (drawTangentBox.isSelected()) {
                        double y0 = isImplicit ? y : function.evaluate(x);
                        double b = y0 - (calculatedSlope * x);
                        String equation;

                        if (Math.abs(calculatedSlope) < 1e-7) {
                            equation = String.format(java.util.Locale.US, "%.4f", y0);
                        } else {
                            String bStr = "";
                            if (Math.abs(b) > 1e-7) {
                                String sign = (b < 0) ? "-" : "+";
                                bStr = String.format(java.util.Locale.US, " %s %.4f", sign, Math.abs(b));
                            }
                            equation = String.format(java.util.Locale.US, "%.4f*x%s", calculatedSlope, bStr);
                        }

                        // --- NEW: Inject the point of contact to the canvas ---
                        canvas.addTangentPoint(x, y0, function.getColor());

                        TextField newRow = menu.setTextFieldAvoiding(function.getColor());
                        newRow.setText(equation);
                        newRow.fireEvent(new ActionEvent(ActionEvent.ACTION, newRow));
                        drawTangentBox.setSelected(false);
                    }
                }
            } catch (NumberFormatException ex) {
                seterrLabel("Please enter valid numbers.");
            }
        };

        xInput.setOnAction(calculateLogic);
        if (isImplicit) yInput.setOnAction(calculateLogic);
        calcBtn.setOnAction(calculateLogic);

        VBox.setMargin(resultLabel, new javafx.geometry.Insets(10, 0, 0, 0));
        this.getChildren().addAll(topRow, inputRow, resultLabel, error);
        applyTheme(this);
    }
}