package com.waskronos.Tetris.store;

/**
 * This object models a high score entry
 * This implements Comparable to support sorting
 */
public class HighScore implements Comparable<HighScore> {
    private String name;
    private int score;
    private long dateTime;

    public HighScore() {
        // This no args constructor supports JSON
    }

    public HighScore(String name, int score, long dateTime) {
        this.name = name;
        this.score = score;
        this.dateTime = dateTime;
    }

    public String getName() { return name; }
    public int getScore() { return score; }
    public long getDateTime() { return dateTime; }

    public void setName(String name) { this.name = name; }
    public void setScore(int score) { this.score = score; }
    public void setDateTime(long dateTime) { this.dateTime = dateTime; }

    /**
     * Higher score comes first
     * Newer date comes first when scores match
     */
    @Override
    public int compareTo(HighScore other) {
        int byScore = Integer.compare(other.score, this.score);
        if (byScore != 0) return byScore;
        return Long.compare(other.dateTime, this.dateTime);
    }
}