package com.example;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.*;
import model.Coordinate_System;
import model.Viewport;
import rendering.GridRenderer;
import ui.GraphCanvas;
import math.GraphFunction;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import ui.IntegralCalculator;
import ui.Menu;
import ui.SlopeCalculator;
import ui.BestFitDialog;
import javafx.event.ActionEvent;

public class Main extends Application {
    private Menu menu;
    private boolean menuVisible = true;
    private boolean isDarkMode = false; // Global theme tracker

    @Override
    public void start(Stage primaryStage) {
        Viewport viewport = new Viewport(-10, 10, -10, 10);
        Coordinate_System coordSystem = new Coordinate_System(viewport);

        GridRenderer gridRenderer = new GridRenderer(coordSystem);
        GraphCanvas canvas = new GraphCanvas(coordSystem, gridRenderer);

        BorderPane root = new BorderPane();
        AnchorPane canvasContainer = new AnchorPane(canvas);
        canvasContainer.setMinWidth(0);
        canvasContainer.setMinHeight(0);
        canvas.widthProperty().bind(canvasContainer.widthProperty());
        canvas.heightProperty().bind(canvasContainer.heightProperty());

        Button homeButton = new Button("🏡");
        homeButton.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-font-size: 30px; -fx-padding: 5 10 5 10;");
        AnchorPane.setTopAnchor(homeButton, 15.0);
        AnchorPane.setRightAnchor(homeButton, 15.0);
        homeButton.setOnAction(e -> {
            viewport.reset();
            canvas.redraw();
        });
        canvasContainer.getChildren().add(homeButton);

        menu = new Menu((inputField, color) -> {
            try {
                int index = menu.getTextFields().indexOf(inputField);

                if (index != -1) {
                    GraphFunction function = new GraphFunction(inputField.getText().trim(), color);
                    inputField.setUserData(function);
                    canvas.setFunction(index, function);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }, canvas);

        // --- TRUE POPUP WINDOWS ---
        IntegralCalculator integralCalculator = new IntegralCalculator(menu, canvas);
        Stage integralStage = new Stage();
        integralStage.setTitle("Area Calculator");
        integralStage.initStyle(StageStyle.UTILITY);
        integralStage.initOwner(primaryStage);
        integralStage.setScene(new Scene(integralCalculator));
        integralStage.setResizable(false);

        SlopeCalculator slopeCalculator = new SlopeCalculator(menu);
        Stage slopeStage = new Stage();
        slopeStage.setTitle("Tangent Calculator");
        slopeStage.initStyle(StageStyle.UTILITY);
        slopeStage.initOwner(primaryStage);
        slopeStage.setScene(new Scene(slopeCalculator));
        slopeStage.setResizable(false);

        // --- GLOBAL TOOLBAR ---
        HBox toolBar = new HBox(15);
        toolBar.setAlignment(Pos.CENTER_LEFT);
        toolBar.setStyle("-fx-background-color: #2b2b2b; -fx-padding: 10 20 10 20; -fx-border-color: #444; -fx-border-width: 0 0 1 0;");

        // 1. Menu Toggle Button
        Button toggleMenuButton = new Button("☰ Menu");
        // 2. THEME TOGGLE BUTTON
        Button themeBtn = new Button("🌙 Dark Mode");

        String topBtnIdle = "-fx-background-color: #333; -fx-text-fill: #e0e0e0; -fx-cursor: hand; -fx-font-size: 14px; -fx-font-weight: bold;";
        String topBtnHover = "-fx-background-color: #505050; -fx-text-fill: #e0e0e0; -fx-cursor: hand; -fx-font-size: 14px; -fx-font-weight: bold;";

        for (Button btn : new Button[]{toggleMenuButton, themeBtn}) {
            btn.setStyle(topBtnIdle);
            btn.setOnMouseEntered(e -> btn.setStyle(topBtnHover));
            btn.setOnMouseExited(e -> btn.setStyle(topBtnIdle));
        }

        toggleMenuButton.setOnAction(e -> {
            if (menuVisible) {
                root.setLeft(null);
                toggleMenuButton.setText("☰ Menu");
            } else {
                root.setLeft(menu);
                toggleMenuButton.setText("Hide Menu");
            }
            menuVisible = !menuVisible;
        });

        // --- THEME LOGIC ---
        Scene scene = new Scene(root, 800, 600); // Created early so we can apply styles to it

        themeBtn.setOnAction(e -> {
            isDarkMode = !isDarkMode;

            try {
                String css = getClass().getResource("/dark-theme.css").toExternalForm();

                if (isDarkMode) {
                    scene.getStylesheets().add(css);
                    integralStage.getScene().getStylesheets().add(css);
                    slopeStage.getScene().getStylesheets().add(css);
                    themeBtn.setText("☀️ Light Mode");
                } else {
                    scene.getStylesheets().clear();
                    integralStage.getScene().getStylesheets().clear();
                    slopeStage.getScene().getStylesheets().clear();
                    themeBtn.setText("🌙 Dark Mode");
                }
            } catch (NullPointerException ex) {
                System.out.println("Could not find dark-theme.css in the resources folder!");
            }

            // Tell the math canvas to invert colors
            canvas.setDarkMode(isDarkMode);
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // 3. Math Tool Buttons
        Button bestFitBtn = new Button("📈 Best Fit");
        Button slopeBtn = new Button("📐 Tangent");
        Button integralBtn = new Button("∫ Area");

        HBox mathTools = new HBox(10, bestFitBtn, slopeBtn, integralBtn);
        mathTools.setAlignment(Pos.CENTER_RIGHT);

        String toolBtnIdle = "-fx-background-color: #333; -fx-text-fill: #e0e0e0; -fx-cursor: hand; -fx-font-size: 14px;";
        String toolBtnHover = "-fx-background-color: #505050; -fx-text-fill: #e0e0e0; -fx-cursor: hand; -fx-font-size: 14px;";

        for (Button btn : new Button[]{bestFitBtn, slopeBtn, integralBtn}) {
            btn.setStyle(toolBtnIdle);
            btn.setOnMouseEntered(e -> btn.setStyle(toolBtnHover));
            btn.setOnMouseExited(e -> btn.setStyle(toolBtnIdle));
        }

        bestFitBtn.setOnAction(e -> {
            BestFitDialog dialog = new BestFitDialog();

            // Inject dark mode into the dialog if active!
            if (isDarkMode) {
                dialog.getDialogPane().getStylesheets().add(getClass().getResource("/dark-theme.css").toExternalForm());
            }

            dialog.showAndWait().ifPresent(equation -> {
                if (equation != null && !equation.isEmpty()) {
                    TextField newRow = menu.setTextField();
                    newRow.setText(equation);
                    newRow.fireEvent(new ActionEvent(ActionEvent.ACTION, newRow));
                }
            });
        });

        slopeBtn.setOnAction(e -> {
            if (slopeStage.isShowing()) slopeStage.toFront();
            else slopeStage.show();
        });

        integralBtn.setOnAction(e -> {
            if (integralStage.isShowing()) integralStage.toFront();
            else integralStage.show();
        });

        toolBar.getChildren().addAll(toggleMenuButton, themeBtn, spacer, mathTools);
        root.setTop(toolBar);

        root.setCenter(canvasContainer);
        root.setLeft(menu);

        primaryStage.setTitle("Graphing App Test");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}