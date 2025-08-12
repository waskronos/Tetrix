package com.waskronos.Tetris;

import javafx.animation.AnimationTimer;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

public class GameScreen extends BorderPane {
    private TetrisApp app;
    private Canvas gameCanvas;
    private GraphicsContext gc;
    private AnimationTimer loop;

    private final int GRID_WIDTH = 10;
    private final int GRID_HEIGHT = 20; //keeping tetris grid size 10x20 (conventional size)
    private final int CELL_SIZE = 30;

    private int[][] grid = new int[GRID_HEIGHT][GRID_WIDTH];

    private int score = 0;
    private int level = 0;
    private int lines_clear = 0;

    public GameScreen(TetrisApp app) {
        this.app = app;

        //initalize an empty grid at start
        for (int y = 0; y < GRID_HEIGHT; y++){
            for (int x = 0; x < GRID_WIDTH; x ++){
                grid[y][x] = 0;
            }
        } //takes every grid from 0 to 10 vertically and also horizontally to 20, deems their product to be zero
        //0 = empty grid, 1+ = filled with smthing

        HBox gameBox = new HBox(20);
        gameBox.setAlignment(Pos.CENTER);

        // Create game canvas for drawing
        gameCanvas = new Canvas(GRID_HEIGHT*CELL_SIZE, GRID_HEIGHT*CELL_SIZE);
        gc = gameCanvas.getGraphicsContext2D();

        VBox statsPanel = createStatsPanel();

        gameBox.getChildren().addAll(gameCanvas, statsPanel);
        setCenter(gameBox);

        BorderPane.setMargin(gameBox, new Insets(20));

        // Create back button
        Button backButton = new Button("Back to Main Menu");
        backButton.setOnAction(e -> {
            //will stop again (multiple ways to stop a game)
            stopGame();
            app.showMainScreen();
        });

        // Add back button to bottom
        setBottom(backButton);
        BorderPane.setAlignment(backButton, Pos.CENTER);
        BorderPane.setMargin(backButton, new Insets(20));

        // Start animation
        startGame();
    }

    private VBox createStatsPanel(){
        VBox statsPanel = new VBox(20);
        statsPanel.setAlignment(Pos.CENTER);
        statsPanel.setPrefWidth(20);

        Label nextLabel = new Label("NEXT");
        nextLabel.setFont(Font.font("ARIAL", FontWeight.BOLD, 20));
        Canvas nextpieceCanvas = new Canvas(120,120);

        Label scoreLabel = new Label("SCORE");
        scoreLabel.setFont(Font.font("ARIAL", FontWeight.BOLD, 20));
        Label scoreValue = new Label("0");
        scoreValue.setFont(Font.font("ARIAL", FontWeight.BOLD, 18));

        Label levelLabel = new Label("LEVEL");
        levelLabel.setFont(Font.font("ARIAL", FontWeight.BOLD, 20));
        Label levelValue = new Label("1");
        levelValue.setFont(Font.font("ARIAL", FontWeight.BOLD, 18));

        Label diffiLabel = new Label("DIFFICULTY");
        diffiLabel.setFont(Font.font("ARIAL", FontWeight.BOLD, 20));
        Label diffiValue = new Label("MEDIUM");
        diffiValue.setFont(Font.font("ARIAL", FontWeight.BOLD, 18));

        statsPanel.getChildren().addAll(
                nextLabel, nextpieceCanvas,
                scoreLabel, scoreValue,
                levelLabel, levelValue,
                diffiLabel, diffiValue
        );
        return statsPanel;
    }

    private void startGame(){
        testPattern();
        loop = new AnimationTimer() {
            @Override
            public void handle(long l) {
                drawGame();
            }
        };

        loop.start();
    }

    private void stopGame(){
        if (loop != null){
            loop.stop();
        }
    }

    private void testPattern(){
        for (int x = 0; x < GRID_WIDTH; x++){
            grid[GRID_HEIGHT-1][x] = 1;
        }

        grid[GRID_HEIGHT - 2][7] = 2; //creates a random stack
        grid[GRID_HEIGHT - 3][7] = 2;
        grid[GRID_HEIGHT - 2][8] = 2;
        grid[GRID_HEIGHT - 3][8] = 2;

        grid[GRID_HEIGHT-2][2] = 3; //creates L shape
        grid[GRID_HEIGHT-3][2] = 3;
        grid[GRID_HEIGHT-4][2] = 3;
        grid[GRID_HEIGHT-2][3] = 3;
    }

    private void drawGame(){
        gc.clearRect(0, 0, gameCanvas.getWidth(), gameCanvas.getHeight());
        gc.setFill(Color.rgb(240,240,240));
        gc.fillRect(0, 0, GRID_WIDTH * CELL_SIZE, GRID_HEIGHT * CELL_SIZE);
        gc.setStroke(Color.LIGHTGRAY);
        gc.setLineWidth(0.6);

        for(int x = 0; x <= GRID_WIDTH; x++){
            gc.strokeLine( x * CELL_SIZE, 0, x * CELL_SIZE, GRID_HEIGHT*CELL_SIZE);
        }

        for (int y = 0; y <= GRID_HEIGHT; y++){
            gc.strokeLine(0, y*CELL_SIZE, GRID_WIDTH*CELL_SIZE, y *CELL_SIZE);
        }

        for (int y = 0; y < GRID_HEIGHT; y++){
            for (int x = 0; x < GRID_WIDTH; x++){
                if (grid[y][x] > 0){
                    drawBlock(x,y, getColor(grid[y][x]));
                }
            }
        }

        gc.setStroke(Color.BLACK);
        gc.setLineWidth(2);
        gc.strokeRect(0, 0, GRID_WIDTH * CELL_SIZE, GRID_HEIGHT * CELL_SIZE);
    }

    private void drawBlock(int x, int y, Color color){
        int pixelX = x * CELL_SIZE;
        int pixelY = y * CELL_SIZE;

        gc.setFill(color); //filling block with 2d color
        gc.fillRect(pixelX + 1, pixelY + 1, CELL_SIZE -2, CELL_SIZE - 2);

        //to give 3d effects we give a highlight shade and a shadow
        gc.setStroke(color.brighter());
        gc.setLineWidth(2);
        gc.strokeLine(pixelX + 1, pixelY + 1, pixelX + CELL_SIZE - 1, pixelY + 1); //top side
        gc.strokeLine(pixelX + 1, pixelY + 1, pixelX + 1, pixelY + CELL_SIZE - 1); //left side to highlight

        gc.setStroke(color.darker());
        gc.strokeLine(pixelX + CELL_SIZE - 1, pixelY + 1, pixelX + CELL_SIZE - 1, pixelY + CELL_SIZE - 1);
        gc.strokeLine(pixelX + 1, pixelY + CELL_SIZE - 1, pixelX + CELL_SIZE - 1, pixelY + CELL_SIZE - 1);
    }

    private Color getColor(int value){
        switch(value){
            case 1: return Color.CYAN;
            case 2: return Color.BLUE;
            case 3: return Color.ORANGE;
            case 4: return Color.YELLOW;
            case 5: return Color.GREEN;
            case 6: return Color.PURPLE;
            case 7: return Color.RED;
            default: return Color.GRAY;
        }
    }
}