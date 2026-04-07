package ui;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import math.calculateLeastSquares;

import java.util.ArrayList;
import java.util.List;

public class BestFitDialog extends Dialog<String> {

    private final boolean isDarkMode;
    private TableView<DataPoint> table;
    private ObservableList<DataPoint> dataPoints;
    private Label errorLabel;

    public static class DataPoint {
        private final SimpleStringProperty x;
        private final SimpleStringProperty y;

        public DataPoint(String x, String y) {
            this.x = new SimpleStringProperty(x);
            this.y = new SimpleStringProperty(y);
        }

        public String getX() { return x.get(); }
        public void setX(String val) { x.set(val); }
        public SimpleStringProperty xProperty() { return x; }

        public String getY() { return y.get(); }
        public void setY(String val) { y.set(val); }
        public SimpleStringProperty yProperty() { return y; }
    }

    public BestFitDialog(boolean isDarkMode) {
        this.isDarkMode = isDarkMode;
        setTitle("Calculate Best Fit Line");
        setHeaderText("Enter coordinate points. (Press Enter to save a cell)");

        String bgColor = isDarkMode ? "#1e293b" : "#f8fafc";
        getDialogPane().setStyle("-fx-background-color: " + bgColor + "; -fx-font-family: 'Segoe UI', sans-serif;");

        getDialogPane().getStyleClass().add("calc-dialog-bg");
        if (isDarkMode) getDialogPane().getStyleClass().add("dark-theme");

        try {
            String css = getClass().getResource("/ui/theme.css").toExternalForm();
            getDialogPane().getStylesheets().add(css);
        } catch (Exception ex) {
            System.err.println("Could not load theme.css for Dialog");
        }

        table = new TableView<>();
        table.setEditable(true);
        table.setPrefHeight(250);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<DataPoint, String> xCol = new TableColumn<>("X");
        xCol.setCellValueFactory(cellData -> cellData.getValue().xProperty());
        xCol.setCellFactory(TextFieldTableCell.forTableColumn());

        xCol.setOnEditCommit(event -> {
            event.getRowValue().setX(event.getNewValue());
            ensureEmptyLastRow();
        });

        TableColumn<DataPoint, String> yCol = new TableColumn<>("Y");
        yCol.setCellValueFactory(cellData -> cellData.getValue().yProperty());
        yCol.setCellFactory(TextFieldTableCell.forTableColumn());

        yCol.setOnEditCommit(event -> {
            event.getRowValue().setY(event.getNewValue());
            ensureEmptyLastRow();
        });

        table.getColumns().addAll(xCol, yCol);

        table.getSelectionModel().setCellSelectionEnabled(true);
        table.setOnMouseClicked(event -> {
            if (event.getClickCount() == 1 && !table.getSelectionModel().isEmpty()) {
                TablePosition<DataPoint, ?> pos = table.getSelectionModel().getSelectedCells().get(0);
                table.edit(pos.getRow(), pos.getTableColumn());
            }
        });

        dataPoints = FXCollections.observableArrayList(new DataPoint("", ""));
        table.setItems(dataPoints);

        Button clearBtn = new Button("Clear All");
        clearBtn.getStyleClass().add("secondary-btn");
        clearBtn.setOnAction(e -> {
            dataPoints.clear();
            dataPoints.add(new DataPoint("", ""));
            errorLabel.setText("");
        });

        HBox topControls = new HBox(10, clearBtn);
        topControls.setAlignment(Pos.CENTER);


        errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: #ef4444; -fx-font-weight: bold; -fx-font-size: 13px;");
        errorLabel.setAlignment(Pos.CENTER);
        errorLabel.setMaxWidth(Double.MAX_VALUE);

        VBox mainLayout = new VBox(15, topControls, table, errorLabel);
        mainLayout.setPadding(new Insets(10, 0, 0, 0));
        VBox.setVgrow(table, Priority.ALWAYS);
        getDialogPane().setContent(mainLayout);

        ButtonType calculateButtonType = new ButtonType("Calculate", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(calculateButtonType, ButtonType.CANCEL);

        Button calcNode = (Button) getDialogPane().lookupButton(calculateButtonType);
        if (calcNode != null) {
            calcNode.getStyleClass().add("primary-btn");

            calcNode.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
                table.edit(-1, null);
                String result = processInputAndCalculate();

                if (result.startsWith("Error:")) {
                    errorLabel.setText(result);
                    event.consume();
                } else {
                    errorLabel.setText("");
                }
            });
        }

        Button cancelNode = (Button) getDialogPane().lookupButton(ButtonType.CANCEL);
        if (cancelNode != null) cancelNode.getStyleClass().add("secondary-btn");

        setResultConverter(dialogButton -> {
            if (dialogButton == calculateButtonType) {
                return processInputAndCalculate();
            }
            return null;
        });
    }

    private void ensureEmptyLastRow() {
        if (dataPoints.isEmpty()) {
            dataPoints.add(new DataPoint("", ""));
            return;
        }

        DataPoint lastPoint = dataPoints.get(dataPoints.size() - 1);
        boolean xHasData = lastPoint.getX() != null && !lastPoint.getX().trim().isEmpty();
        boolean yHasData = lastPoint.getY() != null && !lastPoint.getY().trim().isEmpty();

        if (xHasData || yHasData) {
            dataPoints.add(new DataPoint("", ""));
            table.scrollTo(dataPoints.size() - 1);
        }
    }

    private String processInputAndCalculate() {
        List<Double> xVals = new ArrayList<>();
        List<Double> yVals = new ArrayList<>();

        for (DataPoint dp : dataPoints) {
            try {
                if (dp.getX() != null && dp.getY() != null && !dp.getX().trim().isEmpty() && !dp.getY().trim().isEmpty()) {
                    double x = Double.parseDouble(dp.getX().trim());
                    double y = Double.parseDouble(dp.getY().trim());
                    xVals.add(x);
                    yVals.add(y);
                }
            } catch (NumberFormatException ignored) {}
        }

        if (xVals.size() < 2) {
            return "Error: Best fit requires at least 2 valid points.";
        }

        return calculateLeastSquares.calculateBestFitLine(xVals, yVals);
    }

    public List<double[]> getParsedPoints() {
        List<double[]> points = new ArrayList<>();
        for (DataPoint dp : dataPoints) {
            try {
                if (dp.getX() != null && dp.getY() != null && !dp.getX().trim().isEmpty() && !dp.getY().trim().isEmpty()) {
                    double x = Double.parseDouble(dp.getX().trim());
                    double y = Double.parseDouble(dp.getY().trim());
                    points.add(new double[]{x, y});
                }
            } catch (NumberFormatException ignored) {}
        }
        return points;
    }
}