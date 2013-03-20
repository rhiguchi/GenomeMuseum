package jp.scid.gui.control;

import static java.lang.String.*;

import java.awt.event.ActionEvent;
import java.beans.PropertyChangeListener;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.Action;

import org.jdesktop.application.Application;
import org.jdesktop.application.ApplicationActionMap;
import org.jdesktop.application.ApplicationContext;
import org.jdesktop.application.ResourceMap;

public abstract class ApplicationController {
    private final static Logger logger = Logger.getLogger(ApplicationController.class.getName());

    protected ControllerAction getAction(String key) {
        return getAction(key, getActionMap());
    }

    protected ControllerResource getResource(String key) {
        return getResource(key, getResourceMap());
    }

    protected ApplicationActionMap getActionMap() {
        return getApplicationContext().getActionManager()
                .getActionMap(getApplicationClass(), this);
    }
    
    protected ResourceMap getResourceMap() {
        return getApplicationContext().getResourceManager().getResourceMap(
                getClass(), getApplicationClass());
    }

    static ApplicationContext getApplicationContext(Class<? extends Application> applicationClass) {
        Application instance;
        try {
            instance = Application.getInstance(applicationClass);
        }
        catch (IllegalStateException e) {
            java.beans.Beans.setDesignTime(true);
            instance = Application.getInstance(applicationClass);
        }
        
        return instance.getContext();
    }

    protected ApplicationContext getApplicationContext() {
        return getApplicationContext(getApplicationClass());
    }

    abstract protected Class<? extends Application> getApplicationClass();
    
    protected static ControllerAction getAction(String name, ApplicationActionMap actionMap) {
        logger.log(Level.FINE, format(
                "Get '%s' action from '%s'.", name, actionMap.getActionsClass()));
        
        Action action = actionMap.get(name);
        
        if (action == null) {
            logger.log(Level.WARNING, format(
                    "Action '%s' is not defined on '%s'.", name, actionMap.getActionsClass()));
            action = new AbstractAction(name) {
                public void actionPerformed(ActionEvent e) {}
            };
            action.setEnabled(false);
            return new ControllerAction(action, null);
        }
        else {
            return new ControllerAction(action, name);
        }
    }

    protected static ControllerResource getResource(String key, ResourceMap resourceMap) {
        logger.log(Level.FINE, format(
                "Get '%s' resource from '%s'.", key, resourceMap.getBundleNames()));
        
        if (!resourceMap.containsKey(key))
            logger.log(Level.WARNING, format("Resource '%s' is not defined on '%s'.",
                    key, resourceMap.getBundleNames()));
        
        return new ControllerResource(key, resourceMap);
    }
    
    public static class ControllerAction implements Action {
        private final Action action;
        private final String name;
        
        public ControllerAction(Action action, String name) {
            if (action == null) {
                action = new AbstractAction(name) {
                    public void actionPerformed(ActionEvent e) {}
                };
                action.setEnabled(false);
            }
            
            this.action = action;
            this.name = name;
        }
        
        public String getName() {
            return name;
        }
        
        @Override
        public void actionPerformed(ActionEvent e) {
            action.actionPerformed(e);
        }
        
        public Object getValue(String key) {
            return action.getValue(key);
        }
        
        public void putValue(String key, Object value) {
            action.putValue(key, value);
        }
        
        public void setEnabled(boolean b) {
            action.setEnabled(b);
        }
        
        public boolean isEnabled() {
            return action.isEnabled();
        }
        
        public void addPropertyChangeListener(PropertyChangeListener listener) {
            action.addPropertyChangeListener(listener);
        }
        
        public void removePropertyChangeListener(PropertyChangeListener listener) {
            action.removePropertyChangeListener(listener);
        }
    }
    
    public static class ControllerResource {
        private final String key;
        private final ResourceMap resourceMap;
        
        public ControllerResource(String key, ResourceMap resourceMap) {
            this.key = key;
            this.resourceMap = resourceMap;
        }
        
        public String getKey() {
            return key;
        }
        
        public boolean existsKey() {
            return resourceMap.containsKey(key);
        }
        
        public String getString(Object... args) {
            return resourceMap.getString(key, args);
        }
        
        @Override
        public String toString() {
            return "ApplicationResource [key=" + key + ", map="
                    + resourceMap.getBundleNames() + "]";
        }
    }
}

