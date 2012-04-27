package jp.scid.genomemuseum.model;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.swing.event.TreeModelListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import jp.scid.genomemuseum.model.CollectionBox.BoxType;
import jp.scid.genomemuseum.model.tree.DefaultEventTreeNode;

public class MuseumSourceModel implements TreeModel {
    public static interface NodeElement {
        String getName();
    }
    
    public static class Category implements MuseumSourceModel.NodeElement, Comparable<MuseumSourceModel.Category> {
        final int priority;
        String name;
        
        public Category(int priority, String name) {
            this.priority = priority;
            this.name = name;
        }

        @Override
        public int compareTo(MuseumSourceModel.Category o) {
            return o.priority - this.priority;
        }

        @Override
        public String getName() {
            return name;
        }
        
        public void setName(String name) {
            this.name = name;
        }
        
        @Override
        public String toString() {
            return name;
        }
    }
    
    private final Comparator<TreeNode> categoryNodeComparator = new Comparator<TreeNode>() {
        @Override
        public int compare(TreeNode o1, TreeNode o2) {
            if (o1 instanceof MuseumSourceModel.CategoryNode && o2 instanceof MuseumSourceModel.CategoryNode) {
                return ((MuseumSourceModel.CategoryNode) o1).getPriority() - ((MuseumSourceModel.CategoryNode) o2).getPriority();
                
            }
            else {
                return o1.toString().compareTo(o2.toString());
            }
        }
    };
    
    public static interface LibraryElement extends MuseumSourceModel.NodeElement {
        int orderPriority();
    }
    
    private final DefaultTreeModel delegate;
    
    private final MuseumSourceModel.DefaultCategoryNode librariesNode;
    
    private DefaultEventTreeNode<CollectionBox> boxTreeRootNode = null;
    
    protected Comparator<TreeNode> nodeComparator = null;
    
    public MuseumSourceModel(DefaultTreeModel delegate) {
        this.delegate = delegate;
        
        librariesNode = new DefaultCategoryNode("Libraries", 0);
        addCategoryNode(librariesNode);
    }
    
    public void addCategoryNode(MuseumSourceModel.CategoryNode categoryNode) {
        insertToOrderedIndex(categoryNode, getRoot(), nodeComparator());
    }
    
    protected void addLibraryElement(MutableTreeNode libraryNode) {
        insertToOrderedIndex(libraryNode, librariesNode, nodeComparator());
    }
    
    int insertToOrderedIndex(
            MutableTreeNode child, MutableTreeNode parent, Comparator<? super TreeNode> comparator) {
        @SuppressWarnings("unchecked")
        List<TreeNode> childNodeList =
                Collections.checkedList(Collections.list(parent.children()), TreeNode.class);
        
        int index = Collections.binarySearch(childNodeList, child, comparator);
        if (index < 0) {
            index = -(index + 1);
        }
        
        delegate.insertNodeInto(child, parent, index);
        
        return index;
    }
    
    public void removeNodeFromParent(MutableTreeNode node) {
        delegate.removeNodeFromParent(node);
    }
    
    public void addLibrarySource() {
        // TODO
        
    }
    
    public void removeLibrarySource() {
        // TODO
        
    }
    
    Comparator<TreeNode> nodeComparator() {
        return categoryNodeComparator;
    }
    
    public CollectionBoxService getCollectionBoxTreeModel() {
        return (CollectionBoxService) boxTreeRootNode.getTreeSource();
    }
    
    public void setCollectionBoxTreeModel(CollectionBoxService newSource) {
        if (boxTreeRootNode != null) {
            removeNodeFromParent(boxTreeRootNode);
            boxTreeRootNode = null;
        }
        
        if (newSource != null) {
            boxTreeRootNode = new DefaultEventTreeNode<CollectionBox>(delegate, null, true);
            boxTreeRootNode.setTreeSource(newSource);
            boxTreeRootNode.setUserObject("Collections");
            
            delegate.insertNodeInto(boxTreeRootNode, getRoot(), getRoot().getChildCount());
        }
    }

    public void addCollectionBox(BoxType boxType, CollectionBoxNode parentNode) {
        CollectionBoxService model = getCollectionBoxTreeModel();
        CollectionBox newBox = model.createBox(boxType);
        
        CollectionBox parent = parentNode == null ? null : parentNode.getCollectionBox();
        model.insert(newBox, parent);
    }
    
    public void moveCollectionBox(CollectionBoxNode node, CollectionBoxNode newParent) {
        CollectionBoxService model = getCollectionBoxTreeModel();
        
        CollectionBox parent = newParent == null ? null : newParent.getCollectionBox();
        model.setParent(node.getCollectionBox(), parent);
    }

    public void removeCollectionBox(CollectionBoxNode node) {
        CollectionBoxService model = getCollectionBoxTreeModel();
        model.delete(node.getCollectionBox());
    }
    
    @Override
    public MutableTreeNode getRoot() {
        return (MutableTreeNode) delegate.getRoot();
    }

    @Override
    public Object getChild(Object parent, int index) {
        return delegate.getChild(parent, index);
    }

    @Override
    public int getChildCount(Object parent) {
        return delegate.getChildCount(parent);
    }

    @Override
    public boolean isLeaf(Object node) {
        return delegate.isLeaf(node);
    }

    @Override
    public int getIndexOfChild(Object parent, Object child) {
        return delegate.getIndexOfChild(parent, child);
    }
    
    @Override
    public void valueForPathChanged(TreePath path, Object newValue) {
        delegate.nodeChanged((TreeNode) path.getLastPathComponent());
    }

    @Override
    public void addTreeModelListener(TreeModelListener l) {
        delegate.addTreeModelListener(l);
    }

    @Override
    public void removeTreeModelListener(TreeModelListener l) {
        delegate.removeTreeModelListener(l);
    }
    
    public static interface CategoryNode extends MutableTreeNode {
        int getPriority();
    }
    
    public static interface CollectionBoxNode extends TreeNode {
        CollectionBox getCollectionBox();
    }
    
    public static class DefaultCategoryNode extends DefaultMutableTreeNode implements MuseumSourceModel.CategoryNode {
        protected final int priority;
        
        public DefaultCategoryNode(int priority) {
            this(null, priority);
        }

        public DefaultCategoryNode(Object userObject, int priority) {
            super(userObject);
            
            this.priority = priority;
        }
        
        @Override
        public int getPriority() {
            return 0;
        }
    }
}