package com.jetbrains.vyache.task;

public record Cell(char character, CellAttributes attributes) {

    public static final Cell EMPTY = new Cell(Character.MIN_VALUE, CellAttributes.DEFAULT);

    public boolean isEmpty() {
        return character == Character.MIN_VALUE;
    }
}
