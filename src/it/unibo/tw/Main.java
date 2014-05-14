package it.unibo.tw;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

public class Main {
	
	private static JDBCGenerator jdbcGenerator;
	private static DAOGenerator daoGenerator;
	private static final String pkgFolder = "src/it/unibo/tw";
	private static final String pkg = "it.unibo.tw";
	private static Map<String, Map<String, String>> fieldsFromName;
	

	/* See tables.txt to see a valid syntax example */
	public static void main(String[] args) throws Exception {
		BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream("src/tables.txt")));
		fieldsFromName = new HashMap<String, Map<String, String>>();
		//parse ./tables.txt
		String line, tableName = null, tableNamePlural;
		Map<String, String> fields = new HashMap<String, String>();
		while((line = reader.readLine()) != null) {
			// skip whiteline
			if(line.equals("")) {
				continue;
			}
			
			if(line.indexOf('(') != -1) { // begin table definition
				tableName = Utils.UcFirst(line.split("\\(")[0].trim());
			}
			else if(line.indexOf(')') != -1) { // end table definition
				String[] lastLine = line.split("\\)")[1].trim().split("-");
				tableNamePlural = Utils.UcFirst(lastLine[0].trim());
				// add Long ID to field set (always present)
				fields.put("Id", "Long");
				// set constraints
				String constraints = lastLine[1].replace('<', '(').replace('>', ')');
				// JDBC
				jdbcGenerator = new JDBCGenerator(pkgFolder, pkg, tableName, fields, tableNamePlural, constraints);
				jdbcGenerator.writeBean();
				jdbcGenerator.writeManager();
				// DAO
				daoGenerator = new DAOGenerator(pkgFolder, pkg, tableName, fields, tableNamePlural, constraints);
				daoGenerator.writeDTO();
				daoGenerator.writeDAO();
				fieldsFromName.put(tableName.toLowerCase(), new HashMap<String, String>(fields));
				fields.clear();
			} else { //field
				String[] field = line.split(" ");
				// Name, Type [ FK REFERENCES <Tablename> ]
				String typeAndRef = String.join(" ", Arrays.copyOfRange(field, 1, field.length));
				fields.put( Utils.UcFirst(field[0].trim()), Utils.UcFirst(typeAndRef));
			}
		}
		reader.close();
		//		DAO 		//
		// Generate DAO Factory and DB2 implementation
		// read file from pgkpath.dao and find *DAO.java
		File[] files = new File(pkgFolder + "/dao/").listFiles();
		Queue<String> daos = new LinkedList<String>();
		for(File f : files) {
			if(f.getName().endsWith("DAO.java")) {
				daos.add(f.getName().replace(".java", ""));
			}
		}
		String[] daoArr = daos.toArray(new String[daos.size()]);
		daoGenerator.writeFactories(daoArr);
		// Create Main
		daoGenerator.writeMainTest(daoArr, fieldsFromName);
		System.out.println("Please press F5 in the ecplipse Project.\n"
				+ "Go into beans and dao folder and: Source -> generate hashCode() and equals()");
	}

}
