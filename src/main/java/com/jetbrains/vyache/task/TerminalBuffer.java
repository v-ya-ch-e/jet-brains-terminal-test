package com.jetbrains.vyache.task;

import java.util.ArrayDeque;
import java.util.LinkedList;
import java.util.List;

public class TerminalBuffer {

    private final int width;
    private final int height;
    private final int maxScrollbackSize;
    private final Cell[][] screen;
    private final ArrayDeque<Cell[]> scrollback;
    private final Cursor cursor;

    public TerminalBuffer(int width, int height, int maxScrollbackSize, int cursorRow, int cursorCol, CellAttributes initialAttributes) {
        this.width = width;
        this.height = height;
        this.maxScrollbackSize = maxScrollbackSize;
        this.screen = new Cell[height][width];
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                this.screen[i][j] = Cell.EMPTY;
            }
        }
        this.scrollback = new ArrayDeque<>(maxScrollbackSize);
        this.cursor = new Cursor(this, cursorRow, cursorCol, initialAttributes);
    }

    public TerminalBuffer(int width, int height, int maxScrollbackSize, CellAttributes initialAttributes) {
        this(width, height, maxScrollbackSize, 0, 0, initialAttributes);
    }

    public TerminalBuffer(int width, int height, int maxScrollbackSize) {
        this(width, height, maxScrollbackSize, CellAttributes.DEFAULT);
    }

    // Getting the screen dimensions

    public int getWidth() {
        return this.width;
    }

    public int getHeight() {
        return this.height;
    }

    public int getMaxScrollbackSize() {
        return this.maxScrollbackSize;
    }

    // Setting the cursor position

    public void setCursorPosition(int row, int col) {
        this.cursor.setPosition(row, col);
    }

    public void setCursorRow(int row) {
        this.cursor.setRow(row);
    }

    public void setCursorCol(int col) {
        this.cursor.setCol(col);
    }

    // Moving the cursor

    public void moveCursorUp(int n) {
        this.cursor.moveUp(n);
    }

    public void moveCursorDown(int n) {
        this.cursor.moveDown(n);
    }

    public void moveCursorLeft(int n) {
        this.cursor.moveLeft(n);
    }

    public void moveCursorRight(int n) {
        this.cursor.moveRight(n);
    }

    // Getting the cursor position

    public int getCursorRow() {
        return this.cursor.getRow();
    }

    public int getCursorCol() {
        return this.cursor.getCol();
    }

    // Attribute management

    public void setCurrentForeground(TerminalColor color) {
        this.cursor.setCurrentForeground(color);
    }

    public void setCurrentBackground(TerminalColor color) {
        this.cursor.setCurrentBackground(color);
    }

    public void setCurrentStyles(StyleFlag... flags) {
        this.cursor.setCurrentStyles(flags);
    }

    public void setCurrentAttributes(CellAttributes attributes) {
        this.cursor.setCurrentAttributes(attributes);
    }

    public void resetCurrentAttributes() {
        this.cursor.resetCurrentAttributes();
    }

    public CellAttributes getCurrentAttributes() {
        return this.cursor.getCurrentAttributes();
    }

    // Content access

    public Cell getCellAt(int row, int col) {
        if (row < 0 || row >= height || col < 0 || col >= width) return null;
        return screen[row][col];
    }

    public Cell getCellAtCursor() {
        return getCellAt(cursor.getRow(), cursor.getCol());
    }

    // Editing

    private void shiftEverythingRight(int n) {
        int originalCol = cursor.getCol();
        int originalRow = cursor.getRow();
        
        Cursor shifter = new Cursor(this, originalRow, originalCol);
        int counter = n;
        List<Character> characters = new LinkedList<>();

        while (counter > 0 && !shifter.isAtEndOfScreen()) {
            if (getCellAt(shifter.getRow(), shifter.getCol()).isEmpty()) {
                counter--;
            }
            else {
                counter++;
                characters.add(getCellAt(shifter.getRow(), shifter.getCol()).character());
            }
            shifter.moveRightWrapped(1);
        }
        cursor.moveRightWrapped(n);
        for(char c : characters) {
            writeCharacter(c);
        }
        cursor.setPosition(originalRow, originalCol);
    }

    public void writeCharacter(char c) {
        if(cursor.isAtEndOfScreen()) {
            return; // TODO: Scroll up
        }
        screen[cursor.getRow()][cursor.getCol()] = new Cell(c, cursor.getCurrentAttributes());
        cursor.moveRightWrapped(1);
    }

    public void writeText(String text) {
        for (char c : text.toCharArray()) {
            writeCharacter(c);
        }
    }

    public void insertCharacter(char c) {
        shiftEverythingRight(1);
        writeCharacter(c);
    }

    public void insertText(String text) {
        int shift = text.length();
        shiftEverythingRight(shift);
        writeText(text);
    }

    public void fillLine(char c) {
        for (int i = 0; i < width; i++) {
            screen[cursor.getRow()][i] = new Cell(c, cursor.getCurrentAttributes());
            cursor.moveNextLineWrapped();
        }
    }
}
