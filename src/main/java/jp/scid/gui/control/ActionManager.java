package jp.scid.gui.control;

import static java.lang.String.*;

import java.awt.event.ActionEvent;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.Action;

public class ActionManager {
    private static final Logger logger = Logger.getLogger(ActionManager.class.getName());
            
    private final Object controller;
    
    private final Map<String, Method> methodMap;
    private boolean methodMapInitialized = false;

    public ActionManager(Object controller) {
        super();
        if (controller == null)
            throw new IllegalArgumentException("controller must not be null");
        this.controller = controller;
        
        methodMap = new HashMap<String, Method>();
    }

    public Action getAction(String methodName) {
        Method actionMethod = findMethod(methodName);
        Action action;
        
        if (actionMethod == null) {
            logger.log(Level.WARNING, format(
                    "Method '%s' is not defined on '%s'.",
                    methodName, controller.getClass()));
            
            action = NoMethodAction.newInstance(methodName);
        }
        else {
            action = new ActionEntry(actionMethod, methodName);
        }
        
        return action;
    }

    Method findMethod(String methodName) {
        if (!methodMapInitialized) {
            Class<? extends Object> controllerClass = controller.getClass();
            for (Method method: controllerClass.getDeclaredMethods()) {
                methodMap.put(method.getName(), method);
            }
            methodMapInitialized = true;
        }
        
        return methodMap.get(methodName);
    }
    
    public static class NoMethodAction extends AbstractAction {
        private NoMethodAction(String name) {
            super(name);
        }
        
        static Action newInstance(String methodName) {
            NoMethodAction a = new NoMethodAction(methodName);
            a.setEnabled(false);
            return a;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            // do nothing
        }
        
        @Override
        public final void setEnabled(boolean newValue) {
            super.setEnabled(false);
        }
    }
    
    class ActionEntry extends AbstractAction {
        private final Method actionMethod;
        private final String name;

        public ActionEntry(Method actionMethod, String name) {
            super(name);
            
            this.actionMethod = actionMethod;
            this.name = name;
        }

        public String getName() {
            return name;
        }

        @Override
        public void actionPerformed(ActionEvent event) {
            Class<?>[] parameterTypes = actionMethod.getParameterTypes();
            Object[] args = new Object[parameterTypes.length];
            
            for (int i = 0; i < parameterTypes.length; i++) {
                Class<?> parameterType = parameterTypes[i];
                Object param = null;
                
                if (ActionEvent.class.isAssignableFrom(parameterType)) {
                    param = event;
                }
                
                args[i] = param;
            }

            try {
                actionMethod.invoke(controller, args);
            }
            catch (IllegalArgumentException e) {
                logger.log(Level.WARNING, format(
                        "The method '%s' of '%s' is not invoked.",
                        name, controller), e);
            }
            catch (IllegalAccessException e) {
                logger.log(Level.WARNING, format(
                        "The method '%s' of '%s' is not invoked.",
                        name, controller), e);
            }
            catch (InvocationTargetException e) {
                logger.log(Level.WARNING, format(
                        "The method '%s' of '%s' is fail to invoke.",
                        name, controller), e.getCause());
            }
        }
    }
}
