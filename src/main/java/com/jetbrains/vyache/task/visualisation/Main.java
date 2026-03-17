package com.jetbrains.vyache.task.visualisation;

import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            TerminalBufferTestUI ui = new TerminalBufferTestUI();
            ui.setVisible(true);
        });
    }
}