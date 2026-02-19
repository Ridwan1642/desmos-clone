package ui;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import javafx.geometry.Pos;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.control.Slider;
import javafx.scene.paint.Color;
import javafx.scene.control.Button;
import math.GraphFunction;
import javafx.scene.shape.Rectangle;
import org.w3c.dom.Text;

public class Menu extends VBox {
    private VBox functionBox = new VBox(10);
    private Label error;
    private Label label;
    private List<TextField> textFields = new ArrayList<>();
    private Button addButton;
    private List<Button> hideButtons = new ArrayList<>();
    private ArrayList<Color> colorPalette = new ArrayList<>(List.of(Color.RED, Color.BLUE, Color.GREEN, Color.BLACK, Color.PURPLE));
    private int nextColor=0;
    private BiConsumer<String, Color> onSubmit;
    private GraphCanvas canvas;

    //Menu area creation
    public Menu(BiConsumer<String,Color> onSubmit, GraphCanvas canvas){
        this.setSpacing(10);
        this.setPadding(new Insets(5,5,5,5));
        this.setPrefWidth(300);
        this.setMaxWidth(300);

        setLabel();
        error = new Label();
        this.onSubmit = onSubmit;
        this.canvas = canvas;
        addButton();
        setTextField();
        this.getChildren().addAll(label, functionBox, error, addButton);
    }

    //Color selection
    public Color nextColor(){
        Color color = colorPalette.get(nextColor);
        nextColor = (nextColor+1)%colorPalette.size();
        return color;
    }
    //Menu label
    public void setLabel(){
        label = new Label();
        label.setText("Enter function: ");
        label.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");
        label.setMaxWidth(Double.MAX_VALUE);
        label.setAlignment(Pos.CENTER);

    }

    //Add button creation
    public void addButton(){
        addButton = new Button();
        addButton.setText("Add");
        addButton.setPrefWidth(200);
        addButton.setStyle("-fx-font-size: 16px;");
        addButton.setOnAction( e ->{setTextField();});
    }

    //Error Message label
    public void seterrLabel(String message){
        error.setText(message);
        error.setStyle("-fx-text-fill: red; -fx-font-size: 14px;");
        error.setAlignment(Pos.BOTTOM_CENTER);
    }

    //Menu textfield
    public TextField setTextField(){
        Color color = nextColor();
        TextField input = new TextField();
        input.setPrefWidth(200);
        input.setStyle("-fx-font-size: 18px;");
        //Color box
        Rectangle colorBox = new Rectangle(30, 30, color);
        colorBox.setStroke(Color.BLACK);
        colorBox.setArcWidth(5);
        colorBox.setArcHeight(5);
        colorBox.setCursor(javafx.scene.Cursor.HAND);

        Button colorButton = new Button();
        colorButton.setGraphic(colorBox);
        colorButton.setStyle("-fx-background-color: transparent; -fx-padding: 2; -fx-cursor: hand;");

        //Remove button
        Button removeButton = new Button("X");
        removeButton.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
        removeButton.setPrefWidth(40);

        VBox cellBox = new VBox(5);
        HBox row = new HBox(5, input, colorButton, removeButton);
        VBox sliderBox = new VBox(5);
        sliderBox.setPadding(new Insets(0, 0, 0, 10));

        cellBox.getChildren().addAll(row, sliderBox);
        functionBox.getChildren().add(cellBox); // Add the parent cellBox
        textFields.add(input);

        boolean[] isVisible = {true};

        colorButton.setOnAction(e->{
            isVisible[0] = !isVisible[0];
            colorBox.setFill(isVisible[0] ? color : Color.WHITE);

            Object data = input.getUserData();
            if(data instanceof GraphFunction){
                GraphFunction func = (GraphFunction) data;
                func.setVisible(isVisible[0]);
                canvas.redraw();
            }
        });
        removeButton.setOnAction(e->{
            Object data = input.getUserData();
            if(data instanceof GraphFunction)
                canvas.removeFunction((GraphFunction) data);

            functionBox.getChildren().remove(cellBox);
            textFields.remove(input);
            canvas.redraw();
        });

        input.textProperty().addListener((obs, oldText, newText) -> {
            if (newText.contains("y")) {
                seterrLabel("Implicit function not allowed");
            } else {
               seterrLabel("");
            }
        });

        final GraphFunction[] functionHolder = new GraphFunction[1];

        input.setOnAction(e -> {
            if (onSubmit != null) {
                onSubmit.accept(input.getText(), color);
            }

            sliderBox.getChildren().clear();
            Object data = input.getUserData();

            if (data instanceof GraphFunction) {
                GraphFunction func = (GraphFunction) data;

                // 3. Loop through parameters to build sliders
                for (org.mariuszgromada.math.mxparser.Argument arg : func.getParameters()) {
                    Slider slider = new Slider(-10, 10, 1);
                    javafx.scene.layout.HBox.setHgrow(slider, Priority.ALWAYS);
                    slider.setShowTickMarks(true);

                    Label sLabel = new Label(arg.getArgumentName() + " = ");
                    TextField valueField = new TextField("1.00");
                    valueField.setPrefWidth(50);

                    slider.valueProperty().addListener((o, oldNum, newNum) -> {
                        arg.setArgumentValue(newNum.doubleValue());
                        sLabel.setText(arg.getArgumentName() + " = " + String.format("%.2f", newNum.doubleValue()));
                        canvas.redraw();
                    });

                    valueField.setOnAction(evt -> {
                        try {
                            double val = Double.parseDouble(valueField.getText());

                            // Dynamically expand the slider bounds if they type a huge/tiny number
                            if (val < slider.getMin()) slider.setMin(val);
                            if (val > slider.getMax()) slider.setMax(val);

                            // This automatically triggers the slider listener above to redraw the canvas!
                            slider.setValue(val);
                        } catch (NumberFormatException ex) {
                            // If they type letters or gibberish, reset it back to the safe slider value
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

    public List<TextField> getTextFields(){
        return textFields;
    }
}
