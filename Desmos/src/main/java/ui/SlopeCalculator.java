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
    private Label error;

    public SlopeCalculator(Menu menu) {
        this.menu = menu;
        this.error = new Label();
        this.error.setMinHeight(30); // Increased height
        this.error.setPrefHeight(30);
        this.error.setAlignment(Pos.BOTTOM_CENTER);

        // Increased overall padding and explicitly set preferred width/height
        this.setStyle("-fx-background-color: white; -fx-padding: 25;");
        this.setSpacing(15); // Increased spacing between elements
        this.setPrefWidth(350);
        this.setPrefHeight(250);

        // Skip idle state, go straight to function input
        setFunctionState();
    }

    public void seterrLabel(String message) {
        error.setText(message);
        // Slightly larger error text
        error.setStyle("-fx-text-fill: red; -fx-font-size: 15px;");
    }

    public void seterrLabel() {
        seterrLabel(" ");
    }

    private void setFunctionState() {
        this.getChildren().clear();
        seterrLabel();

        Label newLabel = new Label("Enter function number:");
        // Increased font size
        newLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 16px;");
        HBox topRow = new HBox(newLabel);
        topRow.setAlignment(Pos.CENTER_LEFT);
        topRow.setPadding(new javafx.geometry.Insets(0, 0, 15, 0));

        TextField input = new TextField();
        // Increased width and font size of the input box
        input.setPrefWidth(90);
        input.setStyle("-fx-font-size: 18px;");

        Button nextBtn = new Button("Next");
        // Scaled up the button
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
    }

    private void showCalculationState(int idx) {
        this.getChildren().clear();
        seterrLabel();

        Label instruction = new Label("Enter x value:");
        // Increased font size
        instruction.setStyle("-fx-font-weight: bold; -fx-font-size: 16px;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button backBtn = new Button("Back");
        // Scaled up back button
        backBtn.setStyle("-fx-cursor: hand; -fx-font-size: 14px;");
        backBtn.setOnAction(e -> setFunctionState());

        HBox topRow = new HBox(instruction, spacer, backBtn);
        topRow.setAlignment(Pos.CENTER_LEFT);
        topRow.setPadding(new javafx.geometry.Insets(0, 0, 15, 0));

        TextField xInput = new TextField();
        // Increased width and font size
        xInput.setPrefWidth(90);
        xInput.setStyle("-fx-font-size: 18px;");

        Button calcBtn = new Button("Calculate");
        calcBtn.setStyle("-fx-font-size: 16px; -fx-padding: 5 10 5 10;");

        CheckBox drawTangentBox = new CheckBox("Draw Tangent");
        drawTangentBox.setStyle("-fx-font-size: 15px;");

        HBox inputRow = new HBox(15, xInput, calcBtn, drawTangentBox);
        inputRow.setAlignment(Pos.CENTER_LEFT);

        Label resultLabel = new Label("Slope: ");
        // Made the result text significantly larger and more prominent
        resultLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #2196F3;");

        javafx.event.EventHandler<javafx.event.ActionEvent> calculateLogic = e -> {
            try {
                double x = Double.parseDouble(xInput.getText().trim());
                GraphFunction function = (GraphFunction) menu.getTextFields().get(idx).getUserData();

                math.Slope slopeCalculator = new math.Slope();
                slopeCalculator.setGraphFunction(function);

                if (!slopeCalculator.isValidPoint(x)) {
                    resultLabel.setText("Slope: Undefined");
                    seterrLabel("Point is outside the domain.");
                } else {
                    double calculatedSlope = slopeCalculator.findSlope(x);

                    if (Double.isNaN(calculatedSlope)) {
                        resultLabel.setText("Slope: Undefined");
                    } else if (calculatedSlope >= 10000) {
                        resultLabel.setText("Slope: Infinity (Vertical)");
                        seterrLabel();
                    } else {
                        java.text.DecimalFormat df = new java.text.DecimalFormat("0.####");
                        resultLabel.setText("Slope: " + df.format(calculatedSlope));
                        seterrLabel();

                        if (drawTangentBox.isSelected()) {
                            double y = function.evaluate(x);
                            double b = y - (calculatedSlope * x);
                            String equation;

                            if (Math.abs(calculatedSlope) < 1e-7) {
                                equation = String.format(java.util.Locale.US, "%.4f", y);
                            } else {
                                String bStr = "";
                                if (Math.abs(b) > 1e-7) {
                                    String sign = (b < 0) ? "-" : "+";
                                    bStr = String.format(java.util.Locale.US, " %s %.4f", sign, Math.abs(b));
                                }
                                equation = String.format(java.util.Locale.US, "%.4f*x%s", calculatedSlope, bStr);
                            }

                            TextField newRow = menu.setTextFieldAvoiding(function.getColor());
                            newRow.setText(equation);
                            newRow.fireEvent(new ActionEvent(ActionEvent.ACTION, newRow));
                            drawTangentBox.setSelected(false);
                        }
                    }
                }
            } catch (NumberFormatException ex) {
                seterrLabel("Please enter a valid number for x.");
            }
        };

        xInput.setOnAction(calculateLogic);
        calcBtn.setOnAction(calculateLogic);

        VBox.setMargin(resultLabel, new javafx.geometry.Insets(10, 0, 0, 0));
        this.getChildren().addAll(topRow, inputRow, resultLabel, error);
    }
}