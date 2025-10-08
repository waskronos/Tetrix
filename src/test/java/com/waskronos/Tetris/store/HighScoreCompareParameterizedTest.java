package com.waskronos.Tetris.store;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertTrue;

class HighScoreCompareParameterizedTest {

    @ParameterizedTest
    @CsvSource({
            // name1,score1,time1,name2,score2,time2, expectedSign
            "A, 300, 100, B, 200, 200, -1",
            "A, 200, 100, B, 200, 200, 1",   // newer first at same score
            "A, 150, 300, B, 200, 100, 1"
    })
    void compareToSortsByScoreDescThenDateDesc(String n1, int s1, long t1,
                                               String n2, int s2, long t2,
                                               int expectedSign) {
        HighScore h1 = new HighScore(n1, s1, t1);
        HighScore h2 = new HighScore(n2, s2, t2);

        int cmp = h1.compareTo(h2);

        if (expectedSign < 0) {
            assertTrue(cmp < 0);
        } else if (expectedSign > 0) {
            assertTrue(cmp > 0);
        } else {
            assertTrue(cmp == 0);
        }
    }
}