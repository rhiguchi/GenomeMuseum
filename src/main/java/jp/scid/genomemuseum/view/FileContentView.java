package jp.scid.genomemuseum.view;

import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

public class FileContentView implements GenomeMuseumView {

    public final JTextArea textArea = new JTextArea(); {
        textArea.setEditable(false);
    }
    public final JScrollPane textAreaScroll = new JScrollPane(textArea,
            JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
            JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    
    public final JPanel contentPane = new JPanel(new BorderLayout()); {
        contentPane.add(textAreaScroll, "Center");
        
        contentPane.setPreferredSize(new Dimension(300, 300));
    }
    
    public JPanel getContentPane() {
        return contentPane;
    }
    
    // view test
    public static void main(String[] args) {
        GUICheckApp.launch(args, FileContentView.class);
    }
}
