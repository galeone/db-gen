package it.unibo.tw;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.AbstractMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

public class Main {

	private static JDBCGenerator jdbcGenerator;
	private static DAOGenerator daoGenerator;
	private static HibernateGenerator hibGenerator;
	private static final String pkgFolder = "src/it/unibo/tw";
	private static final String pkg = "it.unibo.tw";
	private static Map<String, Map<String, String>> fieldsFromName = new HashMap<String, Map<String, String>>();
	private static List<Entry<String, String>> names = new ArrayList<Entry<String, String>>();
	private static Map<String, List<Entry<String, Entry<String, String>>>> relations = new HashMap<String, List<Entry<String, Entry<String, String>>>>();
	private static Map<String, String> constraintsByName = new HashMap<String, String>();
	private static Map<String, String> singlePlural = new HashMap<String, String>();
	private static String tableName, tableNamePlural, constraints;
	private static Map<String, String> fields = new HashMap<String, String>();
	private static String line, username, password;

	private static void parseConstraint() {
		constraints = "";
		String[] lastLine = line.split("\\)")[1].trim().split("-");
		if (lastLine.length < 2) {
			return;
		}
		
		String constraints_line = lastLine[1].replace('<', '(').replace('>', ')');		
		String[] constraintsArray = constraints_line.split("&");
		for (int i = 0; i < constraintsArray.length; ++i) 
		{
			String constraint = constraintsArray[i];
			String contraintType = constraint.substring(0, constraint.indexOf("("));
			String[] keys = constraint.replace(contraintType, "").replace(")", "").replace("(", "").trim().split(",");

			for (int j = 0; j < keys.length; j++) 
			{
				String key = Utils.UcFirst(keys[j].trim());
				String constr = fields.get(key);
				
				if (constr == null) 
				{
					System.err.println(key + " is not a valid field in constraint for table: " + tableName);
					System.exit(1);
				}
				
				if (fields.get(key).contains("REFERENCES")) 
				{
					keys[j] = "id" + key;
				}
			}
			
			constraints += contraintType + " ( " + Utils.joinString(", ", keys)	+ " )";
			if (i < constraintsArray.length - 1) 
			{
				constraints += ", ";
			}
		}
	}

	private static void getTableName() {
		tableName = Utils.UcFirst(line.split("\\(")[0].trim());
	}

	private static void getTableNamePlural() {
		String[] lastLine = line.split("\\)")[1].trim().split("-");
		tableNamePlural = Utils.UcFirst(lastLine[0].trim());
	}

	private static void saveField() {
		String[] field = line.trim().split(" ");
		// Name, Type [ FK REFERENCES <Tablename> ]
		String typeAndRef = Utils.UcFirst(Utils.joinString(" ",
				Arrays.copyOfRange(field, 1, field.length)).trim());
		fields.put(Utils.UcFirst(field[0].trim()), typeAndRef);
	}

	private static void saveAssociations() {
		// save associations
		fieldsFromName.put(tableName.toLowerCase(),
				new HashMap<String, String>(fields));
		names.add(new AbstractMap.SimpleEntry<String, String>(tableName,
				tableNamePlural));
		constraintsByName.put(tableName.toLowerCase(), new String(constraints));
		singlePlural.put(tableName, tableNamePlural);
	}
	
	private static void generateEntity(boolean skipHibernate) throws Exception {
		generateEntity(skipHibernate, false);
	}


	private static void generateEntity(boolean skipHibernate, boolean joinTable) throws Exception {
		// JDBC
		jdbcGenerator = new JDBCGenerator(pkgFolder, pkg, tableName, fields,
				tableNamePlural, constraints, singlePlural, username, password);
		jdbcGenerator.writeBean();
		jdbcGenerator.writeManager(joinTable);
		// DAO
		daoGenerator = new DAOGenerator(pkgFolder, pkg, tableName, fields,
				tableNamePlural, constraints, singlePlural, username, password);
		daoGenerator.writeDTO();
		daoGenerator.writeDAO();
		// Hibernate
		if (!skipHibernate) {
			hibGenerator = new HibernateGenerator(pkgFolder, pkg, tableName,
					fields, tableNamePlural, constraints, singlePlural,
					username, password);
			hibGenerator.writeBeans();
			hibGenerator.writeModelCfg();
		}
	}

	/* See tables.txt to see a valid syntax example */
	public static void main(String[] args) throws Exception {
		BufferedReader reader = new BufferedReader(new InputStreamReader(
				new FileInputStream("src/tables.txt")));
		BufferedReader r2 = new BufferedReader(new InputStreamReader(
				new FileInputStream("src/config.json")));
		String json = "";
		while ((line = r2.readLine()) != null) {
			json += line;
		}
		r2.close();

		JSONObject data = (JSONObject) JSONValue.parse(json);
		username = (String) data.get("username");
		password = (String) data.get("password");
		System.out.println("[!] Credentials read");

		// parse ./tables.txt
		while ((line = reader.readLine()) != null) {
			// skip white lines and comments --
			if (line.equals("") || line.startsWith("--")) {
				continue;
			}

			if (line.contains(":")) { // relations - last lines
				Pattern p = Pattern
						.compile(
								"\\s*([a-z0-9]:[a-z0-9]-(?:mono|bi))\\s*<([a-z0-9_]+)\\s*,\\s*([a-z0-9_]+)\\s*>",
								Pattern.CASE_INSENSITIVE);
				Matcher m = p.matcher(line);
				if (m.find()) {
					String relationType = m.group(1).toLowerCase();
					String leftEntity = m.group(2);
					String rightEntity = m.group(3);
					// contains table definition
					if (line.contains("{")) {
						while (!(line = reader.readLine()).contains("}")) {
							if (line.contains("(")) {
								getTableName();
							} else if (line.contains(")")) {
								getTableNamePlural();
								parseConstraint();
								// skip hibernate generation, since relation
								// tables
								// are join tables
								generateEntity(true, true);
								saveAssociations();
								// clear
								fields.clear();
							} else {
								saveField();
							}
						}
						// save relation type and entities involved
						List<Entry<String, Entry<String, String>>> rel = relations
								.get(relationType);
						if (rel == null) {
							relations
									.put(relationType,
											new LinkedList<Entry<String, Entry<String, String>>>());
						}
						relations
								.get(relationType)
								.add(new AbstractMap.SimpleEntry<String, Entry<String, String>>(
										tableNamePlural,
										new AbstractMap.SimpleEntry<String, String>(
												leftEntity, rightEntity)));
					}
				} else {
					System.err.println("Syntax error");
					System.exit(1);
				}

			} else if (line.contains("(")) { // begin table definition
				getTableName();
			} else if (line.contains(")")) { // end table definition
				getTableNamePlural();
				// add Long ID to field set (always present)
				fields.put("Id", "Long");
				// set constraints
				parseConstraint();
				// Generate
				generateEntity(false);
				// save associations
				saveAssociations();
				// clear
				fields.clear();
			} else { // field
				saveField();
			}
		}
		reader.close();
		// DAO //
		List<Entry<String, String>> daoNames = new ArrayList<Entry<String, String>>(
				names.size());
		for (Entry<String, String> name : names) {
			daoNames.add(new AbstractMap.SimpleEntry<String, String>(name
					.getKey() + "DAO", ""));
		}
		// Generate Factories
		daoGenerator.writeFactories(daoNames);
		// Generate Main
		daoGenerator.writeMainTest(daoNames, fieldsFromName);

		// JDBC //
		// Generate DataSource
		jdbcGenerator.writeDataSource();
		// Generate Main
		jdbcGenerator.writeMainTest(names, fieldsFromName);

		// Hibernate //
		// Generate hibernate.cfg.xml
		hibGenerator.writeCfgXML();
		// Add relations to cfgs
		hibGenerator.updateCfgs(relations);
		// Generate Main
		hibGenerator.writeMainTest(names, fieldsFromName, constraintsByName,
				relations);

		// End
		System.out.println("\n1) Press F5 in the eclipse Project.\n"
				+ "2) Go into models folders (model,dao,hibernate) and for every bean do: Source -> generate hashCode() and equals().\n"
				+ "3) Uncomment the DTD declaration in the hibernate configuration files and remove the local one, if you don't need to use it.\n"
				+ "4) Update the connection string in generated files (use intranet or whatever you need)\n");
		System.out.println("[!] Good luck ;)");
	}

}
