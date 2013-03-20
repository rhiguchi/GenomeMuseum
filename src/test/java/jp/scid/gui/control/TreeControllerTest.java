package jp.scid.gui.control;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import javax.swing.JTree;

import jp.scid.gui.model.TreeSource;

import org.junit.Before;
import org.junit.Test;

public class TreeControllerTest {
    TreeController<String, TreeSource<String>> ctrl = null;
    
    <T> TreeController<T, TreeSource<T>> createController() {
        return new TreeController<T, TreeSource<T>>();
    }
    
    @Before
    public void setUp() throws Exception {
        ctrl = createController();
    }
    
    @Test
    public void getModel() {
        assertNull("model", ctrl.getModel());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void setModel() {
        TreeSource<String> model = mock(TreeSource.class);
        
        ctrl.setModel(model);
        
        assertSame("model", model, ctrl.getModel());
        assertSame("set model to treeModel", model, ctrl.getTreeSource());
    }

    @Test
    public void getSelectedNodes() throws Exception {
        assertEquals("size", 0, ctrl.getSelectedPathList().size());
    }
    
    @Test
    public void bindTree() {
        JTree tree = mock(JTree.class);
        
        ctrl.bindTree(tree);
        
        verify(tree).setModel(ctrl.getTreeModel());
        verify(tree).setSelectionModel(ctrl.getTreeSelectionModel());
    }
}
