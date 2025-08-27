package com.waskronos.Tetris;

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

        // Create MediaPlayer instance
        var bgmUrl = getClass().getResource("/sounds/bgm.mp3");
        if (bgmUrl != null) {
            bgmPlayer = new MediaPlayer(new Media(bgmUrl.toExternalForm()));
            bgmPlayer.setCycleCount(MediaPlayer.INDEFINITE);
            bgmPlayer.setVolume(0.6);
            // Start playback. Muting handled later
            if (musicEnabled) {
                bgmPlayer.play();
            }
        } else {
            System.out.println("[BGM] Missing: /sounds/bgm.mp3");
        }

        showSplashScreen();
        primaryStage.show();
    }

    public void showSplashScreen() {
        inGame = false;
        if (bgmPlayer != null) bgmPlayer.setMute(false);
        SplashScreen splash = new SplashScreen(this);
        Scene scene = new Scene(splash);
        primaryStage.setScene(scene);
    }

    public void onSplashFinished() {
        showMainScreen();
    }

    public void showMainScreen(){
        inGame = false;
        if (bgmPlayer != null) bgmPlayer.setMute(false);
        MainScreen mainScreen = new MainScreen(this);
        Scene scene = new Scene(mainScreen);
        primaryStage.setScene(scene);
    }

    public void showConfiguration(){
        inGame = false;
        if (bgmPlayer != null) bgmPlayer.setMute(false);
        ConfigScreen configScreen = new ConfigScreen(this);
        Scene scene = new Scene(configScreen);
        primaryStage.setScene(scene);
    }

    public void showGameScreen(){
        inGame = true;
        if (bgmPlayer != null) bgmPlayer.setMute(true);
        GameScreen gameScreen = new GameScreen(this);
        Scene scene = new Scene(gameScreen);
        primaryStage.setScene(scene);
    }

    public void exitApplication(){
        if (bgmPlayer != null) {
            bgmPlayer.stop();
            bgmPlayer.dispose();
            bgmPlayer = null;
        }
        primaryStage.close();
    }

    // GameScreen notifies when entering/leaving game
    public void enterGame() {
        inGame = true;
        if (bgmPlayer != null) bgmPlayer.setMute(true);
    }
    public void exitGame() {
        inGame = false;
        if (bgmPlayer != null) {
            bgmPlayer.setMute(false);
            // Resume if enabled and not playing
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
            // Start playback if not playing
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