package jp.scid.gui.model.tree;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.tree.TreePath;

public class DefaultTreeExpansionModel implements TreeExpansionModel {
    List<ChangeListener> listeners = new LinkedList<ChangeListener>();
    
    Set<TreePath> expandedPaths = new HashSet<TreePath>();
    
    @Override
    public boolean isExpanded(TreePath path) {
        return expandedPaths.contains(path);
    }
    
    @Override
    public void setExpanded(TreePath path, boolean expanded) {
        if (expanded) {
            expandedPaths.add(path);
        }
        else {
            expandedPaths.remove(path);
        }
    }
    
    public void addChangeListener(ChangeListener listener) {
        listeners.add(listener);
    }
    
    public void removeChangeListener(ChangeListener listener) {
        listeners.remove(listener);
    }
    
    protected void fireStateChanged() {
        if (listeners.isEmpty())
            return;
        
        ChangeEvent event = new ChangeEvent(this);
        
        for (ChangeListener listener: listeners) {
            listener.stateChanged(event);
        }
    }
}
