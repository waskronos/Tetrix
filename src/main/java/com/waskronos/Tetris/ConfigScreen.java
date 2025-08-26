package com.waskronos.Tetris;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

public class ConfigScreen extends BorderPane {
    private TetrisApp app;

    public ConfigScreen(TetrisApp app) {
        this.app = app;

        Label titleLabel = new Label("Configuration");
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 36));
        setTop(titleLabel);
        BorderPane.setAlignment(titleLabel, Pos.CENTER);

        GridPane configGrid = new GridPane();
        configGrid.setHgap(10);
        configGrid.setVgap(20);
        configGrid.setPadding(new Insets(20));
        configGrid.setAlignment(Pos.CENTER);

        configGrid.add(new Label("Difficulty"), 0, 0);
        ComboBox<String> difficultyCombo = new ComboBox<>();
        difficultyCombo.getItems().addAll("Easy", "Medium", "Hard");
        difficultyCombo.setValue("Medium");
        configGrid.add(difficultyCombo, 1, 0);

        configGrid.add(new Label("Music"), 0, 1);
        CheckBox musicCheckbox = new CheckBox("Enable Music");
        configGrid.add(musicCheckbox, 1, 2);

        configGrid.add(new Label("Speed"), 0, 2);
        Slider speedSlider = new Slider(1, 10, 1);
        speedSlider.setShowTickLabels(true);
        speedSlider.setShowTickMarks(true);
        speedSlider.setMajorTickUnit(1);
        speedSlider.setMinorTickCount(0);
        speedSlider.setBlockIncrement(1);
        speedSlider.setSnapToTicks(true);
        configGrid.add(speedSlider, 1, 1);

        configGrid.add(new Label("Sound effects"), 0, 3);
        CheckBox sfxCheckbox = new CheckBox("Enable Sound Effects");
        configGrid.add(sfxCheckbox, 1, 3);

        setCenter(configGrid);

        HBox buttonBox = new HBox(20);
        buttonBox.setAlignment(Pos.CENTER);

        Button saveButton = new Button("Save settings");
        saveButton.setOnAction(e-> {
            app.setDifficulty(difficultyCombo.getValue());
            app.setMusicEnabled(musicCheckbox.isSelected());
            app.setGameSpeed((int) speedSlider.getValue());
            app.setSoundEffectsEnabled(sfxCheckbox.isSelected());

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Settings Saved");
            alert.setHeaderText(null);
            alert.setContentText("Your settings have been saved.");
            alert.showAndWait();
        });

        Button backButton = new Button("Back to Main Menu");
        backButton.setOnAction(e -> app.showMainScreen());

        buttonBox.getChildren().addAll(saveButton, backButton);
        setBottom(buttonBox);
        BorderPane.setAlignment(buttonBox, Pos.CENTER);
        BorderPane.setMargin(buttonBox, new Insets(20));
    }

}
