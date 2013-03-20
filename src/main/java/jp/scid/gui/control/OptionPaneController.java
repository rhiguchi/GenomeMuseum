package jp.scid.gui.control;

import static java.lang.String.*;

import java.awt.Component;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import javax.swing.JDialog;
import javax.swing.JOptionPane;

public class OptionPaneController {
    private static final String MESSAGE_FORMAT_RESOURCE = "message_format.html";
    private final String formatString;
    
    private Component parentComponent = null;
    
    private final JOptionPane optionPane;
    
    public OptionPaneController(JOptionPane optionPane) {
        super();
        this.optionPane = optionPane;
        
        optionPane.setMessageType(JOptionPane.INFORMATION_MESSAGE);
        
        try {
            formatString = getFormatString();
        }
        catch (IOException e) {
            throw new IllegalStateException(e);
        }
   }

    static String getFormatString() throws IOException {
        StringBuilder sb = new StringBuilder();
        InputStream formatResource = DialogManager.class.getResourceAsStream(MESSAGE_FORMAT_RESOURCE);
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(formatResource));
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }
        finally {
            formatResource.close();
        }
        return sb.toString();
    }
    
    public OptionPaneController() {
        this(new JOptionPane(""));
    }
    
    public void setParentComponent(Component parentComponent) {
        this.parentComponent = parentComponent;
    }
    
    public void showMessage(String message, String information) {
        String text = format(formatString, message, information);
        optionPane.setMessage(text);
        
        JDialog dialog = optionPane.createDialog(parentComponent, "");
        
        dialog.setVisible(true);
        dialog.dispose();
    }
    
    public void showMessage(String message) {
        showMessage(message, null);
    }
}
