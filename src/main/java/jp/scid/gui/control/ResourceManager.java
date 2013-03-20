package jp.scid.gui.control;

import java.util.ResourceBundle;

public class ResourceManager {
    private final Class<?> resourceClass;
    private ResourceBundle resourceBundle;

    public ResourceManager(Class<?> resourceClass) {
        this.resourceClass = resourceClass;
    }
    
    public ResourceBundle getResourceBundle() {
        if (resourceBundle == null)
            resourceBundle = ResourceBundle.getBundle(resourceClass.getName());
        return resourceBundle;
    }
    
    public ResourceEntry getEntry(String key) {
        // TODO check key existence
        EnclosingResourceEntry entry = new EnclosingResourceEntry(key);
        return entry;
    }
    
    public static interface ResourceEntry {
        boolean exists();
        String getString(Object... formatArgs);
        
        ResourceEntry getSubEntry(String suffix);
    }
    
    class EnclosingResourceEntry implements ResourceEntry {
        private final String key;

        public EnclosingResourceEntry(String key) {
            this.key = key;
        }
        
        public boolean exists() {
            return getResourceBundle().containsKey(key);
        }

        @Override
        public String getString(Object... formatArgs) {
            String string = getResourceBundle().getString(key);
            
            if (formatArgs == null || formatArgs.length == 0) {
                return string;
            }
            else {
                return String.format(string, formatArgs);
            }
        }
        
        public ResourceEntry getSubEntry(String suffix) {
            return getEntry(key + "." + suffix);
        }

        @Override
        public String toString() {
            return "ResourceEntry [key=" + key + ", class="
                    + resourceClass + "]";
        }
    }
}
