package com.waskronos.Tetris;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

public class ConfigScreen extends BorderPane {
    private TetrisApp app;
    private ComboBox<String> difficultyCombo;
    private Slider speedSlider;
    private CheckBox musicCheckbox;
    private CheckBox sfxCheckbox;

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
        difficultyCombo = new ComboBox<>();
        difficultyCombo.getItems().addAll("Easy", "Medium", "Hard");
        difficultyCombo.setValue(app.getDifficulty());
        configGrid.add(difficultyCombo, 1, 0);

        configGrid.add(new Label("Music"), 0, 1);
        musicCheckbox = new CheckBox("Enable Music");
        musicCheckbox.setSelected(app.isMusicEnabled());
        configGrid.add(musicCheckbox, 1, 2);

        configGrid.add(new Label("Speed"), 0, 2);
        speedSlider = new Slider(1, 10, app.getSpeed());
        speedSlider.setShowTickLabels(true);
        speedSlider.setShowTickMarks(true);
        speedSlider.setMajorTickUnit(1);
        speedSlider.setMinorTickCount(0);
        speedSlider.setBlockIncrement(1);
        speedSlider.setSnapToTicks(true);
        configGrid.add(speedSlider, 1, 1);

        configGrid.add(new Label("Sound effects"), 0, 3);
        sfxCheckbox = new CheckBox("Enable Sound Effects");
        sfxCheckbox.setSelected(app.isSoundEffectsEnabled());
        configGrid.add(sfxCheckbox, 1, 3);

        setCenter(configGrid);

        Button backButton = new Button("Back to Main Menu");
        backButton.setOnAction(e -> {
            saveSettings();
            app.showMainScreen();
        });

        setBottom(backButton);
        BorderPane.setAlignment(backButton, Pos.CENTER);
        BorderPane.setMargin(backButton, new Insets(20));

    }
    
    private void saveSettings() {
        app.setDifficulty(difficultyCombo.getValue());
        app.setSpeed(speedSlider.getValue());
        app.setMusicEnabled(musicCheckbox.isSelected());
        app.setSoundEffectsEnabled(sfxCheckbox.isSelected());
    }

}
