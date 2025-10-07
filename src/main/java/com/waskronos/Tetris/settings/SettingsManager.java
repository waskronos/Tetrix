package com.waskronos.Tetris.settings;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;

/**
 * This class holds runtime settings
 * This uses the Singleton pattern
 * Properties allow simple UI binding
 */
public final class SettingsManager {

    private static final class Holder {
        private static final SettingsManager INSTANCE = new SettingsManager();
    }

    public static SettingsManager getInstance() {
        return Holder.INSTANCE;
    }

    private final IntegerProperty boardWidth  = new SimpleIntegerProperty(10);
    private final IntegerProperty boardHeight = new SimpleIntegerProperty(20);
    private final IntegerProperty cellSize    = new SimpleIntegerProperty(32);

    private final IntegerProperty startingLevel = new SimpleIntegerProperty(1);

    private final BooleanProperty musicEnabled = new SimpleBooleanProperty(true);
    private final BooleanProperty sfxEnabled   = new SimpleBooleanProperty(true);

    private SettingsManager() {}

    public void resetToDefaults() {
        setBoardWidth(10);
        setBoardHeight(20);
        setCellSize(32);
        setStartingLevel(1);
        setMusicEnabled(true);
        setSfxEnabled(true);
    }

    public IntegerProperty boardWidthProperty() { return boardWidth; }
    public int getBoardWidth() { return boardWidth.get(); }
    public void setBoardWidth(int value) { boardWidth.set(value); }

    public IntegerProperty boardHeightProperty() { return boardHeight; }
    public int getBoardHeight() { return boardHeight.get(); }
    public void setBoardHeight(int value) { boardHeight.set(value); }

    public IntegerProperty cellSizeProperty() { return cellSize; }
    public int getCellSize() { return cellSize.get(); }
    public void setCellSize(int value) { cellSize.set(value); }

    public IntegerProperty startingLevelProperty() { return startingLevel; }
    public int getStartingLevel() { return startingLevel.get(); }
    public void setStartingLevel(int value) { startingLevel.set(value); }

    public BooleanProperty musicEnabledProperty() { return musicEnabled; }
    public boolean isMusicEnabled() { return musicEnabled.get(); }
    public void setMusicEnabled(boolean value) { musicEnabled.set(value); }

    public BooleanProperty sfxEnabledProperty() { return sfxEnabled; }
    public boolean isSfxEnabled() { return sfxEnabled.get(); }
    public void setSfxEnabled(boolean value) { sfxEnabled.set(value); }
}