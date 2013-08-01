package jp.scid.genomemuseum.model;

import static java.lang.String.*;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.swing.ListModel;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

import jp.scid.bio.store.SequenceLibrary;
import jp.scid.bio.store.folder.CollectionType;
import jp.scid.bio.store.folder.Folder;
import jp.scid.bio.store.folder.FoldersContainer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.GlazedLists;
import ca.odell.glazedlists.swing.DefaultEventListModel;

import com.explodingpixels.widgets.TextProvider;

public class MuseumTreeSource implements NodeListTreeModel.TreeSource {
    private final static Logger logger = LoggerFactory.getLogger(MuseumTreeSource.class);
    private static final String DEFAULT_LIBRARIES_NAME = "Libraries";
    private static final String DEFAULT_LOCAL_FILES_NAME = "Collections";
    
    private final Category rootItemList;
    private final Category librariesNode;
    private FoldersRoot foldersRoot = null;
    
    private final Map<Long, EventFolderList> folderContainer;
    
    private SequenceLibrary localLibrary = null;
    private Object ncbiSource = null;
    
    public MuseumTreeSource() {
        librariesNode = new Category(DEFAULT_LIBRARIES_NAME);
        
        rootItemList = new Category("root");
        rootItemList.add(librariesNode);
        
        folderContainer = new HashMap<Long, EventFolderList>();
    }
    
    @Override
    public boolean getAllowsChildren(Object obj) {
        logger.debug("TreeSource#getAllowsChildren: {}", obj);
        
        if (obj instanceof FoldersContainer) {
            return true;
        }
        else if (obj instanceof ListModel) {
            return true;
        }
        return false;
    }

    @Override
    public List<?> getChildren(Object parent) {
        logger.debug("TreeSource#getChildren: {}", parent);
        
        if (parent instanceof Folder) {
            return getOrCreateChildFolders((Folder) parent);
        }
        else if (parent instanceof EventFolderList) {
            return ((EventFolderList) parent).elements();
        }
        else if (parent instanceof Category) {
            return ((Category) parent).elements();
        }
        else if (parent == this) {
            return rootItemList.elements();
        }
        else {
            throw new IllegalArgumentException("unknwon parent: " + parent);
        }
    }
    
    public void addChildrenChangeListener(Object parent, ListDataListener l) {
        if (parent instanceof Folder) {
            long id = ((Folder) parent).id();
            getFolderEventDelegate(id).addListDataListener(l);
        }
        else if (parent instanceof ListModel) {
            ((ListModel) parent).addListDataListener(l);
        }
        else if (parent == this) {
            rootItemList.addListDataListener(l);
        }
        else {
            throw new IllegalArgumentException("unknwon parent: " + parent);
        }
    }

    public void removeChildrenChangeListener(Object parent, ListDataListener l) {
        if (parent instanceof Folder) {
            long id = ((Folder) parent).id();
            ListModel model = getFolderEventDelegate(id);
            if (model != null) {
                model.removeListDataListener(l);
            }
        }
        else if (parent instanceof ListModel) {
            ((ListModel) parent).removeListDataListener(l);
        }
        else if (parent == this) {
            rootItemList.removeListDataListener(l);
        }
        else {
            throw new IllegalArgumentException("unknwon parent: " + parent);
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
    
    private List<Folder> getOrCreateChildFolders(Folder folder) {
        Long id = folder.id();
        EventFolderList list = folderContainer.get(id);
        if (list == null) {
            EventList<Folder> eventList = GlazedLists.eventList(localLibrary.getChildFolders(id));
            list = new EventListModelAdapter(eventList, folder);
            folderContainer.put(id, list);
        }
        return list.elements();
    }
    
    private EventFolderList getFolderList(Long parentId) {
        if (parentId == null) {
            return foldersRoot;
        }
        return folderContainer.get(parentId);
    }

    private List<Folder> getChildList(Long parentId) {
        return getFolderList(parentId).elements();
    }
    
    private ListModel getFolderEventDelegate(long id) {
        return folderContainer.get(id);
    }

    public Folder createFolder(CollectionType type, Long parentId) {
        Folder folder = localLibrary.createFolder(type, parentId);
        addFolder(parentId, folder);
        return folder;
    }

    private void addFolder(Long parentId, Folder folder) {
        List<Folder> source = getChildList(parentId);
        source.add(folder);
    }

    public void removeFolder(Folder folder) {
        List<Folder> source = getChildList(folder.parentId());
        source.remove(folder);
        folderContainer.remove(folder.id());
        // TODO recursive
    }

    public boolean changeParent(Folder folder, Long newParentId) {
        if (folder.parentId() == null && newParentId == null
                || folder.parentId().equals(newParentId)) {
            return false;
        }
        List<Folder> source = getChildList(folder.parentId());
        source.remove(folder);
        
        folder.setParentId(newParentId);
        List<Folder> parentChildren = getChildList(newParentId);
        parentChildren.add(folder);
        
        return true;
    }
    
    @Override
    public String toString() {
        return "TreeSource (root)";
    }
    
    public Object getParent(Object node) {
        if (node instanceof Folder) {
            Long parentId = ((Folder) node).parentId();
            return getFolderList(parentId).owner();
        }
        // categories
        else if (node instanceof Category || node instanceof FoldersRoot) {
            return this;
        }
        // libraries
        else if (node != null && (node == localLibrary || node == ncbiSource)) {
            return librariesNode;
        }
        // root
        else if (node == this) {
            return null;
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
    
    public void setSequenceLibrary(SequenceLibrary sequenceLibrary) {
        if (localLibrary != null) {
            rootItemList.remove(foldersRoot);
            librariesNode.remove(localLibrary);
        }
        
        localLibrary = sequenceLibrary;
        
        if (sequenceLibrary != null) {
            librariesNode.add(localLibrary);
            
            EventList<Folder> source = GlazedLists.eventList(sequenceLibrary.getChildFolders(null));
            foldersRoot = new FoldersRoot(source);
            rootItemList.add(foldersRoot);
        }
    }
    
    public void setNcbiSource(Object newNcbiSource) {
        if (ncbiSource != null) {
            librariesNode.remove(ncbiSource);
        }
        
        ncbiSource = newNcbiSource;
        
        if (newNcbiSource != null) {
            librariesNode.add(newNcbiSource);
        }
    }

    
    private static class Category extends DefaultEventListModel<Object> implements TextProvider {
        private final String name;
        
        public Category(String name) {
            super(new BasicEventList<Object>());
            this.name = name;
        }
        
        public boolean add(Object element) {
            return source.add(element);
        }

        public boolean remove(Object element) {
            return source.remove(element);
        }
        
        public List<?> elements() {
            return source;
        }
        
        @Override
        public String getText() {
            return name;
        }
        
        @Override
        public String toString() {
            return getText();
        }
    }
    
    private static interface EventFolderList extends ListModel {
        List<Folder> elements();
        
        Object owner();
    }
    
    private static class FoldersRoot extends EventListModelAdapter {
        public FoldersRoot(EventList<Folder> source) {
            super(source, null);
        }
        
        @Override
        public String toString() {
            return DEFAULT_LOCAL_FILES_NAME;
        }
        
        @Override
        public Object owner() {
            return this;
        }
    }
    
    private static class EventListModelAdapter extends DefaultEventListModel<Folder> implements EventFolderList {
        private final Object owner;
        
        EventListModelAdapter(EventList<Folder> source, Object owner) {
            super(source);
            this.owner = owner;
        }

        public EventList<Folder> elements() {
            return source;
        }
        
        public Object owner() {
            return owner;
        }
        
        @Override
        protected void fireListDataEvent(ListDataEvent original) {
            ListDataEvent proxy = new ListDataEvent(
                    owner(), original.getType(), original.getIndex0(), original.getIndex1());
            super.fireListDataEvent(proxy);
        }
    }
}

