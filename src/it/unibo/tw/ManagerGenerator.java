package it.unibo.tw;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class ManagerGenerator {
	
	private final String pkgFolder, pkg;
	
	public ManagerGenerator(String pkgFolder, String pkg) {
		this.pkg = pkg;
		this.pkgFolder = pkgFolder;
	}
	
	public void writeRepository(String singleName, String pluralName, Map<String, String> fields, String constraints) throws IOException {
		File managersFolder = new File(pkgFolder + "/db/");
		if(! managersFolder.exists()) {
			managersFolder.mkdir();
		}
		StringBuilder sb = new StringBuilder("package " + pkg + ".db;\n\n");
		String className = "Manage" + Utils.UcFirst(pluralName);
		sb.append("import " + pkg + ".beans." + Utils.UcFirst(singleName) + ";\n");
		sb.append("import java.sql.Connection;\n");
		sb.append("import java.sql.PreparedStatement;\n");
		sb.append("import java.sql.ResultSet;\n");
		sb.append("import java.sql.SQLException;\n");
		sb.append("import java.sql.Statement;\n");
		sb.append("public class " + className + " {\n");
		sb.append("\tprivate DataSource dataSource;\n\n");
		sb.append("\tpublic " + className + "() {\n");
		sb.append("\t\tthis.dataSource = new DataSource(DataSource.DB2);\n\t}\n\n");
		sb.append("\tprivate static final String TABLE = \"" + pluralName.toLowerCase() + "\";\n");
		for(Entry<String, String> field : fields.entrySet()) {
			String name = field.getKey();
			sb.append("\tprivate static final String " + name.toUpperCase() + " = " + "\"" + name.toLowerCase() + "\";\n");
		}
		
		sb.append("\n\t// SQL STATEMENTS\n");
		//insert
		sb.append("\tprivate static final String insert = \"INSERT INTO \"");
		sb.append(" + TABLE + \" ( \" + ");
		String toAppend = "";
		int howMany = 0;
		// Map used to save ? position to generate insert method
		Map<String, Integer> insertPositions = new HashMap<String, Integer>();
		for(Entry<String, String> field : fields.entrySet()) {
			String name = field.getKey().toUpperCase();
			toAppend += name + "+ \", \" + ";
			howMany++;
			insertPositions.put(field.getKey(), howMany);
		}
		sb.append(toAppend.substring(0, toAppend.length()-6) + ") VALUES(");
		for(int i=0;i<howMany-1;++i) {
			sb.append("?,");
		}
		sb.append("?) \";\n");
		//delete
		sb.append("\tprivate static final String delete = \"DELETE FROM \"");
		sb.append(" + TABLE + \" WHERE \" + ID + \" = ?\";\n");
		//update
		sb.append("\tprivate static final String update = \"UPDATE \" + TABLE + \" SET ");
		toAppend = "";
		howMany = 0;
		Map<String, Integer> updatePositions = new HashMap<String, Integer>();
		for(Entry<String, String> field : fields.entrySet()) { 
			String name = field.getKey().toUpperCase();
			if(name.equals("ID")) //condition, don't set
				continue;
			toAppend += "\" + " + name + " + \" = ?,";
			howMany++;
			updatePositions.put(field.getKey(), howMany);
		}
		sb.append(toAppend.substring(0,toAppend.length()-1));
		sb.append(" WHERE \" + ID + \" = ?\";\n");
		howMany++;
		//updatePositions.put("ID", howMany);
		//read by id
		sb.append("\tprivate static final String read_by_id = \"SELECT * FROM \" + TABLE + \" WHERE + \" + ID + \"  = ?\";\n");
		// drop
		sb.append("\tprivate static final String drop = \"DROP TABLE \" + TABLE;\n");
		// create
		sb.append("\tprivate static final String create = \"CREATE TABLE \" + TABLE + \" ( \" + \n");
		toAppend = "";
		for(Entry<String, String> field : fields.entrySet()) { 
			String name = field.getKey().toUpperCase();
			String type = field.getValue().toUpperCase();
			if(type.equals("STRING")) {
				type = "VARCHAR(50)";
			} // date ok, double ok, boolean ok
			String elem = "";
			// Foreign key
			if(name.indexOf("ID") == 0){
				elem = name + "+ \"BIGINT NOT NULL ";
				if(name.equals("ID")) { //primary key
					elem += "PRIMARY KEY";
				}
				else { // foreign key
					elem += "REFERENCES " + name.substring(2);
				}
			} else { // no  key
				elem = name + "+ \"" + type + " NOT NULL"; 
			}
			sb.append( "\t\t\t" + elem + ", \" +\n" );
		}
		sb.append("\t\t\t\"" + constraints.toUpperCase());
		sb.append(")\";\n\n//\t\tMETHODS\n\n");
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
		sb.append(parseFields(fields, insertPositions));
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
		sb.append(parseFields(fields, updatePositions));
		sb.append("\t\t\tstatement.executeUpdate();\n");
		sb.append("\t\t} catch(SQLException e) {\n");
		sb.append("\t\t\tthrow new PersistenceException(e.getMessage());\n");
		sb.append("\t\t}\n");
		sb.append("\t}\n\n");
		
		// read
		sb.append("\tpublic " + Utils.UcFirst(singleName) + " read(Long id) throws PersistenceException{\n");
		sb.append("\t\tConnection connection = null;\n");
		sb.append("\t\tPreparedStatement statement = null;\n");
		sb.append("\t\t" + Utils.UcFirst(singleName)+ " res = null;\n");
		sb.append("\t\ttry {\n");
		sb.append("\t\t\tconnection = this.dataSource.getConnection();\n");
		sb.append("\t\t\tSystem.out.println(read_by_id);\n");
		sb.append("\t\t\tstatement = connection.prepareStatement(read_by_id);\n");
		sb.append("\t\t\tstatement.setLong(1, id);\n\n");
		sb.append("\t\t\tResultSet results = statement.executeQuery();\n");
		sb.append("\t\t\twhile(results.next()) {\n");
		//logic
		sb.append("\t\t\t\tres = new " + Utils.UcFirst(singleName)+"();\n");
		for(Entry<String, String> field : fields.entrySet()) {
			String name = field.getKey(), type = field.getValue();
			sb.append("\t\t\t\tres.set" + Utils.UcFirst(name) + "(results.get" + ( type.toLowerCase().contains("varchar") ? "String" : Utils.UcFirst(type) ) + "(" + name.toUpperCase() + "));\n");
		}
		// end logic
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
	
	private String parseFields(Map<String, String> fields, Map<String, Integer> positions) {
		//loop
		String ret = "";
		for(Entry<String, Integer> position : positions.entrySet()) {
			// extract type, ValidName
			System.out.println(position.getKey());
			String type = fields.get(position.getKey()), name = Utils.UcFirst(position.getKey());
			Integer pos = position.getValue();
			// Type analysis
			if(type.toLowerCase().equals("date")) {
				ret += "\t\t\tlong secs = o.get" + name+"().getTime();\n";
				ret += "\t\t\tstatement.setDate(" + pos + ", new java.sql.Date(secs));\n";
			} else if(type.toUpperCase().contains("VARCHAR")) {
				ret += "\t\t\tstatement.setString(" + pos +", o.get" + name+"());\n";
			} else {
				ret += "\t\t\tstatement.set" + Utils.UcFirst(type) + "(" + pos +", o.get" + name +"());\n";
			}
		}
		return ret;
	}
}
