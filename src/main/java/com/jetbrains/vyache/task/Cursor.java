package com.jetbrains.vyache.task;

public class Cursor {

    private final TerminalBuffer terminalBuffer;
    private int row;
    private int col;
    private CellAttributes currentAttributes;

    // Initialization

    public Cursor(TerminalBuffer terminalBuffer, int row, int col, CellAttributes currentAttributes) {
        this.terminalBuffer = terminalBuffer;
        this.row = Math.max(0, Math.min(row, terminalBuffer.getHeight() - 1));
        this.col = Math.max(0, Math.min(col, terminalBuffer.getWidth() - 1));
        this.currentAttributes = currentAttributes;
    }

    public Cursor(TerminalBuffer terminalBuffer, int row, int col) {
        this(terminalBuffer, row, col, CellAttributes.DEFAULT);
    }

    public Cursor(TerminalBuffer terminalBuffer) {
        this(terminalBuffer, 0, 0, CellAttributes.DEFAULT);
    }

    // Setting the cursor position

    public void setPosition(int row, int col) {
        this.row = Math.max(0, Math.min(row, terminalBuffer.getHeight() - 1));
        this.col = Math.max(0, Math.min(col, terminalBuffer.getWidth() - 1));
    }

    public void setRow(int row) {
        this.row = Math.max(0, Math.min(row, terminalBuffer.getHeight() - 1));
    }

    public void setCol(int col) {
        this.col = Math.max(0, Math.min(col, terminalBuffer.getWidth() - 1));
    }

    // Getting the cursor position

    public int getRow() {
        return row;
    }

    public int getCol() {
        return col;
    }

    public boolean isAtEndOfLine() {
        return col == terminalBuffer.getWidth() - 1;
    }

    public boolean isAtEndOfScreen() {
        return isAtLastLine() && isAtEndOfLine();
    }

    public boolean isAtLastLine() {
        return row == terminalBuffer.getHeight() - 1;
    }

    public boolean isAttachedTo(TerminalBuffer terminalBuffer) {
        return this.terminalBuffer == terminalBuffer;
    }

    // Moving the cursor

    public void moveUp(int n) {
        row = Math.max(0, row - n);
    }

    public void moveDown(int n) {
        row = Math.min(terminalBuffer.getHeight() - 1, row + n);
    }

    public void moveLeft(int n) {
        col = Math.max(0, col - n);
    }

    public void moveRight(int n) {
        col = Math.min(terminalBuffer.getWidth() - 1, col + n);
    }

    public boolean canMoveLeftWrapped(int n) {
        return row + Math.floorDiv((col - n), terminalBuffer.getWidth()) >= 0;
    }

    public void moveLeftWrapped(int n) {
        row = Math.floorMod(row + Math.floorDiv((col - n), terminalBuffer.getWidth()), terminalBuffer.getHeight());
        col = Math.floorMod((col - n), terminalBuffer.getWidth());
    }

    public boolean canMoveRightWrapped(int n) {
        return (row + (col + n) / terminalBuffer.getWidth()) < terminalBuffer.getHeight();
    }

    public void moveRightWrapped(int n) {
        row = (row + (col + n) / terminalBuffer.getWidth()) % terminalBuffer.getHeight();
        col = (col + n) % terminalBuffer.getWidth();
    }

    // Attribute management

    public void setCurrentAttributes(CellAttributes attributes) {
        this.currentAttributes = attributes;
    }

    public void resetCurrentAttributes() {
        this.currentAttributes = CellAttributes.DEFAULT;
    }

    public CellAttributes getCurrentAttributes() {
        return currentAttributes;
    }

    public void setCurrentForeground(TerminalColor color) {
        this.currentAttributes = this.currentAttributes.withForeground(color);
    }

    public void setCurrentBackground(TerminalColor color) {
        this.currentAttributes = this.currentAttributes.withBackground(color);
    }

    public void setCurrentStyles(StyleFlag... flags) {
        this.currentAttributes = this.currentAttributes.withStyles(flags);
    }

    // Distance calculation

    public int offsetTo(Cursor other) {
        if(other == null || !other.isAttachedTo(terminalBuffer)) {
            return Integer.MAX_VALUE;
        }
        return (other.getRow() - row)*terminalBuffer.getWidth() - col + other.getCol();
    }

    public int offsetTo(int row, int col) {
        return (row - this.row)*terminalBuffer.getWidth() - this.col + col;
    }
}