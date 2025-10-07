package com.waskronos.Tetris.random;

import com.waskronos.Tetris.core.TetrominoType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class BagRandomizer implements PieceRandomizer {

    private final List<TetrominoType> bag = new ArrayList<>();
    private final Random random;

    public BagRandomizer() {
        this.random = new Random();
    }

    public BagRandomizer(long seed) {
        this.random = new Random(seed);
    }

    private void refill() {
        bag.clear();
        Collections.addAll(bag, TetrominoType.values());
        Collections.shuffle(bag, random);
    }

    @Override
    public TetrominoType next() {
        if (bag.isEmpty()) refill();
        return bag.remove(0);
    }

    @Override
    public TetrominoType peek() {
        if (bag.isEmpty()) refill();
        return bag.get(0);
    }

    public boolean isEmpty() {
        return bag.isEmpty();
    }
}