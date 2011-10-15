package jp.scid.genomemuseum.view;

import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.JPanel;
import javax.swing.JTextArea;

public class FileContentView implements GenomeMuseumView {

    public final JTextArea textArea = new JTextArea(); {
        textArea.setEditable(false);
    }
    
    public final JPanel contentPane = new JPanel(new BorderLayout()); {
        contentPane.add(textArea, "Center");
        
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
