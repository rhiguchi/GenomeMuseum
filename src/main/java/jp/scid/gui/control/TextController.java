package jp.scid.gui.control;

import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.AbstractButton;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;

import jp.scid.gui.model.ValueModel;
import jp.scid.gui.model.ValueModels;

public class TextController extends ValueController<String> implements DocumentListener, ActionListener {
    public TextController(ValueModel<String> valueModel) {
        super(valueModel, "text");
    }
    
    public TextController(String initialValue) {
        this(ValueModels.newValueModel(initialValue));
    }
    
    public TextController() {
        this("");
    }

    public TextComponentTextConnector bindTextComponent(JTextComponent field) {
        TextComponentTextConnector controller = new TextComponentTextConnector(field);
        controller.setModel(getValueModel());
        return controller;
    }

    public TextComponentTextConnector bindTextComponent(JTextField field, Document document) {
        TextComponentTextConnector controller = bindTextComponent(field);
        document.addDocumentListener(this);
        field.addActionListener(this);
        return controller;
    }
    
    public ViewValueConnector<AbstractButton, String> bindButtonText(AbstractButton button) {
        ComponentPropertyConnector<AbstractButton, String> connector =
                new ComponentPropertyConnector<AbstractButton, String>(button, "text");
        connector.setModel(getValueModel());
        return connector;
    }
    
    void documentChange(String newValue) {
        final String currentValue = getValueModel().getValue();
        
        if (!newValue.equals(currentValue)) {
            processValueChange(newValue);
        }
    }

    String getDocumentString(Document model) {
        final String newValue;
        try {
            newValue = model.getText(0, model.getLength());
        }
        catch (BadLocationException e) {
            throw new IllegalStateException(e);
        }
        return newValue;
    }
    
    void documentChangeLater(final Document model) {
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                String newValue = getDocumentString(model);
                documentChange(newValue);
            }
        });
    }
    
    public void removeUpdate(DocumentEvent e) {
        documentChangeLater(e.getDocument());
    }
    
    public void insertUpdate(DocumentEvent e) {
        documentChangeLater(e.getDocument());
    }
    
    public void changedUpdate(DocumentEvent e) {}
    
    @Override
    public void actionPerformed(ActionEvent e) {
        String newValue = ((JTextField) e.getSource()).getText();
        documentChange(newValue);
    }
}
