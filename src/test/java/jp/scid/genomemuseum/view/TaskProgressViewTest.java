package jp.scid.genomemuseum.view;

import java.awt.EventQueue;

import javax.swing.JFrame;

public class TaskProgressViewTest {

    // view test
    public static void main(String[] args) {
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                new TaskProgressViewTest().runSample();
            }
        });
    }
    
    private void runSample() {
        TaskProgressView view = new TaskProgressView();
        
        JFrame frame = new JFrame("test");
        frame.setContentPane(view.getComponent());
        frame.setVisible(true);
    }
}
