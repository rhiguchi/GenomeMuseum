package jp.scid.gui.control.tree;

import javax.swing.JTree;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.tree.ExpandVetoException;
import javax.swing.tree.TreePath;

import jp.scid.gui.model.tree.DefaultTreeExpansionModel;
import jp.scid.gui.model.tree.TreeExpansionModel;

public class TreeExpansionController implements TreeExpansionListener, TreeWillExpandListener {
    
    protected final TreeExpansionModel expansionModel;
    
    public TreeExpansionController() {
        expansionModel = createTreeExpansionModel();
    }
    
    protected TreeExpansionModel createTreeExpansionModel() {
        return new DefaultTreeExpansionModel();
    }
    
    public static TreeExpansionController newConstantDepthExpansionController(int pathDepth) {
        return new ConstantDepthExpansionController(pathDepth);
    }
    
    public void bind(JTree tree) {
        bindTreeExpansion(tree);
        
        tree.addTreeWillExpandListener(this);
        
        for (int row = 0; row < tree.getRowCount(); row++) {
            TreePath path = tree.getPathForRow(row);
            
            tree.expandPath(path);
        }
    }
    
    public void unbind(JTree tree) {
        tree.removeTreeWillExpandListener(this);
    }
    
    public boolean isCollapsable(JTree tree, TreePath path) {
        return true;
    }
    
    public boolean isExpandable(JTree tree, TreePath path) {
        return true;
    }
    
    
    public void bindTreeExpansion(JTree tree) {
        TreeExpansionHandler.newHandler(tree, expansionModel);
        tree.addTreeExpansionListener(this);
    }
    
    public void setPathExpansion(TreePath path, boolean expanded) {
        expansionModel.setExpanded(path, expanded);
    }
    
    @Override
    public void treeExpanded(TreeExpansionEvent event) {
        setPathExpansion(event.getPath(), true);
    }

    @Override
    public void treeCollapsed(TreeExpansionEvent event) {
        setPathExpansion(event.getPath(), false);
    }
    
    @Override
    public void treeWillExpand(TreeExpansionEvent event) throws ExpandVetoException {
        if (!isExpandable((JTree) event.getSource(), event.getPath())) {
            throw new ExpandVetoException(event);
        }
    }

    @Override
    public void treeWillCollapse(TreeExpansionEvent event) throws ExpandVetoException {
        if (!isCollapsable((JTree) event.getSource(), event.getPath())) {
            throw new ExpandVetoException(event);
        }
    }
    
    void updateExpansion(JTree tree, TreeExpansionModel model) {
        for (int row = 0; row < tree.getRowCount(); row++) {
            TreePath path = tree.getPathForRow(row);
            
            if (model.isExpanded(path)) {
                tree.expandPath(path);
            }
            else {
                tree.collapsePath(path);
            }
        }
    }
}