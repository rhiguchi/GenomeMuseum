package jp.scid.genomemuseum.model;

import static java.lang.String.*;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.swing.event.ChangeListener;

import jp.scid.bio.store.SequenceLibrary;
import jp.scid.bio.store.base.ChangeEventSupport;
import jp.scid.bio.store.folder.Folder;
import jp.scid.bio.store.folder.FoldersContainer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.explodingpixels.widgets.TextProvider;

public class MuseumTreeSource implements NodeListTreeModel.TreeSource {
    private final static Logger logger = LoggerFactory.getLogger(MuseumTreeSource.class);
    private final RootItemList rootItemList;
    
    public MuseumTreeSource() {
        rootItemList = new RootItemList();
    }
    
    @Override
    public boolean getAllowsChildren(Object obj) {
        logger.debug("TreeSource#getAllowsChildren: {}", obj);
        
        if (obj instanceof Category) {
            return true;
        }
        else if (obj instanceof FoldersContainer) {
            return true;
        }
        return false;
    }

    @Override
    public List<?> getChildren(Object parent) {
        logger.debug("TreeSource#getChildren: {}", parent);
        
        if (parent == this) {
            return rootItemList.getItems();
        }
        else if (parent instanceof Category) {
            return ((Category) parent).children();
        }
        else if (parent instanceof FoldersContainer) {
            return ((FoldersContainer) parent).getChildFolders();
        }
        return null;
    }
    
    public void addChildrenChangeListener(Object parent, ChangeListener l) {
        if (parent == this) {
            rootItemList.addChangeListener(l);
        }
        else if (parent instanceof FoldersContainer) {
            ((FoldersContainer) parent).addFoldersChangeListener(l);
        }
        else if (parent instanceof Category) {
            ((Category) parent).addChangeListener(l);
        }
    }
    
    public void removeChildrenChangeListener(Object parent, ChangeListener l) {
        if (parent instanceof FoldersContainer) {
            ((FoldersContainer) parent).removeFoldersChangeListener(l);
        }
        else if (parent instanceof RootItemList) {
            ((RootItemList) parent).removeChangeListener(l);
        }
        else if (parent instanceof Category) {
            ((Category) parent).removeChangeListener(l);
        }
    }
    
    public boolean updateValueForPath(Object[] path, Object value) {
        if (value instanceof String) {
            Object node = path[path.length - 1];
            if (node instanceof Folder) {
                Folder folder = (Folder) node;
                folder.setName((String) value);
                return folder.save();
            }
        }
        
        return false;
    }
    
    @Override
    public String toString() {
        return "TreeSource (root)";
    }
    
    public Object getParent(Object node) {
        if (node instanceof Folder) {
            return null; // TODO ((Folder) node).getParent();
        }
        else if (node instanceof Category) {
            return this;
        }
        else if (node == this) {
            return null;
        }
        else if (node.equals(getUserCollectionsRoot())) {
            return this;
        }
        
        throw new IllegalArgumentException(format("cannot find the parent of %s", node));
    }

    public int[] getIndexPath(Object lastElement) {
        List<Object> pathToRoot = getPathToRoot(lastElement);
        int[] indexPath = new int[pathToRoot.size() - 1];
        
        Iterator<Object> it = pathToRoot.iterator();
        Object parent = it.next();
        int depth = 0;
        for (; it.hasNext(); depth++) {
            Object target = it.next();
            List<?> children = getChildren(parent);
            
            int childIndex = children.indexOf(target);
            if (childIndex == -1) {
                throw new IllegalStateException(format("cannot find child %s in parent %s", target, parent));
            }
            indexPath[depth] = childIndex;
            
            parent = target;
        }
        return indexPath;
    }

    public List<Object> getPathToRoot(Object lastElement) {
        List<Object> path = new LinkedList<Object>();
        getPathToRoot(path, lastElement);
        path.add(lastElement);
        return path;
    }
    
    private void getPathToRoot(List<Object> path, Object element) {
        Object parent = getParent(element);
        if (parent != null) {
            path.add(0, parent);
            getPathToRoot(path, parent);
        }
    }
    
    public FoldersContainer getUserCollectionsRoot() {
        return rootItemList.userCollectionsRoot;
    }
    
    public void setSequenceLibrary(SequenceLibrary sequenceLibrary) {
        rootItemList.setLocalFilesSource(sequenceLibrary);
        rootItemList.setUserCollectionsRoot(sequenceLibrary.getUsersFolderRoot());
    }
    
    public void setNcbiSource(Object source) {
        rootItemList.setNcbiSource(source);
    }

    private static class RootItemList {
        private static final String DEFAULT_LIBRARIES_NAME = "Libraries";
        
        @SuppressWarnings("unused")
        private static final String DEFAULT_LOCAL_FILES_NAME = "Local Files";
        
        private final Category librariesNode;
        private FoldersContainer userCollectionsRoot;
        
        private SequenceLibrary localFilesSourceNode;
        
        private Object ncbiSource;
        
        private final ChangableList delegate;
        
        RootItemList() {
            delegate = new ChangableList(this);
            librariesNode = new Category(DEFAULT_LIBRARIES_NAME, 0);
            delegate.add(librariesNode);
        }
        
        public void setLocalFilesSource(SequenceLibrary source) {
            if (localFilesSourceNode != null) {
                librariesNode.removeChild(localFilesSourceNode);
            }
            
            localFilesSourceNode = source;
            if (source != null) {
                librariesNode.addChild(localFilesSourceNode);
            }
        }
        
        public void setNcbiSource(Object newNcbiSource) {
            if (ncbiSource != null) {
                librariesNode.removeChild(ncbiSource);
            }
            
            ncbiSource = newNcbiSource;
            if (newNcbiSource != null) {
                librariesNode.addChild(newNcbiSource);
            }
        }
        
        public void setUserCollectionsRoot(FoldersContainer source) {
            if (this.userCollectionsRoot != null) {
                delegate.remove(this.userCollectionsRoot);
            }
            
            this.userCollectionsRoot = source;
            
            if (source != null) {
                delegate.add(userCollectionsRoot);
            }
        }
        
        public List<Object> getItems() {
            return delegate.getElements();
        }

        public void addChangeListener(ChangeListener listener) {
            delegate.addChangeListener(listener);
        }

        public void removeChangeListener(ChangeListener listener) {
            delegate.removeChangeListener(listener);
        }
    }
    
    private static abstract class TreeElement implements TextProvider {
        private final String name;
        private Object parent;
        
        public TreeElement(String name) {
            this.name = name;
        }
        
        @Override
        public String getText() {
            return name;
        }
        
        @Override
        public String toString() {
            return getText();
        }

        public Object getParent() {
            return parent;
        }

        public void setParent(Object parent) {
            this.parent = parent;
        }
    }
    
    public static class Category extends TreeElement implements TextProvider, Comparable<Category> {
        private final ChangableList delegate;
        private final int order;
        
        Category(String name, int order) {
            super(name);
            
            delegate = new ChangableList(this);
            this.order = order;
        }
        
        @Override
        public int compareTo(Category o) {
            if (this.order < o.order) {
                return -1;
            }
            else if (this.order > o.order) {
                return 1;
            }
            
            return this.getText().compareTo(o.getText());
        }
        
        void addChild(Object object) {
            delegate.add(object);
        }

        void removeChild(Object element) {
            delegate.remove(element);
        }
        
        public List<Object> children() {
            return delegate.getElements();
        }

        public void addChangeListener(ChangeListener listener) {
            delegate.addChangeListener(listener);
        }

        public void removeChangeListener(ChangeListener listener) {
            delegate.removeChangeListener(listener);
        }
    }
    
    private static class ChangableList {
        private final ChangeEventSupport ces;
        private final List<Object> elements;
        
        public ChangableList(Object eventSource) {
            this.ces = new ChangeEventSupport(eventSource);
            elements = new ArrayList<Object>();
        }
        
        public List<Object> getElements() {
            return elements;
        }

        public void addChangeListener(ChangeListener listener) {
            ces.addChangeListener(listener);
        }

        public void removeChangeListener(ChangeListener listener) {
            ces.removeChangeListener(listener);
        }

        public void fireChildrenChange() {
            ces.fireStateChange();
        }

        public boolean add(Object e) {
            try {
                return elements.add(e);
            }
            finally {
                fireChildrenChange();
            }
        }

        public boolean remove(Object o) {
            try {
                return elements.remove(o);
            }
            finally {
                fireChildrenChange();
            }
        }
    }
}

