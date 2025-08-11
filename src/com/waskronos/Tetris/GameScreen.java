package com.waskronos.Tetris;

import javafx.animation.AnimationTimer;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;

public class GameScreen extends BorderPane {
    private TetrisApp app;
    private Canvas gameCanvas;
    private GraphicsContext gc;
    private int blockY = 0;
    private AnimationTimer animationTimer;

    public GameScreen(TetrisApp app) {
        this.app = app;

        // Create game canvas for drawing
        gameCanvas = new Canvas(400, 500);
        gc = gameCanvas.getGraphicsContext2D();
        setCenter(gameCanvas);

        // Create back button
        Button backButton = new Button("Back to Main Menu");
        backButton.setOnAction(e -> {
            // Stop animation when going back
            if (animationTimer != null) {
                animationTimer.stop();
            }
            app.showMainScreen();
        });

        // Add back button to bottom
        setBottom(backButton);
        BorderPane.setAlignment(backButton, Pos.CENTER);
        BorderPane.setMargin(backButton, new Insets(20));

        // Start animation
        startAnimation();
    }

    private void startAnimation() {
        animationTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                // Clear canvas
                gc.clearRect(0, 0, gameCanvas.getWidth(), gameCanvas.getHeight());

                // Draw game board outline
                gc.setStroke(Color.BLACK);
                gc.strokeRect(50, 50, 300, 400);

                // Draw animated falling block
                gc.setFill(Color.RED);
                gc.fillRect(150, blockY + 50, 50, 50);

                // Draw some sample blocks
                gc.setFill(Color.BLUE);
                gc.fillRect(200, 350, 50, 50);

                gc.setFill(Color.GREEN);
                gc.fillRect(250, 400, 50, 50);

                gc.setFill(Color.ORANGE);
                gc.fillRect(100, 400, 50, 50);

                // Update position
                blockY += 5;
                if (blockY > 350) {
                    blockY = 0;
                }
            }
        };

        animationTimer.start();
    }
}