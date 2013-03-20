package jp.scid.gui.control;

import java.awt.EventQueue;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;

import jp.scid.gui.model.ValueModel;
import jp.scid.gui.model.ValueModels;

/**
 * 
 * @author Ryusuke Higuchi
 * @deprecated use {@link TextController}
 */
@Deprecated
public class DocumentTextController extends AbstractController<Document> {
    final DocumentListener modelChangeListener = new DocumentListener() {
        public void removeUpdate(DocumentEvent e) {
            updateModelLater(e.getDocument());
        }
        
        public void insertUpdate(DocumentEvent e) {
            updateModelLater(e.getDocument());
        }
        
        public void changedUpdate(DocumentEvent e) {}
    };
    
    private final ValueModel<String> textModel; 

    public DocumentTextController(ValueModel<String> textModel) {
        this.textModel = textModel;
    }
    
    public DocumentTextController(String initialValue) {
        this(ValueModels.newValueModel(initialValue));
    }
    
    public DocumentTextController() {
        this("");
    }
    
    public void setValue(String text) {
        textModel.setValue(text);
    }
    
    public String getValue() {
        return textModel.getValue();
    }
    
    protected ValueModel<String> getValueModel() {
        return textModel;
    }
    
    public TextComponentTextConnector bindTextComponent(JTextComponent field) {
        TextComponentTextConnector controller = new TextComponentTextConnector(field);
        controller.setModel(getValueModel());
        return controller;
    }
    
    void updateModelLater(final Document model) {
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                updateStringModel(model);
            }
        });
    }

    void updateStringModel(Document model) {
        final String currentValue = textModel.getValue();
        
        final String newValue;
        try {
            newValue = model.getText(0, model.getLength());
        }
        catch (BadLocationException e) {
            throw new IllegalStateException(e);
        }
        
        if (!newValue.equals(currentValue)) {
            textModel.setValue(newValue);
        }
    }
    
    @Override
    protected void processPropertyChange(Document model, String property) {
        updateStringModel(model);
    }
    
    @Override
    protected void listenTo(Document model) {
        model.addDocumentListener(modelChangeListener);
    }
    
    @Override
    protected void deafTo(Document model) {
        model.removeDocumentListener(modelChangeListener);
    }
}
