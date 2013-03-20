package jp.scid.gui.control;

import java.awt.EventQueue;
import java.text.ParseException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JFormattedTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import jp.scid.gui.model.ValueModel;

public class FormattedTextValueConnector<V> extends TextComponentValueConnector<JFormattedTextField, V> 
        implements DocumentListener {
    private final static Logger logger = Logger.getLogger(FormattedTextValueConnector.class.getName());
    
    private final Class<V> valueClass;
    
    @Deprecated
    public FormattedTextValueConnector(JFormattedTextField target, Class<V> valueClass) {
        super(target);
        
        this.valueClass = valueClass;
    }
    
    public FormattedTextValueConnector(JFormattedTextField target) {
        super(target);
        
        this.valueClass = null;
    }
    
    public static <T> FormattedTextValueConnector<T> connect(ValueModel<T> model, JFormattedTextField target) {
        FormattedTextValueConnector<T> conn = new FormattedTextValueConnector<T>(target);
        conn.setModel(model);
        return conn;
    }
    
    public void enableIncrementCommit() {
        getView().getDocument().addDocumentListener(this);
    }
    
    public void disableIncrementCommit() {
        getView().getDocument().removeDocumentListener(this);
    }

    @Override
    protected void updateView(JFormattedTextField target, V model) {
        target.setValue(model);
    }
    
    @Override
    protected V getEditorValue(JFormattedTextField editorView) {
        return valueClass.cast(editorView.getValue());
    }

    void commitEditWhenPossible() {
        JFormattedTextField field = getView();
        if (field.isEditValid()) {
            try {
                field.commitEdit();
            }
            catch (ParseException e) {
                // ignore
                logger.log(Level.FINE, "invalid inupt", e);
            }
        }
    }

    void commitEditLater() {
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                commitEditWhenPossible();
            }
        });
    }
    
    public void removeUpdate(DocumentEvent e) {
        commitEditLater();
    }
    
    public void insertUpdate(DocumentEvent e) {
        commitEditLater();
    }
    
    public void changedUpdate(DocumentEvent e) {}
}