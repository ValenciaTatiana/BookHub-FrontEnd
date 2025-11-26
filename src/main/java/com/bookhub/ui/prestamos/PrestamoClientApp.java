package com.bookhub.cliente.prestamos;

import javax.swing.*;

public class PrestamoClientApp {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {

            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {}

            PrestamoFrame frame = new PrestamoFrame();
            frame.setVisible(true);
        });
    }
}