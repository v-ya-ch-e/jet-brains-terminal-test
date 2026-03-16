package com.jetbrains.vyache.task;

import java.util.Set;

public record CellAttributes(TerminalColor foreground, TerminalColor background, Set<StyleFlag> styles) {

    public static final CellAttributes DEFAULT = 
            new CellAttributes(TerminalColor.DEFAULT, TerminalColor.DEFAULT, Set.<StyleFlag>of());

    public boolean hasStyle(StyleFlag flag) {
        return styles.contains(flag);
    }

    // Modifying the cell attributes

    public CellAttributes withForeground(TerminalColor color) {
        return new CellAttributes(color, background, styles);
    }

    public CellAttributes withBackground(TerminalColor color) {
        return new CellAttributes(foreground, color, styles);
    }

    public CellAttributes withStyles(StyleFlag... flags) {
        return new CellAttributes(foreground, background, Set.<StyleFlag>of(flags));
    }
}
