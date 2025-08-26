package com.waskronos.Tetris;
import javafx.animation.FadeTransition;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.util.Duration;

public class TetrisApp extends Application {
    private Stage primaryStage;
    
    // Game settings
    private String difficulty = "Medium";
    private double speed = 5.0;
    private boolean musicEnabled = false;
    private boolean soundEffectsEnabled = false;
    @Override
    public void start(Stage primayStage){
        this.primaryStage = primayStage;
        primaryStage.setTitle("Tetris");

        primayStage.setWidth(900);
        primaryStage.setHeight(900);
        showMainScreen();
        primaryStage.show();
    }

    public void showMainScreen(){
        MainScreen mainScreen = new MainScreen(this);
        transitionToScene(new Scene(mainScreen));
    }

    public void showConfiguration(){
        ConfigScreen configScreen = new ConfigScreen(this);
        transitionToScene(new Scene(configScreen));
    }

    public void showGameScreen(){
        GameScreen gameScreen = new GameScreen(this);
        transitionToScene(new Scene(gameScreen));
    }

    public void exitApplication(){
        primaryStage.close();
    }
    
    private void transitionToScene(Scene newScene) {
        newScene.getRoot().setOpacity(0);
        primaryStage.setScene(newScene);
        
        FadeTransition fadeIn = new FadeTransition(Duration.millis(300), newScene.getRoot());
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        fadeIn.play();
    }
    
    // Game settings getters and setters
    public String getDifficulty() {
        return difficulty;
    }
    
    public void setDifficulty(String difficulty) {
        this.difficulty = difficulty;
    }
    
    public double getSpeed() {
        return speed;
    }
    
    public void setSpeed(double speed) {
        this.speed = speed;
    }
    
    public boolean isMusicEnabled() {
        return musicEnabled;
    }
    
    public void setMusicEnabled(boolean musicEnabled) {
        this.musicEnabled = musicEnabled;
    }
    
    public boolean isSoundEffectsEnabled() {
        return soundEffectsEnabled;
    }
    
    public void setSoundEffectsEnabled(boolean soundEffectsEnabled) {
        this.soundEffectsEnabled = soundEffectsEnabled;
    }
}
