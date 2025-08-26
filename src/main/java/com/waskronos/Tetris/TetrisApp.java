package com.waskronos.Tetris;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class TetrisApp extends Application {
    private Stage primaryStage;
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
}
