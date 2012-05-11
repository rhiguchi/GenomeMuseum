package jp.scid.genomemuseum.model;

import static java.lang.String.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import jp.scid.genomemuseum.model.sql.V1_0_0;
import jp.scid.genomemuseum.model.sql.V1_0_0Factory;

public class SchemaBuilder {
    public static final String DEFAULT_SCHEMA_NAME = "V1_0_0";
    public static final String DEFAULT_SCHEMA_SQL_RESOURCE = "sql/schema.sql";
    public static final String DEFAULT_SEQUENCES_SQL_RESOURCE = "sql/sequences.sql";
    
    String schemaName;
    Connection connection;
    
    public SchemaBuilder(Connection connection, String schemaName) {
        if (connection == null) throw new IllegalArgumentException("connection must not be null");
        
        this.schemaName = schemaName;
        this.connection = connection;
    }
    
    public SchemaBuilder(Connection connection) {
        this(connection, V1_0_0.V1_0_0.getName());
    }
    
    public MuseumDataSchema getDataSchema() throws SQLException {
        if (!isSchemaExists()) {
            buildSchema();
        }
        
        V1_0_0Factory factory = new V1_0_0Factory(connection);
        return new MuseumDataSchema(factory);
    }
    
    public Connection getConnection() {
        return connection;
    }
    
    public void setConnection(Connection connection) {
        if (connection == null) throw new IllegalArgumentException("connection must not be null");
        
        this.connection = connection;
    }
    
    public boolean isSchemaExists() throws SQLException {
        String sql = getSchemaSelectSql();
        PreparedStatement statement = getConnection().prepareStatement(sql);
        statement.setObject(1, schemaName);
        ResultSet resultSet = statement.executeQuery();
        
        boolean exists = resultSet.next();
        
        resultSet.close();
        
        return exists;
    }
    
    void buildSchema() throws SQLException {
        createShema();
        
        // current schema changing
        String sql = getSchemaChangeSql(schemaName);
        getConnection().prepareStatement(sql).execute();
        
        String schemaSql;
        String sequenceSql;
        try {
            schemaSql = getSchemaSql();
            sequenceSql = getSequenceSql();
        }
        catch (IOException e) {
            throw new SQLException(e);
        }
        PreparedStatement statement = getConnection().prepareStatement(schemaSql);
        statement.execute();
        statement.close();
        
        PreparedStatement seqStmt = getConnection().prepareStatement(sequenceSql);
        seqStmt.execute();
        seqStmt.close();
    }

    String getSchemaSql() throws IOException {
        return getResourceText(DEFAULT_SCHEMA_SQL_RESOURCE);
    }
    
    String getSequenceSql() throws IOException {
        return getResourceText(DEFAULT_SEQUENCES_SQL_RESOURCE);
    }
    
    String getResourceText(String resourceAddr) throws IOException {
        StringBuilder sqlBuilder = new StringBuilder();
        
        InputStream schemaSqlInst = getClass().getResourceAsStream(resourceAddr);
        
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(schemaSqlInst));
            
            char[] cbuf = new char[8196];
            int read;
            
            while ((read = reader.read(cbuf)) != -1) {
                sqlBuilder.append(cbuf, 0, read);
            }
        }
        finally {
            schemaSqlInst.close();
        }
        
        return sqlBuilder.toString();
    }
    
    void createShema() throws SQLException {
        String sql = getSchemaCreationSql(schemaName);
        PreparedStatement statement = getConnection().prepareStatement(sql);
        statement.execute();
        statement.close();
    }
    
    private String getSchemaSelectSql() {
        return "select TABLE_NAME from INFORMATION_SCHEMA.TABLES where TABLE_SCHEMA = ?";
    }
    
    private String getSchemaCreationSql(String schemaName) {
        return format("create schema %s", schemaName);
    }
    
    private String getSchemaChangeSql(String schemaName) {
        return format("SET SCHEMA %s", schemaName);
    }
    

    public static void main(String[] args) {
        if (args.length < 4)
            return;
        String url = args[0];
        String user = args[1];
        String password = args[2];
        String schemaName = args[3];
        
        final Connection connection;
        try {
            connection = DriverManager.getConnection(url, user, password);
            SchemaBuilder builder = new SchemaBuilder(connection, schemaName);
            builder.buildSchema();
        }
        catch (SQLException e) {
            e.printStackTrace();
            return;
        }
        
        try {
            connection.close();
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
    }
}