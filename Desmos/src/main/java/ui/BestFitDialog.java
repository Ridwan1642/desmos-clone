package ui;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import math.calculateLeastSquares;

import java.util.ArrayList;
import java.util.List;

public class BestFitDialog extends Dialog<String> {

    private final VBox pointsContainer = new VBox(5);
    private final List<TextField> xInputs = new ArrayList<>();
    private final List<TextField> yInputs = new ArrayList<>();

    public BestFitDialog() {
        setTitle("Calculate Best Fit Line");
        setHeaderText("Enter up to 10 coordinate points.");

        ComboBox<Integer> pointCountBox = new ComboBox<>();
        pointCountBox.getItems().addAll(2, 3, 4, 5, 6, 7, 8, 9, 10);
        pointCountBox.setValue(3);
        pointCountBox.setOnAction(e -> generateInputFields(pointCountBox.getValue()));

        HBox topControls = new HBox(10, new Label("Number of points:"), pointCountBox);

        VBox mainLayout = new VBox(15, topControls, pointsContainer);
        mainLayout.setPadding(new Insets(10));
        getDialogPane().setContent(mainLayout);

        ButtonType calculateButtonType = new ButtonType("Calculate", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(calculateButtonType, ButtonType.CANCEL);

        generateInputFields(3);

        setResultConverter(dialogButton -> {
            if (dialogButton == calculateButtonType) {
                return processInputAndCalculate();
            }
            return null;
        });
    }

    private void generateInputFields(int count) {
        pointsContainer.getChildren().clear();
        xInputs.clear();
        yInputs.clear();

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(5);
        grid.add(new Label("X"), 0, 0);
        grid.add(new Label("Y"), 1, 0);

        for (int i = 0; i < count; i++) {
            TextField xField = new TextField();
            xField.setPromptText("x" + (i + 1));
            xField.setPrefWidth(60);

            TextField yField = new TextField();
            yField.setPromptText("y" + (i + 1));
            yField.setPrefWidth(60);

            xInputs.add(xField);
            yInputs.add(yField);

            grid.add(xField, 0, i + 1);
            grid.add(yField, 1, i + 1);
        }
        pointsContainer.getChildren().add(grid);
        getDialogPane().getScene().getWindow().sizeToScene();
    }

    // This method now only handles extracting the UI data and parsing it
    private String processInputAndCalculate() {
        List<Double> xVals = new ArrayList<>();
        List<Double> yVals = new ArrayList<>();

        for (int i = 0; i < xInputs.size(); i++) {
            try {
                double x = Double.parseDouble(xInputs.get(i).getText());
                double y = Double.parseDouble(yInputs.get(i).getText());

                xVals.add(x);
                yVals.add(y);
            } catch (NumberFormatException ignored) {
                // Ignore empty or invalid inputs, just like before
            }
        }

        // Pass the raw numbers to the math package
        return calculateLeastSquares.calculateBestFitLine(xVals, yVals);
    }
}