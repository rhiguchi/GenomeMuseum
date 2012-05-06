package jp.scid.genomemuseum.model;

import static org.junit.Assert.*;
import static jp.scid.genomemuseum.model.sql.Tables.*;

import java.util.List;

import jp.scid.genomemuseum.model.sql.tables.records.BoxTreeNodeRecord;
import jp.scid.genomemuseum.model.sql.tables.records.MuseumExhibitRecord;

import org.h2.jdbcx.JdbcConnectionPool;
import org.jooq.InsertQuery;
import org.jooq.Result;
import org.jooq.impl.Factory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class MuseumExhibitLibraryTest {
    private JdbcConnectionPool pool;
    MuseumExhibitLibrary library;
    
    @Before
    public void setup() throws Exception {
        pool = SchemaBuilderTest.getH2TestConnectionPool();
        SchemaBuilder builder = new SchemaBuilder(pool.getConnection());
        Factory factory = builder.getDataSchema().getFactory();
        library = new MuseumExhibitLibrary(factory);
    }
    
    @After
    public void tearDown() throws Exception {
        if (pool != null) {
            pool.dispose();
        }
    }
    
    @Test
    public void databaseInitial() {
        assertTrue("empty on initial", library.fetchExhibits().isEmpty()); 
    }
    
    @Test
    public void newElement() {
        MuseumExhibit element = library.newElement();
        
        assertNotNull("creation", element);
        
//        Integer count = library.factory.selectCount().from(MUSEUM_EXHIBIT).fetchOne(0, Integer.class);
//        assertEquals("add element", 1, count.intValue()); 
//        assertNotNull("id of element", element.getId()); 
        
//        library.newElement();
//        Integer count2 = library.factory.selectCount().from(MUSEUM_EXHIBIT).fetchOne(0, Integer.class);
//        assertEquals("add element second", 2, count2.intValue()); 
    }
    
    @Test
    public void store() {
        MuseumExhibit element = library.newElement();
        
        boolean saveResult = library.save(element);
        
        assertTrue("excuted", saveResult);
        
        assertEquals("stored", 1, library.recourdCount());
    }
    
    @Test
    public void recordCount() {
        library.factory.insertInto(MUSEUM_EXHIBIT)
        .set(MUSEUM_EXHIBIT.NAME, "1")
        .newRecord().set(MUSEUM_EXHIBIT.NAME, "1")
        .newRecord().set(MUSEUM_EXHIBIT.NAME, "1")
        .execute();
        
        assertEquals("select", 3, library.recourdCount()); 
    }
    
    @Test
    public void delete() {
        MuseumExhibit exhibit = library.newElement();
        library.save(exhibit);
        
        library.delete(exhibit);
        
        assertEquals("deleted", 0, library.recourdCount());
    }
    
    // box contents
    BoxTreeNodeRecord newNode() {
        BoxTreeNodeRecord nodeRecord = library.factory.insertInto(BOX_TREE_NODE)
                .set(BOX_TREE_NODE.NAME, "test")
                .returning().fetchOne();
        return nodeRecord;
    }
    
    @Test
    public void boxContents() {
        BoxTreeNodeRecord nodeRecord = newNode();
        
        assert nodeRecord.getId() > 0L;
        
        List<MuseumExhibit> list = library.getBoxContents(nodeRecord.getId());
        
        assertTrue("empty on initial", list.isEmpty());
    }
    
    @Test
    public void addBoxContent() {
        BoxTreeNodeRecord node1 = newNode();
        BoxTreeNodeRecord node2 = newNode();
        
        MuseumExhibit element = library.newElement();
        
        int index1 = library.addBoxContent(node1.getId(), element);
        
        assertEquals("inserted index", 0, index1);
        
        int index2 = library.addBoxContent(node1.getId(), element);
        
        assertEquals("inserted index", 1, index2);
        
        int index3 = library.addBoxContent(node2.getId(), element);
        
        assertEquals("inserted index", 0, index3);
        
        List<MuseumExhibit> node1Contents = library.getBoxContents(node1.getId());
        List<MuseumExhibit> node2Contents = library.getBoxContents(node2.getId());
        
        assertEquals("contents count", 2, node1Contents.size());
        assertEquals("contents count", 1, node2Contents.size());
        
        assertTrue("has row id", node1Contents.get(0).hasRowId());
        
        assertEquals("row id", 1, node1Contents.get(0).getRowId());
        assertEquals("row id", 2, node1Contents.get(1).getRowId());
        assertEquals("row id", 3, node2Contents.get(0).getRowId());
    }
    
    @Test
    public void deleteContent() {
        BoxTreeNodeRecord node = newNode();
        MuseumExhibit element = library.newElement();
        
        library.addBoxContent(node.getId(), element);
        library.addBoxContent(node.getId(), element);
        library.addBoxContent(node.getId(), element);
        
        List<MuseumExhibit> contents = library.getBoxContents(node.getId());
        
        boolean reuslt = library.deleteContent(contents.get(1).getRowId());
        
        assertTrue("suceess", reuslt);
        
        List<MuseumExhibit> contentsNew = library.getBoxContents(node.getId());
        
        assertEquals("deleted", 2, contentsNew.size());
    }
}
