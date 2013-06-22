package jp.scid.genomemuseum.view;

import java.awt.Color;
import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTable;

public class DownloadTaskTableCellTest {

    // view test
    public static void main(String[] args) {
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                new DownloadTaskTableCellTest().runSample();
            }
        });
    }
    
    private void runSample() {
        DownloadTaskTableCell cell = new DownloadTaskTableCell();
        
        JTable table = new JTable(10, 4);
        table.setOpaque(true);
        table.setBackground(Color.white);
        table.setDefaultRenderer(Object.class, cell);
        table.setDefaultEditor(Object.class, cell);
        
        JScrollPane scroll = new JScrollPane(table);
        
        JFrame frame = new JFrame("test");
        frame.setContentPane(scroll);
        frame.pack();
        frame.setVisible(true);
    }
}
