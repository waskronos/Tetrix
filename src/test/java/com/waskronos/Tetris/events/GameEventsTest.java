package com.waskronos.Tetris.events;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GameEventsTest {

    @AfterEach
    void cleanup() {
        // Remove all listeners if test added any
        // We cannot clear internal list, so we remove known ones when added per test.
        // No global teardown required here.
    }

    @Test
    void convenienceFireGameOverUsesDefaultName() {
        List<GameEvents.GameOver> seen = new ArrayList<>();
        GameEvents.GameOverListener spy = seen::add;

        GameEvents.getInstance().addGameOverListener(spy);
        try {
            GameEvents.getInstance().fireGameOver(123, 4);

            assertEquals(1, seen.size());
            GameEvents.GameOver evt = seen.get(0);
            assertEquals("PLAYER", evt.getName());
            assertEquals(123, evt.getScore());
            assertEquals(4, evt.getLevel());
            assertTrue(evt.getTimestamp() > 0);
        } finally {
            GameEvents.getInstance().removeGameOverListener(spy);
        }
    }

    @Test
    void exceptionInOneListenerDoesNotStopOthers() {
        List<String> calls = new ArrayList<>();
        GameEvents.GameOverListener bad = e -> { throw new RuntimeException("boom"); };
        GameEvents.GameOverListener good = e -> calls.add("ok");

        GameEvents.getInstance().addGameOverListener(bad);
        GameEvents.getInstance().addGameOverListener(good);
        try {
            GameEvents.getInstance().fireGameOver(10, 1);
            assertEquals(1, calls.size());
        } finally {
            GameEvents.getInstance().removeGameOverListener(bad);
            GameEvents.getInstance().removeGameOverListener(good);
        }
    }

    @Test
    void mockListenerIsInvokedWithMockito() {
        GameEvents.GameOverListener mock = Mockito.mock(GameEvents.GameOverListener.class);
        GameEvents.getInstance().addGameOverListener(mock);
        try {
            GameEvents.GameOver go = new GameEvents.GameOver("X", 77, 2, 999L);
            GameEvents.getInstance().fireGameOver(go);

            verify(mock, times(1)).onGameOver(go);
            verifyNoMoreInteractions(mock);
        } finally {
            GameEvents.getInstance().removeGameOverListener(mock);
        }
    }

    @Test
    void spyTestDoubleCapturesEvents() {
        class SpyListener implements GameEvents.GameOverListener {
            GameEvents.GameOver last;
            int count = 0;
            @Override public void onGameOver(GameEvents.GameOver event) {
                count++;
                last = event;
            }
        }
        SpyListener spy = new SpyListener();

        GameEvents.getInstance().addGameOverListener(spy);
        try {
            GameEvents.getInstance().fireGameOver(55, 6);
            assertEquals(1, spy.count);
            assertNotNull(spy.last);
            assertEquals(55, spy.last.getScore());
            assertEquals(6, spy.last.getLevel());
        } finally {
            GameEvents.getInstance().removeGameOverListener(spy);
        }
    }
}