package com.jetbrains.vyache.task;

/**
 * Terminal text style flags.
 *
 * <p>These flags are stored in a {@link java.util.Set} inside {@link CellAttributes}
 * so that any combination of styles can be active simultaneously. The set-based
 * approach was chosen over a bitmask to keep the API expressive and type-safe at the
 * small cost of a {@code Set} allocation; since attributes are immutable records
 * shared across many cells, this cost is amortised.
 */
public enum StyleFlag {
    /** Bold (increased weight) text. */
    BOLD,
    /** Italic (slanted) text. */
    ITALIC,
    /** Underlined text. */
    UNDERLINE
}
