package com.waskronos.Tetris.ui;

import com.waskronos.Tetris.app.TetrisApp;
import com.waskronos.Tetris.audio.AudioService;
import com.waskronos.Tetris.audio.DefaultAudioService;
import com.waskronos.Tetris.core.Tetramino;
import com.waskronos.Tetris.core.TetrominoFactory;
import com.waskronos.Tetris.core.TetrominoType;
import com.waskronos.Tetris.random.BagRandomizer;
import com.waskronos.Tetris.random.PieceRandomizer;
import com.waskronos.Tetris.settings.SettingsManager;
import com.waskronos.Tetris.store.HighScoresStore;
import com.waskronos.Tetris.net.ServerConnection;
import javafx.animation.AnimationTimer;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextInputDialog;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.util.Duration;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Optional;

public class GameScreen extends BorderPane {

    private final TetrisApp app;
    private final SettingsManager settings = SettingsManager.getInstance();

    private final TetrominoFactory factory = new TetrominoFactory();
    private final PieceRandomizer randomizer;

    private final AudioService audio = new DefaultAudioService(settings);

    private final Canvas gameCanvas;
    private final GraphicsContext gc;
    private AnimationTimer loop;

    private final int gridWidth;
    private final int gridHeight;
    private final int cellSize;

    private long lastFall = 0;
    private long fallIntervalNs = 500_000_000L;
    private long baseFallIntervalNs = 500_000_000L;
    private static final long BASE_MAX_INTERVAL_NS = 700_000_000L;
    private static final long BASE_MIN_INTERVAL_NS = 70_000_000L;
    private static final long MIN_INTERVAL_NS = 50_000_000L;
    private static final double LEVEL_ACCEL_FACTOR = 0.80;

    private final int[][] grid;

    private Tetramino currentTetramino;
    private int currentColorIndex;
    private TetrominoType currentType;

    private Tetramino nextTetramino;
    private int nextColorIndex;
    private TetrominoType nextType;

    private int score = 0;
    private int level = 1;

    private boolean isPaused = false;
    private boolean isGameOver = false;
    private double gameOverY = -60;

    private Button backButton;
    private Button pauseButton;
    private BorderPane pauseOverlay;

    private Label scoreValue;
    private Label levelValue;
    private Label musicValue;
    private Label sfxValue;
    private Canvas nextPieceCanvas;

    private Font ui20;
    private Font ui18;

    private String difficulty = "MEDIUM";
    private String playerName = "PLAYER";

    private boolean aiActive = false;
    private Label aiBadge;
    private final Deque<Runnable> aiPlan = new ArrayDeque<>();
    private long nextAiActionAtNs = 0;
    private static final long AI_STEP_INTERVAL_NS = 80_000_000L;

    private boolean highScorePromptEnabled = true;

    private boolean externalControlled = false;
    private boolean serverConnected = false;
    private Label serverLabel;
    private Timeline serverPoll;

    public GameScreen(TetrisApp app) {
        this(app, new BagRandomizer(), true);
    }

    public GameScreen(TetrisApp app, PieceRandomizer randomizer, boolean registerKeys) {
        this.app = app;
        this.randomizer = randomizer;
        this.app.enterGame();

        var cssUrl = getClass().getResource("/styles/app.css");
        if (cssUrl != null) getStylesheets().add(cssUrl.toExternalForm());

        this.gridWidth = Math.max(6, settings.getBoardWidth());
        this.gridHeight = Math.max(10, settings.getBoardHeight());
        this.cellSize = Math.max(8, settings.getCellSize());
        this.grid = new int[gridHeight][gridWidth];

        this.difficulty = (app.getDifficulty() == null) ? "MEDIUM" : app.getDifficulty().trim().toUpperCase();
        this.baseFallIntervalNs = computeFallIntervalNs(app.getGameSpeed(), difficulty);
        this.fallIntervalNs = baseFallIntervalNs;

        ui20 = Font.font("Arial", 20);
        ui18 = Font.font("Arial", 18);

        HBox centerHBox = new HBox(20);
        centerHBox.setAlignment(Pos.CENTER);
        gameCanvas = new Canvas(gridWidth * cellSize, gridHeight * cellSize);
        gc = gameCanvas.getGraphicsContext2D();

        VBox statsPanel = createStatsPanel();
        centerHBox.getChildren().addAll(gameCanvas, statsPanel);

        createPauseOverlay();

        StackPane centerStack = new StackPane(centerHBox, pauseOverlay);
        setCenter(centerStack);
        BorderPane.setMargin(centerStack, new Insets(20));

        HBox buttonBox = new HBox(20);
        buttonBox.setAlignment(Pos.CENTER);

        pauseButton = new Button("Pause");
        pauseButton.setOnAction(e -> handlePauseOrRestart());

        backButton = new Button("Back to Main Menu");
        backButton.setOnAction(e -> {
            stopGame();
            app.exitGame();
            app.showMainScreen();
        });

        buttonBox.getChildren().addAll(pauseButton, backButton);
        setBottom(buttonBox);
        BorderPane.setAlignment(buttonBox, Pos.CENTER);
        BorderPane.setMargin(buttonBox, new Insets(20));

        if (registerKeys) setupInputHandlers();

        generateNextTetramino();
        spawnTetramino();
        startGame();

        gameCanvas.setFocusTraversable(true);
        gameCanvas.requestFocus();
    }

    public void setHighScorePromptEnabled(boolean enabled) {
        this.highScorePromptEnabled = enabled;
    }

    public void setAiActive(boolean active) {
        this.aiActive = active;
        if (aiBadge != null) {
            aiBadge.setVisible(active);
            aiBadge.setManaged(active);
        }
        aiPlan.clear();
    }

    public void setExternalControlled(boolean external) {
        this.externalControlled = external;
        ensureServerPoll();
        if (serverLabel != null) {
            serverLabel.setVisible(external);
            serverLabel.setManaged(external);
        }
    }

    public void setPlayerName(String name) {
        if (name != null && !name.isBlank()) {
            this.playerName = name;
        }
    }

    public int getScore() { return score; }
    public int getLevel() { return level; }
    public String getPlayerName() { return playerName; }

    private VBox createStatsPanel() {
        VBox statsPanel = new VBox(16);
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

        Label musicLabel = new Label("MUSIC M");
        musicLabel.setFont(ui20);
        musicValue = new Label(settings.isMusicEnabled() ? "ON" : "OFF");
        musicValue.setFont(ui18);

        Label sfxLabel = new Label("SFX N");
        sfxLabel.setFont(ui20);
        sfxValue = new Label(settings.isSfxEnabled() ? "ON" : "OFF");
        sfxValue.setFont(ui18);

        aiBadge = new Label("AI ACTIVE");
        aiBadge.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
        aiBadge.setVisible(false);
        aiBadge.setManaged(false);

        serverLabel = new Label("SERVER: OFFLINE");
        serverLabel.setStyle("-fx-text-fill: orange; -fx-font-weight: bold;");
        serverLabel.setVisible(false);
        serverLabel.setManaged(false);

        statsPanel.getChildren().addAll(
                nextLabel, nextPieceCanvas,
                scoreLabel, scoreValue,
                levelLabel, levelValue,
                diffiLabel, diffiValue,
                musicLabel, musicValue,
                sfxLabel, sfxValue,
                aiBadge,
                serverLabel
        );
        return statsPanel;
    }

    private void setupInputHandlers() {
        gameCanvas.setOnKeyPressed(e -> {
            KeyCode code = e.getCode();
            switch (code) {
                case A, D, S, Q, E, SPACE, LEFT, RIGHT, DOWN -> {
                    if (aiActive) setAiActive(false);
                }
                default -> {}
            }
            handleKeyPressed(code);
        });
    }

    private void handleKeyPressed(KeyCode code) {
        if (externalControlled && !serverConnected) return;
        if (currentTetramino == null) return;
        if (isPaused && code != KeyCode.ESCAPE) return;

        switch (code) {
            case A -> moveLeft();
            case D -> moveRight();
            case S -> softDrop();
            case SPACE -> hardDrop();
            case Q, LEFT -> rotateCCW();
            case E, RIGHT -> rotateCW();
            case M -> {
                boolean newVal = !settings.isMusicEnabled();
                settings.setMusicEnabled(newVal);
                app.setMusicEnabled(newVal);
                updateAudioLabels();
            }
            case N -> {
                boolean newVal = !settings.isSfxEnabled();
                settings.setSfxEnabled(newVal);
                app.setSoundEffectsEnabled(newVal);
                audio.setSfxEnabled(newVal);
                updateAudioLabels();
            }
            case ESCAPE -> togglePause();
            default -> {}
        }
    }

    private void updateAudioLabels() {
        if (musicValue != null) musicValue.setText(settings.isMusicEnabled() ? "ON" : "OFF");
        if (sfxValue != null) sfxValue.setText(settings.isSfxEnabled() ? "ON" : "OFF");
    }

    private void startGame() {
        lastFall = 0;
        nextAiActionAtNs = 0;
        loop = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (!isPaused) {
                    if (lastFall == 0) lastFall = now;

                    if (!isGameOver && now - lastFall >= fallIntervalNs) {
                        updateGame();
                        lastFall = now;
                    } else if (isGameOver) {
                        double targetY = (gridHeight * cellSize) / 2.0 - 20;
                        if (gameOverY < targetY) gameOverY += 6;
                    }

                    if (aiActive && !aiPlan.isEmpty() && now >= nextAiActionAtNs) {
                        Runnable step = aiPlan.pollFirst();
                        if (step != null) step.run();
                        nextAiActionAtNs = now + AI_STEP_INTERVAL_NS;
                    }
                }

                drawGame();
                drawNextPiecePreview();
            }
        };
        loop.start();
    }

    private void stopGame() {
        if (loop != null) loop.stop();
        if (serverPoll != null) serverPoll.stop();
    }

    private void createPauseOverlay() {
        pauseOverlay = new BorderPane();
        pauseOverlay.setStyle("-fx-background-color: rgba(0,0,0,0.7);");
        pauseOverlay.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        pauseOverlay.setPickOnBounds(true);

        VBox content = new VBox(20);
        content.setAlignment(Pos.CENTER);

        Label pauseLabel = new Label("GAME PAUSED");
        pauseLabel.setFont(Font.font("Arial", 36));
        pauseLabel.setTextFill(Color.WHITE);

        Button resumeButton = new Button("Resume");
        resumeButton.setOnAction(e -> togglePause());

        Button mainMenuButton = new Button("Main Menu");
        mainMenuButton.setOnAction(e -> {
            stopGame();
            app.exitGame();
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

    public void restartGame() {
        isGameOver = false;
        isPaused = false;
        aiPlan.clear();
        gameOverY = -60;

        for (int y = 0; y < gridHeight; y++) {
            for (int x = 0; x < gridWidth; x++) {
                grid[y][x] = 0;
            }
        }

        score = 0;
        level = 1;
        updateStatsLabels();

        fallIntervalNs = baseFallIntervalNs;
        lastFall = 0;

        pauseOverlay.setVisible(false);
        pauseOverlay.setManaged(false);

        if (pauseButton != null) pauseButton.setText("Pause");

        generateNextTetramino();
        spawnTetramino();
        if (loop == null) startGame();
    }

    public void togglePause() {
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

    public void setControlsVisible(boolean visible) {
        if (pauseButton != null) {
            pauseButton.setVisible(visible);
            pauseButton.setManaged(visible);
        }
        if (backButton != null) {
            backButton.setVisible(visible);
            backButton.setManaged(visible);
        }
    }

    private void generateNextTetramino() {
        nextType = randomizer.next();
        nextColorIndex = codeFromType(nextType);
        nextTetramino = factory.create(nextType);
    }

    private void spawnTetramino() {
        currentTetramino = nextTetramino;
        currentType = nextType;
        currentColorIndex = nextColorIndex;

        generateNextTetramino();

        int startX = (gridWidth - currentTetramino.getShape()[0].length) / 2;
        int startY = 0;
        currentTetramino.setPosition(startX, startY);

        if (willCollide(0, 0)) {
            isGameOver = true;
            currentTetramino = null;
            gameOverY = -60;

            audio.playGameOver();
            com.waskronos.Tetris.events.GameEvents.getInstance()
                    .fireGameOver(new com.waskronos.Tetris.events.GameEvents.GameOver(
                            playerName, score, level, System.currentTimeMillis()
                    ));

            if (pauseButton != null) pauseButton.setText("Restart");

            if (highScorePromptEnabled) {
                promptHighScore();
            }
        } else if (aiActive) {
            planAiForCurrentPiece();
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
        audio.playLand();
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
        return collidesAt(baseX, baseY, shape);
    }

    private boolean collidesAt(int baseX, int baseY, int[][] shape) {
        for (int r = 0; r < shape.length; r++) {
            for (int c = 0; c < shape[r].length; c++) {
                if (shape[r][c] == 0) continue;
                int gx = baseX + c;
                int gy = baseY + r;
                if (gy >= gridHeight) return true;
                if (gx < 0 || gx >= gridWidth) return true;
                if (gy >= 0 && grid[gy][gx] != 0) return true;
            }
        }
        return false;
    }

    private int computeGhostY() {
        if (currentTetramino == null) return -1;
        int[][] shape = currentTetramino.getShape();
        int x = currentTetramino.getX();
        int y = currentTetramino.getY();
        int gy = y;
        while (!collidesAt(x, gy + 1, shape)) {
            gy++;
        }
        return gy;
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
                if (gy >= 0 && gy < gridHeight && gx >= 0 && gx < gridWidth) {
                    grid[gy][gx] = currentColorIndex;
                }
            }
        }
    }

    private void clearCompletedLines() {
        int linesClearedThisDrop = 0;
        for (int y = gridHeight - 1; y >= 0; y--) {
            boolean full = true;
            for (int x = 0; x < gridWidth; x++) {
                if (grid[y][x] == 0) { full = false; break; }
            }
            if (full) {
                linesClearedThisDrop++;
                for (int row = y; row > 0; row--) {
                    System.arraycopy(grid[row - 1], 0, grid[row], 0, gridWidth);
                }
                for (int x = 0; x < gridWidth; x++) grid[0][x] = 0;
                y++;
            }
        }
        if (linesClearedThisDrop > 0) {
            audio.playLine();
            addScoreForLines(linesClearedThisDrop);
        }
    }

    private void addScoreForLines(int lines) {
        score += 40 * lines;
        updateLevel();
        updateStatsLabels();
    }

    private void addScoreForLand() {
        score += 5;
        updateLevel();
        updateStatsLabels();
    }

    private void updateLevel() {
        int newLevel = score / 150 + 1;
        if (newLevel != level) {
            level = newLevel;
            updateFallingSpeed();
            audio.playLevelUp();
        }
    }

    private void updateStatsLabels() {
        if (scoreValue != null) scoreValue.setText(String.valueOf(score));
        if (levelValue != null) levelValue.setText(String.valueOf(level));
    }

    private void updateFallingSpeed() {
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

    public void moveLeft() {
        if (isPaused || isGameOver || currentTetramino == null) return;
        if (!willCollide(-1, 0)) currentTetramino.move(-1, 0);
        audio.playMove();
    }

    public void moveRight() {
        if (isPaused || isGameOver || currentTetramino == null) return;
        if (!willCollide(1, 0)) currentTetramino.move(1, 0);
        audio.playMove();
    }

    public void softDrop() {
        if (isPaused || isGameOver || currentTetramino == null) return;
        if (!willCollide(0, 1)) {
            currentTetramino.move(0, 1);
            audio.playMove();
        } else {
            lockAndSpawn();
        }
    }

    public void rotateCW() {
        if (isPaused || isGameOver || currentTetramino == null) return;
        currentTetramino.rotateCW();
        if (willCollide(0, 0)) currentTetramino.rotateCCW();
    }

    public void rotateCCW() {
        if (isPaused || isGameOver || currentTetramino == null) return;
        currentTetramino.rotateCCW();
        if (willCollide(0, 0)) currentTetramino.rotateCW();
    }

    public void hardDrop() {
        if (isPaused || isGameOver || currentTetramino == null) return;
        while (!willCollide(0, 1)) {
            currentTetramino.move(0, 1);
        }
        lockAndSpawn();
    }

    public void drawGame() {
        gc.clearRect(0, 0, gameCanvas.getWidth(), gameCanvas.getHeight());

        gc.setFill(Color.LIGHTGRAY);
        gc.fillRect(0, 0, gridWidth * cellSize, gridHeight * cellSize);

        gc.setStroke(Color.DARKGRAY);
        gc.setLineWidth(0.5);
        for (int x = 0; x <= gridWidth; x++) {
            gc.strokeLine(x * cellSize, 0, x * cellSize, gridHeight * cellSize);
        }
        for (int y = 0; y <= gridHeight; y++) {
            gc.strokeLine(0, y * cellSize, gridWidth * cellSize, y * cellSize);
        }

        for (int y = 0; y < gridHeight; y++) {
            for (int x = 0; x < gridWidth; x++) {
                if (grid[y][x] > 0) {
                    drawBlock(x, y, getColor(grid[y][x]));
                }
            }
        }

        drawGhostPiece();

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
                        if (gy >= 0 && gy < gridHeight && gx >= 0 && gx < gridWidth) {
                            drawBlock(gx, gy, color);
                        }
                    }
                }
            }
        }

        if (isGameOver) {
            gc.setFill(Color.color(0, 0, 0, 0.6));
            gc.fillRect(0, 0, gridWidth * cellSize, gridHeight * cellSize);

            gc.setFont(ui20 != null ? ui20 : Font.font("Arial", 20));
            gc.setFill(Color.WHITE);
            double x = (gridWidth * cellSize) / 2.0 - 100;
            gc.fillText("GAME OVER", x, gameOverY);
            gc.fillText("Score: " + score, x, gameOverY + 30);
            gc.fillText("Level: " + level, x, gameOverY + 60);
        }
    }

    private void drawNextPiecePreview() {
        GraphicsContext ngc = nextPieceCanvas.getGraphicsContext2D();
        ngc.clearRect(0, 0, nextPieceCanvas.getWidth(), nextPieceCanvas.getHeight());

        if (difficulty.equalsIgnoreCase("HARD")) {
            ngc.setFont(Font.font("Arial", 48));
            ngc.setFill(Color.DARKRED);
            ngc.fillText("\uD83D\uDC80", 20, 56);
            return;
        }

        if (nextTetramino != null) {
            int[][] shape = nextTetramino.getShape();
            Color color = getColor(nextColorIndex);

            int box = 18;
            int w = shape[0].length;
            int h = shape.length;
            int offX = (int) (nextPieceCanvas.getWidth() - w * box) / 2;
            int offY = (int) (nextPieceCanvas.getHeight() - h * box) / 2;

            for (int r = 0; r < h; r++) {
                for (int c = 0; c < w; c++) {
                    if (shape[r][c] != 0) {
                        int px = offX + c * box;
                        int py = offY + r * box;
                        ngc.setFill(color);
                        ngc.fillRect(px, py, box - 2, box - 2);

                        ngc.setStroke(color.brighter());
                        ngc.setLineWidth(2);
                        ngc.strokeRect(px, py, box - 2, box - 2);
                    }
                }
            }
        }
    }

    private void drawBlock(int x, int y, Color color) {
        int px = x * cellSize;
        int py = y * cellSize;

        gc.setFill(color);
        gc.fillRect(px + 1, py + 1, cellSize - 2, cellSize - 2);

        gc.setStroke(color.brighter());
        gc.setLineWidth(2);
        gc.strokeLine(px + 1, py + 1, px + cellSize - 1, py + 1);
        gc.strokeLine(px + 1, py + 1, px + 1, py + cellSize - 1);

        gc.setStroke(color.darker());
        gc.strokeLine(px + cellSize - 1, py + 1, px + cellSize - 1, py + cellSize - 1);
        gc.strokeLine(px + 1, py + cellSize - 1, px + cellSize - 1, py + cellSize - 1);
    }

    private javafx.scene.paint.Color getColor(int code) {
        return switch (code) {
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

    private int codeFromType(TetrominoType type) {
        return switch (type) {
            case I -> 1;
            case O -> 2;
            case T -> 3;
            case S -> 4;
            case Z -> 5;
            case J -> 6;
            case L -> 7;
        };
    }

    private void planAiForCurrentPiece() {
        aiPlan.clear();
        if (currentTetramino == null) return;

        int bestRot = 0;
        int bestX = currentTetramino.getX();
        double bestScore = -1e9;

        for (int rot = 0; rot < 4; rot++) {
            Tetramino tmp = factory.create(currentType);
            for (int i = 0; i < rot; i++) tmp.rotateCW();
            int[][] shape = tmp.getShape();

            for (int x = -2; x <= gridWidth; x++) {
                Integer y = dropYFor(shape, x);
                if (y == null) continue;

                int[][] g = copyGrid(grid);
                placeShape(g, shape, x, y, currentColorIndex);

                int cleared = clearLinesCount(g);
                int holes = countHoles(g);
                int aggH = aggregateHeight(g);
                int bump = bumpiness(g);

                double score = cleared * 100 - holes * 50 - aggH * 1 - bump * 1;
                if (score > bestScore) {
                    bestScore = score;
                    bestRot = rot;
                    bestX = x;
                }
            }
        }

        final int targetRot = bestRot;
        final int targetX = bestX;

        for (int i = 0; i < targetRot; i++) {
            aiPlan.add(this::rotateCW);
        }

        aiPlan.add(() -> {
            int dx = targetX - currentTetramino.getX();
            if (dx < 0) {
                for (int i = 0; i < -dx; i++) moveLeft();
            } else if (dx > 0) {
                for (int i = 0; i < dx; i++) moveRight();
            }
        });

        aiPlan.add(this::hardDrop);
    }

    private Integer dropYFor(int[][] shape, int baseX) {
        int y = -shape.length;
        while (true) {
            if (collidesAtGrid(grid, baseX, y, shape)) {
                if (y == -shape.length) return null;
                return y - 1;
            }
            y++;
            if (y > gridHeight) return null;
        }
    }

    private static boolean collidesAtGrid(int[][] g, int baseX, int baseY, int[][] shape) {
        int h = g.length, w = g[0].length;
        for (int r = 0; r < shape.length; r++) {
            for (int c = 0; c < shape[r].length; c++) {
                if (shape[r][c] == 0) continue;
                int gx = baseX + c;
                int gy = baseY + r;
                if (gy >= h) return true;
                if (gx < 0 || gx >= w) return true;
                if (gy >= 0 && g[gy][gx] != 0) return true;
            }
        }
        return false;
    }

    private static void placeShape(int[][] g, int[][] shape, int baseX, int baseY, int code) {
        for (int r = 0; r < shape.length; r++) {
            for (int c = 0; c < shape[r].length; c++) {
                if (shape[r][c] == 0) continue;
                int gx = baseX + c;
                int gy = baseY + r;
                if (gy >= 0 && gy < g.length && gx >= 0 && gx < g[0].length) {
                    g[gy][gx] = code;
                }
            }
        }
    }

    private static int[][] copyGrid(int[][] g) {
        int[][] out = new int[g.length][g[0].length];
        for (int i = 0; i < g.length; i++) {
            System.arraycopy(g[i], 0, out[i], 0, g[i].length);
        }
        return out;
    }

    private static int clearLinesCount(int[][] g) {
        int h = g.length, w = g[0].length;
        int count = 0;
        for (int y = 0; y < h; y++) {
            boolean full = true;
            for (int x = 0; x < w; x++) {
                if (g[y][x] == 0) {
                    full = false;
                    break;
                }
            }
            if (full) {
                count++;
                for (int row = y; row > 0; row--) {
                    System.arraycopy(g[row - 1], 0, g[row], 0, w);
                }
                for (int x = 0; x < w; x++) g[0][x] = 0;
            }
        }
        return count;
    }

    private static int countHoles(int[][] g) {
        int h = g.length, w = g[0].length;
        int holes = 0;
        for (int x = 0; x < w; x++) {
            boolean filledSeen = false;
            for (int y = 0; y < h; y++) {
                if (g[y][x] != 0) {
                    filledSeen = true;
                } else if (filledSeen) {
                    holes++;
                }
            }
        }
        return holes;
    }

    private static int aggregateHeight(int[][] g) {
        int h = g.length, w = g[0].length, sum = 0;
        for (int x = 0; x < w; x++) sum += columnHeight(g, x);
        return sum;
    }

    private static int columnHeight(int[][] g, int x) {
        for (int y = 0; y < g.length; y++) {
            if (g[y][x] != 0) return g.length - y;
        }
        return 0;
    }

    private static int bumpiness(int[][] g) {
        int w = g[0].length;
        int prev = columnHeight(g, 0);
        int sum = 0;
        for (int x = 1; x < w; x++) {
            int ch = columnHeight(g, x);
            sum += Math.abs(ch - prev);
            prev = ch;
        }
        return sum;
    }

    private void drawGhostPiece() {
        if (isPaused || isGameOver) return;
        if (currentTetramino == null) return;

        int gy = computeGhostY();
        if (gy < 0) return;

        int[][] shape = currentTetramino.getShape();
        int baseX = currentTetramino.getX();
        Color base = getColor(currentColorIndex);
        Color fill = Color.color(base.getRed(), base.getGreen(), base.getBlue(), 0.18);
        Color stroke = Color.color(base.brighter().getRed(), base.brighter().getGreen(), base.brighter().getBlue(), 0.35);

        for (int r = 0; r < shape.length; r++) {
            for (int c = 0; c < shape[r].length; c++) {
                if (shape[r][c] == 0) continue;
                int gx = baseX + c;
                int gy2 = gy + r;
                if (gy2 >= 0 && gy2 < gridHeight && gx >= 0 && gx < gridWidth) {
                    drawGhostBlock(gx, gy2, fill, stroke);
                }
            }
        }
    }

    private void drawGhostBlock(int x, int y, Color fill, Color stroke) {
        int px = x * cellSize;
        int py = y * cellSize;
        gc.setFill(fill);
        gc.fillRect(px + 1, py + 1, cellSize - 2, cellSize - 2);
        gc.setStroke(stroke);
        gc.setLineWidth(1.5);
        gc.strokeRect(px + 1, py + 1, cellSize - 2, cellSize - 2);
    }

    private void ensureServerPoll() {
        if (!externalControlled) return;
        if (serverPoll != null) return;
        serverLabel.setVisible(true);
        serverLabel.setManaged(true);
        serverPoll = new Timeline(new KeyFrame(Duration.seconds(1), ev -> {
            new Thread(() -> {
                boolean up = ServerConnection.ping();
                javafx.application.Platform.runLater(() -> {
                    serverConnected = up;
                    if (serverLabel != null) {
                        serverLabel.setText(up ? "SERVER: CONNECTED" : "SERVER: OFFLINE");
                        serverLabel.setStyle(up ? "-fx-text-fill: #39d353; -fx-font-weight: bold;"
                                : "-fx-text-fill: orange; -fx-font-weight: bold;");
                    }
                });
            }, "server-ping").start();
        }));
        serverPoll.setCycleCount(Timeline.INDEFINITE);
        serverPoll.play();
    }

    private void promptHighScore() {
        TextInputDialog dlg = new TextInputDialog(playerName);
        dlg.setTitle("Save High Score");
        dlg.setHeaderText("Score: " + score + "   Level: " + level);
        dlg.setContentText("Name:");
        Optional<String> res = dlg.showAndWait();
        res.ifPresent(n -> HighScoresStore.getInstance().addScoreAsync(n, score, level));
    }
}