package ui;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.control.Label;
import javafx.scene.paint.Color;
import math.GraphFunction;
import model.ShadedRegion;

public class IntegralCalculator extends VBox {
    private Menu menu;
    private GraphCanvas canvas;
    private Label error;
    private boolean isDarkMode = false;

    public IntegralCalculator(Menu menu, GraphCanvas canvas) {
        this.menu = menu;
        this.canvas = canvas;
        this.error = new Label();
        this.error.setMinHeight(25);
        this.error.setPrefHeight(25);
        this.error.setAlignment(Pos.BOTTOM_CENTER);

        this.setSpacing(10);
        setDarkMode(false);
        showInputState();
    }

    public void setDarkMode(boolean dark) {
        this.isDarkMode = dark;
        this.setStyle(dark ? "-fx-background-color: #404040; -fx-padding: 15;" : "-fx-background-color: white; -fx-padding: 15;");
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
                    l.setStyle("-fx-text-fill: #ff6b6b; -fx-font-size: 14px;");
                } else if (l.getText().startsWith("Area")) {
                    l.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: " + (isDarkMode ? "#E040FB" : "#9C27B0") + ";");
                } else {
                    l.setStyle("-fx-font-weight: bold; -fx-font-size: 16px; -fx-text-fill: " + textFill + ";");
                }
            } else if (node instanceof TextField) {
                ((TextField) node).setStyle("-fx-background-color: " + fieldBg + "; -fx-text-fill: " + textFill + "; " + border);
            } else if (node instanceof javafx.scene.Parent) {
                applyTheme((javafx.scene.Parent) node);
            }
        }
    }

    private void setErrLabel(String message) {
        error.setText(message);
        error.setStyle("-fx-text-fill: " + (isDarkMode ? "#ff6b6b" : "red") + "; -fx-font-size: 14px;");
    }

    private void showInputState() {
        this.getChildren().clear();
        setErrLabel("");

        Label title = new Label("Area Between Curves");
        HBox topRow = new HBox(title);
        topRow.setAlignment(Pos.CENTER_LEFT);
        topRow.setPadding(new javafx.geometry.Insets(0, 0, 10, 0));

        TextField f1Input = new TextField();
        f1Input.setPromptText("Func 1 (e.g., 1)");
        TextField f2Input = new TextField();
        f2Input.setPromptText("Func 2 (Optional)");
        TextField aInput = new TextField();
        aInput.setPromptText("Lower Bound (a)");
        aInput.setPrefWidth(110);
        TextField bInput = new TextField();
        bInput.setPromptText("Upper Bound (b)");
        bInput.setPrefWidth(110);

        HBox boundsRow = new HBox(10, aInput, bInput);
        Button calcBtn = new Button("Calculate");
        Button clearBtn = new Button("Clear");
        HBox btnRow = new HBox(10, calcBtn, clearBtn);
        btnRow.setAlignment(Pos.CENTER_LEFT);

        Label resultLabel = new Label("Area: ");

        calcBtn.setOnAction(e -> {
            try {
                int f1Idx = Integer.parseInt(f1Input.getText().trim()) - 1;
                int f2Idx = f2Input.getText().trim().isEmpty() ? -1 : Integer.parseInt(f2Input.getText().trim()) - 1;
                double a = Double.parseDouble(aInput.getText().trim());
                double b = Double.parseDouble(bInput.getText().trim());

                if (f1Idx < 0 || f1Idx >= menu.getTextFields().size()) {
                    setErrLabel("Function 1 is invalid.");
                    return;
                }

                GraphFunction f1 = (GraphFunction) menu.getTextFields().get(f1Idx).getUserData();
                GraphFunction f2 = null;

                if (f2Idx != -1) {
                    if (f2Idx >= 0 && f2Idx < menu.getTextFields().size()) {
                        f2 = (GraphFunction) menu.getTextFields().get(f2Idx).getUserData();
                    } else {
                        setErrLabel("Function 2 is invalid.");
                        return;
                    }
                }

                if (f1 == null || (f2Idx != -1 && f2 == null)) {
                    setErrLabel("Target function is empty.");
                    return;
                }

                double area = calculateNumericalArea(f1, f2, a, b);
                resultLabel.setText(String.format(java.util.Locale.US, "Area: %.4f", area));
                setErrLabel("");

                Color shadeColor = f1.getColor();
                ShadedRegion region = new ShadedRegion(f1, f2, a, b, shadeColor);
                canvas.clearShadedRegions();
                canvas.addShadedRegion(region);
                canvas.redraw();

            } catch (NumberFormatException ex) {
                setErrLabel("Please enter valid numbers.");
            }
        });

        clearBtn.setOnAction(e -> {
            canvas.clearShadedRegions();
            canvas.redraw();
            resultLabel.setText("Area: ");
            setErrLabel("");
        });

        this.getChildren().addAll(topRow, f1Input, f2Input, boundsRow, btnRow, resultLabel, error);
        applyTheme(this);
    }

    private double calculateNumericalArea(GraphFunction f1, GraphFunction f2, double a, double b) {
        int n = 10000;
        double minA = Math.min(a, b);
        double maxB = Math.max(a, b);
        double h = (maxB - minA) / n;
        double totalArea = 0.0;

        for (int i = 0; i < n; i++) {
            double x1 = minA + (i * h);
            double x2 = minA + ((i + 1) * h);

            double y1 = Math.abs(f1.evaluate(x1) - ((f2 != null) ? f2.evaluate(x1) : 0));
            double y2 = Math.abs(f1.evaluate(x2) - ((f2 != null) ? f2.evaluate(x2) : 0));

            if (!Double.isNaN(y1) && !Double.isNaN(y2)) {
                totalArea += (h / 2.0) * (y1 + y2);
            }
        }
        return totalArea;
    }
}