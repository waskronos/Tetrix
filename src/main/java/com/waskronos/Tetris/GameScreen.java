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

    private boolean isPaused = false;
    private BorderPane pauseOverlay;

    // Use the class field for timing so we can reset it on resume
    private long lastFall = 0;

    // Grid stores 0 = empty, 1..7 = fixed block with color id
    private int[][] grid = new int[GRID_HEIGHT][GRID_WIDTH];

    private Tetramino currentTetramino;
    private int currentColorIndex;

    private Tetramino nextTetramino;
    private int nextColorIndex;  // 1..7 matches getColor
    private Random random = new Random();

    // Timing (controlled by config + level)
    private long fallIntervalNs = 500_000_000L;    // current interval
    private long baseFallIntervalNs = 500_000_000L;
    private static final long BASE_MAX_INTERVAL_NS = 700_000_000L; // ~0.7s at speed=1
    private static final long BASE_MIN_INTERVAL_NS = 70_000_000L;  // ~0.07s at speed=10
    private static final long MIN_INTERVAL_NS      = 50_000_000L;  // absolute clamp ~0.05s
    private static final double LEVEL_ACCEL_FACTOR = 0.85;         // 15% faster each level

    // Leveling by lines (classic)
    private int totalLinesCleared = 0;
    private int linesPerLevel = 10; // every 10 lines, next level

    private int score = 0;
    private int level = 1;
    private Label scoreValue;
    private Label levelValue;

    private String difficulty = "MEDIUM";
    private Canvas nextPieceCanvas;

    public GameScreen(TetrisApp app) {
        this.app = app;

        // Pull difficulty from app for UI and preview behavior
        this.difficulty = (app.getDifficulty() == null) ? "MEDIUM" : app.getDifficulty().trim().toUpperCase();
        // Compute base fall interval from Config (speed + difficulty), then set current
        baseFallIntervalNs = computeFallIntervalNs(app.getGameSpeed(), app.getDifficulty());
        fallIntervalNs = baseFallIntervalNs;
        initializeGrid();

        // Compute base fall interval from config (speed + difficulty), then set current
        baseFallIntervalNs = computeFallIntervalNs(app.getGameSpeed(), app.getDifficulty());
        fallIntervalNs = baseFallIntervalNs;

        HBox gameBox = new HBox(20);
        gameBox.setAlignment(Pos.CENTER);

        gameCanvas = new Canvas(GRID_WIDTH * CELL_SIZE, GRID_HEIGHT * CELL_SIZE);
        gc = gameCanvas.getGraphicsContext2D();

        VBox statsPanel = createStatsPanel();
        gameBox.getChildren().addAll(gameCanvas, statsPanel);
        setCenter(gameBox);
        BorderPane.setMargin(gameBox, new Insets(20));

        HBox buttonBox = new HBox(20);
        buttonBox.setAlignment(Pos.CENTER);

        Button pauseButton = new Button("Pause");
        pauseButton.setOnAction(e -> togglePause());

        Button backButton = new Button("Back to Main Menu");
        backButton.setOnAction(e -> {
            stopGame();
            app.showMainScreen();
        });

        buttonBox.getChildren().addAll(pauseButton, backButton);
        setBottom(buttonBox);
        BorderPane.setAlignment(buttonBox, Pos.CENTER);
        BorderPane.setMargin(buttonBox, new Insets(20));

        // Create overlay after layout is set
        createPauseOverlay();

        setupInputHandlers();
        generateNextTetramino();
        spawnTetramino();
        startGame();

        // Ensure the canvas has focus so key events work
        gameCanvas.requestFocus();
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
        // Key handlers are attached to the canvas; give it focus
        gameCanvas.setFocusTraversable(true);

        gameCanvas.setOnKeyPressed(e -> {
            if (currentTetramino == null) return;
            KeyCode code = e.getCode();

            // When paused, ignore all except unpause
            if (isPaused && code != KeyCode.P && code != KeyCode.ESCAPE) return;

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
                    // Optional rotate with W if you want
                    // currentTetramino.rotateCW(); if (willCollide(0,0)) currentTetramino.rotateCCW();
                }
                case SPACE -> {
                    // Hard drop
                    while (!willCollide(0, 1)) {
                        currentTetramino.move(0, 1);
                    }
                    lockAndSpawn();
                }
                case LEFT -> {
                    currentTetramino.rotateCCW();
                    if (willCollide(0, 0)) {
                        currentTetramino.rotateCW();
                    }
                }
                case RIGHT -> {
                    currentTetramino.rotateCW();
                    if (willCollide(0, 0)) {
                        currentTetramino.rotateCCW();
                    }
                }
                case P, ESCAPE -> togglePause();
            }
        });

        // Make sure the canvas keeps focus when the mouse enters it
        gameCanvas.setOnMouseEntered(e -> gameCanvas.requestFocus());
    }

    private void startGame() {
        lastFall = 0; // fresh start
        loop = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (isPaused) return;

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

    private void createPauseOverlay() {
        pauseOverlay = new BorderPane();
        pauseOverlay.setStyle("-fx-background-color: rgba(0, 0, 0, 0.7);");
        pauseOverlay.prefWidthProperty().bind(widthProperty());
        pauseOverlay.prefHeightProperty().bind(heightProperty());

        VBox content = new VBox(20);
        content.setAlignment(Pos.CENTER);

        Label pauseLabel = new Label("GAME PAUSED");
        pauseLabel.setFont(Font.font("Arial", FontWeight.BOLD, 36));
        pauseLabel.setTextFill(Color.WHITE);

        Button resumeButton = new Button("Resume");
        resumeButton.setOnAction(e -> togglePause());

        Button mainMenuButton = new Button("Main Menu");
        mainMenuButton.setOnAction(e -> {
            stopGame();
            app.showMainScreen();
        });

        content.getChildren().addAll(pauseLabel, resumeButton, mainMenuButton);
        pauseOverlay.setCenter(content);

        pauseOverlay.setVisible(false);
        pauseOverlay.setManaged(false);
        getChildren().add(pauseOverlay);
    }

    private void togglePause() {
        isPaused = !isPaused;
        if (isPaused) {
            if (loop != null) loop.stop();
            pauseOverlay.setVisible(true);
            pauseOverlay.setManaged(true);
            pauseOverlay.toFront();
        } else {
            pauseOverlay.setVisible(false);
            pauseOverlay.setManaged(false);
            // Prevent an immediate drop after resuming
            lastFall = System.nanoTime();
            if (loop != null) loop.start();
        }
    }

    private void generateNextTetramino(){
        int[][][] shapes = {
                {{1, 1, 1, 1}},          // I
                {{1, 1}, {1, 1}},        // O
                {{0, 1, 0}, {1, 1, 1}},  // T
                {{0, 1, 1}, {1, 1, 0}},  // S
                {{1, 1, 0}, {0, 1, 1}},  // Z
                {{1, 0, 0}, {1, 1, 1}},  // J
                {{0, 0, 1}, {1, 1, 1}}   // L
        };
        nextColorIndex = random.nextInt(shapes.length) + 1;
        int[][] shape = shapes[nextColorIndex - 1];
        // Centered at (0,0) for preview
        nextTetramino = new Tetramino(shape, 0, 0);
    }

    private void spawnTetramino() { // now we use the next tetramino as the current tetramino to spawn
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

    // Scale speed from the configured base, not a hard-coded 0.5s
    private void updateFallingSpeed(){
        // Every 2 levels, speed up by 10% (tweak to taste)
        int speedups = Math.max(0, (level - 1) / 2);
        double factor = Math.pow(0.9, speedups);
        long newInterval = (long) (baseFallIntervalNs * Math.pow(LEVEL_ACCEL_FACTOR, Math.max(0, level - 1)));
        // Clamp to a reasonable minimum
        fallIntervalNs = Math.max(MIN_INTERVAL_NS, newInterval);
    }

    private long computeFallIntervalNs(int speed1to10, String difficulty) {
        // Map speed 1..10 to 0.7s..0.07s linearly
        int s = Math.min(10, Math.max(1, speed1to10));
        double t = (s - 1) / 9.0; // 0..1
        long base = (long) (BASE_MAX_INTERVAL_NS - t * (BASE_MAX_INTERVAL_NS - BASE_MIN_INTERVAL_NS));

        // Difficulty multiplier
        String d = difficulty == null ? "MEDIUM" : difficulty.trim().toUpperCase();
        double mult = switch (d) {
            case "EASY" -> 1.1;  // a bit slower
            case "HARD" -> 0.85; // a bit faster
            default -> 1.0;      // MEDIUM
        };

        long result = (long) (base * mult);
        return Math.max(MIN_INTERVAL_NS, result);
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