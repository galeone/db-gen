package it.unibo.tw;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.AbstractMap;

public class Main {
	
	private static JDBCGenerator jdbcGenerator;
	private static DAOGenerator daoGenerator;
	private static HibernateGenerator hibGenerator;
	private static final String pkgFolder = "src/it/unibo/tw";
	private static final String pkg = "it.unibo.tw";
	private static Map<String, Map<String, String>> fieldsFromName;
	private static List<Entry<String, String>> names = new ArrayList<Entry<String, String>>(); // singular, plural
	private static Map<String, String> constraintsByName = new HashMap<String, String>();
	private static Map<String, String> singlePlural = new HashMap<String, String>();
	
	private static String parseConstraint(String constraint, Map<String, String> fields) {
		String contraintType = constraint.substring(0, constraint.indexOf("("));
		String[] keys = constraint.replace(contraintType, "").replace(")", "").replace("(", "").trim().split(",");

		for(int i = 0; i< keys.length; i++) {
			String key = keys[i].trim();
			if(fields.get(key).contains("REFERENCES")) {
				keys[i] = "id" + key;
			}
		}
		return contraintType + " ( " + String.join(", ", keys) + " )";
	}
	

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
				String constraints = parseConstraint(lastLine[1].replace('<', '(').replace('>', ')'), fields);
				// JDBC
				jdbcGenerator = new JDBCGenerator(pkgFolder, pkg, tableName, fields, tableNamePlural, constraints, singlePlural);
				jdbcGenerator.writeBean();
				jdbcGenerator.writeManager();
				// DAO
				daoGenerator = new DAOGenerator(pkgFolder, pkg, tableName, fields, tableNamePlural, constraints, singlePlural);
				daoGenerator.writeDTO();
				daoGenerator.writeDAO();
				// Hibernate
				hibGenerator = new HibernateGenerator(pkgFolder, pkg, tableName, fields, tableNamePlural, constraints, singlePlural);
				hibGenerator.writeBeans();
				hibGenerator.writeCfgsXML();
				
				// save associations
				fieldsFromName.put(tableName.toLowerCase(), new HashMap<String, String>(fields));
				names.add( new AbstractMap.SimpleEntry<String, String>(tableName, tableNamePlural));
				constraintsByName.put(tableName.toLowerCase(), new String(constraints));
				singlePlural.put(tableName, tableNamePlural);
				// clear
				fields.clear();
			} else { //field
				String[] field = line.split(" ");
				// Name, Type [ FK REFERENCES <Tablename> ]
				String typeAndRef = String.join(" ", Arrays.copyOfRange(field, 1, field.length));
				fields.put( Utils.UcFirst(field[0].trim()), Utils.UcFirst(typeAndRef.trim()));
			}
		}
		reader.close();
		//		DAO 		//
		// Generate DAO Factory and DB2 implementation
		// read file from pgkpath.dao and find *DAO.java
		/*
		File[] files = new File(pkgFolder + "/dao/").listFiles();
		Queue<String> daos = new LinkedList<String>();
		for(File f : files) {
			if(f.getName().endsWith("DAO.java")) {
				daos.add(f.getName().replace(".java", ""));
			}
		}
		
		String[] daoArr = daos.toArray(new String[daos.size()]);
		*/
		List<Entry<String, String>> daoNames = new ArrayList<Entry<String, String>>(names.size());
		for(Entry<String, String> name : names) {
			daoNames.add(new AbstractMap.SimpleEntry<String,String>(name.getKey() + "DAO", ""));
		}
		daoGenerator.writeFactories(daoNames);
		// Create Main
		daoGenerator.writeMainTest(daoNames, fieldsFromName);
		
		// JDBC //
		// Generate Main
		jdbcGenerator.writeMainTest(names, fieldsFromName);
		
		// Hibernate //
		// Generate Main
		hibGenerator.writeMainTest(names, fieldsFromName, constraintsByName);
		System.out.println("Please press F5 in the eclipse Project.\n"
				+ "Go into models folders (model,dao,hibernate) and: Source -> generate hashCode() and equals()");
	}

}
