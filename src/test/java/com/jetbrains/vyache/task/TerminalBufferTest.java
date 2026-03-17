package com.jetbrains.vyache.task;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class TerminalBufferTest {

    private static final int WIDTH = 10;
    private static final int HEIGHT = 5;
    private static final int MAX_SCROLLBACK = 100;

    private TerminalBuffer buffer;

    @BeforeEach
    void setUp() {
        buffer = new TerminalBuffer(WIDTH, HEIGHT, MAX_SCROLLBACK);
    }

    // =====================================================================
    //  Initialization
    // =====================================================================

    @Nested
    class Initialization {

        @Test
        void dimensionsAreStoredCorrectly() {
            assertEquals(WIDTH, buffer.getWidth());
            assertEquals(HEIGHT, buffer.getHeight());
            assertEquals(MAX_SCROLLBACK, buffer.getMaxScrollbackSize());
        }

        @Test
        void customDimensions() {
            TerminalBuffer b = new TerminalBuffer(132, 43, 500);
            assertEquals(132, b.getWidth());
            assertEquals(43, b.getHeight());
            assertEquals(500, b.getMaxScrollbackSize());
        }

        @Test
        void screenStartsFilledWithEmptyCells() {
            for (int r = 0; r < HEIGHT; r++) {
                for (int c = 0; c < WIDTH; c++) {
                    Cell cell = buffer.getCellAt(r, c);
                    assertNotNull(cell);
                    assertTrue(cell.isEmpty());
                    assertEquals(CellAttributes.DEFAULT, cell.attributes());
                }
            }
        }

        @Test
        void cursorStartsAtOriginByDefault() {
            assertEquals(0, buffer.getCursorRow());
            assertEquals(0, buffer.getCursorCol());
        }

        @Test
        void cursorStartsAtSpecifiedPosition() {
            TerminalBuffer b = new TerminalBuffer(WIDTH, HEIGHT, MAX_SCROLLBACK, 2, 3, CellAttributes.DEFAULT);
            assertEquals(2, b.getCursorRow());
            assertEquals(3, b.getCursorCol());
        }

        @Test
        void cursorPositionClampedOnCreation() {
            TerminalBuffer b = new TerminalBuffer(WIDTH, HEIGHT, MAX_SCROLLBACK, 999, 999, CellAttributes.DEFAULT);
            assertEquals(HEIGHT - 1, b.getCursorRow());
            assertEquals(WIDTH - 1, b.getCursorCol());
        }

        @Test
        void negativeCursorPositionClampedOnCreation() {
            TerminalBuffer b = new TerminalBuffer(WIDTH, HEIGHT, MAX_SCROLLBACK, -5, -5, CellAttributes.DEFAULT);
            assertEquals(0, b.getCursorRow());
            assertEquals(0, b.getCursorCol());
        }

        @Test
        void initialAttributesAreDefault() {
            assertEquals(CellAttributes.DEFAULT, buffer.getCurrentAttributes());
        }

        @Test
        void customInitialAttributes() {
            CellAttributes attrs = new CellAttributes(TerminalColor.RED, TerminalColor.BLUE, Set.of(StyleFlag.BOLD));
            TerminalBuffer b = new TerminalBuffer(WIDTH, HEIGHT, MAX_SCROLLBACK, 0, 0, attrs);
            assertEquals(attrs, b.getCurrentAttributes());
        }

        @Test
        void defaultEmptyAndUndefinedChars() {
            assertEquals(' ', buffer.getDefaultEmptyChar());
            assertEquals(Character.MIN_VALUE, buffer.getDefaultUndefinedChar());
        }

        @Test
        void customEmptyAndUndefinedChars() {
            TerminalBuffer b = new TerminalBuffer(WIDTH, HEIGHT, MAX_SCROLLBACK, 0, 0, CellAttributes.DEFAULT, '.', '?');
            assertEquals('.', b.getDefaultEmptyChar());
            assertEquals('?', b.getDefaultUndefinedChar());
        }

        @Test
        void screenContentIsBlankOnStart() {
            String content = buffer.getScreenContent();
            String expectedLine = " ".repeat(WIDTH) + "\n";
            assertEquals(expectedLine.repeat(HEIGHT), content);
        }

        @Test
        void scrollbackIsEmptyOnStart() {
            assertEquals("", buffer.getScrollbackContent());
        }
    }

    // =====================================================================
    //  Cursor Position – Get / Set
    // =====================================================================

    @Nested
    class CursorPosition {

        @Test
        void setCursorPositionBothCoordinates() {
            buffer.setCursorPosition(3, 7);
            assertEquals(3, buffer.getCursorRow());
            assertEquals(7, buffer.getCursorCol());
        }

        @Test
        void setCursorRowOnly() {
            buffer.setCursorRow(4);
            assertEquals(4, buffer.getCursorRow());
            assertEquals(0, buffer.getCursorCol());
        }

        @Test
        void setCursorColOnly() {
            buffer.setCursorCol(9);
            assertEquals(0, buffer.getCursorRow());
            assertEquals(9, buffer.getCursorCol());
        }

        @Test
        void setCursorPositionClampedToUpperBounds() {
            buffer.setCursorPosition(100, 100);
            assertEquals(HEIGHT - 1, buffer.getCursorRow());
            assertEquals(WIDTH - 1, buffer.getCursorCol());
        }

        @Test
        void setCursorPositionClampedToLowerBounds() {
            buffer.setCursorPosition(-10, -10);
            assertEquals(0, buffer.getCursorRow());
            assertEquals(0, buffer.getCursorCol());
        }

        @Test
        void setCursorRowClamped() {
            buffer.setCursorRow(HEIGHT + 5);
            assertEquals(HEIGHT - 1, buffer.getCursorRow());
            buffer.setCursorRow(-3);
            assertEquals(0, buffer.getCursorRow());
        }

        @Test
        void setCursorColClamped() {
            buffer.setCursorCol(WIDTH + 5);
            assertEquals(WIDTH - 1, buffer.getCursorCol());
            buffer.setCursorCol(-3);
            assertEquals(0, buffer.getCursorCol());
        }

        @Test
        void setCursorToExactBoundary() {
            buffer.setCursorPosition(HEIGHT - 1, WIDTH - 1);
            assertEquals(HEIGHT - 1, buffer.getCursorRow());
            assertEquals(WIDTH - 1, buffer.getCursorCol());
        }
    }

    // =====================================================================
    //  Cursor Movement
    // =====================================================================

    @Nested
    class CursorMovement {

        @Test
        void moveCursorRight() {
            buffer.moveCursorRight(3);
            assertEquals(0, buffer.getCursorRow());
            assertEquals(3, buffer.getCursorCol());
        }

        @Test
        void moveCursorLeft() {
            buffer.setCursorCol(5);
            buffer.moveCursorLeft(3);
            assertEquals(2, buffer.getCursorCol());
        }

        @Test
        void moveCursorDown() {
            buffer.moveCursorDown(2);
            assertEquals(2, buffer.getCursorRow());
        }

        @Test
        void moveCursorUp() {
            buffer.setCursorRow(3);
            buffer.moveCursorUp(2);
            assertEquals(1, buffer.getCursorRow());
        }

        @Test
        void moveCursorRightClampsAtBoundary() {
            buffer.moveCursorRight(WIDTH + 10);
            assertEquals(WIDTH - 1, buffer.getCursorCol());
        }

        @Test
        void moveCursorLeftClampsAtZero() {
            buffer.moveCursorLeft(5);
            assertEquals(0, buffer.getCursorCol());
        }

        @Test
        void moveCursorDownClampsAtBoundary() {
            buffer.moveCursorDown(HEIGHT + 10);
            assertEquals(HEIGHT - 1, buffer.getCursorRow());
        }

        @Test
        void moveCursorUpClampsAtZero() {
            buffer.moveCursorUp(5);
            assertEquals(0, buffer.getCursorRow());
        }

        @Test
        void moveByZeroDoesNothing() {
            buffer.setCursorPosition(2, 5);
            buffer.moveCursorUp(0);
            buffer.moveCursorDown(0);
            buffer.moveCursorLeft(0);
            buffer.moveCursorRight(0);
            assertEquals(2, buffer.getCursorRow());
            assertEquals(5, buffer.getCursorCol());
        }

        @Test
        void moveCursorInAllDirections() {
            buffer.setCursorPosition(2, 5);
            buffer.moveCursorUp(1);
            buffer.moveCursorRight(2);
            buffer.moveCursorDown(2);
            buffer.moveCursorLeft(3);
            assertEquals(3, buffer.getCursorRow());
            assertEquals(4, buffer.getCursorCol());
        }
    }

    // =====================================================================
    //  Attribute Management
    // =====================================================================

    @Nested
    class AttributeManagement {

        @Test
        void setCurrentForeground() {
            buffer.setCurrentForeground(TerminalColor.RED);
            CellAttributes attrs = buffer.getCurrentAttributes();
            assertEquals(TerminalColor.RED, attrs.foreground());
            assertEquals(TerminalColor.DEFAULT, attrs.background());
            assertTrue(attrs.styles().isEmpty());
        }

        @Test
        void setCurrentBackground() {
            buffer.setCurrentBackground(TerminalColor.GREEN);
            CellAttributes attrs = buffer.getCurrentAttributes();
            assertEquals(TerminalColor.DEFAULT, attrs.foreground());
            assertEquals(TerminalColor.GREEN, attrs.background());
        }

        @Test
        void setCurrentStyles() {
            buffer.setCurrentStyles(StyleFlag.BOLD, StyleFlag.ITALIC);
            CellAttributes attrs = buffer.getCurrentAttributes();
            assertTrue(attrs.hasStyle(StyleFlag.BOLD));
            assertTrue(attrs.hasStyle(StyleFlag.ITALIC));
            assertFalse(attrs.hasStyle(StyleFlag.UNDERLINE));
        }

        @Test
        void setCurrentAttributes() {
            CellAttributes custom = new CellAttributes(TerminalColor.CYAN, TerminalColor.MAGENTA, Set.of(StyleFlag.UNDERLINE));
            buffer.setCurrentAttributes(custom);
            assertEquals(custom, buffer.getCurrentAttributes());
        }

        @Test
        void resetCurrentAttributes() {
            buffer.setCurrentForeground(TerminalColor.RED);
            buffer.setCurrentBackground(TerminalColor.BLUE);
            buffer.setCurrentStyles(StyleFlag.BOLD);
            buffer.resetCurrentAttributes();
            assertEquals(CellAttributes.DEFAULT, buffer.getCurrentAttributes());
        }

        @Test
        void attributesApplyToWrittenCharacters() {
            buffer.setCurrentForeground(TerminalColor.RED);
            buffer.setCurrentStyles(StyleFlag.BOLD);
            buffer.writeCharacter('X');
            Cell cell = buffer.getCellAt(0, 0);
            assertEquals(TerminalColor.RED, cell.attributes().foreground());
            assertTrue(cell.attributes().hasStyle(StyleFlag.BOLD));
        }

        @Test
        void changingAttributesBetweenWrites() {
            buffer.setCurrentForeground(TerminalColor.RED);
            buffer.writeCharacter('A');
            buffer.setCurrentForeground(TerminalColor.BLUE);
            buffer.writeCharacter('B');

            assertEquals(TerminalColor.RED, buffer.getCellAt(0, 0).attributes().foreground());
            assertEquals(TerminalColor.BLUE, buffer.getCellAt(0, 1).attributes().foreground());
        }

        @Test
        void setMultipleStyleFlagsReplacePrevious() {
            buffer.setCurrentStyles(StyleFlag.BOLD, StyleFlag.ITALIC);
            buffer.setCurrentStyles(StyleFlag.UNDERLINE);
            CellAttributes attrs = buffer.getCurrentAttributes();
            assertFalse(attrs.hasStyle(StyleFlag.BOLD));
            assertFalse(attrs.hasStyle(StyleFlag.ITALIC));
            assertTrue(attrs.hasStyle(StyleFlag.UNDERLINE));
        }
    }

    // =====================================================================
    //  Content Access – Screen
    // =====================================================================

    @Nested
    class ScreenContentAccess {

        @Test
        void getCellAtReturnsCorrectCell() {
            buffer.writeCharacter('A');
            Cell cell = buffer.getCellAt(0, 0);
            assertEquals('A', cell.character());
        }

        @Test
        void getCellAtOutOfBoundsReturnsNull() {
            assertNull(buffer.getCellAt(-1, 0));
            assertNull(buffer.getCellAt(HEIGHT, 0));
            assertNull(buffer.getCellAt(0, -1));
            assertNull(buffer.getCellAt(0, WIDTH));
        }

        @Test
        void getCellAtCursorReturnsCurrentPosition() {
            buffer.setCursorPosition(2, 3);
            buffer.writeCharacter('Z');
            buffer.setCursorPosition(2, 3);
            Cell cell = buffer.getCellAtCursor();
            assertEquals('Z', cell.character());
        }

        @Test
        void getScreenCharAtReturnsCharacter() {
            buffer.writeCharacter('X');
            assertEquals('X', buffer.getScreenCharAt(0, 0));
        }

        @Test
        void getScreenCharAtEmptyCellReturnsDefaultEmpty() {
            assertEquals(' ', buffer.getScreenCharAt(0, 0));
        }

        @Test
        void getScreenCharAtOutOfBoundsReturnsDefaultUndefined() {
            assertEquals(Character.MIN_VALUE, buffer.getScreenCharAt(-1, 0));
            assertEquals(Character.MIN_VALUE, buffer.getScreenCharAt(0, WIDTH));
        }

        @Test
        void getScreenAttributesAtReturnsAttributes() {
            buffer.setCurrentForeground(TerminalColor.RED);
            buffer.writeCharacter('A');
            CellAttributes attrs = buffer.getScreenAttributesAt(0, 0);
            assertEquals(TerminalColor.RED, attrs.foreground());
        }

        @Test
        void getScreenAttributesAtOutOfBoundsReturnsNull() {
            assertNull(buffer.getScreenAttributesAt(-1, 0));
            assertNull(buffer.getScreenAttributesAt(0, WIDTH));
        }

        @Test
        void getScreenLineAsString() {
            buffer.writeText("Hello");
            String line = buffer.getScreenLineAsString(0);
            assertEquals("Hello" + " ".repeat(WIDTH - 5), line);
        }

        @Test
        void getScreenLineAsStringEmptyLine() {
            assertEquals(" ".repeat(WIDTH), buffer.getScreenLineAsString(0));
        }

        @Test
        void getScreenContent() {
            buffer.writeText("Hi");
            String content = buffer.getScreenContent();
            String firstLine = "Hi" + " ".repeat(WIDTH - 2) + "\n";
            String emptyLine = " ".repeat(WIDTH) + "\n";
            assertEquals(firstLine + emptyLine.repeat(HEIGHT - 1), content);
        }

        @Test
        void getScreenContentAllEmpty() {
            String emptyLine = " ".repeat(WIDTH) + "\n";
            assertEquals(emptyLine.repeat(HEIGHT), buffer.getScreenContent());
        }
    }

    // =====================================================================
    //  Content Access – Scrollback
    // =====================================================================

    @Nested
    class ScrollbackContentAccess {

        @Test
        void scrollbackCharAtReturnsCorrectChar() {
            buffer.writeText("ABCDEFGHIJ");
            buffer.insertLineAtBottom();
            assertEquals('A', buffer.getScrollbackCharAt(0, 0));
            assertEquals('J', buffer.getScrollbackCharAt(0, 9));
        }

        @Test
        void scrollbackCharAtOutOfBoundsReturnsUndefined() {
            assertEquals(Character.MIN_VALUE, buffer.getScrollbackCharAt(0, 0));
            assertEquals(Character.MIN_VALUE, buffer.getScrollbackCharAt(-1, 0));
        }

        @Test
        void scrollbackAttributesAtReturnsCorrectAttributes() {
            buffer.setCurrentForeground(TerminalColor.GREEN);
            buffer.writeText("ABCDEFGHIJ");
            buffer.insertLineAtBottom();
            CellAttributes attrs = buffer.getScrollbackAttributesAt(0, 0);
            assertNotNull(attrs);
            assertEquals(TerminalColor.GREEN, attrs.foreground());
        }

        @Test
        void scrollbackAttributesAtOutOfBoundsReturnsNull() {
            assertNull(buffer.getScrollbackAttributesAt(0, 0));
            assertNull(buffer.getScrollbackAttributesAt(-1, 0));
        }

        @Test
        void scrollbackAttributesAtInvalidColReturnsNull() {
            buffer.writeText("AB");
            buffer.insertLineAtBottom();
            assertNull(buffer.getScrollbackAttributesAt(0, WIDTH + 5));
            assertNull(buffer.getScrollbackAttributesAt(0, -1));
        }

        @Test
        void getScrollbackLineAsString() {
            buffer.writeText("HelloWorld");
            buffer.insertLineAtBottom();
            assertEquals("HelloWorld", buffer.getScrollbackLineAsString(0));
        }

        @Test
        void getScrollbackContent() {
            buffer.setCursorPosition(0, 0);
            buffer.writeText("LINE_ONE__");
            buffer.setCursorPosition(1, 0);
            buffer.writeText("LINE_TWO__");
            buffer.insertLineAtBottom();
            buffer.insertLineAtBottom();
            String content = buffer.getScrollbackContent();
            assertTrue(content.contains("LINE_ONE__"));
            assertTrue(content.contains("LINE_TWO__"));
        }

        @Test
        void getFullContentIncludesBoth() {
            buffer.writeText("TopLine___");
            buffer.insertLineAtBottom();
            buffer.setCursorPosition(0, 0);
            buffer.writeText("ScreenLine");
            String full = buffer.getFullContent();
            assertTrue(full.contains("TopLine___"));
            assertTrue(full.contains("ScreenLine"));
        }
    }

    // =====================================================================
    //  Writing Characters and Text
    // =====================================================================

    @Nested
    class WriteOperations {

        @Test
        void writeCharacterAtOrigin() {
            buffer.writeCharacter('A');
            assertEquals('A', buffer.getScreenCharAt(0, 0));
            assertEquals(0, buffer.getCursorRow());
            assertEquals(1, buffer.getCursorCol());
        }

        @Test
        void writeCharacterAdvancesCursor() {
            buffer.writeCharacter('A');
            buffer.writeCharacter('B');
            buffer.writeCharacter('C');
            assertEquals('A', buffer.getScreenCharAt(0, 0));
            assertEquals('B', buffer.getScreenCharAt(0, 1));
            assertEquals('C', buffer.getScreenCharAt(0, 2));
            assertEquals(3, buffer.getCursorCol());
        }

        @Test
        void writeCharacterWrapsToNextLine() {
            for (int i = 0; i < WIDTH; i++) {
                buffer.writeCharacter((char) ('A' + i));
            }
            assertEquals(1, buffer.getCursorRow());
            assertEquals(0, buffer.getCursorCol());
        }

        @Test
        void writeCharacterAtEndOfScreenScrolls() {
            buffer.setCursorPosition(HEIGHT - 1, WIDTH - 1);
            buffer.writeCharacter('Z');
            assertEquals('Z', buffer.getScreenCharAt(HEIGHT - 2, WIDTH - 1));
        }

        @Test
        void writeCharacterAtEndOfScreenPushesToScrollback() {
            buffer.setCursorPosition(0, 0);
            buffer.writeText("TOP_LINE__");
            buffer.setCursorPosition(HEIGHT - 1, WIDTH - 1);
            buffer.writeCharacter('Z');
            assertTrue(buffer.getScrollbackContent().contains("TOP_LINE__"));
        }

        @Test
        void writeTextBasic() {
            buffer.writeText("Hello");
            assertEquals("Hello" + " ".repeat(WIDTH - 5), buffer.getScreenLineAsString(0));
        }

        @Test
        void writeTextOverridesExistingContent() {
            buffer.writeText("XXXXXXXXXX");
            buffer.setCursorPosition(0, 0);
            buffer.writeText("Hi");
            assertEquals('H', buffer.getScreenCharAt(0, 0));
            assertEquals('i', buffer.getScreenCharAt(0, 1));
            assertEquals('X', buffer.getScreenCharAt(0, 2));
        }

        @Test
        void writeTextWrapsAcrossLines() {
            String text = "ABCDEFGHIJ" + "KLMNO";
            buffer.writeText(text);
            assertEquals("ABCDEFGHIJ", buffer.getScreenLineAsString(0));
            String secondLine = buffer.getScreenLineAsString(1);
            assertTrue(secondLine.startsWith("KLMNO"));
        }

        @Test
        void writeTextFillsAllButTriggersScrollOnLastChar() {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < WIDTH * HEIGHT; i++) {
                sb.append((char) ('A' + (i % 26)));
            }
            buffer.writeText(sb.toString());
            // The last character write triggers a scroll: row 0 goes to scrollback,
            // everything shifts up, and the last row becomes empty except the last
            // char is written at (HEIGHT-2, WIDTH-1) after scroll.
            assertFalse(buffer.getScrollbackContent().isEmpty());
            // After scroll, old row 1 is now row 0
            for (int c = 0; c < WIDTH; c++) {
                int idx = WIDTH + c;
                assertEquals((char) ('A' + (idx % 26)), buffer.getScreenCharAt(0, c));
            }
            // Last row is empty (new line inserted at bottom)
            assertEquals(' ', buffer.getScreenCharAt(HEIGHT - 1, 0));
        }

        @Test
        void writeTextScrollsWhenExceedingScreen() {
            buffer.writeText("X".repeat(WIDTH * (HEIGHT + 2)));
            assertFalse(buffer.getScrollbackContent().isEmpty());
        }

        @Test
        void writeTextWithAttributes() {
            buffer.setCurrentForeground(TerminalColor.YELLOW);
            buffer.setCurrentBackground(TerminalColor.BLACK);
            buffer.writeText("Hi");
            CellAttributes a0 = buffer.getScreenAttributesAt(0, 0);
            CellAttributes a1 = buffer.getScreenAttributesAt(0, 1);
            assertEquals(TerminalColor.YELLOW, a0.foreground());
            assertEquals(TerminalColor.BLACK, a0.background());
            assertEquals(TerminalColor.YELLOW, a1.foreground());
        }

        @Test
        void writeCharacterAtSpecificPositionViaCursorSet() {
            buffer.setCursorPosition(2, 5);
            buffer.writeCharacter('Q');
            assertEquals('Q', buffer.getScreenCharAt(2, 5));
        }

        @Test
        void writeEmptyString() {
            buffer.writeText("");
            assertEquals(0, buffer.getCursorRow());
            assertEquals(0, buffer.getCursorCol());
            assertTrue(buffer.getCellAt(0, 0).isEmpty());
        }

        @Test
        void writeSingleCharacterString() {
            buffer.writeText("Z");
            assertEquals('Z', buffer.getScreenCharAt(0, 0));
            assertEquals(0, buffer.getCursorRow());
            assertEquals(1, buffer.getCursorCol());
        }
    }

    // =====================================================================
    //  Insert Operations
    // =====================================================================

    @Nested
    class InsertOperations {

        @Test
        void insertCharacterPushesExistingContent() {
            buffer.writeText("BCD");
            buffer.setCursorPosition(0, 0);
            buffer.insertCharacter('A');
            assertEquals('A', buffer.getScreenCharAt(0, 0));
            assertEquals('B', buffer.getScreenCharAt(0, 1));
            assertEquals('C', buffer.getScreenCharAt(0, 2));
            assertEquals('D', buffer.getScreenCharAt(0, 3));
        }

        @Test
        void insertTextPushesExistingContent() {
            buffer.writeText("CD");
            buffer.setCursorPosition(0, 0);
            buffer.insertText("AB");
            assertEquals('A', buffer.getScreenCharAt(0, 0));
            assertEquals('B', buffer.getScreenCharAt(0, 1));
            assertEquals('C', buffer.getScreenCharAt(0, 2));
            assertEquals('D', buffer.getScreenCharAt(0, 3));
        }

        @Test
        void insertTextIntoEmptyBuffer() {
            buffer.insertText("Hello");
            assertEquals('H', buffer.getScreenCharAt(0, 0));
            assertEquals('e', buffer.getScreenCharAt(0, 1));
            assertEquals('l', buffer.getScreenCharAt(0, 2));
            assertEquals('l', buffer.getScreenCharAt(0, 3));
            assertEquals('o', buffer.getScreenCharAt(0, 4));
        }

        @Test
        void insertTextAtMiddleOfLine() {
            buffer.writeText("AE");
            buffer.setCursorPosition(0, 1);
            buffer.insertText("BCD");
            assertEquals('A', buffer.getScreenCharAt(0, 0));
            assertEquals('B', buffer.getScreenCharAt(0, 1));
            assertEquals('C', buffer.getScreenCharAt(0, 2));
            assertEquals('D', buffer.getScreenCharAt(0, 3));
            assertEquals('E', buffer.getScreenCharAt(0, 4));
        }

        @Test
        void insertTextWrapsExistingContentToNextLine() {
            String fill = "ABCDEFGHIJ";
            buffer.writeText(fill);
            buffer.setCursorPosition(0, 0);
            buffer.insertText("XY");
            // After inserting "XY" at the start, existing chars shift right by 2
            // Row 0: X Y A B C D E F G H
            // Row 1: I J (rest empty)
            assertEquals('X', buffer.getScreenCharAt(0, 0));
            assertEquals('Y', buffer.getScreenCharAt(0, 1));
            assertEquals('A', buffer.getScreenCharAt(0, 2));
            assertEquals('G', buffer.getScreenCharAt(0, 8));
            assertEquals('H', buffer.getScreenCharAt(0, 9));
            assertEquals('I', buffer.getScreenCharAt(1, 0));
            assertEquals('J', buffer.getScreenCharAt(1, 1));
        }

        @Test
        void insertTextMoveCursor() {
            buffer.insertText("ABC");
            assertEquals(0, buffer.getCursorRow());
            assertEquals(3, buffer.getCursorCol());
        }

        @Test
        void insertCharacterAdvancesCursorByOne() {
            buffer.insertCharacter('X');
            assertEquals(0, buffer.getCursorRow());
            assertEquals(1, buffer.getCursorCol());
        }

        @Test
        void insertOnFullLineWrapsToNextLine() {
            buffer.writeText("ABCDEFGHIJ");
            buffer.setCursorPosition(0, 5);
            buffer.insertText("12345");

            assertEquals('A', buffer.getScreenCharAt(0, 0));
            assertEquals('1', buffer.getScreenCharAt(0, 5));
            assertEquals('5', buffer.getScreenCharAt(0, 9));

            assertEquals('F', buffer.getScreenCharAt(1, 0));
            assertEquals('J', buffer.getScreenCharAt(1, 4));
        }

        @Test
        void insertTextDisplacedCharRetainsOriginalForeground() {
            buffer.setCurrentForeground(TerminalColor.RED);
            buffer.writeText("AB");
            buffer.setCursorPosition(0, 0);
            buffer.setCurrentForeground(TerminalColor.GREEN);
            buffer.insertText("X");

            assertEquals(TerminalColor.GREEN, buffer.getScreenAttributesAt(0, 0).foreground()); // inserted 'X'
            assertEquals(TerminalColor.RED, buffer.getScreenAttributesAt(0, 1).foreground());   // displaced 'A'
            assertEquals(TerminalColor.RED, buffer.getScreenAttributesAt(0, 2).foreground());   // displaced 'B'
        }

        @Test
        void insertTextDisplacedCharRetainsOriginalBackground() {
            buffer.setCurrentBackground(TerminalColor.BLUE);
            buffer.writeText("AB");
            buffer.setCursorPosition(0, 0);
            buffer.setCurrentBackground(TerminalColor.YELLOW);
            buffer.insertText("Z");

            assertEquals(TerminalColor.YELLOW, buffer.getScreenAttributesAt(0, 0).background()); // 'Z'
            assertEquals(TerminalColor.BLUE, buffer.getScreenAttributesAt(0, 1).background());   // displaced 'A'
            assertEquals(TerminalColor.BLUE, buffer.getScreenAttributesAt(0, 2).background());   // displaced 'B'
        }

        @Test
        void insertTextDisplacedCharRetainsOriginalStyles() {
            buffer.setCurrentStyles(StyleFlag.BOLD);
            buffer.writeText("AB");
            buffer.setCursorPosition(0, 0);
            buffer.setCurrentStyles(StyleFlag.ITALIC);
            buffer.insertText("X");

            assertTrue(buffer.getScreenAttributesAt(0, 0).hasStyle(StyleFlag.ITALIC));
            assertFalse(buffer.getScreenAttributesAt(0, 0).hasStyle(StyleFlag.BOLD));
            assertTrue(buffer.getScreenAttributesAt(0, 1).hasStyle(StyleFlag.BOLD));   // displaced 'A'
            assertFalse(buffer.getScreenAttributesAt(0, 1).hasStyle(StyleFlag.ITALIC));
        }

        @Test
        void insertTextWrappedDisplacedCharsRetainAttributes() {
            buffer.setCurrentForeground(TerminalColor.RED);
            buffer.writeText("ABCDEFGHIJ");
            buffer.setCursorPosition(0, 0);
            buffer.setCurrentForeground(TerminalColor.GREEN);
            buffer.insertText("XY");

            // Row 0: X(GREEN) Y(GREEN) A(RED) B(RED) ... H(RED)
            // Row 1: I(RED) J(RED) ...
            assertEquals(TerminalColor.GREEN, buffer.getScreenAttributesAt(0, 0).foreground()); // 'X'
            assertEquals(TerminalColor.GREEN, buffer.getScreenAttributesAt(0, 1).foreground()); // 'Y'
            assertEquals(TerminalColor.RED, buffer.getScreenAttributesAt(0, 2).foreground());   // displaced 'A'
            assertEquals(TerminalColor.RED, buffer.getScreenAttributesAt(1, 0).foreground());   // displaced 'I' (wrapped)
            assertEquals(TerminalColor.RED, buffer.getScreenAttributesAt(1, 1).foreground());   // displaced 'J' (wrapped)
        }
    }

    // =====================================================================
    //  Fill Line
    // =====================================================================

    @Nested
    class FillLineOperations {

        @Test
        void fillLineWithCharacter() {
            buffer.fillLine('#');
            String prevLine = buffer.getScreenLineAsString(0);
            for (char c : prevLine.toCharArray()) {
                assertEquals('#', c);
            }
        }

        @Test
        void fillLineUsesCurrentAttributes() {
            buffer.setCurrentForeground(TerminalColor.RED);
            buffer.setCursorPosition(0, 0);
            buffer.fillLine('*');
            for (int c = 0; c < WIDTH; c++) {
                CellAttributes attrs = buffer.getScreenAttributesAt(0, c);
                assertEquals(TerminalColor.RED, attrs.foreground());
            }
        }

        @Test
        void fillLineMoveCursorToNextLine() {
            buffer.setCursorPosition(0, 3);
            buffer.fillLine('-');
            assertEquals(1, buffer.getCursorRow());
        }

        @Test
        void fillLineResetsCursorColumnToZero() {
            buffer.setCursorPosition(0, 5);
            buffer.fillLine('-');
            assertEquals(1, buffer.getCursorRow());
            assertEquals(0, buffer.getCursorCol());
        }

        @Test
        void fillLineAtLastLineScrolls() {
            buffer.setCursorPosition(HEIGHT - 1, 0);
            buffer.fillLine('Z');
            assertFalse(buffer.getScrollbackContent().isEmpty());
        }

        @Test
        void fillLineOverwritesExistingContent() {
            buffer.writeText("ABCDEFGHIJ");
            buffer.setCursorPosition(0, 5);
            buffer.fillLine('.');
            for (int c = 0; c < WIDTH; c++) {
                assertEquals('.', buffer.getScreenCharAt(0, c));
            }
        }

        @Test
        void fillMultipleLines() {
            buffer.setCursorPosition(0, 0);
            buffer.fillLine('A');
            buffer.fillLine('B');
            for (int c = 0; c < WIDTH; c++) {
                assertEquals('A', buffer.getScreenCharAt(0, c));
                assertEquals('B', buffer.getScreenCharAt(1, c));
            }
        }
    }

    // =====================================================================
    //  insertLineAtBottom
    // =====================================================================

    @Nested
    class InsertLineAtBottom {

        @Test
        void shiftsScreenUpByOne() {
            buffer.setCursorPosition(0, 0);
            buffer.writeText("ROW_0_____");
            buffer.setCursorPosition(1, 0);
            buffer.writeText("ROW_1_____");
            buffer.insertLineAtBottom();

            assertEquals("ROW_1_____", buffer.getScreenLineAsString(0));
        }

        @Test
        void lastLineBecomesEmpty() {
            buffer.setCursorPosition(HEIGHT - 1, 0);
            buffer.writeText("XXXXXXXXXX");
            buffer.insertLineAtBottom();
            assertEquals(" ".repeat(WIDTH), buffer.getScreenLineAsString(HEIGHT - 1));
        }

        @Test
        void topLinePushedToScrollback() {
            buffer.writeText("FIRST_LINE");
            buffer.insertLineAtBottom();
            assertEquals("FIRST_LINE", buffer.getScrollbackLineAsString(0));
        }

        @Test
        void cursorMovesUpAfterInsert() {
            buffer.setCursorPosition(3, 5);
            buffer.insertLineAtBottom();
            assertEquals(2, buffer.getCursorRow());
            assertEquals(5, buffer.getCursorCol());
        }

        @Test
        void cursorAtTopRowClampsToZero() {
            buffer.setCursorPosition(0, 0);
            buffer.insertLineAtBottom();
            assertEquals(0, buffer.getCursorRow());
        }

        @Test
        void multipleInsertsScrollMultipleLines() {
            for (int i = 0; i < HEIGHT; i++) {
                buffer.setCursorPosition(i, 0);
                buffer.writeText(String.format("ROW_%d_____", i).substring(0, WIDTH));
            }

            buffer.insertLineAtBottom();
            buffer.insertLineAtBottom();

            assertEquals("ROW_0_____", buffer.getScrollbackLineAsString(0));
            assertEquals("ROW_1_____", buffer.getScrollbackLineAsString(1));
        }
    }

    // =====================================================================
    //  Clear Operations
    // =====================================================================

    @Nested
    class ClearOperations {

        @Test
        void clearScreenMakesAllCellsEmpty() {
            buffer.writeText("Some content here!");
            buffer.clearScreen();
            for (int r = 0; r < HEIGHT; r++) {
                for (int c = 0; c < WIDTH; c++) {
                    assertTrue(buffer.getCellAt(r, c).isEmpty());
                }
            }
        }

        @Test
        void clearScreenDoesNotAffectScrollback() {
            buffer.writeText("TopLine___");
            buffer.insertLineAtBottom();
            buffer.clearScreen();
            assertFalse(buffer.getScrollbackContent().isEmpty());
            assertEquals("TopLine___", buffer.getScrollbackLineAsString(0));
        }

        @Test
        void clearAllClearsScreenAndScrollback() {
            buffer.writeText("SomeText__");
            buffer.insertLineAtBottom();
            buffer.clearAll();
            for (int r = 0; r < HEIGHT; r++) {
                for (int c = 0; c < WIDTH; c++) {
                    assertTrue(buffer.getCellAt(r, c).isEmpty());
                }
            }
            assertEquals("", buffer.getScrollbackContent());
        }

        @Test
        void clearScreenPreservesCursorPosition() {
            buffer.setCursorPosition(2, 5);
            buffer.clearScreen();
            assertEquals(2, buffer.getCursorRow());
            assertEquals(5, buffer.getCursorCol());
        }

        @Test
        void clearOnAlreadyEmptyBuffer() {
            buffer.clearScreen();
            String emptyLine = " ".repeat(WIDTH) + "\n";
            assertEquals(emptyLine.repeat(HEIGHT), buffer.getScreenContent());
        }

        @Test
        void clearAllOnAlreadyEmptyBuffer() {
            buffer.clearAll();
            String emptyLine = " ".repeat(WIDTH) + "\n";
            assertEquals(emptyLine.repeat(HEIGHT), buffer.getScreenContent());
            assertEquals("", buffer.getScrollbackContent());
        }
    }

    // =====================================================================
    //  Scrollback Ring Buffer Behavior
    // =====================================================================

    @Nested
    class ScrollbackBehavior {

        @Test
        void scrollbackRespectsMaxSize() {
            int maxScrollback = 3;
            TerminalBuffer smallBuffer = new TerminalBuffer(WIDTH, HEIGHT, maxScrollback);

            for (int i = 0; i < 10; i++) {
                smallBuffer.setCursorPosition(0, 0);
                smallBuffer.writeText(String.format("LINE_%d____", i).substring(0, WIDTH));
                smallBuffer.insertLineAtBottom();
            }

            String scrollback = smallBuffer.getScrollbackContent();
            assertFalse(scrollback.contains("LINE_0"));
            assertFalse(scrollback.contains("LINE_6"));
            assertTrue(scrollback.contains("LINE_7"));
            assertTrue(scrollback.contains("LINE_8"));
            assertTrue(scrollback.contains("LINE_9"));
        }

        @Test
        void scrollbackOrderPreserved() {
            buffer.setCursorPosition(0, 0);
            buffer.writeText("FIRST_LINE");
            buffer.setCursorPosition(1, 0);
            buffer.writeText("SECONDLINE");
            buffer.insertLineAtBottom();
            buffer.insertLineAtBottom();

            // getScrollbackContent iterates from oldest (0) to newest (size-1),
            // so the oldest line appears first in the string
            String content = buffer.getScrollbackContent();
            int firstPos = content.indexOf("FIRST_LINE");
            int secondPos = content.indexOf("SECONDLINE");
            assertTrue(secondPos > firstPos, "Newest line should appear after oldest in scrollback content");
        }

        @Test
        void scrollbackEmptyAfterClearAll() {
            buffer.writeText("Something!");
            buffer.insertLineAtBottom();
            buffer.clearAll();
            assertEquals("", buffer.getScrollbackContent());
        }

        @Test
        void scrollbackSurvivesClearScreen() {
            buffer.writeText("Preserved!");
            buffer.insertLineAtBottom();
            buffer.clearScreen();
            assertTrue(buffer.getScrollbackContent().contains("Preserved!"));
        }

        @Test
        void fullContentOrderIsScrollbackThenScreen() {
            buffer.writeText("ScrollLine");
            buffer.insertLineAtBottom();
            buffer.setCursorPosition(0, 0);
            buffer.writeText("ScreenLine");

            String full = buffer.getFullContent();
            int scrollPos = full.indexOf("ScrollLine");
            int screenPos = full.indexOf("ScreenLine");
            assertTrue(scrollPos < screenPos, "Scrollback should appear before screen in full content");
        }

        @Test
        void scrollbackCharAtWithValidAndInvalidCol() {
            buffer.writeText("ABCDEFGHIJ");
            buffer.insertLineAtBottom();
            assertEquals('A', buffer.getScrollbackCharAt(0, 0));
            assertEquals('J', buffer.getScrollbackCharAt(0, WIDTH - 1));
            assertEquals(Character.MIN_VALUE, buffer.getScrollbackCharAt(0, WIDTH));
        }
    }

    // =====================================================================
    //  Edge Cases & Boundary Conditions
    // =====================================================================

    @Nested
    class EdgeCases {

        @Test
        void singleCellBuffer() {
            TerminalBuffer tiny = new TerminalBuffer(1, 1, 5);
            tiny.writeCharacter('A'); // A is in scrollback
            assertEquals(' ', tiny.getScreenCharAt(0, 0));
        }

        @Test
        void singleCellBufferScrollsOnWrite() {
            TerminalBuffer tiny = new TerminalBuffer(1, 1, 5);
            tiny.writeCharacter('A');
            tiny.writeCharacter('B');
            // After second write, 'A' scrolled off screen
            assertEquals('A', tiny.getScrollbackCharAt(0, 0));
            assertEquals('B', tiny.getScrollbackCharAt(1, 0));
        }

        @Test
        void writeExactlyOneFullLine() {
            buffer.writeText("ABCDEFGHIJ");
            assertEquals("ABCDEFGHIJ", buffer.getScreenLineAsString(0));
            assertEquals(1, buffer.getCursorRow());
            assertEquals(0, buffer.getCursorCol());
        }

        @Test
        void cursorDoesNotMoveOnEmptyWrite() {
            buffer.setCursorPosition(2, 3);
            buffer.writeText("");
            assertEquals(2, buffer.getCursorRow());
            assertEquals(3, buffer.getCursorCol());
        }

        @Test
        void getCellAtWithCursorFromDifferentBuffer() {
            TerminalBuffer other = new TerminalBuffer(WIDTH, HEIGHT, MAX_SCROLLBACK);
            Cursor foreignCursor = new Cursor(other, 0, 0);
            assertNull(buffer.getCellAt(foreignCursor));
        }

        @Test
        void writeCharacterUsesCurrentAttributesNotDefault() {
            CellAttributes custom = new CellAttributes(
                    TerminalColor.BRIGHT_WHITE, TerminalColor.BRIGHT_BLACK,
                    Set.of(StyleFlag.BOLD, StyleFlag.ITALIC, StyleFlag.UNDERLINE));
            buffer.setCurrentAttributes(custom);
            buffer.writeCharacter('Z');
            Cell cell = buffer.getCellAt(0, 0);
            assertEquals(custom, cell.attributes());
        }

        @Test
        void fillLineWithNullCharIsAllowed() {
            buffer.fillLine('\0');
            for (int c = 0; c < WIDTH; c++) {
                assertEquals('\0', buffer.getCellAt(0, c).character());
            }
        }

        @Test
        void writeAfterClearScreen() {
            buffer.writeText("OldContent");
            buffer.clearScreen();
            buffer.setCursorPosition(0, 0);
            buffer.writeText("NewContent");
            assertEquals('N', buffer.getScreenCharAt(0, 0));
            assertEquals('e', buffer.getScreenCharAt(0, 1));
        }

        @Test
        void insertLineAtBottomOnEmptyScreen() {
            buffer.insertLineAtBottom();
            for (int r = 0; r < HEIGHT; r++) {
                for (int c = 0; c < WIDTH; c++) {
                    assertTrue(buffer.getCellAt(r, c).isEmpty());
                }
            }
        }

        @Test
        void multipleWritesAndScrollsProduceCorrectContent() {
            // Prepare lines to insert, each line uniquely labeled
            String[] lines = new String[HEIGHT + 3];
            for (int i = 0; i < HEIGHT + 3; i++) {
                String base = String.format("LINE_%d", i);
                if (base.length() < WIDTH) {
                    base = base + "_".repeat(WIDTH - base.length());
                }
                lines[i] = base.substring(0, WIDTH);
            }

            // Write each line at the bottom and scroll after each write
            for (String line : lines) {
                buffer.setCursorPosition(HEIGHT - 1, 0);
                buffer.writeText(line);
            }

            // Now check:
            // 1. The scrollback lines contain the oldest pushed-away lines in order.
            // 2. The screen contains the last HEIGHT-1 lines written (the last one is empty).
            int expectedScrollbackLines = lines.length - HEIGHT + 1;
            for (int sb = 0; sb < expectedScrollbackLines; sb++) {
                assertEquals(lines[sb], buffer.getScrollbackLineAsString(buffer.getScrollbackSize() - expectedScrollbackLines + sb), "Scrollback line mismatch at " + sb);
            }
            for (int screenRow = 0; screenRow < HEIGHT - 1; screenRow++) {
                int srcIndex = lines.length - HEIGHT + 1 + screenRow;
                assertEquals(lines[srcIndex], buffer.getScreenLineAsString(screenRow), "Screen line mismatch at " + screenRow);
            }
        }

        @Test
        void zeroScrollbackSize() {
            TerminalBuffer noScroll = new TerminalBuffer(WIDTH, HEIGHT, 1);
            noScroll.writeText("ABCDEFGHIJ");
            noScroll.insertLineAtBottom();
            noScroll.setCursorPosition(0, 0);
            noScroll.writeText("KLMNOPQRST");
            noScroll.insertLineAtBottom();
            assertEquals("KLMNOPQRST", noScroll.getScrollbackLineAsString(0));
        }

        @Test
        void writeTextExactlyFillsScreenMinusOne() {
            String text = "X".repeat(WIDTH * HEIGHT - 1);
            buffer.writeText(text);
            // All cells except the very last one should be 'X'
            for (int r = 0; r < HEIGHT; r++) {
                for (int c = 0; c < WIDTH; c++) {
                    if (r == HEIGHT - 1 && c == WIDTH - 1) {
                        assertEquals(' ', buffer.getScreenCharAt(r, c));
                    } else {
                        assertEquals('X', buffer.getScreenCharAt(r, c));
                    }
                }
            }
        }

        @Test
        void insertTextAtEndOfLine() {
            buffer.writeText("ABCDEFGHIJ");
            buffer.setCursorPosition(0, WIDTH - 1);
            buffer.insertCharacter('Z');
            assertEquals('Z', buffer.getScreenCharAt(0, WIDTH - 1));
            assertEquals('J', buffer.getScreenCharAt(1, 0));
        }

        @Test
        void zeroScrollbackCapacityDoesNotCrash() {
            TerminalBuffer noScroll = new TerminalBuffer(WIDTH, HEIGHT, 0);
            assertDoesNotThrow(() -> {
                noScroll.writeText("ABCDEFGHIJ");
                noScroll.insertLineAtBottom();
                noScroll.writeText("KLMNOPQRST");
                noScroll.insertLineAtBottom();
            });
        }

        @Test
        void zeroScrollbackCapacityKeepsOnlyMostRecentLine() {
            // capacity=0 is silently clamped to 1, so only the last pushed line is retained
            TerminalBuffer noScroll = new TerminalBuffer(WIDTH, HEIGHT, 0);
            noScroll.setCursorPosition(0, 0);
            noScroll.writeText("FIRSTLINE_");
            noScroll.insertLineAtBottom();
            noScroll.setCursorPosition(0, 0);
            noScroll.writeText("SECONDLINE");
            noScroll.insertLineAtBottom();

            // Only SECONDLINE survives (capacity is 1)
            assertFalse(noScroll.getScrollbackContent().contains("FIRSTLINE_"));
            assertTrue(noScroll.getScrollbackContent().contains("SECONDLINE"));
        }
    }

    // =====================================================================
    //  Combined / Integration Scenarios
    // =====================================================================

    @Nested
    class IntegrationScenarios {

        @Test
        void typicalTerminalSession() {
            buffer.writeText("$ ls");
            buffer.setCursorPosition(1, 0);
            buffer.writeText("file1.txt");
            buffer.setCursorPosition(2, 0);
            buffer.writeText("file2.txt");
            buffer.setCursorPosition(3, 0);
            buffer.writeText("$ ");

            assertEquals('$', buffer.getScreenCharAt(0, 0));
            assertEquals('f', buffer.getScreenCharAt(1, 0));
            assertEquals('$', buffer.getScreenCharAt(3, 0));
        }

        @Test
        void attributesSurviveScrolling() {
            buffer.setCurrentForeground(TerminalColor.RED);
            buffer.writeText("REDLINE___");
            buffer.insertLineAtBottom();

            CellAttributes scrollbackAttrs = buffer.getScrollbackAttributesAt(0, 0);
            assertNotNull(scrollbackAttrs);
            assertEquals(TerminalColor.RED, scrollbackAttrs.foreground());
        }

        @Test
        void clearAndRewritePreservesIntegrity() {
            buffer.writeText("Original");
            buffer.clearScreen();
            buffer.setCursorPosition(0, 0);
            buffer.setCurrentForeground(TerminalColor.BLUE);
            buffer.writeText("Updated");

            assertEquals('U', buffer.getScreenCharAt(0, 0));
            assertEquals(TerminalColor.BLUE, buffer.getScreenAttributesAt(0, 0).foreground());
            assertTrue(buffer.getCellAt(0, 7).isEmpty());
        }

        @Test
        void fillThenWriteOverPartOfLine() {
            buffer.fillLine('=');
            buffer.setCursorPosition(0, 3);
            buffer.writeText("Hi");
            assertEquals('=', buffer.getScreenCharAt(0, 0));
            assertEquals('H', buffer.getScreenCharAt(0, 3));
            assertEquals('i', buffer.getScreenCharAt(0, 4));
            assertEquals('=', buffer.getScreenCharAt(0, 5));
        }

        @Test
        void scrollbackAndScreenContentCombined() {
            for (int i = 0; i < HEIGHT; i++) {
                buffer.setCursorPosition(i, 0);
                String text = String.valueOf((char) ('A' + i)).repeat(WIDTH);
                buffer.writeText(text);
            }
            for (int i = 0; i < 3; i++) {
                buffer.insertLineAtBottom();
            }

            String full = buffer.getFullContent();
            assertTrue(full.contains("A".repeat(WIDTH)));
            assertTrue(full.contains("B".repeat(WIDTH)));
            assertTrue(full.contains("C".repeat(WIDTH)));
        }

        @Test
        void writeWithDifferentAttributesPerChar() {
            TerminalColor[] colors = {TerminalColor.RED, TerminalColor.GREEN, TerminalColor.BLUE};
            for (int i = 0; i < 3; i++) {
                buffer.setCurrentForeground(colors[i]);
                buffer.writeCharacter((char) ('A' + i));
            }
            assertEquals(TerminalColor.RED, buffer.getScreenAttributesAt(0, 0).foreground());
            assertEquals(TerminalColor.GREEN, buffer.getScreenAttributesAt(0, 1).foreground());
            assertEquals(TerminalColor.BLUE, buffer.getScreenAttributesAt(0, 2).foreground());
        }

        @Test
        void insertTextPreservesAttributesOfDisplacedCells() {
            buffer.setCurrentForeground(TerminalColor.RED);
            buffer.writeText("AB");
            buffer.setCursorPosition(0, 0);
            buffer.setCurrentForeground(TerminalColor.GREEN);
            buffer.insertText("X");

            assertEquals(TerminalColor.GREEN, buffer.getScreenAttributesAt(0, 0).foreground()); // inserted 'X'
            assertEquals(TerminalColor.RED, buffer.getScreenAttributesAt(0, 1).foreground());   // displaced 'A' must keep RED
            assertEquals(TerminalColor.RED, buffer.getScreenAttributesAt(0, 2).foreground());   // displaced 'B' must keep RED
        }

        @Test
        void complexCursorMovementAndWrite() {
            buffer.setCursorPosition(0, 0);
            buffer.writeText("Hello");    // cursor at (0,5)
            buffer.moveCursorDown(1);     // cursor at (1,5)
            buffer.setCursorCol(0);       // cursor at (1,0)
            buffer.writeText("World");    // cursor at (1,5)
            buffer.moveCursorUp(1);       // cursor at (0,5)
            buffer.moveCursorRight(4);    // cursor at (0,9)
            buffer.writeText("!");        // writes at (0,9)

            assertEquals('H', buffer.getScreenCharAt(0, 0));
            assertEquals('!', buffer.getScreenCharAt(0, 9));
            assertEquals('W', buffer.getScreenCharAt(1, 0));
        }
    }

    // =====================================================================
    //  Cursor.offsetTo
    // =====================================================================

    @Nested
    class CursorOffsetTo {

        @Test
        void offsetToAheadCursorIsPositive() {
            Cursor c1 = new Cursor(buffer, 0, 0);
            Cursor c2 = new Cursor(buffer, 0, 3);
            assertEquals(3, c1.offsetTo(c2));
        }

        @Test
        void offsetToSamePositionIsZero() {
            Cursor c = new Cursor(buffer, 2, 5);
            assertEquals(0, c.offsetTo(c));
        }

        @Test
        void offsetToBehindCursorIsNegative() {
            Cursor c1 = new Cursor(buffer, 0, 5);
            Cursor c2 = new Cursor(buffer, 0, 2);
            assertEquals(-3, c1.offsetTo(c2));
        }

        @Test
        void offsetToNextLineAccountsForWidth() {
            Cursor c1 = new Cursor(buffer, 0, 5);
            Cursor c2 = new Cursor(buffer, 1, 3);
            // (1-0)*WIDTH - 5 + 3 = 10 - 5 + 3 = 8
            assertEquals(8, c1.offsetTo(c2));
        }

        @Test
        void offsetToPreviousLineIsNegative() {
            Cursor c1 = new Cursor(buffer, 1, 3);
            Cursor c2 = new Cursor(buffer, 0, 5);
            // (0-1)*WIDTH - 3 + 5 = -10 - 3 + 5 = -8
            assertEquals(-8, c1.offsetTo(c2));
        }

        @Test
        void offsetToNullReturnsMaxInt() {
            Cursor c = new Cursor(buffer, 0, 0);
            assertEquals(Integer.MAX_VALUE, c.offsetTo(null));
        }

        @Test
        void offsetToForeignBufferCursorReturnsMaxInt() {
            TerminalBuffer other = new TerminalBuffer(WIDTH, HEIGHT, MAX_SCROLLBACK);
            Cursor c1 = new Cursor(buffer, 0, 0);
            Cursor c2 = new Cursor(other, 0, 5);
            assertEquals(Integer.MAX_VALUE, c1.offsetTo(c2));
        }
    }

    // =====================================================================
    //  Custom Empty/Undefined Char Overloads
    // =====================================================================

    @Nested
    class CustomCharOverloads {

        @Test
        void getScreenCharAtWithCustomChars() {
            buffer.writeCharacter('A');
            assertEquals('A', buffer.getScreenCharAt(0, 0, '.', '?'));
            assertEquals('.', buffer.getScreenCharAt(0, 1, '.', '?'));
            assertEquals('?', buffer.getScreenCharAt(-1, 0, '.', '?'));
            assertEquals('?', buffer.getScreenCharAt(0, WIDTH, '.', '?'));
        }

        @Test
        void getScrollbackCharAtWithCustomChars() {
            buffer.writeText("ABCDEFGHIJ");
            buffer.insertLineAtBottom();
            assertEquals('A', buffer.getScrollbackCharAt(0, 0, '.', '?'));
            assertEquals('?', buffer.getScrollbackCharAt(5, 0, '.', '?'));
            assertEquals('?', buffer.getScrollbackCharAt(0, WIDTH, '.', '?'));
        }

        @Test
        void getScreenLineAsStringWithCustomChars() {
            buffer.writeText("Hi");
            String line = buffer.getScreenLineAsString(0, '.', '?');
            assertEquals("Hi" + ".".repeat(WIDTH - 2), line);
        }

        @Test
        void getScrollbackLineAsStringWithCustomChars() {
            buffer.writeText("AB");
            buffer.insertLineAtBottom();
            String line = buffer.getScrollbackLineAsString(0, '.', '?');
            assertEquals("AB" + ".".repeat(WIDTH - 2), line);
        }

        @Test
        void getScreenContentWithCustomChars() {
            buffer.writeText("Hi");
            String content = buffer.getScreenContent('.', '?');
            String firstLine = "Hi" + ".".repeat(WIDTH - 2) + "\n";
            String emptyLine = ".".repeat(WIDTH) + "\n";
            assertEquals(firstLine + emptyLine.repeat(HEIGHT - 1), content);
        }

        @Test
        void getScrollbackContentWithCustomChars() {
            buffer.writeText("ABCDEFGHIJ");
            buffer.insertLineAtBottom();
            String content = buffer.getScrollbackContent('.', '?');
            assertEquals("ABCDEFGHIJ\n", content);
        }

        @Test
        void getFullContentWithCustomChars() {
            buffer.writeText("ScrollLine");
            buffer.insertLineAtBottom();
            buffer.setCursorPosition(0, 0);
            buffer.writeText("ScreenLine");
            String full = buffer.getFullContent('.', '?');
            assertTrue(full.contains("ScrollLine"));
            assertTrue(full.contains("ScreenLine"));
        }

        @Test
        void defaultOverloadsMatchExplicitDefaults() {
            buffer.writeText("Hello");
            assertEquals(
                    buffer.getScreenContent(),
                    buffer.getScreenContent(' ', Character.MIN_VALUE));
            assertEquals(
                    buffer.getScreenLineAsString(0),
                    buffer.getScreenLineAsString(0, ' ', Character.MIN_VALUE));
            assertEquals(
                    buffer.getScreenCharAt(0, 0),
                    buffer.getScreenCharAt(0, 0, ' ', Character.MIN_VALUE));
        }
    }

    // =====================================================================
    //  Resize — Dimensions
    // =====================================================================

    @Nested
    class ResizeDimensions {

        @Test
        void resizeUpdatesWidthAndHeight() {
            buffer.resize(20, 10);
            assertEquals(20, buffer.getWidth());
            assertEquals(10, buffer.getHeight());
        }

        @Test
        void resizeUpdatesScrollbackLimit() {
            buffer.resize(20, 10, 500);
            assertEquals(20, buffer.getWidth());
            assertEquals(10, buffer.getHeight());
            assertEquals(500, buffer.getMaxScrollbackSize());
        }

        @Test
        void resizeWithoutScrollbackKeepsOldLimit() {
            buffer.resize(20, 10);
            assertEquals(MAX_SCROLLBACK, buffer.getMaxScrollbackSize());
        }

        @Test
        void resizeToSameDimensionsPreservesContent() {
            buffer.writeText("Hello");
            buffer.resize(WIDTH, HEIGHT);
            assertEquals(WIDTH, buffer.getWidth());
            assertEquals(HEIGHT, buffer.getHeight());
            assertTrue(buffer.getFullContent().contains("Hello"));
        }

        @Test
        void resizeScreenGridHasCorrectSize() {
            buffer.resize(15, 8);
            for (int r = 0; r < 8; r++) {
                for (int c = 0; c < 15; c++) {
                    assertNotNull(buffer.getCellAt(r, c));
                }
            }
            assertNull(buffer.getCellAt(8, 0));
            assertNull(buffer.getCellAt(0, 15));
        }
    }

    // =====================================================================
    //  Resize — Content Preservation
    // =====================================================================

    @Nested
    class ResizeContentPreservation {

        @Test
        void resizeWiderPreservesContent() {
            buffer.writeText("Hello");
            buffer.resize(20, HEIGHT);
            String full = buffer.getFullContent();
            assertTrue(full.contains("Hello"));
        }

        @Test
        void resizeTallerPreservesContent() {
            buffer.writeText("Hello");
            buffer.resize(WIDTH, 10);
            String full = buffer.getFullContent();
            assertTrue(full.contains("Hello"));
        }

        @Test
        void resizeNarrowerReflowsLongLine() {
            // WIDTH=10, write a full line "ABCDEFGHIJ"
            buffer.writeText("ABCDEFGHIJ");
            // Resize to width 5 — the 10-char line should reflow into two 5-char rows
            buffer.resize(5, HEIGHT);
            String full = buffer.getFullContent();
            assertTrue(full.contains("ABCDE"));
            assertTrue(full.contains("FGHIJ"));
        }

        @Test
        void resizeNarrowerPreservesCharacterOrder() {
            buffer.writeText("ABCDEFGHIJ");
            buffer.resize(5, 10);
            String full = buffer.getFullContent();
            int posA = full.indexOf('A');
            int posF = full.indexOf('F');
            assertTrue(posA < posF);
        }

        @Test
        void resizePreservesScrollbackContent() {
            buffer.writeText("ScrollLine");
            buffer.insertLineAtBottom();
            buffer.setCursorPosition(0, 0);
            buffer.writeText("ScreenLine");
            buffer.resize(WIDTH, HEIGHT);
            String full = buffer.getFullContent();
            assertTrue(full.contains("ScrollLine"));
            assertTrue(full.contains("ScreenLine"));
        }

        @Test
        void resizePreservesContentOrderScrollbackThenScreen() {
            buffer.writeText("FIRST_LINE");
            buffer.insertLineAtBottom();
            buffer.setCursorPosition(0, 0);
            buffer.writeText("SECONDLINE");
            buffer.resize(WIDTH, HEIGHT);
            String full = buffer.getFullContent();
            int firstPos = full.indexOf("FIRST_LINE");
            int secondPos = full.indexOf("SECONDLINE");
            assertTrue(firstPos >= 0, "FIRST_LINE should be in full content");
            assertTrue(secondPos >= 0, "SECONDLINE should be in full content");
            assertTrue(firstPos < secondPos, "Scrollback content should appear before screen content");
        }

        @Test
        void resizeMultipleScreenLinesPreserved() {
            buffer.setCursorPosition(0, 0);
            buffer.writeText("Line_0____");
            buffer.setCursorPosition(1, 0);
            buffer.writeText("Line_1____");
            buffer.setCursorPosition(2, 0);
            buffer.writeText("Line_2____");
            buffer.resize(WIDTH, HEIGHT);
            String full = buffer.getFullContent();
            assertTrue(full.contains("Line_0____"));
            assertTrue(full.contains("Line_1____"));
            assertTrue(full.contains("Line_2____"));
        }

        @Test
        void resizeEmptyBufferProducesEmptyScreen() {
            buffer.resize(15, 8);
            for (int r = 0; r < 8; r++) {
                for (int c = 0; c < 15; c++) {
                    assertTrue(buffer.getCellAt(r, c).isEmpty());
                }
            }
        }

        @Test
        void resizePreservesAttributes() {
            buffer.setCurrentForeground(TerminalColor.RED);
            buffer.setCurrentStyles(StyleFlag.BOLD);
            buffer.writeText("Styled");
            buffer.resize(20, HEIGHT);
            String full = buffer.getFullContent();
            assertTrue(full.contains("Styled"));
            // Find "Styled" in the new grid and check attributes
            for (int r = 0; r < buffer.getHeight(); r++) {
                Cell cell = buffer.getCellAt(r, 0);
                if (cell != null && cell.character() == 'S') {
                    assertEquals(TerminalColor.RED, cell.attributes().foreground());
                    assertTrue(cell.attributes().hasStyle(StyleFlag.BOLD));
                    return;
                }
            }
            // Also check scrollback
            for (int r = 0; r < buffer.getScrollbackSize(); r++) {
                CellAttributes attrs = buffer.getScrollbackAttributesAt(r, 0);
                if (attrs != null && buffer.getScrollbackCharAt(r, 0) == 'S') {
                    assertEquals(TerminalColor.RED, attrs.foreground());
                    assertTrue(attrs.hasStyle(StyleFlag.BOLD));
                    return;
                }
            }
            fail("'S' with RED foreground and BOLD not found after resize");
        }
    }

    // =====================================================================
    //  Resize — Blank Lines
    // =====================================================================

    @Nested
    class ResizeBlankLines {

        @Test
        void resizePreservesBlankLineBetweenContent() {
            buffer.setCursorPosition(0, 0);
            buffer.writeText("Hello");
            // row 1 left empty
            buffer.setCursorPosition(2, 0);
            buffer.writeText("World");

            buffer.resize(WIDTH, HEIGHT);
            String full = buffer.getFullContent();
            int helloPos = full.indexOf("Hello");
            int worldPos = full.indexOf("World");
            assertTrue(helloPos >= 0);
            assertTrue(worldPos >= 0);
            assertTrue(worldPos > helloPos);
        }

        @Test
        void resizeTrailingEmptyLinesPreserved() {
            buffer.setCursorPosition(0, 0);
            buffer.writeText("Top");
            // rows 1-4 are empty
            buffer.resize(WIDTH, HEIGHT);
            assertEquals(HEIGHT, buffer.getHeight());
            assertTrue(buffer.getFullContent().contains("Top"));
        }
    }

    // =====================================================================
    //  Resize — Cursor Position
    // =====================================================================

    @Nested
    class ResizeCursorPosition {

        @Test
        void resizeCursorClampedToNewBounds() {
            buffer.setCursorPosition(4, 9); // last row, last col
            buffer.resize(5, 3);
            assertTrue(buffer.getCursorRow() < 3);
            assertTrue(buffer.getCursorCol() < 5);
        }

        @Test
        void resizeCursorPreservedWhenWithinNewBounds() {
            buffer.setCursorPosition(2, 5);
            buffer.resize(20, 10);
            assertEquals(2, buffer.getCursorRow());
            assertEquals(5, buffer.getCursorCol());
        }

        @Test
        void resizeCursorRowClampedWhenHeightShrinks() {
            buffer.setCursorPosition(4, 0); // row 4 (last row for HEIGHT=5)
            buffer.resize(WIDTH, 3);
            assertTrue(buffer.getCursorRow() <= 2);
        }

        @Test
        void resizeCursorColClampedWhenWidthShrinks() {
            buffer.setCursorPosition(0, 9); // col 9 (last col for WIDTH=10)
            buffer.resize(5, HEIGHT);
            assertTrue(buffer.getCursorCol() <= 4);
        }
    }

    // =====================================================================
    //  Resize — Scrollback Limit
    // =====================================================================

    @Nested
    class ResizeScrollbackLimit {

        @Test
        void resizeReducesScrollbackLimit() {
            // Push many lines into scrollback
            for (int i = 0; i < 10; i++) {
                buffer.setCursorPosition(0, 0);
                buffer.writeText(String.format("SB_LINE_%d_", i).substring(0, WIDTH));
                buffer.insertLineAtBottom();
            }
            assertTrue(buffer.getScrollbackSize() > 3);

            buffer.resize(WIDTH, HEIGHT, 3);
            assertEquals(3, buffer.getMaxScrollbackSize());
            // After replay into a buffer with maxScrollback=3, at most 3 lines kept
            assertTrue(buffer.getScrollbackSize() <= 3);
        }

        @Test
        void resizeIncreasesScrollbackLimit() {
            buffer.resize(WIDTH, HEIGHT, 5000);
            assertEquals(5000, buffer.getMaxScrollbackSize());
        }
    }

    // =====================================================================
    //  Resize — Edge Cases
    // =====================================================================

    @Nested
    class ResizeEdgeCases {

        @Test
        void resizeTo1x1() {
            buffer.writeText("ABCDEFGHIJ");
            assertDoesNotThrow(() -> buffer.resize(1, 1));
            assertEquals(1, buffer.getWidth());
            assertEquals(1, buffer.getHeight());
        }

        @Test
        void resizeFrom1x1() {
            TerminalBuffer tiny = new TerminalBuffer(1, 1, 100);
            tiny.writeCharacter('X');
            tiny.resize(10, 5);
            assertEquals(10, tiny.getWidth());
            assertEquals(5, tiny.getHeight());
            assertTrue(tiny.getFullContent().contains("X"));
        }

        @Test
        void resizeDoesNotCrashOnEmptyBuffer() {
            assertDoesNotThrow(() -> buffer.resize(20, 10));
        }

        @Test
        void resizeVeryWide() {
            buffer.writeText("Short");
            buffer.resize(200, HEIGHT);
            assertTrue(buffer.getFullContent().contains("Short"));
            assertEquals(200, buffer.getWidth());
        }

        @Test
        void resizeVeryTall() {
            buffer.writeText("Line");
            buffer.resize(WIDTH, 100);
            assertTrue(buffer.getFullContent().contains("Line"));
            assertEquals(100, buffer.getHeight());
        }

        @Test
        void resizeShorterThanContentPushesToScrollback() {
            for (int i = 0; i < HEIGHT; i++) {
                buffer.setCursorPosition(i, 0);
                buffer.writeText(String.format("ROW_%d_____", i).substring(0, WIDTH));
            }
            buffer.resize(WIDTH, 2);
            assertEquals(2, buffer.getHeight());
            assertFalse(buffer.getScrollbackContent().isEmpty());
            String full = buffer.getFullContent();
            assertTrue(full.contains("ROW_0"));
        }

        @Test
        void resizePreservesCurrentAttributes() {
            CellAttributes custom = new CellAttributes(
                    TerminalColor.CYAN, TerminalColor.MAGENTA, Set.of(StyleFlag.UNDERLINE));
            buffer.setCurrentAttributes(custom);
            buffer.resize(20, 10);
            assertEquals(custom, buffer.getCurrentAttributes());
        }

        @Test
        void resizePreservesDefaultChars() {
            TerminalBuffer b = new TerminalBuffer(WIDTH, HEIGHT, MAX_SCROLLBACK, 0, 0,
                    CellAttributes.DEFAULT, '.', '?');
            b.writeText("Hi");
            b.resize(20, 10);
            assertEquals('.', b.getDefaultEmptyChar());
            assertEquals('?', b.getDefaultUndefinedChar());
        }

        @Test
        void doubleResizePreservesContent() {
            buffer.writeText("Persist");
            buffer.resize(20, 10);
            buffer.resize(5, 3);
            assertTrue(buffer.getFullContent().contains("Persi"));
        }

        @Test
        void resizeNarrowerThenWiderPreservesReflowedContent() {
            buffer.writeText("ABCDEFGHIJ");
            buffer.resize(5, 10);
            // Line reflowed: "ABCDE" and "FGHIJ"
            buffer.resize(20, 10);
            // Each row remains separate after widening
            String full = buffer.getFullContent();
            assertTrue(full.contains("ABCDE"));
            assertTrue(full.contains("FGHIJ"));
        }

        @Test
        void resizeWithScrollbackAndScreen() {
            // Fill screen
            for (int i = 0; i < HEIGHT; i++) {
                buffer.setCursorPosition(i, 0);
                buffer.writeText(String.valueOf((char) ('A' + i)).repeat(WIDTH));
            }
            // Push 3 lines to scrollback
            for (int i = 0; i < 3; i++) {
                buffer.insertLineAtBottom();
            }

            buffer.resize(WIDTH + 5, HEIGHT + 5);
            String full = buffer.getFullContent();
            // All original lines (A-E rows) should be present
            for (int i = 0; i < HEIGHT; i++) {
                char ch = (char) ('A' + i);
                assertTrue(full.contains(String.valueOf(ch).repeat(WIDTH)),
                        "Missing row of '" + ch + "' after resize");
            }
        }
    }
}
