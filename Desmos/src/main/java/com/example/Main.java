package com.example;


import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import model.Coordinate_System;
import model.Viewport;
import org.w3c.dom.Text;
import rendering.GridRenderer;
import ui.GraphCanvas;
import math.GraphFunction;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import ui.Menu;
import ui.SlopeCalculator;

public class Main extends Application {
    private Menu menu;
    private boolean menuVisible = true;
    @Override
    public void start(Stage primaryStage) {

        // Create viewport ---
        Viewport viewport = new Viewport(-10, 10, -10, 10);

        //Create coordinate system ---
        Coordinate_System coordSystem = new Coordinate_System(viewport);

        //Create canvas ---
        GraphCanvas canvas = new GraphCanvas(coordSystem,new GridRenderer(coordSystem));

        //Add grid renderer ---
        GridRenderer gridRenderer = new GridRenderer(coordSystem);


        // Layout ---
        BorderPane root = new BorderPane();

        // AnchorPane---
        AnchorPane canvasContainer = new AnchorPane(canvas);
        canvasContainer.setMinWidth(0);
        canvasContainer.setMinHeight(0);
        canvas.widthProperty().bind(canvasContainer.widthProperty());
        canvas.heightProperty().bind(canvasContainer.heightProperty());

         //floating Home button
        Button homeButton = new Button("🏡");
        homeButton.setStyle("-fx-background-color: white; " +
                "-fx-border-color: #ccc; " +
                "-fx-border-radius: 5; " +
                "-fx-background-radius: 5; " +
                "-fx-cursor: hand; " +
                "-fx-font-size: 30px; " +
                "-fx-padding: 5 10 5 10;");
        AnchorPane.setTopAnchor(homeButton,15.0);
        AnchorPane.setRightAnchor(homeButton, 15.0);

        homeButton.setOnAction(e->{
            viewport.reset();
            canvas.redraw();
        });

        canvasContainer.getChildren().add(homeButton);


        menu = new Menu((inputText, color) -> {
            try {
                for (int i = 0; i < menu.getTextFields().size(); i++) {
                    javafx.scene.control.TextField tf = menu.getTextFields().get(i);

                    // FIX: Check if the text matches, ignoring focus entirely!
                    if (tf.getText().trim().equals(inputText.trim())) {

                        GraphFunction function = new GraphFunction(inputText.trim(), color);

                        // Store the object securely
                        tf.setUserData(function);

                        // Update the canvas
                        canvas.setFunction(i, function);

                        break;
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }, canvas);

        //Slope section
        SlopeCalculator SlopeCalculator = new SlopeCalculator(menu);

        AnchorPane.setBottomAnchor(SlopeCalculator, 15.0);
        AnchorPane.setRightAnchor(SlopeCalculator, 15.0);

        canvasContainer.getChildren().add(SlopeCalculator);

        // hide/show menu button ---
        Button toggleMenuButton = new Button("Hide Menu");
        toggleMenuButton.setOnAction(e -> {
            if (menuVisible) {
                root.setLeft(null);           // Hide menu
                toggleMenuButton.setText("Show Menu");
            } else {
                root.setLeft(menu);           // Show menu
                toggleMenuButton.setText("Hide Menu");
            }
            menuVisible = !menuVisible;
        });

        HBox topBar = new HBox(toggleMenuButton);
        topBar.setSpacing(10);
        topBar.setStyle("-fx-padding: 5; -fx-background-color: #eee;"); // optional styling
        root.setTop(topBar);

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
