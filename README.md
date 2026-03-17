# Terminal Text Buffer

A Java implementation of the core data structure that terminal emulators use to store and manipulate displayed text.
Built for the JetBrains Internships application.

## Building and running

**Prerequisites:** JDK 21+, Maven 3.8+

```bash
# Compile
mvn compile

# Run tests
mvn test

# Launch the interactive Test UI
mvn compile exec:java -Dexec.mainClass="com.jetbrains.vyache.task.visualisation.Main"
```

## Project structure

```
src/main/java/com/jetbrains/vyache/task/
├── TerminalBuffer.java          # Main buffer — screen grid + scrollback + public API
├── Cursor.java                  # Cursor position, movement, and current attributes
├── Cell.java                    # Single grid cell (character + attributes)
├── CellAttributes.java          # Immutable foreground/background/styles triple
├── TerminalColor.java           # 16 standard ANSI colours + DEFAULT
├── StyleFlag.java               # Bold, Italic, Underline flags
├── ScrollbackRingBuffer.java    # Fixed-capacity ring buffer for scrollback history
└── visualisation/
    ├── TerminalBufferTestUI.java # Swing-based interactive test harness
    └── Main.java                # Entry point for the UI
```

---

## Solution overview

### Architecture

The terminal buffer is decomposed into several focused, single-responsibility classes:

```
┌────────────────────────────────────────────────────┐
│                  TerminalBuffer                    │  Public façade — all terminal operations
│                                                    │
│  ┌──────────────────────┐  ┌────────────────────┐  │
│  │  Cell[height][width]  │  │ ScrollbackRingBuffer│ │
│  │  (screen grid)        │  │ (history lines)     │ │
│  └──────────────────────┘  └────────────────────┘  │
│                                                    │
│  ┌──────────────────────┐                          │
│  │       Cursor          │  Position + attributes  │
│  └──────────────────────┘                          │
└────────────────────────────────────────────────────┘
```

- **`TerminalBuffer`** is the public façade. It owns the screen grid (`Cell[][]`), the scrollback buffer, and the cursor. All mutating operations go through it.
- **`Cursor`** tracks (row, col) and the "current" `CellAttributes`. It is bound to a specific buffer at construction time so that clamping and boundary checks always use the correct dimensions.
- **`Cell`** is an immutable record holding a character and its attributes. The sentinel `Cell.EMPTY` (`character == \u0000`) represents an unoccupied position.
- **`CellAttributes`** is an immutable record with foreground colour, background colour, and a `Set<StyleFlag>`. It uses a "wither" pattern for cheap single-field derivation.
- **`ScrollbackRingBuffer`** is a package-private circular buffer of `Cell[]` rows.

### Data model: why immutable records?

`Cell` and `CellAttributes` are Java records. This gives several benefits:

1. **Thread-compatible** — no mutable shared state between cells.
2. **Referential transparency** — two cells with the same content are `.equals()`; test assertions are straightforward.
3. **No defensive copying** — records can be shared freely (e.g. when a displaced cell is re-enqueued in `insertText`, the original attributes are preserved for free).
4. **Compact** — `CellAttributes.DEFAULT` is a single shared instance; the majority of cells in a typical terminal session reference it.

### Coordinate system

| Region     | Row 0 meaning | Access methods |
|------------|---------------|----------------|
| Screen     | Top row of the visible terminal | `getScreenCharAt`, `getCellAt`, `getScreenLineAsString`, `getScreenContent` |
| Scrollback | Oldest retained line | `getScrollbackCharAt`, `getScrollbackLineAsString`, `getScrollbackContent` |
| Combined   | Oldest scrollback line, then screen | `getFullContent` |

### Default characters

Two configurable sentinels handle the distinction between "empty cell" and "out-of-bounds":

| Constant | Default value | Usage |
|----------|--------------|-------|
| `DEFAULT_EMPTY_CHAR` | `' '` (space) | Returned for cells that exist but have never been written to |
| `DEFAULT_UNDEFINED_CHAR` | `'\u0000'` (`Character.MIN_VALUE`) | Returned for coordinates outside the grid |

This allows callers to distinguish "no content" from "invalid access" without exceptions.

All content-access methods (`getScreenCharAt`, `getScrollbackCharAt`, `getScreenLineAsString`,
`getScrollbackLineAsString`, `getScreenContent`, `getScrollbackContent`, `getFullContent`) have
overloads that accept custom `(customEmptyChar, customUndefinedChar)` parameters, allowing
callers to override the defaults on a per-call basis without reconstructing the buffer.

---

## Functionality reference

### Setup

- Configurable width, height, and maximum scrollback size.
- Optional initial cursor position (clamped to bounds) and initial attributes.
- Optional custom empty/undefined characters.

### Cursor

- **Get/Set** — `getCursorRow()`, `getCursorCol()`, `setCursorPosition(row, col)`, `setCursorRow(row)`, `setCursorCol(col)`.
- **Move** — `moveCursorUp(n)`, `moveCursorDown(n)`, `moveCursorLeft(n)`, `moveCursorRight(n)`.
- All position values are clamped to screen bounds; the cursor never escapes the grid.

### Attributes

- `setCurrentForeground(color)` / `setCurrentBackground(color)` / `setCurrentStyles(flags...)` — change individual aspects.
- `setCurrentAttributes(attrs)` — replace all at once.
- `resetCurrentAttributes()` — revert to `CellAttributes.DEFAULT`.
- Attributes are "sticky": once set, they apply to every subsequent write until changed.

### Editing (cursor-relative)

| Method | Behaviour |
|--------|-----------|
| `writeCharacter(c)` | Overwrites the cell at the cursor, advances cursor right. Wraps at end of line; scrolls at end of screen. |
| `writeText(text)` | Calls `writeCharacter` for each character — a sequential "print". |
| `insertCharacter(c)` | Inserts at cursor, pushing existing non-empty content rightward. |
| `insertText(text)` | Inserts text at cursor. Displaced characters wrap to next lines; attributes of displaced cells are preserved. |
| `fillLine(c)` | Fills the cursor's entire row, then moves cursor to the start of the next line (scrolling if needed). |

### Editing (position-independent)

| Method | Behaviour |
|--------|-----------|
| `insertLineAtBottom()` | Scrolls screen up by 1: top row → scrollback, blank row at bottom, cursor row −1. |
| `clearScreen()` | Fills all screen cells with `Cell.EMPTY`. Scrollback untouched. Cursor position preserved. |
| `clearAll()` | Clears screen and scrollback. |
| `resize(width, height)` | Resizes screen dimensions, replaying all content into the new grid (see [Resize](#9-resize-via-content-replay) for details). |
| `resize(width, height, maxScrollbackSize)` | Resizes screen dimensions and scrollback limit. |

### Content access

| Method | Returns |
|--------|---------|
| `getCellAt(row, col)` | The `Cell` at a screen position (or `null`) |
| `getCellAtCursor()` | The `Cell` under the cursor |
| `getScreenCharAt(row, col)` | Character at screen position |
| `getScrollbackCharAt(row, col)` | Character at scrollback position |
| `getScreenLineAsString(row)` | Full-width string of a screen row |
| `getScrollbackLineAsString(row)` | Full-width string of a scrollback row |
| `getScreenContent()` | All screen rows as a newline-separated string |
| `getScrollbackContent()` | All scrollback rows (oldest first) |
| `getFullContent()` | Scrollback + screen combined |
| `getScreenAttributesAt(row, col)` | `CellAttributes` at a screen position |
| `getScrollbackAttributesAt(row, col)` | `CellAttributes` at a scrollback position |

All content-access methods above also have overloads accepting `(customEmptyChar, customUndefinedChar)`.

---

## Design decisions and trade-offs

### 1. Ring buffer for scrollback

**Decision:** Scrollback is stored in a `ScrollbackRingBuffer` — a fixed-size circular array.

**Rationale:** Terminal scrollback is a classic bounded FIFO. A ring buffer gives O(1) append and O(1) random access with zero copying. When the buffer is full, the oldest line is silently overwritten — exactly the semantics terminals require.

**Trade-off:** The maximum scrollback size must be known at construction time and cannot grow. This is acceptable for terminals where the scrollback limit is always a fixed setting.

**Detail:** The minimum capacity is clamped to 1 (not 0) to avoid divide-by-zero in the modulo arithmetic. A capacity of 0 therefore behaves as "remember only the most recent scrolled-off line".

### 2. Immutable Cell and CellAttributes (Java records)

**Decision:** Both `Cell` and `CellAttributes` are immutable records. Style flags are stored in an immutable `Set<StyleFlag>` rather than a bitmask.

**Rationale:**
- Immutability eliminates entire classes of bugs (shared mutable state, forgetting to copy).
- Records provide automatic `equals()`, `hashCode()`, and `toString()` — essential for testing.
- The `withForeground()` / `withBackground()` / `withStyles()` API reads naturally and composes well.

**Trade-off:** Every write creates a new `Cell` object. In practice, the JVM's young-generation GC handles short-lived record allocations very efficiently, and the dominant pattern (`CellAttributes.DEFAULT`) is a single shared instance.

**Alternative considered:** A bitmask for styles would save a `Set` allocation per `CellAttributes`, but the set is shared via immutable `Set.of()` and typical terminals use very few distinct attribute combinations, so the memory overhead is negligible and the type-safety benefit is worth it.

### 3. Cursor bound to buffer

**Decision:** Each `Cursor` holds a reference to the `TerminalBuffer` it belongs to, and all position mutations clamp against that buffer's dimensions.

**Rationale:** This makes it impossible to set the cursor to an out-of-bounds position. The `isAttachedTo()` check prevents accidental cross-buffer reads (see `getCellAt(Cursor)`).

**Trade-off:** The cursor cannot be reused across buffers. This is intentional — a cursor position is meaningless without knowing the grid size.

### 4. Write vs. Insert semantics

**Decision:** Two distinct writing modes:
- `writeText` — overwrites cells in-place (like normal terminal output).
- `insertText` — shifts existing content rightward (like an insert-mode editor).

**Rationale:** Both modes exist in real terminal emulators (CSI `@` for insert, normal output for overwrite). The `insertText` implementation uses a queue to track displaced cells and a secondary "shifter" cursor so that the main cursor's final position reflects only the newly inserted characters.

**Attribute preservation on insert:** When existing content is displaced, the `Cell` record (which bundles character + attributes) is re-enqueued as-is. This naturally preserves the original styling of displaced characters — no special-case code needed.

### 5. Scroll triggered by writing at end-of-screen

**Decision:** When a character is written at the very last cell (bottom-right), `insertLineAtBottom()` is called automatically. The main cursor is moved up by 1 inside `insertLineAtBottom()`; auxiliary cursors (used by `insertText`) compensate separately.

**Rationale:** This mirrors real terminal behaviour where the screen scrolls when output reaches the bottom-right corner.

### 6. `fillLine` moves cursor to next line

**Decision:** After filling a row, the cursor moves to column 0 of the next row (scrolling if on the last row).

**Rationale:** This matches the expectation of "fill this line and be ready to fill the next one", enabling easy iteration like filling multiple consecutive lines.

### 7. Configurable empty/undefined characters

**Decision:** The caller can configure which character represents "empty" and "out-of-bounds" when reading content as strings.

**Rationale:** Different consumers may want spaces, dots, or question marks for empty cells. Separating the data model (a cell is empty when `character == \u0000`) from the display representation (space, dot, etc.) keeps the core logic clean.

### 8. No external libraries (except testing)

The entire implementation uses only the Java standard library. JUnit 5 is the sole external dependency (test scope). This was a hard constraint of the task.

### 9. Resize via content replay

**Decision:** `resize(width, height, maxScrollbackSize)` creates a fresh temporary `TerminalBuffer` of the target dimensions, replays all existing content (scrollback then screen, line by line) into it, and then transplants the temporary buffer's internal state (screen grid, scrollback, cursor position) back into `this`.

**Rationale:** This approach reuses all existing write/scroll logic rather than implementing separate reflow code. By writing non-empty cells from each source row into the new grid starting at the bottom row and scrolling after each line, the replay naturally handles:
- **Narrowing:** long lines wrap onto multiple rows in the new grid.
- **Widening:** short lines simply have more trailing empty space.
- **Scrollback limit change:** the new ring buffer enforces the new limit during replay.

**Trade-offs:**
- **Empty lines are preserved:** a helper (`isLineEmpty`) detects all-empty source rows and still triggers a scroll for them, so blank lines in the middle of content survive the resize.
- **Wrapped lines are not re-joined:** when widening, a line that was previously soft-wrapped across two rows remains two separate rows instead of being merged back into one. This is because the buffer does not track soft-wrap vs. hard-wrap boundaries.
- **Cursor position is clamped:** the cursor keeps its pre-resize (row, col) if it fits the new dimensions, otherwise it is clamped to the new bounds. It does not track which logical content line it was on.
- **O(scrollback + screen) cost:** replay touches every stored cell. For typical terminal sizes this is negligible, but for extremely large scrollback buffers it can be noticeable.

### 10. Custom-char overloads for content access

**Decision:** Every content-reading method (`getScreenCharAt`, `getScrollbackCharAt`, `getScreenLineAsString`, etc.) has a two-parameter overload accepting `(customEmptyChar, customUndefinedChar)`. The no-arg versions delegate to these using the buffer's defaults.

**Rationale:** This enables callers (e.g. the Test UI, debug tools, or future renderers) to use different sentinel characters per-call without needing to reconstruct the buffer. The refactoring also reduces code duplication: each original method body was replaced by a single delegation call.

---

## Test coverage

The test suite (`TerminalBufferTest`) contains **181 tests** organized into nested classes:

| Group | Tests | Coverage focus |
|-------|-------|----------------|
| `Initialization` | 12 | Constructors, dimensions, default state, clamping |
| `CursorPosition` | 7 | Absolute set, clamping to bounds |
| `CursorMovement` | 9 | Directional moves, clamping, zero-move, compound movement |
| `AttributeManagement` | 8 | FG/BG/styles, reset, attributes applied to written cells |
| `ScreenContentAccess` | 9 | `getCellAt`, `getScreenCharAt`, `getScreenLineAsString`, `getScreenContent` |
| `ScrollbackContentAccess` | 7 | Scrollback char/attribute/line/content access, `getFullContent` |
| `WriteOperations` | 12 | Single char, text, wrapping, scrolling, overwrite, attributes |
| `InsertOperations` | 11 | Insert char/text, displacement, wrapping, attribute preservation |
| `FillLineOperations` | 7 | Fill, attributes, cursor movement, overwrite, scroll |
| `InsertLineAtBottom` | 6 | Scroll mechanics, scrollback push, cursor adjustment |
| `ClearOperations` | 6 | `clearScreen`, `clearAll`, scrollback survival, idempotency |
| `ScrollbackBehavior` | 7 | Ring buffer eviction, ordering, clear semantics |
| `EdgeCases` | 13 | 1×1 buffer, exact fills, foreign cursor, zero scrollback |
| `IntegrationScenarios` | 6 | Multi-step workflows, attribute survival across scrolling |
| `CursorOffsetTo` | 7 | Linear offset calculation, null/foreign buffer guards |
| `CustomCharOverloads` | 8 | Custom empty/undefined char overloads for all content-access methods |
| `ResizeDimensions` | 5 | Width/height/scrollback-limit updates, grid size |
| `ResizeContentPreservation` | 8 | Content survival, reflow on narrow, scrollback + screen order, attributes |
| `ResizeBlankLines` | 2 | Empty lines between content preserved, trailing empty lines |
| `ResizeCursorPosition` | 4 | Cursor clamping and preservation across resize |
| `ResizeScrollbackLimit` | 2 | Increasing/reducing scrollback limit during resize |
| `ResizeEdgeCases` | 10 | 1×1 buffers, double resize, narrow→wide, very large dimensions |

---

## Test UI

The `TerminalBufferTestUI` is a Swing application that provides an interactive visual test harness for every public method on `TerminalBuffer`. It uses the Catppuccin Mocha colour scheme.

### Panels

| Tab | Purpose |
|-----|---------|
| **Buffer creation** (top) | Create a buffer with minimal or full constructor parameters |
| **Cursor** | Set absolute position, move in all four directions by N cells |
| **Attributes** | Set foreground, background, styles individually or all at once; reset |
| **Getters** | Call any content-access method and display the result |
| **Editing** | Write char/text, insert char/text, fill line, insert line at bottom, clear screen/all |

The terminal grid is rendered in a custom `JPanel` with:
- Line numbers in a gutter
- Scrollback lines above a separator
- Empty cells shown as dim dots
- The cursor highlighted with a blue overlay
- Colours and bold/italic/underline rendered according to cell attributes

---

## Possible improvements

1. **Wide character support** — CJK ideographs and emoji occupy 2 cells in real terminals. This would require a `cellWidth` field on `Cell` and adjustments to cursor advancement and rendering.
2. **Line wrapping metadata** — tracking which lines are soft-wrapped (continuation of a long line) vs. hard-wrapped (explicit newline). This would allow resize to re-join soft-wrapped lines when widening and to preserve blank lines that were explicitly produced by the shell.
3. **Cursor position preservation on resize** — remembering the cursor's logical position (e.g. distance from the bottom of content) and restoring it after replay, rather than leaving it wherever the replay ends.
4. **Scroll regions** — ANSI terminals support scroll margins (DECSTBM), allowing only a sub-region of the screen to scroll.
5. **Copy-on-write rows** — for large screens with mostly empty rows, sharing a single `EMPTY_ROW` instance and only allocating a dedicated array on first write.
6. **StringBuilder pool** — the `getScreenContent()` / `getFullContent()` methods allocate a new `StringBuilder` on every call; a reusable buffer could reduce GC pressure in high-refresh scenarios.
