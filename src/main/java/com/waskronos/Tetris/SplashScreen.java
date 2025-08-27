package com.waskronos.Tetris;

import javafx.animation.PauseTransition;
import javafx.geometry.Pos;
import javafx.scene.layout.BorderPane;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.util.Duration;

public class SplashScreen extends BorderPane{
    private final TetrisApp app;
    private MediaPlayer splashPlayer;

    public SplashScreen(TetrisApp app) {
        this.app = app;

        var url = getClass().getResource("/video/splash.mp4");
        if (url != null) {
            splashPlayer = new MediaPlayer(new Media(url.toExternalForm()));
            splashPlayer.setCycleCount(1);
            splashPlayer.setAutoPlay(true);
            splashPlayer.setOnEndOfMedia(this::finish);
            splashPlayer.setOnError(() -> {
                System.out.println("[SPLASH] Media error: " + splashPlayer.getError());
                finish();
            });

            MediaView view = new MediaView(splashPlayer);
            view.setPreserveRatio(true);

            // Scale video and center
            view.fitWidthProperty().bind(widthProperty().multiply(0.6));
            view.fitHeightProperty().bind(heightProperty().multiply(0.6));

            // White background to blend
            setStyle("-fx-background-color: white;");

            setCenter(view);
            BorderPane.setAlignment(view, Pos.CENTER);
        } else {
            System.out.println("[SPLASH] Missing /video/splash.mp4, skipping splash.");
            finish();
            return;
        }

        // Timeout if media events fail
        PauseTransition timeout = new PauseTransition(Duration.seconds(3.2));
        timeout.setOnFinished(e -> finish());
        timeout.play();
    }

    private void finish() {
        if (splashPlayer != null) {
            splashPlayer.stop();
            splashPlayer.dispose();
            splashPlayer = null;
        }
        app.onSplashFinished();
    }
}