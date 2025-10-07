package com.waskronos.Tetris.core;

public enum TetrominoType {
    I(new int[][] { {1,1,1,1} }),
    O(new int[][] { {1,1}, {1,1} }),
    T(new int[][] { {0,1,0}, {1,1,1} }),
    S(new int[][] { {0,1,1}, {1,1,0} }),
    Z(new int[][] { {1,1,0}, {0,1,1} }),
    J(new int[][] { {1,0,0}, {1,1,1} }),
    L(new int[][] { {0,0,1}, {1,1,1} });

    private final int[][] shape;

    TetrominoType(int[][] shape) {
        this.shape = shape;
    }

    // Defensive deep copy so rotation/mutation doesn’t affect the enum data
    public int[][] getShapeCopy() {
        int[][] copy = new int[shape.length][];
        for (int r = 0; r < shape.length; r++) {
            copy[r] = shape[r].clone();
        }
        return copy;
    }
}