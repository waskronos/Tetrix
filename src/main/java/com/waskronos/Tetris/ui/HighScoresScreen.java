package com.waskronos.Tetris.ui;

import com.waskronos.Tetris.app.TetrisApp;
import com.waskronos.Tetris.store.HighScore;
import com.waskronos.Tetris.store.HighScores;
import com.waskronos.Tetris.store.HighScoresStore;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class HighScoresScreen extends BorderPane {
    // This screen shows the top scores.

    private final TetrisApp app;
    private final VBox listBox = new VBox(6);
    private final Label title = new Label("HIGH SCORES TOP 10");

    public HighScoresScreen(TetrisApp app) {
        this.app = app;

        setPadding(new Insets(20));

        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");

        listBox.setAlignment(Pos.CENTER_LEFT);

        Button refresh = new Button("Refresh");
        refresh.setOnAction(e -> refreshScores());

        Button back = new Button("Back");
        back.setOnAction(e -> app.showMainScreen());

        HBox actions = new HBox(12, refresh, back);
        actions.setAlignment(Pos.CENTER);

        VBox content = new VBox(16, title, listBox, actions);
        content.setAlignment(Pos.CENTER);
        content.setFillWidth(false);

        setCenter(content);

        refreshScores();
    }

    private void refreshScores() {
        listBox.getChildren().clear();
        Label loading = new Label("Loading...");
        listBox.getChildren().add(loading);

        HighScoresStore.getInstance().loadAsync(hs -> {
            listBox.getChildren().clear();
            List<HighScore> top = hs.top();

            if (top.isEmpty()) {
                Label empty = new Label("No scores yet. Play a game.");
                listBox.getChildren().add(empty);
                return;
            }

            SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm");
            int rank = 1;
            for (HighScore s : top) {
                String when = fmt.format(new Date(s.getDateTime()));
                Label row = new Label(String.format("%2d) %-6s  %7d  %s", rank, s.getName(), s.getScore(), when));
                row.setStyle("-fx-font-family: monospace; -fx-font-size: 16px;");
                listBox.getChildren().add(row);
                rank++;
            }
        });
    }
}