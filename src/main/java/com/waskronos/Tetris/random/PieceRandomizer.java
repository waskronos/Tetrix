package com.waskronos.Tetris.random;

import com.waskronos.Tetris.core.TetrominoType;

public interface PieceRandomizer {
    TetrominoType next();
    TetrominoType peek();
}