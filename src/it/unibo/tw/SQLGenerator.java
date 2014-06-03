package it.unibo.tw;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

public class SQLGenerator {

	private Map<String, String> fields;
	private String pluralName, constraints, singleName;
	private String sqlStatements;
	private Map<String, Integer> insertPositions, updatePositions;
	private Map<String, String> singlePlural;
	private Map<String, List<String>> typeIDsAssociations = new HashMap<String, List<String>>();
	private Integer randomNumber;
	private int idCount = 0;

	public SQLGenerator(Map<String, String> fields, String pluralName,
			String singleName, String constraints,
			Map<String, String> singlePlural) {
		this.fields = fields;
		this.pluralName = pluralName;
		this.singleName = singleName;
		this.constraints = constraints;
		this.singlePlural = singlePlural;
		randomNumber = new Random(System.currentTimeMillis()).nextInt(20);
		generate();
	}

	private String getTableNameStatement() {
		return "\tprivate static final String TABLE = \""
				+ pluralName.toLowerCase() + "\";\n";
	}

	private String getDropAndCreateStatements() {
		// drop
		StringBuilder sb = new StringBuilder(
				"\tprivate static final String drop = \"DROP TABLE \" + TABLE;\n");
		// create
		sb.append("\tprivate static final String create = \"CREATE TABLE \" + TABLE + \" ( \" + \n");
		String stmt = "";
		for (Entry<String, String> field : fields.entrySet()) {
			String name = field.getKey().toUpperCase();
			String type = field.getValue().toUpperCase();
			if (type.equals("STRING")) {
				type = " VARCHAR(50)";
			} else if (type.equals("BOOLEAN")) {
				type = "SMALLINT";
			}// date ok, double ok, long ok
			String elem = "";
			// Foreign key
			if (name.indexOf("ID") == 0) {
				elem = name + "+ \" BIGINT NOT NULL ";
				if (name.equals("ID")) { // primary key
					elem += "PRIMARY KEY";
				} else { // foreign key
					elem += "REFERENCES "
							+ singlePlural
									.get(field.getKey().replace("id", ""));
				}
			} else { // no key
				if (type.equals("LONG")) {
					type = "BIGINT";
				}
				elem = name + "+ \" " + type + " NOT NULL";
			}
			stmt += "\t\t\t" + elem + ", \" +\n";
		}

		if ("".equals(constraints)) {
			sb.append(stmt.substring(0, stmt.lastIndexOf(",")) + ")\";\n\n");
		} else {
			sb.append(stmt);
			sb.append("\t\t\t\"" + constraints.toUpperCase() + ")\";\n\n");
		}
		return sb.toString();
	}

	public String getHibernateModel() {
		StringBuilder sb = new StringBuilder();
		String id = "\t<id name=\"id\" column=\"id\">\n\t\t<generator class=\"increment\"/>\n\t</id>\n";
		for (Entry<String, String> field : fields.entrySet()) {
			String name = field.getKey().toUpperCase();
			String type = field.getValue();

			// Skip primary key
			if (name.equals("ID")) {
				continue;
			} else { // no primary key - element
				sb.append("\t<property column=\"" + name + "\" name=\"");
				sb.append(Utils.LcFirst(field.getKey()) + "\" type=\""
						+ type.toLowerCase() + "\"");
				if (type.toLowerCase().equals("string")) {
					sb.append(" length=\"50\"");
				}
				sb.append(" />\n");
			}
		}
		return id + sb.toString();
	}

	public String getTableNameDropAndCreateStatements() {
		StringBuilder sb = new StringBuilder(getTableNameStatement());
		sb.append(getDropAndCreateStatements());
		return sb.toString();
	}

	public String getConstantFieldsName(boolean skipID) {
		String ret = "";
		for (Entry<String, String> field : fields.entrySet()) {
			String name = field.getKey().toUpperCase();
			if (skipID && name.equals("ID")) {
				continue;
			}
			ret += "\tprivate static final String " + name + " = " + "\""
					+ name.toLowerCase() + "\";\n";
		}
		// Handle special ID (not generated but specified)
		if (!skipID && constraints.contains("PRIMARY KEY")) {
			String id = constraints.replace("PRIMARY KEY", "").trim()
					.toLowerCase();
			ret += "\tprivate static final String ID = \"" + id + "\";\n";
			idCount = id.split(",").length;
		}
		return ret;
	}

	public String getConstantFieldsName() {
		return getConstantFieldsName(false);
	}

	private void generate() {
		StringBuilder sb = new StringBuilder();
		sb.append(getTableNameStatement());
		sb.append(getConstantFieldsName());

		sb.append("\n\t// SQL STATEMENTS\n");
		// insert
		sb.append("\tprivate static final String insert = \"INSERT INTO \"");
		sb.append(" + TABLE + \" ( \" + ");
		String toAppend = "";
		int howMany = 0;
		// Map used to save ? position to generate insert method
		insertPositions = new HashMap<String, Integer>();
		for (Entry<String, String> field : fields.entrySet()) {
			String name = field.getKey().toUpperCase();
			toAppend += name + "+ \", \" + ";
			howMany++;
			insertPositions.put(field.getKey(), howMany);
		}
		sb.append(toAppend.substring(0, toAppend.length() - 6) + ") VALUES(");
		for (int i = 0; i < howMany - 1; ++i) {
			sb.append("?,");
		}
		sb.append("?) \";\n");
		// delete
		sb.append("\tprivate static final String delete = \"DELETE FROM \"");
		sb.append(" + TABLE + \" WHERE \" + ID + \" = ");
		if (idCount != 0) {
			sb.append("(");
			for (int i = 0; i < idCount - 1; i++) {
				sb.append("?, ");
			}
			sb.append("? )\";\n");
		} else {
			sb.append("?\";\n");
		}
		// update
		sb.append("\tprivate static final String update = \"UPDATE \" + TABLE + \" SET ");
		toAppend = "";
		howMany = 0;
		updatePositions = new HashMap<String, Integer>();
		for (Entry<String, String> field : fields.entrySet()) {
			String name = field.getKey().toUpperCase();
			if (name.equals("ID")) // condition, don't set
				continue;
			toAppend += "\" + " + name + " + \" = ?,";
			howMany++;
			updatePositions.put(field.getKey(), howMany);
		}
		sb.append(toAppend.substring(0, toAppend.length() - 1));
		sb.append(" WHERE \" + ID + \" = ");
		if (idCount != 0) {
			sb.append("(");
			for (int i = 0; i < idCount - 1; i++) {
				sb.append("?, ");
			}
			sb.append("? )\";\n");
		} else {
			sb.append("?\";\n");
		}
		howMany++;
		// updatePositions.put("ID", howMany);
		// read by id
		sb.append("\tprivate static final String read_by_id = \"SELECT * FROM \" + TABLE + \" WHERE \" + ID + \"  = ");
		if (idCount != 0) {
			sb.append("(");
			for (int i = 0; i < idCount - 1; i++) {
				sb.append("?, ");
			}
			sb.append("? )\";\n");
		} else {
			sb.append("?\";\n");
		}
		// Drop and create
		sb.append(getDropAndCreateStatements());

		this.sqlStatements = sb.toString();

	}

	public String getSQLConstants() {
		return this.sqlStatements;
	}

	public String getUpdateSetter() {
		return getStatementSetter(fields, updatePositions);
	}

	public String getInsertSetter() {
		return getStatementSetter(fields, insertPositions);
	}

	public String getObjectInit(String objName, Map<String, String> fields) {
		return getObjectInit(objName, fields, "");
	}

	public String getObjectInit(String objName, Map<String, String> fields,
			String objType) {
		String ret = "";
		// References are valid only for dao and jdbc, hibernate needs other
		boolean hibernate = objType.equals("");

		for (Entry<String, String> field : fields.entrySet()) {
			String name = Utils.UcFirst(field.getKey()), type = field.getValue().toLowerCase();
			String id = "" + (randomNumber++);
			// save id value for this type
			if (!hibernate && name.toLowerCase().equals("id")) {
				if (typeIDsAssociations.get(objType) == null) {
					typeIDsAssociations.put(objType, new LinkedList<String>());
				}
				typeIDsAssociations.get(objType).add(id);
			}
			ret += "\t\t";
			String common = objName + ".set" + name + "(";
			if (type.equals("date")) {
				ret += "cal = Calendar.getInstance();\n";
				ret += "\t\tcal.set(2014,Calendar.JUNE,"
						+ (new Random(System.currentTimeMillis()).nextInt(27) + 1)
						+ ");\n";
				ret += "\t\t" + common + "cal.getTime());\n";
			} else if (type.equals("string")) {
				ret += common + "\"" + name + (randomNumber++) + "\");\n";
			} else if (type.equals("long")) { // ids always long
				ret += common;
				// setIdXXX
				String nameLC = name.toLowerCase();
				if (!hibernate && nameLC.startsWith("id")
						&& !nameLC.equals("id")) {
					List<String> tida = typeIDsAssociations.get(name
							.substring(2));
					if (tida == null) {
						System.err
								.println("[!!] Type-ID Associations not found for: "
										+ name.substring(2));
						System.err
								.println("[!!] Please check the table definition order");
						System.exit(1);
					}
					id = tida.get(0);
					tida.remove(0);
				}
				ret += id + "L);\n";
			} else if (type.equals("double")) {
				ret += common + (randomNumber++) + "d);\n";
			} else if (type.equals("boolean")) {
				ret += common + (++randomNumber % 2 == 0 ? "false" : "true")
						+ ");\n";
			} else {
				ret += common + (randomNumber++) + ");\n";
			}
		}
		return ret;
	}

	public String getReadSetter(String postfix) {
		String ret = "\t\t\t\tres = new " + Utils.UcFirst(singleName) + postfix
				+ "();\n";
		for (Entry<String, String> field : fields.entrySet()) {
			String name = field.getKey(), type = field.getValue();
			ret += "\t\t\t\tres.set"
					+ Utils.UcFirst(name)
					+ "(results.get"
					+ (type.toLowerCase().contains("varchar") ? "String"
							: Utils.UcFirst(type)) + "(" + name.toUpperCase()
					+ "));\n";
		}
		return ret;
	}

	public String getReadSetter() {
		return getReadSetter("");
	}

	private String getStatementSetter(Map<String, String> fields,
			Map<String, Integer> positions) {
		// loop
		String ret = "";
		for (Entry<String, Integer> position : positions.entrySet()) {
			// extract type, ValidName
			String type = fields.get(position.getKey()), name = Utils
					.UcFirst(position.getKey());
			Integer pos = position.getValue();
			// Type analysis
			if (type.toLowerCase().equals("date")) {
				ret += "\t\t\tlong secs = o.get" + name + "().getTime();\n";
				ret += "\t\t\tstatement.setDate(" + pos
						+ ", new java.sql.Date(secs));\n";
			} else if (type.toUpperCase().contains("VARCHAR")) {
				ret += "\t\t\tstatement.setString(" + pos + ", o.get" + name
						+ "());\n";
			} else {
				ret += "\t\t\tstatement.set" + Utils.UcFirst(type) + "(" + pos
						+ ", o.get" + name + "());\n";
			}
		}
		return ret;
	}
}
