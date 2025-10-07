package com.waskronos.Tetris.store;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * This class stores high scores
 * It keeps the top ten entries
 */
public class HighScores {
    private List<HighScore> entries = new ArrayList<>();

    public HighScores() {}

    public List<HighScore> getEntries() { return entries; }

    public void setEntries(List<HighScore> entries) {
        this.entries = (entries == null) ? new ArrayList<>() : new ArrayList<>(entries);
    }

    /**
     * Add a score and keep only the top ten
     * Uses Comparable and streams for simple sorting
     */
    public void addAndTrim(HighScore hs) {
        entries.add(hs);
        entries = entries.stream()
                .sorted()
                .limit(10)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * Return a sorted copy of the top ten
     */
    public List<HighScore> top() {
        List<HighScore> copy = new ArrayList<>(entries);
        Collections.sort(copy);
        if (copy.size() > 10) {
            return new ArrayList<>(copy.subList(0, 10));
        }
        return copy;
    }
}