package jp.scid.genomemuseum.view;

import java.awt.EventQueue;

import javax.swing.JFrame;

public class OverviewMotifViewTest {
    void showPanel() {
        JFrame frame = new JFrame(getClass().getSimpleName());
        OverviewMotifListView view = new OverviewMotifListView();
        
        frame.setContentPane(view.getContentPane());
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }

    public static void main(String[] args) {
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                OverviewMotifViewTest test = new OverviewMotifViewTest();
                test.showPanel();
            }
        });
    }
}
