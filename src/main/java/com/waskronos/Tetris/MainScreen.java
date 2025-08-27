package com.waskronos.Tetris;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

public class MainScreen extends BorderPane {
    private TetrisApp app;
    public MainScreen(TetrisApp app){
        this.app = app;
        Label titleLabel = new Label("TETRIX");
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 47));
        setTop(titleLabel);
        BorderPane.setAlignment(titleLabel, Pos.CENTER);

        Button playButton = createButton("Play");
        Button configButton = createButton("Configure");
        Button exitButton = createButton("Exit");

        configButton.setOnAction(e -> app.showConfiguration());
        exitButton.setOnAction(e -> app.exitApplication());
        playButton.setOnAction(e -> app.showGameScreen());

        VBox buttonBox = new VBox(20);
        buttonBox.setAlignment(Pos.CENTER);
        buttonBox.getChildren().addAll(playButton, configButton,exitButton);

        setCenter(buttonBox);
    }

    private Button createButton(String heading){
        Button button = new Button(heading);
        button.setFont(Font.font("Arial", FontWeight.BOLD, 20));
        button.setPrefSize(200, 50);
        return button;
    }
}
