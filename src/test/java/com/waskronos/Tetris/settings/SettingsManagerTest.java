package com.waskronos.Tetris.settings;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SettingsManagerTest {

    @AfterEach
    void reset() {
        SettingsManager.getInstance().resetToDefaults();
    }

    @Test
    void singletonIsSameInstance() {
        SettingsManager a = SettingsManager.getInstance();
        SettingsManager b = SettingsManager.getInstance();
        assertSame(a, b);
    }

    @Test
    void resetToDefaultsRestoresValues() {
        SettingsManager s = SettingsManager.getInstance();
        s.setBoardWidth(12);
        s.setBoardHeight(30);
        s.setCellSize(24);
        s.setStartingLevel(3);
        s.setMusicEnabled(false);
        s.setSfxEnabled(false);

        s.resetToDefaults();

        assertEquals(10, s.getBoardWidth());
        assertEquals(20, s.getBoardHeight());
        assertEquals(32, s.getCellSize());
        assertEquals(1, s.getStartingLevel());
        assertTrue(s.isMusicEnabled());
        assertTrue(s.isSfxEnabled());
    }
}