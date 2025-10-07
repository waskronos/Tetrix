package com.waskronos.Tetris.store;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.waskronos.Tetris.app.TetrisApp;
import com.waskronos.Tetris.settings.SettingsManager;
import javafx.application.Platform;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class SettingsStore {
    // This class loads and saves settings as JSON
    // This follows single responsibility

    private static final SettingsStore INSTANCE = new SettingsStore();
    public static SettingsStore getInstance() { return INSTANCE; }

    private final ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    private final Path filePath = Path.of(System.getProperty("user.home"), ".tetrix", "settings.json");

    private SettingsStore() {}

    public void loadAndApplyAsync(TetrisApp app) {
        new Thread(() -> {
            SettingsData data = loadInternal();
            if (data == null) return;
            Platform.runLater(() -> applyToAppAndManager(app, data));
        }, "settings-load").start();
    }

    public void saveFromCurrentAsync(TetrisApp app) {
        new Thread(() -> {
            SettingsData data = captureFromAppAndManager(app);
            saveInternal(data);
        }, "settings-save").start();
    }

    private SettingsData loadInternal() {
        File file = filePath.toFile();
        if (!file.exists() || file.length() == 0) return null;
        try {
            return mapper.readValue(file, SettingsData.class);
        } catch (IOException e) {
            return null;
        }
    }

    private void saveInternal(SettingsData data) {
        try {
            Files.createDirectories(filePath.getParent());
            mapper.writeValue(filePath.toFile(), data);
        } catch (IOException ignored) {
        }
    }

    private static SettingsData captureFromAppAndManager(TetrisApp app) {
        SettingsManager s = SettingsManager.getInstance();
        SettingsData d = new SettingsData();
        d.setDifficulty(app.getDifficulty());
        d.setGameSpeed(app.getGameSpeed());
        d.setMusicEnabled(app.isMusicEnabled());
        d.setSfxEnabled(app.isSoundEffectsEnabled());
        d.setBoardWidth(s.getBoardWidth());
        d.setBoardHeight(s.getBoardHeight());
        d.setCellSize(s.getCellSize());
        d.setStartingLevel(s.getStartingLevel());
        return d;
    }

    private static void applyToAppAndManager(TetrisApp app, SettingsData d) {
        if (d.getDifficulty() != null && !d.getDifficulty().isBlank()) app.setDifficulty(d.getDifficulty());
        if (d.getGameSpeed() > 0) app.setGameSpeed(d.getGameSpeed());
        app.setMusicEnabled(d.isMusicEnabled());
        app.setSoundEffectsEnabled(d.isSfxEnabled());

        SettingsManager s = SettingsManager.getInstance();
        if (d.getBoardWidth() > 0) s.setBoardWidth(d.getBoardWidth());
        if (d.getBoardHeight() > 0) s.setBoardHeight(d.getBoardHeight());
        if (d.getCellSize() > 0) s.setCellSize(d.getCellSize());
        if (d.getStartingLevel() > 0) s.setStartingLevel(d.getStartingLevel());
        s.setMusicEnabled(d.isMusicEnabled());
        s.setSfxEnabled(d.isSfxEnabled());
    }
}