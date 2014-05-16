package it.unibo.tw.db;

import java.sql.*;

public class DataSource {
    
    // tipo di DBMS utilizzato
    private int usedDb;
    
    // nome del database
    private String dbName = "tw_stud";
    
    
    public final static int DB2 = 0;
    public final static int HSQLDB = 1;
    public final static int MYSQL = 2;
    
    public DataSource(int databaseType){
        this.usedDb = databaseType;
    }
    
    public Connection getConnection() throws PersistenceException {
        String driver;
        String dbUri;
        String userName = "";
        String password = "";
        switch ( this.usedDb ) {
            case DB2:
                userName = "xxx";
                password = "xxx";

                driver = "com.ibm.db2.jcc.DB2Driver";
                dbUri = "jdbc:db2://diva.deis.unibo.it:50000/"+dbName;
                
                break;
            case HSQLDB:
                driver = "org.hsqldb.jdbcDriver";
                // tre modalita' (vedi http://hsqldb.org/doc/guide/ch01.html):
                //  1) hsql --> Hsqldb as a Server
                //  2) mem --> Memory-Only Databases
                //  3) file --> In-Process (Standalone) Mode
                dbUri = "jdbc:hsqldb:hsql://localhost/"+dbName;
                userName = "sa";
                password = "";
                break;
            case MYSQL:
                driver = "com.mysql.jdbc.Driver";
                dbUri = "jdbc:mysql://localhost:3306/"+dbName;
                userName = "root";
                password = "";
                break;
            default:
                return null;
        }
        
        Connection connection = null;
        try {
            System.out.println("DataSource.getConnection() driver = "+driver);
            Class.forName(driver);
            System.out.println("DataSource.getConnection() dbUri = "+dbUri);
            connection = DriverManager.getConnection(dbUri, userName, password);
        }
        catch (ClassNotFoundException e) {
            throw new PersistenceException(e.getMessage());
        }
        catch(SQLException e) {
            throw new PersistenceException(e.getMessage());
        }
        return connection;
    }
    
}
