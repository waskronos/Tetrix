package com.waskronos.Tetris.events;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class GameEvents {
    // this is an event publisher, this follows the observer pattern

    public static final class GameOver {
        // this object holds game over data, this is immutable

        private final String name;
        private final int score;
        private final int level;
        private final long timestamp;

        public GameOver(String name, int score, int level, long timestamp) {
            this.name = name;
            this.score = score;
            this.level = level;
            this.timestamp = timestamp;
        }

        public String getName() { return name; }
        public int getScore() { return score; }
        public int getLevel() { return level; }
        public long getTimestamp() { return timestamp; }
    }

    @FunctionalInterface
    public interface GameOverListener {
        void onGameOver(GameOver event);
    }

    private static final GameEvents INSTANCE = new GameEvents();
    public static GameEvents getInstance() { return INSTANCE; }

    // This list is thread safe for add and iteration
    private final List<GameOverListener> gameOverListeners = new CopyOnWriteArrayList<>();

    private GameEvents() {}

    public void addGameOverListener(GameOverListener listener) {
        if (listener != null) gameOverListeners.add(listener);
    }

    public void removeGameOverListener(GameOverListener listener) {
        gameOverListeners.remove(listener);
    }

    public void fireGameOver(int score, int level) {
        fireGameOver(new GameOver("PLAYER", score, level, System.currentTimeMillis()));
    }

    public void fireGameOver(GameOver event) {
        for (GameOverListener l : gameOverListeners) {
            try {
                l.onGameOver(event);
            } catch (Exception ignore) {
            }
        }
    }
}