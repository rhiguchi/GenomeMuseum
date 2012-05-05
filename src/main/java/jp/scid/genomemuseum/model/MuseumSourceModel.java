package jp.scid.genomemuseum.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeNode;

import jp.scid.genomemuseum.model.CollectionBox.BoxType;
import jp.scid.genomemuseum.model.CollectionBox.GroupCollectionBox;

public class MuseumSourceModel {
    public static interface NodeElement {
        String getName();
    }
    
    public static class Category implements NodeElement, Comparable<MuseumSourceModel.Category> {
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
    
    public static interface LibraryElement extends NodeElement {
        int orderPriority();
    }
    
    private final DefaultTreeModel delegate;
    
    private final DefaultCategoryNode librariesNode;
    
    private DefaultMutableTreeNode localLibraryNode = null;
    
    private CollectionBoxTreeRootNode collectionBoxTreeRootNode = null;
    
    protected Comparator<TreeNode> nodeComparator = null;
    
    public MuseumSourceModel(DefaultTreeModel delegate) {
        this.delegate = delegate;
        
        librariesNode = new DefaultCategoryNode("Libraries", 0);
        addCategoryNode(librariesNode);
    }
    
    public void addCategoryNode(CategoryNode categoryNode) {
        insertToOrderedIndex(categoryNode, getRoot(), nodeComparator());
    }
    
    protected void addLibraryElement(MutableTreeNode libraryNode) {
        insertToOrderedIndex(libraryNode, librariesNode, nodeComparator());
    }
    
    public CategoryNode getLibrariesNode() {
        return librariesNode;
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
    
    public void setLocalLibrarySource(Object librarySource) {
        if (localLibraryNode != null) {
            delegate.removeNodeFromParent(localLibraryNode);
        }
        
        localLibraryNode = null;
        
        if (librarySource != null) {
            localLibraryNode = new DefaultMutableTreeNode(librarySource, false);
            addLibraryElement(localLibraryNode);
        }
    }
    
    public DefaultMutableTreeNode getLocalLibraryNode() {
        return localLibraryNode;
    }
    
    Comparator<TreeNode> nodeComparator() {
        return categoryNodeComparator;
    }
    
    public CollectionBoxTreeRootNode getCollectionBoxTreeRootNode() {
        return collectionBoxTreeRootNode;
    }
    
    void setCollectionBoxTreeRootNode(CollectionBoxTreeRootNode newNode) {
        if (collectionBoxTreeRootNode != null) {
            removeNodeFromParent(collectionBoxTreeRootNode);
        }
        
        this.collectionBoxTreeRootNode = newNode;
        
        if (newNode != null) {
            delegate.insertNodeInto(newNode, getRoot(), getRoot().getChildCount());
        }
    }
    
    public CollectionBoxService getCollectionBoxTreeModel() {
        return collectionBoxTreeRootNode == null ? null : collectionBoxTreeRootNode.getService();
    }
    
    public void setCollectionBoxTreeModel(CollectionBoxService newSource) {
        final CollectionBoxTreeRootNode newNode;
        
        if (newSource != null) {
            newNode = new CollectionBoxTreeRootNode(newSource, 10);
            newNode.setUserObject("Collections");
        }
        else {
            newNode = null;
        }
        
        setCollectionBoxTreeRootNode(newNode);
    }
    
    public void reloadCollectionBoxNode(CollectionBoxNode node) {
        GroupCollectionBox parent = (GroupCollectionBox) node.getCollectionBox();
        List<CollectionBox> children = parent.fetchChildren();
        
        List<CollectionBoxNode> newChildNodeList = new ArrayList<CollectionBoxNode>();
        
        for (CollectionBox child: children) {
            CollectionBoxNode childNode = createCollectionBoxNode(child);
            newChildNodeList.add(childNode);
        }
        
        node.setChildren(newChildNodeList);
        
        delegate.reload(node);
    }
    
    protected CollectionBoxNode createCollectionBoxNode(CollectionBox box) {
        boolean allowChildren = box instanceof GroupCollectionBox;
        DefaultCollectionBoxNode node = new DefaultCollectionBoxNode(box, allowChildren);
        return node;
    }

    public CollectionBoxNode addCollectionBox(BoxType boxType, CollectionBoxNode target) {
        if (boxType == null) throw new IllegalArgumentException("boxType must not be null");
        if (target == null) throw new IllegalArgumentException("target must not be null");
        
        final CollectionBoxNode parentNode =
                target.getAllowsChildren() ? target : (CollectionBoxNode) target.getParent();
        
        final GroupCollectionBox parentBox = (GroupCollectionBox) parentNode.getCollectionBox();
        
        final CollectionBox newBox = parentBox.addChild(boxType);
        
        CollectionBoxNode newChild = createCollectionBoxNode(newBox);
        
        List<CollectionBox> children = parentBox.fetchChildren();
        int index = children.indexOf(newBox);
        if (index < 0)
            index = parentNode.getChildCount();
        
        delegate.insertNodeInto(newChild, parentNode, index);
        
        return newChild;
    }
    
    boolean canMove(CollectionBox box, CollectionBox target) {
        final boolean canMove;
        
        if (box.getId() != null && box.getId().equals(target.getId())) {
            canMove = false;
        }
        else if (target instanceof GroupCollectionBox) {
            Long targetId = target.getId();
            
            if (targetId != null && box instanceof GroupCollectionBox) {
                canMove = !((GroupCollectionBox) box).isAncestorOf(targetId);
            }
            else {
                canMove = true;
            }
        }
        else {
            canMove = false;
        }
        
        return canMove;
    }
    
    public boolean canMove(CollectionBoxNode node, CollectionBoxNode target) {
        CollectionBox boxOfNode = node.getCollectionBox();
        CollectionBox boxOfTarget = target.getCollectionBox();
        
        return canMove(boxOfNode, boxOfTarget);
    }
    
    public void moveCollectionBox(CollectionBoxNode boxNode, CollectionBoxNode target) {
        if (boxNode == null) throw new IllegalArgumentException("boxNode must not be null");
        if (target == null) throw new IllegalArgumentException("target must not be null");
        
        CollectionBox box = boxNode.getCollectionBox();
        GroupCollectionBox parentBox = (GroupCollectionBox) target.getCollectionBox();
        
        int index = box.setParent(parentBox);
        
        delegate.insertNodeInto(boxNode, target, index);
    }

    public void removeCollectionBox(CollectionBoxNode collectionBoxNode) {
        delegate.removeNodeFromParent(collectionBoxNode);
        CollectionBox box = collectionBoxNode.getCollectionBox();
        box.delete();
    }
    
    public MutableTreeNode getRoot() {
        return (MutableTreeNode) delegate.getRoot();
    }

    public static interface CategoryNode extends MutableTreeNode {
        int getPriority();
    }
    
    public static interface CollectionBoxNode extends MutableTreeNode {
        CollectionBox getCollectionBox();
        
        void setChildren(List<CollectionBoxNode> newChildren);
    }
    
    // Node Implementations
    static class DefaultCategoryNode extends DefaultMutableTreeNode implements MuseumSourceModel.CategoryNode {
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
    
    static class DefaultCollectionBoxNode extends DefaultMutableTreeNode implements CollectionBoxNode {
        private final CollectionBox box;
        
        public DefaultCollectionBoxNode(CollectionBox box, boolean allowChildren) {
            super(box, allowChildren);
            this.box = box;
        }
        
        @Override
        public CollectionBox getCollectionBox() {
            return box;
        }
        
        @Override
        public void setChildren(List<CollectionBoxNode> newChildren) {
            removeAllChildren();
            
            for (CollectionBoxNode node: newChildren) {
                add(node);
            }
        }
        
        @Override
        public String toString() {
            if (box != null) {
                return box.getName();
            }
            return super.toString();
        }
    }
    
    static class CollectionBoxTreeRootNode extends DefaultCollectionBoxNode implements CategoryNode {
        final int priority;
        final CollectionBoxService service;

        public CollectionBoxTreeRootNode(CollectionBoxService service, int priority) {
            super(service.getGroupingDelegate(), true);
            
            this.service = service;
            this.priority = priority;
            
            setUserObject(service);
        }
        
        public CollectionBoxService getService() {
            return service;
        }
        
        @Override
        public int getPriority() {
            return priority;
        }
        
        @Override
        public String toString() {
            return getUserObject().toString();
        }
    }
}