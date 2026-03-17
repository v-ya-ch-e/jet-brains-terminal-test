package com.jetbrains.vyache.task;

/**
 * Represents a single cell in the terminal grid.
 *
 * <p>Each cell holds a character and its associated {@link CellAttributes} (foreground color,
 * background color, and style flags). Cells are immutable records — writing to a screen position
 * replaces the entire {@code Cell} object rather than mutating it.
 *
 * <p>The sentinel value {@link #EMPTY} represents an unoccupied cell (character is
 * {@link Character#MIN_VALUE} with default attributes). This avoids nullable cells in
 * the screen grid while still distinguishing "no content" from a space character.
 *
 * @param character  the character stored in this cell
 * @param attributes the visual attributes (colors, styles) of this cell
 */
public record Cell(char character, CellAttributes attributes) {

    /** Canonical empty cell — used to initialise every position in a fresh screen row. */
    public static final Cell EMPTY = new Cell(Character.MIN_VALUE, CellAttributes.DEFAULT);

    /**
     * Returns {@code true} if this cell has not been written to.
     *
     * <p>A cell is considered empty when its character equals {@link Character#MIN_VALUE},
     * which is the sentinel chosen because it is a non-printable control character that
     * will never appear in normal terminal output.
     *
     * @return {@code true} if the cell is empty
     */
    public boolean isEmpty() {
        return character == Character.MIN_VALUE;
    }
}
