package com.waskronos.Tetris;

import javafx.animation.AnimationTimer;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.Random;

public class GameScreen extends BorderPane {

    private TetrisApp app;
    private Canvas gameCanvas;
    private GraphicsContext gc;
    private AnimationTimer loop;

    private final int GRID_WIDTH = 10;
    private final int GRID_HEIGHT = 20;
    private final int CELL_SIZE = 30;

    // Grid stores 0 = empty, 1..7 = fixed block with color id
    private int[][] grid = new int[GRID_HEIGHT][GRID_WIDTH];

    private Tetramino currentTetramino;
    private int currentColorIndex;

    private Tetramino nextTetramino;
    private int nextColorIndex;  // 1..7 matches getColor
    private Random random = new Random();

    // Timing (you can later scale by level)
    private long fallIntervalNs = 500_000_000; // 0.5s per step

    private int score = 0;
    private int level = 1;
    private Label scoreValue;
    private Label levelValue;

    private String difficulty = "MEDIUM";
    private Canvas nextPieceCanvas;

    public GameScreen(TetrisApp app) {
        this.app = app;

        initializeGrid();

        HBox gameBox = new HBox(20);
        gameBox.setAlignment(Pos.CENTER);

        gameCanvas = new Canvas(GRID_WIDTH * CELL_SIZE, GRID_HEIGHT * CELL_SIZE);
        gc = gameCanvas.getGraphicsContext2D();

        VBox statsPanel = createStatsPanel();
        gameBox.getChildren().addAll(gameCanvas, statsPanel);
        setCenter(gameBox);
        BorderPane.setMargin(gameBox, new Insets(20));

        Button backButton = new Button("Back to Main Menu");
        backButton.setOnAction(e -> {
            stopGame();
            app.showMainScreen();
        });
        setBottom(backButton);
        BorderPane.setAlignment(backButton, Pos.CENTER);
        BorderPane.setMargin(backButton, new Insets(20));

        setupInputHandlers();
        generateNextTetramino();
        spawnTetramino();
        startGame();
    }

    private void initializeGrid() {
        for (int y = 0; y < GRID_HEIGHT; y++) {
            for (int x = 0; x < GRID_WIDTH; x++) {
                grid[y][x] = 0;
            }
        }
    }

    private VBox createStatsPanel() {
        VBox statsPanel = new VBox(20);
        statsPanel.setAlignment(Pos.CENTER);
        statsPanel.setPrefWidth(120);

        Label nextLabel = new Label("NEXT");
        nextLabel.setFont(Font.font("ARIAL", FontWeight.BOLD, 20));
        nextPieceCanvas = new Canvas(80, 80);

        Label scoreLabel = new Label("SCORE");
        scoreLabel.setFont(Font.font("ARIAL", FontWeight.BOLD, 20));
        scoreValue = new Label("0");
        scoreValue.setFont(Font.font("ARIAL", FontWeight.BOLD, 18));

        Label levelLabel = new Label("LEVEL");
        levelLabel.setFont(Font.font("ARIAL", FontWeight.BOLD, 20));
        levelValue = new Label("1");
        levelValue.setFont(Font.font("ARIAL", FontWeight.BOLD, 18));

        Label diffiLabel = new Label("DIFFICULTY");
        diffiLabel.setFont(Font.font("ARIAL", FontWeight.BOLD, 20));
        Label diffiValue = new Label(difficulty);
        diffiValue.setFont(Font.font("ARIAL", FontWeight.BOLD, 18));

        statsPanel.getChildren().addAll(
                nextLabel, nextPieceCanvas,
                scoreLabel, scoreValue,
                levelLabel, levelValue,
                diffiLabel, diffiValue
        );
        return statsPanel;
    }

    private void setupInputHandlers() {
        // Allow canvas to receive key events
        gameCanvas.setFocusTraversable(true);
        gameCanvas.setOnKeyPressed(e -> {
            if (currentTetramino == null) return;
            KeyCode code = e.getCode();
            switch (code) {
                case A -> {
                    if (!willCollide(-1, 0)) currentTetramino.move(-1, 0);
                }
                case D -> {
                    if (!willCollide(1, 0)) currentTetramino.move(1, 0);
                }
                case S -> {
                    // Soft drop
                    if (!willCollide(0, 1)) {
                        currentTetramino.move(0, 1);
                    } else {
                        lockAndSpawn();
                    }
                }
                case W -> {
                    // (Optional) rotate; only if implemented in Tetramino
                    // rotateTetramino();
                }
                case SPACE -> {
                    // Hard drop (optional)
                    while (!willCollide(0, 1)) {
                        currentTetramino.move(0, 1);
                    }
                    lockAndSpawn();
                }
                case LEFT -> {
                    currentTetramino.rotateCCW();
                    if (willCollide(0, 0)) {
                        // Undo rotation if collides
                        currentTetramino.rotateCW();
                    }
                }
                case RIGHT -> {
                    currentTetramino.rotateCW();
                    if (willCollide(0, 0)) {
                        // Undo rotation if collides
                        currentTetramino.rotateCCW();
                    }
                }
            }
        });
    }

    private void startGame() {
        loop = new AnimationTimer() {
            private long lastFall = 0;

            @Override
            public void handle(long now) {
                if (lastFall == 0) lastFall = now;

                if (now - lastFall >= fallIntervalNs) {
                    updateGame();
                    lastFall = now;
                }
                drawGame();
                drawNextPiecePreview();
            }
        };
        loop.start();
    }

    private void stopGame() {
        if (loop != null) loop.stop();
    }

    private void generateNextTetramino(){
        int[][][] shapes = {
                {{1, 1, 1, 1}},          // I
                {{1, 1}, {1, 1}},        // O
                {{0, 1, 0}, {1, 1, 1}},    // T
                {{0, 1, 1}, {1, 1, 0}},    // S
                {{1, 1, 0}, {0, 1, 1}},    // Z
                {{1, 0, 0}, {1, 1, 1}},    // J
                {{0, 0, 1}, {1, 1, 1}}     // L
        };
        nextColorIndex = random.nextInt(shapes.length) + 1;
        int[][] shape = shapes[nextColorIndex - 1];
        // Centered at (0,0) for preview
        nextTetramino = new Tetramino(shape, 0, 0);
    }

    private void spawnTetramino() { //now we use the next tetramino as the current tetramino to spawn
        currentTetramino = nextTetramino;
        currentColorIndex = nextColorIndex;
        generateNextTetramino();

        int startX = (GRID_WIDTH - currentTetramino.getShape()[0].length) / 2;
        int startY = 0;
        currentTetramino.setPosition(startX, startY);

        // Immediate game over detection if spawn position collides = no more space
        if (willCollide(0, 0)) {
            stopGame();
            System.out.println("GAME OVER");
            currentTetramino = null;
        }
    }

    private void updateGame() {
        if (currentTetramino == null) return;

        // If moving down would collide -> lock & spawn new
        if (willCollide(0, 1)) {
            lockAndSpawn();
        } else {
            currentTetramino.move(0, 1);
        }
    }

    private void lockAndSpawn() {
        placeTetramino();
        addScoreForLand();
        clearCompletedLines();
        spawnTetramino();
    }

    private boolean willCollide(int dx, int dy) {
        if (currentTetramino == null) return false;
        int[][] shape = currentTetramino.getShape();
        int baseX = currentTetramino.getX() + dx;
        int baseY = currentTetramino.getY() + dy;

        for (int r = 0; r < shape.length; r++) {
            for (int c = 0; c < shape[r].length; c++) {
                if (shape[r][c] == 0) continue;

                int gridX = baseX + c;
                int gridY = baseY + r;

                // Outside bottom
                if (gridY >= GRID_HEIGHT) return true;
                // Outside sides
                if (gridX < 0 || gridX >= GRID_WIDTH) return true;
                // Collides with fixed block
                if (gridY >= 0 && grid[gridY][gridX] != 0) return true;
            }
        }
        return false;
    }

    private void placeTetramino() {
        int[][] shape = currentTetramino.getShape();
        int baseX = currentTetramino.getX();
        int baseY = currentTetramino.getY();

        for (int r = 0; r < shape.length; r++) {
            for (int c = 0; c < shape[r].length; c++) {
                if (shape[r][c] == 0) continue;
                int gx = baseX + c;
                int gy = baseY + r;
                if (gy >= 0 && gy < GRID_HEIGHT && gx >= 0 && gx < GRID_WIDTH) {
                    grid[gy][gx] = currentColorIndex;
                }
            }
        }
    }

    private void clearCompletedLines() {
        int linesClearedThisDrop = 0;
        for (int y = GRID_HEIGHT - 1; y >= 0; y--) {
            boolean full = true;
            for (int x = 0; x < GRID_WIDTH; x++) {
                if (grid[y][x] == 0) { full = false; break; }
            }
            if (full) {
                linesClearedThisDrop++;
                // Shift everything down
                for (int row = y; row > 0; row--) {
                    System.arraycopy(grid[row - 1], 0, grid[row], 0, GRID_WIDTH);
                }
                // Clear top row
                for (int x = 0; x < GRID_WIDTH; x++) grid[0][x] = 0;
                y++; // re-check same y after shifting
            }
        }
        if (linesClearedThisDrop > 0) {
            addScoreForLines(linesClearedThisDrop);
        }
    }

    private void addScoreForLines(int lines){
        score += 10*lines;
        updateLevel();
        updateStatsLabels();
    }
    private void addScoreForLand(){
        score += 1;
        updateLevel();
        updateStatsLabels();
    }

    private void updateLevel(){
        int newLevel = score/50+1;
        if (newLevel != level){
            level = newLevel;
            updateFallingSpeed();
        }
    }

    private void updateStatsLabels(){
        scoreValue.setText(String.valueOf(score));
        levelValue.setText(String.valueOf(level));
    }

    private void updateFallingSpeed(){
        long baseInterval = 500_000_000L; // 0.5s
        long speedupPer2Levels = 50_000_000L; // 0.05s per 2 levels (gentle)
        int speedups = (level - 1) / 2;
        fallIntervalNs = Math.max(100_000_000L, baseInterval - speedupPer2Levels * speedups);
    }

    public void drawGame() {
        gc.clearRect(0, 0, gameCanvas.getWidth(), gameCanvas.getHeight());

        gc.setFill(Color.LIGHTGRAY);
        gc.fillRect(0, 0, GRID_WIDTH * CELL_SIZE, GRID_HEIGHT * CELL_SIZE);

        gc.setStroke(Color.DARKGRAY);
        gc.setLineWidth(0.5);
        for (int x = 0; x <= GRID_WIDTH; x++) {
            gc.strokeLine(x * CELL_SIZE, 0, x * CELL_SIZE, GRID_HEIGHT * CELL_SIZE);
        }
        for (int y = 0; y <= GRID_HEIGHT; y++) {
            gc.strokeLine(0, y * CELL_SIZE, GRID_WIDTH * CELL_SIZE, y * CELL_SIZE);
        }

        // Fixed blocks
        for (int y = 0; y < GRID_HEIGHT; y++) {
            for (int x = 0; x < GRID_WIDTH; x++) {
                if (grid[y][x] > 0) {
                    drawBlock(x, y, getColor(grid[y][x]));
                }
            }
        }

        // Falling piece
        if (currentTetramino != null) {
            int[][] shape = currentTetramino.getShape();
            int baseX = currentTetramino.getX();
            int baseY = currentTetramino.getY();
            Color color = getColor(currentColorIndex);

            for (int r = 0; r < shape.length; r++) {
                for (int c = 0; c < shape[r].length; c++) {
                    if (shape[r][c] != 0) {
                        int gx = baseX + c;
                        int gy = baseY + r;
                        if (gy >= 0 && gy < GRID_HEIGHT && gx >= 0 && gx < GRID_WIDTH) {
                            drawBlock(gx, gy, color);
                        }
                    }
                }
            }
        }
    }
    private void drawNextPiecePreview(){
        GraphicsContext ngc = nextPieceCanvas.getGraphicsContext2D();
        ngc.clearRect(0, 0, nextPieceCanvas.getWidth(), nextPieceCanvas.getHeight());

        if (difficulty.equalsIgnoreCase("HARD")){
            ngc.setFont(Font.font("ARIAL", FontWeight.BOLD, 48));
            ngc.setFill(Color.DARKRED);
            ngc.fillText("\uD83D\uDC80", 20, 56); // Skull emoji
            return;
        }

        if (nextTetramino != null) {
            int[][] shape = nextTetramino.getShape();
            Color color = getColor(nextColorIndex);

            int boxSize = 18;
            int shapeWidth = shape[0].length;
            int shapeHeight = shape.length;

            int offsetX = (int)(nextPieceCanvas.getWidth() - shapeWidth * boxSize) / 2;
            int offsetY = (int)(nextPieceCanvas.getHeight() - shapeHeight * boxSize) / 2;

            for (int r = 0; r < shapeHeight; r++) {
                for (int c = 0; c < shapeWidth; c++) {
                    if (shape[r][c] != 0) {
                        int px = offsetX + c * boxSize;
                        int py = offsetY + r * boxSize;
                        ngc.setFill(color);
                        ngc.fillRect(px, py, boxSize - 2, boxSize - 2);

                        ngc.setStroke(color.brighter());
                        ngc.setLineWidth(2);
                        ngc.strokeRect(px, py, boxSize - 2, boxSize - 2);
                    }
                }
            }
        }
    }

    private void drawBlock(int x, int y, Color color) {
        int px = x * CELL_SIZE;
        int py = y * CELL_SIZE;

        gc.setFill(color);
        gc.fillRect(px + 1, py + 1, CELL_SIZE - 2, CELL_SIZE - 2);

        gc.setStroke(color.brighter());
        gc.setLineWidth(2);
        gc.strokeLine(px + 1, py + 1, px + CELL_SIZE - 1, py + 1);
        gc.strokeLine(px + 1, py + 1, px + 1, py + CELL_SIZE - 1);

        gc.setStroke(color.darker());
        gc.strokeLine(px + CELL_SIZE - 1, py + 1, px + CELL_SIZE - 1, py + CELL_SIZE - 1);
        gc.strokeLine(px + 1, py + CELL_SIZE - 1, px + CELL_SIZE - 1, py + CELL_SIZE - 1);
    }

    private Color getColor(int value) {
        return switch (value) {
            case 1 -> Color.CYAN;
            case 2 -> Color.YELLOW;
            case 3 -> Color.PURPLE;
            case 4 -> Color.GREEN;
            case 5 -> Color.RED;
            case 6 -> Color.BLUE;
            case 7 -> Color.ORANGE;
            default -> Color.GRAY;
        };
    }
}