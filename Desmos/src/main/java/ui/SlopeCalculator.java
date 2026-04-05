package ui;

import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import math.GraphFunction;
import math.Slope;

import java.util.ArrayList;
import java.util.List;

public class SlopeCalculator extends VBox {
    private final Menu menu;
    private final GraphCanvas canvas;
    private Label error;
    private CheckBox drawLineCheck;
    private CheckBox addToMenuCheck; // --- NEW: The Menu Toggle! ---

    public SlopeCalculator(Menu menu, GraphCanvas canvas) {
        this.menu = menu;
        this.canvas = canvas;

        this.getStyleClass().add("calc-dialog-bg");
        this.setSpacing(20);
        this.setPrefWidth(400);

        showInputState();
    }

    public void refresh() {
        showInputState();
    }

    private void showInputState() {
        this.getChildren().clear();

        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(10);

        Label funcLabel = new Label("Select Function:");
        funcLabel.getStyleClass().add("calc-label");
        ComboBox<String> funcDropdown = new ComboBox<>();
        funcDropdown.setPromptText("Choose a function...");
        funcDropdown.setMaxWidth(Double.MAX_VALUE);
        funcDropdown.getStyleClass().add("combo-box");

        List<Integer> validIndices = new ArrayList<>();
        for (int i = 0; i < menu.getTextFields().size(); i++) {
            TextField tf = menu.getTextFields().get(i);
            if (tf.getUserData() instanceof GraphFunction) {
                String eq = tf.getText().isEmpty() ? "Empty" : tf.getText();
                funcDropdown.getItems().add("Row " + (i + 1) + ": " + eq);
                validIndices.add(i);
            }
        }

        Label inputLabel = new Label("Enter x coordinate:");
        inputLabel.getStyleClass().add("calc-label");
        TextField inputField = new TextField();
        inputField.setPromptText("e.g., 2.5");
        inputField.getStyleClass().add("input-field");

        Label yInputLabel = new Label("Enter y coordinate:");
        yInputLabel.getStyleClass().add("calc-label");
        TextField yInputField = new TextField();
        yInputField.setPromptText("e.g., 3.0");
        yInputField.getStyleClass().add("input-field");

        yInputLabel.setVisible(false);
        yInputLabel.setManaged(false);
        yInputField.setVisible(false);
        yInputField.setManaged(false);

        drawLineCheck = new CheckBox("Draw Tangent Line on Graph");
        drawLineCheck.setSelected(true);
        drawLineCheck.getStyleClass().add("calc-label");

        // --- NEW: Add to Menu CheckBox ---
        addToMenuCheck = new CheckBox("Add Tangent to Function List");
        addToMenuCheck.setSelected(false); // Default to false to prevent accidental menu spam
        addToMenuCheck.getStyleClass().add("calc-label");

        funcDropdown.setOnAction(e -> {
            int selIdx = funcDropdown.getSelectionModel().getSelectedIndex();
            if (selIdx >= 0) {
                GraphFunction f = (GraphFunction) menu.getTextFields().get(validIndices.get(selIdx)).getUserData();

                yInputLabel.setVisible(false);
                yInputLabel.setManaged(false);
                yInputField.setVisible(false);
                yInputField.setManaged(false);

                if (f.getType() == GraphFunction.Type.PARAMETRIC || f.getType() == GraphFunction.Type.POLAR) {
                    inputLabel.setText("Enter parameter (t or θ):");
                }
                else if (f.isImplicit()) {
                    inputLabel.setText("Enter x coordinate:");
                    yInputLabel.setVisible(true);
                    yInputLabel.setManaged(true);
                    yInputField.setVisible(true);
                    yInputField.setManaged(true);
                }
                else {
                    inputLabel.setText("Enter x coordinate:");
                }
            }
        });

        grid.add(funcLabel, 0, 0);
        grid.add(funcDropdown, 0, 1);
        grid.add(inputLabel, 0, 2);
        grid.add(inputField, 0, 3);
        grid.add(yInputLabel, 0, 4);
        grid.add(yInputField, 0, 5);
        grid.add(drawLineCheck, 0, 6);
        grid.add(addToMenuCheck, 0, 7); // --- NEW: Added beneath the draw toggle ---

        ColumnConstraints col1 = new ColumnConstraints();
        col1.setPercentWidth(100);
        grid.getColumnConstraints().add(col1);

        Button calcBtn = new Button("Calculate Tangent");
        calcBtn.getStyleClass().add("primary-btn");
        calcBtn.setMaxWidth(Double.MAX_VALUE);

        Button clearBtn = new Button("Clear Point");
        clearBtn.getStyleClass().add("secondary-btn");
        clearBtn.setMaxWidth(Double.MAX_VALUE);

        GridPane btnGrid = new GridPane();
        btnGrid.setHgap(15);
        btnGrid.add(calcBtn, 0, 0);
        btnGrid.add(clearBtn, 1, 0);
        ColumnConstraints bc1 = new ColumnConstraints(); bc1.setPercentWidth(50);
        ColumnConstraints bc2 = new ColumnConstraints(); bc2.setPercentWidth(50);
        btnGrid.getColumnConstraints().addAll(bc1, bc2);

        VBox resultCard = new VBox();
        resultCard.getStyleClass().add("result-card");

        Label resHeaderLabel = new Label("Tangent Properties:");
        resHeaderLabel.getStyleClass().add("result-header-text");
        HBox resHeader = new HBox(resHeaderLabel);
        resHeader.getStyleClass().add("result-header");

        Label resSlope = new Label("Waiting for input...");
        resSlope.getStyleClass().add("result-value");
        resSlope.setStyle("-fx-font-size: 20px;");

        Label resEquation = new Label("y - y₁ = m(x - x₁)");
        resEquation.getStyleClass().add("result-formula");

        resultCard.getChildren().addAll(resHeader, resSlope, resEquation);

        error = new Label("");
        error.getStyleClass().add("error-label");

        // --- MATH LOGIC ---
        calcBtn.setOnAction(e -> {
            try {
                int selIdx = funcDropdown.getSelectionModel().getSelectedIndex();
                if (selIdx < 0) {
                    error.setText("Please select a function first.");
                    return;
                }

                double inputVal = Double.parseDouble(inputField.getText().trim());
                GraphFunction f = (GraphFunction) menu.getTextFields().get(validIndices.get(selIdx)).getUserData();

                Slope slopeMath = new Slope();
                slopeMath.setGraphFunction(f);

                double m = Double.NaN;
                double px = 0, py = 0;

                if (f.getType() == GraphFunction.Type.PARAMETRIC) {
                    m = slopeMath.findParametricSlope(inputVal);
                    double[] coords = slopeMath.getCoordinatesAtT(inputVal);
                    px = coords[0]; py = coords[1];
                }
                else if (f.getType() == GraphFunction.Type.POLAR) {
                    m = slopeMath.findPolarSlope(inputVal);
                    double[] coords = slopeMath.getCoordinatesAtT(inputVal);
                    px = coords[0]; py = coords[1];
                }
                else if (f.getType() == GraphFunction.Type.CARTESIAN) {
                    m = slopeMath.findSlope(inputVal);
                    px = inputVal;
                    py = f.evaluate(px);
                }
                else if (f.isImplicit()) {
                    px = inputVal;
                    try {
                        py = Double.parseDouble(yInputField.getText().trim());
                    } catch (NumberFormatException ex) {
                        error.setText("Please enter a valid y coordinate.");
                        return;
                    }
                    m = slopeMath.findSlope(px, py);
                    if (Math.abs(f.evaluate(px, py)) > 0.5) {
                        error.setText("Note: Point is not perfectly on the curve.");
                    } else {
                        error.setText("");
                    }
                }

                if (Double.isNaN(m)) {
                    if (error.getText().isEmpty()) error.setText("Vertical tangent or undefined point.");
                    resSlope.setText("Slope (m) = Undefined");
                    resEquation.setText("x = " + String.format("%.3f", px));
                } else {
                    resSlope.setText(String.format("Slope (m) ≈ %.4f", m));

                    String sign = (m * -px + py) >= 0 ? "+" : "-";
                    double yInt = Math.abs(m * -px + py);
                    resEquation.setText(String.format("y = %.3fx %s %.3f", m, sign, yInt));
                }

                canvas.clearTangentLines();
                canvas.addTangentPoint(px, py, f.getColor());

                if (drawLineCheck.isSelected() && !Double.isNaN(m)) {
                    canvas.addTangentLine(px, py, m, f.getColor());
                }

                // --- NEW: ADD TO MENU LOGIC ---
                if (addToMenuCheck.isSelected() && !Double.isNaN(m)) {
                    // 1. Calculate the Y-Intercept algebraically
                    double yInt = py - m * px;
                    String signForMenu = yInt >= 0 ? "+" : "-";

                    // 2. Format the string safely for mxparser (e.g. 2.500*x + 1.200)
                    String eqStr = String.format("%.4f*x %s %.4f", m, signForMenu, Math.abs(yInt));

                    // 3. Smart Row Finder: Look for an existing empty row first!
                    TextField targetRow = null;
                    for (TextField tf : menu.getTextFields()) {
                        if (tf.getText().trim().isEmpty()) {
                            targetRow = tf;
                            break;
                        }
                    }

                    // If absolutely no empty rows exist, create one
                    if (targetRow == null) {
                        targetRow = menu.setTextField();
                    }

                    // 4. Fill the text and fire the Enter event
                    targetRow.setText(eqStr);
                    targetRow.fireEvent(new javafx.event.ActionEvent(javafx.event.ActionEvent.ACTION, targetRow));

                    // Auto-uncheck it so clicking "Calculate" repeatedly doesn't spam the sidebar
                    addToMenuCheck.setSelected(false);
                }

            } catch (NumberFormatException ex) {
                error.setText("Please enter a valid number.");
            }
        });

        clearBtn.setOnAction(e -> {
            canvas.clearTangentLines();
            resSlope.setText("Waiting for input...");
            resEquation.setText("y - y₁ = m(x - x₁)");
            error.setText("");
            inputField.clear();
            yInputField.clear();
        });

        this.getChildren().addAll(grid, btnGrid, resultCard, error);
    }
}