package ec.edu.uce.ApiWebConsumer;

/**
 * @autor John Andino
 */

import ec.edu.uce.ApiWebConsumer.view.JFrame;

import javax.swing.*;

public class ApiWebConsumerApplication {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame app = new JFrame();
            app.createAndShowGUI();
        });
    }
}
