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
    public final Character DEFAULT_EMPTY_CHAR;
    public final Character DEFAULT_UNDEFINED_CHAR;

    // Initialization

    public TerminalBuffer(int width, int height, int maxScrollbackSize, int cursorRow, int cursorCol, CellAttributes initialAttributes,
                                Character defaultEmptyChar, Character defaultUndefinedChar) {
        this.width = width;
        this.height = height;
        this.maxScrollbackSize = maxScrollbackSize;
        this.screen = new Cell[height][width];
        for(int i = 0; i < height; i++) {
            Arrays.fill(screen[i], Cell.EMPTY);
        }
        this.scrollback = new ScrollbackRingBuffer(maxScrollbackSize);
        this.cursor = new Cursor(this, cursorRow, cursorCol, initialAttributes);
        DEFAULT_EMPTY_CHAR = defaultEmptyChar;
        DEFAULT_UNDEFINED_CHAR = defaultUndefinedChar;
    }

    public TerminalBuffer(int width, int height, int maxScrollbackSize, int cursorRow, int cursorCol, CellAttributes initialAttributes) {
        this(width, height, maxScrollbackSize, cursorRow, cursorCol, initialAttributes, ' ', Character.MIN_VALUE);
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

    public int getScrollbackSize() {
        return this.scrollback.size();
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
        if(performer.isAttachedTo(this))
            return getCellAt(performer.getRow(), performer.getCol());
        else 
            return null;
    }

    public CellAttributes getScreenAttributesAt(int row, int col) {
        Cell cell = getCellAt(row, col);
        if(cell == null) return null;
        return cell.attributes();
    }

    public CellAttributes getScrollbackAttributesAt(int row, int col) {
        Cell[] cellLine = scrollback.getElement(row);
        if(cellLine == null || col < 0 || col >= cellLine.length) return null;
        return cellLine[col].attributes();
    }

    public char getScreenCharAt(int row, int col) {
        Cell cell = getCellAt(row, col);
        if(cell == null) return DEFAULT_UNDEFINED_CHAR;
        if(cell.isEmpty()) return DEFAULT_EMPTY_CHAR;
        return cell.character();
    }

    public char getScrollbackCharAt(int row, int col) {
        Cell[] cellLine = scrollback.getElement(row);
        if(cellLine == null || col < 0 || col >= cellLine.length) return DEFAULT_UNDEFINED_CHAR;
        if(cellLine[col].isEmpty()) return DEFAULT_EMPTY_CHAR;
        return cellLine[col].character();
    }

    public String getScreenLineAsString(int row) {
        StringBuilder line = new StringBuilder();
        for(int i = 0; i < width; i++) {
            line.append(getScreenCharAt(row, i));
        }
        return line.toString();
    }

    public String getScrollbackLineAsString(int row) {
        StringBuilder line = new StringBuilder();
        for(int i = 0; i < width; i++) {
            line.append(getScrollbackCharAt(row, i));
        }
        return line.toString();
    }

    public String getScreenContent() {
        StringBuilder content = new StringBuilder();
        for(int i = 0; i < height; i++) {
            content.append(getScreenLineAsString(i));
            content.append("\n");
        }
        return content.toString();
    }

    public String getScrollbackContent() {
        StringBuilder content = new StringBuilder();
        for(int i = 0; i < scrollback.size(); i++) {
            content.append(getScrollbackLineAsString(i));
            content.append("\n");
        }
        return content.toString();
    }

    public String getFullContent() {
        StringBuilder content = new StringBuilder();
        content.append(getScrollbackContent());
        content.append(getScreenContent());
        return content.toString();
    }

    // Editing regarding the cursor position

    public void writeCharacter(char c, Cursor performer) {
        screen[performer.getRow()][performer.getCol()] = new Cell(c, performer.getCurrentAttributes());
        if(performer.isAtEndOfScreen()) {
            insertLineAtBottom();
            if(performer != cursor) {
                performer.moveUp(1);
            }
        }
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
