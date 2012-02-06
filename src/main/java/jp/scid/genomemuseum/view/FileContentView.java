package jp.scid.genomemuseum.view;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

public class FileContentView implements GenomeMuseumView {

    public final JTextArea textArea = new JTextArea(); {
        textArea.setEditable(false);
        textArea.setFont(Font.decode("monospaced-14"));
        textArea.setText("Test Content");
    }
    
    public final JScrollPane textAreaScroll = new JScrollPane(textArea,
            JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
            JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    
    public final JPanel contentPane = new JPanel(new BorderLayout()); {
        contentPane.add(textAreaScroll, "Center");
        
        contentPane.setPreferredSize(new Dimension(300, 300));
        contentPane.setMinimumSize(new Dimension(200, 0));
    }
    
    public JComponent getContentPane() {
        return contentPane;
    }
    
    // view test
    public static void main(String[] args) {
        GUICheckApp.launch(args, FileContentView.class);
    }
}
