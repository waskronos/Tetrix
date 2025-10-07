package com.waskronos.Tetris.audio;

import com.waskronos.Tetris.settings.SettingsManager;
import javafx.scene.media.AudioClip;

public class DefaultAudioService implements AudioService {

    private final SettingsManager settings;

    private final AudioClip sfxSlam;
    private final AudioClip sfxLand;
    private final AudioClip sfxLine;
    private final AudioClip sfxLevel;
    private final AudioClip sfxMove;
    private final AudioClip sfxGameOver;

    public DefaultAudioService(SettingsManager settings) {
        this.settings = settings;

        sfxSlam     = load("/sounds/slam.wav");
        sfxLand     = load("/sounds/land.mp3");
        sfxLine     = load("/sounds/clear.wav");
        sfxLevel    = load("/sounds/lvup.wav");
        sfxMove     = load("/sounds/move.wav");
        sfxGameOver = load("/sounds/gameover.mp3");
    }

    private AudioClip load(String path) {
        var url = getClass().getResource(path);
        return (url == null) ? null : new AudioClip(url.toExternalForm());
    }

    @Override public void playMove()     { if (settings.isSfxEnabled() && sfxMove != null) sfxMove.play(); }
    @Override public void playLand()     { if (settings.isSfxEnabled() && sfxLand != null) sfxLand.play(); }
    @Override public void playLine()     { if (settings.isSfxEnabled() && sfxLine != null) sfxLine.play(); }
    @Override public void playLevelUp()  { if (settings.isSfxEnabled() && sfxLevel != null) sfxLevel.play(); }
    @Override public void playSlam()     { if (settings.isSfxEnabled() && sfxSlam != null) sfxSlam.play(); }
    @Override public void playGameOver() { if (settings.isSfxEnabled() && sfxGameOver != null) sfxGameOver.play(); }

    @Override public void setSfxEnabled(boolean enabled) { settings.setSfxEnabled(enabled); }
    @Override public boolean isSfxEnabled()              { return settings.isSfxEnabled(); }
}