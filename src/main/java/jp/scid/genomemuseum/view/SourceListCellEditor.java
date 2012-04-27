package jp.scid.genomemuseum.view;

import javax.swing.DefaultCellEditor;
import javax.swing.JTextField;

public class SourceListCellEditor extends DefaultCellEditor {
    public SourceListCellEditor(JTextField textField) {
        super(textField);
    }

    public SourceListCellEditor() {
        this(new JTextField());
    }
}
