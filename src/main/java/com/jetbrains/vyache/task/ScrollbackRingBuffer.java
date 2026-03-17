package com.jetbrains.vyache.task;

/**
 * Fixed-capacity circular (ring) buffer that stores scrollback lines for a
 * {@link TerminalBuffer}.
 *
 * <h3>Why a ring buffer?</h3>
 * <p>Terminal scrollback is a classic FIFO with a hard size limit: new lines are
 * appended at the tail, and once the maximum capacity is reached the oldest line
 * at the head is silently discarded. A ring buffer provides O(1) add and O(1)
 * indexed access with zero copying or shifting — ideal for high-throughput
 * terminal output where thousands of lines may scroll by quickly.
 *
 * <h3>Implementation details</h3>
 * <ul>
 *   <li>{@code head} points to the oldest stored element.</li>
 *   <li>{@code tail} points to the next write slot.</li>
 *   <li>When the buffer is full, adding a new element overwrites the oldest and
 *       advances {@code head}.</li>
 *   <li>The minimum effective capacity is 1 (a requested 0 is clamped to 1).
 *       This avoids divide-by-zero in the modulo arithmetic while still allowing
 *       a buffer that only remembers the most recently scrolled-off line.</li>
 *   <li>Each element is a {@code Cell[]} — a complete row of the screen at the
 *       time it was pushed off.  The array reference is stored directly (no
 *       defensive copy), because once a row enters scrollback it is never mutated.</li>
 * </ul>
 *
 * <p>This class is package-private; only {@link TerminalBuffer} interacts with it.
 */
class ScrollbackRingBuffer {
    private final Cell[][] buffer;
    private int head = 0;
    private int tail = 0;
    private int size = 0;
    private final int capacity;

    /**
     * Creates a ring buffer with the given capacity (minimum 1).
     *
     * @param capacity maximum number of scrollback lines to retain
     */
    ScrollbackRingBuffer(int capacity) {
        this.capacity = Math.max(1, capacity);
        this.buffer = new Cell[this.capacity][];
    }

    /**
     * Removes all elements and resets the buffer to its initial empty state.
     */
    void clear() {
        for(int i = 0; i < capacity; i++) {
            buffer[i] = null;
        }
        head = 0;
        tail = 0;
        size = 0;
    }

    /**
     * Appends a line to the buffer. If the buffer is full, the oldest line is
     * silently overwritten.
     *
     * @param element the row of cells to store
     */
    void add(Cell[] element) {
        buffer[tail] = element;
        tail = (tail + 1) % capacity;
        
        if (size < capacity) {
            size++;
        } else {
            head = (head + 1) % capacity;
        }
    }

    /**
     * Returns the element at the given logical index (0 = oldest line).
     *
     * @param index logical index into the scrollback history
     * @return the stored {@code Cell[]} row, or {@code null} if out of bounds
     */
    Cell[] getElement(int index) {
        if (index < 0 || index >= size) return null;
        return buffer[(head + index) % capacity];
    }

    /**
     * @return the number of lines currently stored (always &le; capacity)
     */
    int size() {
        return size;
    }
}