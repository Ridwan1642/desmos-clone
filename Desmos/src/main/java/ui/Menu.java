package ui;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

import javafx.geometry.Pos;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.control.Slider;
import javafx.scene.paint.Color;
import javafx.scene.control.Button;
import math.GraphFunction;
import javafx.scene.shape.Rectangle;

public class Menu extends VBox {
    private VBox functionBox = new VBox(10);
    private Label error;
    private Label label;
    private List<TextField> textFields = new ArrayList<>();
    private Button addButton;
    private List<Button> hideButtons = new ArrayList<>();
    private ArrayList<Color> colorPalette = new ArrayList<>(List.of(Color.RED, Color.BLUE, Color.GREEN, Color.BLACK, Color.PURPLE));
    private int nextColor = 0;
    private BiConsumer<TextField, Color> onSubmit;
    private GraphCanvas canvas;
    private boolean isDarkMode = false;


    public Menu(BiConsumer<TextField, Color> onSubmit, GraphCanvas canvas) {
        this.setSpacing(10);
        this.setPadding(new Insets(5, 5, 5, 5));
        this.setPrefWidth(300);
        this.setMaxWidth(300);

        setLabel();
        error = new Label();
        this.onSubmit = onSubmit;
        this.canvas = canvas;

        setTextField();

        HBox buttonLayout = createActionButtons();

        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setContent(functionBox);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-border-color: transparent;");
        VBox.setVgrow(scrollPane,Priority.ALWAYS);

        this.getChildren().addAll(label, scrollPane, error, buttonLayout);
    }

    private Color getThemeColor(Color originalColor) {
        if (!isDarkMode) return originalColor;
        if (originalColor.equals(Color.BLACK)) return Color.WHITE;

        return originalColor.deriveColor(0, 0.8, 0.8, 1.0);
    }

    public void setDarkMode(boolean dark) {
        this.isDarkMode = dark;

        if (dark) {
            this.setStyle("-fx-background-color: #404040;"); // A lighter, softer dark grey
            label.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #e0e0e0;");
        } else {
            this.setStyle("-fx-background-color: transparent;");
            label.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #000000;");
        }

        String fieldBg = dark ? "black" : "white";
        String textFill = dark ? "white" : "black";
        String border = dark ? "-fx-border-color: #555; -fx-border-radius: 3;" : "-fx-border-color: #ccc; -fx-border-radius: 3;";
        String baseStyle = "-fx-font-size: 18px; -fx-background-color: " + fieldBg + "; -fx-text-fill: " + textFill + "; " + border;
        for (TextField tf : textFields) {
            tf.setStyle(baseStyle);
            Color originalColor = (Color) tf.getProperties().get("originalColor");
            Rectangle colorBox = (Rectangle) tf.getProperties().get("colorBox");

            if (originalColor != null) {
                Color newColor =getThemeColor(originalColor);

                Object data = tf.getUserData();
                if (data instanceof GraphFunction) {
                    GraphFunction func = (GraphFunction) data;
                    func.setColor(newColor);
                    if (func.isVisible()) {
                        colorBox.setFill(newColor);
                    }
                } else {
                    colorBox.setFill(newColor);
                }
            }
        }
        updateSliderStyles();
        canvas.setDarkMode(dark);
    }

    private void updateSliderStyles() {
        String textFill = isDarkMode ? "white" : "black";
        String fieldBg = isDarkMode ? "black" : "white";
        String borderColor = isDarkMode ? "#555" : "#777";

        for (Node cellNode : functionBox.getChildren()) {
            if (cellNode instanceof VBox) {
                VBox cellBox = (VBox) cellNode;
                if (cellBox.getChildren().size() > 1 && cellBox.getChildren().get(1) instanceof VBox) {
                    VBox sliderBox = (VBox) cellBox.getChildren().get(1);
                    for (Node rowNode : sliderBox.getChildren()) {
                        if (rowNode instanceof HBox) {
                            HBox sRow = (HBox) rowNode;
                            for (Node node : sRow.getChildren()) {
                                if (node instanceof Label) {
                                    node.setStyle("-fx-text-fill: " + textFill + ";");
                                } else if (node instanceof TextField) {
                                    node.setStyle("-fx-text-fill: " + textFill + "; -fx-background-color: " + fieldBg + "; -fx-border-color: " + borderColor + "; -fx-border-radius: 3;");                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public Color nextColor() {
        Color color = colorPalette.get(nextColor);
        nextColor = (nextColor + 1) % colorPalette.size();
        return color;
    }

    public void setLabel() {
        label = new Label();
        label.setText("Enter function: ");
        label.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: black;");
        label.setMaxWidth(Double.MAX_VALUE);
        label.setAlignment(Pos.CENTER);

    }

    private HBox createActionButtons() {
        addButton = new Button("Add Function");
        addButton.setPrefWidth(200);
        addButton.setStyle("-fx-font-size: 16px; -fx-cursor: hand; -fx-font-weight: bold;");

        addButton.setOnAction(e -> {
            TextField newField = setTextField();
            newField.requestFocus(); // Auto-focus the new box when clicked
        });

        HBox buttonBox = new HBox(addButton);
        buttonBox.setAlignment(Pos.CENTER);
        return buttonBox;
    }


    public void seterrLabel(String message) {
        error.setText(message);
        error.setStyle("-fx-text-fill: red; -fx-font-size: 14px;");
        error.setAlignment(Pos.BOTTOM_CENTER);
    }

    public TextField setTextFieldAvoiding(Color avoidColor) {
        Color upcomingColor = colorPalette.get(nextColor);
        Color upcomingDisplayColor = getThemeColor(upcomingColor);
        if (upcomingDisplayColor.equals(avoidColor)) {
            nextColor = (nextColor + 1) % colorPalette.size(); // Skip this color
        }
        return setTextField();
    }

    public TextField setTextField() {
        Color color = nextColor();
        Color displayColor = getThemeColor(color);
        String fieldBg = isDarkMode ? "black" : "white";
        String textFill = isDarkMode ? "white" : "black";
        String border = isDarkMode ? "-fx-border-color: #555; -fx-border-radius: 3;" : "-fx-border-color: #ccc; -fx-border-radius: 3;";
        TextField input = new TextField();
        input.setPrefWidth(200);
        input.setStyle("-fx-font-size: 18px; -fx-background-color: " + fieldBg + "; -fx-text-fill: " + textFill + "; " + border);


        Rectangle colorBox = new Rectangle(30, 30, displayColor);
        colorBox.setStroke(Color.BLACK);
        colorBox.setArcWidth(5);
        colorBox.setArcHeight(5);
        colorBox.setCursor(javafx.scene.Cursor.HAND);

        input.getProperties().put("originalColor", color);
        input.getProperties().put("colorBox", colorBox);

        Button colorButton = new Button();
        colorButton.setGraphic(colorBox);
        colorButton.setStyle("-fx-background-color: transparent; -fx-padding: 2; -fx-cursor: hand;");


        Button removeButton = new Button("X");
        removeButton.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
        removeButton.setPrefWidth(40);

        VBox cellBox = new VBox(5);
        HBox row = new HBox(5, input, colorButton, removeButton);
        VBox sliderBox = new VBox(5);
        sliderBox.setPadding(new Insets(0, 0, 0, 10));

        cellBox.getChildren().addAll(row, sliderBox);
        functionBox.getChildren().add(cellBox);
        textFields.add(input);

        boolean[] isVisible = {true};

        colorButton.setOnAction(e -> {
            isVisible[0] = !isVisible[0];
            Color activeColor = getThemeColor(color);
            colorBox.setFill(isVisible[0] ? activeColor : Color.WHITE);

            Object data = input.getUserData();
            if (data instanceof GraphFunction) {
                GraphFunction func = (GraphFunction) data;
                func.setVisible(isVisible[0]);
                canvas.redraw();
            }
        });
        removeButton.setOnAction(e -> {
            Object data = input.getUserData();
            if (data instanceof GraphFunction)
                canvas.removeFunction((GraphFunction) data);

            functionBox.getChildren().remove(cellBox);
            textFields.remove(input);
            canvas.redraw();
        });

        input.textProperty().addListener((obs, oldText, newText) -> {
                seterrLabel("");
        });

        //final GraphFunction[] functionHolder = new GraphFunction[1];

        input.setOnAction(e -> {
            if (onSubmit != null) {
                Color activeColor = getThemeColor(color);
                onSubmit.accept(input, activeColor);
            }
            if (textFields.indexOf(input) == textFields.size() - 1 && !input.getText().trim().isEmpty()) {
                TextField nextInput = setTextField();
                nextInput.requestFocus();
            }

            sliderBox.getChildren().clear();
            Object data = input.getUserData();


            if (data instanceof GraphFunction) {
                GraphFunction func = (GraphFunction) data;


                for (org.mariuszgromada.math.mxparser.Argument arg : func.getParameters()) {
                    Slider slider = new Slider(-10, 10, 1);
                    javafx.scene.layout.HBox.setHgrow(slider, Priority.ALWAYS);
                    slider.setShowTickMarks(true);

                    Label sLabel = new Label(arg.getArgumentName() + " = ");
                    TextField valueField = new TextField("1.00");
                    valueField.setPrefWidth(50);
                    String sTextFill = isDarkMode ? "white" : "black";
                    String sFieldBg = isDarkMode ? "black" : "white";
                    String sBorderColor = isDarkMode ? "#555" : "#777";

                    sLabel.setStyle("-fx-text-fill: " + sTextFill + ";");
                    valueField.setStyle("-fx-text-fill: " + sTextFill + "; -fx-background-color: " + sFieldBg + "; -fx-border-color: " + sBorderColor + "; -fx-border-radius: 3;");

                    slider.valueProperty().addListener((o, oldNum, newNum) -> {
                        arg.setArgumentValue(newNum.doubleValue());
                        sLabel.setText(arg.getArgumentName() + " = " + String.format("%.2f", newNum.doubleValue()));
                        canvas.redraw();
                    });

                    valueField.setOnAction(evt -> {
                        try {
                            double val = Double.parseDouble(valueField.getText());


                            if (val < slider.getMin()) slider.setMin(val);
                            if (val > slider.getMax()) slider.setMax(val);


                            slider.setValue(val);
                        } catch (NumberFormatException ex) {

                            valueField.setText(String.format("%.2f", slider.getValue()));
                        }
                    });

                    HBox sRow = new HBox(5, sLabel, valueField, slider);
                    sRow.setAlignment(Pos.CENTER_LEFT);
                    sliderBox.getChildren().add(sRow);
                }
            }
        });


        return input;
    }
    public List<TextField> getTextFields() {
        return textFields;
    }
}