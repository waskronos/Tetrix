package com.waskronos.Tetris.ui;

import com.waskronos.Tetris.app.TetrisApp;
import com.waskronos.Tetris.settings.SettingsManager;
import com.waskronos.Tetris.store.SettingsStore;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

public class ConfigScreen extends BorderPane {
    // This screen edits settings.
    // This uses SettingsManager as a singleton and SettingsStore for persistence.
    // This follows single responsibility.

    private final TetrisApp app;
    private final SettingsManager settings = SettingsManager.getInstance();

    public ConfigScreen(TetrisApp app) {
        this.app = app;

        var cssUrl = getClass().getResource("/styles/app.css");
        if (cssUrl != null) getStylesheets().add(cssUrl.toExternalForm());

        Label titleLabel = new Label("Configuration");
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 36));
        titleLabel.getStyleClass().add("title");
        setTop(titleLabel);
        BorderPane.setAlignment(titleLabel, Pos.CENTER);
        BorderPane.setMargin(titleLabel, new Insets(12));

        GridPane configGrid = new GridPane();
        configGrid.setHgap(12);
        configGrid.setVgap(14);
        configGrid.setPadding(new Insets(20));
        configGrid.setAlignment(Pos.CENTER);

        int row = 0;

        configGrid.add(new Label("Difficulty"), 0, row);
        ComboBox<String> difficultyCombo = new ComboBox<>();
        difficultyCombo.getItems().addAll("Easy", "Medium", "Hard");
        difficultyCombo.setValue(app.getDifficulty());
        configGrid.add(difficultyCombo, 1, row++);

        configGrid.add(new Label("Speed"), 0, row);
        Slider speedSlider = new Slider(1, 10, app.getGameSpeed());
        speedSlider.setShowTickLabels(true);
        speedSlider.setShowTickMarks(true);
        speedSlider.setMajorTickUnit(1);
        speedSlider.setMinorTickCount(0);
        speedSlider.setBlockIncrement(1);
        speedSlider.setSnapToTicks(true);
        configGrid.add(speedSlider, 1, row++);

        configGrid.add(new Label("Music"), 0, row);
        CheckBox musicCheckbox = new CheckBox("Enable Music");
        musicCheckbox.setSelected(app.isMusicEnabled());
        configGrid.add(musicCheckbox, 1, row++);

        configGrid.add(new Label("Sound effects"), 0, row);
        CheckBox sfxCheckbox = new CheckBox("Enable Sound Effects");
        sfxCheckbox.setSelected(app.isSoundEffectsEnabled());
        configGrid.add(sfxCheckbox, 1, row++);

        configGrid.add(new Label("Board Width"), 0, row);
        Spinner<Integer> widthSpinner = new Spinner<>(6, 20, settings.getBoardWidth());
        widthSpinner.setEditable(true);
        configGrid.add(widthSpinner, 1, row++);

        configGrid.add(new Label("Board Height"), 0, row);
        Spinner<Integer> heightSpinner = new Spinner<>(10, 40, settings.getBoardHeight());
        heightSpinner.setEditable(true);
        configGrid.add(heightSpinner, 1, row++);

        configGrid.add(new Label("Cell Size px"), 0, row);
        Spinner<Integer> cellSpinner = new Spinner<>(16, 48, settings.getCellSize(), 2);
        cellSpinner.setEditable(true);
        configGrid.add(cellSpinner, 1, row++);

        setCenter(configGrid);

        HBox buttonBox = new HBox(20);
        buttonBox.setAlignment(Pos.CENTER);

        Button saveButton = new Button("Save settings");
        saveButton.setOnAction(e -> {
            app.setDifficulty(difficultyCombo.getValue());
            app.setMusicEnabled(musicCheckbox.isSelected());
            app.setGameSpeed((int) speedSlider.getValue());
            app.setSoundEffectsEnabled(sfxCheckbox.isSelected());

            settings.setBoardWidth(widthSpinner.getValue());
            settings.setBoardHeight(heightSpinner.getValue());
            settings.setCellSize(cellSpinner.getValue());
            settings.setMusicEnabled(musicCheckbox.isSelected());
            settings.setSfxEnabled(sfxCheckbox.isSelected());

            SettingsStore.getInstance().saveFromCurrentAsync(app);

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Settings Saved");
            alert.setHeaderText(null);
            alert.setContentText("Your settings have been saved.");
            alert.showAndWait();
        });

        Button resetButton = new Button("Reset to Defaults");
        resetButton.setOnAction(e -> {
            settings.resetToDefaults();
            widthSpinner.getValueFactory().setValue(settings.getBoardWidth());
            heightSpinner.getValueFactory().setValue(settings.getBoardHeight());
            cellSpinner.getValueFactory().setValue(settings.getCellSize());
        });

        Button backButton = new Button("Back to Main Menu");
        backButton.setOnAction(e -> app.showMainScreen());

        buttonBox.getChildren().addAll(saveButton, resetButton, backButton);
        setBottom(buttonBox);
        BorderPane.setAlignment(buttonBox, Pos.CENTER);
        BorderPane.setMargin(buttonBox, new Insets(20));
    }
}