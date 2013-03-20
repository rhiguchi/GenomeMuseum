package jp.scid.genomemuseum.gui;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeNode;

import jp.scid.bio.store.FolderDirectory;
import jp.scid.bio.store.SequenceLibrary;
import jp.scid.bio.store.SequenceLibrary.FolderType;
import jp.scid.bio.store.SequenceLibrary.FolderTypeConverter;
import jp.scid.bio.store.jooq.Tables;
import jp.scid.bio.store.jooq.tables.records.FolderRecord;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FolderDirectoryTreeController extends TreeController<FolderDirectory> {
    private final static Logger logger = LoggerFactory.getLogger(FolderDirectoryTreeController.class);
    
    private GenomeMuseumTreeSource treeSource = null;
    
    public FolderDirectoryTreeController() {
    }
    
    public void addCollectionElement() {
        TreeNode node = addBox(FolderType.COLLECTION);
        editNode(node);
    }
    
    public void addNodeElement() {
        TreeNode node = addBox(FolderType.COLLECTION);
        editNode(node);
    }
    
    public TreeNode addBox(FolderType type) {
        FolderRecord newFolder = treeSource.createFolder(type);
        TreeNode newNode = add(newFolder);
        
        FolderRecord parent = getSelectedFolder();
        treeSource.insertFolder(newFolder, parent);
        
        return newNode;
    }
    
    private FolderRecord getSelectedFolder() {
        Object userObject = getSelection();
        if (userObject instanceof FolderRecord) {
            return (FolderRecord) userObject;
        }
        return null;
    }
    
    @Override
    public void remove(MutableTreeNode node) {
        if (((DefaultMutableTreeNode) node).getUserObject() instanceof FolderRecord) {
            super.remove(node);
            
            FolderRecord folder = (FolderRecord) ((DefaultMutableTreeNode) node).getUserObject();
            treeSource.removeFolder(folder.getId().longValue());
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
            userCollectionsNode = new Category("Collections");
            
            rootElements = new LinkedList<Category>();
            Collections.addAll(rootElements, librariesNode, userCollectionsNode);
            
        }

        public void removeFolder(long folderId) {
            Long parentId = sequenceLibrary.getParentId(folderId);
            sequenceLibrary.deleteFolder(folderId);
            
            // TODO fire node removed
        }

        public void insertFolder(FolderRecord newFolder, FolderRecord parent) {
            Long parentId = parent == null ? null : parent.getId();
            sequenceLibrary.insertFolderInto(newFolder, parentId);
            
            // TODO fire node inserted
        }

        public FolderRecord createFolder(FolderType type) {
            return sequenceLibrary.createFolder(type);
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
            
            logger.warn("unknown node {}", node);
            return true;
        }
        
        public static abstract class TreeElement {
            private final String name;

            public TreeElement(String name) {
                this.name = name;
            }
            
            public String name() {
                return name;
            }
            
            public abstract boolean isLeaf();
        }
        
        public static class Category extends TreeElement {
            public Category(String name) {
                super(name);
            }
            
            @Override
            public final boolean isLeaf() {
                return false;
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
