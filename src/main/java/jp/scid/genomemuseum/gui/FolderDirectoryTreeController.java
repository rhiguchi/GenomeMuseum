package jp.scid.genomemuseum.gui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import javax.swing.Action;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreePath;

import jp.scid.bio.store.FolderDirectory;
import jp.scid.bio.store.SequenceLibrary;
import jp.scid.bio.store.SequenceLibrary.FolderType;
import jp.scid.bio.store.SequenceLibrary.FolderTypeConverter;
import jp.scid.bio.store.jooq.Tables;
import jp.scid.bio.store.jooq.tables.records.FolderRecord;
import jp.scid.gui.control.ActionManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.explodingpixels.widgets.TextProvider;

public class FolderDirectoryTreeController extends TreeController<FolderDirectory> {
    private final static Logger logger = LoggerFactory.getLogger(FolderDirectoryTreeController.class);
    
    final Action nodeFolderAddAction;
    final Action collectionFolderAddAction;
    final Action filterFolderAddAction;
    
    public FolderDirectoryTreeController() {
        ActionManager actionManager = new ActionManager(this);
        nodeFolderAddAction = actionManager.getAction("addNodeFolder");
        collectionFolderAddAction = actionManager.getAction("addCollectionFolder");
        filterFolderAddAction = actionManager.getAction("addFilterFolder");
    }
    
    public void addCollectionFolder() {
        TreePath path = addFolder(FolderType.COLLECTION);
        startEditingAtPath(path);
    }
    
    public void addNodeFolder() {
        TreePath path = addFolder(FolderType.NODE);
        startEditingAtPath(path);
    }
    
    public void addFilterFolder() {
        TreePath path = addFolder(FolderType.FILTER);
        startEditingAtPath(path);
    }
    
    private GenomeMuseumTreeSource treeSource() {
        return (GenomeMuseumTreeSource) getTreeSource();
    }
    
    public TreePath addFolder(FolderType type) {
        GenomeMuseumTreeSource treeSource = treeSource();
        FolderRecord newFolder = treeSource.createFolder(type);
        DefaultMutableTreeNode parent = getInsertParent();
        
        TreePath path = add(newFolder, parent);
        
        Long parentId;
        if (parent.getUserObject() instanceof FolderRecord) {
            parentId = ((FolderRecord) parent.getUserObject()).getId();
        }
        else {
            parentId = null;
        }
        
        newFolder.setParentId(parentId);
        treeSource.add(newFolder);
        
        return path;
    }
    
    protected DefaultMutableTreeNode getInsertParent() {
        DefaultMutableTreeNode node = getSelectedTreeNode();
        if (node != null && node.getUserObject() instanceof FolderRecord) {
            return node;
        }
        
        return getFoldersRoot(); 
    }
    
    protected DefaultMutableTreeNode getFoldersRoot() {
        GenomeMuseumTreeSource treeSource = treeSource();
        int[] indices = treeSource.getIndexPathForFoldersRoot();
        TreePath path = getTreePath(indices);
        return (DefaultMutableTreeNode) path.getLastPathComponent(); 
    }
    
//    private FolderRecord getSelectedFolder() {
//        Object userObject = getSelection();
//        if (userObject instanceof FolderRecord) {
//            return (FolderRecord) userObject;
//        }
//        return null;
//    }
    
    @Override
    public void remove(MutableTreeNode node) {
        if (((DefaultMutableTreeNode) node).getUserObject() instanceof FolderRecord) {
            super.remove(node);
            
            FolderRecord folder = (FolderRecord) ((DefaultMutableTreeNode) node).getUserObject();
            treeSource().removeFolder(folder.getId().longValue());
        }
        
        logger.warn("remove node {} is not allowed", node);
    }
    
    public static class GenomeMuseumTreeSource implements TreeSource {
        private final static Logger logger = LoggerFactory.getLogger(GenomeMuseumTreeSource.class);
        
        private final SequenceLibrary sequenceLibrary;
        
        private final Category librariesNode;
        private final Category userCollectionsNode;
        
        private final List<Category> rootElements;
        
        public GenomeMuseumTreeSource(SequenceLibrary sequenceLibrary) {
            this.sequenceLibrary = sequenceLibrary;
            
            librariesNode = new Category("Libraries");
            librariesNode.addChild(sequenceLibrary);
            
            userCollectionsNode = new Category("Collections");
            
            rootElements = new LinkedList<Category>();
            Collections.addAll(rootElements, librariesNode, userCollectionsNode);
            
        }

        public void add(FolderRecord newFolder) {
            sequenceLibrary.insertFolder(newFolder);
        }

        public void removeFolder(long folderId) {
            sequenceLibrary.deleteFolder(folderId);
        }

        public FolderRecord createFolder(FolderType type) {
            return sequenceLibrary.createFolder(type);
        }

        public int[] getIndexPathForFoldersRoot() {
            int index = rootElements.indexOf(userCollectionsNode);
            return new int[]{index};
        }
        
        @Override
        public List<?> children(Object parent) {
            if (parent == null) {
                return rootElements();
            }
            else if (parent == userCollectionsNode) {
                return getFolders(null);
            }
            else if (parent instanceof FolderRecord) {
                return getFolders(((FolderRecord) parent).getId());
            }
            else if (parent instanceof Category) {
                return ((Category) parent).children();
            }
            
            logger.warn("unknown parent node {}", parent);
            return Collections.emptyList();
        }

        protected List<FolderRecord> getFolders(Long parentId) {
            return sequenceLibrary.getFolders(parentId);
        }

        public List<?> rootElements() {
            return rootElements;
        }

        @Override
        public boolean isLeaf(Object node) {
            if (node instanceof TreeElement) {
                return ((TreeElement) node).isLeaf();
            }
            else if (node instanceof FolderRecord) {
                FolderRecord record = (FolderRecord) node;
                switch (record.getValue(Tables.FOLDER.TYPE, FolderTypeConverter.getInstance())) {
                case NODE:
                    return false;
                default:
                    return true;
                }
            }
            else if (node instanceof SequenceLibrary) {
                return true;
            }
            
            logger.warn("unknown node {}", node);
            return true;
        }
        
        public static abstract class TreeElement implements TextProvider {
            private final String name;

            public TreeElement(String name) {
                this.name = name;
            }
            
            public String name() {
                return name;
            }
            
            public abstract boolean isLeaf();
            
            @Override
            public String getText() {
                return name;
            }
        }
        
        public static class Category extends TreeElement {
            private final List<Object> childList;
            public Category(String name) {
                super(name);
                childList = new ArrayList<Object>();
            }
            
            @Override
            public final boolean isLeaf() {
                return false;
            }
            
            public void addChild(Object object) {
                childList.add(object);
            }
            
            public List<?> children() {
                return Collections.unmodifiableList(childList);
            }
        }
        
        public static class LeafElement extends TreeElement {
            public LeafElement(String name) {
                super(name);
            }
            
            @Override
            public final boolean isLeaf() {
                return true;
            }
        }
    }
    
    static class Binding {
        FolderDirectoryTreeController ctrl;
        
        public Binding(FolderDirectoryTreeController controller) {
            super();
            this.ctrl = controller;
        }

        public void bindTree(JTree tree) {
            tree.setRootVisible(false);
            tree.setModel(ctrl.getTreeModel());
            tree.setSelectionModel(ctrl.getSelectionModel());
            
            ctrl.updateExpansion(tree);
            tree.addTreeWillExpandListener(ctrl);

            tree.setTransferHandler(ctrl.getTransferHandler());
        }
    }
}
