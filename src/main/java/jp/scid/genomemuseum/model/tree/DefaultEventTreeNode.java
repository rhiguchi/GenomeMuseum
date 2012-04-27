package jp.scid.genomemuseum.model.tree;

import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;

public class DefaultEventTreeNode<E> extends SourceTreeNode<E> {
    final DefaultTreeModel treeModel;

    public DefaultEventTreeNode(DefaultTreeModel treeModel, E userObject, boolean allowsChildren) {
        super(userObject, allowsChildren);
        if (treeModel == null) throw new IllegalArgumentException("treeModel must not be null");
        
        this.treeModel = treeModel;
    }
    
    @Override
    protected MutableTreeNode createChildTreeNode(E userObject, boolean allowsChildren)  {
        DefaultEventTreeNode<E> node =
                new DefaultEventTreeNode<E>(treeModel, userObject, allowsChildren);
        node.setParent(this);
        node.setTreeSource(getTreeSource());
        
        return node;
    }

    @Override
    protected void fireChildInserted(int index, Object node) {
        treeModel.nodesWereInserted(this, new int[]{index});
    }

    @Override
    protected void fireChildChanged(int index, Object node) {
        treeModel.nodesChanged(this, new int[]{index});
    }

    @Override
    protected void fireChildRemoved(int index, Object node) {
        treeModel.nodesWereRemoved(this, new int[]{index}, new Object[]{node});
    }
}
