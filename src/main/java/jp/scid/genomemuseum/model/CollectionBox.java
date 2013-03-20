package jp.scid.genomemuseum.model;

import static jp.scid.genomemuseum.model.sql.Tables.*;

import java.util.Collections;
import java.util.List;

import javax.swing.event.ChangeListener;

import jp.scid.genomemuseum.model.sql.tables.records.BoxTreeNodeRecord;

public abstract class CollectionBox {
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
    
    @Deprecated
    CollectionBoxService service;
    
    private BoxType boxType;

    private CollectionBox(BoxTreeNodeRecord record) {
        if (record == null) throw new IllegalArgumentException("record must not be null");
        
        this.record = record;
    }
    
    static CollectionBox newCollectionBox(BoxTreeNodeRecord record) {
        if (record == null) throw new IllegalArgumentException("record must not be null");
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
        
        return box;
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
    
    private BoxType getBoxType() {
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
    
    public boolean delete() {
        int count = getRecord().delete();
        return count > 0;
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
    
    public static class FreeCollectionBox extends CollectionBox {
        private FreeCollectionBox(BoxTreeNodeRecord record) {
            super(record);
        }
        
        public List<MuseumExhibit> fetchExhibits() {
            return service.fetchContent(getId());
        }
        
        public MuseumExhibit addContent(long exhibitId) {
            return service.addContentTo(getId(), exhibitId);
        }
    }
    
    public static class GroupCollectionBox extends CollectionBox {
        private GroupCollectionBox(BoxTreeNodeRecord record) {
            super(record);
        }

        public List<MuseumExhibit> fetchExhibits() {
            return Collections.emptyList();
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
