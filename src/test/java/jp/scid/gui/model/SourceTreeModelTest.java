package jp.scid.gui.model;

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.NoSuchElementException;

import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;

import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.GlazedLists;

public class SourceTreeModelTest {
    private SourceTreeModel<String> model = null;
    
    <T> SourceTreeModel<T> createModel() {
        return new SourceTreeModel<T>();
    }
    
    @Before
    public void setUp() throws Exception {
        model = createModel();
    }

    @Test
    public void getTreeSource() {
        assertNull("initial", model.getTreeSource());
    }
    
    @Test
    public void getRoot() {
        assertNull("initial", model.getRoot());
    }
    
    // setTreeSource
    @Test
    public void setTreeSource() {
        TreeSource<String> source = TreeSourceMock.of();
        model.setTreeSource(source);
        
        assertSame("source value", source, model.getTreeSource());
    }
    
    @Test
    public void setTreeSource_getRoot() {
        model.setTreeSource(TreeSourceMock.of("value"));
        assertEquals("update root", "value", model.getRoot());
        
        model.setTreeSource(TreeSourceMock.of("test"));
        assertEquals("update root", "test", model.getRoot());
    }
    
    @Test
    public void setTreeSource_propertyChangeListener() {
        TreeSource<String> source = TreeSourceMock.of();
        
        model.setTreeSource(source);
        
        verify(source).addPropertyChangeListener(model.getTreeSourceChangeListener());
        verify(source, never()).removePropertyChangeListener(model.getTreeSourceChangeListener());
        
        TreeSource<String> source2 = TreeSourceMock.of();
        
        model.setTreeSource(source2);
        
        verify(source).removePropertyChangeListener(model.getTreeSourceChangeListener());
        verify(source).addPropertyChangeListener(model.getTreeSourceChangeListener());
        verify(source2).addPropertyChangeListener(model.getTreeSourceChangeListener());
        
        model.setTreeSource(null);
        
        verify(source2).removePropertyChangeListener(model.getTreeSourceChangeListener());
    }

    // isLeaf
    @Test
    public void isLeaf() {
        TreeSource<String> source = TreeSourceMock.of("root",
                Arrays.asList("leaf", "non-leaf"));
        when(source.isLeaf("leaf")).thenReturn(true);
        when(source.isLeaf("non-leaf")).thenReturn(false);
        
        model.setTreeSource(source);
        model.getChildCount("root");
        assertTrue("leaf", model.isLeaf("leaf"));
        assertFalse("non-leaf", model.isLeaf("non-leaf"));
    }
    
    @Test
    public void isLeaf_exception() {
        try {
            model.isLeaf(null);
            fail();
        }
        catch (IllegalStateException expected) {
            assertTrue("no TreeSource", true);
        }
        
        model.setTreeSource(TreeSourceMock.<String>of());
        
        try {
            model.isLeaf(null);
            fail();
        }
        catch (NoSuchElementException expected) {
            assertTrue("null root", true);
        }
        
        model.setTreeSource(TreeSourceMock.of("root"));
        
        try {
            model.isLeaf("elm");
            fail();
        }
        catch (NoSuchElementException expected) {
            assertTrue("not opend element", true);
        }
    }
    
    // getChild
    @Test
    public void getChildCount() {
        TreeSource<String> source = TreeSourceMock.of("root", Arrays.asList("1", "2", "3"));

        model.setTreeSource(source);
        assertEquals("root value", 3, model.getChildCount("root"));
        // assert read only once
        verify(source).getChildren("root");
    }

    @Test
    public void getChildCount_fromLeaf() {
        TreeSource<String> source = TreeSourceMock.of("root", Arrays.asList("1", "2", "3"));
        when(source.isLeaf("root")).thenReturn(true);
        
        model.setTreeSource(source);
        assertEquals("root value", 0, model.getChildCount("root"));
        // never read
        verify(source, never()).getChildren("root");
    }
    
    @Test
    public void getChildCount_descendant() {
        TreeSource<String> source = TreeSourceMock.of("root", Arrays.asList("1", "2", "3"));
        TreeSourceMock.makeChildren(source, "1", Arrays.asList("a", "b"));
        TreeSourceMock.makeChildren(source, "3", Collections.<String>emptyList());
        
        model.setTreeSource(source);
        
        model.getChildCount("root");
        // assert not pre-read
        verify(source, never()).getChildren("1");
        
        assertEquals("child", 2, model.getChildCount("1"));
        assertEquals("child", 0, model.getChildCount("2"));
        assertEquals("child", 0, model.getChildCount("3"));
        // read only once
        verify(source).getChildren("1");
        verify(source).getChildren("2");
        verify(source).getChildren("3");
    }
    
    // getChild
    @Test
    public void getChild() {
        TreeSource<String> source = TreeSourceMock.of("root", Arrays.asList("1", "b", "3"));
        
        model.setTreeSource(source);
        assertEquals("root value", "1", model.getChild("root", 0));
        assertEquals("root value", "b", model.getChild("root", 1));
        assertEquals("root value", "3", model.getChild("root", 2));
        // assert read only once
        verify(source).getChildren("root");
    }
    
    @Test
    public void getChild_fromLeaf() {
        TreeSource<String> source = TreeSourceMock.of("root", Arrays.asList("1", "2", "3"));
        when(source.isLeaf("root")).thenReturn(true);
        
        model.setTreeSource(source);
        try {
            assertEquals("root value", 0, model.getChild("root", 0));
            fail();
        }
        catch (IndexOutOfBoundsException expected) {
            assertTrue("leaf child", true);
        }
    }
    
    @Test
    public void getChild_descendant() {
        TreeSource<String> source = TreeSourceMock.of("root", Arrays.asList("1", "2", "3"));
        TreeSourceMock.makeChildren(source, "1", Arrays.asList("a", "b"));
        
        model.setTreeSource(source);
        
        // assert not pre-read
        verify(source, never()).getChildren("1");
        
        assertEquals("child", "a", model.getChild(model.getChild("root", 0), 0));
        assertEquals("child", "b", model.getChild(model.getChild("root", 0), 1));
        // read only once
        verify(source).getChildren("1");
    }
    
    // With EventList
    @Test
    public void eventlist_getChildCount() {
        EventList<String> list = GlazedLists.eventListOf("1", "2", "3");
        TreeSource<String> source = TreeSourceMock.of("root", list);
        
        model.setTreeSource(source);
        
        assertEquals("initial", 3, model.getChildCount("root"));
        
        // add
        list.add("4");
        list.add("5");
        
        assertEquals("added", 5, model.getChildCount("root"));
        
        // remove
        list.remove("2");
        
        assertEquals("added", 4, model.getChildCount("root"));
    }
    
    @Test
    public void eventlist_getChild() {
        EventList<String> list = GlazedLists.eventListOf("1", "2", "3");
        TreeSource<String> source = TreeSourceMock.of("root", list);
        
        model.setTreeSource(source);
        
        assertEquals("initial 1", "1", model.getChild("root", 0));
        assertEquals("initial 2", "2", model.getChild("root", 1));
        assertEquals("initial 3", "3", model.getChild("root", 2));
        
        // add
        list.add(1, "4");
        list.add(4, "5");
        
        assertEquals("added", "4", model.getChild("root", 1));
        assertEquals("added", "5", model.getChild("root", 4));
        
        // remove
        list.remove(0);
        list.remove("3");
        
        assertEquals("added", "4", model.getChild("root", 0));
        assertEquals("added", "5", model.getChild("root", 2));
    }
    
    @Test
    public void eventlist_getIndexOfChild() {
        EventList<String> list = GlazedLists.eventListOf("1", "2", "3");
        TreeSource<String> source = TreeSourceMock.of("root", list);
        
        model.setTreeSource(source);
        
        assertEquals("initial 1", 0, model.getIndexOfChild("root", "1"));
        assertEquals("initial 2", 1, model.getIndexOfChild("root", "2"));
        assertEquals("initial 3", 2, model.getIndexOfChild("root", "3"));
        
        // add
        list.add(0, "4");
        list.add(2, "5");
        
        assertEquals("added", 0, model.getIndexOfChild("root", "4"));
        assertEquals("added", 2, model.getIndexOfChild("root", "5"));
        
        // remove
        list.remove(2);
        list.remove("4");
        
        assertEquals("added", 1, model.getIndexOfChild("root", "2"));
        assertEquals("added", 2, model.getIndexOfChild("root", "3"));
    }
    
    @Test
    public void eventlist_movePath() {
        EventList<String> list = GlazedLists.eventListOf("1", "2", "3");
        EventList<String> list2 = GlazedLists.eventListOf("4");
        TreeSource<String> source = TreeSourceMock.of("root", list);
        TreeSourceMock.makeChildren(source, "2", list2);
        
        model.setTreeSource(source);
        
        assertEquals("initial 1", 3, model.getChildCount("root"));
        assertEquals("initial 2", 1, model.getChildCount("2"));
        
        // add
        model.movePath(Arrays.asList("root", "3"),
                Arrays.asList("root", "2"));
        
        assertEquals("added", 2, model.getChildCount("root"));
        assertEquals("added", 2, model.getChildCount("2"));
        assertEquals("added", Arrays.asList("1", "2"), list);
        assertEquals("added", Arrays.asList("4", "3"), list2);
    }
    
    @Test
    public void eventlist_removePath() {
        EventList<String> list = GlazedLists.eventListOf("1", "2", "3");
        EventList<String> list2 = GlazedLists.eventListOf("4", "5");
        TreeSource<String> source = TreeSourceMock.of("root", list);
        TreeSourceMock.makeChildren(source, "2", list2);
        
        model.setTreeSource(source);
        
        assertEquals("initial 1", 3, model.getChildCount("root"));
        assertEquals("initial 2", 2, model.getChildCount("2"));
        
        // add
        model.removePath(Arrays.asList("root", "2", "4"));
        
        assertEquals("added", "5", model.getChild("2", 0));
        assertEquals("added", 1, model.getChildCount("2"));
        assertEquals("added", Arrays.asList("5"), list2);
    }
    
    
    // someChildrenInserted
    @Test
    public void event_remove() throws Exception {
        EventList<String> list = GlazedLists.eventListOf("1", "2", "3");
        EventList<String> list2 = GlazedLists.eventListOf("4", "5");
        TreeSource<String> source = TreeSourceMock.of("root", list);
        TreeSourceMock.makeChildren(source, "2", list2);
        
        model.setTreeSource(source);
        
        EventAnswer answer = new EventAnswer();
        
        TreeModelListener listener = mock(TreeModelListener.class);
        doAnswer(answer).when(listener).treeNodesRemoved(any(TreeModelEvent.class));
        model.addTreeModelListener(listener);
        
        assertEquals("initial 1", 3, model.getChildCount("root"));
        assertEquals("initial 2", 2, model.getChildCount("2"));
        
        list2.remove(1);
        
        assertEquals(model, answer.event.getSource());
        assertArrayEquals(new Object[]{"root", "2"}, answer.event.getPath());
        assertArrayEquals(new int[]{1}, answer.event.getChildIndices());
        assertArrayEquals(new Object[]{"5"}, answer.event.getChildren());
    }
    
    @Test
    public void event_insert() throws Exception {
        EventList<String> list = GlazedLists.eventListOf("1", "2", "3");
        EventList<String> list2 = GlazedLists.eventListOf("4");
        TreeSource<String> source = TreeSourceMock.of("root", list);
        TreeSourceMock.makeChildren(source, "2", list2);
        
        model.setTreeSource(source);
        
        EventAnswer answer = new EventAnswer();
        
        TreeModelListener listener = mock(TreeModelListener.class);
        doAnswer(answer).when(listener).treeNodesInserted(any(TreeModelEvent.class));
        model.addTreeModelListener(listener);
        
        assertEquals("initial 1", 3, model.getChildCount("root"));
        assertEquals("initial 2", 1, model.getChildCount("2"));
        
        list2.add(0, "5");
        
        assertEquals(model, answer.event.getSource());
        assertArrayEquals(new Object[]{"root", "2"}, answer.event.getPath());
        assertArrayEquals(new int[]{0}, answer.event.getChildIndices());
        assertArrayEquals(new Object[]{"5"}, answer.event.getChildren());
    }
    
    @Test
    public void event_update() throws Exception {
        EventList<String> list = GlazedLists.eventListOf("1", "2", "3");
        EventList<String> list2 = GlazedLists.eventListOf("4");
        TreeSource<String> source = TreeSourceMock.of("root", list);
        TreeSourceMock.makeChildren(source, "2", list2);
        
        model.setTreeSource(source);
        
        EventAnswer answer = new EventAnswer();
        
        TreeModelListener listener = mock(TreeModelListener.class);
        doAnswer(answer).when(listener).treeNodesChanged(any(TreeModelEvent.class));
        model.addTreeModelListener(listener);
        
        assertEquals("initial 1", 3, model.getChildCount("root"));
        assertEquals("initial 2", 1, model.getChildCount("2"));
        
        list.set(1, "2");
        
        assertEquals(model, answer.event.getSource());
        assertArrayEquals(new Object[]{"root"}, answer.event.getPath());
        assertArrayEquals(new int[]{1}, answer.event.getChildIndices());
        assertArrayEquals(new Object[]{"2"}, answer.event.getChildren());
    }
    
    static class EventAnswer implements Answer<TreeModelListener> {
        TreeModelEvent event = null;
        
        @Override
        public TreeModelListener answer(InvocationOnMock invocation) throws Throwable {
            event = (TreeModelEvent) invocation.getArguments()[0];
            return null;
        }
    }
}
