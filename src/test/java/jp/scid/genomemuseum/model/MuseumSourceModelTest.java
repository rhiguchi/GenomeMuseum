package jp.scid.genomemuseum.model;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.List;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;

import jp.scid.genomemuseum.model.CollectionBox.BoxType;
import jp.scid.genomemuseum.model.CollectionBox.GroupCollectionBox;
import jp.scid.genomemuseum.model.MuseumSourceModel.CollectionBoxNode;
import jp.scid.genomemuseum.model.MuseumSourceModel.CollectionBoxTreeRootNode;

import org.junit.Before;
import org.junit.Test;

public class MuseumSourceModelTest {
    DefaultTreeModel treeModel;
    MuseumSourceModel model;
    CollectionBoxService mockService;
    
    @SuppressWarnings("unchecked")
    @Before
    public void setUp() throws Exception {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("root");
        treeModel = new DefaultTreeModel(root);
        model = new MuseumSourceModel(treeModel);
        
        mockService = mock(CollectionBoxService.class);
    }

    @Test
    public void hasLibrariesNode() {
        assertNotNull(model.getLibrariesNode());
        
        assertEquals("Libraries node on root", model.getLibrariesNode(),
                treeModel.getChild(treeModel.getRoot(), 0));
    }

    @Test
    public void collectionBoxTreeRootNode() {
        GroupCollectionBox delegate = mock(GroupCollectionBox.class);
        when(mockService.getGroupingDelegate()).thenReturn(delegate);
        
        CollectionBoxTreeRootNode node = new CollectionBoxTreeRootNode(mockService, 10);
        model.setCollectionBoxTreeRootNode(node);
        
        assertSame("node same", node, model.getCollectionBoxTreeRootNode());
        assertSame("node element", delegate, node.getCollectionBox());
        
        assertSame("insert second child on root",
                node, treeModel.getChild(treeModel.getRoot(), 1));
        
        model.setCollectionBoxTreeRootNode(null);
        
        assertNull("null set", model.getCollectionBoxTreeRootNode());
        
        assertEquals("remove from root",
                1, treeModel.getChildCount(treeModel.getRoot()));
    }
    
    @Test
    public void collectionBoxTreeModel() {
        model.setCollectionBoxTreeModel(mockService);
        
        assertSame("set same service", mockService, model.getCollectionBoxTreeModel());
        assertNotNull("set box tree node", model.getCollectionBoxTreeRootNode());
        
        model.setCollectionBoxTreeModel(null);
        
        assertNull("set null service", model.getCollectionBoxTreeModel());
        assertNull("remove box tree node", model.getCollectionBoxTreeRootNode());
    }
    
    
    @Test
    public void canMove() {
        CollectionBox newBox1 = mock(CollectionBox.class);
        when(newBox1.getId()).thenReturn(1L);
        
        CollectionBox newBox2 = mock(GroupCollectionBox.class);
        when(newBox2.getId()).thenReturn(2L);
        
        CollectionBox newBox3 = mock(GroupCollectionBox.class);
        when(newBox3.getId()).thenReturn(3L);
        when(newBox3.getParentId()).thenReturn(2L);
        
        
        assertTrue("can move to other parent", model.canMove(newBox1, newBox3));
        assertTrue("can move to same parent", model.canMove(newBox1, newBox2));
        assertFalse("cannot move to self", model.canMove(newBox2, newBox2));
        assertFalse("cannot move to leaf", model.canMove(newBox3, newBox1));
    }
    
    @Test
    public void moveCollectionBox() {
        CollectionBox newBox1 = createCollectionBox(1);
        CollectionBox newBox2 = createCollectionBox(2, BoxType.GROUP);
        when(mockService.newElement()).thenReturn(newBox1, newBox2);
        model.setCollectionBoxTreeModel(mockService);
        
        CollectionBoxNode node1 =
                model.addCollectionBox(BoxType.FREE, model.getCollectionBoxTreeRootNode());
        CollectionBoxNode node2 =
                model.addCollectionBox(BoxType.GROUP, model.getCollectionBoxTreeRootNode());
        
        // move to group node
        model.moveCollectionBox(node1, node2);
        
        assertSame("change parent", node1.getParent(), node2);
        assertEquals("change parent id", Long.valueOf(2), newBox1.getParentId());
        
        // move to root of collections
        model.moveCollectionBox(node1, model.getCollectionBoxTreeRootNode());
        
        assertSame("change parent", node1.getParent(), model.getCollectionBoxTreeRootNode());
        assertEquals("change parent id", null, newBox1.getParentId());
    }
    
    @Test
    public void reloadCollectionBoxNode() {
        CollectionBox box1 = createCollectionBox(1);
        CollectionBox box2 = createCollectionBox(2, BoxType.GROUP);
        CollectionBox box3 = createCollectionBox(3);
        List<CollectionBox> rootChildren = Arrays.asList(box1, box2);
        List<CollectionBox> groupChildren = Arrays.asList(box3);
        
        when(mockService.search("parent_id IS NULL")).thenReturn(rootChildren);
        when(mockService.search("parent_id = ?", 2L)).thenReturn(groupChildren);
        model.setCollectionBoxTreeModel(mockService);
        
        model.reloadCollectionBoxNode(model.getCollectionBoxTreeRootNode());
        assertEquals("reload children",
                2, model.getCollectionBoxTreeRootNode().getChildCount());
        TreeNode groupChild = model.getCollectionBoxTreeRootNode().getChildAt(1);
        
        model.reloadCollectionBoxNode((CollectionBoxNode) groupChild);
        assertEquals("reload children",
                1, groupChild.getChildCount());
    }
    
    @Test
    public void setLocalLibrarySource() {
        model.setLocalLibrarySource("lib source");
        
        assertSame("local lib node",
                model.getLocalLibraryNode(), model.getLibrariesNode().getChildAt(0));
        
        model.setLocalLibrarySource(null);
        
        assertEquals("remove lib source", 0, model.getLibrariesNode().getChildCount());
    }
    
    CollectionBox createCollectionBox(long id) {
        return createCollectionBox(id, BoxType.FREE);
    }
    
    CollectionBox createCollectionBox(long id, BoxType boxType) {
        CollectionBox box = CollectionBox.newCollectionBox(boxType, mockService); 
        box.setId(id);
        return box;
    }
}
