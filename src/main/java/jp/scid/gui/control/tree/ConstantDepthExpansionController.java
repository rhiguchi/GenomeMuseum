package jp.scid.gui.control.tree;

import javax.swing.JTree;
import javax.swing.tree.TreePath;

class ConstantDepthExpansionController extends TreeExpansionController {
    private final int pathDepth;
    
    public ConstantDepthExpansionController(int pathDepth) {
        super();
        this.pathDepth = pathDepth;
    }

    @Override
    public boolean isCollapsable(JTree tree, TreePath path) {
        if (path == null)
            return false;
        
        return pathDepth < path.getPathCount();
    }
}