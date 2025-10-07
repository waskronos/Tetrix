package com.waskronos.Tetris.store;

/**
 * This object holds settings data for persistence.
 * This is a simple data transfer object.
 */
public class SettingsData {
    private String difficulty;
    private int gameSpeed;
    private boolean musicEnabled;
    private boolean sfxEnabled;

    private int boardWidth;
    private int boardHeight;
    private int cellSize;
    private int startingLevel;

    public SettingsData() {}

    public String getDifficulty() { return difficulty; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }

    public int getGameSpeed() { return gameSpeed; }
    public void setGameSpeed(int gameSpeed) { this.gameSpeed = gameSpeed; }

    public boolean isMusicEnabled() { return musicEnabled; }
    public void setMusicEnabled(boolean musicEnabled) { this.musicEnabled = musicEnabled; }

    public boolean isSfxEnabled() { return sfxEnabled; }
    public void setSfxEnabled(boolean sfxEnabled) { this.sfxEnabled = sfxEnabled; }

    public int getBoardWidth() { return boardWidth; }
    public void setBoardWidth(int boardWidth) { this.boardWidth = boardWidth; }

    public int getBoardHeight() { return boardHeight; }
    public void setBoardHeight(int boardHeight) { this.boardHeight = boardHeight; }

    public int getCellSize() { return cellSize; }
    public void setCellSize(int cellSize) { this.cellSize = cellSize; }

    public int getStartingLevel() { return startingLevel; }
    public void setStartingLevel(int startingLevel) { this.startingLevel = startingLevel; }
}