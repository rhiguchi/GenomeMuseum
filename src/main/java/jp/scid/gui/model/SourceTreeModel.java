package jp.scid.gui.model;

import static java.lang.String.*;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.WeakHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.event.TreeModelListener;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.FunctionList;
import ca.odell.glazedlists.FunctionList.AdvancedFunction;
import ca.odell.glazedlists.FunctionList.Function;
import ca.odell.glazedlists.GlazedLists;
import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;

public class SourceTreeModel<E> implements TreeModel {
    private final static Logger logger = Logger.getLogger(SourceTreeModel.class.getName());
    
    private final PropertyChangeListener changeListener = new PropertyChangeListener() {
        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            if ("value".equals(evt.getPropertyName())) {
                rootChanged();
            }
        }
    };
    
    private final DefaultTreeModel delegate = new EventDelegate(this);
    private TreeSource<E> treeSource = null;
    
    private E rootElement = null;
    private final WeakHashMap<E, Node<E>> treeNodeMap = new WeakHashMap<E, Node<E>>();

    public TreeSource<E> getTreeSource() {
        return treeSource;
    }
    
    public void setTreeSource(TreeSource<E> newTreeSource) {
        if (treeSource != null) {
            treeSource.removePropertyChangeListener(getTreeSourceChangeListener());
        }
        treeSource = newTreeSource;
        
        if (newTreeSource != null) {
            newTreeSource.addPropertyChangeListener(getTreeSourceChangeListener());
        }
        
        rootChanged();
    }

    void rootChanged() {
        clearAllTreeNodes();
        
        setRoot(treeSource == null ? null : treeSource.getValue());
    }

    Node<E> createNode(E sourceValue, Node<E> parent) {
        boolean isLeaf = treeSource.isLeaf(sourceValue);
        Node<E> childNode = new NodeImpl(sourceValue, isLeaf, parent);
        return childNode;
    }
    
    public boolean isRootTraceable(Object element) {
        return getNodeOrNull(element) != null;
    }
    
    void setRoot(E newRoot) {
        rootElement = newRoot;
        Node<E> rootNode = newRoot == null ? null : createNode(newRoot, null);
        
        if (rootNode != null) {
            treeNodeMap.put(newRoot, rootNode);
        }
        
        delegate.setRoot(rootNode);
    }
    
    public E getParent(Object element) throws NoSuchElementException {
        Node<E> parent = getNode(element).getParent();
        return parent != null ? parent.getUserObject() : null;
    }
  
    public List<E> getPathToRoot(Object element) throws NoSuchElementException {
        LinkedList<E> path = new LinkedList<E>();

        for (Node<E> node = getNode(element); node != null; node = node.getParent()) {
            path.addFirst(node.getUserObject());
        }

        return path;
    }

    void clearAllTreeNodes() {
        treeNodeMap.clear();
    }
    
//    void removeFromMap(SourceTreeNode<E> target) {
//        Queue<SourceTreeNode<E>> desc = new LinkedList<SourceTreeNode<E>>();
//        desc.add(target);
//        
//        while (!desc.isEmpty()) {
//            SourceTreeNode<E> node = desc.remove();
//            if (node.getChildren() != null) {
//                desc.addAll(node.getChildren());
//            }
//            
//            treeNodeMap.remove(node.getUserObject());
//        }
//    }

    PropertyChangeListener getTreeSourceChangeListener() {
        return changeListener;
    }
    
    Node<E> getNodeOrNull(Object element) {
        return treeNodeMap.get(element);
    }
    
    Node<E> getNode(Object element) {
        // ensureTreeSourceIsNotNull
        if (treeSource == null)
            throw new IllegalStateException("need a treeSource");
        
        Node<E> node = getNodeOrNull(element);
        if (node == null)
            throw new NoSuchElementException(format(
                    "'%s' cannot trace to parent." +
                    " Try getRoot() or getChild[Count]() from parent before access.", element));
        return node;
    }
    
    // TreeModel implementation
    @Override
    public E getRoot() {
        logger.log(Level.FINE, "getRoot(): {0}", rootElement);
        
        return rootElement;
    }
    
    @Override
    public boolean isLeaf(Object element) throws NoSuchElementException {
        logger.log(Level.FINE, "isLeaf({0}): ", element);
        
        return getNode(element).isLeaf();
    }

    @Override
    public E getChild(Object element, int index) throws NoSuchElementException {
        logger.log(Level.FINE, "getChild({0}, {1}): ", new Object[]{element, index});
        
        return getNode(element).getChildAt(index).getUserObject();
    }

    @Override
    public int getChildCount(Object element) {
        logger.log(Level.FINE, "getChildCount({0}): ", element);
        
        return getNode(element).getChildCount();
    }

    @Override
    public void valueForPathChanged(TreePath path, Object newValue) {
        logger.log(Level.FINE,
                "valueForPathChanged({0}, {1}): ", new Object[]{path, newValue});
        
        Object element = path.getLastPathComponent();
        Node<E> node = getNode(element);
        
        getTreeSource().updateNodeValue(node.getUserObject(), newValue);
        delegate.nodeChanged(node);
    }

    @Override
    public int getIndexOfChild(Object parent, Object child) {
        logger.log(Level.FINE,
                "getIndexOfChild({0}, {1}): ", new Object[]{parent, child});
        
        Node<E> node = getNode(parent);
        return node.getIndexOfChildElement(child);
    }

    @Override
    public void addTreeModelListener(TreeModelListener l) {
        delegate.addTreeModelListener(l);
    }

    @Override
    public void removeTreeModelListener(TreeModelListener l) {
        delegate.removeTreeModelListener(l);
    }

    // node manipulations
    public void movePath(List<E> sourcePath, List<E> destParent) {
        List<Node<E>> sourceNodePath = getNodePath(sourcePath);
        List<Node<E>> destNodePath = getNodePath(destParent);
        
        if (sourceNodePath.get(0) != destNodePath.get(0)) {
            throw new IllegalArgumentException(
                    format("Paths source '%s' and dest '%s' must be same root", 
                           sourcePath, destParent));
        }
        
        if (Collections.indexOfSubList(destNodePath, sourceNodePath) != -1) {
            throw new IllegalArgumentException(
                    format("Source '%s' cannot move to child '%s'", 
                            sourcePath, destParent));
        }
        
        Node<E> sourceNode = sourceNodePath.get(sourceNodePath.size() - 1);
        Node<E> sourceParent = sourceNode.getParent();
        Node<E> destNode = destNodePath.get(destNodePath.size() - 1);
        
        destNode.add(sourceNode);
        sourceParent.remove(sourceNode);
    }

    List<Node<E>> getNodePath(List<E> elementPath) {
        List<Node<E>> sourceNodePath = new ArrayList<SourceTreeModel.Node<E>>(elementPath.size());
        
        Node<E> root = getNode(elementPath.get(0));
        sourceNodePath.add(root);
        
        for (E child: elementPath.subList(1, elementPath.size())) {
            Node<E> parentNode = sourceNodePath.get(sourceNodePath.size() - 1);
            
            int childIndex = parentNode.getIndexOfChildElement(child);
            Node<E> childNode = parentNode.getChildAt(childIndex);            
            sourceNodePath.add(childNode);
        }
        return sourceNodePath;
    }
    
    public void removePath(List<? super E> path) {
        Object parent = path.get(0);
        
        for (int depth = 1; depth < path.size(); depth++) {
            Object child = path.get(depth);
            int childIndex = getIndexOfChild(parent, child);
            
            if (depth < path.size() - 1) {
                parent = child;
            }
            else {
                getNode(parent).remove(childIndex);
            }
        }
    }
    
    public void removePath(int[] indexPath) {
        Node<E> parent = getNode(getRoot());
        
        for (int depth = 1; depth < indexPath.length; depth++) {
            int childIndex = indexPath[depth];
            
            if (depth < indexPath.length - 1) {
                parent = parent.getChildAt(childIndex);
            }
            else {
                parent.remove(childIndex);
            }
        }
    }
    
    DefaultTreeModel getDelegate() {
        return delegate;
    }

    class ChildNodeMaker implements AdvancedFunction<E, Node<E>> {
        private final Node<E> parent;
        
        public ChildNodeMaker(Node<E> parent) {
            this.parent = parent;
        }

        @Override
        public Node<E> evaluate(E sourceValue) {
            Node<E> node = getNodeOrNull(sourceValue);
            if (node == null) {
                node = createNode(sourceValue, parent);
                treeNodeMap.put(sourceValue, node);
            }
            return node;
        }

        @Override
        public Node<E> reevaluate(E sourceValue, Node<E> transformedValue) {
            treeNodeMap.remove(sourceValue);
            return evaluate(sourceValue);
        }

        @Override
        public void dispose(E sourceValue, Node<E> transformedValue) {
            if (transformedValue.getParent() == parent)
                transformedValue.setParent(null);
        }
    }
    
    static class NodeObjectMaker<E> implements Function<Node<E>, E> {
        @Override
        public E evaluate(Node<E> sourceValue) {
            return sourceValue.getUserObject();
        }
    }
    
    EventList<Node<E>> createChildNodeList(EventList<E> sourceChildList, Node<E> parent) {
        ChildNodeMaker nodeMaker = new ChildNodeMaker(parent);
        return new FunctionList<E, Node<E>>(sourceChildList, nodeMaker, new NodeObjectMaker<E>());
    }
    
    EventList<E> getChildSourceList(E userObject) {
        List<E> children = treeSource.getChildren(userObject);
        
        final EventList<E> childEventList;
        if (children instanceof EventList) {
            childEventList = (EventList<E>) children;
        }
        else {
            childEventList = GlazedLists.eventList(children);
        }
        
        return childEventList;
    }
    
    class NodeImpl implements Node<E>, ListEventListener<Node<E>> {
        private final E userObject;
        private final boolean isLeaf;
        private Node<E> parent;
        
        EventList<Node<E>> childNodeList = null;
        List<Node<E>> viewChildNodeList = null;
        
        NodeImpl(E userObject, boolean isLeaf, Node<E> parent) {
            this.userObject = userObject;
            this.isLeaf = isLeaf;
            this.parent = parent;
        }
        
        public E getUserObject() {
            return userObject;
        }
        
        @Override
        public Node<E> getParent() {
            return parent;
        }
        
        public void setParent(Node<E> newParent) {
            this.parent = newParent;
        }
        
        @Override
        public boolean isLeaf() {
            return isLeaf;
        }
        
        EventList<Node<E>> getChildNodeList() {
            if (childNodeList == null) {
                EventList<E> sourceList = getChildSourceList(userObject);
                childNodeList = createChildNodeList(sourceList, this);
            }
            return childNodeList;
        }
        
        List<Node<E>> getChildren() {
            if (isLeaf()) {
                return Collections.emptyList();
            }
            
            if (viewChildNodeList == null) {
                EventList<Node<E>> eventList = getChildNodeList();
                
                viewChildNodeList = new ArrayList<Node<E>>(eventList);
                
                ListEventListener<Node<E>> listener = GlazedLists.weakReferenceProxy(eventList, this);
                eventList.addListEventListener(listener);
            }
            
            return viewChildNodeList;
        }
        
        @Override
        public boolean getAllowsChildren() {
            return !isLeaf;
        }
        
        @Override
        public Node<E> getChildAt(int childIndex) {
            return getChildren().get(childIndex);
        }
        
        @Override
        public int getChildCount() {
            return getChildren().size();
        }
        
        @Override
        public int getIndex(TreeNode node) {
            return getChildren().indexOf(node);
        }
        
        public int getIndexOfChildElement(Object element) {
            int index = 0;
            for (Iterator<Node<E>> ite = getChildren().iterator(); ite.hasNext(); index++) {
                Node<E> node = ite.next();
                if (node.getUserObject().equals(element)) {
                    return index;
                }
            }
            
            return -1;
        }
        
        @Override
        public Enumeration<Node<E>> children() {
            return Collections.enumeration(getChildren());
        }
        
        public void add(Node<E> child) {
            getChildNodeList().add(child);
        }
        
        public void remove(Node<E> child) {
            getChildNodeList().remove(child);
        }
        
        public void remove(int index) {
            getChildNodeList().remove(index);
        }
        
        @Override
        public void listChanged(ListEvent<Node<E>> listChanges) {
            while (listChanges.nextBlock()) {
                int start = listChanges.getBlockStartIndex();
                int end = listChanges.getBlockEndIndex();
                int[] indice = createIndice(start, end);
                
                int type = listChanges.getType();
                
                if (type == ListEvent.INSERT) {
                    childInserted(indice);
                }
                else if (type == ListEvent.UPDATE) {
                    childUpdated(indice);
                }
                else if (type == ListEvent.DELETE) {
                    childDeleted(indice);
                }
            }
        }

        void childInserted(int[] indice) {
            for (int i = 0; i < indice.length; i++) {
                int index = indice[i];
                Node<E> newChild = childNodeList.get(index);
                viewChildNodeList.add(index, newChild);
            }
            fireTreeModeInserted(this, indice);
        }

        void childUpdated(int[] indice) {
            for (int i = 0; i < indice.length; i++) {
                int index = indice[i];
                Node<E> changed = childNodeList.get(index);
                viewChildNodeList.set(index, changed);
            }
            fireTreeModeUpdated(this, indice);
        }

        void childDeleted(int[] indice) {
            List<Node<E>> removedList = new ArrayList<Node<E>>(indice.length);
            
            for (int i = 0; i < indice.length; i++) {
                int index = indice[i];
                Node<E> removed = viewChildNodeList.remove(index);
                removedList.add(removed);
            }
            
            fireTreeModeRemoved(this, indice, removedList.toArray());
        }
        
        int[] createIndice(int start, int end) {
            int[] indice = new int[end - start + 1];
            for (int i = 0; i < indice.length; i++) {
                indice[i] = start + i;
            }
            return indice;
        }
        
        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + getOuterType().hashCode();
            result = prime * result + ((userObject == null) ? 0 : userObject.hashCode());
            return result;
        }

        @Override
        public String toString() {
            return "Node [element=" + userObject + ", isLeaf=" + isLeaf + "]";
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null) return false;
            if (getClass() != obj.getClass()) return false;
            @SuppressWarnings("unchecked")
            NodeImpl other = (NodeImpl) obj;
            if (!getOuterType().equals(other.getOuterType())) return false;
            if (userObject == null) {
                if (other.userObject != null) return false;
            }
            else if (!userObject.equals(other.userObject)) return false;
            return true;
        }

        private SourceTreeModel<?> getOuterType() {
            return SourceTreeModel.this;
        }
    }
    
    protected void fireTreeModeInserted(Node<?> node, int[] indice) {
        getDelegate().nodesWereInserted(node, indice);
    }
    
    protected void fireTreeModeUpdated(Node<?> node, int[] indice) {
        getDelegate().nodesChanged(node, indice);
    }
    
    protected void fireTreeModeRemoved(Node<?> node, int[] indice, Object[] removed) {
        getDelegate().nodesWereRemoved(node, indice, removed);
    }
    

    static class EventDelegate extends DefaultTreeModel {
        private final SourceTreeModel<?> source;
        public EventDelegate(SourceTreeModel<?> source) {
            super(null, true);
            this.source = source;
        }
        
        @Override
        protected void fireTreeNodesChanged(
                Object source, Object[] path, int[] childIndices, Object[] children) {
            super.fireTreeNodesChanged(
                    this.source, convertToElementPath(path), childIndices,
                    convertToElementPath(children));
        }
        
        @Override
        protected void fireTreeNodesInserted(
                Object source, Object[] path, int[] childIndices, Object[] children) {
            super.fireTreeNodesInserted(
                    this.source, convertToElementPath(path), childIndices,
                    convertToElementPath(children));
        }
        
        @Override
        protected void fireTreeNodesRemoved(
                Object source, Object[] path, int[] childIndices, Object[] children) {
            super.fireTreeNodesRemoved(
                    this.source, convertToElementPath(path), childIndices,
                    convertToElementPath(children));
        }
        
        @Override
        protected void fireTreeStructureChanged(
                Object source, Object[] path, int[] childIndices, Object[] children) {
            super.fireTreeStructureChanged(
                    this.source, convertToElementPath(path), childIndices,
                    convertToElementPath(children));
        }

        Object[] convertToElementPath(Object[] path) {
            if (path == null || path.length == 0)
                return new Object[0];
            
            Object[] elementPath = new Object[path.length];
            
            for (int i = 0; i < path.length; i++) {
                Node<?> realElement = (Node<?>) path[i];
                elementPath[i] = realElement.getUserObject();
            }
            return elementPath;
        }
    }
    
    static interface Node<E> extends TreeNode {
        Node<E> getChildAt(int childIndex);
        
        Node<E> getParent();
        
        void setParent(Node<E> newParent);
        
        void add(Node<E> child);
        
        void remove(Node<E> child);
        
        void remove(int index);
        
        
        int getIndexOfChildElement(Object element);
        
        E getUserObject();
    }
}
