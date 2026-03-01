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

public class SlopeCalculator extends VBox {
    private Menu menu;
    private Label error;

    public SlopeCalculator(Menu menu) {
        this.menu = menu;
        this.error = new Label();

        // FIX 1: Lock in the label height so the VBox NEVER squishes it!
        this.error.setMinHeight(25);
        this.error.setPrefHeight(25);
        this.error.setAlignment(Pos.BOTTOM_CENTER);

        this.setStyle("-fx-background-color: white; -fx-border-color: #ccc; -fx-border-radius: 5; -fx-padding: 10;");
        this.setSpacing(10);
        showIdleState();
    }

    private void showIdleState() {
        this.getChildren().clear();
        Button slope = new Button("dy/dx");
        slope.setStyle("-fx-cursor: hand; -fx-font-size: 20px; -fx-padding: 5 10 5 10;");

        slope.setOnAction(e -> {
            setFunctionState();
        });
        this.getChildren().add(slope);
    }

    // Error Message label
    public void seterrLabel(String message) {
        // This prints to your IDE console so you know the logic is working!

        error.setText(message);
        error.setStyle("-fx-text-fill: red; -fx-font-size: 14px;");
    }

    public void seterrLabel() {
        seterrLabel(" "); // FIX 2: Use a space, not an empty string
    }

    private void setFunctionState() {
        this.getChildren().clear();
        seterrLabel(); // Clear any old errors

        // 1. TOP ROW: Label and Cross Button
        Label newLabel = new Label("Enter function number:");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button cross = new Button("X");
        cross.setStyle("-fx-text-fill: red; -fx-font-weight: bold; -fx-background-color: transparent; -fx-cursor: hand;");
        cross.setOnAction(e -> showIdleState());

        HBox topRow = new HBox(newLabel, spacer, cross);
        topRow.setAlignment(Pos.CENTER_LEFT);

        // 2. INPUT ROW: Text Field and Next Button
        TextField input = new TextField();
        input.setPrefWidth(60);
        input.setStyle("-fx-font-size: 18px;");

        Button nextBtn = new Button("Next");
        HBox inputRow = new HBox(10, input, nextBtn);
        inputRow.setAlignment(Pos.CENTER_LEFT);

        // 3. BACK TO BASICS: Simple Try-Catch Logic
        javafx.event.EventHandler<javafx.event.ActionEvent> processInput = e -> {
            try {
                // Parse the number and convert to a computer index (0-based)
                int funcNo = Integer.parseInt(input.getText().trim()) - 1;

                // Check if the box actually exists in the menu
                if (funcNo >= 0 && funcNo < menu.getTextFields().size()) {

                    Object data = menu.getTextFields().get(funcNo).getUserData();

                    // Check if a math function is saved inside it
                    if (data instanceof math.GraphFunction) {
                        showCalculationState(funcNo); // Success!
                    } else {
                        seterrLabel("Function " + (funcNo + 1) + " is empty.");
                    }

                } else {
                    seterrLabel("Function " + (funcNo + 1) + " doesn't exist.");
                }

            } catch (NumberFormatException ex) {
                // Catches if they type letters or symbols
                seterrLabel("Please enter a valid number.");
            }
        };

        // Attach the logic to both the Enter key and the Next button
        input.setOnAction(processInput);
        nextBtn.setOnAction(processInput);

        // 4. ADD EVERYTHING TO SCREEN
        this.getChildren().addAll(topRow, inputRow, error);
    }

    private void showCalculationState(int idx) {
        this.getChildren().clear();
        seterrLabel(); // Clear any previous errors

        // 1. TOP ROW: Instructions and Back Button
        Label instruction = new Label("Enter x value:");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button backBtn = new Button("Back");
        backBtn.setStyle("-fx-cursor: hand;");
        backBtn.setOnAction(e -> setFunctionState()); // Takes the user back to function selection

        Button cross = new Button("X");
        cross.setStyle("-fx-text-fill: red; -fx-font-weight: bold; -fx-background-color: transparent; -fx-cursor: hand;");
        cross.setOnAction(e -> showIdleState());

        HBox topRow = new HBox(instruction, spacer, backBtn,cross);
        topRow.setAlignment(Pos.CENTER_LEFT);

        // 2. INPUT ROW: TextField for x, and a Calculate button
        TextField xInput = new TextField();
        xInput.setPrefWidth(60);
        xInput.setMaxWidth(60);
        xInput.setStyle("-fx-font-size: 18px;");

        Button calcBtn = new Button("Calculate");
        HBox inputRow = new HBox(10, xInput, calcBtn);
        inputRow.setAlignment(Pos.CENTER_LEFT);

        // 3. RESULT DISPLAY: Where the final slope will be shown
        Label resultLabel = new Label("Slope: ");
        resultLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #2196F3;");

        // 4. THE MATH LOGIC
        javafx.event.EventHandler<javafx.event.ActionEvent> calculateLogic = e -> {
            try {
                // Parse the user's x input
                double x = Double.parseDouble(xInput.getText().trim());

                // Grab the exact GraphFunction they selected earlier
                GraphFunction function = (GraphFunction) menu.getTextFields().get(idx).getUserData();

                // USE YOUR NEW SLOPE CLASS!
                math.Slope slopeCalculator = new math.Slope();
                slopeCalculator.setGraphFunction(function);

                // Check validity using your custom method
                if (!slopeCalculator.isValidPoint(x)) {
                    resultLabel.setText("Slope: Undefined");
                    seterrLabel("Point is outside the domain.");
                } else {
                    // Find the slope using your custom method
                    double calculatedSlope = slopeCalculator.findSlope(x);

                    if (Double.isNaN(calculatedSlope)) {
                        resultLabel.setText("Slope: Undefined");
                    } else if (calculatedSlope >= 10000) {
                        resultLabel.setText("Slope: Infinity (Vertical)");
                        seterrLabel();
                    } else {
                        resultLabel.setText(String.format("Slope: %f", calculatedSlope));
                        seterrLabel(); // Clear any existing errors on success
                    }
                }

            } catch (NumberFormatException ex) {
                seterrLabel("Please enter a valid number for x.");
            }
        };

        // Attach logic to both the Enter key and the Calculate button
        xInput.setOnAction(calculateLogic);
        calcBtn.setOnAction(calculateLogic);

        // 5. ADD EVERYTHING TO THE SCREEN
        this.getChildren().addAll(topRow, inputRow, resultLabel, error);
    }
}