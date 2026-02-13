package ui;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import javafx.geometry.Pos;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.control.Button;
import math.GraphFunction;
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

        // Hide button
        Button hideButton = new Button("Hide");
        hideButton.setPrefWidth(60);

        javafx.scene.layout.HBox row = new javafx.scene.layout.HBox(5, input, hideButton);
        functionBox.getChildren().add(row);
        textFields.add(input);

        hideButton.setOnAction(e -> {
            Object data = input.getUserData();
            if (data instanceof GraphFunction) {
                GraphFunction func = (GraphFunction) data;
                func.setVisible(!func.isVisible());
                hideButton.setText(func.isVisible() ? "Hide" : "Show");

                canvas.redraw();
            }
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
        });


        return input;
    }

    public List<TextField> getTextFields(){
        return textFields;
    }
}
