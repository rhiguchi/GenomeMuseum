package jp.scid.genomemuseum.view;

import static org.junit.Assert.*;

import java.awt.EventQueue;

import javax.swing.JFrame;

import org.junit.Test;

public class OverviewMotifViewTest {

    @Test
    public void test() {
        fail("Not yet implemented");
    }
    
    void showPanel() {
        JFrame frame = new JFrame(getClass().getSimpleName());
        OverviewMotifView view = new OverviewMotifView();
        
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
