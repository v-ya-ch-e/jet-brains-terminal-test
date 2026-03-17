package com.jetbrains.vyache.task;

import java.util.LinkedList;
import java.util.Arrays;
import java.util.Queue;

/**
 * Core data structure of a terminal emulator — a grid of {@link Cell}s backed by a
 * scrollback history.
 *
 * <h2>Architecture overview</h2>
 * <pre>
 *  ┌──────────────────────┐
 *  │  ScrollbackRingBuffer│  ← lines that scrolled off the top (unmodifiable history)
 *  ├──────────────────────┤
 *  │                      │
 *  │    screen[height]    │  ← the visible/editable grid (width × height)
 *  │    [width]           │
 *  │          ▮ cursor    │
 *  └──────────────────────┘
 * </pre>
 *
 * <h3>Screen</h3>
 * <p>A 2-D array {@code Cell[height][width]} represents the visible portion of the
 * terminal. Every cell is initialised to {@link Cell#EMPTY}. Writes overwrite cells
 * in-place; the cursor advances after each character.
 *
 * <h3>Scrollback</h3>
 * <p>When a new empty line is inserted at the bottom ({@link #insertLineAtBottom()}),
 * the top row of the screen is pushed into a {@link ScrollbackRingBuffer} and all
 * remaining rows shift up by one.  The ring buffer silently discards the oldest
 * line when the configured maximum is exceeded.
 *
 * <h3>Cursor and attributes</h3>
 * <p>A single {@link Cursor} tracks the write position and carries the "current"
 * {@link CellAttributes}.  Every character written inherits the attributes that are
 * active at the time of the write.  Attributes can be changed between individual
 * character writes for per-cell styling.
 *
 * <h3>Coordinate system</h3>
 * <ul>
 *   <li>Screen coordinates: row ∈ [0, height-1], col ∈ [0, width-1]</li>
 *   <li>Scrollback coordinates: row 0 = oldest retained line</li>
 * </ul>
 *
 * <h3>Default characters</h3>
 * <ul>
 *   <li>{@code DEFAULT_EMPTY_CHAR} (default: {@code ' '}) — returned by
 *       {@code getScreenCharAt} / {@code getScrollbackCharAt} for empty cells.</li>
 *   <li>{@code DEFAULT_UNDEFINED_CHAR} (default: {@code \u0000}) — returned for
 *       out-of-bounds accesses.</li>
 * </ul>
 *
 * @see Cursor
 * @see Cell
 * @see CellAttributes
 * @see ScrollbackRingBuffer
 */
public class TerminalBuffer {

    private int width;
    private int height;
    private int maxScrollbackSize;
    private Cell[][] screen;
    private ScrollbackRingBuffer scrollback;
    private final Cursor cursor;
    private final char DEFAULT_EMPTY_CHAR;
    private final char DEFAULT_UNDEFINED_CHAR;

    // ─── Constructors ────────────────────────────────────────────────

    /**
     * Full constructor with every configurable parameter.
     *
     * @param width               number of columns
     * @param height              number of rows (visible screen)
     * @param maxScrollbackSize   maximum number of lines kept in scrollback
     * @param cursorRow           initial cursor row (clamped to bounds)
     * @param cursorCol           initial cursor column (clamped to bounds)
     * @param initialAttributes   starting cell attributes for the cursor
     * @param defaultEmptyChar    character returned for empty cells in content-access methods
     * @param defaultUndefinedChar character returned for out-of-bounds accesses
     */
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

    /**
     * Constructor with custom cursor position and attributes, using default empty/undefined chars.
     *
     * @param width             number of columns
     * @param height            number of rows
     * @param maxScrollbackSize maximum scrollback lines
     * @param cursorRow         initial cursor row
     * @param cursorCol         initial cursor column
     * @param initialAttributes starting attributes
     */
    public TerminalBuffer(int width, int height, int maxScrollbackSize, int cursorRow, int cursorCol, CellAttributes initialAttributes) {
        this(width, height, maxScrollbackSize, cursorRow, cursorCol, initialAttributes, ' ', Character.MIN_VALUE);
    }

    /**
     * Constructor with custom attributes, cursor starts at (0, 0).
     *
     * @param width             number of columns
     * @param height            number of rows
     * @param maxScrollbackSize maximum scrollback lines
     * @param initialAttributes starting attributes
     */
    public TerminalBuffer(int width, int height, int maxScrollbackSize, CellAttributes initialAttributes) {
        this(width, height, maxScrollbackSize, 0, 0, initialAttributes);
    }

    /**
     * Minimal constructor — cursor at origin, default attributes.
     *
     * @param width             number of columns
     * @param height            number of rows
     * @param maxScrollbackSize maximum scrollback lines
     */
    public TerminalBuffer(int width, int height, int maxScrollbackSize) {
        this(width, height, maxScrollbackSize, CellAttributes.DEFAULT);
    }

    // ─── Dimension and configuration queries ─────────────────────────

    /** @return the number of columns in the screen grid */
    public int getWidth() {
        return this.width;
    }

    /** @return the number of rows in the screen grid */
    public int getHeight() {
        return this.height;
    }

    /** @return the configured maximum number of scrollback lines */
    public int getMaxScrollbackSize() {
        return this.maxScrollbackSize;
    }

    /** @return the current number of lines stored in scrollback */
    public int getScrollbackSize() {
        return this.scrollback.size();
    }

    /** @return the character used to represent empty cells in string output */
    public char getDefaultEmptyChar() {
        return this.DEFAULT_EMPTY_CHAR;
    }

    /** @return the character returned for out-of-bounds cell accesses */
    public char getDefaultUndefinedChar() {
        return this.DEFAULT_UNDEFINED_CHAR;
    }

    // ─── Cursor position ───────────────────────────────────────────

    /**
     * Moves the cursor to an absolute screen position (clamped to bounds).
     *
     * @param row target row
     * @param col target column
     */
    public void setCursorPosition(int row, int col) {
        this.cursor.setPosition(row, col);
    }

    /**
     * Sets only the cursor row (clamped to {@code [0, height-1]}).
     *
     * @param row target row
     */
    public void setCursorRow(int row) {
        this.cursor.setRow(row);
    }

    /**
     * Sets only the cursor column (clamped to {@code [0, width-1]}).
     *
     * @param col target column
     */
    public void setCursorCol(int col) {
        this.cursor.setCol(col);
    }

    // ─── Cursor movement ────────────────────────────────────────────

    /**
     * Moves the cursor up by {@code n} rows, clamping at row 0.
     *
     * @param n number of rows
     */
    public void moveCursorUp(int n) {
        this.cursor.moveUp(n);
    }

    /**
     * Moves the cursor down by {@code n} rows, clamping at the last row.
     *
     * @param n number of rows
     */
    public void moveCursorDown(int n) {
        this.cursor.moveDown(n);
    }

    /**
     * Moves the cursor left by {@code n} columns, clamping at column 0.
     *
     * @param n number of columns
     */
    public void moveCursorLeft(int n) {
        this.cursor.moveLeft(n);
    }

    /**
     * Moves the cursor right by {@code n} columns, clamping at the last column.
     *
     * @param n number of columns
     */
    public void moveCursorRight(int n) {
        this.cursor.moveRight(n);
    }

    // ─── Cursor position queries ─────────────────────────────────────

    /** @return the cursor's current row (0-based) */
    public int getCursorRow() {
        return this.cursor.getRow();
    }

    /** @return the cursor's current column (0-based) */
    public int getCursorCol() {
        return this.cursor.getCol();
    }

    // ─── Attribute management ───────────────────────────────────────

    /**
     * Sets the foreground colour for subsequent writes, preserving background and styles.
     *
     * @param color the foreground colour
     */
    public void setCurrentForeground(TerminalColor color) {
        this.cursor.setCurrentForeground(color);
    }

    /**
     * Sets the background colour for subsequent writes, preserving foreground and styles.
     *
     * @param color the background colour
     */
    public void setCurrentBackground(TerminalColor color) {
        this.cursor.setCurrentBackground(color);
    }

    /**
     * Replaces the style flags for subsequent writes, preserving colours.
     *
     * @param flags the new style flags (replaces all previous flags)
     */
    public void setCurrentStyles(StyleFlag... flags) {
        this.cursor.setCurrentStyles(flags);
    }

    /**
     * Sets all attributes (foreground, background, styles) at once for subsequent writes.
     *
     * @param attributes the complete attribute set
     */
    public void setCurrentAttributes(CellAttributes attributes) {
        this.cursor.setCurrentAttributes(attributes);
    }

    /**
     * Resets the cursor's attributes to {@link CellAttributes#DEFAULT}
     * (default colours, no styles).
     */
    public void resetCurrentAttributes() {
        this.cursor.resetCurrentAttributes();
    }

    /**
     * Returns the current attributes that will be applied to the next write.
     *
     * @return the active {@link CellAttributes}
     */
    public CellAttributes getCurrentAttributes() {
        return this.cursor.getCurrentAttributes();
    }

    // ─── Content access — screen ───────────────────────────────────

    public Cell[][] getScreen() {
        return this.screen;
    }

    /**
     * Returns the {@link Cell} at the given screen position.
     *
     * @param row screen row
     * @param col screen column
     * @return the cell, or {@code null} if the coordinates are out of bounds
     */
    public Cell getCellAt(int row, int col) {
        if (row < 0 || row >= height || col < 0 || col >= width) return null;
        return screen[row][col];
    }

    /**
     * Returns the cell at the current cursor position.
     *
     * @return the cell under the cursor
     */
    public Cell getCellAtCursor() {
        return getCellAt(cursor.getRow(), cursor.getCol());
    }

    /**
     * Returns the cell at the position of an external {@link Cursor}.
     *
     * <p>If the given cursor belongs to a different buffer, {@code null} is
     * returned.  This guard prevents accidental cross-buffer reads when
     * auxiliary cursors are used in {@link #insertText(String)}.
     *
     * @param performer the cursor whose position to query
     * @return the cell, or {@code null} if the cursor is foreign or out of bounds
     */
    public Cell getCellAt(Cursor performer) {
        if(performer.isAttachedTo(this))
            return getCellAt(performer.getRow(), performer.getCol());
        else 
            return null;
    }

    /**
     * Returns the attributes of a screen cell.
     *
     * @param row screen row
     * @param col screen column
     * @return the cell's attributes, or {@code null} if out of bounds
     */
    public CellAttributes getScreenAttributesAt(int row, int col) {
        Cell cell = getCellAt(row, col);
        if(cell == null) return null;
        return cell.attributes();
    }

    /**
     * Returns the attributes of a scrollback cell.
     *
     * @param row scrollback row (0 = oldest)
     * @param col column
     * @return the cell's attributes, or {@code null} if out of bounds
     */
    public CellAttributes getScrollbackAttributesAt(int row, int col) {
        Cell[] cellLine = scrollback.getElement(row);
        if(cellLine == null || col < 0 || col >= cellLine.length) return null;
        return cellLine[col].attributes();
    }

    public char getScreenCharAt(int row, int col, char customEmptyChar, char customUndefinedChar) {
        Cell cell = getCellAt(row, col);
        if(cell == null) return customUndefinedChar;
        if(cell.isEmpty()) return customEmptyChar;
        return cell.character();
    }

    /**
     * Returns the character at a screen position.
     *
     * @param row screen row
     * @param col screen column
     * @return the character, {@link #getDefaultEmptyChar()} for empty cells,
     *         or {@link #getDefaultUndefinedChar()} if out of bounds
     */
    public char getScreenCharAt(int row, int col) {
        return getScreenCharAt(row, col, DEFAULT_EMPTY_CHAR, DEFAULT_UNDEFINED_CHAR);
    }

    public char getScrollbackCharAt(int row, int col, char customEmptyChar, char customUndefinedChar) {
        Cell[] cellLine = scrollback.getElement(row);
        if(cellLine == null || col < 0 || col >= cellLine.length) return customUndefinedChar;
        if(cellLine[col].isEmpty()) return customEmptyChar;
        return cellLine[col].character();
    }

    /**
     * Returns the character at a scrollback position.
     *
     * @param row scrollback row (0 = oldest)
     * @param col column
     * @return the character, {@link #getDefaultEmptyChar()} for empty cells,
     *         or {@link #getDefaultUndefinedChar()} if out of bounds
     */
    public char getScrollbackCharAt(int row, int col) {
        return getScrollbackCharAt(row, col, DEFAULT_EMPTY_CHAR, DEFAULT_UNDEFINED_CHAR);
    }

    public String getScreenLineAsString(int row, char customEmptyChar, char customUndefinedChar) {
        StringBuilder line = new StringBuilder();
        for(int i = 0; i < width; i++) {
            line.append(getScreenCharAt(row, i, customEmptyChar, customUndefinedChar));
        }
        return line.toString();
    }

    /**
     * Returns a screen row as a fixed-width string (padded with {@link #getDefaultEmptyChar()}).
     *
     * @param row screen row index
     * @return the row as a string of exactly {@link #getWidth()} characters
     */
    public String getScreenLineAsString(int row) {
        return getScreenLineAsString(row, DEFAULT_EMPTY_CHAR, DEFAULT_UNDEFINED_CHAR);
    }

    public String getScrollbackLineAsString(int row, char customEmptyChar, char customUndefinedChar) {
        StringBuilder line = new StringBuilder();
        for(int i = 0; i < width; i++) {
            line.append(getScrollbackCharAt(row, i, customEmptyChar, customUndefinedChar));
        }
        return line.toString();
    }

    /**
     * Returns a scrollback row as a fixed-width string.
     *
     * @param row scrollback row index (0 = oldest)
     * @return the row as a string of exactly {@link #getWidth()} characters
     */
    public String getScrollbackLineAsString(int row) {
        return getScrollbackLineAsString(row, DEFAULT_EMPTY_CHAR, DEFAULT_UNDEFINED_CHAR);
    }

    public String getScreenContent(char customEmptyChar, char customUndefinedChar) {
        StringBuilder content = new StringBuilder();
        for(int i = 0; i < height; i++) {
            content.append(getScreenLineAsString(i, customEmptyChar, customUndefinedChar));
            content.append("\n");
        }
        return content.toString();
    }

    /**
     * Returns the entire screen content as a multi-line string.
     *
     * <p>Each row is followed by a newline character. The result always
     * contains exactly {@link #getHeight()} lines of {@link #getWidth()} characters.
     *
     * @return the screen content
     */
    public String getScreenContent() {
        return getScreenContent(DEFAULT_EMPTY_CHAR, DEFAULT_UNDEFINED_CHAR);
    }

    public String getScrollbackContent(char customEmptyChar, char customUndefinedChar) {
        StringBuilder content = new StringBuilder();
        for(int i = 0; i < scrollback.size(); i++) {
            content.append(getScrollbackLineAsString(i, customEmptyChar, customUndefinedChar));
            content.append("\n");
        }
        return content.toString();
    }

    /**
     * Returns the entire scrollback content as a multi-line string.
     *
     * <p>Lines are ordered oldest-first. Returns an empty string if the
     * scrollback is empty.
     *
     * @return the scrollback content
     */
    public String getScrollbackContent() {
        return getScrollbackContent(DEFAULT_EMPTY_CHAR, DEFAULT_UNDEFINED_CHAR);
    }

    public String getFullContent(char customEmptyChar, char customUndefinedChar) {
        return getScrollbackContent(customEmptyChar, customUndefinedChar) + getScreenContent(customEmptyChar, customUndefinedChar);
    }

    /**
     * Returns scrollback + screen content combined (scrollback first).
     *
     * @return the full terminal content as a string
     */
    public String getFullContent() {
        return getFullContent(DEFAULT_EMPTY_CHAR, DEFAULT_UNDEFINED_CHAR);
    }

    // ─── Editing — cursor-relative operations ──────────────────────

    /**
     * Writes a cell at the performer's position, handling end-of-screen scrolling.
     *
     * <p>If the performer is at the very last cell of the screen (bottom-right),
     * a scroll is triggered ({@link #insertLineAtBottom()}) before advancing.
     * For auxiliary performers (not the main cursor), the performer is adjusted
     * up by one to compensate for the scroll shift; the main cursor's adjustment
     * is handled by {@code insertLineAtBottom} itself.
     */
    private void writeCell(Cell cell, Cursor performer) {
        screen[performer.getRow()][performer.getCol()] = cell;
        if(performer.isAtEndOfScreen()) {
            insertLineAtBottom();
            if(performer != cursor) {
                performer.moveUp(1);
            }
        }
        performer.moveRightWrapped(1);
    }

    private void writeCell(Cell cell) {
        writeCell(cell, cursor);
    }

    /**
     * Writes a single character at the given performer cursor's position using
     * that cursor's current attributes. Package-private to support auxiliary
     * cursors in {@link #insertText(String)}.
     */
    void writeCharacter(char c, Cursor performer) {
        writeCell(new Cell(c, performer.getCurrentAttributes()), performer);
    }

    /**
     * Writes a single character at the main cursor position with the current attributes.
     *
     * <p>The cursor advances right by one position after the write. If the cursor
     * was at the end of a line, it wraps to column 0 of the next line. If it was at
     * the very last cell of the screen, a scroll is triggered first.
     *
     * @param c the character to write
     */
    public void writeCharacter(char c) {
        writeCharacter(c, cursor);
    }

    /**
     * Writes a string starting at the current cursor position, <em>overwriting</em>
     * existing content. The cursor advances after each character.
     *
     * <p>This is the primary "print" operation of the terminal. Characters that
     * extend past the end of a line wrap to the next line; characters that extend
     * past the bottom of the screen trigger scrolling.
     *
     * @param text the text to write (may be empty)
     */
    public void writeText(String text) {
        for (char c : text.toCharArray()) {
            writeCharacter(c);
        }
    }

    /**
     * Inserts a single character at the cursor position, shifting existing content right.
     *
     * <p>Equivalent to {@code insertText(String.valueOf(c))}.
     *
     * @param c the character to insert
     */
    public void insertCharacter(char c) {
        insertText(String.valueOf(c));
    }

    /**
     * Inserts text at the cursor position, pushing existing characters to the right.
     *
     * <p>Unlike {@link #writeText(String)}, which overwrites cells, this method
     * preserves existing non-empty content by shifting it rightward. Characters
     * pushed past the end of a line wrap onto the next line; characters pushed
     * past the bottom of the screen cause scrolling.
     *
     * <h4>Algorithm</h4>
     * <ol>
     *   <li>The new characters are queued. For each new character written at the
     *       cursor, the cell previously occupying that position (if non-empty) is
     *       re-enqueued for later placement.</li>
     *   <li>Once all new characters have been placed, a secondary "shifter" cursor
     *       continues writing the displaced cells until the queue is drained.
     *       The shifter is a separate {@link Cursor} so that the main cursor's
     *       final position reflects only the inserted text length.</li>
     * </ol>
     *
     * <p>Displaced cells retain their original {@link CellAttributes} because the
     * {@code Cell} record (character + attributes) is re-enqueued as-is.
     *
     * @param text the text to insert
     */
    public void insertText(String text) {
        int insertLength = text.length();

        Queue<Cell> toInsert = new LinkedList<>();
        for(char c : text.toCharArray()) {
            toInsert.offer(new Cell(c, cursor.getCurrentAttributes()));
        }

        while (!toInsert.isEmpty() && insertLength > 0) {
            Cell c = toInsert.poll();
            insertLength--;
            if (!getCellAtCursor().isEmpty()) {
                toInsert.offer(getCellAtCursor());
            }
            writeCell(c, cursor);
        }

        Cursor shifter = new Cursor(this, cursor.getRow(), cursor.getCol(), cursor.getCurrentAttributes());
        while (!toInsert.isEmpty()) {
            Cell c = toInsert.poll();
            if (!getCellAt(shifter).isEmpty()) {
                toInsert.offer(getCellAt(shifter));
            }
            writeCell(c, shifter);
        }
    }

    /**
     * Fills the entire current row with the given character using the current attributes.
     *
     * <p>After filling, the cursor moves to column 0 of the next row. If the cursor
     * was on the last row, a scroll is triggered before the move.
     *
     * @param c the character to fill with
     */
    public void fillLine(char c) {
        for (int i = 0; i < width; i++) {
            screen[cursor.getRow()][i] = new Cell(c, cursor.getCurrentAttributes());
        }
        if(cursor.isAtLastLine()) {
            insertLineAtBottom();
        }
        cursor.moveDown(1);
        cursor.setCol(0);
    }

    // ─── Editing — scrollback and clearing ───────────────────────────

    /**
     * Scrolls the screen up by one line: the top row moves into scrollback,
     * rows shift up, and a blank row appears at the bottom.
     *
     * <p>The cursor row is decremented by one (clamped at 0) so that it
     * continues to point at the same logical content after the shift.
     */
    public void insertLineAtBottom() {
        scrollback.add(screen[0]);
        for(int i = 0; i < height - 1; i++) {
            screen[i] = screen[i + 1];
        }
        screen[height - 1] = new Cell[width];
        Arrays.fill(screen[height - 1], Cell.EMPTY);
        cursor.moveUp(1);
    }

    /**
     * Clears the visible screen by filling every cell with {@link Cell#EMPTY}.
     *
     * <p>Scrollback is <em>not</em> affected. The cursor position is preserved.
     */
    public void clearScreen() {
        for(int i = 0; i < height; i++) {
            Arrays.fill(screen[i], Cell.EMPTY);
        }
    }

    /**
     * Clears both the screen and the scrollback buffer.
     */
    public void clearAll() {
        clearScreen();
        scrollback.clear();
    }

    private void writeTextWithoutEmpty(Cell[] text) {
        for (Cell c : text) {
            if(c.isEmpty()) continue;
            writeCell(c, cursor);
        }
    }

    public void resize(int width, int height, int maxScrollbackSize) {
        TerminalBuffer newTerminal = new TerminalBuffer(width, height, maxScrollbackSize, 0, 0, cursor.getCurrentAttributes(), DEFAULT_EMPTY_CHAR, DEFAULT_UNDEFINED_CHAR);
        
        for(int i = 0; i < getScrollbackSize(); i++) {
            newTerminal.setCursorPosition(height-1, 0);
            newTerminal.writeTextWithoutEmpty(scrollback.getElement(i));
            if(newTerminal.cursor.isAtLastLine() && newTerminal.cursor.getCol() != 0) {
                newTerminal.insertLineAtBottom();
            }
        }

        for(int i = 0; i < getHeight(); i++) {
            newTerminal.setCursorPosition(height-1, 0);
            newTerminal.writeTextWithoutEmpty(screen[i]);
            if(newTerminal.cursor.isAtLastLine() && newTerminal.cursor.getCol() != 0) {
                newTerminal.insertLineAtBottom();
            }
        }

        this.width = width;
        this.height = height;
        this.maxScrollbackSize = maxScrollbackSize;
        this.screen = newTerminal.getScreen();
        this.cursor.setPosition(newTerminal.cursor.getRow(), newTerminal.cursor.getCol());
        this.scrollback = newTerminal.scrollback;
    }

    public void resize(int width, int height) {
        resize(width, height, maxScrollbackSize);
    }
}
