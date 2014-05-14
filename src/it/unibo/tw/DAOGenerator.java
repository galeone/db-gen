package it.unibo.tw;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Map;

public class DAOGenerator {
	
	private BeanGenerator beanGenerator;
	private String pkg, pkgFolder, tableName;
	private Map<String, String> fields;
	private SQLGenerator sqlGen;
	
	public DAOGenerator(String pkgFolder, String pkg, String tableName, Map<String, String> fields, String pluralName, String constraints) {
		beanGenerator = new BeanGenerator(pkgFolder, pkg, "dao");
		this.pkg = pkg;
		this.pkgFolder = pkgFolder + "/dao/";
		this.fields = fields;
		this.tableName = tableName;
		this.sqlGen = new SQLGenerator(fields, pluralName, tableName, constraints);
	}
	
	public void writeDTO() throws Exception {
		beanGenerator.WriteBean(tableName, fields);
	}
	
	public void writeDAO() throws IOException {
		// Directory
		File dir = new File(pkgFolder);
		if(!dir.exists()) {
			dir.mkdir();
		}
		
		String baseName = Utils.UcFirst(tableName);
		String dto = baseName + "DTO";
		String dao = baseName + "DAO";
		
		StringBuilder sb = new StringBuilder("package " + pkg + ".dao;\n\n");
		// Create DAO interface
		sb.append("public interface " + dao +" {\n\n");
		sb.append("\t// CRUD\n\tpublic void create(");
		sb.append(dto);
		sb.append(" o);\n\tpublic ");
		sb.append(dto);
		sb.append(" read(Long id);\n\tpublic boolean update(");
		sb.append(dto);
		sb.append(" o);\n\tpublic boolean delete(Long id);\n\n");
		sb.append("\t//Required methods\n");
		sb.append("\tpublic boolean createTable();\n\tpublic boolean dropTable();\n\n");
		sb.append("\t//My methods\n\n");
		sb.append("}");
		// Write interface
		
		String filename = pkgFolder + dao + ".java";
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filename)));
		writer.write(sb.toString());
		writer.close();
		System.out.println("[!] Created: " + filename);
		
		// db2 implementation
		String dest = pkgFolder + "/db2/";
		dir = new File(dest);
		if(!dir.exists()) {
			dir.mkdir();
		}
			
		sb.setLength(0); // clear sb
		sb.append("package " + pkg + ".dao.db2;\n\n");
		sb.append("import " + pkg + ".dao.*;\n");
		sb.append("import java.sql.Connection;\nimport java.sql.PreparedStatement;\nimport java.sql.ResultSet;\nimport java.util.logging.Logger;\n\n");
		sb.append("public class Db2" + dao + " implements " + dao+  " {\n");
		sb.append("\tLogger logger = Logger.getLogger( getClass().getCanonicalName() );\n\n");
		sb.append(sqlGen.getSQLConstants());
		// dao methds
		// create
		sb.append("\t@Override\n\tpublic void create(");
		sb.append(dto);
		sb.append(" o) {\n");
		sb.append("\t\tConnection conn = Db2DAOFactory.createConnection();\n");
		sb.append("\t\ttry {\n");
		String db2dao = "Db2" + dao; 
		sb.append("\t\t\tPreparedStatement statement = conn.prepareStatement(insert);\n");
		sb.append("\t\t\tstatement.clearParameters();\n\n");
		sb.append(sqlGen.getInsertSetter());
		sb.append("\t\t\tstatement.executeUpdate();\n");
		sb.append("\t\t\tstatement.close();\n");
		sb.append("\t\t} catch (Exception e) {\n");
		sb.append("\t\t\tlogger.warning(\"create(): failed to insert entry: \" + e.getMessage());\n");
		sb.append("\t\t\te.printStackTrace();\n");
		sb.append("\t\t}\n\t}\n\n");
		// read
		sb.append("\t@Override\n\tpublic " + dto + " read(Long id) {\n");
		sb.append("\t\t"+ dto + " res = null;\n");
		sb.append("\t\tif ( id < 0 )  {\n");
		sb.append("\t\t\tlogger.warning(\"read(): cannot read an entry with a negative id\");\n");
		sb.append("\t\t\treturn res;\n");
		sb.append("\t\t}\n");
		sb.append("\t\tConnection conn = Db2DAOFactory.createConnection();\n\n");
		sb.append("\t\ttry {\n");
		sb.append("\t\t\tPreparedStatement statement = conn.prepareStatement(read_by_id);\n");
		sb.append("\t\t\tstatement.clearParameters();\n");
		sb.append("\t\t\tstatement.setLong(1, id);\n");
		sb.append("\t\t\tResultSet results = statement.executeQuery();\n");
		sb.append("\t\t\tif(results.next()) {\n");
		sb.append(sqlGen.getReadSetter("DTO"));
		sb.append("\t\t\t}\n");
		sb.append("\t\t\tresults.close();\n");
		sb.append("\t\t\tstatement.close();\n");
		sb.append("\t\t} catch (Exception e) {\n");
		sb.append("\t\t\tlogger.warning(\"read(): failed to retrieve entry with id = \" + id+\": \"+e.getMessage());\n");
		sb.append("\t\t\te.printStackTrace();\n");
		sb.append("\t\t} finally {\n");
		sb.append("\t\t\tDb2DAOFactory.closeConnection(conn);\n");
		sb.append("\t\t}\n");
		sb.append("\t\treturn res;\n\t}\n\n");
		// update
		sb.append("\t@Override\n\tpublic boolean update(" + dto + " o) {\n");
		sb.append("\t\tboolean result = false;\n");
		sb.append("\t\tif( o == null) {\n");
		sb.append("\t\t\tlogger.warning( \"update(): failed to update a null entry\");\n");
		sb.append("\t\t\treturn result;\n");
		sb.append("\t\t}\n");
		sb.append("\t\tConnection conn = Db2DAOFactory.createConnection();\n\n");
		sb.append("\t\ttry {\n");
		sb.append("\t\t\tPreparedStatement statement = conn.prepareStatement(update);\n");
		sb.append("\t\t\tstatement.clearParameters();\n");
		sb.append(sqlGen.getUpdateSetter());
		sb.append("\t\t\tstatement.executeQuery();\n");
		sb.append("\t\t\tresult = true;\n");
		sb.append("\t\t\tstatement.close();\n");
		sb.append("\t\t} catch (Exception e) {\n");
		sb.append("\t\t\tlogger.warning(\"update(): failed to update entry \" +e.getMessage());\n");
		sb.append("\t\t\te.printStackTrace();\n");
		sb.append("\t\t} finally {\n");
		sb.append("\t\t\tDb2DAOFactory.closeConnection(conn);\n");
		sb.append("\t\t}\n");
		sb.append("\t\treturn result;\n\t}\n\n");
		// delete
		sb.append("\t@Override\n\tpublic boolean delete(Long id) {\n");
		sb.append("\t\tboolean result = false;\n");
		sb.append("\t\tif ( id < 0 )  {\n");
		sb.append("\t\t\tlogger.warning(\"delete(): cannot delete an entry with a negative id\");\n");
		sb.append("\t\t\treturn result;\n");
		sb.append("\t\t}\n");
		sb.append("\t\tConnection conn = Db2DAOFactory.createConnection();\n\n");
		sb.append("\t\ttry {\n");
		sb.append("\t\t\tPreparedStatement statement = conn.prepareStatement(delete);\n");
		sb.append("\t\t\tstatement.clearParameters();\n");
		sb.append("\t\t\tstatement.setLong(1, id);\n");
		sb.append("\t\t\tstatement.executeUpdate();\n");
		sb.append("\t\t\tresult = true;\n");
		sb.append("\t\t\tstatement.close();\n");
		sb.append("\t\t} catch (Exception e) {\n");
		sb.append("\t\t\tlogger.warning(\"delete(): failed to delete entry with id = \" + id + \":  \" +e.getMessage());\n");
		sb.append("\t\t\te.printStackTrace();\n");
		sb.append("\t\t} finally {\n");
		sb.append("\t\t\tDb2DAOFactory.closeConnection(conn);\n");
		sb.append("\t\t}\n");
		sb.append("\t\treturn result;\n\t}\n\n");
		// createTable
		sb.append("\tpublic boolean createTable() {\n");
		sb.append("\t\tboolean result = false;\n");
		sb.append("\t\tConnection conn = Db2DAOFactory.createConnection();\n\n");
		sb.append("\t\ttry {\n");
		sb.append("\t\t\tPreparedStatement statement = conn.prepareStatement(create);\n");
		sb.append("\t\t\tstatement.execute(create);\n");
		sb.append("\t\t\tresult = true;\n");
		sb.append("\t\t\tstatement.close();\n");
		sb.append("\t\t} catch (Exception e) {\n");
		sb.append("\t\t\tlogger.warning(\"createTable(): failed to create table '\" + TABLE +\"': \" + e.getMessage());\n");
		sb.append("\t\t} finally {\n");
		sb.append("\t\t\tDb2DAOFactory.closeConnection(conn);\n");
		sb.append("\t\t}\n");
		sb.append("\t\treturn result;\n\t}\n\n");
		// dropTable
		sb.append("\tpublic boolean dropTable() {\n");
		sb.append("\t\tboolean result = false;\n");
		sb.append("\t\tConnection conn = Db2DAOFactory.createConnection();\n\n");
		sb.append("\t\ttry {\n");
		sb.append("\t\t\tPreparedStatement statement = conn.prepareStatement(drop);\n");
		sb.append("\t\t\tstatement.execute(drop);\n");
		sb.append("\t\t\tresult = true;\n");
		sb.append("\t\t\tstatement.close();\n");
		sb.append("\t\t} catch (Exception e) {\n");
		sb.append("\t\t\tlogger.warning(\"createTable(): failed to drop table '\" + TABLE +\"': \" + e.getMessage());\n");
		sb.append("\t\t} finally {\n");
		sb.append("\t\t\tDb2DAOFactory.closeConnection(conn);\n");
		sb.append("\t\t}\n");
		sb.append("\t\treturn result;\n\t}\n\n");
		// other methods space
		sb.append("\t//Other methods\n\n}");
		// Write file
		filename = dest + db2dao + ".java";
		writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filename)));
		writer.write(sb.toString());
		writer.close();
		System.out.println("[!] Created: " + filename);
	}
	
	void writeFactories(String[] daos) throws IOException {
		// DAOFactory (abstract)
		StringBuilder sb = new StringBuilder("package " + pkg + ".dao;\n\n");
		sb.append("import " + pkg + ".dao.db2.Db2DAOFactory;\n\n");
		sb.append("public abstract class DAOFactory {\n");
		sb.append("\tpublic static final int DB2 = 0;\n\n");
		sb.append("\tpublic static DAOFactory getDAOFactory(int whichFactory) {\n");
		sb.append("\t\tswitch ( whichFactory ) {\n");
		sb.append("\t\tcase DB2:\n");
		sb.append("\t\t\treturn new Db2DAOFactory();\n");
		sb.append("\t\tdefault:\n");
		sb.append("\t\t\treturn null;\n");
		sb.append("\t\t}\n\t}\n\n");
		for(String d : daos ) {
			sb.append("\tpublic abstract " + d + " get" + d + "();\n");
		}
		sb.append("}");
		
		String filename = pkgFolder + "/DAOFactory.java";
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filename)));
		writer.write(sb.toString());
		writer.close();
		System.out.println("[!] Created: " + filename);
		
		sb.setLength(0); // clear sb
		sb.append("package " + pkg + ".dao.db2;\n\n");
		sb.append("import " + pkg + ".dao.*;\n");
		sb.append("import java.sql.Connection;\nimport java.sql.DriverManager;\n\n");
		sb.append("public class Db2DAOFactory extends DAOFactory {\n\n");
		sb.append("\tpublic static final String DRIVER = \"com.ibm.db2.jcc.DB2Driver\";\n");
		sb.append("\tpublic static final String DBURL = \"jdbc:db2://diva.deis.unibo.it:50000/tw_stud\";\n");
		sb.append("\tpublic static final String USERNAME = \"xxx\";\n");
		sb.append("\tpublic static final String PASSWORD = \"yyy\";\n");
		sb.append("\tstatic {\n");
		sb.append("\t\ttry {\n");
		sb.append("\t\t\tClass.forName(DRIVER);\n");
		sb.append("\t\t} catch(Exception e) {\n");
		sb.append("\t\t\tSystem.err.println(\"failed to load DB2 JDBC driver\" + \": \" + e.toString());\n");
		sb.append("\t\t\te.printStackTrace();\n");
		sb.append("\t\t}\t}\n\n");
		sb.append("\tpublic static Connection createConnection() {\n");
		sb.append("\t\ttry {\n");
		sb.append("\t\t\treturn DriverManager.getConnection(DBURL,USERNAME,PASSWORD);\n");
		sb.append("\t\t} catch (Exception e) {\n");
		sb.append("\t\t\tSystem.err.println(Db2DAOFactory.class.getName() + \".createConnection(): failed creating connection: \" + e.toString());\n");
		sb.append("\t\t\te.printStackTrace();\n");
		sb.append("\t\t\treturn null;\n");
		sb.append("\t\t}\n\t}\n\n");
		sb.append("\tpublic static void closeConnection(Connection conn) {\n");
		sb.append("\t\ttry {\n");
		sb.append("\t\t\tconn.close();\n");
		sb.append("\t\t} catch(Exception e) {\n");
		sb.append("\t\t\tSystem.err.println(Db2DAOFactory.class.getName() + \".closeConnection(): failed closing connection: \" + e.toString());\n");
		sb.append("\t\t\te.printStackTrace();\n");
		sb.append("\t\t}\n\t}\n\n");
		sb.append("\t//Override abstract methods\n\n");
		for(String d : daos) {
			sb.append("\t@Override\n\tpublic " + d + " get" + d + "() {\n");
			sb.append("\t\treturn new Db2" + d + "();\n\t}\n\n");
		}
		sb.append("}");
		
		filename = pkgFolder + "/db2/Db2DAOFactory.java";
		writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filename)));
		writer.write(sb.toString());
		writer.close();
		System.out.println("[!] Created: " + filename);
	}
	
	public void writeMainTest(String[] daos, Map<String, Map<String, String>> fieldsFromName) throws IOException {
		StringBuilder sb = new StringBuilder("package " + pkg + ".dao;\n\n");
		sb.append("import java.util.Calendar;\n\n");
		sb.append("public class DAOMainTest {\n");
		sb.append("\tpublic static final int DAO = DAOFactory.DB2;\n\n");
		sb.append("\tpublic static void main(String[] args) {\n");
		sb.append("\t\tDAOFactory daoFactoryInstance = DAOFactory.getDAOFactory(DAO);\n");
		sb.append("\t\tCalendar cal;\n\n");
		char varName = 'a';
		Map<String, String> fields;
		for(String d : daos) {
			String var = d.toLowerCase();
			sb.append("\t\t" + d);
			sb.append(" ");
			sb.append(var);
			sb.append(" = daoFactoryInstance.get"+d+"();\n");
			sb.append("\t\t" + var + ".dropTable();\n");
			sb.append("\t\t" + var + ".createTable();\n\n");
			
			fields = fieldsFromName.get(d.replace("DAO", "").toLowerCase()); 
			
			// create 2 instance
			for(int i=0;i<2;++i) {
				String dto = d.replace("DAO", "DTO");
				String objName = varName + "" + i;
				sb.append("\t\t" + dto + " "+ objName + " = new " + dto + "();\n");
				sb.append(sqlGen.getObjectInit(objName, fields));
				sb.append("\t\t" + var + ".create(" + objName + ");\n\n");
			}
			varName++;
		}
		
		sb.append("\n\t}\n\n\t//Test && Support methods\n\n}");
		String filename = pkgFolder + "/DAOMainTest.java";
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filename)));
		writer.write(sb.toString());
		writer.close();
		System.out.println("[!] Created: " + filename);
	}

}
