package jp.scid.genomemuseum.model;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.DefaultMutableTreeNode;

import org.junit.Before;
import org.junit.Test;

public class NodeListTreeModelTest {
    NodeListTreeModel model;
    
    @Before
    public void setUp() throws Exception {
        model = new NodeListTreeModel();
    }

    @Test
    public void getRoot() {
        TestTreeSource treeSource = new TestTreeSource();
        model.setTreeSource(treeSource);
        
        assertThat(model.getRoot(), instanceOf(DefaultMutableTreeNode.class));
        assertEquals(treeSource, ((DefaultMutableTreeNode) model.getRoot()).getUserObject());
        
        model.setTreeSource(null);
        assertEquals(null, model.getRoot());
    }
    
    @Test
    public void reload() {
        model.setTreeSource(new TestTreeSource());
        
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) model.getRoot();
        model.reload(root);
        
        assertEquals(3, root.getChildCount());
        assertEquals("element1", userObjectOf(root.getChildAt(0)));
        assertEquals("element3", userObjectOf(root.getChildAt(2)));
    }
    
    @Test
    public void getChild() {
        model.setTreeSource(new TestTreeSource());
        
        Object root = model.getRoot();
        
        assertEquals(userObjectOf(model.getChild(root, 0)), "element1");
        assertEquals(userObjectOf(model.getChild(root, 1)), "element2");
        
        Object element1 = model.getChild(root, 0);
        assertEquals(userObjectOf(model.getChild(element1, 0)), "elementA");
        assertEquals(userObjectOf(model.getChild(element1, 1)), "elementB");
    }
    
    @Test
    public void getChildCount() {
        model.setTreeSource(new TestTreeSource());
        
        Object root = model.getRoot();
        assertEquals(3, model.getChildCount(root));
        
        Object element1 = model.getChild(root, 0);
        assertEquals(2, model.getChildCount(element1));
        
        assertEquals(0, model.getChildCount(model.getChild(root, 1)));
    }
    
    @Test
    public void getIndexOfChild() {
        model.setTreeSource(new TestTreeSource());

        Object root = model.getRoot();
        Object element1 = model.getChild(root, 0);
        Object element2 = model.getChild(root, 1);
        Object elementB = model.getChild(element1, 1);
        
        assertEquals(0, model.getIndexOfChild(root, element1));
        assertEquals(1, model.getIndexOfChild(root, element2));
        assertEquals(1, model.getIndexOfChild(element1, elementB));
        
        assertEquals(-1, model.getIndexOfChild(root, elementB));
    }
    
    @Test
    public void isLeaf() {
        model.setTreeSource(new TestTreeSource());

        Object root = model.getRoot();
        assertFalse(model.isLeaf(root));
        
        assertTrue(model.isLeaf(model.getChild(root, 1)));
    }
    
    @Test
    public void listener() {
        TestEventHandler handler = new TestEventHandler();
        model.addTreeModelListener(handler);
        model.setTreeSource(new TestTreeSource());
        
        assertEquals(model, handler.firedEvents.element().getSource());
        assertEquals(model, handler.firedEvents.element().getSource());
    }
    
    @Test
    public void insert() {
        TestTreeSource treeSource = new TestTreeSource();
        model.setTreeSource(treeSource);
        Object root = model.getRoot();
        assertEquals(3, model.getChildCount(root));
        
        treeSource.updateRootElements();
        
        assertEquals(2, model.getChildCount(root));
        assertEquals(userObjectOf(model.getChild(root, 0)), "element2");
        assertEquals(userObjectOf(model.getChild(root, 1)), "element4");
    }
    
    static class TestEventHandler implements TreeModelListener {
        LinkedList<TreeModelEvent> firedEvents = new LinkedList<TreeModelEvent>();
        
        public void treeStructureChanged(TreeModelEvent e) {
            firedEvents.add(e);
        }
        
        public void treeNodesRemoved(TreeModelEvent e) {
            firedEvents.add(e);
        }
        
        public void treeNodesInserted(TreeModelEvent e) {
            firedEvents.add(e);
        }
        
        public void treeNodesChanged(TreeModelEvent e) {
            firedEvents.add(e);
        }
    }
    
    static Object userObjectOf(Object treeNode) {
        return ((DefaultMutableTreeNode) treeNode).getUserObject();
    }
    
    static class TestTreeSource implements NodeListTreeModel.TreeSource {
        private List<?> rootElements = Arrays.asList("element1", "element2", "element3");
        private List<ChangeListener> rootChildrenChangeListeners = new ArrayList<ChangeListener>();
        
        public boolean getAllowsChildren(Object nodeObject) {
            if ("element2".equals(nodeObject)) {
                return false;
            }
            return true;
        }

        public List<?> getChildren(Object parent) {
            if (parent == this) {
                return rootElements;
            }
            else if (parent.equals("element1")) {
                return Arrays.asList("elementA", "elementB");
            }
            
            return Collections.emptyList();
        }
        
        public void updateRootElements() {
            rootElements = Arrays.asList("element2", "element4");
            
            ChangeEvent e = new ChangeEvent(this);
            for (ChangeListener l: rootChildrenChangeListeners) {
                l.stateChanged(e);
            }
        }

        public void addChildrenChangeListener(Object parent, ChangeListener l) {
            rootChildrenChangeListeners.add(l);
        }

        public void removeChildrenChangeListener(Object parent, ChangeListener l) {
            // do nothing
        }
    }
}
