package jp.scid.gui.control;

import javax.swing.AbstractButton;
import javax.swing.ButtonModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import jp.scid.gui.model.ValueModel;

public class ButtonSelectionConnector extends ViewValueConnector<AbstractButton, Boolean> {
    
    final ChangeListener selectionChangeListener = new ChangeListener() {
        @Override
        public void stateChanged(ChangeEvent e) {
            updateModelFromEditor();
        }
    };
    
    public ButtonSelectionConnector(AbstractButton targetView) {
        super(targetView);
    }

    public void listenEditingTo(ButtonModel editorModel) {
        editorModel.addChangeListener(selectionChangeListener);
    }
    
    public void deafEditingTo(ButtonModel editorModel) {
        editorModel.addChangeListener(selectionChangeListener);
    }
    
    @Override
    protected void updateView(AbstractButton target, Boolean modelValue) {
        target.setSelected(modelValue);
    }
    
    void updateModelFromEditor() {
        ValueModel<Boolean> model = getModel();
        
        if (model == null)
            return;
        
        boolean currentValue = model.getValue();
        
        boolean newValue = getEditorValue(getView());
        
        if (currentValue != newValue) {
            model.setValue(newValue);
        }
    }
    
    boolean getEditorValue(AbstractButton button) {
        return button.isSelected();
    }
}
