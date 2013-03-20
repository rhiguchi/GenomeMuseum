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
import jp.scid.genomemuseum.model.CollectionBox.FreeCollectionBox;
import jp.scid.genomemuseum.model.CollectionBox.GroupCollectionBox;
import jp.scid.genomemuseum.model.CollectionBox.SmartCollectionBox;

public class MuseumSourceModel extends SourceListModel {
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
    
    
    private final DefaultCategoryNode librariesNode;
    
    private LocalLibraryNode localLibraryNode = null;
    
    private CollectionBoxTreeRootNode collectionBoxTreeRootNode = null;
    
    protected Comparator<TreeNode> nodeComparator = null;
    
    public MuseumSourceModel(DefaultTreeModel delegate) {
        super(delegate);
        
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
            localLibraryNode = new LocalLibraryNode(librarySource);
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
    
    public void reloadCollectionBoxNode(GroupCollectionNode node) {
        List<CollectionBox> children = node.fetchChildren();
        
        List<CollectionNode> newChildNodeList = new ArrayList<CollectionNode>();
        
        for (CollectionBox child: children) {
            CollectionNode childNode = createCollectionBoxNode(child);
            newChildNodeList.add(childNode);
        }
        
        node.setChildren(newChildNodeList);
        
        delegate.reload(node);
    }
    
    protected CollectionNode createCollectionBoxNode(CollectionBox box) {
        DefaultCollectionBoxNode node;
        
        if (box instanceof GroupCollectionBox) {
            node = new DefaultGroupCollectionNode((GroupCollectionBox) box, getCollectionBoxTreeModel());
        }
        else if (box instanceof SmartCollectionBox) {
            node = new DefaultCollectionBoxNode(box);
        }
        // FreeCollectionBox for others
        else {
            node = new DefaultCollectionBoxNode(box);
        }
        
        return node;
    }

    public CollectionNode addCollectionBox(BoxType boxType, GroupCollectionNode target) {
        if (boxType == null) throw new IllegalArgumentException("boxType must not be null");
        if (target == null) throw new IllegalArgumentException("target must not be null");
        
        final CollectionBox newBox = target.addChild(boxType);
        
        CollectionNode newChild = createCollectionBoxNode(newBox);
        
        List<CollectionBox> children = target.fetchChildren();
        int index = children.indexOf(newBox);
        if (index < 0)
            index = target.getChildCount();
        
        delegate.insertNodeInto(newChild, target, index);
        
        return newChild;
    }
    
    public void moveCollectionBox(CollectionNode boxNode, GroupCollectionNode target) {
        if (boxNode == null) throw new IllegalArgumentException("boxNode must not be null");
        if (target == null) throw new IllegalArgumentException("target must not be null");
        
        CollectionBox box = boxNode.getCollectionBox();
        target.setParentOfBox(box);
        
        int index = target.fetchChildren().indexOf(box);
        if (index < 0)
            index = target.fetchChildren().size();
        
        delegate.insertNodeInto(boxNode, target, index);
    }

    public void removeCollectionBox(CollectionNode collectionBoxNode) {
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
    
    public static interface ExhibitCollectionNode extends MutableTreeNode {
        
    }
    
    public static interface CollectionNode extends ExhibitCollectionNode {
        CollectionBox getCollectionBox();
        
        boolean isContentsEditable();
    }
    
    public static interface GroupCollectionNode extends CollectionNode {
        List<CollectionBox> fetchChildren();
        
        CollectionBox addChild(BoxType boxType);
        
        void setParentOfBox(CollectionBox child);
        
        boolean canMove(CollectionNode newChild);
        
        void setChildren(List<CollectionNode> newChildren);
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
    
    static class DefaultGroupCollectionNode extends DefaultCollectionBoxNode implements GroupCollectionNode {
        private final CollectionBoxService service;
        
        public DefaultGroupCollectionNode(GroupCollectionBox box, CollectionBoxService service) {
            super(box, true);
            this.service = service;
        }

        @Override
        public List<CollectionBox> fetchChildren() {
            return service.fetchChildren(getBoxId());
        }

        @Override
        public CollectionBox addChild(BoxType boxType) {
            return service.addChild(boxType, getBoxId());
        }
        
        @Override
        public void setParentOfBox(CollectionBox child) {
            service.setParent(child, getBoxId());
        }

        public boolean canMove(CollectionNode newChild) {
            boolean canMove = true;
            
            for (TreeNode ancestor: getPath()) {
                if (newChild.equals(ancestor)) {
                    canMove = false;
                    break;
                }
            }
            
            return canMove;
        }
        
        @Override
        public void setChildren(List<CollectionNode> newChildren) {
            removeAllChildren();
            
            for (CollectionNode node: newChildren) {
                add(node);
            }
        }
        
        public CollectionBoxService getService() {
            return service;
        }
    }
    
    static class DefaultCollectionBoxNode extends DefaultMutableTreeNode implements CollectionNode {
        private final CollectionBox box;
        
        DefaultCollectionBoxNode(CollectionBox box, boolean allowChildren) {
            super(box, allowChildren);
            this.box = box;
        }
        
        public DefaultCollectionBoxNode(CollectionBox box) {
            this(box, false);
        }
        
        @Override
        public CollectionBox getCollectionBox() {
            return box;
        }
        
        public Long getBoxId() {
            return box.getId();
        }
        
        @Override
        public boolean isContentsEditable() {
            return box instanceof FreeCollectionBox;
        }
        
        @Override
        public String toString() {
            if (box != null) {
                return box.getName();
            }
            return super.toString();
        }
    }

    static class LocalLibraryNode extends DefaultMutableTreeNode implements ExhibitCollectionNode {
        public LocalLibraryNode(Object userObject) {
            super(userObject, false);
        }
    }
    
    static class CollectionBoxTreeRootNode extends DefaultGroupCollectionNode implements CategoryNode {
        final int priority;

        public CollectionBoxTreeRootNode(CollectionBoxService service, int priority) {
            super(service.getGroupingDelegate(), service);
            
            this.priority = priority;
            
            setUserObject(service);
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

class SourceListModel<E> {
    final DefaultTreeModel delegate;

    public SourceListModel(DefaultTreeModel delegate) {
        this.delegate = delegate;
    }
    
    public SourceListModel() {
        this(new DefaultTreeModel(null));
    }
    
    public void addElement(E newElement) {
        
    }
    
//    int indexOfChild(E element) {
//        
//    }
//    
//    int[] indexPathToRoot(E newElement) {
//        
//    }
}
