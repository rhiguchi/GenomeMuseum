package jp.scid.genomemuseum.model;

import jp.scid.genomemuseum.model.sql.tables.records.BoxTreeNodeRecord;

public class CollectionBox {
    public static enum BoxType {
        FREE(1),
        GROUP(2),
        SMART(3);
        
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
    
    private BoxType boxType;

    public CollectionBox() {
        this(new BoxTreeNodeRecord());
    }
    
    CollectionBox(BoxTreeNodeRecord record) {
        this.record = record;
    }
    
    BoxTreeNodeRecord getRecord() {
        return record;
    }
    
    public Long getId() {
        return record.getId();
    }
    
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

    void setBoxType(BoxType boxType) {
        if (boxType == null) throw new IllegalArgumentException("boxType must not be null");
        
        record.setNodeType(boxType.getIntValue());
        this.boxType = boxType;
    }

    public void setParentId(Long value) {
        record.setParentId(value);
    }

    public Long getParentId() {
        return record.getParentId();
    }

    public void setName(String value) {
        record.setName(value);
    }

    public String getName() {
        return record.getName();
    }
    
    public boolean isPersited() {
        return getId() != 0;
    }
    
    @Override
    public String toString() {
        return getName() + " [" + getBoxType() + "]";
    }
}
