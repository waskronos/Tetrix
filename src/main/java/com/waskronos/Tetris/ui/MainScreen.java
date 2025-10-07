package com.waskronos.Tetris.ui;

import com.waskronos.Tetris.app.TetrisApp;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;

public class MainScreen extends BorderPane {
    // This screen is the entry point
    // This follows single responsibility

    private final TetrisApp app;

    public MainScreen(TetrisApp app){
        this.app = app;

        Label titleLabel = new Label("TETRIX");
        titleLabel.getStyleClass().add("title");
        setTop(titleLabel);
        BorderPane.setAlignment(titleLabel, Pos.CENTER);

        Button playButton = makeButton("Play");
        Button configButton = makeButton("Configure");
        Button highScoresButton = makeButton("High Scores");
        Button exitButton = makeButton("Exit");

        playButton.setOnAction(e -> app.showModeSelectScreen());
        configButton.setOnAction(e -> app.showConfiguration());
        highScoresButton.setOnAction(e -> app.showHighScoresScreen());
        exitButton.setOnAction(e -> app.exitApplication());

        VBox buttonBox = new VBox(20, playButton, configButton, highScoresButton, exitButton);
        buttonBox.setAlignment(Pos.CENTER);
        setCenter(buttonBox);
    }

    private Button makeButton(String text){
        Button b = new Button(text);
        b.getStyleClass().add("big");
        return b;
    }
}