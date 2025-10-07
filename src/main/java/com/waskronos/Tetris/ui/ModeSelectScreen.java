package com.waskronos.Tetris.ui;

import com.waskronos.Tetris.app.TetrisApp;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class ModeSelectScreen extends BorderPane {
    // This screen lets the player pick a mode
    // This keeps controls visible for clarity

    private final TetrisApp app;

    public ModeSelectScreen(TetrisApp app) {
        this.app = app;

        Label title = new Label("Select Mode");
        title.getStyleClass().add("title");
        setTop(title);
        BorderPane.setAlignment(title, Pos.CENTER);
        BorderPane.setMargin(title, new Insets(16, 16, 8, 16));

        Button singleBtn = makeBtn("Single Player", () -> app.showGameScreen());
        Button twoBtn    = makeBtn("Two Player",    () -> app.showTwoPlayerScreen());
        Button aiBtn     = makeBtn("Single (Assisted)", () -> app.showGameScreenAssisted());

        VBox buttons = new VBox(14, singleBtn, twoBtn, aiBtn);
        buttons.setAlignment(Pos.CENTER);
        setCenter(buttons);

        Button back = new Button("Back");
        back.setOnAction(e -> app.showMainScreen());
        HBox backRow = new HBox(back);
        backRow.setAlignment(Pos.CENTER);
        backRow.setPadding(new Insets(10, 0, 6, 0));

        VBox singleBox = buildSingleControlsBox();
        VBox twoBox    = buildTwoControlsBox();

        HBox bottomBoxes = new HBox(16, singleBox, twoBox);
        bottomBoxes.setAlignment(Pos.CENTER);
        bottomBoxes.setPadding(new Insets(6, 12, 12, 12));
        HBox.setHgrow(singleBox, Priority.ALWAYS);
        HBox.setHgrow(twoBox, Priority.ALWAYS);

        VBox bottom = new VBox(backRow, bottomBoxes);
        bottom.setAlignment(Pos.CENTER);
        setBottom(bottom);
    }

    private Button makeBtn(String text, Runnable action) {
        Button b = new Button(text);
        b.getStyleClass().add("big");
        b.setOnAction(e -> action.run());
        return b;
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