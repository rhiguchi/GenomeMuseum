package jp.scid.genomemuseum.model;

import static jp.scid.genomemuseum.model.sql.Tables.*;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import jp.scid.genomemuseum.model.CollectionBox.BoxType;
import jp.scid.genomemuseum.model.CollectionBox.GroupCollectionBox;
import jp.scid.genomemuseum.model.sql.tables.records.BoxTreeNodeRecord;
import jp.scid.genomemuseum.model.sql.tables.records.CollectionBoxItemRecord;
import jp.scid.genomemuseum.model.sql.tables.records.MuseumExhibitRecord;

import org.jooq.Record;
import org.jooq.RecordHandler;
import org.jooq.impl.Factory;

public class CollectionBoxService extends JooqEntityService<CollectionBox, BoxTreeNodeRecord> {
    static class ContentsExhibitHandler implements RecordHandler<Record> {
        private final List<MuseumExhibit> list = new LinkedList<MuseumExhibit>();
        
        @Override
        public void next(Record record) {
            MuseumExhibitRecord exhibitRecord = record.into(MUSEUM_EXHIBIT);
            MuseumExhibit element = new MuseumExhibit(exhibitRecord);
            
            Integer rowId = record.getValueAsInteger(COLLECTION_BOX_ITEM.ID);
            element.setRowId(rowId);
            
            list.add(element);
        }
    
        public List<MuseumExhibit> getElements() {
            return new ArrayList<MuseumExhibit>(list);
        }
    }

    final GroupCollectionBox groupingDelegate;
    
    CollectionBoxService(Factory factory) {
        super(factory, BOX_TREE_NODE);
        
        groupingDelegate = (GroupCollectionBox) CollectionBox.newCollectionBox(BoxType.GROUP, this);
    }
    
    @Override
    protected CollectionBox createElement(BoxTreeNodeRecord record) {
        CollectionBox box = CollectionBox.newCollectionBox(record, this);
        return box;
    }
    
    @Override
    protected BoxTreeNodeRecord recordOfElement(CollectionBox element) {
        return element.getRecord();
    }
    
    public GroupCollectionBox getGroupingDelegate() {
        return groupingDelegate;
    }

    public List<CollectionBox> fetchChildren() {
        return groupingDelegate.fetchChildren();
    }
    
    public CollectionBox addChild(BoxType boxType) {
        return groupingDelegate.addChild(boxType);
    }
    
    public CollectionBox addChild(BoxType boxType, Long parentId) {
        CollectionBox box = CollectionBox.newCollectionBox(boxType, this);
        
        box.setName("New Box");
        box.setParentId(parentId);
        box.getRecord().store();
        
        return box;
    }

    public boolean isAncestor(long boxId, long maybeAncestor) {
        boolean ancester = false;
        
        for (Long targetParentId = getParentId(boxId);
                targetParentId != null;
                targetParentId = getParentId(targetParentId)) {
            if (targetParentId.equals(maybeAncestor)) {
                ancester = true;
                break;
            }
        }
        
        return ancester;
    }
    
    private Long getParentId(long boxId) {
        return factory.select(BOX_TREE_NODE.PARENT_ID).from(BOX_TREE_NODE)
                .where(BOX_TREE_NODE.ID.equal(boxId))
                .fetchOne(0, Long.class);
    }
    
    BoxTreeNodeRecord newRecord(BoxType boxType) {
        BoxTreeNodeRecord record = factory.newRecord(table);
        record.setNodeType(boxType.getIntValue());
        
        return record;
    }

    public List<CollectionBox> fetchChildren(Long parentId) {
        final List<CollectionBox> children;
        
        if (parentId == null) {
            children = search("parent_id IS NULL");
        }
        else {
            children = search("parent_id = ?", parentId);
        }
        
        return children;
    }
    
    public List<MuseumExhibit> fetchContent(long boxId) {
        List<MuseumExhibit> elements = factory.select()
                .from(COLLECTION_BOX_ITEM)
                .join(MUSEUM_EXHIBIT)
                .on(COLLECTION_BOX_ITEM.EXHIBIT_ID.equal(MUSEUM_EXHIBIT.ID))
                .where(COLLECTION_BOX_ITEM.BOX_ID.equal(boxId))
                .orderBy(COLLECTION_BOX_ITEM.ID)
                .fetchInto(new CollectionBoxService.ContentsExhibitHandler())
                .getElements();
        
        return elements;
    }
    
    public MuseumExhibit addContentTo(long boxId, long exhibitId) {
        CollectionBoxItemRecord record = factory.insertInto(COLLECTION_BOX_ITEM)
                .set(COLLECTION_BOX_ITEM.BOX_ID, boxId)
                .set(COLLECTION_BOX_ITEM.EXHIBIT_ID, exhibitId)
                .returning().fetchOne();
        
        long recordId = record.getId();
        
        MuseumExhibit exhibit = factory.select().from(COLLECTION_BOX_ITEM)
                .join(MUSEUM_EXHIBIT)
                .on(COLLECTION_BOX_ITEM.EXHIBIT_ID.equal(MUSEUM_EXHIBIT.ID))
                .where(COLLECTION_BOX_ITEM.ID.equal(recordId))
                .fetchInto(new CollectionBoxService.ContentsExhibitHandler()).getElements().get(0);
        
        return exhibit;
    }
    
    public boolean removeContent(long contentId) {
        int result = factory.delete(COLLECTION_BOX_ITEM)
                .where(COLLECTION_BOX_ITEM.ID.equal(contentId))
                .execute();
        return result > 0;
    }
    
    public void setParent(CollectionBox box, Long parentId) {
        box.setParentId(parentId);
        box.getRecord().store();
    }
}

