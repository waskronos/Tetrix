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
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.media.AudioClip;
import java.util.Random;

public class GameScreen extends BorderPane {

    private final TetrisApp app;
    private final Canvas gameCanvas;
    private final GraphicsContext gc;
    private AnimationTimer loop;

    private final int GRID_WIDTH = 10;
    private final int GRID_HEIGHT = 20;
    private final int CELL_SIZE = 30;

    private boolean isPaused = false;
    private BorderPane pauseOverlay;

    private long lastFall = 0;

    private final int[][] grid = new int[GRID_HEIGHT][GRID_WIDTH];

    private Tetramino currentTetramino;
    private int currentColorIndex;

    private Tetramino nextTetramino;
    private int nextColorIndex;  // 1..7 matches getColor
    private final Random random = new Random();

    private long fallIntervalNs = 500_000_000L;
    private long baseFallIntervalNs = 500_000_000L;
    private static final long BASE_MAX_INTERVAL_NS = 700_000_000L;
    private static final long BASE_MIN_INTERVAL_NS = 70_000_000L;
    private static final long MIN_INTERVAL_NS      = 50_000_000L;
    private static final double LEVEL_ACCEL_FACTOR = 0.80;

    private int totalLinesCleared = 0;
    private int linesPerLevel = 10;

    private int score = 0;
    private int level = 1;
    private Label scoreValue;
    private Label levelValue;

    private String difficulty = "MEDIUM";
    private Canvas nextPieceCanvas;

    private boolean isGameOver = false;
    private double gameOverY = -60;

    private Button backButton;
    private Button pauseButton;

    private Font ui20;
    private Font ui18;

    private AudioClip sfxSlam, sfxLand, sfxLine, sfxLevel, sfxMove, sfxGameOver;
    private boolean wasSlam = false;

    private StackPane centerStack;

    public GameScreen(TetrisApp app) {
        this.app = app;
        this.app.enterGame(); //Mute BGM while in game

        this.difficulty = (app.getDifficulty() == null) ? "MEDIUM" : app.getDifficulty().trim().toUpperCase();

        ui20 = Font.loadFont(getClass().getResourceAsStream("/fonts/pixelbold.ttf"), 20);
        ui18 = Font.loadFont(getClass().getResourceAsStream("/fonts/pixel.ttf"), 18);
        if (ui20 == null) ui20 = Font.font("Arial", 20);
        if (ui18 == null) ui18 = Font.font("Arial", 18);

        var u1 = getClass().getResource("/sounds/slam.wav");
        if (u1 != null) sfxSlam = new AudioClip(u1.toExternalForm());
        var u2 = getClass().getResource("/sounds/land.mp3");
        if (u2 != null) sfxLand = new AudioClip(u2.toExternalForm());
        var u3 = getClass().getResource("/sounds/clear.wav");
        if (u3 != null) sfxLine = new AudioClip(u3.toExternalForm());
        var u4 = getClass().getResource("/sounds/lvup.wav");
        if (u4 != null) sfxLevel = new AudioClip(u4.toExternalForm());
        var u5 = getClass().getResource("/sounds/move.wav");
        if (u5 != null) sfxMove = new AudioClip(u5.toExternalForm());
        var u6 = getClass().getResource("/sounds/gameover.mp3");
        if (u6 != null) sfxGameOver = new AudioClip(u6.toExternalForm());

        baseFallIntervalNs = computeFallIntervalNs(app.getGameSpeed(), app.getDifficulty());
        fallIntervalNs = baseFallIntervalNs;

        initializeGrid();

        HBox gameBox = new HBox(20);
        gameBox.setAlignment(Pos.CENTER);

        gameCanvas = new Canvas(GRID_WIDTH * CELL_SIZE, GRID_HEIGHT * CELL_SIZE);
        gc = gameCanvas.getGraphicsContext2D();

        VBox statsPanel = createStatsPanel();
        gameBox.getChildren().addAll(gameCanvas, statsPanel);

        createPauseOverlay();

        centerStack = new StackPane();
        centerStack.getChildren().addAll(gameBox, pauseOverlay);
        setCenter(centerStack);
        BorderPane.setMargin(centerStack, new Insets(20));

        HBox buttonBox = new HBox(20);
        buttonBox.setAlignment(Pos.CENTER);

        pauseButton = new Button("Pause");
        pauseButton.setOnAction(e -> handlePauseOrRestart());

        backButton = new Button("Back to Main Menu");
        backButton.setOnAction(e -> {
            stopGame();
            app.exitGame(); // Unmute BGM when exiting
            app.showMainScreen();
        });

        buttonBox.getChildren().addAll(pauseButton, backButton);
        setBottom(buttonBox);
        BorderPane.setAlignment(buttonBox, Pos.CENTER);
        BorderPane.setMargin(buttonBox, new Insets(20));

        setupInputHandlers();
        generateNextTetramino();
        spawnTetramino();
        startGame();

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

        Label nextLabel = new Label("NEXT");
        nextLabel.setFont(ui20);
        nextPieceCanvas = new Canvas(80, 80);

        Label scoreLabel = new Label("SCORE");
        scoreLabel.setFont(ui20);
        scoreValue = new Label("0");
        scoreValue.setFont(ui18);

        Label levelLabel = new Label("LEVEL");
        levelLabel.setFont(ui20);
        levelValue = new Label("1");
        levelValue.setFont(ui18);

        Label diffiLabel = new Label("DIFFICULTY");
        diffiLabel.setFont(ui20);
        Label diffiValue = new Label(difficulty);
        diffiValue.setFont(ui18);

        statsPanel.getChildren().addAll(
                nextLabel, nextPieceCanvas,
                scoreLabel, scoreValue,
                levelLabel, levelValue,
                diffiLabel, diffiValue
        );
        return statsPanel;
    }

    private void setupInputHandlers() {
        gameCanvas.setFocusTraversable(true);

        gameCanvas.setOnKeyPressed(e -> {
            if (currentTetramino == null) return;
            KeyCode code = e.getCode();

            if (isPaused && code != KeyCode.P && code != KeyCode.ESCAPE) return;

            switch (code) {
                case A -> {
                    if (!willCollide(-1, 0)) currentTetramino.move(-1, 0);
                    if (app.isSoundEffectsEnabled() && sfxMove != null) sfxMove.play();
                }
                case D -> {
                    if (!willCollide(1, 0)) currentTetramino.move(1, 0);
                    if (app.isSoundEffectsEnabled() && sfxMove != null) sfxMove.play();
                }
                case S -> {
                    if (!willCollide(0, 1)) {
                        currentTetramino.move(0, 1);
                        if (app.isSoundEffectsEnabled() && sfxMove != null) sfxMove.play();
                    } else {
                        lockAndSpawn();
                    }
                }
                case SPACE -> {
                    wasSlam = true;
                    while (!willCollide(0, 1)) {
                        currentTetramino.move(0, 1);
                    }
                    lockAndSpawn();
                }
                case LEFT -> {
                    currentTetramino.rotateCCW();
                    if (willCollide(0, 0)) currentTetramino.rotateCW();
                }
                case RIGHT -> {
                    currentTetramino.rotateCW();
                    if (willCollide(0, 0)) currentTetramino.rotateCCW();
                }
                case P, ESCAPE -> togglePause();
            }
        });

        gameCanvas.setOnMouseEntered(e -> gameCanvas.requestFocus());
    }

    private void startGame() {
        lastFall = 0;
        loop = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (isPaused) return;

                if (lastFall == 0) lastFall = now;

                if (!isGameOver && now - lastFall >= fallIntervalNs) {
                    updateGame();
                    lastFall = now;
                } else if (isGameOver) {
                    double targetY = (GRID_HEIGHT * CELL_SIZE) / 2.0 - 20;
                    if (gameOverY < targetY) gameOverY += 6;
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
        pauseOverlay.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        pauseOverlay.setPickOnBounds(true);

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
            app.exitGame(); // Unmute BGM
            app.showMainScreen();
        });

        content.getChildren().addAll(pauseLabel, resumeButton, mainMenuButton);
        pauseOverlay.setCenter(content);

        pauseOverlay.setVisible(false);
        pauseOverlay.setManaged(false);
    }

    private void handlePauseOrRestart() {
        if (isGameOver) {
            restartGame();
        } else {
            togglePause();
        }
    }

    private void restartGame() {
        isGameOver = false;
        isPaused = false;
        wasSlam = false;
        gameOverY = -60;

        pauseOverlay.setVisible(false);
        pauseOverlay.setManaged(false);

        initializeGrid();
        score = 0;
        level = 1;
        totalLinesCleared = 0;
        updateStatsLabels();

        fallIntervalNs = baseFallIntervalNs;
        lastFall = 0;

        pauseButton.setText("Pause");
        pauseButton.setDisable(false);
        backButton.setDisable(false);

        generateNextTetramino();
        spawnTetramino();
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
            lastFall = System.nanoTime();
            if (loop != null) loop.start();
        }
    }

    private void generateNextTetramino(){
        int[][][] shapes = {
                {{1, 1, 1, 1}},
                {{1, 1}, {1, 1}},
                {{0, 1, 0}, {1, 1, 1}},
                {{0, 1, 1}, {1, 1, 0}},
                {{1, 1, 0}, {0, 1, 1}},
                {{1, 0, 0}, {1, 1, 1}},
                {{0, 0, 1}, {1, 1, 1}}
        };
        nextColorIndex = random.nextInt(shapes.length) + 1;
        int[][] shape = shapes[nextColorIndex - 1];
        nextTetramino = new Tetramino(shape, 0, 0);
    }

    private void spawnTetramino() {
        currentTetramino = nextTetramino;
        currentColorIndex = nextColorIndex;
        generateNextTetramino();

        int startX = (GRID_WIDTH - currentTetramino.getShape()[0].length) / 2;
        int startY = 0;
        currentTetramino.setPosition(startX, startY);

        if (willCollide(0, 0)) {
            isGameOver = true;
            currentTetramino = null;
            gameOverY = -60;

            if (app.isSoundEffectsEnabled() && sfxGameOver != null) sfxGameOver.play();

            if (pauseButton != null) {
                pauseButton.setText("Restart");
                pauseButton.setDisable(false);
            }
            if (backButton != null) {
                backButton.setDisable(false);
            }
            return;
        }
    }

    private void updateGame() {
        if (currentTetramino == null) return;

        if (willCollide(0, 1)) {
            lockAndSpawn();
        } else {
            currentTetramino.move(0, 1);
        }
    }

    private void lockAndSpawn() {
        if (app.isSoundEffectsEnabled()) {
            if (wasSlam) {
                if (sfxSlam != null) sfxSlam.play();
            } else {
                if (sfxLand != null) sfxLand.play();
            }
        }
        wasSlam = false;
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

                if (gridY >= GRID_HEIGHT) return true;
                if (gridX < 0 || gridX >= GRID_WIDTH) return true;
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
                for (int row = y; row > 0; row--) {
                    System.arraycopy(grid[row - 1], 0, grid[row], 0, GRID_WIDTH);
                }
                for (int x = 0; x < GRID_WIDTH; x++) grid[0][x] = 0;
                y++;
            }
        }
        if (linesClearedThisDrop > 0) {
            if (app.isSoundEffectsEnabled() && sfxLine != null) sfxLine.play();
            addScoreForLines(linesClearedThisDrop);
        }
    }

    private void addScoreForLines(int lines){
        score += 40*lines;
        updateLevel();
        updateStatsLabels();
    }
    private void addScoreForLand(){
        score += 5;
        updateLevel();
        updateStatsLabels();
    }

    private void updateLevel(){
        int newLevel = score/150+1;
        if (newLevel != level){
            level = newLevel;
            updateFallingSpeed();
            if (app.isSoundEffectsEnabled() && sfxLevel != null) sfxLevel.play();
        }
    }

    private void updateStatsLabels(){
        scoreValue.setText(String.valueOf(score));
        levelValue.setText(String.valueOf(level));
    }

    private void updateFallingSpeed(){
        long newInterval = (long) (baseFallIntervalNs * Math.pow(LEVEL_ACCEL_FACTOR, Math.max(0, level - 1)));
        fallIntervalNs = Math.max(MIN_INTERVAL_NS, newInterval);
    }

    private long computeFallIntervalNs(int speed1to10, String difficulty) {
        int s = Math.min(10, Math.max(1, speed1to10));
        double t = (s - 1) / 9.0;
        long base = (long) (BASE_MAX_INTERVAL_NS - t * (BASE_MAX_INTERVAL_NS - BASE_MIN_INTERVAL_NS));

        String d = difficulty == null ? "MEDIUM" : difficulty.trim().toUpperCase();
        double mult = switch (d) {
            case "EASY" -> 1.1;
            case "HARD" -> 0.85;
            default -> 1.0;
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

        for (int y = 0; y < GRID_HEIGHT; y++) {
            for (int x = 0; x < GRID_WIDTH; x++) {
                if (grid[y][x] > 0) {
                    drawBlock(x, y, getColor(grid[y][x]));
                }
            }
        }

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

        if (isGameOver) {
            gc.setFill(Color.color(0, 0, 0, 0.6));
            gc.fillRect(0, 0, GRID_WIDTH * CELL_SIZE, GRID_HEIGHT * CELL_SIZE);

            gc.setFont(ui20 != null ? ui20 : Font.font("ARIAL", 20));
            gc.setFill(Color.WHITE);

            double x = (GRID_WIDTH * CELL_SIZE) / 2.0 - 100;
            gc.fillText("GAME OVER", x, gameOverY);
            gc.fillText("Score: " + score, x, gameOverY + 30);
            gc.fillText("Level: " + level, x, gameOverY + 60);
        }
    }

    private void drawNextPiecePreview(){
        GraphicsContext ngc = nextPieceCanvas.getGraphicsContext2D();
        ngc.clearRect(0, 0, nextPieceCanvas.getWidth(), nextPieceCanvas.getHeight());

        if (difficulty.equalsIgnoreCase("HARD")){
            ngc.setFont(Font.font("ARIAL", FontWeight.BOLD, 48));
            ngc.setFill(Color.DARKRED);
            ngc.fillText("\uD83D\uDC80", 20, 56);
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