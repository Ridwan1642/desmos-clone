package com.example;


import javafx.scene.control.CheckBox;
import javafx.scene.paint.Color;
import model.Coordinate_System;
import model.Viewport;
import rendering.GridRenderer;
import ui.GraphCanvas;
import math.GraphFunction;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import javafx.scene.layout.Pane;
import ui.Menu;

public class Main extends Application {
    private Menu menu;
    @Override
    public void start(Stage primaryStage) {

        // --- STEP 1: Create viewport ---
        Viewport viewport = new Viewport(-10, 10, -10, 10);

        // --- STEP 2: Create coordinate system ---
        Coordinate_System coordSystem = new Coordinate_System(viewport);

        // --- STEP 3: Create canvas ---
        GraphCanvas canvas = new GraphCanvas(coordSystem,new GridRenderer(coordSystem));

        // --- STEP 4: Add grid renderer ---
        GridRenderer gridRenderer = new GridRenderer(coordSystem);


        // --- STEP 5: Layout ---
        // --- STEP 5: Layout ---
        BorderPane root = new BorderPane();

// Wrap canvas in a Pane that handles resizing
        Pane canvasContainer = new Pane(canvas);
        canvas.widthProperty().bind(canvasContainer.widthProperty());
        canvas.heightProperty().bind(canvasContainer.heightProperty());


         menu = new Menu((inputText, color) -> {
            try {
                for (int i = 0; i < menu.getTextFields().size(); i++) {
                    javafx.scene.control.TextField tf = menu.getTextFields().get(i);

                    if (tf.isFocused()) {

                        GraphFunction function = new GraphFunction(inputText.trim(), color);

                        // Store SAME object for hide button
                        tf.setUserData(function);

                        // Update correct index in renderer
                        canvas.setFunction(i, function);

                        break;
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }, canvas);



        root.setCenter(canvasContainer);
        root.setLeft(menu);
        Scene scene = new Scene(root, 800, 600);
        primaryStage.setTitle("Graphing App Test");
        primaryStage.setScene(scene);
        primaryStage.show();

    }

    public static void main(String[] args) {
        launch(args);
    }
}
