package jp.scid.gui.control;

import java.awt.EventQueue;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;

import jp.scid.gui.model.ValueModel;

@Deprecated
public class TextComponentTextHandler extends ValueHandler<String> implements DocumentListener {
    private final JTextComponent textComponent;
    
    public void removeUpdate(DocumentEvent e) {
        updateModelLater(e.getDocument());
    }
    
    public void insertUpdate(DocumentEvent e) {
        updateModelLater(e.getDocument());
    }
    
    public void changedUpdate(DocumentEvent e) {}
    
    public TextComponentTextHandler(JTextComponent textComponent) {
        this.textComponent = textComponent;
    }

    void updateModelLater(final Document document) {
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                updateModel(document);
            }
        });
    }
    
    public void listenTo(Document docment) {
        docment.addDocumentListener(this);
    }
    
    public void deafTo(Document docment) {
        docment.removeDocumentListener(this);
    }
    
    public void updateModel(Document document) {
        ValueModel<String> model = getModel();
        
        if (model == null)
            return;
        
        String currentValue = model.getValue();
        
        final String newValue;
        try {
            newValue = document.getText(0, document.getLength());
        }
        catch (BadLocationException e) {
            throw new IllegalStateException(e);
        }
        
        if (currentValue != null && !currentValue.equals(newValue) ||
                currentValue == null && newValue != null) {
            model.setValue(newValue);
        }
    }

    @Override
    protected void handleValue(String newValue) {
        textComponent.setText(newValue);
    }
}
