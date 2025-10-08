package com.waskronos.Tetris.app;

import com.waskronos.Tetris.ui.*;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.stage.Stage;

public class TetrisApp extends Application {
    private Stage primaryStage;

    private String difficulty = "Medium";
    private boolean musicEnabled = true;
    private boolean soundFX = true;
    private int gameSpeed = 5;

    private MediaPlayer bgmPlayer;
    private boolean inGame = false;

    @Override
    public void start(Stage primaryStageParam){
        this.primaryStage = primaryStageParam;
        primaryStage.setTitle("Tetris");
        primaryStage.setWidth(900);
        primaryStage.setHeight(900);

        var bgmUrl = getClass().getResource("/sounds/bgm.mp3");
        if (bgmUrl != null) {
            try {
                var media = new Media(bgmUrl.toExternalForm());
                media.setOnError(() -> System.out.println("[BGM] Media error: " + media.getError()));
                bgmPlayer = new MediaPlayer(media);
                bgmPlayer.setOnError(() -> System.out.println("[BGM] Player error: " + bgmPlayer.getError()));
                bgmPlayer.setCycleCount(MediaPlayer.INDEFINITE);
                bgmPlayer.setVolume(0.6);
                if (musicEnabled) bgmPlayer.play();
            } catch (Exception ex) {
                System.out.println("[BGM] Failed to init: " + ex.getMessage());
            }
        }

        showSplashScreen();
        primaryStage.show();
        enforceMaximized();
    }

    private void enforceMaximized() {
        primaryStage.setFullScreen(false);
        primaryStage.setMaximized(true);
        primaryStage.setMinWidth(900);
        primaryStage.setMinHeight(900);
    }

    private void applyStyles(Scene scene) {
        var cssUrl = getClass().getResource("/styles/app.css");
        if (cssUrl != null) scene.getStylesheets().add(cssUrl.toExternalForm());
    }

    public void showSplashScreen() {
        inGame = false;
        if (bgmPlayer != null) bgmPlayer.setMute(false);
        SplashScreen splash = new SplashScreen(this);
        Scene scene = new Scene(splash);
        applyStyles(scene);
        primaryStage.setScene(scene);
        enforceMaximized();
    }

    public void onSplashFinished() { showMainScreen(); }

    public void showMainScreen(){
        inGame = false;
        if (bgmPlayer != null) bgmPlayer.setMute(false);
        MainScreen mainScreen = new MainScreen(this);
        Scene scene = new Scene(mainScreen);
        applyStyles(scene);
        primaryStage.setScene(scene);
        enforceMaximized();
    }

    public void showModeSelectScreen() {
        inGame = false;
        if (bgmPlayer != null) bgmPlayer.setMute(false);
        ModeSelectScreen screen = new ModeSelectScreen(this);
        Scene scene = new Scene(screen);
        applyStyles(scene);
        primaryStage.setScene(scene);
        enforceMaximized();
    }

    public void showConfiguration(){
        inGame = false;
        if (bgmPlayer != null) bgmPlayer.setMute(false);
        ConfigScreen configScreen = new ConfigScreen(this);
        Scene scene = new Scene(configScreen);
        applyStyles(scene);
        primaryStage.setScene(scene);
        enforceMaximized();
    }

    public void showGameScreen(){
        inGame = true;
        if (bgmPlayer != null) bgmPlayer.setMute(true);
        GameScreen gameScreen = new GameScreen(this);
        Scene scene = new Scene(gameScreen);
        applyStyles(scene);
        primaryStage.setScene(scene);
        enforceMaximized();
    }

    public void showGameScreenAssisted(){
        inGame = true;
        if (bgmPlayer != null) bgmPlayer.setMute(true);
        GameScreen gameScreen = new GameScreen(this);
        gameScreen.setAiActive(true);
        Scene scene = new Scene(gameScreen);
        applyStyles(scene);
        primaryStage.setScene(scene);
        enforceMaximized();
    }

    public void showTwoPlayerScreen() {
        inGame = true;
        if (bgmPlayer != null) bgmPlayer.setMute(true);
        TwoPlayerScreen screen = new TwoPlayerScreen(this);
        Scene scene = new Scene(screen);
        applyStyles(scene);
        primaryStage.setScene(scene);
        enforceMaximized();
    }

    // Two-player configured start; supports Human/AI/External per player.
    public void showTwoPlayerConfigured(TwoPlayerScreen.PlayerMode p1Mode, TwoPlayerScreen.PlayerMode p2Mode) {
        inGame = true;
        if (bgmPlayer != null) bgmPlayer.setMute(true);
        TwoPlayerScreen screen = new TwoPlayerScreen(this, p1Mode, p2Mode);
        Scene scene = new Scene(screen);
        applyStyles(scene);
        primaryStage.setScene(scene);
        enforceMaximized();
    }

    public void showHighScoresScreen() {
        inGame = false;
        if (bgmPlayer != null) bgmPlayer.setMute(false);
        HighScoresScreen screen = new HighScoresScreen(this);
        Scene scene = new Scene(screen);
        applyStyles(scene);
        primaryStage.setScene(scene);
        enforceMaximized();
    }

    public void exitApplication(){
        if (bgmPlayer != null) {
            bgmPlayer.stop();
            bgmPlayer.dispose();
            bgmPlayer = null;
        }
        primaryStage.close();
    }

    public void enterGame() {
        inGame = true;
        if (bgmPlayer != null) bgmPlayer.setMute(true);
    }
    public void exitGame() {
        inGame = false;
        if (bgmPlayer != null) {
            bgmPlayer.setMute(false);
            if (musicEnabled && bgmPlayer.getStatus() != MediaPlayer.Status.PLAYING) {
                bgmPlayer.play();
            }
        }
    }

    public String getDifficulty(){ return difficulty; }
    public void setDifficulty(String difficulty){ this.difficulty = difficulty; }

    public boolean isMusicEnabled(){ return musicEnabled; }
    public void setMusicEnabled(boolean musicEnabled){
        this.musicEnabled = musicEnabled;
        if (bgmPlayer == null) return;
        if (musicEnabled) {
            if (bgmPlayer.getStatus() != MediaPlayer.Status.PLAYING) bgmPlayer.play();
            bgmPlayer.setMute(inGame);
        } else {
            bgmPlayer.pause();
        }
    }

    public boolean isSoundEffectsEnabled() { return soundFX; }
    public void setSoundEffectsEnabled(boolean soundEffectsEnabled) { this.soundFX = soundEffectsEnabled; }

    public int getGameSpeed() { return gameSpeed; }
    public void setGameSpeed(int gameSpeed) {
        if (gameSpeed < 1) gameSpeed = 1;
        if (gameSpeed > 10) gameSpeed = 10;
        this.gameSpeed = gameSpeed;
    }
}