package com.jetbrains.vyache.task;

public class ScrollbackRingBuffer {
    private final Cell[][] buffer;
    private int head = 0;
    private int tail = 0;
    private int size = 0;
    private final int capacity;

    ScrollbackRingBuffer(int capacity) {
        this.capacity = Math.max(1, capacity);
        this.buffer = new Cell[this.capacity][];
    }

    void clear() {
        for(int i = 0; i < capacity; i++) {
            buffer[i] = null;
        }
        head = 0;
        tail = 0;
        size = 0;
    }

    void add(Cell[] element) {
        buffer[tail] = element;
        tail = (tail + 1) % capacity;
        
        if (size < capacity) {
            size++;
        } else {
            head = (head + 1) % capacity;
        }
    }

    Cell[] getElement(int index) {
        if (index < 0 || index >= size) return null;
        return buffer[(head + index) % capacity];
    }

    int size() {
        return size;
    }
}