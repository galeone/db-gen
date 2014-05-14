package it.unibo.tw;

import java.io.IOException;
import java.util.Map;

public class JDBCGenerator {
	
	private BeanGenerator beanGenerator;
	private ManagerGenerator managerGenerator;
	private String tableName, pluralName, constraints;
	private Map<String, String> fields;
	
	public JDBCGenerator(String pkgFolder, String pkg, String tableName, Map<String, String> fields, String pluralName, String constraints) {
		beanGenerator = new BeanGenerator(pkgFolder, pkg, "jdbc");
		managerGenerator = new ManagerGenerator(pkgFolder, pkg);
		this.tableName = tableName;
		this.fields = fields;
		this.constraints = constraints;
		this.pluralName = pluralName;
	}
	
	public void writeBean() throws Exception {
		beanGenerator.WriteBean(tableName, fields);
	}
	
	public void  writeManager() throws IOException {
		managerGenerator.writeManager(tableName, pluralName, fields, constraints);
	}

}
