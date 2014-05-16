package it.unibo.tw;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

public class SQLGenerator {
	
	private Map<String, String> fields;
	private String pluralName, constraints, singleName;
	private String sqlStatements;
	private Map<String, Integer> insertPositions, updatePositions;
	private Map<String, String> singlePlural;
	
	public SQLGenerator(Map<String, String> fields, String pluralName, String singleName, String constraints, Map<String, String> singlePlural) {
		this.fields = fields;
		this.pluralName = pluralName;
		this.singleName = singleName;
		this.constraints = constraints;
		this.singlePlural = singlePlural;
		generate();
	}
	
	private String getTableNameStatement() {
		return "\tprivate static final String TABLE = \"" + pluralName.toLowerCase() + "\";\n";
	}
	
	private String getDropAndCreateStatements() {
		// drop
		StringBuilder sb = new StringBuilder("\tprivate static final String drop = \"DROP TABLE \" + TABLE;\n");
		// create
		sb.append("\tprivate static final String create = \"CREATE TABLE \" + TABLE + \" ( \" + \n");
		for(Entry<String, String> field : fields.entrySet()) { 
			String name = field.getKey().toUpperCase();
			String type = field.getValue().toUpperCase();
			if(type.equals("STRING")) {
				type = " VARCHAR(50)";
			} // date ok, double ok, boolean ok
			String elem = "";
			// Foreign key
			if(name.indexOf("ID") == 0){
				elem = name + "+ \" BIGINT NOT NULL ";
				if(name.equals("ID")) { //primary key
					elem += "PRIMARY KEY";
				}
				else { // foreign key
					elem += "REFERENCES " + singlePlural.get(field.getKey().replace("id", ""));
				}
			} else { // no  key
				elem = name + "+ \" " + type + " NOT NULL"; 
			}
			sb.append( "\t\t\t" + elem + ", \" +\n" );
		}
		sb.append("\t\t\t\"" + constraints.toUpperCase());
		sb.append(")\";\n\n");
		return sb.toString();
	}
	
	public String getHibernateModel() {
		StringBuilder sb = new StringBuilder();
		String id = "\t<id name=\"id\" column=\"id\">\n\t\t<generator class=\"increment\"/>\n\t</id>\n";
		for(Entry<String, String> field : fields.entrySet()) { 
			String name = field.getKey().toUpperCase();
			String type = field.getValue();

			// Foreign key
			if(name.indexOf("ID") == 0){
				if(name.equals("ID")) { //primary key
					// do nothing
				}
				else { // foreign key - new set
					sb.append("\t<!-- insert foreign key reference - set or whatelse -->\n");
				}
			} else { // no  key - element
				sb.append("\t<property column=\"" + name + "\" name=\"");
				sb.append(field.getKey() + "\" type=\"" + type.toLowerCase() + "\"");
				if(type.toLowerCase().equals("string")) {
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
		for(Entry<String, String> field : fields.entrySet()) {
			String name = field.getKey().toUpperCase();
			if(skipID && name.equals("ID")) {
				continue;
			}
			ret += "\tprivate static final String " + name + " = " + "\"" + name.toLowerCase() + "\";\n";
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
		//insert
		sb.append("\tprivate static final String insert = \"INSERT INTO \"");
		sb.append(" + TABLE + \" ( \" + ");
		String toAppend = "";
		int howMany = 0;
		// Map used to save ? position to generate insert method
		insertPositions = new HashMap<String, Integer>();
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
		updatePositions = new HashMap<String, Integer>();
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
		String ret = "";
		Random r = new Random();
		r.setSeed(r.nextLong());
		
		for(Entry<String, String> field : fields.entrySet()) {
			String name = Utils.UcFirst(field.getKey()), type = field.getValue().toLowerCase();
			if(type.equals("date")) {
				ret += "\t\tcal = Calendar.getInstance();\n";
				ret += "\t\tcal.set(2014,Calendar.JUNE," + (r.nextInt(27) +1) + ");\n";
				ret += "\t\t" + objName + ".set" + name + "(cal.getTime());\n";
			} else if(type.equals("string")) {
				ret += "\t\t" + objName + ".set" + name + "(\"" + name + (r.nextInt(30) + 1) + "\");\n";
			} else if(type.equals("long")) {
				ret += "\t\t" + objName + ".set" + name + "(" + (r.nextInt(100) +1) + "L);\n";
			} else if(type.equals("double")) {
				ret += "\t\t" + objName + ".set" + name + "(" + (r.nextInt(100) +1) + "d);\n";
			} else {
				ret += "\t\t" + objName + ".set" + name + "(" + (r.nextInt(100) +1) + ");\n";
			}
			r.setSeed(r.nextLong());
		}
		return ret;
	}
	
	public String getReadSetter(String postfix) {
		String ret = "\t\t\t\tres = new " + Utils.UcFirst(singleName)+ postfix + "();\n";
		for(Entry<String, String> field : fields.entrySet()) {
			String name = field.getKey(), type = field.getValue();
			ret += "\t\t\t\tres.set" + Utils.UcFirst(name) + "(results.get" + ( type.toLowerCase().contains("varchar") ? "String" : Utils.UcFirst(type) ) + "(" + name.toUpperCase() + "));\n";
		}
		return ret;
	}
	
	public String getReadSetter() {
		return getReadSetter("");
	}
	
	private String getStatementSetter(Map<String, String> fields, Map<String, Integer> positions) {
		//loop
		String ret = "";
		for(Entry<String, Integer> position : positions.entrySet()) {
			// extract type, ValidName
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
