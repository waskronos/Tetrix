package com.waskronos.Tetris.store;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

import static org.junit.jupiter.api.Assertions.*;

class HighScoresStoreTest {

    private String oldHome;
    private Path testHome;

    @BeforeEach
    void setUp() throws Exception {
        oldHome = System.getProperty("user.home");
        testHome = Files.createTempDirectory("tetrix-home-");
        System.setProperty("user.home", testHome.toString());
    }

    @AfterEach
    void tearDown() throws Exception {
        System.setProperty("user.home", oldHome);
        deleteRecursivelyWithRetries(testHome);
    }

    @Test
    void saveAndLoadWritesJsonInUserHome() throws Exception {
        HighScoresStore store = newStoreBoundToCurrentHome();

        HighScores scores = new HighScores();
        scores.addAndTrim(new HighScore("TEST", 999, 3L));
        store.save(scores); // synchronous write

        Path json = testHome.resolve(".tetrix").resolve("highscores.json");
        assertTrue(Files.exists(json));

        HighScores loaded = store.load();
        assertFalse(loaded.top().isEmpty());
        assertEquals("TEST", loaded.top().get(0).getName());
        assertEquals(999, loaded.top().get(0).getScore());
    }

    private static HighScoresStore newStoreBoundToCurrentHome() throws Exception {
        Constructor<HighScoresStore> c = HighScoresStore.class.getDeclaredConstructor();
        c.setAccessible(true);
        return c.newInstance();
    }

    private static void deleteRecursivelyWithRetries(Path root) throws Exception {
        if (root == null) return;
        for (int i = 0; i < 12; i++) {
            try {
                if (!Files.exists(root)) return;
                Files.walk(root)
                        .sorted(Comparator.reverseOrder())
                        .forEach(p -> {
                            try { Files.deleteIfExists(p); } catch (Exception ignore) {}
                        });
                if (!Files.exists(root)) return;
            } catch (Exception ignore) {
            }
            Thread.sleep(150);
        }
    }
}