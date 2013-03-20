package jp.scid.gui.model;

import static org.mockito.Mockito.*;

import java.util.List;

public class TreeSourceMock {
    private TreeSourceMock() {
    }
    
    @SuppressWarnings("unchecked")
    public static <T> TreeSource<T> of() {
        TreeSource<T> source = mock(TreeSource.class);
        
        return source;
    }
    
    public static <T> TreeSource<T> of(T rootObject) {
        TreeSource<T> source = of();
        when(source.getValue()).thenReturn(rootObject);
        when(source.isLeaf(rootObject)).thenReturn(true);
        return source;
    }
    
    public static <T> TreeSource<T> of(T rootObject, List<T> rootChildren) {
        TreeSource<T> source = of();
        when(source.getValue()).thenReturn(rootObject);
        makeChildren(source, rootObject, rootChildren);
        
        return source;
    }
    
    public static <T> TreeSource<T> makeChildren(TreeSource<T> source, T node, List<T> rootChildren) {
        when(source.isLeaf(node)).thenReturn(false);
        when(source.getChildren(node)).thenReturn(rootChildren);
        return source;
    }
}
