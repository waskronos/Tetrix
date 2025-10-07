package com.waskronos.Tetris.audio;

public interface AudioService {
    void playMove();
    void playLand();
    void playLine();
    void playLevelUp();
    void playSlam();
    void playGameOver();

    void setSfxEnabled(boolean enabled);
    boolean isSfxEnabled();
}