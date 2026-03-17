package com.jetbrains.vyache.task;

/**
 * The 16 standard ANSI terminal colours plus a {@link #DEFAULT} sentinel.
 *
 * <p>{@code DEFAULT} means "use the terminal's default foreground/background" and is
 * the initial value for both foreground and background in {@link CellAttributes#DEFAULT}.
 * The remaining 16 values correspond to the SGR colour indices 0-7 (normal) and
 * 8-15 (bright/high-intensity) defined by ECMA-48 / ANSI X3.64.
 *
 * <p>Actual RGB rendering is left to the UI layer (see {@code TerminalBufferTestUI}),
 * keeping the data model independent of any colour scheme.
 */
public enum TerminalColor {
    /** Terminal's default colour (no explicit colour set). */
    DEFAULT,
    BLACK,
    RED,
    GREEN,
    YELLOW,
    BLUE,
    MAGENTA,
    CYAN,
    WHITE,
    BRIGHT_BLACK,
    BRIGHT_RED,
    BRIGHT_GREEN,
    BRIGHT_YELLOW,
    BRIGHT_BLUE,
    BRIGHT_MAGENTA,
    BRIGHT_CYAN,
    BRIGHT_WHITE
}
