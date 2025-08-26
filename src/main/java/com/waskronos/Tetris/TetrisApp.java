package com.waskronos.Tetris;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class TetrisApp extends Application {
    private Stage primaryStage;

    private String difficulty = "Medium";
    private boolean musicEnabled = false;
    private boolean soundFX = false;
    private int gameSpeed = 5;

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
        Scene scene = new Scene(mainScreen);
        primaryStage.setScene(scene);
    }

    public void showConfiguration(){
        ConfigScreen configScreen = new ConfigScreen(this);
        Scene scene = new Scene(configScreen);
        primaryStage.setScene(scene);
    }

    public void showGameScreen(){
        GameScreen gameScreen = new GameScreen(this);
        Scene scene = new Scene(gameScreen);
        primaryStage.setScene(scene);
    }

    public void exitApplication(){
        primaryStage.close();
    }

    //Setting holder/setter/getter whatever
    public String getDifficulty(){
        return difficulty;
    }
    public void setDifficulty(String difficulty){
        this.difficulty = difficulty;
    }

    public boolean isMusicEnabled(){
        return musicEnabled;
    }

    public void setMusicEnabled(boolean musicEnabled){
        this.musicEnabled = musicEnabled;
    }
    public boolean isSoundEffectsEnabled() {
        return soundFX;
    }
    public void setSoundEffectsEnabled(boolean soundEffectsEnabled) {
        this.soundFX = soundEffectsEnabled;
    }

    public int getGameSpeed() {
        return gameSpeed;
    }
    public void setGameSpeed(int gameSpeed) {
        // clamp to closest values so nothings unusual
        if (gameSpeed < 1) gameSpeed = 1;
        if (gameSpeed > 10) gameSpeed = 10;
        this.gameSpeed = gameSpeed;
    }
}
