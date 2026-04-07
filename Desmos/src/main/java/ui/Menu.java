package ui;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

import javafx.geometry.Pos;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.input.KeyCode;
import math.GraphFunction;

public class Menu extends VBox {
    private static final String NORMAL_CHARS = "0123456789xyn+-().t";
    private static final String SUPER_CHARS  = "⁰¹²³⁴⁵⁶⁷⁸⁹ˣʸⁿ⁺⁻⁽⁾·ᵗ";

    private final VBox functionBox = new VBox();
    private final Label error;
    private final List<TextField> textFields = new ArrayList<>();
    private final ArrayList<Color> defaultPalette = new ArrayList<>(List.of(
            Color.web("#c74440"), Color.web("#2d70b3"), Color.web("#388c46"),
            Color.web("#6042a6"), Color.web("#000000")
    ));
    private int nextColor = 0;
    private final BiConsumer<TextField, Color> onSubmit;
    private final GraphCanvas canvas;
    private boolean isDarkMode = false;

    public Menu(BiConsumer<TextField, Color> onSubmit, GraphCanvas canvas) {
        this.getStyleClass().add("menu-sidebar");
        this.setPrefWidth(350);
        this.setMaxWidth(350);

        this.onSubmit = onSubmit;
        this.canvas = canvas;

        error = new Label();
        error.setStyle("-fx-text-fill: #ef4444; -fx-padding: 10; -fx-font-weight: bold;");

        setTextField();

        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setContent(functionBox);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent; -fx-border-color: transparent;");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        Button addBtn = new Button("+ Add Expression");
        addBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #888; -fx-font-size: 14px; -fx-cursor: hand; -fx-font-weight: bold;");
        addBtn.setOnAction(e -> setTextField().requestFocus());

        this.getChildren().addAll(scrollPane, addBtn, error);
    }

    public void setDarkMode(boolean dark) {
        this.isDarkMode = dark;
        canvas.setDarkMode(dark);
        for (TextField tf : textFields) {
            if (!tf.getText().startsWith("Error:")) {
                tf.setStyle("-fx-text-fill: " + (dark ? "white" : "black") + "; -fx-font-size: 18px;");
            }
        }
    }

    public Color getNextPaletteColor() {
        Color color = defaultPalette.get(nextColor);
        nextColor = (nextColor + 1) % defaultPalette.size();
        return color;
    }

    public TextField setTextFieldAvoiding(Color avoidColor) { return setTextField(); }

    public TextField setTextField() {
        int rowIndex = textFields.size() + 1;
        Color assignedColor = getNextPaletteColor();

        VBox cellBox = new VBox();
        cellBox.getStyleClass().add("function-row");

        Label numberLabel = new Label(String.valueOf(rowIndex));
        numberLabel.getStyleClass().add("row-number");
        numberLabel.setMinWidth(20);

        Button visibilityBtn = new Button();
        javafx.scene.shape.Circle visibilityCircle = new javafx.scene.shape.Circle(7, assignedColor);
        visibilityBtn.setGraphic(visibilityCircle);
        visibilityBtn.setMinSize(26, 26);
        visibilityBtn.setMaxSize(26, 26);
        visibilityBtn.getStyleClass().add("icon-btn");

        ColorPicker colorPicker = new ColorPicker(assignedColor);
        colorPicker.setMinSize(28, 28);
        colorPicker.setMaxSize(28, 28);
        colorPicker.getStyleClass().add("icon-color-picker");

        TextField input = new TextField();
        input.getStyleClass().add("math-input");
        HBox.setHgrow(input, Priority.ALWAYS);
        input.setPromptText("y = ...");

        Button removeBtn = new Button("X");
        removeBtn.setMinSize(26, 26);
        removeBtn.setMaxSize(26, 26);
        removeBtn.getStyleClass().add("icon-btn");
        removeBtn.setStyle("-fx-text-fill: #ef4444;");

        HBox topRow = new HBox(5, numberLabel, visibilityBtn, colorPicker, input, removeBtn);
        topRow.setAlignment(Pos.CENTER_LEFT);

        VBox sliderBox = new VBox(5);
        sliderBox.setPadding(new Insets(5, 5, 5, 40));

        cellBox.getChildren().addAll(topRow, sliderBox);
        functionBox.getChildren().add(cellBox);
        textFields.add(input);
        input.getProperties().put("colorPicker", colorPicker);

        input.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.RIGHT) {
                int caret = input.getCaretPosition();
                String text = input.getText();
                if (caret > 0 && SUPER_CHARS.indexOf(text.charAt(caret - 1)) != -1) {
                    if (caret == text.length() || text.charAt(caret) != '\u200B') {
                        String newText = text.substring(0, caret) + "\u200B" + text.substring(caret);
                        input.setText(newText);
                        input.positionCaret(caret + 1);
                        e.consume();
                    }
                }
            }
        });

        input.textProperty().addListener((obs, oldText, newText) -> {
            if (newText == null) return;
            if (newText.startsWith("Error:")) {
                return;
            }

            String defaultColor = isDarkMode ? "white" : "black";
            input.setStyle("-fx-text-fill: " + defaultColor + "; -fx-font-size: 18px;");
            error.setText("");

            String replaced = newText;
            for (int i = 0; i < NORMAL_CHARS.length(); i++) {
                replaced = replaced.replace("^" + NORMAL_CHARS.charAt(i), String.valueOf(SUPER_CHARS.charAt(i)));
            }

            char[] chars = replaced.toCharArray();
            boolean modified = false;
            for (int i = 1; i < chars.length; i++) {
                if (SUPER_CHARS.indexOf(chars[i - 1]) != -1) {
                    int normalIdx = NORMAL_CHARS.indexOf(chars[i]);
                    if (normalIdx != -1) {
                        chars[i] = SUPER_CHARS.charAt(normalIdx);
                        modified = true;
                    }
                }
            }
            if (modified) replaced = new String(chars);

            if (!replaced.equals(newText)) {
                final String finalText = replaced;
                javafx.application.Platform.runLater(() -> {
                    int diff = newText.length() - finalText.length();
                    int originalCaret = input.getCaretPosition();
                    input.setText(finalText);
                    input.positionCaret(Math.max(0, originalCaret - diff));
                });
                return;
            }

            String checkStr = replaced.trim();
            if (checkStr.contains("=")) {
                String leftSide = checkStr.substring(0, checkStr.indexOf('=')).trim();

                if (leftSide.matches(".*(^|[^a-zA-Z])r([^a-zA-Z]|$).*") && !leftSide.equals("r")) {
                    javafx.application.Platform.runLater(() -> {
                        input.setText("Error: Invalid Polar");
                        input.setStyle("-fx-text-fill: red; -fx-font-size: 18px;");
                        input.selectAll();
                    });
                    return;
                }
            }

            if (onSubmit != null && !newText.trim().isEmpty()) {
                onSubmit.accept(input, colorPicker.getValue());
            }
        });


        boolean[] isVisible = {true};
        visibilityBtn.setOnAction(e -> {
            isVisible[0] = !isVisible[0];
            if (isVisible[0]) {
                visibilityCircle.setFill(colorPicker.getValue());
                visibilityCircle.setStroke(Color.TRANSPARENT);
            } else {
                visibilityCircle.setFill(Color.TRANSPARENT);
                visibilityCircle.setStroke(Color.GRAY);
                visibilityCircle.setStrokeWidth(2);
            }
            Object data = input.getUserData();
            if (data instanceof GraphFunction) {
                ((GraphFunction) data).setVisible(isVisible[0]);
                canvas.redraw();
            }
        });

        colorPicker.setOnAction(e -> {
            Color newColor = colorPicker.getValue();
            if (isVisible[0]) visibilityCircle.setFill(newColor);

            Object data = input.getUserData();
            if (data instanceof GraphFunction) {
                ((GraphFunction) data).setColor(newColor);
                canvas.redraw();
            }
        });

        removeBtn.setOnAction(e -> {
            Object data = input.getUserData();
            if (data instanceof GraphFunction) canvas.removeFunction((GraphFunction) data);
            functionBox.getChildren().remove(cellBox);
            textFields.remove(input);
            recalculateRowNumbers();
            canvas.redraw();
        });

        input.setOnAction(e -> {
            int currentIndex = textFields.indexOf(input);
            if (currentIndex == textFields.size() - 1 && !input.getText().trim().isEmpty()) {
                TextField nextInput = setTextField();
                nextInput.requestFocus();
            } else if (currentIndex < textFields.size() - 1) {
                textFields.get(currentIndex + 1).requestFocus();
            }

            sliderBox.getChildren().clear();
            Object data = input.getUserData();

            if (data instanceof GraphFunction) {
                GraphFunction func = (GraphFunction) data;
                if (func.getType() == GraphFunction.Type.PARAMETRIC || func.getType() == GraphFunction.Type.POLAR) {
                    HBox domainBox = new HBox(8);
                    domainBox.setAlignment(Pos.CENTER_LEFT);

                    String varName = func.getType() == GraphFunction.Type.PARAMETRIC ? "t" : "θ";

                    String startMin = Math.abs(func.getTMin() - 0) < 0.001 ? "0" : String.format("%.2f", func.getTMin());
                    String startMax = Math.abs(func.getTMax() - (12 * Math.PI)) < 0.001 ? "12pi" : String.format("%.2f", func.getTMax());

                    TextField minField = new TextField(startMin);
                    minField.setPrefWidth(60);
                    minField.setStyle(isDarkMode ? "-fx-background-color: transparent; -fx-text-fill: white; -fx-border-color: #475569; -fx-border-radius: 4;" : "-fx-background-color: transparent; -fx-border-color: #cbd5e1; -fx-border-radius: 4;");

                    Label domainLabel = new Label(" ≤  " + varName + "  ≤ ");
                    domainLabel.setStyle(isDarkMode ? "-fx-text-fill: white; -fx-font-weight: bold;" : "-fx-text-fill: #334155; -fx-font-weight: bold;");

                    TextField maxField = new TextField(startMax);
                    maxField.setPrefWidth(60);
                    maxField.setStyle(isDarkMode ? "-fx-background-color: transparent; -fx-text-fill: white; -fx-border-color: #475569; -fx-border-radius: 4;" : "-fx-background-color: transparent; -fx-border-color: #cbd5e1; -fx-border-radius: 4;");

                    domainBox.getChildren().addAll(minField, domainLabel, maxField);
                    sliderBox.getChildren().add(domainBox);

                    javafx.beans.value.ChangeListener<String> boundsListener = (obs, oldV, newV) -> {
                        org.mariuszgromada.math.mxparser.Expression minE = new org.mariuszgromada.math.mxparser.Expression(minField.getText().trim());
                        org.mariuszgromada.math.mxparser.Expression maxE = new org.mariuszgromada.math.mxparser.Expression(maxField.getText().trim());

                        double minVal = minE.calculate();
                        double maxVal = maxE.calculate();

                        if (!Double.isNaN(minVal) && !Double.isNaN(maxVal) && minVal < maxVal) {
                            func.setTMin(minVal);
                            func.setTMax(maxVal);
                            canvas.redraw();
                        }
                    };

                    minField.textProperty().addListener(boundsListener);
                    maxField.textProperty().addListener(boundsListener);
                }
                for (org.mariuszgromada.math.mxparser.Argument arg : func.getParameters()) {
                    Slider slider = new Slider(-10, 10, 1);
                    HBox.setHgrow(slider, Priority.ALWAYS);
                    slider.setShowTickMarks(true);

                    Label sLabel = new Label(arg.getArgumentName() + " = ");
                    sLabel.setStyle(isDarkMode ? "-fx-text-fill: white; -fx-font-weight: bold;" : "-fx-text-fill: #334155; -fx-font-weight: bold;");

                    TextField valueField = new TextField("1.00");
                    valueField.setPrefWidth(55);
                    valueField.setStyle("-fx-background-color: transparent; -fx-border-color: #cbd5e1; -fx-border-radius: 4;");
                    if (isDarkMode) valueField.setStyle("-fx-background-color: transparent; -fx-text-fill: white; -fx-border-color: #475569; -fx-border-radius: 4;");

                    slider.valueProperty().addListener((o, oldNum, newNum) -> {
                        arg.setArgumentValue(newNum.doubleValue());
                        valueField.setText(String.format("%.2f", newNum.doubleValue()));
                        canvas.redraw();
                    });
                    valueField.setOnAction(ev -> {
                        try {
                            double val = Double.parseDouble(valueField.getText().trim());

                            if (val < slider.getMin()) slider.setMin(val - Math.abs(val * 0.5));
                            if (val > slider.getMax()) slider.setMax(val + Math.abs(val * 0.5));

                            slider.setValue(val);
                        } catch (NumberFormatException ex) {
                            valueField.setText(String.format("%.2f", slider.getValue()));
                        }
                    });

                    valueField.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
                        if (!isNowFocused) {
                            valueField.fireEvent(new javafx.event.ActionEvent());
                        }
                    });
                    HBox sRow = new HBox(8, sLabel, valueField, slider);
                    sRow.setAlignment(Pos.CENTER_LEFT);
                    sliderBox.getChildren().add(sRow);
                }
            }
        });

        return input;
    }

    private void recalculateRowNumbers() {
        for (int i = 0; i < functionBox.getChildren().size(); i++) {
            Node cellNode = functionBox.getChildren().get(i);
            if (cellNode instanceof VBox) {
                HBox topRow = (HBox) ((VBox) cellNode).getChildren().get(0);
                Label numLabel = (Label) topRow.getChildren().get(0);
                numLabel.setText(String.valueOf(i + 1));
            }
        }
    }

    public List<TextField> getTextFields() { return textFields; }
}