package ui;

import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import math.GraphFunction;
import model.ShadedRegion;

import java.util.ArrayList;
import java.util.List;

public class IntegralCalculator extends VBox {
    private final Menu menu;
    private final GraphCanvas canvas;
    private Label error;

    private final javafx.animation.PauseTransition mathDebouncer = new javafx.animation.PauseTransition(javafx.util.Duration.millis(100));

    public IntegralCalculator(Menu menu, GraphCanvas canvas) {
        this.menu = menu;
        this.canvas = canvas;

        this.getStyleClass().add("calc-dialog-bg");
        this.setSpacing(20);
        this.setPrefWidth(480);

        showInputState();
    }

    public void refresh() {
        showInputState();
    }

    private void showInputState() {
        this.getChildren().clear();

        ToggleGroup axisGroup = new ToggleGroup();
        RadioButton xAxisBtn = new RadioButton("Integrate w.r.t X (dx)");
        xAxisBtn.setToggleGroup(axisGroup);
        xAxisBtn.setSelected(true);
        xAxisBtn.getStyleClass().add("calc-label");

        RadioButton yAxisBtn = new RadioButton("Integrate w.r.t Y (dy)");
        yAxisBtn.setToggleGroup(axisGroup);
        yAxisBtn.getStyleClass().add("calc-label");

        HBox axisSelection = new HBox(20, xAxisBtn, yAxisBtn);
        axisSelection.setAlignment(Pos.CENTER);

        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(8);

        Label f1Label = new Label("Primary Function f(x)");
        f1Label.getStyleClass().add("calc-label");
        ComboBox<String> f1Dropdown = new ComboBox<>();
        f1Dropdown.setPromptText("Select f(x)");
        f1Dropdown.setMaxWidth(Double.MAX_VALUE);
        f1Dropdown.getStyleClass().add("combo-box");

        Label f2Label = new Label("Secondary Function g(x)");
        f2Label.getStyleClass().add("calc-label");
        ComboBox<String> f2Dropdown = new ComboBox<>();
        f2Dropdown.setPromptText("Select g(x) (Optional)");
        f2Dropdown.setMaxWidth(Double.MAX_VALUE);
        f2Dropdown.getStyleClass().add("combo-box");
        f2Dropdown.getItems().add("None");

        List<Integer> validIndices = new ArrayList<>();
        for (int i = 0; i < menu.getTextFields().size(); i++) {
            TextField tf = menu.getTextFields().get(i);
            if (tf.getUserData() instanceof GraphFunction) {
                String eq = tf.getText().isEmpty() ? "Empty" : tf.getText();
                String display = "f" + (i + 1) + "(x) = " + eq;
                f1Dropdown.getItems().add(display);
                f2Dropdown.getItems().add(display);
                validIndices.add(i);
            }
        }

        Label aLabel = new Label("Lower Bound (a):");
        aLabel.getStyleClass().add("calc-label");
        TextField aInput = new TextField();
        aInput.setPromptText("Σ e.g., 0");
        aInput.getStyleClass().add("input-field");

        Label bLabel = new Label("Upper Bound (b):");
        bLabel.getStyleClass().add("calc-label");
        TextField bInput = new TextField();
        bInput.setPromptText("π e.g., 3.14");
        bInput.getStyleClass().add("input-field");

        axisGroup.selectedToggleProperty().addListener((obs, old, newVal) -> {
            canvas.clearInteractiveIntegration();
            if (newVal == yAxisBtn) {
                f1Label.setText("Primary Function f(y)"); f2Label.setText("Secondary Function g(y)");
                aLabel.setText("Lower Bound (c):"); bLabel.setText("Upper Bound (d):");
            } else {
                f1Label.setText("Primary Function f(x)"); f2Label.setText("Secondary Function g(x)");
                aLabel.setText("Lower Bound (a):"); bLabel.setText("Upper Bound (b):");
            }
        });
        f1Dropdown.setOnAction(e -> canvas.clearInteractiveIntegration());
        f2Dropdown.setOnAction(e -> canvas.clearInteractiveIntegration());

        grid.add(f1Label, 0, 0); grid.add(f2Label, 1, 0);
        grid.add(f1Dropdown, 0, 1); grid.add(f2Dropdown, 1, 1);
        grid.add(aLabel, 0, 2); grid.add(bLabel, 1, 2);
        grid.add(aInput, 0, 3); grid.add(bInput, 1, 3);

        ColumnConstraints col1 = new ColumnConstraints(); col1.setPercentWidth(50);
        ColumnConstraints col2 = new ColumnConstraints(); col2.setPercentWidth(50);
        grid.getColumnConstraints().addAll(col1, col2);

        Button calcBtn = new Button("Calculate Area");
        calcBtn.getStyleClass().add("primary-btn");
        calcBtn.setMaxWidth(Double.MAX_VALUE);

        Button clearBtn = new Button("Clear All");
        clearBtn.getStyleClass().add("secondary-btn");
        clearBtn.setMaxWidth(Double.MAX_VALUE);

        GridPane btnGrid = new GridPane();
        btnGrid.setHgap(15);
        btnGrid.add(calcBtn, 0, 0); btnGrid.add(clearBtn, 1, 0);
        btnGrid.getColumnConstraints().addAll(col1, col2);

        VBox resultCard = new VBox();
        resultCard.getStyleClass().add("result-card");
        Label resHeaderLabel = new Label("Calculated Area (A):");
        resHeaderLabel.getStyleClass().add("result-header-text");
        HBox resHeader = new HBox(resHeaderLabel);
        resHeader.getStyleClass().add("result-header");

        Label resValue = new Label("Waiting for input...");
        resValue.getStyleClass().add("result-value");

        Label resFormula = new Label("A = ∫ | f(x) - g(x) | dx");
        resFormula.getStyleClass().add("result-formula");
        resultCard.getChildren().addAll(resHeader, resValue, resFormula);

        error = new Label("");
        error.getStyleClass().add("error-label");

        calcBtn.setOnAction(e -> {
            try {
                int f1SelIdx = f1Dropdown.getSelectionModel().getSelectedIndex();
                int f2SelIdx = f2Dropdown.getSelectionModel().getSelectedIndex();

                if (f1SelIdx < 0) {
                    error.setText("Please select Primary Function.");
                    return;
                }

                double a = Double.parseDouble(aInput.getText().trim());
                double b = Double.parseDouble(bInput.getText().trim());

                GraphFunction f1 = (GraphFunction) menu.getTextFields().get(validIndices.get(f1SelIdx)).getUserData();
                GraphFunction f2 = f2SelIdx > 0 ? (GraphFunction) menu.getTextFields().get(validIndices.get(f2SelIdx - 1)).getUserData() : null;

                if (f1.isImplicit() || (f2 != null && f2.isImplicit())) {
                    error.setText("Error: Implicit functions cannot be integrated.");
                    resValue.setText("Error");
                    canvas.clearInteractiveIntegration();
                    return;
                }

                boolean isRespectToY = yAxisBtn.isSelected();
                double initialArea = 0.0;

                if (f1.getType() == GraphFunction.Type.PARAMETRIC) {
                    initialArea = calculateParametricArea(f1, a, b);
                    resFormula.setText("A = ∫ y(t)x'(t) dt");
                } else if (f1.getType() == GraphFunction.Type.POLAR) {
                    initialArea = calculatePolarArea(f1, a, b);
                    resFormula.setText("A = ½ ∫ [r(θ)]² dθ");
                } else {
                    initialArea = calculateNumericalArea(f1, f2, a, b, isRespectToY);
                    resFormula.setText(isRespectToY ? "A = ∫ | f(y) - g(y) | dy" : "A = ∫ | f(x) - g(x) | dx");
                }

                resValue.setText(String.format("≈ %.4f units²", initialArea));
                error.setText("");

                ShadedRegion region = new ShadedRegion(f1, f2, a, b, f1.getColor(), isRespectToY);
                canvas.setInteractiveIntegration(region, (newA, newB) -> {
                    aInput.setText(String.format("%.2f", newA));
                    bInput.setText(String.format("%.2f", newB));
                    resValue.setText("Calculating...");

                    mathDebouncer.setOnFinished(event -> {
                        new Thread(() -> {
                            double dynamicArea = 0.0;
                            if (f1.getType() == GraphFunction.Type.PARAMETRIC) dynamicArea = calculateParametricArea(f1, newA, newB);
                            else if (f1.getType() == GraphFunction.Type.POLAR) dynamicArea = calculatePolarArea(f1, newA, newB);
                            else dynamicArea = calculateNumericalArea(f1, f2, newA, newB, isRespectToY);

                            final double finalArea = dynamicArea;
                            Platform.runLater(() -> resValue.setText(String.format("≈ %.4f units²", finalArea)));
                        }).start();
                    });
                    mathDebouncer.playFromStart();
                });

            } catch (NumberFormatException ex) {
                error.setText("Please enter valid numeric bounds.");
            }
        });

        clearBtn.setOnAction(e -> {
            canvas.clearInteractiveIntegration();
            resValue.setText("Waiting for input...");
            resFormula.setText("A = ∫ | f(x) - g(x) | dx");
            error.setText("");
            f1Dropdown.getSelectionModel().clearSelection();
            f2Dropdown.getSelectionModel().clearSelection();
            aInput.clear(); bInput.clear();
        });

        this.getChildren().addAll(axisSelection, grid, btnGrid, resultCard, error);
    }

    private double calculateNumericalArea(GraphFunction f1, GraphFunction f2, double a, double b, boolean isRespectToY) {
        int n = 10000;
        double min = Math.min(a, b); double max = Math.max(a, b);
        double h = (max - min) / n; double totalArea = 0.0;

        double trackX1 = 0.1;
        double trackX2 = 0.1;

        for (int i = 0; i < n; i++) {
            double v1 = min + (i * h); double v2 = min + ((i + 1) * h);
            double y1, y2;

            if (isRespectToY) {
                trackX1 = f1.inverseEvaluate(v1, trackX1);
                double currentX1 = trackX1;

                double currentF2_v1 = 0;
                if (f2 != null) {
                    trackX2 = f2.inverseEvaluate(v1, trackX2);
                    currentF2_v1 = trackX2;
                }
                y1 = Math.abs(currentX1 - currentF2_v1);

                trackX1 = f1.inverseEvaluate(v2, trackX1);
                double nextX1 = trackX1;

                double nextF2_v2 = 0;
                if (f2 != null) {
                    trackX2 = f2.inverseEvaluate(v2, trackX2);
                    nextF2_v2 = trackX2;
                }
                y2 = Math.abs(nextX1 - nextF2_v2);

            } else {
                y1 = Math.abs(f1.evaluate(v1) - ((f2 != null) ? f2.evaluate(v1) : 0));
                y2 = Math.abs(f1.evaluate(v2) - ((f2 != null) ? f2.evaluate(v2) : 0));
            }

            if (!Double.isNaN(y1) && !Double.isNaN(y2)) {
                totalArea += (h / 2.0) * (y1 + y2);
            }
        }
        return totalArea;
    }

    private double calculateParametricArea(GraphFunction f, double tStart, double tEnd) {
        int n = 10000;
        double h = (tEnd - tStart) / n;
        double area = 0.0; double diffH = 1e-7;

        for (int i = 0; i < n; i++) {
            double t1 = tStart + (i * h); double t2 = tStart + ((i + 1) * h);
            double y1 = f.evaluateParametricY(t1); double y2 = f.evaluateParametricY(t2);
            double dxdt1 = (f.evaluateT(t1 + diffH) - f.evaluateT(t1 - diffH)) / (2 * diffH);
            double dxdt2 = (f.evaluateT(t2 + diffH) - f.evaluateT(t2 - diffH)) / (2 * diffH);
            area += (h / 2.0) * (y1 * dxdt1 + y2 * dxdt2);
        }
        return Math.abs(area);
    }

    private double calculatePolarArea(GraphFunction f, double thetaStart, double thetaEnd) {
        int n = 10000;
        double h = (thetaEnd - thetaStart) / n;
        double area = 0.0;
        for (int i = 0; i < n; i++) {
            double r1 = f.evaluateT(thetaStart + (i * h));
            double r2 = f.evaluateT(thetaStart + ((i + 1) * h));
            area += (h / 2.0) * (0.5 * r1 * r1 + 0.5 * r2 * r2);
        }
        return Math.abs(area);
    }
}