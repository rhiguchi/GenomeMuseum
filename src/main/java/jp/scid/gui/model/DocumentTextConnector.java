package jp.scid.gui.model;

import java.awt.EventQueue;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;

public class DocumentTextConnector extends ValueModelConnector<String, Document> implements DocumentListener {

    public DocumentTextConnector(ValueModel<String> target) {
        super(target);
    }

    private void updateLater() {
        Runnable task = new Runnable() {
            public void run() {
                updateModelValue();
            }
        };
        EventQueue.invokeLater(task);
    }
    
    @Override public void insertUpdate(DocumentEvent e) { updateLater(); }
    @Override public void removeUpdate(DocumentEvent e) { updateLater(); }
    @Override public void changedUpdate(DocumentEvent e) { updateLater(); }

    @Override
    protected String getModelValue(Document source) {
        try {
            return source.getText(0, source.getLength());
        }
        catch (BadLocationException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    protected void installSourceChangeListener(Document source) {
        source.addDocumentListener(this);
    }

    @Override
    protected void uninstallSourceChangeListener(Document source) {
        source.removeDocumentListener(this);
    }
    
}