package jp.scid.gui.control;

import javax.swing.text.JTextComponent;

import jp.scid.gui.model.ValueModel;

public class TextComponentTextConnector extends TextComponentValueConnector<JTextComponent, String> {
    public TextComponentTextConnector(JTextComponent target) {
        super(target);
    }
    
    @Override
    protected String getEditorValue(JTextComponent editorView) {
        return editorView.getText();
    }
    
    @Override
    protected void updateView(JTextComponent view, String value) {
        int caretPosition = view.getCaretPosition();
        view.setText(value);
        if (caretPosition > view.getText().length())
            caretPosition = view.getText().length();
        
        view.setCaretPosition(caretPosition);
    }
    
    public static TextComponentTextConnector connect(ValueModel<String> model, JTextComponent target) {
        TextComponentTextConnector conn = new TextComponentTextConnector(target);
        conn.setModel(model);
        return conn;
    }
}