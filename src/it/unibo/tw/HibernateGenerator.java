package it.unibo.tw;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Queue;

public class HibernateGenerator {
	
	private BeanGenerator beanGenerator;
	private String pkg, pkgFolder, tableName, pluralName, username, password;
	private Map<String, String> fields, singlePlural;
	private SQLGenerator sqlGen;
	
	public HibernateGenerator(String pkgFolder, String pkg, String tableName, Map<String, String> fields, String pluralName, String constraints, Map<String, String> singlePlural, String username, String password) {
		beanGenerator = new BeanGenerator(pkgFolder, pkg, "hibernate");
		this.pkg = pkg;
		this.pkgFolder = pkgFolder + "/hibernate/";
		this.fields = fields;
		this.tableName = tableName;
		this.pluralName = pluralName;
		this.singlePlural = singlePlural;
		this.sqlGen = new SQLGenerator(fields, pluralName, tableName, constraints, singlePlural);
		this.username = username;
		this.password = password;
	}
	
	public void writeBeans() throws Exception {
		beanGenerator.WriteBean(tableName, fields);
	}
	
	public void writeModelCfg() throws Exception {
		StringBuilder sb = new StringBuilder("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n<!DOCTYPE hibernate-mapping PUBLIC\n");
		sb.append("\t\"-//Hibernate/Hibernate Mapping DTD 3.0//EN\"\n");
		sb.append("\t\"http://hibernate.sourceforge.net/hibernate-mapping-3.0.dtd\">\n\n");
		sb.append("<hibernate-mapping>\n<class name=\"");
		sb.append(pkg + ".hibernate." + tableName + "\" table=\"" + pluralName.toLowerCase() + "\">\n");
		sb.append(sqlGen.getHibernateModel());
		sb.append("</class>\n</hibernate-mapping>");
		Utils.WriteFile(pkgFolder + tableName + ".hbm.xml", sb.toString());
	}
	
	public void writeCfgXML() throws Exception {
		StringBuilder sb = new StringBuilder("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n<!DOCTYPE hibernate-configuration PUBLIC\n");
		sb.append("\t\"-//Hibernate/Hibernate Configuration DTD 3.0//EN\"\n");
		sb.append("\t\"http://hibernate.sourceforge.net/hibernate-configuration-3.0.dtd\">\n\n");
		
		sb.append("<hibernate-configuration>\n\t<session-factory>\n\t\t<!-- Database connection settings -->\n");
		sb.append("\t\t<property name=\"connection.driver_class\">com.ibm.db2.jcc.DB2Driver</property>\n");
		sb.append("\t\t<property name=\"connection.url\">jdbc:db2://diva.deis.unibo.it:50000/tw_stud</property>\n");
		sb.append("\t\t<property name=\"connection.username\">"+ username + "</property>\n");
		sb.append("\t\t<property name=\"connection.password\">"+ password +"</property>\n");
		sb.append("\t\t<property name=\"connection.pool_size\">1</property>\n");
		sb.append("\t\t<property name=\"dialect\">org.hibernate.dialect.DB2Dialect</property>\n");
		sb.append("\t\t<!-- <property name=\"dialect\">org.hibernate.dialect.HSQLDialect</property> -->\n");
		sb.append("\t\t<!-- <property name=\"dialect\">org.hibernate.dialect.MySQLDialect</property> -->\n");
		sb.append("\t\t<property name=\"current_session_context_class\">thread</property>\n");
		sb.append("\t\t<property name=\"cache.provider_class\">org.hibernate.cache.NoCacheProvider</property>\n");
		sb.append("\t\t<property name=\"show_sql\">true</property>\n");
		sb.append("\t\t<property name=\"hbm2ddl.auto\">create</property>\n");
		// mapping loop
		File[] files = new File(pkgFolder).listFiles();
		for(File f : files) {
			if(f.getName().endsWith(".hbm.xml")) {
				sb.append("\t\t<mapping resource=\""  +pkg.replace(".", "/")+ "/hibernate/" + f.getName() + "\"/>\n");
			}
		}
		// end mapping loop
		sb.append("\t</session-factory>\n</hibernate-configuration>\n");
		
		Utils.WriteFile("src/hibernate.cfg.xml", sb.toString());
	}
	
	public void writeMainTest(List<Entry<String, String>> models, Map<String, Map<String, String>> fieldsFromName, Map<String, String> constraintsByName, Map <String, Entry<String, Entry<String, String>>> relations) throws IOException {
		StringBuilder sb = new StringBuilder("package " + pkg + ".hibernate;\n\n");
		sb.append("import java.sql.Connection;\n");
		sb.append("import java.sql.DriverManager;\n");
		sb.append("import java.sql.Statement;\n");
		sb.append("import java.util.Calendar;\n");
		sb.append("import org.hibernate.Query;\n");
		sb.append("import org.hibernate.Session;\n");
		sb.append("import org.hibernate.SessionFactory;\n");
		sb.append("import org.hibernate.Transaction;\n");
		sb.append("import org.hibernate.cfg.Configuration;\n\n");
		
		sb.append("public class HibernateMainTest {\n");	
		//sql statements
		Queue<String> tableNames = new LinkedList<String>();
		//remove repeated id declaration
		String constantsToAppend = "";
		boolean skipId = false;
		for(Entry<String, String> entry : models) {
			String singular = entry.getKey(), plural = entry.getValue();
			sqlGen = new SQLGenerator(fieldsFromName.get(singular.toLowerCase()), plural, singular, constraintsByName.get(singular.toLowerCase()), singlePlural);
			constantsToAppend += sqlGen.getConstantFieldsName(skipId);
			skipId = true;
		}

		sb.append(constantsToAppend);
		
		// append with correct order
		for(Entry<String, String> entry : models) {
			String singular = entry.getKey(), plural = entry.getValue();
			String constraints = constraintsByName.get(singular.toLowerCase());
			sqlGen = new SQLGenerator(fieldsFromName.get(singular.toLowerCase()), plural, singular, constraints, singlePlural);
			String newTableName = "TABLE_" +  plural.toUpperCase();
			tableNames.add(newTableName);
			String stmt = sqlGen.getTableNameDropAndCreateStatements().replace("TABLE =", newTableName+ " = ").replace("+ TABLE", "+ " +newTableName ).replace("String create", "String CREATE_" + newTableName).replace("String drop", "String DROP_" + newTableName);
			System.err.println(stmt);
			//if("".equals(constraints)) {
			//	stmt = stmt.substring(0, stmt.lastIndexOf(",")) + ")\";\n";
			//}
			sb.append(stmt);
		}
		sb.append("\tpublic static void main(String[] args) {\n\n");
		sb.append("\t\tSessionFactory sessionFactory = new Configuration().configure().buildSessionFactory();\n");
		sb.append("\t\tSession session = sessionFactory.openSession();\n");
		sb.append("\t\tTransaction tx = null;\n\n");
		sb.append("\t\ttry { // Table creation\n");
		sb.append("\t\t\t//Unibo Intranet DB2 tw_stud connection\n");
		sb.append("\t\t\t//Class.forName(\"COM.ibm.db2.jdbc.app.DB2Driver\").newInstance();\n");
		sb.append("\t\t\t//String url = \"jdbc:db2:tw_stud\";\n\n");
		sb.append("\t\t\t//Remote DB2 tw_stud connection\n");
		sb.append("\t\t\tClass.forName(\"com.ibm.db2.jcc.DB2Driver\");\n");
		sb.append("\t\t\tString url = \"jdbc:db2://diva.deis.unibo.it:50000/tw_stud\";\n\n");
		sb.append("\t\t\tString username = \""+username + "\";\n\t\t\tString password = \""+password+"\";\n\n");
		sb.append("\t\t\tConnection conn = DriverManager.getConnection(url, username, password);\n");
		sb.append("\t\t\tStatement st = conn.createStatement();\n\n");
		
		for(String s : tableNames) {
			sb.append("\t\t\ttry { //Try to execute sql\n");
			sb.append("\t\t\t\tSystem.out.println(\"Executing: \" +DROP_" + s+");\n");
			sb.append("\t\t\t\tst.executeUpdate(DROP_" + s + ");\n");
			sb.append("\t\t\t} catch(Exception e) {} //Table doesn't exist\n\n");
		}
		sb.append("\n");
		for(String s :tableNames) {
			sb.append("\t\t\tSystem.out.println(\"Executing: \" +CREATE_" + s+");\n");
			sb.append("\t\t\tst.executeUpdate(CREATE_"+ s + ");\n");
		}
		
		sb.append("\t\t} catch(Exception e) {\n");
		sb.append("\t\t\te.printStackTrace();\n");
		sb.append("\t\t} finally {\n\t\t\tsession.close();\n\t\t}\n\n");
		sb.append("\t\t// Insert entries\n");
		sb.append("\t\ttry {\n");
		sb.append("\t\t\tsession = sessionFactory.openSession();\n");
		sb.append("\t\t\ttx = session.beginTransaction();\n");
		sb.append("\t\t\tCalendar cal = null;\n\n");
		
		Map<String, LinkedList<String>> objTypeAssoc = new HashMap<String, LinkedList<String>>();
		// Type XXX elements(a1,a2, ecc)
		char varName = 'a';
		for(Entry<String, String> entry : models) {
			String singular = entry.getKey(), plural = entry.getValue();
			// Skip generation of Entity relations that in hibernate are handled with sets (N:M)
			if(relations.get(plural) != null) {
				continue;
			}
			sqlGen = new SQLGenerator(fieldsFromName.get(singular.toLowerCase()), plural, singular, constraintsByName.get(singular.toLowerCase()), singlePlural);
			objTypeAssoc.put(singular, new LinkedList<String>());
			
			// create 2 instances
			for(int i=0;i<2;++i) {
				String objName = varName + "" + i;
				objTypeAssoc.get(singular).add(objName);
				sb.append("\t\t\t" + singular + " "+ objName + " = new " + singular + "();\n");
				// Hibernate handle setId automatically
				String[] setofSet = sqlGen.getObjectInit(objName, fieldsFromName.get(singular.toLowerCase())).split("\n");
				List<String> relationSet = new ArrayList<String>();
				for(String set : setofSet) {
					boolean pk = set.contains("setId("); // skip primary key (hibernate handled)
					Pattern p = Pattern.compile("setId([^\\(]+)");
					Matcher m = p.matcher(set);
					if(!pk) {
						if(m.find()) { // 1:N relation, foreign key case
							// setIdXXX(precedentElementOfThisType.getId());
							sb.append("\t\t\t" + objName+ ".setId" + m.group(1) + "(");
							sb.append(objTypeAssoc.get(m.group(1)).pop());
							sb.append(".getId());\n");
							relationSet.add(m.group(1));
							System.out.println(m.group(1));
						} else {
							sb.append("\t");
							sb.append(set);
							sb.append("\n");
						}
					}
				}
				sb.append("\t\t\tsession.saveOrUpdate(" + objName + ");\n\n");
			}
			varName++;
		}
		sb.append("\t\t\ttx.commit();\n");
		sb.append("\t\t} catch(Exception e1) {\n");
		sb.append("\t\t\tif (tx != null) {\n");
		sb.append("\t\t\t\ttry {\n");
		sb.append("\t\t\t\t\ttx.rollback();\n");
		sb.append("\t\t\t\t} catch(Exception e2){\n");
		sb.append("\t\t\t\t\te2.printStackTrace();\n");
		sb.append("\t\t\t\t}\n\t\t\t}\n\t\t\te1.printStackTrace();\n\t\t} finally {\n");
		sb.append("\t\t\tsession.close();\n\t\t}\n\n");
		sb.append("\t\t//Queries\n\n");
		sb.append("\t\ttry {}\n\t\tcatch (Exception e1) {\n\t\t\te1.printStackTrace();\n");
		sb.append("\t\t} finally {\n\t\t\tsession.close();\n\t\t}\n\t}\n}");
		
		Utils.WriteFile(pkgFolder + "HibernateMainTest.java", sb.toString());
	}

}
