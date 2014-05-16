package it.unibo.tw;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class JDBCGenerator {
	
	private BeanGenerator beanGenerator;
	private ManagerGenerator managerGenerator;
	private String tableName, pluralName, constraints, pkg, pkgFolder;
	private Map<String, String> fields, singlePlural;
	private SQLGenerator sqlGen;
	
	public JDBCGenerator(String pkgFolder, String pkg, String tableName, Map<String, String> fields, String pluralName, String constraints, Map<String, String> singlePlural) {
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

	}
	
	public void writeBean() throws Exception {
		beanGenerator.WriteBean(tableName, fields);
	}
	
	public void  writeManager() throws IOException {
		managerGenerator.writeManager(tableName, pluralName, fields, constraints, singlePlural);
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
				sb.append(sqlGen.getObjectInit(objName, fields));
				sb.append("\t\t" + var + ".insert(" + objName + ");\n\n");
			}
			varName++;
		}
		
		sb.append("\n\t}\n\n\t//Test && Support methods\n\n}");
		String filename = pkgFolder + "/db/JDBCMainTest.java";
		Utils.WriteFile(filename, sb.toString());
	}

}
