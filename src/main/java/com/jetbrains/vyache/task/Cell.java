package com.jetbrains.vyache.task;

public record Cell(char character, CellAttributes attributes) {

    public static final Cell EMPTY = new Cell(' ', CellAttributes.DEFAULT);
}
