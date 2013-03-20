package jp.scid.gui.control;

import java.awt.EventQueue;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;

import jp.scid.gui.model.ValueModel;

/**
 * {@link JTextComponent} およびそのサブクラスと値モデルを結合するためのクラス。
 * 
 * {@link Document} の変更は削除イベントと挿入イベントに分かれるため、
 * モデルからのアップデートは次の Event Dispatch Thread から遅延実行される。
 * @author Ryusuke Higuchi
 *
 * @param <T>
 * @param <V>
 */
abstract class TextComponentValueConnector<T extends JTextComponent, V> extends ViewValueConnector<T, V> {
    public TextComponentValueConnector(T view) {
        super(view);
    }

    final DocumentListener editingValueChangeListener = new DocumentListener() {
        public void removeUpdate(DocumentEvent e) {
            updateModelLater();
        }
        
        public void insertUpdate(DocumentEvent e) {
            updateModelLater();
        }
        
        public void changedUpdate(DocumentEvent e) {}
    };

    @Deprecated
    public void listenEditingTo(Document editorModel) {
        editorModel.addDocumentListener(editingValueChangeListener);
    }
    
    @Deprecated
    public void deafEditingTo(Document editorModel) {
        editorModel.removeDocumentListener(editingValueChangeListener);
    }

    void updateModelLater() {
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                updateModelFromEditor();
            }
        });
    }
    
    void updateModelFromEditor() {
        ValueModel<V> model = getModel();
        
        if (model == null)
            return;
        
        V currentValue = model.getValue();
        
        final V newValue = getEditorValue(getView());
        
        if (currentValue != null && !currentValue.equals(newValue) ||
                currentValue == null && newValue != null) {
            model.setValue(newValue);
        }
    }
    
    abstract protected V getEditorValue(T editorView);
}
