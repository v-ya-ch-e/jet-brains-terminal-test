package com.jetbrains.vyache.task;

import java.util.LinkedList;
import java.util.Arrays;
import java.util.Queue;

public class TerminalBuffer {

    private final int width;
    private final int height;
    private final int maxScrollbackSize;
    private final Cell[][] screen;
    private final ScrollbackRingBuffer scrollback;
    private final Cursor cursor;

    public TerminalBuffer(int width, int height, int maxScrollbackSize, int cursorRow, int cursorCol, CellAttributes initialAttributes) {
        this.width = width;
        this.height = height;
        this.maxScrollbackSize = maxScrollbackSize;
        this.screen = new Cell[height][width];
        for(int i = 0; i < height; i++) {
            Arrays.fill(screen[i], Cell.EMPTY);
        }
        this.scrollback = new ScrollbackRingBuffer(maxScrollbackSize);
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

    public Cell getCellAt(Cursor performer) {
        return getCellAt(performer.getRow(), performer.getCol());
    }

    // Editing regarding the cursor position

    // private void shiftEverythingRight(int n) {
    //     int originalCol = cursor.getCol();
    //     int originalRow = cursor.getRow();
        
    //     Cursor shifter = new Cursor(this, originalRow, originalCol, cursor.getCurrentAttributes());
    //     int counter = n;
    //     List<Character> characters = new LinkedList<>();

    //     while (counter > 0 && !shifter.isAtEndOfScreen()) {
    //         if (getCellAt(shifter.getRow(), shifter.getCol()).isEmpty()) {
    //             counter--;
    //         }
    //         else {
    //             counter++;
    //             characters.add(getCellAt(shifter.getRow(), shifter.getCol()).character());
    //         }
    //         shifter.moveRightWrapped(1);
    //     }
    //     cursor.moveRightWrapped(n);
    //     for(char c : characters) {
    //         writeCharacter(c);
    //     }
    //     cursor.setPosition(originalRow, originalCol);
    // }

    public void writeCharacter(char c, Cursor performer) {
        if(performer.isAtEndOfScreen()) {
            insertLineAtBottom();
            if(performer != cursor) {
                performer.moveUp(1);
            }
        }
        screen[performer.getRow()][performer.getCol()] = new Cell(c, performer.getCurrentAttributes());
        performer.moveRightWrapped(1);
    }

    public void writeCharacter(char c) {
        writeCharacter(c, cursor);
    }

    public void writeText(String text) {
        for (char c : text.toCharArray()) {
            writeCharacter(c);
        }
    }

    public void insertCharacter(char c) {
        insertText(String.valueOf(c));
    }

    public void insertText(String text) {
        int insertLength = text.length();

        Queue<Character> toInsert = new LinkedList<>();
        
        for(char c : text.toCharArray()) {
            toInsert.offer(c);
        }

        while (!toInsert.isEmpty() && insertLength > 0) {
            Character c = toInsert.poll();
            insertLength--;
            if (!getCellAtCursor().isEmpty()) {
                toInsert.offer(getCellAtCursor().character());
            }
            writeCharacter(c, cursor);
        }

        // Pushing the rest of the characters
        Cursor shifter = new Cursor(this, cursor.getRow(), cursor.getCol(), cursor.getCurrentAttributes());
        while (!toInsert.isEmpty()) {
            Character c = toInsert.poll();
            if (!getCellAt(shifter).isEmpty()) {
                toInsert.offer(getCellAt(shifter).character());
            }
            writeCharacter(c, shifter);
        }
    }

    public void fillLine(char c) {
        for (int i = 0; i < width; i++) {
            screen[cursor.getRow()][i] = new Cell(c, cursor.getCurrentAttributes());
        }
        if(cursor.isAtLastLine()) {
            insertLineAtBottom();
        }
        cursor.moveDown(1);
    }

    // Editing regarding the scrollback

    public void insertLineAtBottom() {
        scrollback.add(screen[0]);
        for(int i = 0; i < height - 1; i++) {
            screen[i] = screen[i + 1];
        }
        screen[height - 1] = new Cell[width];
        Arrays.fill(screen[height - 1], Cell.EMPTY);
        cursor.moveUp(1);
    }

    public void clearScreen() {
        for(int i = 0; i < height; i++) {
            Arrays.fill(screen[i], Cell.EMPTY);
        }
    }

    public void clearAll() {
        clearScreen();
        scrollback.clear();
    }
}
