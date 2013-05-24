package jp.scid.genomemuseum.model;

import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractListModel;
import javax.swing.DefaultListModel;
import javax.swing.ListModel;

import jp.scid.bio.store.SequenceLibrary;
import jp.scid.bio.store.folder.CollectionType;
import jp.scid.bio.store.folder.GroupFolder;
import jp.scid.genomemuseum.model.MuseumTreeSource.FolderContainer;
import jp.scid.genomemuseum.model.MuseumTreeSource.FolderTreeNode;

import com.explodingpixels.widgets.TextProvider;

public class MuseumTreeSource implements NodeListTreeModel.TreeSource {
    private final RootItemList rootItemList;
    
    private SequenceLibrary sequenceLibrary;
    
    public MuseumTreeSource() {
        rootItemList = new RootItemList();
    }
    
    @Override
    public boolean getAllowsChildren(Object obj) {
        if (obj instanceof Category) {
            return true;
        }
        else if (obj instanceof SequenceLibrary || obj instanceof GroupFolder) {
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
            if (parent == rootItemList.userCollections) {
                return sequenceLibrary.getRootFolderList();
            }
            return ((Category) parent).children();
        }
        else if (parent instanceof SequenceLibrary) {
            return ((SequenceLibrary) parent).getRootFolderList();
        }
        else if (parent instanceof GroupFolder) {
            return ((GroupFolder) parent).getChildFolders();
        }
        return null;
    }
    
    public void setSequenceLibrary(SequenceLibrary sequenceLibrary) {
        rootItemList.setLocalFilesSource(sequenceLibrary);
        rootItemList.setUserCollectionsRoot(sequenceLibrary);
        this.sequenceLibrary = sequenceLibrary;
    }
    
    public UserCollectionsRoot getUserCollectionRoot() {
        return rootItemList.userCollections; // TODO
    }
    
    public int[] getIndexPath(Object object) {
        return null; // TODO
    }

    private static class RootItemList extends AbstractListModel {
        private static final String DEFAULT_LIBRARIES_NAME = "Libraries";
        
        private static final String DEFAULT_LOCAL_FILES_NAME = "Local Files";
        
        private final Category librariesNode;
        private UserCollectionsRoot userCollections;
        
        private LeafElement localFilesSourceNode;
        
        private List<Object> items = new ArrayList<Object>();
        
        RootItemList() {
            librariesNode = new Category(DEFAULT_LIBRARIES_NAME, 0);
            items.add(librariesNode);
        }
        
        public void setLocalFilesSource(Object source) {
            if (localFilesSourceNode != null) {
                librariesNode.removeChild(localFilesSourceNode);
                localFilesSourceNode = null;
            }
            
            if (source != null) {
                localFilesSourceNode = new LeafElement(DEFAULT_LOCAL_FILES_NAME, source);
                librariesNode.addChild(localFilesSourceNode);
            }
        }
        
        public void setUserCollectionsRoot(Object source) {
            if (userCollections != null) {
                items.remove(userCollections);
                userCollections = null;
            }
            
            if (source != null) {
                userCollections = new UserCollectionsRoot();
                items.add(userCollections);
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
    
    public static class UserCollectionsRoot implements TextProvider, FolderContainer {
        private static final String DEFAULT_COLLECTIONS_NAME = "Collections";
        
        UserCollectionsRoot() {
            // TODO Auto-generated constructor stub
        }
        
        @Override
        public String getText() {
            return DEFAULT_COLLECTIONS_NAME;
        }

        @Override
        public FolderTreeNode addChild(CollectionType type) {
            // TODO Auto-generated method stub
            return null;
        }
        
        @Override
        public boolean canMove(FolderTreeNode node) {
            // TODO Auto-generated method stub
            return false;
        }
    }
    
    private static abstract class TreeElement implements TextProvider {
        private final String name;
        
        public TreeElement(String name) {
            this.name = name;
        }
        
        @Override
        public String getText() {
            return name;
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

    public static class LeafElement extends TreeElement {
        private final Object nodeObject;
        
        LeafElement(String name, Object nodeObject) {
            super(name);
            this.nodeObject = nodeObject;
        }
        
        LeafElement(String name) {
            this(name, null);
        }
    }
    
    public interface FolderContainer {

        FolderTreeNode addChild(CollectionType type);
        
        boolean canMove(FolderTreeNode node);
    }
    
    public interface FolderTreeNode {

        FolderContainer getParentContainer();

        void remove();
        
    }
    

    public interface SequenceImportable {

        FolderTreeNode addChild(CollectionType type);
        
    }

    public FolderTreeNode addFolder(CollectionType type, FolderTreeNode parent) {
        // TODO Auto-generated method stub
        return null;
    }
}

