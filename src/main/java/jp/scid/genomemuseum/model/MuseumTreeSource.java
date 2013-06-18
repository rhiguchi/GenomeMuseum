package jp.scid.genomemuseum.model;

import static java.lang.String.*;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.swing.AbstractListModel;
import javax.swing.DefaultListModel;
import javax.swing.ListModel;

import jp.scid.bio.store.SequenceLibrary;
import jp.scid.bio.store.folder.Folder;
import jp.scid.bio.store.folder.FoldersContainer;

import com.explodingpixels.widgets.TextProvider;

public class MuseumTreeSource implements NodeListTreeModel.TreeSource {
    private final RootItemList rootItemList;
    
    public MuseumTreeSource() {
        rootItemList = new RootItemList();
    }
    
    @Override
    public boolean getAllowsChildren(Object obj) {
        if (obj instanceof Category) {
            return true;
        }
        else if (obj instanceof FoldersContainer) {
            return true;
        }
        return false;
    }

    @Override
    public ListModel getChildren(Object parent) {
        if (parent == this) {
            return rootItemList;
        }
        else if (parent instanceof Category) {
            return ((Category) parent).children();
        }
        else if (parent instanceof FoldersContainer) {
            return ((FoldersContainer) parent).getContentFolders();
        }
        return null;
    }
    
    public Object getParent(Object node) {
        if (node instanceof Folder) {
            return ((Folder) node).getParent();
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
            ListModel children = getChildren(parent);
            
            int childIndex = indexOfChild(target, children);
            if (childIndex == -1) {
                throw new IllegalStateException(format("cannot find child %s in parent %s", target, parent));
            }
            indexPath[depth] = childIndex;
            
            parent = target;
        }
        return indexPath;
    }

    private int indexOfChild(Object target, ListModel children) {
        for (int i = children.getSize() - 1; i >= 0; i--) {
            Object child = children.getElementAt(i);
            if (target.equals(child)) {
                return i;
            }
        }
        return -1;
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

    private static class RootItemList extends AbstractListModel {
        private static final String DEFAULT_LIBRARIES_NAME = "Libraries";
        
        @SuppressWarnings("unused")
        private static final String DEFAULT_LOCAL_FILES_NAME = "Local Files";
        
        private final Category librariesNode;
        private FoldersContainer userCollectionsRoot;
        
        private SequenceLibrary localFilesSourceNode;
        
        private Object ncbiSource;
        
        private List<Object> items = new ArrayList<Object>();
        
        RootItemList() {
            librariesNode = new Category(DEFAULT_LIBRARIES_NAME, 0);
            items.add(librariesNode);
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
                items.remove(this.userCollectionsRoot);
            }
            
            this.userCollectionsRoot = source;
            
            if (source != null) {
                items.add(userCollectionsRoot);
            }
        }
        
        @Override
        public int getSize() {
            return items.size();
        }

        @Override
        public Object getElementAt(int index) {
            return items.get(index);
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

        public Object getParent() {
            return parent;
        }

        public void setParent(Object parent) {
            this.parent = parent;
        }
    }
    
    public static class Category extends TreeElement implements TextProvider, Comparable<Category> {
        private final DefaultListModel children;
        private final int order;
        
        Category(String name, int order) {
            super(name);
            this.order = order;
            children = new DefaultListModel();
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
            children.addElement(object);
        }

        void removeChild(Object element) {
            children.removeElement(element);
        }
        
        public ListModel children() {
            return children;
        }
    }
}

