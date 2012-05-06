package jp.scid.genomemuseum.view;

import javax.swing.JFrame;

public class ExhibitListViewTest {
    ExhibitListView craeteView() {
        return new ExhibitListView();
    }
    
    public static void main(String[] args) {
        final ExhibitListViewTest test = new ExhibitListViewTest();
        
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                JFrame frame = new JFrame(test.getClass().getSimpleName());
                frame.setContentPane(test.craeteView().getContainer());
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame.pack();
                frame.setLocationRelativeTo(null);
                
                frame.setVisible(true);
            }
        });
    }
}
