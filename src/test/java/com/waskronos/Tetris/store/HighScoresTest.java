package com.waskronos.Tetris.store;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class HighScoresTest {

    @Test
    void addAndTrimKeepsTopTen() {
        HighScores hs = new HighScores();
        long t = 1_700_000_000_000L;

        for (int i = 0; i < 20; i++) {
            hs.addAndTrim(new HighScore("P" + i, 100 + i, t + i));
        }

        var top = hs.top();
        assertEquals(10, top.size());
        assertEquals(119, top.get(0).getScore());
        assertEquals(110, top.get(9).getScore());
    }

    @Test
    void topOrdersByScoreThenDate() {
        HighScores hs = new HighScores();
        hs.addAndTrim(new HighScore("Old High", 200, 10));
        hs.addAndTrim(new HighScore("New High", 200, 20));
        hs.addAndTrim(new HighScore("Lower", 180, 30));

        var top = hs.top();
        assertEquals("New High", top.get(0).getName());
        assertEquals("Old High", top.get(1).getName());
        assertEquals("Lower", top.get(2).getName());
    }
}