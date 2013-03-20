package jp.scid.gui.control.tree;

import javax.swing.JTree;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.tree.TreePath;

import jp.scid.gui.model.tree.TreeExpansionModel;

public class TreeExpansionHandler implements ChangeListener {
    final JTree tree;

    protected TreeExpansionModel model;
    
    public TreeExpansionHandler(JTree tree) {
        this.tree = tree;
    }
    
    public static TreeExpansionHandler newHandler(JTree tree, TreeExpansionModel model) {
        TreeExpansionHandler handler = new TreeExpansionHandler(tree);
        handler.setModel(model);
        return handler;
    }

    public TreeExpansionModel getModel() {
        return model;
    }
    
    public void setModel(TreeExpansionModel newModel) {
        if (this.model != null) {
            this.model.removeChangeListener(this);
        }
        
        this.model = newModel;
        
        if (newModel != null) {
            updateExpansion(newModel);
            newModel.addChangeListener(this);
        }
    }

    void updateExpansion(TreeExpansionModel model) {
        for (int row = 0; row < tree.getRowCount(); row++) {
            TreePath path = tree.getPathForRow(row);
            
            if (model.isExpanded(path)) {
                if (!tree.isExpanded(path))
                    tree.expandPath(path);
            }
            else {
                if (tree.isExpanded(path))
                    tree.collapsePath(path);
            }
        }
    }
    
    @Override
    public void stateChanged(ChangeEvent e) {
        updateExpansion(getModel());
    }
}