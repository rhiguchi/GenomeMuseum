package jp.scid.genomemuseum.model;

import static jp.scid.genomemuseum.model.sql.Tables.*;
import static org.junit.Assert.*;

import java.util.List;

import jp.scid.genomemuseum.model.CollectionBox.BoxType;
import jp.scid.genomemuseum.model.CollectionBox.FreeCollectionBox;
import jp.scid.genomemuseum.model.CollectionBox.GroupCollectionBox;

import org.h2.jdbcx.JdbcConnectionPool;
import org.jooq.impl.Factory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class CollectionBoxServiceTest {
    private JdbcConnectionPool pool;
    CollectionBoxService service;
    
    @Before
    public void setup() throws Exception {
        pool = SchemaBuilderTest.getH2TestConnectionPool();
        SchemaBuilder builder = new SchemaBuilder(pool.getConnection());
        Factory factory = builder.getDataSchema().getFactory();
        service = new CollectionBoxService(factory);
    }
    
    @After
    public void tearDown() throws Exception {
        if (pool != null) {
            pool.dispose();
        }
    }
    
    @Test
    public void addChild() {
        CollectionBox box = service.addRootItem(BoxType.FREE);
        
        assertTrue("free box", box instanceof FreeCollectionBox);
        assertEquals("inserted", 1, service.getCount());
        assertNotNull("id inserted", box.getId());
        
        CollectionBox groupBox = service.addRootItem(BoxType.GROUP);
        
        assertTrue("group box", groupBox instanceof GroupCollectionBox);
        assertEquals("inserted", 2, service.getCount());
        assertNotNull("id inserted", groupBox.getId());
    }
    
    @Test
    public void find() {
        service.factory.insertInto(BOX_TREE_NODE)
        .set(BOX_TREE_NODE.ID, 100L)
        .newRecord()
        .set(BOX_TREE_NODE.ID, 102L)
        .execute();
        
        assertNotNull("find element", service.find(100));
        assertNotNull("find element", service.find(102L));
        assertNull("non exists element", service.find(101));
    }
    
    @Test
    public void search() {
        service.factory.insertInto(BOX_TREE_NODE)
        .set(BOX_TREE_NODE.NAME, "aaa")
        .newRecord()
        .set(BOX_TREE_NODE.NAME, "aab")
        .newRecord()
        .set(BOX_TREE_NODE.NAME, "abb")
        .execute();
        
        assertEquals("search all", 3, service.search(null).size());
        assertEquals("search like query",
                3, service.search("name LIKE ?", "%a%").size());
        assertEquals("search like query",
                2, service.search("name LIKE ?", "%ab%").size());
    }

    @Test
    public void delete() {
        service.factory.insertInto(BOX_TREE_NODE)
        .set(BOX_TREE_NODE.ID, 100L)
        .newRecord()
        .set(BOX_TREE_NODE.ID, 102L)
        .execute();
        
        CollectionBox box = service.find(100);
        
        service.delete(box);
        
        assertNull("delete", service.find(100));
    }
    
    @Test
    public void fetchChildren() {
        service.factory.insertInto(BOX_TREE_NODE)
        .set(BOX_TREE_NODE.ID, 1L)
        .newRecord()
        .set(BOX_TREE_NODE.ID, 2L)
        .newRecord()
        .set(BOX_TREE_NODE.ID, 3L)
        .execute();
        
        service.factory.insertInto(BOX_TREE_NODE)
        .set(BOX_TREE_NODE.ID, 4L).set(BOX_TREE_NODE.PARENT_ID, 1L)
        .execute();
        
        List<CollectionBox> children = service.fetchChildren(null);
        assertEquals("child count of root", 3, children.size());
        
        assertEquals("child count of a node", 1, service.fetchChildren(1L).size());
    }
    
    @Test
    public void fetchContent() {
        service.factory.insertInto(BOX_TREE_NODE, BOX_TREE_NODE.ID)
        .values(1L).values(2L).values(3L)
        .execute();
        
        service.factory
        .insertInto(MUSEUM_EXHIBIT, MUSEUM_EXHIBIT.ID)
        .values(1L).values(2L).values(3L)
        .execute();
        
        service.factory
        .insertInto(COLLECTION_BOX_ITEM, COLLECTION_BOX_ITEM.BOX_ID, COLLECTION_BOX_ITEM.EXHIBIT_ID)
        .values(1L, 1L)
        .values(1L, 1L)
        .values(1L, 2L)
        .values(3L, 1L)
        .values(3L, 3L)
        .execute();
        
        assertEquals("count of content 1", 3, service.fetchContent(1).size());
        assertEquals("count of content 2", 0, service.fetchContent(2).size());
        assertEquals("count of content 3", 2, service.fetchContent(3L).size());
    }
    
    @Test
    public void addContentTo() {
        service.factory.insertInto(BOX_TREE_NODE, BOX_TREE_NODE.ID)
        .values(1L).values(2L).values(3L)
        .execute();
        
        service.factory
        .insertInto(MUSEUM_EXHIBIT, MUSEUM_EXHIBIT.ID)
        .values(1L).values(2L).values(3L)
        .execute();
        
        service.addContentTo(1L, 3L);
        service.addContentTo(1L, 1L);
        service.addContentTo(1L, 3L);
        service.addContentTo(2L, 1L);
        
        Integer count = service.factory.selectCount().from(COLLECTION_BOX_ITEM)
        .where(COLLECTION_BOX_ITEM.BOX_ID.equal(1L))
        .fetchOne(0, Integer.class);
        
        assertEquals("inserted", 3, count.intValue());
    }
    
    @Test
    public void isAncestor() {
        service.factory.insertInto(BOX_TREE_NODE).set(BOX_TREE_NODE.ID, 1L).execute();
        service.factory.insertInto(BOX_TREE_NODE).set(BOX_TREE_NODE.ID, 2L).set(BOX_TREE_NODE.PARENT_ID, 1L).execute();
        service.factory.insertInto(BOX_TREE_NODE).set(BOX_TREE_NODE.ID, 3L).set(BOX_TREE_NODE.PARENT_ID, 2L).execute();
        service.factory.insertInto(BOX_TREE_NODE).set(BOX_TREE_NODE.ID, 4L).execute();
        
        assertTrue("parent", service.isAncestor(2L, 1L));
        assertTrue("ancestor", service.isAncestor(3L, 1L));
        assertFalse("self", service.isAncestor(1L, 1L));
        assertFalse("other", service.isAncestor(2L, 4L));
    }
}
