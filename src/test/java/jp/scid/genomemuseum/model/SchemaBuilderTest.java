package jp.scid.genomemuseum.model;

import static org.junit.Assert.*;

import java.sql.SQLException;

import org.h2.jdbcx.JdbcConnectionPool;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class SchemaBuilderTest {
    SchemaBuilder builder;
    private JdbcConnectionPool pool = null;
    
    @Before
    public void setup() throws Exception {
        pool = getH2TestConnectionPool();
        builder = new SchemaBuilder(pool.getConnection());
    }
    
    @After
    public void tearDown() throws Exception {
        if (pool != null) {
            pool.dispose();
        }
    }
    
    @Test
    public void getDataSchema() throws SQLException {
        MuseumDataSchema dataSchema = builder.getDataSchema();
        
        assertNotNull(dataSchema);
    }

    static JdbcConnectionPool getH2TestConnectionPool() {
        try {
            Class.forName("org.h2.Driver");
        }
        catch (ClassNotFoundException e) {
            throw new IllegalStateException("need h2 database driber", e);
        }
        
        return JdbcConnectionPool.create("jdbc:h2:mem:", "SchemaBuilderTest", "");
    }
}
