package com.waskronos.Tetris.core;

public class TetrominoFactory {

    public Tetramino create(TetrominoType type) {
        int[][] shapeCopy = type.getShapeCopy();
        return new Tetramino(shapeCopy, 0, 0);
    }
}