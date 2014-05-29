package it.unibo.tw;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Queue;

import javax.xml.transform.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.*;

public class HibernateGenerator {

	private BeanGenerator beanGenerator;
	private String pkg, pkgFolder, tableName, pluralName, username, password;
	private Map<String, String> fields, singlePlural;
	private SQLGenerator sqlGen;

	public HibernateGenerator(String pkgFolder, String pkg, String tableName,
			Map<String, String> fields, String pluralName, String constraints,
			Map<String, String> singlePlural, String username, String password) {
		beanGenerator = new BeanGenerator(pkgFolder, pkg, "hibernate");
		this.pkg = pkg;
		this.pkgFolder = pkgFolder + "/hibernate/";
		this.fields = fields;
		this.tableName = tableName;
		this.pluralName = pluralName;
		this.singlePlural = singlePlural;
		this.sqlGen = new SQLGenerator(fields, pluralName, tableName,
				constraints, singlePlural);
		this.username = username;
		this.password = password;
	}

	public void writeBeans() throws Exception {
		beanGenerator.WriteBean(tableName, fields);
	}

	public void writeModelCfg() throws Exception {
		File mappingDTD = new File("src/hibernate-mapping-3.0.dtd");
		StringBuilder sb = new StringBuilder(
				"<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");
		sb.append("<!--\n<!DOCTYPE hibernate-mapping PUBLIC \"-//Hibernate/Hibernate Mapping DTD 3.0//EN\" \"http://hibernate.sourceforge.net/hibernate-mapping-3.0.dtd\">\n -->\n");
		sb.append("<!DOCTYPE hibernate-mapping SYSTEM\n\t\""+ mappingDTD.getAbsolutePath() + "\">\n\n");
		sb.append("<hibernate-mapping>\n<class name=\"");
		sb.append(pkg + ".hibernate." + tableName + "\" table=\""
				+ pluralName.toLowerCase() + "\">\n");
		sb.append(sqlGen.getHibernateModel());
		sb.append("</class>\n</hibernate-mapping>");
		Utils.WriteFile(pkgFolder + tableName + ".hbm.xml", sb.toString());
	}

	public void writeCfgXML() throws Exception {
		File confDTD = new File("src/hibernate-configuration-3.0.dtd");
		StringBuilder sb = new StringBuilder(
				"<?xml version=\"1.0\" encoding=\"utf-8\"?>\n<!-- <!DOCTYPE hibernate-configuration PUBLIC\n");
		sb.append("\t\"-//Hibernate/Hibernate Configuration DTD 3.0//EN\"\n");
		sb.append("\t\"http://hibernate.sourceforge.net/hibernate-configuration-3.0.dtd\"> -->\n");
		sb.append("<!DOCTYPE hibernate-configuration SYSTEM\n\t\""+confDTD.getAbsolutePath()+"\">\n\n");
		sb.append("<hibernate-configuration>\n\t<session-factory>\n\t\t<!-- Database connection settings -->\n");
		sb.append("\t\t<property name=\"connection.driver_class\">com.ibm.db2.jcc.DB2Driver</property>\n");
		sb.append("\t\t<property name=\"connection.url\">jdbc:db2://diva.deis.unibo.it:50000/tw_stud</property>\n");
		sb.append("\t\t<property name=\"connection.username\">" + username
				+ "</property>\n");
		sb.append("\t\t<property name=\"connection.password\">" + password
				+ "</property>\n");
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
		for (File f : files) {
			if (f.getName().endsWith(".hbm.xml")) {
				sb.append("\t\t<mapping resource=\"" + pkg.replace(".", "/")
						+ "/hibernate/" + f.getName() + "\"/>\n");
			}
		}
		// end mapping loop
		sb.append("\t</session-factory>\n</hibernate-configuration>\n");

		Utils.WriteFile("src/hibernate.cfg.xml", sb.toString());
	}

	public void writeMainTest(List<Entry<String, String>> models,
			Map<String, Map<String, String>> fieldsFromName,
			Map<String, String> constraintsByName,
			Map<String, List<Entry<String, Entry<String, String>>>> relations)
			throws IOException {
		StringBuilder sb = new StringBuilder("package " + pkg
				+ ".hibernate;\n\n");
		sb.append("import java.sql.Connection;\n");
		sb.append("import java.sql.DriverManager;\n");
		sb.append("import java.sql.Statement;\n");
		sb.append("import java.util.Calendar;\n");
		sb.append("import java.util.HashSet;\n");
		sb.append("import org.hibernate.Query;\n");
		sb.append("import org.hibernate.Session;\n");
		sb.append("import org.hibernate.SessionFactory;\n");
		sb.append("import org.hibernate.Transaction;\n");
		sb.append("import org.hibernate.cfg.Configuration;\n\n");

		sb.append("public class HibernateMainTest {\n");
		// sql statements
		Queue<String> tableNames = new LinkedList<String>();
		// remove repeated id declaration
		String constantsToAppend = "";
		boolean skipId = false;
		for (Entry<String, String> entry : models) {
			String singular = entry.getKey(), plural = entry.getValue();
			sqlGen = new SQLGenerator(
					fieldsFromName.get(singular.toLowerCase()), plural,
					singular, constraintsByName.get(singular.toLowerCase()),
					singlePlural);
			constantsToAppend += sqlGen.getConstantFieldsName(skipId);
			skipId = true;
		}

		sb.append(constantsToAppend);

		// append with correct order
		for (Entry<String, String> entry : models) {
			String singular = entry.getKey(), plural = entry.getValue();
			String constraints = constraintsByName.get(singular.toLowerCase());
			sqlGen = new SQLGenerator(
					fieldsFromName.get(singular.toLowerCase()), plural,
					singular, constraints, singlePlural);
			String newTableName = "TABLE_" + plural.toUpperCase();
			tableNames.add(newTableName);
			String stmt = sqlGen.getTableNameDropAndCreateStatements()
					.replace("TABLE =", newTableName + " = ")
					.replace("+ TABLE", "+ " + newTableName)
					.replace("String create", "String CREATE_" + newTableName)
					.replace("String drop", "String DROP_" + newTableName);
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
		sb.append("\t\t\tString username = \"" + username
				+ "\";\n\t\t\tString password = \"" + password + "\";\n\n");
		sb.append("\t\t\tConnection conn = DriverManager.getConnection(url, username, password);\n");
		sb.append("\t\t\tStatement st = conn.createStatement();\n\n");

		for (String s : tableNames) {
			sb.append("\t\t\ttry { //Try to execute sql\n");
			sb.append("\t\t\t\tSystem.out.println(\"Executing: \" +DROP_" + s
					+ ");\n");
			sb.append("\t\t\t\tst.executeUpdate(DROP_" + s + ");\n");
			sb.append("\t\t\t} catch(Exception e) {} //Table doesn't exist\n\n");
		}
		sb.append("\n");
		for (String s : tableNames) {
			sb.append("\t\t\tSystem.out.println(\"Executing: \" +CREATE_" + s
					+ ");\n");
			sb.append("\t\t\tst.executeUpdate(CREATE_" + s + ");\n");
		}

		sb.append("\t\t} catch(Exception e) {\n");
		sb.append("\t\t\te.printStackTrace();\n");
		sb.append("\t\t} finally {\n\t\t\tsession.close();\n\t\t}\n\n");
		sb.append("\t\t// Init session\n");
		sb.append("\t\tsessionFactory = new Configuration().configure().buildSessionFactory();\n");
		sb.append("\t\tsession = sessionFactory.openSession();\n");
		sb.append("\t\ttx = null;\n\n");
		sb.append("\t\t// Insert entries\n");
		sb.append("\t\ttry {\n");
		sb.append("\t\t\tsession = sessionFactory.openSession();\n");
		sb.append("\t\t\ttx = session.beginTransaction();\n");
		sb.append("\t\t\tCalendar cal = null;\n\n");

		Map<String, LinkedList<String>> objTypeAssoc = new HashMap<String, LinkedList<String>>(), objTypeAssocFull = new HashMap<String, LinkedList<String>>(); // required
																																								// for
																																								// sets
		// Type fff elements(a1,a2, ecc)
		char varName = 'a';
		for (Entry<String, String> entry : models) {
			String singular = entry.getKey(), plural = entry.getValue();
			// Skip generation of Entity relations that in hibernate are handled
			// with
			// sets (N:M) (join tables)
			boolean jump = false;
			for (String relT : new String[] { "n:n-mono", "n:n-bi" }) {
				jump = false;
				List<Entry<String, Entry<String, String>>> rel = relations
						.get(relT);
				if (rel != null) {
					for (Entry<String, Entry<String, String>> e : rel) {
						if (e.getKey().equals(plural)) {
							jump = true;
							break;
						}
					}
				}
				if (jump) {
					break;
				}
			}
			if (jump) {
				System.out.println("[!] Skipping: " + plural
						+ " in Hibernate main generation");
				continue;
			}

			sqlGen = new SQLGenerator(
					fieldsFromName.get(singular.toLowerCase()), plural,
					singular, constraintsByName.get(singular.toLowerCase()),
					singlePlural);
			objTypeAssoc.put(singular, new LinkedList<String>());
			objTypeAssocFull.put(singular, new LinkedList<String>());
			// create 2 instances
			for (int i = 0; i < 2; ++i) {
				String objName = varName + "" + i;
				objTypeAssoc.get(singular).add(objName);
				objTypeAssocFull.get(singular).add(objName);
				sb.append("\t\t\t" + singular + " " + objName + " = new "
						+ singular + "();\n");
				// Hibernate handle setId automatically
				String[] setofSet = sqlGen.getObjectInit(objName,
						fieldsFromName.get(singular.toLowerCase())).split("\n");
				List<String> relationSet = new ArrayList<String>();
				for (String set : setofSet) {
					boolean pk = set.contains("setId("); // skip primary key
															// (hibernate
															// handled)
					Pattern p = Pattern.compile("setId([^\\(]+)");
					Matcher m = p.matcher(set);
					if (!pk) {
						if (m.find()) { // 1:N relation, foreign key case
							// setIdXXX(precedentElementOfThisType.getId());
							sb.append("\t\t\t" + objName + ".setId"
									+ m.group(1) + "(");
							sb.append(objTypeAssoc.get(m.group(1)).pop());
							sb.append(".getId());\n");
							relationSet.add(m.group(1));
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
		// Handle relations
		sb.append("\t\t\t//Relations\n\n");

		// 1:n mono
		List<Entry<String, Entry<String, String>>> oneToManyMono = relations
				.get("1:n-mono");
		// If I instantiated an object that has a set, populate with other
		// object of that type
		if (oneToManyMono != null) {
			for (Entry<String, Entry<String, String>> rel : oneToManyMono) {
				String left = rel.getValue().getKey(), right = rel.getValue()
						.getValue();
				// A copy of rights is required since we remove the elements on
				// every iteration
				// and we have to reuse them
				List<String> rights = new ArrayList<String>(
						objTypeAssocFull.get(right));
				List<String> lefts = objTypeAssocFull.get(left);

				if (lefts != null && rights != null) {
					int size = lefts.size();
					int rightSize = rights.size();
					for (int i = 0; i < size && rightSize > 0; i++) {
						sb.append("\t\t\t");
						sb.append(lefts.get(i));
						sb.append(".set");
						sb.append(singlePlural.get(right));
						sb.append("(new HashSet<");
						sb.append(right);
						sb.append(">());\n");
						for (int k = 0; k < rightSize; ++k) {
							sb.append("\t\t\t");
							sb.append(lefts.get(i));
							sb.append(".get");
							sb.append(singlePlural.get(right));
							sb.append("().add(");
							// 1:n relation, must remove element every time in
							// order to assign
							// elements without sharing them
							sb.append(rights.get(0));
							rights.remove(0);
							sb.append(");\n");
						}
						rightSize = 0;
						sb.append("\t\t\tsession.saveOrUpdate(");
						sb.append(lefts.get(i));
						sb.append(");\n\n");
					}
				}
			}
		}

		// n:n-mono - the one created with a join table
		List<Entry<String, Entry<String, String>>> manyToManyMono = relations
				.get("n:n-mono");
		if (manyToManyMono != null) {
			for (Entry<String, Entry<String, String>> rel : manyToManyMono) {
				String left = rel.getValue().getKey(), right = rel.getValue()
						.getValue();
				// A copy of rights is required since we remove the elements on
				// every iteration
				// and we have to reuse them
				List<String> rights = new ArrayList<String>(
						objTypeAssocFull.get(right));
				List<String> lefts = objTypeAssocFull.get(left);

				if (lefts != null && rights != null) {
					int size = lefts.size();
					int rightSize = rights.size();
					for (int i = 0; i < size && rightSize > 0; i++) {
						sb.append("\t\t\t");
						sb.append(lefts.get(i));
						sb.append(".set");
						sb.append(singlePlural.get(right));
						sb.append("(new HashSet<");
						sb.append(right);
						sb.append(">());\n");
						for (int k = 0; k < rightSize; ++k) {
							sb.append("\t\t\t");
							sb.append(lefts.get(i));
							sb.append(".get");
							sb.append(singlePlural.get(right));
							sb.append("().add(");
							sb.append(rights.get(k));
							sb.append(");\n");
						}
						// do not add every time every element, just remove one
						rightSize--;
						sb.append("\t\t\tsession.saveOrUpdate(");
						sb.append(lefts.get(i));
						sb.append(");\n\n");
					}
				}
				// update every item (all) added to the set
				for (String el : rights) {
					sb.append("\t\t\tsession.saveOrUpdate(");
					sb.append(el);
					sb.append(");\n");
				}
			}
		}

		// n:n-bi - bidirectional relation between 2 entities, use a join table
		List<Entry<String, Entry<String, String>>> manyToManyBi = relations
				.get("n:n-bi");
		if (manyToManyBi != null) {
			sb.append("\n\t\t\t//Set property (inverse=\"false\") makes hibernate generate insert queries on the join table when saving the set\n");
			for (Entry<String, Entry<String, String>> rel : manyToManyBi) {
				String left = rel.getValue().getKey(), right = rel.getValue()
						.getValue();
				// A copy of rights is required since we remove the elements on
				// every iteration
				// and we have to reuse them
				List<String> rights = new ArrayList<String>(
						objTypeAssocFull.get(right));
				List<String> lefts = objTypeAssocFull.get(left);

				if (lefts != null && rights != null) {
					int size = lefts.size();
					int rightSize = rights.size();
					for (int i = 0; i < size && rightSize > 0; i++) {
						sb.append("\t\t\t");
						sb.append(lefts.get(i));
						sb.append(".set");
						sb.append(singlePlural.get(right));
						sb.append("(new HashSet<");
						sb.append(right);
						sb.append(">());\n");
						for (int k = 0; k < rightSize; ++k) {
							sb.append("\t\t\t");
							sb.append(lefts.get(i));
							sb.append(".get");
							sb.append(singlePlural.get(right));
							sb.append("().add(");
							sb.append(rights.get(k));
							sb.append(");\n");
						}
						// do not add every time every element, just remove one
						rightSize--;
						sb.append("\t\t\tsession.saveOrUpdate(");
						sb.append(lefts.get(i));
						sb.append(");\n\n");
					}
				}
				// update every item (all) added to the set
				for (String el : rights) {
					sb.append("\t\t\tsession.saveOrUpdate(");
					sb.append(el);
					sb.append(");\n");
				}
			}
		}

		// 1:n bi
		List<Entry<String, Entry<String, String>>> oneToManyBi = relations
				.get("1:n-bi");
		// If I instantiated an object that has a set, populate with other
		// object of that type
		if (oneToManyBi != null) {
			sb.append("\n\t\t\t// The set property (inverse=\"false\") makes hibernate generate update queries on the table");
			sb.append("\n\t\t\t// referenced by the set when saving the set\n");
			for (Entry<String, Entry<String, String>> rel : oneToManyBi) {
				String left = rel.getValue().getKey(), right = rel.getValue()
						.getValue();
				// A copy of rights is required since we remove the elements on
				// every iteration
				// and we have to reuse them
				List<String> rights = new ArrayList<String>(
						objTypeAssocFull.get(right));
				List<String> lefts = objTypeAssocFull.get(left);

				if (lefts != null && rights != null) {
					int size = lefts.size();
					int rightSize = rights.size();
					for (int i = 0; i < size && rightSize > 0; i++) {
						sb.append("\t\t\t");
						sb.append(lefts.get(i));
						sb.append(".set");
						sb.append(singlePlural.get(right));
						sb.append("(new HashSet<");
						sb.append(right);
						sb.append(">());\n");
						for (int k = 0; k < rightSize; ++k) {
							sb.append("\t\t\t");
							sb.append(lefts.get(i));
							sb.append(".get");
							sb.append(singlePlural.get(right));
							sb.append("().add(");
							// 1:n relation, must remove element every time in
							// order to assign
							// elements without sharing them
							sb.append(rights.get(0));
							rights.remove(0);
							sb.append(");\n");
						}
						rightSize = 0;
						sb.append("\t\t\tsession.saveOrUpdate(");
						sb.append(lefts.get(i));
						sb.append(");\n\n");
					}
				}
			}
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
		sb.append("\t\t//New session\n\t\tsession = sessionFactory.openSession();\n\n");
		sb.append("\t\ttry {}\n\t\tcatch (Exception e1) {\n\t\t\te1.printStackTrace();\n");
		sb.append("\t\t} finally {\n\t\t\tsession.close();\n\t\t}\n\t}\n}");

		Utils.WriteFile(pkgFolder + "HibernateMainTest.java", sb.toString());
	}

	public void updateCfgs(
			Map<String, List<Entry<String, Entry<String, String>>>> relations)
			throws Exception {
		List<Entry<String, Entry<String, String>>> oneToManyMono = relations
				.get("1:n-mono"), oneToManyBi = relations.get("1:n-bi"), manyToManyMono = relations
				.get("n:n-mono"), manyToManyBi = relations.get("n:n-bi");

		if (oneToManyMono != null) {
			// add to left-entity a set of right-entity
			for (Entry<String, Entry<String, String>> rel : oneToManyMono) {
				String left = rel.getValue().getKey(), right = rel.getValue()
						.getValue();
				// Create elements
				DocumentBuilderFactory dbf = DocumentBuilderFactory
						.newInstance();
				DocumentBuilder db = dbf.newDocumentBuilder();
				String filename = pkgFolder + left + ".hbm.xml";
				Document doc = db.parse(filename);

				Element set = doc.createElement("set");
				set.setAttribute("name", singlePlural.get(right).toLowerCase());

				Element key = doc.createElement("key");
				key.setAttribute("column", "id" + left);

				Element otm = doc.createElement("one-to-many");
				otm.setAttribute("class", pkg + ".hibernate." + right);

				set.appendChild(key);
				set.appendChild(otm);

				// Update
				updateDOM(filename, doc, set);
				updateBean(filename.replace(".hbm.xml", ".java"), right,
						singlePlural.get(right));
			}
		}

		if (manyToManyMono != null) {
			for (Entry<String, Entry<String, String>> rel : manyToManyMono) {
				String left = rel.getValue().getKey(), right = rel.getValue()
						.getValue();
				// Create elements
				DocumentBuilderFactory dbf = DocumentBuilderFactory
						.newInstance();
				DocumentBuilder db = dbf.newDocumentBuilder();
				String filename = pkgFolder + left + ".hbm.xml";
				Document doc = db.parse(filename);

				Element set = doc.createElement("set");
				set.setAttribute("name", singlePlural.get(right).toLowerCase());
				set.setAttribute("table", rel.getKey());

				Element key = doc.createElement("key");
				key.setAttribute("column", "id" + left);

				Element otm = doc.createElement("many-to-many");
				otm.setAttribute("class", pkg + ".hibernate." + right);
				otm.setAttribute("column", "id" + right);

				set.appendChild(key);
				set.appendChild(otm);

				// Update
				updateDOM(filename, doc, set);
				updateBean(filename.replace(".hbm.xml", ".java"), right,
						singlePlural.get(right));
			}
		}

		if (manyToManyBi != null) {
			// add to left-entity a set of right-entity
			for (Entry<String, Entry<String, String>> rel : manyToManyBi) {
				String left = rel.getValue().getKey(), right = rel.getValue()
						.getValue();
				// Create elements
				// left side
				DocumentBuilderFactory dbf = DocumentBuilderFactory
						.newInstance();
				DocumentBuilder db = dbf.newDocumentBuilder();
				String filename = pkgFolder + left + ".hbm.xml";
				Document doc = db.parse(filename);

				Element set = doc.createElement("set");
				set.setAttribute("name", singlePlural.get(right).toLowerCase());
				set.setAttribute("table", rel.getKey());
				// inverse="false"
				// when we add elements to the set of related element
				// hibernate will generate queries to update the join table
				// so we don't need to update other elements
				set.setAttribute("inverse", "false");

				Element key = doc.createElement("key");
				key.setAttribute("column", "id" + left);

				Element otm = doc.createElement("many-to-many");
				otm.setAttribute("class", pkg + ".hibernate." + right);
				otm.setAttribute("column", "id" + right);

				set.appendChild(key);
				set.appendChild(otm);

				// Update
				updateDOM(filename, doc, set);
				updateBean(filename.replace(".hbm.xml", ".java"), right,
						singlePlural.get(right));

				// right side
				filename = pkgFolder + right + ".hbm.xml";
				doc = db.parse(filename);

				set = doc.createElement("set");
				set.setAttribute("name", singlePlural.get(left).toLowerCase());
				set.setAttribute("table", rel.getKey());
				// inverse="true"
				set.setAttribute("inverse", "true");

				key = doc.createElement("key");
				key.setAttribute("column", "id" + right);

				otm = doc.createElement("many-to-many");
				otm.setAttribute("class", pkg + ".hibernate." + left);
				otm.setAttribute("column", "id" + left);

				set.appendChild(key);
				set.appendChild(otm);

				// Update
				updateDOM(filename, doc, set);
				updateBean(filename.replace(".hbm.xml", ".java"), left,
						singlePlural.get(left));
			}
		}

		// 1:n-bi
		if (oneToManyBi != null) {
			// add to left-entity a set of right-entity
			for (Entry<String, Entry<String, String>> rel : oneToManyBi) {
				String left = rel.getValue().getKey(), right = rel.getValue()
						.getValue();
				// Create elements
				// left side
				DocumentBuilderFactory dbf = DocumentBuilderFactory
						.newInstance();
				DocumentBuilder db = dbf.newDocumentBuilder();
				String filename = pkgFolder + left + ".hbm.xml";
				Document doc = db.parse(filename);

				Element set = doc.createElement("set");
				set.setAttribute("name", singlePlural.get(right).toLowerCase());
				// add elemets to set valid values on session save
				set.setAttribute("inverse", "false");

				Element key = doc.createElement("key");
				key.setAttribute("column", "id" + left);

				Element otm = doc.createElement("one-to-many");
				otm.setAttribute("class", pkg + ".hibernate." + right);

				set.appendChild(key);
				set.appendChild(otm);

				// Update
				updateDOM(filename, doc, set);
				// add set to the 1 (of 1:n) entity
				updateBean(filename.replace(".hbm.xml", ".java"), right,
						singlePlural.get(right));
			}
		}

	}

	private void updateDOM(String cfg, Document doc, Element e)
			throws Exception {
		NodeList properties = doc.getElementsByTagName("property");
		properties.item(0).getParentNode().insertBefore(e, properties.item(0));

		// save
		Transformer trans = TransformerFactory.newInstance().newTransformer();
		trans.setOutputProperty(OutputKeys.INDENT, "yes");
		DocumentType docType = doc.getDoctype();
		/*
		trans.setOutputProperty(OutputKeys.DOCTYPE_PUBLIC,
				docType.getPublicId());
		*/
		trans.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM,
				docType.getSystemId());

		StreamResult res = new StreamResult(new StringWriter());
		DOMSource source = new DOMSource(doc);
		trans.transform(source, res);

		Utils.WriteFile(cfg, res.getWriter().toString());
	}

	private void updateBean(String bean, String singular, String plural)
			throws Exception {
		BufferedReader reader = new BufferedReader(new InputStreamReader(
				new FileInputStream(new File(bean))));
		ArrayList<String> lines = new ArrayList<String>(50);
		String line = null;
		boolean importAdded = false, helpersAdded = false;
		while ((line = reader.readLine()) != null) {
			if (!importAdded && line.contains("import")) {
				importAdded = true;
				line = line + "\nimport java.util.Set;\n";
			} else if (!helpersAdded && line.matches("\\tpublic .*\\{")) {
				StringBuilder sb = new StringBuilder();
				String pluralLC = plural.toLowerCase();
				String singularUF = Utils.UcFirst(singular);

				sb.append("\tprivate Set<");
				sb.append(singularUF); // type
				sb.append("> ");
				sb.append(pluralLC);
				sb.append(";\n\n");
				sb.append("\tpublic void set");
				sb.append(Utils.UcFirst(plural));
				sb.append("(Set<");
				sb.append(singularUF);
				sb.append("> ");
				sb.append(pluralLC);
				sb.append(") {\n\t\tthis.");
				sb.append(pluralLC);
				sb.append(" = ");
				sb.append(pluralLC);
				sb.append(";\n\t}\n\n\tpublic Set<");
				sb.append(singularUF);
				sb.append("> get");
				sb.append(Utils.UcFirst(plural));
				sb.append("() {\n\t\treturn this.");
				sb.append(pluralLC);
				sb.append(";\n\t}\n\n");
				sb.append(line);
				line = sb.toString();
				helpersAdded = true;
			}
			lines.add(line);
		}
		reader.close();
		Utils.WriteFile(bean,
				Utils.joinString("\n", lines.toArray(new String[lines.size()])));
	}

}
