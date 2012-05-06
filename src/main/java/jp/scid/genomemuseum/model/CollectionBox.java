package jp.scid.genomemuseum.model;

import static jp.scid.genomemuseum.model.sql.Tables.*;

import java.util.Collections;
import java.util.List;

import javax.swing.event.ChangeListener;

import jp.scid.genomemuseum.model.sql.tables.records.BoxTreeNodeRecord;

public abstract class CollectionBox implements ExhibitListModel {
    public static enum BoxType {
        GROUP(0),
        FREE(1),
        SMART(2);
        
        private final int intValue;
        
        private BoxType(int intValue) {
            this.intValue = intValue;
        }
        
        int getIntValue() {
            return intValue;
        }
        
        static BoxType findFrom(int intValue) {
            for (BoxType t: values()) {
                if (t.intValue == intValue) return t;
            }
            return null;
        }
    }

    private final BoxTreeNodeRecord record;
    
    CollectionBoxService service;
    
    private BoxType boxType;

    private CollectionBox(BoxTreeNodeRecord record) {
        if (record == null) throw new IllegalArgumentException("record must not be null");
        
        this.record = record;
    }
    
    static CollectionBox newCollectionBox(BoxTreeNodeRecord record, CollectionBoxService service) {
        if (record == null) throw new IllegalArgumentException("record must not be null");
        if (service == null) throw new IllegalArgumentException("service must not be null");
        
        BoxType boxType =
            BoxType.findFrom(record.getValueAsInteger(
                    BOX_TREE_NODE.NODE_TYPE, BoxType.GROUP.getIntValue()));

        final CollectionBox box;
        if (BoxType.FREE == boxType) {
            box = new FreeCollectionBox(record);
        }
        else if (BoxType.GROUP == boxType) {
            box = new GroupCollectionBox(record);
        }
        else if (BoxType.SMART == boxType) {
            box = new SmartCollectionBox(record);
        }
        else {
            throw new IllegalArgumentException("nodeType must be specified");
        }
        
        box.service = service;
        
        return box;
    }
    
    static CollectionBox newCollectionBox(BoxType boxType, CollectionBoxService service) {
        if (boxType == null) throw new IllegalArgumentException("boxType must not be null");
        
        BoxTreeNodeRecord record = service.newRecord(boxType);
        
        return newCollectionBox(record, service);
    }
    
    BoxTreeNodeRecord getRecord() {
        return record;
    }
    
    public Long getId() {
        return record.getId();
    }
    
    void setId(long newId) {
        record.setId(newId);
    }
    
    @Deprecated
    public BoxType getBoxType() {
        if (boxType == null) {
            if (record.getNodeType() != null) {
                boxType = BoxType.findFrom(record.getNodeType());
            }
            
            if (boxType == null) {
                boxType = BoxType.FREE;
            }
        }
        return boxType;
    }

    void setParentId(Long value) {
        record.setParentId(value);
    }

    Long getParentId() {
        return record.getParentId();
    }

    public void setName(String value) {
        record.setName(value);
    }

    public String getName() {
        return record.getName();
    }
    
    boolean isPersited() {
        return getId() != null;
    }
    
    @Override
    public boolean storeExhibit(MuseumExhibit exhibit) {
        boolean result = exhibit.store();
        return result;
    }
    
    public int setParent(GroupCollectionBox newParent) {
        if (getId() == null || newParent.isAncestorOf(getId())) {
            throw new IllegalArgumentException("cannot set parent to " + newParent);
        }
        
        service.setParent(this, newParent.getId());
        
        List<CollectionBox> children = newParent.fetchChildren();
        int index = children.indexOf(this);
        if (index < 0)
            index = children.size();
        
        return index;
    }
    
    public boolean delete() {
        int count = getRecord().delete();
        return count > 0;
    }
    
    @Override
    public void addExhibitsChangeListener(ChangeListener listener) {
        // TODO Auto-generated method stub
        
    }
    
    @Override
    public void removeExhibitsChangeListener(ChangeListener listener) {
        // TODO Auto-generated method stub
        
    }
    
    @Override
    public String toString() {
        return getName() + " [" + getBoxType() + "]";
    }

    @Override
    public boolean equals(Object obj) {
        Long myId = getId();
        if (myId != null && obj instanceof CollectionBox) {
            Long targetId = ((CollectionBox) obj).getId();
            return myId.equals(targetId);
        }
        else {
            return super.equals(obj);
        }
    }
    
    public static class FreeCollectionBox extends CollectionBox implements FreeExhibitCollectionModel {
        private FreeCollectionBox(BoxTreeNodeRecord record) {
            super(record);
        }
        
        public List<MuseumExhibit> fetchExhibits() {
            return service.fetchContent(getId());
        }
        
        public MuseumExhibit addContent(long exhibitId) {
            return service.addContentTo(getId(), exhibitId);
        }
        
        public void setContentIndex(MuseumExhibit content, int newIndex) {
            // TODO
        }

        public boolean removeContent(long contentId) {
            return service.removeContent(contentId);
        }

        @Override
        public boolean deleteExhibit(MuseumExhibit e) {
            if (e.hasRowId()) {
                return removeContent(e.getRowId());
            }
            else {
                return false;
            }
        }

        @Override
        public void addExhibit(int index, MuseumExhibit e) {
            // TODO Auto-generated method stub
            addContent(e.getId());
        }
    }
    
    public static class GroupCollectionBox extends CollectionBox {
        private GroupCollectionBox(BoxTreeNodeRecord record) {
            super(record);
        }

        public List<MuseumExhibit> fetchExhibits() {
            return Collections.emptyList();
        }
        
        public List<CollectionBox> fetchChildren() {
            return service.fetchChildren(getId());
        }
        
        public CollectionBox addChild(BoxType boxType) {
            return service.addChild(boxType, getId());
        }
        
        public boolean isAncestorOf(long boxId) {
            Long myId = getId();
            if (myId == null) {
                return true;
            }
            else {
                return service.isAncestor(boxId, myId.longValue());
            }
        }
    }
    
    public static class SmartCollectionBox extends CollectionBox {
        private SmartCollectionBox(BoxTreeNodeRecord record) {
            super(record);
        }

        public List<MuseumExhibit> fetchExhibits() {
            return Collections.emptyList();
        }
    }
}
