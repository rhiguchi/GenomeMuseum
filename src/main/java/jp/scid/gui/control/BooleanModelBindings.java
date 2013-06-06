package jp.scid.gui.control;

import java.awt.Component;

import javax.swing.Action;
import javax.swing.JProgressBar;

import jp.scid.gui.model.ValueModel;

public class BooleanModelBindings extends AbstractValueModelBindigs<Boolean> {

    public BooleanModelBindings(ValueModel<Boolean> model) {
        super(model);
    }

    public ModelConnector bindToActionEnabled(Action action) {
        return installModel(new ActionEnableConnector(action));
    }
    
    public ModelConnector bindToComponentVisibled(Component component) {
        return installModel(new ComponentVisibleConnector(component));
    }
    
    public ModelConnector bindToProgressBarIndeterminate(JProgressBar component) {
        return installModel(new ProgressBarIndeterminateConnector(component));
    }
    
    public static interface ModelConnector {
        void dispose();
    }
    
    private static class ActionEnableConnector extends AbstractPropertyConnector<Boolean> {
        private final Action action;
        
        public ActionEnableConnector(Action action) {
            if (action == null) throw new IllegalArgumentException("action must not be null");
            this.action = action;
        }

        @Override
        protected void valueChanged(Boolean newValue) {
            action.setEnabled(newValue);
        }
    }
    
    private static class ComponentVisibleConnector extends AbstractPropertyConnector<Boolean> {
        private final Component component;
        
        public ComponentVisibleConnector(Component component) {
            this.component = component;
        }

        @Override
        protected void valueChanged(Boolean newValue) {
            component.setVisible(newValue);
        }
    }
    
    private static class ProgressBarIndeterminateConnector extends AbstractPropertyConnector<Boolean> {
        private final JProgressBar progressBar;
        
        public ProgressBarIndeterminateConnector(JProgressBar progressBar) {
            this.progressBar = progressBar;
        }
        
        @Override
        protected void valueChanged(Boolean newValue) {
            progressBar.setIndeterminate(newValue);
        }
    }
}
