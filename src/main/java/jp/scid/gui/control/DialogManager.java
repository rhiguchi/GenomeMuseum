package jp.scid.gui.control;

import java.awt.Component;
import java.awt.FileDialog;
import java.awt.Window;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.MessageFormat;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

public class DialogManager {
    // file dialog
    private final FileDialog fileDialog;
    private final FileDialog saveDialog;
    private Component optionPaneParent;
    private final String messageFormatString;

    DialogManager(Builder builder) {
        this.fileDialog = builder.fileDialog;
        this.saveDialog = builder.saveDialog;
        this.optionPaneParent = builder.optionPaneParent;
        this.messageFormatString = builder.messageFormatString;
    }

    public void showWarning(String message) {
        showWarning(message, null);
    }

    public void showWarning(String message, String description) {
        String msg = createMessageDialogText(message, description);
        JOptionPane.showMessageDialog(getOptionPaneParent(), msg, null, JOptionPane.WARNING_MESSAGE);
    }

    public File askFile(){
        return askFile(null);
    }

    public File askFile(String file) {
        FileDialog d = saveDialog;
        d.setFile(file);

        d.setVisible(true);
        repaintOwnerWindow(d);

        if (d.getFile() == null) return null;

        File f = new File(d.getDirectory(), d.getFile());
        return f;
    }

    public File selectFile() {
        fileDialog.setModal(true);
        fileDialog.setMode(FileDialog.LOAD);

        fileDialog.setVisible(true);

        if (fileDialog.getFile() == null) return null;

        return new File(fileDialog.getDirectory(), fileDialog.getFile());
    }
    
    public void setOptionPaneParent(Component optionPaneParent) {
        this.optionPaneParent = optionPaneParent;
    }
    
    public Component getOptionPaneParent() {
        return optionPaneParent;
    }

    protected String createMessageDialogText(String message, String description){
        if (description == null)
            description = "";

        return MessageFormat.format(messageFormatString, message, description);
    }

    private void repaintOwnerWindow(Window window){
        Window owner = window.getOwner();
        if (owner == null) return;

        owner.repaint();
    }
    
    public static class Builder {
        public static final String MESSAGE_FORMAT_RESOURCE = "message_format.html";
        
        private final JFrame dummyParent = new JFrame();
        private FileDialog fileDialog = new FileDialog(dummyParent);
        private FileDialog saveDialog = new FileDialog(dummyParent);
        private Component optionPaneParent = null;
        private String messageFormatString;
        {
            InputStream formatResource = DialogManager.class.getResourceAsStream(MESSAGE_FORMAT_RESOURCE);
            BufferedReader reader = new BufferedReader(new InputStreamReader(formatResource));
            
        }
        
        public DialogManager build() {
            return new DialogManager(this);
        }
        
        public void setFileDialog(FileDialog fileDialog) {
            this.fileDialog = fileDialog;
        }
        
        public void setSaveDialog(FileDialog saveDialog) {
            this.saveDialog = saveDialog;
        }
        
        public void setOptionPaneParent(Component optionPaneParent) {
            this.optionPaneParent = optionPaneParent;
        }
    }
}
