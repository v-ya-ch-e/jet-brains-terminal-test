package com.jetbrains.vyache.task;

/**
 * Represents the cursor inside a {@link TerminalBuffer}.
 *
 * <p>The cursor tracks a <em>(row, col)</em> position on the screen grid and carries
 * the {@link CellAttributes} that will be stamped onto every character written through it.
 * Position values are always clamped to the screen bounds on construction and mutation,
 * so the cursor can never reference a cell outside the buffer.
 *
 * <h3>Design notes</h3>
 * <ul>
 *   <li>The cursor is <em>bound</em> to a specific {@code TerminalBuffer} at construction
 *       time.  This coupling is intentional: clamping logic needs the buffer's width/height,
 *       and helper methods like {@link #isAtEndOfScreen()} are meaningless without a
 *       buffer reference.</li>
 *   <li>Auxiliary cursors can be created independently of the buffer's "main" cursor
 *       (see {@link TerminalBuffer#insertText(String)}), enabling displacement writes
 *       that shift existing content to the right without moving the primary cursor.</li>
 *   <li>Wrapped movement ({@link #moveRightWrapped(int)}, {@link #moveLeftWrapped(int)})
 *       treats the screen as a flat linear array and converts between linear and
 *       2-D coordinates using division and modulo on the buffer width.  This is used
 *       internally by {@code writeCell} to advance the cursor across line boundaries.</li>
 * </ul>
 */
public class Cursor {

    private final TerminalBuffer terminalBuffer;
    private int row;
    private int col;
    private CellAttributes currentAttributes;

    // ─── Constructors ────────────────────────────────────────────────

    /**
     * Creates a cursor attached to the given buffer at the specified position.
     *
     * <p>Both {@code row} and {@code col} are clamped to
     * {@code [0, height-1]} and {@code [0, width-1]} respectively.
     *
     * @param terminalBuffer    the buffer this cursor belongs to
     * @param row               initial row (clamped)
     * @param col               initial column (clamped)
     * @param currentAttributes initial cell attributes for writes
     */
    public Cursor(TerminalBuffer terminalBuffer, int row, int col, CellAttributes currentAttributes) {
        this.terminalBuffer = terminalBuffer;
        this.row = Math.max(0, Math.min(row, terminalBuffer.getHeight() - 1));
        this.col = Math.max(0, Math.min(col, terminalBuffer.getWidth() - 1));
        this.currentAttributes = currentAttributes;
    }

    /**
     * Creates a cursor with default attributes at the specified position.
     *
     * @param terminalBuffer the buffer this cursor belongs to
     * @param row            initial row (clamped)
     * @param col            initial column (clamped)
     */
    public Cursor(TerminalBuffer terminalBuffer, int row, int col) {
        this(terminalBuffer, row, col, CellAttributes.DEFAULT);
    }

    /**
     * Creates a cursor at the origin (0, 0) with default attributes.
     *
     * @param terminalBuffer the buffer this cursor belongs to
     */
    public Cursor(TerminalBuffer terminalBuffer) {
        this(terminalBuffer, 0, 0, CellAttributes.DEFAULT);
    }

    // ─── Absolute positioning ────────────────────────────────────────

    /**
     * Moves the cursor to an absolute position, clamped to screen bounds.
     *
     * @param row target row
     * @param col target column
     */
    public void setPosition(int row, int col) {
        this.row = Math.max(0, Math.min(row, terminalBuffer.getHeight() - 1));
        this.col = Math.max(0, Math.min(col, terminalBuffer.getWidth() - 1));
    }

    /**
     * Sets only the cursor row, clamped to {@code [0, height-1]}.
     *
     * @param row target row
     */
    public void setRow(int row) {
        this.row = Math.max(0, Math.min(row, terminalBuffer.getHeight() - 1));
    }

    /**
     * Sets only the cursor column, clamped to {@code [0, width-1]}.
     *
     * @param col target column
     */
    public void setCol(int col) {
        this.col = Math.max(0, Math.min(col, terminalBuffer.getWidth() - 1));
    }

    // ─── Position queries ────────────────────────────────────────────

    /**
     * @return the current row index (0-based)
     */
    public int getRow() {
        return row;
    }

    /**
     * @return the current column index (0-based)
     */
    public int getCol() {
        return col;
    }

    /**
     * @return {@code true} if the cursor is on the last column of the current row
     */
    public boolean isAtEndOfLine() {
        return col == terminalBuffer.getWidth() - 1;
    }

    /**
     * @return {@code true} if the cursor is at the very last cell of the screen
     *         (last row, last column)
     */
    public boolean isAtEndOfScreen() {
        return isAtLastLine() && isAtEndOfLine();
    }

    /**
     * @return {@code true} if the cursor is on the last row of the screen
     */
    public boolean isAtLastLine() {
        return row == terminalBuffer.getHeight() - 1;
    }

    /**
     * Checks whether this cursor belongs to the given buffer (identity comparison).
     *
     * @param terminalBuffer the buffer to test against
     * @return {@code true} if this cursor was created for {@code terminalBuffer}
     */
    public boolean isAttachedTo(TerminalBuffer terminalBuffer) {
        return this.terminalBuffer == terminalBuffer;
    }

    // ─── Directional movement (clamped) ──────────────────────────────

    /**
     * Moves the cursor up by {@code n} rows, clamping at row 0.
     *
     * @param n number of rows to move (non-negative)
     */
    public void moveUp(int n) {
        row = Math.max(0, row - n);
    }

    /**
     * Moves the cursor down by {@code n} rows, clamping at the last row.
     *
     * @param n number of rows to move (non-negative)
     */
    public void moveDown(int n) {
        row = Math.min(terminalBuffer.getHeight() - 1, row + n);
    }

    /**
     * Moves the cursor left by {@code n} columns, clamping at column 0.
     *
     * @param n number of columns to move (non-negative)
     */
    public void moveLeft(int n) {
        col = Math.max(0, col - n);
    }

    /**
     * Moves the cursor right by {@code n} columns, clamping at the last column.
     *
     * @param n number of columns to move (non-negative)
     */
    public void moveRight(int n) {
        col = Math.min(terminalBuffer.getWidth() - 1, col + n);
    }

    /**
     * Tests whether a wrapped left-move of {@code n} positions would remain on-screen.
     *
     * @param n number of linear positions
     * @return {@code true} if the resulting row would be &ge; 0
     */
    public boolean canMoveLeftWrapped(int n) {
        return row + Math.floorDiv((col - n), terminalBuffer.getWidth()) >= 0;
    }

    /**
     * Moves the cursor left by {@code n} positions using line-wrapping.
     *
     * <p>The screen is treated as a flat linear array of {@code height * width} cells.
     * Moving left past column 0 wraps to the end of the previous row. The resulting
     * position wraps around the screen modularly.
     *
     * @param n number of linear positions to retreat
     */
    public void moveLeftWrapped(int n) {
        row = Math.floorMod(row + Math.floorDiv((col - n), terminalBuffer.getWidth()), terminalBuffer.getHeight());
        col = Math.floorMod((col - n), terminalBuffer.getWidth());
    }

    /**
     * Tests whether a wrapped right-move of {@code n} positions would remain on-screen.
     *
     * @param n number of linear positions
     * @return {@code true} if the resulting row would be within screen bounds
     */
    public boolean canMoveRightWrapped(int n) {
        return (row + (col + n) / terminalBuffer.getWidth()) < terminalBuffer.getHeight();
    }

    /**
     * Moves the cursor right by {@code n} positions using line-wrapping.
     *
     * <p>Moving past the last column wraps to column 0 of the next row.
     * The resulting position wraps around the screen modularly.
     *
     * @param n number of linear positions to advance
     */
    public void moveRightWrapped(int n) {
        row = (row + (col + n) / terminalBuffer.getWidth()) % terminalBuffer.getHeight();
        col = (col + n) % terminalBuffer.getWidth();
    }

    // ─── Attribute management ────────────────────────────────────────

    /**
     * Replaces the current attributes that will be applied to subsequent writes.
     *
     * @param attributes the new attributes
     */
    public void setCurrentAttributes(CellAttributes attributes) {
        this.currentAttributes = attributes;
    }

    /**
     * Resets the current attributes to {@link CellAttributes#DEFAULT}.
     */
    public void resetCurrentAttributes() {
        this.currentAttributes = CellAttributes.DEFAULT;
    }

    /**
     * @return the current cell attributes
     */
    public CellAttributes getCurrentAttributes() {
        return currentAttributes;
    }

    /**
     * Changes only the foreground colour, preserving background and styles.
     *
     * @param color the new foreground colour
     */
    public void setCurrentForeground(TerminalColor color) {
        this.currentAttributes = this.currentAttributes.withForeground(color);
    }

    /**
     * Changes only the background colour, preserving foreground and styles.
     *
     * @param color the new background colour
     */
    public void setCurrentBackground(TerminalColor color) {
        this.currentAttributes = this.currentAttributes.withBackground(color);
    }

    /**
     * Replaces the style flags, preserving foreground and background colours.
     *
     * @param flags the new style flags (replaces all previous flags)
     */
    public void setCurrentStyles(StyleFlag... flags) {
        this.currentAttributes = this.currentAttributes.withStyles(flags);
    }

    // ─── Distance calculation ────────────────────────────────────────

    /**
     * Computes the signed linear offset from this cursor to {@code other}.
     *
     * <p>The offset is the number of cells separating the two positions when
     * the screen is laid out as a flat array (row-major order). A positive
     * value means {@code other} is ahead (to the right / below); negative
     * means behind.
     *
     * @param other the target cursor (must belong to the same buffer)
     * @return the signed offset, or {@link Integer#MAX_VALUE} if {@code other}
     *         is {@code null} or belongs to a different buffer
     */
    public int offsetTo(Cursor other) {
        if(other == null || !other.isAttachedTo(terminalBuffer)) {
            return Integer.MAX_VALUE;
        }
        return (other.getRow() - row)*terminalBuffer.getWidth() - col + other.getCol();
    }

    /**
     * Computes the signed linear offset from this cursor to an arbitrary position.
     *
     * @param row target row
     * @param col target column
     * @return the signed offset in cells (row-major order)
     */
    public int offsetTo(int row, int col) {
        return (row - this.row)*terminalBuffer.getWidth() - this.col + col;
    }
}