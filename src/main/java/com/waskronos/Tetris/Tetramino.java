package com.waskronos.Tetris;

public class Tetramino {

    private int[][] shape;
    private int x;
    private int y;

    public Tetramino(int[][] shape, int startX, int startY) {
        // Deep copy the shape array
        this.shape = new int[shape.length][];
        for (int i = 0; i < shape.length; i++) {
            this.shape[i] = shape[i].clone();
        }
        this.x = startX;
        this.y = startY;
    }

    public int[][] getShape() {
        return shape;
    }

    public int getX() { return x; }
    public int getY() { return y; }

    public void move(int dx, int dy) { // Move tetramino by offset
        x += dx;
        y += dy;
    }

    public void moveDown() { y++; }
    public void moveUp() { y--; }   // Needed if you revert a move-after-collision approach
    public void moveLeft() { x--; }
    public void moveRight() { x++; }

    public void setPosition(int nx, int ny) {
        this.x = nx;
        this.y = ny;
    }

    public void rotateCW() {
        int rows = shape.length;
        int cols = shape[0].length;
        int[][] rotated = new int[cols][rows];
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                rotated[c][rows - 1 - r] = shape[r][c];
            }
        }
        shape = rotated;
    }

    public void rotateCCW() {
        int rows = shape.length;
        int cols = shape[0].length;
        int[][] rotated = new int[cols][rows];
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                rotated[cols - 1 - c][r] = shape[r][c];
            }
        }
        shape = rotated;
    }
}