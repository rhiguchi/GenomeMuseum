package jp.scid.genomemuseum.model;

import static jp.scid.genomemuseum.model.sql.Tables.*;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.swing.event.ListDataListener;

import jp.scid.genomemuseum.model.CollectionBox.BoxType;
import jp.scid.genomemuseum.model.sql.tables.records.BoxTreeNodeRecord;
import jp.scid.genomemuseum.model.tree.DefaultSourceTreeModel;
import jp.scid.genomemuseum.model.tree.SourceTreeModel;

import org.jooq.Condition;
import org.jooq.RecordHandler;
import org.jooq.impl.Factory;

public class CollectionBoxService implements SourceTreeModel<CollectionBox> {
    private final Factory factory;
    private final DefaultSourceTreeModel<CollectionBox> treeModelDelegate;
    
    CollectionBoxService(Factory factory) {
        this.factory = factory;
        treeModelDelegate = new DefaultSourceTreeModel<CollectionBox>();
    }
    
    public List<CollectionBox> getChildren(CollectionBox parent) {
        Condition condition;
        if (parent == null) {
            condition = BOX_TREE_NODE.PARENT_ID.isNull();
        }
        else {
            condition = BOX_TREE_NODE.PARENT_ID.equal(parent.getId());
        }
        
        List<CollectionBox> results = factory.selectFrom(BOX_TREE_NODE)
                .where(condition).fetchInto(new CollectionBoxListBuilder()).build();
        
        return results;
    }
    
    static class CollectionBoxListBuilder implements RecordHandler<BoxTreeNodeRecord> {
        List<CollectionBox> list = new LinkedList<CollectionBox>();
        
        @Override
        public void next(BoxTreeNodeRecord record) {
            CollectionBox box = new CollectionBox(record);
            list.add(box);
        }
        
        public List<CollectionBox> build() {
            return new ArrayList<CollectionBox>(list);
        }
    }
    
    boolean getAllowedChildren(CollectionBox element) {
        return element.getBoxType() == BoxType.GROUP;
    }
    
    public boolean insert(CollectionBox element, CollectionBox parent) {
        if (parent != null && parent.getId() == null) {
            throw new IllegalArgumentException("id of parent must not be empty");
        }
        
        element.setParentId(parent == null ? null : parent.getId());
        
        return insert(element);
    }
    
    public boolean insert(CollectionBox element) {
        boolean sotred = element.getRecord().store() > 0;
        if (sotred) {
            
        }
        return sotred;
    }
    
    public CollectionBox createBox(BoxType type) {
        if (type == null) throw new IllegalArgumentException("type must not be null");
        
        CollectionBox box = createBox();
        box.setBoxType(type);
        return box;
    }
    
    CollectionBox createBox() {
        BoxTreeNodeRecord node = factory.newRecord(BOX_TREE_NODE);
        
        return new CollectionBox(node);
    }
    
    public boolean update(CollectionBox element) {
        if (element.getId() == null)
            throw new IllegalArgumentException("id of element is not spcified");
        BoxTreeNodeRecord record = new BoxTreeNodeRecord();
        record.from(element);
        int count = factory.executeUpdate(BOX_TREE_NODE, record);
        return count > 0;
    }
    
    public boolean delete(CollectionBox element) {
        int count = factory.executeDelete(BOX_TREE_NODE, BOX_TREE_NODE.ID.equal(element.getId()));
        return count > 0;
    }

    public boolean isLeaf(CollectionBox element) {
        return element.getBoxType() != BoxType.GROUP;
    }

    public void addChildrenListener(CollectionBox parent, ListDataListener l) {
        treeModelDelegate.addChildrenListener(parent, l);
    }

    public void removeChildrenListener(CollectionBox parent, ListDataListener l) {
        treeModelDelegate.removeChildrenListener(parent, l);
    }

    public boolean setParent(CollectionBox element, CollectionBox parent) {
        // TODO Auto-generated method stub
        return false;
    }
}
