package it.unibo.tw;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Map;

public class ManagerGenerator {
	
	private final String pkgFolder, pkg;
	
	public ManagerGenerator(String pkgFolder, String pkg) {
		this.pkg = pkg;
		this.pkgFolder = pkgFolder;
	}
	
	public void writeManager(String singleName, String pluralName, Map<String, String> fields, String constraints) throws IOException {
		File managersFolder = new File(pkgFolder + "/db/");
		if(! managersFolder.exists()) {
			managersFolder.mkdir();
		}
		StringBuilder sb = new StringBuilder("package " + pkg + ".db;\n\n");
		String className = "Manage" + Utils.UcFirst(pluralName);
		sb.append("import " + pkg + ".model." + Utils.UcFirst(singleName) + ";\n");
		sb.append("import java.sql.Connection;\n");
		sb.append("import java.sql.PreparedStatement;\n");
		sb.append("import java.sql.ResultSet;\n");
		sb.append("import java.sql.SQLException;\n");
		sb.append("import java.sql.Statement;\n");
		sb.append("public class " + className + " {\n");
		sb.append("\tprivate DataSource dataSource;\n\n");
		sb.append("\tpublic " + className + "() {\n");
		sb.append("\t\tthis.dataSource = new DataSource(DataSource.DB2);\n\t}\n\n");
		SQLGenerator sqlGen = new SQLGenerator(fields, pluralName, singleName, constraints);
		sb.append(sqlGen.getSQLConstants());
		sb.append("//\t\tMETHODS\n\n");
		// methods
		
		// dropAndCreateTable
		sb.append("\tpublic void dropAndCreateTable() throws PersistenceException{\n");
		sb.append("\t\tConnection connection = this.dataSource.getConnection();\n");
		sb.append("\t\tStatement statement = null;\n");
		sb.append("\t\ttry {\n");
		sb.append("\t\t\tstatement = connection.createStatement ();\n");
		sb.append("\t\t\ttry {\n");
		sb.append("\t\t\t\tstatement.executeUpdate(drop);\n");
		sb.append("\t\t\t} catch (SQLException e) {\n");
		sb.append("\t\t\t\t// the table does not exist\n");
		sb.append("\t\t\t}\n");
		sb.append("\t\t\tstatement.executeUpdate (create);\n");
		sb.append("\t\t\tSystem.out.println(create);\n");
		sb.append("\t\t\tstatement.close();\n");
		sb.append("\t\t} catch (SQLException e) {\n");
		sb.append("\t\t\tthrow new PersistenceException(e.getMessage());\n");
		sb.append("\t\t}\n");
		sb.append("\t\tfinally {\n");
		sb.append("\t\t\ttry {\n");
		sb.append("\t\t\t\tif (statement != null) {\n");
		sb.append("\t\t\t\t\tstatement.close();\n");
		sb.append("\t\t\t\t}\n");
		sb.append("\t\t\t\tif (connection!= null) {\n");
		sb.append("\t\t\t\t\tconnection.close();\n");
		sb.append("\t\t\t\t}\n");
		sb.append("\t\t\t} catch (SQLException e) {\n");
		sb.append("\t\t\t\tthrow new PersistenceException(e.getMessage());\n");
		sb.append("\t\t\t}\n");
		sb.append("\t\t}\n");
		sb.append("\t}\n\n");
		
		// insert
		sb.append("\tpublic void insert(" + Utils.UcFirst(singleName) + " o) throws PersistenceException{\n");
		sb.append("\t\tConnection connection = null;\n");
		sb.append("\t\tPreparedStatement statement = null;\n");
		sb.append("\t\ttry {\n");
		sb.append("\t\t\tconnection = this.dataSource.getConnection();\n");
		sb.append("\t\t\tstatement = connection.prepareStatement(insert);\n");
		// loop
		sb.append(sqlGen.getInsertSetter());
		sb.append("\t\t\tstatement.executeUpdate();\n");
		sb.append("\t\t} catch(SQLException e) {\n");
		sb.append("\t\tthrow new PersistenceException(e.getMessage());\n");
		sb.append("\t\t}\n");
		sb.append("\t}\n\n");
		
		// delete
		sb.append("\tpublic void delete(Long id) throws PersistenceException{\n");
		sb.append("\t\tConnection connection = null;\n");
		sb.append("\t\tPreparedStatement statement = null;\n");
		sb.append("\t\ttry {\n");
		sb.append("\t\t\tconnection = this.dataSource.getConnection();\n");
		sb.append("\t\t\tSystem.out.println(delete);\n");
		sb.append("\t\t\tstatement = connection.prepareStatement(delete);\n");
		sb.append("\t\t\tstatement.setLong(1, id);\n");
		sb.append("\t\t\tstatement.executeUpdate();\n");
		sb.append("\t\t} catch (SQLException e) {\n");
		sb.append("\t\t\tthrow new PersistenceException(e.getMessage());\n");
		sb.append("\t\t}\n");
		sb.append("\t}\n\n");
		
		// update
		sb.append("\tpublic void update(" + Utils.UcFirst(singleName) + " o) throws PersistenceException{\n");
		sb.append("\t\tConnection connection = null;\n");
		sb.append("\t\tPreparedStatement statement = null;\n");
		sb.append("\t\ttry {\n");
		sb.append("\t\t\tconnection = this.dataSource.getConnection();\n");
		sb.append("\t\t\tSystem.out.println(update);\n");
		sb.append("\t\t\tstatement = connection.prepareStatement(update);\n");
		//loop
		sb.append(sqlGen.getUpdateSetter());
		sb.append("\t\t\tstatement.executeUpdate();\n");
		sb.append("\t\t} catch(SQLException e) {\n");
		sb.append("\t\t\tthrow new PersistenceException(e.getMessage());\n");
		sb.append("\t\t}\n");
		sb.append("\t}\n\n");
		
		// read
		sb.append("\tpublic " + Utils.UcFirst(singleName) + " read(Long id) throws PersistenceException{\n");
		sb.append("\t\tConnection connection = null;\n");
		sb.append("\t\tPreparedStatement statement = null;\n");
		sb.append("\t\t" + Utils.UcFirst(singleName) + " res = null;\n");
		sb.append("\t\ttry {\n");
		sb.append("\t\t\tconnection = this.dataSource.getConnection();\n");
		sb.append("\t\t\tSystem.out.println(read_by_id);\n");
		sb.append("\t\t\tstatement = connection.prepareStatement(read_by_id);\n");
		sb.append("\t\t\tstatement.setLong(1, id);\n\n");
		sb.append("\t\t\tResultSet results = statement.executeQuery();\n");
		sb.append("\t\t\tif(results.next()) {\n");
		sb.append(sqlGen.getReadSetter());
		sb.append("\t\t\t}\n");
		sb.append("\t\t} catch(SQLException e) {\n");
		sb.append("\t\t\tthrow new PersistenceException(e.getMessage());\n");
		sb.append("\t\t}\n");
		sb.append("\t\treturn res;\n");
		sb.append("\t}\n\n");
		
		//end of class
		sb.append("\n}");
		
		// write file
		String filename = pkgFolder+ "/db/" + className + ".java";
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filename)));
		writer.write(sb.toString());
		writer.close();
		System.out.println("[!] Created: " + filename);
	}
}
