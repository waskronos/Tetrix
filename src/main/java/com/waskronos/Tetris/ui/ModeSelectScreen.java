package com.waskronos.Tetris.ui;

import com.waskronos.Tetris.app.TetrisApp;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class ModeSelectScreen extends BorderPane {
    // Mode selection with One Player and Two Player configs per milestone.

    private final TetrisApp app;

    public ModeSelectScreen(TetrisApp app) {
        this.app = app;

        Label title = new Label("Select Mode");
        title.getStyleClass().add("title");
        setTop(title);
        BorderPane.setAlignment(title, Pos.CENTER);
        BorderPane.setMargin(title, new Insets(16, 16, 8, 16));

        // One Player config: AI Assist toggle.
        Label oneHeader = new Label("One Player");
        oneHeader.getStyleClass().add("subtitle");
        CheckBox aiAssist = new CheckBox("AI Assist");
        Button startOne = new Button("Start");
        startOne.getStyleClass().add("big");
        startOne.setOnAction(e -> {
            if (aiAssist.isSelected()) app.showGameScreenAssisted();
            else app.showGameScreen();
        });
        VBox oneBox = new VBox(8, oneHeader, aiAssist, startOne);
        stylePanel(oneBox);

        // Two Player config: choose each player mode.
        Label twoHeader = new Label("Two Player");
        twoHeader.getStyleClass().add("subtitle");

        ComboBox<String> p1Mode = new ComboBox<>();
        p1Mode.getItems().addAll("Human", "AI", "External");
        p1Mode.getSelectionModel().select("Human");

        ComboBox<String> p2Mode = new ComboBox<>();
        p2Mode.getItems().addAll("Human", "AI", "External");
        p2Mode.getSelectionModel().select("Human");

        HBox rows = new HBox(10,
                new Label("P1:"), p1Mode,
                new Label("P2:"), p2Mode
        );
        Button startTwo = new Button("Start");
        startTwo.getStyleClass().add("big");
        startTwo.setOnAction(e -> app.showTwoPlayerConfigured(
                toMode(p1Mode.getValue()),
                toMode(p2Mode.getValue())
        ));
        VBox twoBox = new VBox(8, twoHeader, rows, startTwo);
        stylePanel(twoBox);

        HBox center = new HBox(16, oneBox, twoBox);
        center.setAlignment(Pos.CENTER);
        setCenter(center);

        Button back = new Button("Back");
        back.setOnAction(e -> app.showMainScreen());
        HBox backRow = new HBox(back);
        backRow.setAlignment(Pos.CENTER);
        backRow.setPadding(new Insets(10, 0, 6, 0));

        // Controls help panels.
        VBox singleHelp = buildSingleControlsBox();
        VBox twoHelp    = buildTwoControlsBox();

        HBox bottomBoxes = new HBox(16, singleHelp, twoHelp);
        bottomBoxes.setAlignment(Pos.CENTER);
        bottomBoxes.setPadding(new Insets(6, 12, 12, 12));
        HBox.setHgrow(singleHelp, Priority.ALWAYS);
        HBox.setHgrow(twoHelp, Priority.ALWAYS);

        VBox bottom = new VBox(backRow, bottomBoxes);
        bottom.setAlignment(Pos.CENTER);
        setBottom(bottom);
    }

    private TwoPlayerScreen.PlayerMode toMode(String v) {
        return switch (v) {
            case "AI" -> TwoPlayerScreen.PlayerMode.AI;
            case "External" -> TwoPlayerScreen.PlayerMode.EXTERNAL;
            default -> TwoPlayerScreen.PlayerMode.HUMAN;
        };
    }

    private VBox buildSingleControlsBox() {
        Label header = new Label("Single Player Controls");
        header.getStyleClass().add("subtitle");

        Label cw   = new Label("➡️ Rotate clockwise E");
        Label ccw  = new Label("⬅️ Rotate counter clockwise Q");
        Label move = new Label("A D Move left right");
        Label soft = new Label("S Soft drop");
        Label hard = new Label("Space Hard drop");
        Label pause= new Label("Esc Pause");

        VBox box = new VBox(6, header, cw, ccw, move, soft, hard, pause);
        stylePanel(box);
        return box;
    }

    private VBox buildTwoControlsBox() {
        Label header = new Label("Two Player Controls");
        header.getStyleClass().add("subtitle");

        Label p1 = new Label("P1 A D move S soft Q E rotate Space hard drop");
        Label p2 = new Label("P2 Left Right move Down soft O P rotate Enter hard drop");
        Label pause = new Label("Pause Esc");

        VBox box = new VBox(6, header, p1, p2, pause);
        stylePanel(box);
        return box;
    }

    private void stylePanel(VBox box) {
        box.getStyleClass().add("panel");
        box.setAlignment(Pos.TOP_LEFT);
        box.setPadding(new Insets(12));
        box.setMinWidth(360);
    }
}