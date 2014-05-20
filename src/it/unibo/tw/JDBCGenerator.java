package it.unibo.tw;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class JDBCGenerator {
	
	private BeanGenerator beanGenerator;
	private ManagerGenerator managerGenerator;
	private String tableName, pluralName, constraints, pkg, pkgFolder, username, password;
	private Map<String, String> fields, singlePlural;
	private SQLGenerator sqlGen;
	
	public JDBCGenerator(String pkgFolder, String pkg, String tableName, Map<String, String> fields, String pluralName, String constraints, Map<String, String> singlePlural, String username, String password) {
		beanGenerator = new BeanGenerator(pkgFolder, pkg, "model");
		managerGenerator = new ManagerGenerator(pkgFolder, pkg);
		this.tableName = tableName;
		this.fields = fields;
		this.constraints = constraints;
		this.pluralName = pluralName;
		this.pkgFolder = pkgFolder;
		this.pkg = pkg;
		this.singlePlural = singlePlural;
		this.sqlGen = new SQLGenerator(fields, pluralName, tableName, constraints, singlePlural);
		this.username = username;
		this.password = password;
	}
	
	public void writeBean() throws Exception {
		beanGenerator.WriteBean(tableName, fields);
	}
	
	public void writeManager() throws IOException {
		managerGenerator.writeManager(tableName, pluralName, fields, constraints, singlePlural);
	}
	
	private void writePersistenceException() throws IOException {
		StringBuilder sb = new StringBuilder("package " + pkg + ".db;\n\npublic class PersistenceException extends Exception {\n");
		sb.append("\tprivate static final long serialVersionUID = 5068319580102263L;\n\n\tpublic PersistenceException(String msg){\n");
		sb.append("\t\tsuper(msg);\n\t}\n}");
		Utils.WriteFile(pkgFolder + "/db/PersistenceException.java", sb.toString());

	}
	
	public void writeDataSource() throws Exception {
		// write required exception
		writePersistenceException();
		StringBuilder sb = new StringBuilder("package " + pkg + ".db;\n\nimport java.sql.*;\n\npublic class DataSource {\n\n");
		sb.append("\t//DBMS\n\tprivate int usedDb;\n\n\t//name\n\tprivate String dbName = \"tw_stud\";\n\tpublic final static int DB2 = 0;\n\n");
		sb.append("\tpublic DataSource(int databaseType){\n\t\tthis.usedDb = databaseType;\n\t}\n\n");
		sb.append("\tpublic Connection getConnection() throws PersistenceException {\n");
		sb.append("\t\tString driver, dbUri, userName = \"\", password = \"\";\n\n");
		sb.append("\t\tswitch ( this.usedDb ) {\n");
		sb.append("\t\t\tcase DB2:\n");
		sb.append("\t\t\t\tuserName = \""+username+"\";\n");
		sb.append("\t\t\t\tpassword = \"" + password +"\";\n");
		sb.append("\t\t\t\tdriver = \"com.ibm.db2.jcc.DB2Driver\";\n");
		sb.append("\t\t\t\tdbUri = \"jdbc:db2://diva.deis.unibo.it:50000/\"+dbName;\n");
		sb.append("\t\t\t\tbreak;\n");
		sb.append("\t\t\tdefault:\n");
		sb.append("\t\t\t\treturn null;\n");
		sb.append("\t\t}\n");
		sb.append("\t\tConnection connection = null;\n\t\ttry{\n");
		sb.append("\t\t\tSystem.out.println(\"DataSource.getConnection() driver = \"+driver);\n");
		sb.append("\t\t\tClass.forName(driver);\n");
		sb.append("\t\t\tSystem.out.println(\"DataSource.getConnection() dbUri = \"+dbUri);\n");
		sb.append("\t\t\tconnection = DriverManager.getConnection(dbUri, userName, password);\n");
		sb.append("\t\t} catch (ClassNotFoundException e) {\n");
		sb.append("\t\t\tthrow new PersistenceException(e.getMessage());\n");
		sb.append("\t\t} catch(SQLException e) {\n");
		sb.append("\t\t\tthrow new PersistenceException(e.getMessage());\n");
		sb.append("\t\t}\n");
		sb.append("\t\treturn connection;\n\t}\n\n}");
		Utils.WriteFile(pkgFolder + "/db/DataSource.java", sb.toString());
	}
	
	public void writeMainTest(List<Entry<String, String>> models, Map<String, Map<String, String>> fieldsFromName) throws IOException {
		StringBuilder sb = new StringBuilder("package " + pkg + ".db;\n\n");
		sb.append("import " + pkg + ".model.*;\n");
		sb.append("import java.util.Calendar;\n\n");
		sb.append("public class JDBCMainTest {\n");
		sb.append("\tpublic static void main(String[] args) throws PersistenceException {\n");
		sb.append("\t\tCalendar cal;\n\n");
		char varName = 'a';
		Map<String, String> fields;
		for(Entry<String, String>  entry : models) {
			String d = entry.getKey(); // singular name
			String var = "manage" +d.toLowerCase();
			String plural = entry.getValue();//plural
			sb.append("\t\tManage" + plural+ " ");
			sb.append(var);
			sb.append(" = new Manage"+plural+"();\n");
			sb.append("\t\t" + var + ".dropAndCreateTable();\n\n");
			fields = fieldsFromName.get(d.toLowerCase()); 
			
			// create 2 instance
			for(int i=0;i<2;++i) {
				String objName = varName + "" + i;
				sb.append("\t\t" + d + " "+ objName + " = new " + d + "();\n");
				sb.append(sqlGen.getObjectInit(objName, fields, d));
				sb.append("\t\t" + var + ".insert(" + objName + ");\n\n");
			}
			varName++;
		}
		
		sb.append("\n\t}\n\n\t//Test && Support methods\n\n}");
		String filename = pkgFolder + "/db/JDBCMainTest.java";
		Utils.WriteFile(filename, sb.toString());
	}

}
