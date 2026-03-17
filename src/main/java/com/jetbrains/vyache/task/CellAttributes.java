package com.jetbrains.vyache.task;

import java.util.Set;

/**
 * Immutable value object describing the visual appearance of a terminal {@link Cell}.
 *
 * <p>Attributes include foreground colour, background colour, and a set of {@link StyleFlag}s
 * (bold, italic, underline). Because this is a Java {@code record}, equality and hashing
 * are derived from all three components automatically.
 *
 * <p>The class follows a "wither" pattern: each {@code with*()} method returns a new
 * {@code CellAttributes} with a single field replaced, leaving the original unchanged.
 * This makes it cheap to derive variations (e.g. change foreground while keeping
 * background and styles) without mutable state.
 *
 * @param foreground the foreground (text) colour
 * @param background the background colour
 * @param styles     the set of active style flags
 */
public record CellAttributes(TerminalColor foreground, TerminalColor background, Set<StyleFlag> styles) {

    /** Default attributes: default colours, no styles. */
    public static final CellAttributes DEFAULT = 
            new CellAttributes(TerminalColor.DEFAULT, TerminalColor.DEFAULT, Set.<StyleFlag>of());

    /**
     * Checks whether the given style flag is active in this attribute set.
     *
     * @param flag the style flag to test
     * @return {@code true} if the flag is present
     */
    public boolean hasStyle(StyleFlag flag) {
        return styles.contains(flag);
    }

    /**
     * Returns a copy with a different foreground colour.
     *
     * @param color the new foreground colour
     * @return new {@code CellAttributes} with the foreground replaced
     */
    public CellAttributes withForeground(TerminalColor color) {
        return new CellAttributes(color, background, styles);
    }

    /**
     * Returns a copy with a different background colour.
     *
     * @param color the new background colour
     * @return new {@code CellAttributes} with the background replaced
     */
    public CellAttributes withBackground(TerminalColor color) {
        return new CellAttributes(foreground, color, styles);
    }

    /**
     * Returns a copy with a completely replaced set of style flags.
     *
     * <p>Calling this method <em>replaces</em> all existing styles rather than
     * adding to them.  Pass an empty varargs to clear all styles.
     *
     * @param flags the new style flags
     * @return new {@code CellAttributes} with the styles replaced
     */
    public CellAttributes withStyles(StyleFlag... flags) {
        return new CellAttributes(foreground, background, Set.<StyleFlag>of(flags));
    }
}
