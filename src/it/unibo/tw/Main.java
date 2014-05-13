package it.unibo.tw;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class Main {
	
	private static BeanGenerator beanGenerator;
	private static ManagerGenerator managerGenerator;

	/* See tables.txt to see a valid syntax example */
	public static void main(String[] args) throws Exception {
		beanGenerator = new BeanGenerator("src/it/unibo/tw", "it.unibo.tw");
		managerGenerator = new ManagerGenerator("src/it/unibo/tw", "it.unibo.tw");
		BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream("src/tables.txt")));

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
				// New Java Bean
				beanGenerator.WriteBean(tableName, fields);
				// New Manager
				managerGenerator.writeRepository(tableName, tableNamePlural, fields, lastLine[1].replace('<', '(').replace('>', ')'));
				fields.clear();
			} else { //field
				String[] field = line.split(" ");
				// Name, Type [ FK REFERENCES <Tablename> ]
				String typeAndRef = String.join(" ", Arrays.copyOfRange(field, 1, field.length));
				fields.put( Utils.UcFirst(field[0].trim()), Utils.UcFirst(typeAndRef));
			}
		}
		reader.close();
		System.out.println("Please press F5 in the ecplipse Project.\nNow import the created beans into the project and for every bean do: Source -> generate hashCode() and equals()");
	}

}
