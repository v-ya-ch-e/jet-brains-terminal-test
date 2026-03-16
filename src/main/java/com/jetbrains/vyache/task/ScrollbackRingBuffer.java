package com.jetbrains.vyache.task;

public class ScrollbackRingBuffer {
    private final Cell[][] buffer;
    private int head = 0;
    private int tail = 0;
    private int size = 0;
    private final int capacity;

    public ScrollbackRingBuffer(int capacity) {
        this.capacity = capacity;
        this.buffer = new Cell[capacity][];
    }

    public void add(Cell[] element) {
        buffer[tail] = element;
        tail = (tail + 1) % capacity;
        
        if (size < capacity) {
            size++;
        } else {
            head = (head + 1) % capacity;
        }
    }

    public Cell[] getElement(int index) {
        if (index < 0 || index >= size) return null;
        return buffer[(head + index) % capacity];
    }

    public Cell[] pollElement() {
        if (size == 0) return null;
        Cell[] result = buffer[head];
        head = (head + 1) % capacity;
        size--;
        return result;
    }

    public int size() {
        return size;
    }
}