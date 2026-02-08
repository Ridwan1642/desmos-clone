package com.example;

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

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) {

        // --- STEP 1: Create viewport ---
        Viewport viewport = new Viewport(-10, 10, -10, 10);

        // --- STEP 2: Create coordinate system ---
        Coordinate_System coordSystem = new Coordinate_System(viewport);

        // --- STEP 3: Create canvas ---
        GraphCanvas canvas = new GraphCanvas(coordSystem,new GridRenderer(coordSystem));
        GraphFunction f = new GraphFunction("x^2");
        canvas.setFunction(f);

        // --- STEP 4: Add grid renderer ---
        GridRenderer gridRenderer = new GridRenderer(coordSystem);
        canvas.setGridRenderer(gridRenderer);

        // --- STEP 5: Layout ---
        // --- STEP 5: Layout ---
        BorderPane root = new BorderPane();

// Wrap canvas in a Pane that handles resizing
        Pane canvasContainer = new Pane(canvas);
        canvas.widthProperty().bind(canvasContainer.widthProperty());
        canvas.heightProperty().bind(canvasContainer.heightProperty());

        root.setCenter(canvasContainer);
        Scene scene = new Scene(root, 800, 600);
        primaryStage.setTitle("Graphing App Test");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
