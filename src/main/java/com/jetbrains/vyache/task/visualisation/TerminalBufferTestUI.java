package com.jetbrains.vyache.task.visualisation;

import com.jetbrains.vyache.task.*;
import com.jetbrains.vyache.task.Cursor;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

public class TerminalBufferTestUI extends JFrame {

    // ===== Catppuccin Mocha palette =====
    private static final Color BASE       = new Color(30, 30, 46);
    private static final Color MANTLE     = new Color(24, 24, 37);
    private static final Color CRUST      = new Color(17, 17, 27);
    private static final Color SURFACE0   = new Color(49, 50, 68);
    private static final Color SURFACE1   = new Color(69, 71, 90);
    private static final Color SURFACE2   = new Color(88, 91, 112);
    private static final Color TEXT       = new Color(205, 214, 244);
    private static final Color SUBTEXT0   = new Color(166, 173, 200);
    private static final Color BLUE       = new Color(137, 180, 250);
    private static final Color PEACH      = new Color(250, 179, 135);
    private static final Color GREEN      = new Color(166, 227, 161);
    private static final Color RED        = new Color(243, 139, 168);
    private static final Color HOVER_BG   = new Color(59, 60, 78);

    private static final Color CURSOR_FILL   = new Color(137, 180, 250, 160);
    private static final Color CURSOR_BORDER = BLUE;
    private static final Color EMPTY_DOT     = new Color(88, 91, 112, 100);
    private static final Color GRID_BORDER   = SURFACE2;
    private static final Color SEP_BG        = new Color(69, 71, 90, 50);
    private static final Color SCROLLBACK_BG = new Color(20, 20, 32);

    static { setupTheme(); }

    // ===== State =====
    private TerminalBuffer buffer;

    // ===== Creation fields =====
    private final JTextField widthField           = tf("80", 5, "Buffer width in columns");
    private final JTextField heightField          = tf("24", 5, "Buffer height in rows");
    private final JTextField maxScrollbackField   = tf("1000", 5, "Maximum number of scrollback lines");
    private final JTextField initCursorRowField   = tf("0", 4, "Initial cursor row (for full constructor)");
    private final JTextField initCursorColField   = tf("0", 4, "Initial cursor column (for full constructor)");
    private final JComboBox<TerminalColor> initFgCombo  = cb(TerminalColor.values(), "Initial foreground color (for full constructor)");
    private final JComboBox<TerminalColor> initBgCombo  = cb(TerminalColor.values(), "Initial background color (for full constructor)");
    private final JCheckBox initBoldCb       = chk("Bold", "Initial bold style flag");
    private final JCheckBox initItalicCb     = chk("Italic", "Initial italic style flag");
    private final JCheckBox initUnderlineCb  = chk("Underline", "Initial underline style flag");
    private final JTextField emptyCharField  = tf(" ", 2, "Character shown for empty cells (default: space)");
    private final JTextField undefCharField  = tf("", 2, "Character shown for undefined/out-of-range cells");
    private final JLabel statusLabel         = lbl("No buffer. Create one first.");

    // ===== Display =====
    private final TerminalDisplayPanel termPanel = new TerminalDisplayPanel();
    private final JScrollPane termScroll;
    private final JLabel cursorLbl  = lbl("\u2590 Cursor: \u2014");
    private final JLabel attrsLbl   = lbl("\u25cf Attrs: \u2014");
    private final JLabel dimsLbl    = lbl("");

    // ===== Cursor tab =====
    private final JTextField cRowField = tf("0", 4, "Target cursor row");
    private final JTextField cColField = tf("0", 4, "Target cursor column");
    private final JTextField moveNField = tf("1", 3, "Number of positions to move");

    // ===== Attributes tab =====
    private final JComboBox<TerminalColor> aFgCombo = cb(TerminalColor.values(), "Foreground color to apply");
    private final JComboBox<TerminalColor> aBgCombo = cb(TerminalColor.values(), "Background color to apply");
    private final JCheckBox aBoldCb      = chk("Bold", "Bold style flag");
    private final JCheckBox aItalicCb    = chk("Italic", "Italic style flag");
    private final JCheckBox aUnderlineCb = chk("Underline", "Underline style flag");
    private final JLabel currentAttrsLbl = lbl("\u2014");

    // ===== Getters tab =====
    private final JTextField gRowField = tf("0", 4, "Row for screen/scrollback getter calls");
    private final JTextField gColField = tf("0", 4, "Column for screen/scrollback getter calls");
    private final JTextField pRowField = tf("0", 4, "Row for the performer Cursor object");
    private final JTextField pColField = tf("0", 4, "Column for the performer Cursor object");
    private final JTextArea getterResult = new JTextArea(5, 40);

    // ===== Editing tab =====
    private final JTextField wCharField  = tf("", 2, "Single character to write at cursor");
    private final JTextField iCharField  = tf("", 2, "Single character to insert at cursor");
    private final JTextField wTextField  = tf("", 18, "Text string to write sequentially");
    private final JTextField iTextField  = tf("", 18, "Text string to insert at cursor position");
    private final JTextField fCharField  = tf("", 2, "Character to fill the entire cursor row");
    private final JTextField pwRowField  = tf("0", 4, "Performer cursor row");
    private final JTextField pwColField  = tf("0", 4, "Performer cursor column");
    private final JTextField pwCharField = tf("", 2, "Character to write at performer position");

    // ===== Resize tab =====
    private final JTextField rWidthField  = tf("80", 5, "New buffer width in columns");
    private final JTextField rHeightField = tf("24", 5, "New buffer height in rows");
    private final JTextField rScrollField = tf("1000", 5, "New maximum scrollback lines (only for 3-arg resize)");

    // ===== Constructor =====
    public TerminalBufferTestUI() {
        setTitle("TerminalBuffer Test UI");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        getContentPane().setBackground(BASE);
        setLayout(new BorderLayout(6, 6));

        getterResult.setEditable(false);
        getterResult.setBackground(SURFACE0);
        getterResult.setForeground(TEXT);
        getterResult.setFont(mono());
        getterResult.setCaretColor(TEXT);

        termScroll = new JScrollPane(termPanel);
        termScroll.getViewport().setBackground(BASE);
        termScroll.setBorder(darkBorder("Terminal"));
        termScroll.getVerticalScrollBar().setUnitIncrement(16);

        JPanel statusBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 24, 4));
        statusBar.setBackground(MANTLE);
        statusBar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, SURFACE1));
        cursorLbl.setForeground(BLUE);
        cursorLbl.setFont(mono());
        attrsLbl.setForeground(PEACH);
        attrsLbl.setFont(mono());
        dimsLbl.setForeground(SUBTEXT0);
        dimsLbl.setFont(mono());
        statusBar.add(cursorLbl);
        statusBar.add(attrsLbl);
        statusBar.add(dimsLbl);

        JPanel center = new JPanel(new BorderLayout());
        center.setBackground(BASE);
        center.add(termScroll, BorderLayout.CENTER);
        center.add(statusBar, BorderLayout.SOUTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setBackground(SURFACE0);
        tabs.setForeground(TEXT);
        tabs.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        tabs.addTab("\u2630 Cursor", buildCursorPanel());
        tabs.addTab("\u2699 Attributes", buildAttributesPanel());
        tabs.addTab("\u2315 Getters", buildGettersPanel());
        tabs.addTab("\u270e Editing", buildEditingPanel());
        tabs.addTab("\u2922 Resize", buildResizePanel());
        tabs.setPreferredSize(new Dimension(0, 280));

        add(buildCreatePanel(), BorderLayout.NORTH);
        add(center, BorderLayout.CENTER);
        add(tabs, BorderLayout.SOUTH);

        setMinimumSize(new Dimension(1280, 920));
        setSize(1280, 920);
        setLocationRelativeTo(null);
    }

    // ================================================================
    //  Panel builders
    // ================================================================

    private JPanel buildCreatePanel() {
        JPanel p = darkGBPanel("Buffer creation");
        GridBagConstraints c = gbc();

        c.gridy = 0;
        addLF(p, c, "Width:", widthField);
        addLF(p, c, "Height:", heightField);
        addLF(p, c, "MaxScrollback:", maxScrollbackField);

        c.gridy = 1; c.gridx = 0;
        addLF(p, c, "Cursor Row:", initCursorRowField);
        addLF(p, c, "Col:", initCursorColField);
        addLF(p, c, "FG:", initFgCombo);
        addLF(p, c, "BG:", initBgCombo);

        c.gridy = 2; c.gridx = 0;
        p.add(initBoldCb, c); c.gridx++;
        p.add(initItalicCb, c); c.gridx++;
        p.add(initUnderlineCb, c); c.gridx++;
        addLF(p, c, "Empty char:", emptyCharField);
        addLF(p, c, "Undef char:", undefCharField);
        p.add(btn("Create (minimal)",
                "new TerminalBuffer(width, height, maxScrollback) \u2014 uses default cursor (0,0), default attributes, default chars",
                e -> createMinimal()), c); c.gridx++;
        p.add(btn("Create (full)",
                "new TerminalBuffer(width, height, maxScrollback, cursorRow, cursorCol, attrs, emptyChar, undefChar)",
                e -> createFull()), c); c.gridx++;

        c.gridy = 3; c.gridx = 0;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.fill = GridBagConstraints.HORIZONTAL;
        statusLabel.setForeground(PEACH);
        p.add(statusLabel, c);
        return p;
    }

    private JPanel buildCursorPanel() {
        JPanel p = darkGBPanel("Cursor position & movement");
        GridBagConstraints c = gbc();

        c.gridy = 0;
        addLF(p, c, "Row:", cRowField);
        addLF(p, c, "Col:", cColField);
        p.add(btn("Set position",
                "setCursorPosition(row, col) \u2014 moves cursor to absolute (row, col), clamped to screen bounds",
                e -> act(() -> buffer.setCursorPosition(pi(cRowField), pi(cColField)))), c); c.gridx++;
        p.add(btn("Set row",
                "setCursorRow(row) \u2014 changes only the cursor row, clamped to [0, height-1]",
                e -> act(() -> buffer.setCursorRow(pi(cRowField)))), c); c.gridx++;
        p.add(btn("Set col",
                "setCursorCol(col) \u2014 changes only the cursor column, clamped to [0, width-1]",
                e -> act(() -> buffer.setCursorCol(pi(cColField)))), c); c.gridx++;

        c.gridy = 1; c.gridx = 0;
        addLF(p, c, "Move n:", moveNField);
        p.add(btn("\u2191 Up",
                "moveCursorUp(n) \u2014 moves cursor up by n rows, clamped at row 0",
                e -> act(() -> buffer.moveCursorUp(pi(moveNField)))), c); c.gridx++;
        p.add(btn("\u2193 Down",
                "moveCursorDown(n) \u2014 moves cursor down by n rows, clamped at last row",
                e -> act(() -> buffer.moveCursorDown(pi(moveNField)))), c); c.gridx++;
        p.add(btn("\u2190 Left",
                "moveCursorLeft(n) \u2014 moves cursor left by n columns, clamped at col 0",
                e -> act(() -> buffer.moveCursorLeft(pi(moveNField)))), c); c.gridx++;
        p.add(btn("\u2192 Right",
                "moveCursorRight(n) \u2014 moves cursor right by n columns, clamped at last col",
                e -> act(() -> buffer.moveCursorRight(pi(moveNField)))), c); c.gridx++;
        return p;
    }

    private JPanel buildAttributesPanel() {
        JPanel p = darkGBPanel("Current attributes");
        GridBagConstraints c = gbc();

        c.gridy = 0;
        addLF(p, c, "Foreground:", aFgCombo);
        addLF(p, c, "Background:", aBgCombo);
        p.add(aBoldCb, c); c.gridx++;
        p.add(aItalicCb, c); c.gridx++;
        p.add(aUnderlineCb, c); c.gridx++;

        c.gridy = 1; c.gridx = 0;
        p.add(btn("Set foreground",
                "setCurrentForeground(color) \u2014 changes only the foreground color for subsequent writes",
                e -> act(() -> buffer.setCurrentForeground((TerminalColor) aFgCombo.getSelectedItem()))), c); c.gridx++;
        p.add(btn("Set background",
                "setCurrentBackground(color) \u2014 changes only the background color for subsequent writes",
                e -> act(() -> buffer.setCurrentBackground((TerminalColor) aBgCombo.getSelectedItem()))), c); c.gridx++;
        p.add(btn("Set styles",
                "setCurrentStyles(flags...) \u2014 replaces all style flags (bold/italic/underline) at once",
                e -> act(() -> buffer.setCurrentStyles(buildStyleFlags()))), c); c.gridx++;
        p.add(btn("Set all attributes",
                "setCurrentAttributes(attrs) \u2014 sets foreground, background, and styles in one call",
                e -> act(() -> buffer.setCurrentAttributes(buildAttrs(
                        (TerminalColor) aFgCombo.getSelectedItem(),
                        (TerminalColor) aBgCombo.getSelectedItem(),
                        aBoldCb.isSelected(), aItalicCb.isSelected(), aUnderlineCb.isSelected()
                )))), c); c.gridx++;
        p.add(btn("Reset attributes",
                "resetCurrentAttributes() \u2014 resets to CellAttributes.DEFAULT (default color, no styles)",
                e -> act(() -> buffer.resetCurrentAttributes())), c); c.gridx++;

        c.gridy = 2; c.gridx = 0;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.fill = GridBagConstraints.HORIZONTAL;
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        row.setBackground(SURFACE0);
        row.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));
        JLabel curLbl = lbl("getCurrentAttributes():");
        curLbl.setForeground(SUBTEXT0);
        row.add(curLbl);
        currentAttrsLbl.setForeground(PEACH);
        currentAttrsLbl.setFont(mono());
        row.add(currentAttrsLbl);
        p.add(row, c);
        return p;
    }

    private JPanel buildGettersPanel() {
        JPanel outer = new JPanel(new BorderLayout(4, 4));
        outer.setBackground(BASE);
        outer.setBorder(darkBorder("Getters"));

        JPanel inputs = darkGBPanel(null);
        inputs.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));
        GridBagConstraints ic = gbc();
        addLF(inputs, ic, "Row:", gRowField);
        addLF(inputs, ic, "Col:", gColField);
        addLF(inputs, ic, "Performer Row:", pRowField);
        addLF(inputs, ic, "Performer Col:", pColField);

        JPanel buttons = new JPanel(new GridLayout(0, 3, 4, 4));
        buttons.setBackground(BASE);
        buttons.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));
        gBtn(buttons, "getCellAt(row, col)",
                "Returns the Cell object at the given screen (row, col). null if out of bounds.",
                () -> fmtCell(buffer.getCellAt(pi(gRowField), pi(gColField))));
        gBtn(buttons, "getCellAtCursor()",
                "Returns the Cell at the current cursor position.",
                () -> fmtCell(buffer.getCellAtCursor()));
        gBtn(buttons, "getCellAt(Cursor)",
                "Creates new Cursor(buffer, performerRow, performerCol) and returns the Cell at that position.",
                () -> { com.jetbrains.vyache.task.Cursor p = new com.jetbrains.vyache.task.Cursor(buffer, pi(pRowField), pi(pColField)); return fmtCell(buffer.getCellAt(p)); });
        gBtn(buttons, "getScreenCharAt(r, c)",
                "Returns the character at screen (row, col). Empty cells return DEFAULT_EMPTY_CHAR.",
                () -> "'" + buffer.getScreenCharAt(pi(gRowField), pi(gColField)) + "'");
        gBtn(buttons, "getScrollbackCharAt(r, c)",
                "Returns the character at scrollback (row, col). Row 0 = oldest scrollback line.",
                () -> "'" + buffer.getScrollbackCharAt(pi(gRowField), pi(gColField)) + "'");
        gBtn(buttons, "getScreenLineAsString(row)",
                "Returns the entire screen row as a string of width characters.",
                () -> "\"" + buffer.getScreenLineAsString(pi(gRowField)) + "\"");
        gBtn(buttons, "getScrollbackLineAsString(row)",
                "Returns the scrollback row as a string. Row 0 = oldest line.",
                () -> "\"" + buffer.getScrollbackLineAsString(pi(gRowField)) + "\"");
        gBtn(buttons, "getScreenAttributesAt(r, c)",
                "Returns CellAttributes (fg, bg, styles) at the given screen position.",
                () -> { CellAttributes a = buffer.getScreenAttributesAt(pi(gRowField), pi(gColField)); return a == null ? "null" : a.toString(); });
        gBtn(buttons, "getScrollbackAttributesAt(r, c)",
                "Returns CellAttributes at the given scrollback position. Row 0 = oldest.",
                () -> { CellAttributes a = buffer.getScrollbackAttributesAt(pi(gRowField), pi(gColField)); return a == null ? "null" : a.toString(); });
        gBtn(buttons, "getScreenContent()",
                "Returns the full screen buffer as a multi-line string (height lines of width chars).",
                () -> buffer.getScreenContent());
        gBtn(buttons, "getScrollbackContent()",
                "Returns the full scrollback as a multi-line string (newest line first).",
                () -> buffer.getScrollbackContent());
        gBtn(buttons, "getFullContent()",
                "Returns getScrollbackContent() + getScreenContent() combined.",
                () -> buffer.getFullContent());

        JScrollPane rs = new JScrollPane(getterResult);
        rs.setPreferredSize(new Dimension(0, 90));
        rs.setBorder(darkBorder("Result"));
        rs.getViewport().setBackground(SURFACE0);

        outer.add(inputs, BorderLayout.NORTH);
        outer.add(buttons, BorderLayout.CENTER);
        outer.add(rs, BorderLayout.SOUTH);
        return outer;
    }

    private JPanel buildEditingPanel() {
        JPanel p = darkGBPanel("Editing");
        GridBagConstraints c = gbc();

        c.gridy = 0;
        addLF(p, c, "Char:", wCharField);
        p.add(btn("Write char",
                "writeCharacter(c) \u2014 writes character at cursor position, then advances cursor right (wraps at end of line)",
                e -> act(() -> { String s = wCharField.getText(); if (!s.isEmpty()) buffer.writeCharacter(s.charAt(0)); })), c); c.gridx++;
        p.add(Box.createHorizontalStrut(16), c); c.gridx++;
        addLF(p, c, "Char:", iCharField);
        p.add(btn("Insert char",
                "insertCharacter(c) \u2014 inserts character at cursor, shifting existing content to the right",
                e -> act(() -> { String s = iCharField.getText(); if (!s.isEmpty()) buffer.insertCharacter(s.charAt(0)); })), c); c.gridx++;

        c.gridy = 1; c.gridx = 0;
        addLF(p, c, "Text:", wTextField);
        p.add(btn("Write text",
                "writeText(text) \u2014 writes each character of the string sequentially using writeCharacter(c)",
                e -> act(() -> buffer.writeText(wTextField.getText()))), c); c.gridx++;
        p.add(Box.createHorizontalStrut(16), c); c.gridx++;
        addLF(p, c, "Text:", iTextField);
        p.add(btn("Insert text",
                "insertText(text) \u2014 inserts text at cursor, pushing all existing content to the right",
                e -> act(() -> buffer.insertText(iTextField.getText()))), c); c.gridx++;

        c.gridy = 2; c.gridx = 0;
        addLF(p, c, "Fill char:", fCharField);
        p.add(btn("Fill line",
                "fillLine(c) \u2014 fills the current cursor row entirely with the character, then moves cursor to next line",
                e -> act(() -> { String s = fCharField.getText(); if (!s.isEmpty()) buffer.fillLine(s.charAt(0)); })), c); c.gridx++;
        p.add(Box.createHorizontalStrut(16), c); c.gridx++;
        p.add(btn("Insert line at bottom",
                "insertLineAtBottom() \u2014 scrolls top screen line into scrollback, inserts a blank line at the bottom, cursor moves up by 1",
                e -> act(() -> buffer.insertLineAtBottom())), c); c.gridx++;
        p.add(btn("Clear screen",
                "clearScreen() \u2014 fills all screen cells with Cell.EMPTY (scrollback is untouched)",
                e -> act(() -> buffer.clearScreen())), c); c.gridx++;
        p.add(btn("Clear all",
                "clearAll() \u2014 clears both the screen and the scrollback buffer",
                e -> act(() -> buffer.clearAll())), c); c.gridx++;

//        c.gridy = 3; c.gridx = 0;
//        addLF(p, c, "Performer Row:", pwRowField);
//        addLF(p, c, "Col:", pwColField);
//        addLF(p, c, "Char:", pwCharField);
//        p.add(btn("Write char at performer",
//                "writeCharacter(c, Cursor) \u2014 creates new Cursor(buffer, row, col, currentAttrs) and writes the character at that position",
//                e -> act(() -> {
//                    String s = pwCharField.getText();
//                    if (!s.isEmpty()) {
//                        com.jetbrains.vyache.task.Cursor perf = new Cursor(buffer, pi(pwRowField), pi(pwColField), buffer.getCurrentAttributes());
//                        buffer.writeCharacter(s.charAt(0), perf);
//                    }
//                })), c); c.gridx++;

        return p;
    }

    private JPanel buildResizePanel() {
        JPanel p = darkGBPanel("Resize buffer");
        GridBagConstraints c = gbc();

        c.gridy = 0;
        addLF(p, c, "New width:", rWidthField);
        addLF(p, c, "New height:", rHeightField);
        p.add(btn("Resize (w, h)",
                "resize(width, height) \u2014 resizes to new dimensions, keeping the current maxScrollbackSize. Content is reflowed.",
                e -> act(() -> buffer.resize(pi(rWidthField), pi(rHeightField)))), c); c.gridx++;

        c.gridy = 1; c.gridx = 0;
        addLF(p, c, "New maxScrollback:", rScrollField);
        p.add(btn("Resize (w, h, maxScrollback)",
                "resize(width, height, maxScrollbackSize) \u2014 resizes to new dimensions with a new scrollback limit. Content is replayed and reflowed.",
                e -> act(() -> buffer.resize(pi(rWidthField), pi(rHeightField), pi(rScrollField)))), c); c.gridx++;

        c.gridy = 2; c.gridx = 0;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.fill = GridBagConstraints.HORIZONTAL;
        JLabel hint = lbl("Tip: long lines reflow (wrap) when narrowing. Previously wrapped lines do not re-join when widening.");
        hint.setForeground(SUBTEXT0);
        hint.setFont(hint.getFont().deriveFont(Font.ITALIC));
        p.add(hint, c);

        return p;
    }

    // ================================================================
    //  Buffer lifecycle
    // ================================================================

    private void createMinimal() {
        int w = pi(widthField), h = pi(heightField), max = pi(maxScrollbackField);
        if (w <= 0 || h <= 0 || max < 0) { statusLabel.setText("Invalid dimensions."); return; }
        setBuffer(new TerminalBuffer(w, h, max));
    }

    private void createFull() {
        int w = pi(widthField), h = pi(heightField), max = pi(maxScrollbackField);
        int cr = pi(initCursorRowField), cc = pi(initCursorColField);
        if (w <= 0 || h <= 0 || max < 0) { statusLabel.setText("Invalid dimensions."); return; }
        CellAttributes a = buildAttrs(
                (TerminalColor) initFgCombo.getSelectedItem(),
                (TerminalColor) initBgCombo.getSelectedItem(),
                initBoldCb.isSelected(), initItalicCb.isSelected(), initUnderlineCb.isSelected());
        String es = emptyCharField.getText(), us = undefCharField.getText();
        char ec = es.isEmpty() ? ' ' : es.charAt(0);
        char uc = us.isEmpty() ? Character.MIN_VALUE : us.charAt(0);
        setBuffer(new TerminalBuffer(w, h, max, cr, cc, a, ec, uc));
    }

    private void setBuffer(TerminalBuffer b) { this.buffer = b; refresh(); }

    private void act(Runnable action) {
        if (buffer == null) { JOptionPane.showMessageDialog(this, "Create a buffer first."); return; }
        action.run();
        refresh();
    }

    private void refresh() {
        if (buffer == null) {
            statusLabel.setText("No buffer. Create one first.");
            cursorLbl.setText("\u2590 Cursor: \u2014");
            attrsLbl.setText("\u25cf Attrs: \u2014");
            dimsLbl.setText("");
            currentAttrsLbl.setText("\u2014");
            termPanel.repaint();
            return;
        }
        statusLabel.setText(String.format("W=%d  H=%d  MaxScrollback=%d  EmptyChar='%s'  UndefChar='%s'",
                buffer.getWidth(), buffer.getHeight(), buffer.getMaxScrollbackSize(),
                buffer.getDefaultEmptyChar(), buffer.getDefaultUndefinedChar()));
        cursorLbl.setText(String.format("\u2590 Cursor: row=%d  col=%d", buffer.getCursorRow(), buffer.getCursorCol()));
        attrsLbl.setText("\u25cf Attrs: " + buffer.getCurrentAttributes());
        dimsLbl.setText(String.format("%dx%d  scrollback=%d lines", buffer.getWidth(), buffer.getHeight(), countScrollbackLines()));
        cRowField.setText(String.valueOf(buffer.getCursorRow()));
        cColField.setText(String.valueOf(buffer.getCursorCol()));
        currentAttrsLbl.setText(String.valueOf(buffer.getCurrentAttributes()));
        rWidthField.setText(String.valueOf(buffer.getWidth()));
        rHeightField.setText(String.valueOf(buffer.getHeight()));
        rScrollField.setText(String.valueOf(buffer.getMaxScrollbackSize()));

        termPanel.cachedSbLines = countScrollbackLines();
        termPanel.revalidate();
        termPanel.repaint();
        scrollToCursor();
    }

    private void scrollToCursor() {
        if (buffer == null) return;
        SwingUtilities.invokeLater(() -> {
            FontMetrics fm = termPanel.getFontMetrics(termPanel.getFont());
            int ch = fm.getHeight();
            int sbLines = termPanel.cachedSbLines;
            int hasSep = sbLines > 0 ? 1 : 0;
            int curVR = sbLines + hasSep + buffer.getCursorRow();
            int y = curVR * ch;
            termPanel.scrollRectToVisible(new Rectangle(0, Math.max(0, y - ch), 1, ch * 3));
        });
    }

    // ================================================================
    //  Terminal display panel (custom cell renderer)
    // ================================================================

    private class TerminalDisplayPanel extends JPanel implements Scrollable {
        private static final int GUTTER = 5;
        int cachedSbLines = 0;

        TerminalDisplayPanel() {
            setBackground(BASE);
            setFont(mono());
        }

        @Override
        public Dimension getPreferredSize() {
            if (buffer == null) return new Dimension(600, 300);
            FontMetrics fm = getFontMetrics(getFont());
            int cw = fm.charWidth('M'), ch = fm.getHeight();
            int hasSep = cachedSbLines > 0 ? 1 : 0;
            int rows = cachedSbLines + hasSep + buffer.getHeight();
            int cols = GUTTER + buffer.getWidth();
            return new Dimension(cols * cw + 4, rows * ch + 4);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            if (buffer == null) {
                g2.setColor(SUBTEXT0);
                g2.setFont(getFont().deriveFont(Font.ITALIC));
                g2.drawString("Create a buffer to begin.", 20, 30);
                return;
            }

            Font base = getFont();
            FontMetrics fm = g2.getFontMetrics(base);
            int cw = fm.charWidth('M'), ch = fm.getHeight(), asc = fm.getAscent();
            int gutterW = GUTTER * cw;
            int sbLines = cachedSbLines;
            int hasSep = sbLines > 0 ? 1 : 0;
            int scrH = buffer.getHeight(), cols = buffer.getWidth();
            int curRow = buffer.getCursorRow(), curCol = buffer.getCursorCol();

            Rectangle clip = g2.getClipBounds();
            int firstVR = clip != null ? Math.max(0, clip.y / ch) : 0;
            int totalRows = sbLines + hasSep + scrH;
            int lastVR = clip != null ? Math.min(totalRows - 1, (clip.y + clip.height) / ch + 1) : totalRows - 1;

            for (int vr = firstVR; vr <= lastVR; vr++) {
                int y = vr * ch;

                if (vr < sbLines) {
                    g2.setColor(SCROLLBACK_BG);
                    g2.fillRect(gutterW, y, cols * cw, ch);
                    drawGutter(g2, y, asc, cw, vr, SURFACE2, base);
                    for (int col = 0; col < cols; col++)
                        drawScrollbackCell(g2, vr, col, gutterW + col * cw, y, cw, ch, asc, base);

                } else if (hasSep == 1 && vr == sbLines) {
                    g2.setColor(SEP_BG);
                    g2.fillRect(0, y, getWidth(), ch);
                    g2.setColor(SURFACE2);
                    g2.drawLine(gutterW, y + ch / 2, gutterW + cols * cw, y + ch / 2);
                    g2.setFont(base.deriveFont(Font.ITALIC));
                    g2.setColor(SUBTEXT0);
                    String label = "\u2500\u2500\u2500 screen \u2500\u2500\u2500";
                    g2.drawString(label, gutterW + (cols * cw - fm.stringWidth(label)) / 2, y + asc);

                } else {
                    int sr = vr - sbLines - hasSep;
                    if (sr >= 0 && sr < scrH) {
                        drawGutter(g2, y, asc, cw, sr, SUBTEXT0, base);
                        for (int col = 0; col < cols; col++) {
                            boolean isCursor = (sr == curRow && col == curCol);
                            drawScreenCell(g2, sr, col, gutterW + col * cw, y, cw, ch, asc, base, isCursor);
                        }
                    }
                }
            }

            int screenY = (sbLines + hasSep) * ch;
            g2.setColor(GRID_BORDER);
            g2.drawRect(gutterW, screenY, cols * cw, scrH * ch);
            if (sbLines > 0) {
                g2.setColor(SURFACE1);
                g2.drawRect(gutterW, 0, cols * cw, sbLines * ch);
            }

            g2.setColor(SURFACE1);
            g2.drawLine(gutterW - 1, 0, gutterW - 1, totalRows * ch);
        }

        private void drawGutter(Graphics2D g2, int y, int asc, int cw, int lineNum, Color color, Font base) {
            g2.setColor(color);
            g2.setFont(base);
            g2.drawString(String.format("%4d", lineNum), 0, y + asc);
        }

        private void drawScreenCell(Graphics2D g2, int row, int col, int x, int y,
                                     int cw, int ch, int asc, Font base, boolean isCursor) {
            Cell cell = buffer.getCellAt(row, col);
            if (cell == null) return;
            CellAttributes attrs = cell.attributes();

            if (isCursor) {
                g2.setColor(CURSOR_FILL);
                g2.fillRect(x, y, cw, ch);
            } else {
                Color bg = termBg(attrs.background());
                if (bg != null) { g2.setColor(bg); g2.fillRect(x, y, cw, ch); }
            }

            if (cell.isEmpty()) {
                if (!isCursor) {
                    g2.setColor(EMPTY_DOT);
                    g2.fillOval(x + cw / 2 - 1, y + ch / 2 - 1, 3, 3);
                }
            } else {
                g2.setColor(isCursor ? CRUST : termFg(attrs.foreground()));
                g2.setFont(styledFont(base, attrs));
                g2.drawString(String.valueOf(cell.character()), x, y + asc);
                if (attrs.hasStyle(StyleFlag.UNDERLINE))
                    g2.drawLine(x, y + asc + 2, x + cw - 1, y + asc + 2);
            }

            if (isCursor) {
                g2.setColor(CURSOR_BORDER);
                g2.drawRect(x, y, cw - 1, ch - 1);
            }
        }

        private void drawScrollbackCell(Graphics2D g2, int sbIdx, int col, int x, int y,
                                         int cw, int ch, int asc, Font base) {
            CellAttributes attrs = buffer.getScrollbackAttributesAt(sbIdx, col);
            if (attrs == null) return;
            char character = buffer.getScrollbackCharAt(sbIdx, col);

            Color bg = termBg(attrs.background());
            if (bg != null) { g2.setColor(bg); g2.fillRect(x, y, cw, ch); }

            boolean isContent = character >= ' '
                    && character != buffer.getDefaultEmptyChar()
                    && character != buffer.getDefaultUndefinedChar();
            if (isContent) {
                g2.setColor(termFg(attrs.foreground()));
                g2.setFont(styledFont(base, attrs));
                g2.drawString(String.valueOf(character), x, y + asc);
                if (attrs.hasStyle(StyleFlag.UNDERLINE))
                    g2.drawLine(x, y + asc + 2, x + cw - 1, y + asc + 2);
            }
        }

        @Override public Dimension getPreferredScrollableViewportSize() {
            if (buffer == null) return new Dimension(600, 300);
            FontMetrics fm = getFontMetrics(getFont());
            int cw = fm.charWidth('M'), ch = fm.getHeight();
            int viewRows = Math.min(buffer.getHeight() + 6, cachedSbLines + (cachedSbLines > 0 ? 1 : 0) + buffer.getHeight());
            return new Dimension((GUTTER + buffer.getWidth()) * cw + 4, viewRows * ch);
        }
        @Override public int getScrollableUnitIncrement(Rectangle vr, int o, int d) {
            FontMetrics fm = getFontMetrics(getFont());
            return o == SwingConstants.VERTICAL ? fm.getHeight() : fm.charWidth('M');
        }
        @Override public int getScrollableBlockIncrement(Rectangle vr, int o, int d) {
            return o == SwingConstants.VERTICAL ? vr.height : vr.width;
        }
        @Override public boolean getScrollableTracksViewportWidth()  { return false; }
        @Override public boolean getScrollableTracksViewportHeight() { return false; }
    }

    // ================================================================
    //  Color mapping
    // ================================================================

    private static Color rawTermColor(TerminalColor tc) {
        return switch (tc) {
            case DEFAULT       -> null;
            case BLACK         -> new Color(69, 71, 90);
            case RED           -> new Color(243, 139, 168);
            case GREEN         -> new Color(166, 227, 161);
            case YELLOW        -> new Color(249, 226, 175);
            case BLUE          -> new Color(137, 180, 250);
            case MAGENTA       -> new Color(203, 166, 247);
            case CYAN          -> new Color(148, 226, 213);
            case WHITE         -> new Color(186, 194, 222);
            case BRIGHT_BLACK  -> new Color(88, 91, 112);
            case BRIGHT_RED    -> new Color(235, 160, 172);
            case BRIGHT_GREEN  -> new Color(179, 235, 176);
            case BRIGHT_YELLOW -> new Color(252, 235, 202);
            case BRIGHT_BLUE   -> new Color(162, 199, 252);
            case BRIGHT_MAGENTA-> new Color(218, 188, 250);
            case BRIGHT_CYAN   -> new Color(176, 235, 228);
            case BRIGHT_WHITE  -> new Color(205, 214, 244);
        };
    }

    private static Color termFg(TerminalColor tc) {
        if (tc == TerminalColor.DEFAULT) return TEXT;
        Color c = rawTermColor(tc);
        return c != null ? c : TEXT;
    }

    private static Color termBg(TerminalColor tc) {
        if (tc == TerminalColor.DEFAULT) return null;
        Color c = rawTermColor(tc);
        if (c == null) return null;
        return new Color(
                (c.getRed() + BASE.getRed() * 2) / 3,
                (c.getGreen() + BASE.getGreen() * 2) / 3,
                (c.getBlue() + BASE.getBlue() * 2) / 3);
    }

    private static Font styledFont(Font base, CellAttributes attrs) {
        int style = Font.PLAIN;
        if (attrs.hasStyle(StyleFlag.BOLD)) style |= Font.BOLD;
        if (attrs.hasStyle(StyleFlag.ITALIC)) style |= Font.ITALIC;
        return style != base.getStyle() ? base.deriveFont(style) : base;
    }

    // ================================================================
    //  Helpers
    // ================================================================

    private int countScrollbackLines() {
        if (buffer == null) return 0;
        return buffer.getScrollbackSize();
    }

    private static int pi(JTextField f) {
        try { return Integer.parseInt(f.getText().trim()); }
        catch (NumberFormatException e) { return 0; }
    }

    private static CellAttributes buildAttrs(TerminalColor fg, TerminalColor bg,
                                              boolean bold, boolean italic, boolean underline) {
        List<StyleFlag> flags = new ArrayList<>();
        if (bold) flags.add(StyleFlag.BOLD);
        if (italic) flags.add(StyleFlag.ITALIC);
        if (underline) flags.add(StyleFlag.UNDERLINE);
        CellAttributes a = CellAttributes.DEFAULT.withForeground(fg).withBackground(bg);
        return flags.isEmpty() ? a : a.withStyles(flags.toArray(new StyleFlag[0]));
    }

    private StyleFlag[] buildStyleFlags() {
        List<StyleFlag> list = new ArrayList<>();
        if (aBoldCb.isSelected()) list.add(StyleFlag.BOLD);
        if (aItalicCb.isSelected()) list.add(StyleFlag.ITALIC);
        if (aUnderlineCb.isSelected()) list.add(StyleFlag.UNDERLINE);
        return list.toArray(new StyleFlag[0]);
    }

    private static String fmtCell(Cell cell) {
        if (cell == null) return "null (out of bounds)";
        return "char='" + cell.character() + "'  empty=" + cell.isEmpty() + "  attrs=" + cell.attributes();
    }

    private void gBtn(JPanel panel, String label, String tooltip, java.util.function.Supplier<String> action) {
        JButton b = btn(label, tooltip, e -> {
            if (buffer == null) { JOptionPane.showMessageDialog(this, "Create a buffer first."); return; }
            getterResult.setText(action.get());
        });
        panel.add(b);
    }

    // ===== Widget factories =====

    private static Font mono() { return new Font(Font.MONOSPACED, Font.PLAIN, 13); }

    private static JTextField tf(String text, int cols, String tooltip) {
        JTextField f = new JTextField(text, cols);
        f.setBackground(SURFACE0); f.setForeground(TEXT); f.setCaretColor(TEXT);
        f.setToolTipText(tooltip);
        f.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(SURFACE1),
                BorderFactory.createEmptyBorder(3, 5, 3, 5)));
        return f;
    }

    @SuppressWarnings("unchecked")
    private static <E> JComboBox<E> cb(E[] items, String tooltip) {
        JComboBox<E> c = new JComboBox<>(items);
        c.setBackground(SURFACE0); c.setForeground(TEXT);
        c.setToolTipText(tooltip);
        return c;
    }

    private static JCheckBox chk(String text, String tooltip) {
        JCheckBox c = new JCheckBox(text);
        c.setBackground(BASE); c.setForeground(TEXT);
        c.setToolTipText(tooltip);
        return c;
    }

    private static JLabel lbl(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(TEXT);
        return l;
    }

    private static JButton btn(String text, String tooltip, java.awt.event.ActionListener al) {
        JButton b = new JButton(text);
        b.setBackground(SURFACE0); b.setForeground(TEXT);
        b.setFocusPainted(false);
        b.setToolTipText(tooltip);
        b.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(SURFACE1),
                BorderFactory.createEmptyBorder(5, 12, 5, 12)));
        b.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { b.setBackground(HOVER_BG); }
            @Override public void mouseExited(MouseEvent e)  { b.setBackground(SURFACE0); }
        });
        b.addActionListener(al);
        return b;
    }

    private static TitledBorder darkBorder(String title) {
        TitledBorder b = BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(SURFACE1), title);
        b.setTitleColor(SUBTEXT0);
        return b;
    }

    private static JPanel darkGBPanel(String title) {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(BASE);
        if (title != null) p.setBorder(darkBorder(title));
        return p;
    }

    private static GridBagConstraints gbc() {
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(3, 5, 3, 5);
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.NONE;
        c.gridx = 0; c.gridy = 0;
        return c;
    }

    private static void addLF(JPanel p, GridBagConstraints c, String label, JComponent field) {
        p.add(lbl(label), c); c.gridx++;
        p.add(field, c); c.gridx++;
    }

    // ================================================================
    //  Dark theme
    // ================================================================

    private static void setupTheme() {
        try { UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName()); }
        catch (Exception ignored) {}
        UIManager.put("Panel.background", BASE);
        UIManager.put("Label.foreground", TEXT);
        UIManager.put("TextField.background", SURFACE0);
        UIManager.put("TextField.foreground", TEXT);
        UIManager.put("TextField.caretForeground", TEXT);
        UIManager.put("TextArea.background", SURFACE0);
        UIManager.put("TextArea.foreground", TEXT);
        UIManager.put("Button.background", SURFACE0);
        UIManager.put("Button.foreground", TEXT);
        UIManager.put("ComboBox.background", SURFACE0);
        UIManager.put("ComboBox.foreground", TEXT);
        UIManager.put("ComboBox.selectionBackground", SURFACE1);
        UIManager.put("ComboBox.selectionForeground", TEXT);
        UIManager.put("CheckBox.background", BASE);
        UIManager.put("CheckBox.foreground", TEXT);
        UIManager.put("TabbedPane.background", BASE);
        UIManager.put("TabbedPane.foreground", TEXT);
        UIManager.put("TabbedPane.selected", SURFACE0);
        UIManager.put("TabbedPane.contentAreaColor", BASE);
        UIManager.put("ScrollPane.background", BASE);
        UIManager.put("ScrollBar.background", MANTLE);
        UIManager.put("ScrollBar.thumb", SURFACE1);
        UIManager.put("ScrollBar.thumbDarkShadow", SURFACE2);
        UIManager.put("ScrollBar.thumbHighlight", SURFACE1);
        UIManager.put("ScrollBar.thumbShadow", SURFACE0);
        UIManager.put("ScrollBar.track", MANTLE);
        UIManager.put("SplitPane.background", BASE);
        UIManager.put("OptionPane.background", BASE);
        UIManager.put("OptionPane.messageForeground", TEXT);
        UIManager.put("TitledBorder.titleColor", SUBTEXT0);
        UIManager.put("ToolTip.background", SURFACE0);
        UIManager.put("ToolTip.foreground", TEXT);
        UIManager.put("ToolTip.border", BorderFactory.createLineBorder(SURFACE2));
    }

    // ================================================================
    //  Main
    // ================================================================

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new TerminalBufferTestUI().setVisible(true));
    }
}
