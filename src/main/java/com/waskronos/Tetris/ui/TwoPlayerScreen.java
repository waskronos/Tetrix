package com.waskronos.Tetris.ui;

import com.waskronos.Tetris.app.TetrisApp;
import com.waskronos.Tetris.events.GameEvents;
import com.waskronos.Tetris.random.BagRandomizer;
import com.waskronos.Tetris.random.PieceRandomizer;
import com.waskronos.Tetris.store.HighScoresStore;
import com.waskronos.Tetris.net.ServerConnection;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextInputDialog;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

import java.util.Optional;

public class TwoPlayerScreen extends BorderPane {

    public enum PlayerMode { HUMAN, AI, EXTERNAL }

    private final TetrisApp app;
    private final GameScreen p1;
    private final GameScreen p2;

    private boolean p1Ended = false;
    private boolean p2Ended = false;
    private boolean matchEnded = false;

    private Button actionBtn;
    private Button backBtn;
    private Label resultBanner;

    private final PlayerMode p1Mode;
    private final PlayerMode p2Mode;

    private String winnerCandidate = null;

    private final GameEvents.GameOverListener gameOverListener = evt -> {
        String who = evt.getName();
        if (!"P1".equalsIgnoreCase(who) && !"P2".equalsIgnoreCase(who)) return;
        Platform.runLater(() -> onBoardGameOver(who));
    };

    private boolean serverConnected = false;
    private Label serverLabel;
    private Timeline serverPoll;

    public TwoPlayerScreen(TetrisApp app) {
        this(app, PlayerMode.HUMAN, PlayerMode.HUMAN);
    }

    public TwoPlayerScreen(TetrisApp app, PlayerMode p1Mode, PlayerMode p2Mode) {
        this.app = app;
        this.p1Mode = p1Mode;
        this.p2Mode = p2Mode;

        long seed = System.currentTimeMillis();
        PieceRandomizer r1 = new BagRandomizer(seed);
        PieceRandomizer r2 = new BagRandomizer(seed);

        p1 = new GameScreen(app, r1, false);
        p2 = new GameScreen(app, r2, false);

        p1.setPlayerName("P1");
        p2.setPlayerName("P2");

        p1.setAiActive(p1Mode == PlayerMode.AI);
        p2.setAiActive(p2Mode == PlayerMode.AI);

        p1.setHighScorePromptEnabled(false);
        p2.setHighScorePromptEnabled(false);

        p1.setExternalControlled(p1Mode == PlayerMode.EXTERNAL);
        p2.setExternalControlled(p2Mode == PlayerMode.EXTERNAL);

        try { p1.setControlsVisible(false); } catch (Throwable ignored) {}
        try { p2.setControlsVisible(false); } catch (Throwable ignored) {}

        serverLabel = new Label("SERVER: OFFLINE");
        serverLabel.setStyle("-fx-text-fill: orange; -fx-font-weight: bold;");

        HBox boards = new HBox(20, p1, p2);
        boards.setAlignment(Pos.CENTER);
        boards.setPadding(new Insets(12));

        resultBanner = new Label();
        resultBanner.setStyle("-fx-font-size: 32px; -fx-font-weight: bold; -fx-text-fill: #ffffff; -fx-background-color: rgba(0,0,0,0.55); -fx-padding: 12 18 12 18; -fx-background-radius: 10;");
        resultBanner.setVisible(false);
        resultBanner.setManaged(false);

        StackPane center = new StackPane(boards, resultBanner);
        StackPane.setAlignment(resultBanner, Pos.TOP_CENTER);
        StackPane.setMargin(resultBanner, new Insets(16, 0, 0, 0));
        setTop(serverLabel);
        BorderPane.setAlignment(serverLabel, Pos.CENTER);
        BorderPane.setMargin(serverLabel, new Insets(8, 0, 0, 0));
        setCenter(center);

        backBtn = new Button("Back to Mode Select");
        backBtn.setOnAction(e -> {
            cleanup();
            app.showModeSelectScreen();
        });
        backBtn.setFocusTraversable(false);

        actionBtn = new Button("Pause or Resume");
        actionBtn.setOnAction(e -> {
            p1.togglePause();
            p2.togglePause();
        });
        actionBtn.setFocusTraversable(false);

        HBox buttonRow = new HBox(12, actionBtn, backBtn);
        buttonRow.setAlignment(Pos.CENTER);
        buttonRow.setPadding(new Insets(10, 0, 14, 0));
        setBottom(buttonRow);

        sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene == null) return;

            newScene.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
                if (matchEnded) { e.consume(); return; }

                KeyCode k = e.getCode();

                if (k == KeyCode.ESCAPE) {
                    p1.togglePause();
                    p2.togglePause();
                    e.consume();
                    return;
                }

                if (p1Mode == PlayerMode.HUMAN || (p1Mode == PlayerMode.EXTERNAL && serverConnected)) {
                    switch (k) {
                        case A -> { p1.moveLeft(); e.consume(); return; }
                        case D -> { p1.moveRight(); e.consume(); return; }
                        case S -> { p1.softDrop(); e.consume(); return; }
                        case Q -> { p1.rotateCCW(); e.consume(); return; }
                        case E -> { p1.rotateCW(); e.consume(); return; }
                        case SPACE -> { p1.hardDrop(); e.consume(); return; }
                    }
                }

                if (p2Mode == PlayerMode.HUMAN || (p2Mode == PlayerMode.EXTERNAL && serverConnected)) {
                    switch (k) {
                        case LEFT  -> { p2.moveLeft(); e.consume(); return; }
                        case RIGHT -> { p2.moveRight(); e.consume(); return; }
                        case DOWN  -> { p2.softDrop(); e.consume(); return; }
                        case O     -> { p2.rotateCCW(); e.consume(); return; }
                        case P     -> { p2.rotateCW(); e.consume(); return; }
                        case ENTER -> { p2.hardDrop(); e.consume(); return; }
                    }
                }
            });

            newScene.setOnMouseClicked(ev -> newScene.getRoot().requestFocus());
            newScene.getRoot().requestFocus();
        });

        GameEvents.getInstance().addGameOverListener(gameOverListener);

        serverPoll = new Timeline(new KeyFrame(Duration.seconds(1), ev -> {
            new Thread(() -> {
                boolean up = ServerConnection.ping();
                Platform.runLater(() -> {
                    serverConnected = up;
                    serverLabel.setText(up ? "SERVER: CONNECTED" : "SERVER: OFFLINE");
                    serverLabel.setStyle(up ? "-fx-text-fill: #39d353; -fx-font-weight: bold;"
                            : "-fx-text-fill: orange; -fx-font-weight: bold;");
                });
            }, "server-ping-2p").start();
        }));
        serverPoll.setCycleCount(Timeline.INDEFINITE);
        serverPoll.play();
    }

    private void cleanup() {
        GameEvents.getInstance().removeGameOverListener(gameOverListener);
        if (serverPoll != null) serverPoll.stop();
    }

    private void onBoardGameOver(String who) {
        if (matchEnded) return;

        if ("P1".equalsIgnoreCase(who)) {
            p1Ended = true;
            if (winnerCandidate == null) winnerCandidate = "P2";
        } else if ("P2".equalsIgnoreCase(who)) {
            p2Ended = true;
            if (winnerCandidate == null) winnerCandidate = "P1";
        }

        if (p1Ended && p2Ended) {
            if (winnerCandidate == null) {
                endMatchWithMessage("TIE");
            } else if ("P1".equals(winnerCandidate)) {
                endMatchWithMessage("PLAYER 1 WINS");
                promptWinnerHighScore(p1, defaultNameFor(p1Mode, "P1"));
            } else {
                endMatchWithMessage("PLAYER 2 WINS");
                promptWinnerHighScore(p2, defaultNameFor(p2Mode, "P2"));
            }
        }
    }

    private String defaultNameFor(PlayerMode mode, String base) {
        return switch (mode) {
            case AI -> "AI-" + base;
            case EXTERNAL -> "EXT-" + base;
            default -> base;
        };
    }

    private void endMatchWithMessage(String message) {
        matchEnded = true;
        resultBanner.setText(message);
        resultBanner.setVisible(true);
        resultBanner.setManaged(true);

        actionBtn.setText("Restart");
        actionBtn.setOnAction(e -> {
            p1.restartGame();
            p2.restartGame();
            p1Ended = false;
            p2Ended = false;
            matchEnded = false;
            winnerCandidate = null;
            resultBanner.setVisible(false);
            resultBanner.setManaged(false);
            actionBtn.setText("Pause or Resume");
            actionBtn.setOnAction(ev -> {
                p1.togglePause();
                p2.togglePause();
            });
            if (getScene() != null) getScene().getRoot().requestFocus();
        });
    }

    private void promptWinnerHighScore(GameScreen winner, String defaultName) {
        TextInputDialog dlg = new TextInputDialog(defaultName);
        dlg.setTitle("Save Winner High Score");
        dlg.setHeaderText("Score: " + winner.getScore() + "   Level: " + winner.getLevel());
        dlg.setContentText("Name:");
        Optional<String> res = dlg.showAndWait();
        res.ifPresent(n -> HighScoresStore.getInstance().addScoreAsync(n, winner.getScore(), winner.getLevel()));
    }
}