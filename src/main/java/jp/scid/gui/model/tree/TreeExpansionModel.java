package jp.scid.gui.model.tree;

import javax.swing.event.ChangeListener;
import javax.swing.tree.TreePath;

public interface TreeExpansionModel {
    boolean isExpanded(TreePath path);
    
    void setExpanded(TreePath path, boolean expanded);
    
    void addChangeListener(ChangeListener listener);
    
    void removeChangeListener(ChangeListener listener);
}
