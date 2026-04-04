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
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import ui.IntegralCalculator;
import ui.Menu;
import ui.SlopeCalculator;
import ui.BestFitDialog;
import javafx.event.ActionEvent;
import javafx.animation.*;
import javafx.scene.shape.*;
import javafx.scene.text.*;
import javafx.scene.effect.DropShadow;
import javafx.util.Duration;

public class Main extends Application {
    private Menu menu;
    private boolean menuVisible = true;
    private boolean isDarkMode = false;

    @Override
    public void start(Stage primaryStage) {
        Viewport viewport = new Viewport(-10, 10, -10, 10);
        Coordinate_System coordSystem = new Coordinate_System(viewport);

        GridRenderer gridRenderer = new GridRenderer(coordSystem);
        GraphCanvas canvas = new GraphCanvas(coordSystem, gridRenderer);

        Canvas overlayCanvas = new Canvas();
        overlayCanvas.setMouseTransparent(true);

        Canvas mathCanvas = new Canvas();
        mathCanvas.setMouseTransparent(true);
        canvas.setCanvases(mathCanvas, overlayCanvas);

        StackPane canvasLayers = new StackPane(canvas, mathCanvas, overlayCanvas);

        canvas.widthProperty().bind(canvasLayers.widthProperty());
        canvas.heightProperty().bind(canvasLayers.heightProperty());
        mathCanvas.widthProperty().bind(canvasLayers.widthProperty());
        mathCanvas.heightProperty().bind(canvasLayers.heightProperty());
        overlayCanvas.widthProperty().bind(canvasLayers.widthProperty());
        overlayCanvas.heightProperty().bind(canvasLayers.heightProperty());

        AnchorPane canvasContainer = new AnchorPane(canvasLayers);
        canvasContainer.setMinWidth(0);
        canvasContainer.setMinHeight(0);
        canvasLayers.prefWidthProperty().bind(canvasContainer.widthProperty());
        canvasLayers.prefHeightProperty().bind(canvasContainer.heightProperty());

        BorderPane root = new BorderPane();

        Button homeButton = new Button("🏡");
        homeButton.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-font-size: 30px; -fx-padding: 5 10 5 10;");
        AnchorPane.setTopAnchor(homeButton, 15.0);
        AnchorPane.setRightAnchor(homeButton, 15.0);

        homeButton.setOnAction(e -> {
            viewport.reset();
            coordSystem.enforceAspectRatio(canvas.getWidth(), canvas.getHeight());
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

        IntegralCalculator integralCalculator = new IntegralCalculator(menu, canvas);
        Stage integralStage = new Stage();
        integralStage.setTitle("Area Calculator");
        integralStage.initStyle(StageStyle.UTILITY);
        integralStage.initOwner(primaryStage);
        integralStage.setScene(new Scene(integralCalculator));
        integralStage.setResizable(false);

        SlopeCalculator slopeCalculator = new SlopeCalculator(menu, canvas);
        Stage slopeStage = new Stage();
        slopeStage.setTitle("Tangent Calculator");
        slopeStage.initStyle(StageStyle.UTILITY);
        slopeStage.initOwner(primaryStage);
        slopeStage.setScene(new Scene(slopeCalculator));
        slopeStage.setResizable(false);

        HBox toolBar = new HBox(15);
        toolBar.setAlignment(Pos.CENTER_LEFT);
        toolBar.setStyle("-fx-background-color: #2b2b2b; -fx-padding: 10 20 10 20; -fx-border-color: #444; -fx-border-width: 0 0 1 0;");

        Button toggleMenuButton = new Button("☰ Menu");
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

        themeBtn.setOnAction(e -> {
            isDarkMode = !isDarkMode;

            try {
                String css = getClass().getResource("/dark-theme.css").toExternalForm();

                if (isDarkMode) {
                    root.getScene().getStylesheets().add(css);
                    integralStage.getScene().getStylesheets().add(css);
                    slopeStage.getScene().getStylesheets().add(css);
                    themeBtn.setText("☀️ Light Mode");
                } else {
                    root.getScene().getStylesheets().clear();
                    integralStage.getScene().getStylesheets().clear();
                    slopeStage.getScene().getStylesheets().clear();
                    themeBtn.setText("🌙 Dark Mode");
                }
            } catch (NullPointerException ex) {
                System.out.println("Could not find dark-theme.css in the resources folder!");
            }

            canvas.setDarkMode(isDarkMode);
            menu.setDarkMode(isDarkMode);
            slopeCalculator.setDarkMode(isDarkMode);
            integralCalculator.setDarkMode(isDarkMode);
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

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
            BestFitDialog dialog = new BestFitDialog(isDarkMode);
            if (isDarkMode) {
                try {
                    dialog.getDialogPane().getStylesheets().add(getClass().getResource("/dark-theme.css").toExternalForm());
                } catch (Exception ex) { }
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

        StackPane superRoot = new StackPane(root);

        StackPane splashScreen = buildSplashScreen(superRoot);
        superRoot.getChildren().add(splashScreen);

        Scene scene = new Scene(superRoot, 800, 600);
        primaryStage.setTitle("Graphing App Test");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private StackPane buildSplashScreen(StackPane superRoot) {
        StackPane splash = new StackPane();
        splash.setStyle("-fx-background-color: #121212;");

        Path mathPath = new Path();
        mathPath.setStroke(Color.web("#00d2ff"));
        mathPath.setStrokeWidth(4);
        mathPath.setStrokeLineCap(StrokeLineCap.ROUND);
        mathPath.setStrokeLineJoin(StrokeLineJoin.ROUND);

        DropShadow glow = new DropShadow();
        glow.setColor(Color.web("#00d2ff"));
        glow.setRadius(15);
        glow.setSpread(0.2);
        mathPath.setEffect(glow);

        mathPath.getElements().add(new MoveTo(0, 300));
        for (int i = 1; i <= 800; i++) {
            double amplitude = 150 * Math.exp(-i * 0.003);
            double y = 300 + Math.sin(i * 0.05) * amplitude;
            mathPath.getElements().add(new LineTo(i, y));
        }

        double pathLength = 2000;
        mathPath.getStrokeDashArray().setAll(pathLength);
        mathPath.setStrokeDashOffset(pathLength);

        Text title = new Text("DESMOS ++");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 50));
        title.setFill(Color.WHITE);

        Text subtitle = new Text("Interactive Math Engine");
        subtitle.setFont(Font.font("Segoe UI", FontWeight.LIGHT, 20));
        subtitle.setFill(Color.web("#aaaaaa"));

        VBox titleBox = new VBox(5, title, subtitle);
        titleBox.setAlignment(Pos.CENTER);
        titleBox.setOpacity(0);

        Pane pathContainer = new Pane(mathPath);
        splash.getChildren().addAll(pathContainer, titleBox);

        Timeline drawCurve = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(mathPath.strokeDashOffsetProperty(), pathLength)),
                new KeyFrame(Duration.seconds(2.5), new KeyValue(mathPath.strokeDashOffsetProperty(), 0, Interpolator.EASE_OUT))
        );

        FadeTransition textFade = new FadeTransition(Duration.seconds(1.5), titleBox);
        textFade.setFromValue(0);
        textFade.setToValue(1);

        ScaleTransition textScale = new ScaleTransition(Duration.seconds(1.5), titleBox);
        textScale.setFromX(0.9);
        textScale.setFromY(0.9);
        textScale.setToX(1);
        textScale.setToY(1);

        ParallelTransition textAnim = new ParallelTransition(textFade, textScale);

        FadeTransition splashFadeOut = new FadeTransition(Duration.seconds(1), splash);
        splashFadeOut.setFromValue(1);
        splashFadeOut.setToValue(0);

        SequentialTransition sequence = new SequentialTransition(
                drawCurve,
                textAnim,
                new PauseTransition(Duration.seconds(0.8)),
                splashFadeOut
        );

        sequence.setOnFinished(e -> superRoot.getChildren().remove(splash));
        sequence.play();

        return splash;
    }

    public static void main(String[] args) {
        launch(args);
    }
}