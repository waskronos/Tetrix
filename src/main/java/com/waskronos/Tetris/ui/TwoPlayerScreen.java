package com.waskronos.Tetris.ui;

import com.waskronos.Tetris.app.TetrisApp;
import com.waskronos.Tetris.events.GameEvents;
import com.waskronos.Tetris.random.BagRandomizer;
import com.waskronos.Tetris.random.PieceRandomizer;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.control.Label;

public class TwoPlayerScreen extends BorderPane {
    // This screen shows two boards and routes keys for both players

    private final TetrisApp app;
    private final GameScreen p1;
    private final GameScreen p2;

    private boolean p1Ended = false;
    private boolean p2Ended = false;
    private boolean matchEnded = false;

    private Button actionBtn;
    private Label resultBanner;

    public TwoPlayerScreen(TetrisApp app) {
        this.app = app;

        long seed = System.currentTimeMillis();
        PieceRandomizer r1 = new BagRandomizer(seed);
        PieceRandomizer r2 = new BagRandomizer(seed);

        p1 = new GameScreen(app, r1, false);
        p2 = new GameScreen(app, r2, false);

        p1.setPlayerName("P1");
        p2.setPlayerName("P2");

        try { p1.setControlsVisible(false); } catch (Throwable ignored) {}
        try { p2.setControlsVisible(false); } catch (Throwable ignored) {}

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
        setCenter(center);

        Button backBtn = new Button("Back to Mode Select");
        backBtn.setOnAction(e -> app.showModeSelectScreen());

        actionBtn = new Button("Pause or Resume");
        actionBtn.setOnAction(e -> {
            p1.togglePause();
            p2.togglePause();
        });

        HBox buttonRow = new HBox(12, actionBtn, backBtn);
        buttonRow.setAlignment(Pos.CENTER);
        buttonRow.setPadding(new Insets(10, 0, 14, 0));
        setBottom(buttonRow);

        sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene == null) return;

            newScene.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
                KeyCode k = e.getCode();

                if (k == KeyCode.ESCAPE) {
                    p1.togglePause();
                    p2.togglePause();
                    e.consume();
                    return;
                }

                switch (k) {
                    case A -> { p1.moveLeft(); e.consume(); return; }
                    case D -> { p1.moveRight(); e.consume(); return; }
                    case S -> { p1.softDrop(); e.consume(); return; }
                    case Q -> { p1.rotateCCW(); e.consume(); return; }
                    case E -> { p1.rotateCW(); e.consume(); return; }
                    case SPACE -> { p1.hardDrop(); e.consume(); return; }
                }

                switch (k) {
                    case LEFT  -> { p2.moveLeft(); e.consume(); return; }
                    case RIGHT -> { p2.moveRight(); e.consume(); return; }
                    case DOWN  -> { p2.softDrop(); e.consume(); return; }
                    case O     -> { p2.rotateCCW(); e.consume(); return; }
                    case P     -> { p2.rotateCW(); e.consume(); return; }
                    case ENTER -> { p2.hardDrop(); e.consume(); return; }
                }
            });

            newScene.getRoot().requestFocus();
        });

        GameEvents.getInstance().addGameOverListener(evt ->
                Platform.runLater(() -> onBoardGameOver(evt.getName()))
        );
    }

    private void onBoardGameOver(String who) {
        if (matchEnded) return;

        if ("P1".equalsIgnoreCase(who)) {
            p1Ended = true;
            if (!p2Ended) {
                endMatchWithMessage("PLAYER 2 WINS");
            } else {
                endMatchWithMessage("TIE");
            }
        } else if ("P2".equalsIgnoreCase(who)) {
            p2Ended = true;
            if (!p1Ended) {
                endMatchWithMessage("PLAYER 1 WINS");
            } else {
                endMatchWithMessage("TIE");
            }
        } else {
            // Unknown name. Do nothing.
        }
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
            resultBanner.setVisible(false);
            resultBanner.setManaged(false);
            actionBtn.setText("Pause or Resume");
            actionBtn.setOnAction(ev -> {
                p1.togglePause();
                p2.togglePause();
            });
        });
    }
}