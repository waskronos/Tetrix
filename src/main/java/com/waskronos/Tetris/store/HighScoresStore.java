package com.waskronos.Tetris.store;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import javafx.application.Platform;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class HighScoresStore {
    // Persists high scores as JSON.

    private static final HighScoresStore INSTANCE = new HighScoresStore();
    public static HighScoresStore getInstance() { return INSTANCE; }

    private final ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    private final Path filePath = Path.of(System.getProperty("user.home"), ".tetrix", "highscores.json");

    private HighScoresStore() {
        // No global auto-save here to avoid duplicates; saving is triggered by UI prompts.
    }

    public void addScoreAsync(String name, int score, int level) {
        new Thread(() -> {
            HighScores all = load();
            all.addAndTrim(new HighScore(name == null || name.isBlank() ? "PLAYER" : name.trim(), score, System.currentTimeMillis()));
            save(all);
        }, "hiscore-save").start();
    }

    public HighScores load() {
        File f = filePath.toFile();
        if (!f.exists() || f.length() == 0) return new HighScores();
        try {
            return mapper.readValue(f, HighScores.class);
        } catch (IOException e) {
            return new HighScores();
        }
    }

    public void save(HighScores scores) {
        try {
            Files.createDirectories(filePath.getParent());
            mapper.writeValue(filePath.toFile(), scores);
        } catch (IOException ignored) {
        }
    }

    public void loadAsync(java.util.function.Consumer<HighScores> callback) {
        new Thread(() -> {
            HighScores hs = load();
            Platform.runLater(() -> callback.accept(hs));
        }, "hiscore-load").start();
    }
}