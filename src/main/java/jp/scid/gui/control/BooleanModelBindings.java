package jp.scid.gui.control;

import javax.swing.Action;

import jp.scid.gui.model.ValueModel;

public class BooleanModelBindings {
    private final ValueModel<Boolean> model;

    public BooleanModelBindings(ValueModel<Boolean> model) {
        if (model == null) throw new IllegalArgumentException("model must not be null");
        this.model = model;
    }
    
    public ModelConnector bindToActionEnabled(Action action) {
        ActionEnableConnector connector = new ActionEnableConnector(action);
        connector.setModel(model);
        return connector;
    }
    
    public static interface ModelConnector {
        void dispose();
    }
    
    private class ActionEnableConnector extends ValueChangeHandler<Boolean> implements ModelConnector {
        private final Action action;
        
        public ActionEnableConnector(Action action) {
            if (action == null) throw new IllegalArgumentException("action must not be null");
            this.action = action;
        }

        @Override
        protected void valueChanged(Boolean newValue) {
            action.setEnabled(newValue);
        }
        
        public void dispose() {
            setModel(null);
        }
    }
}
