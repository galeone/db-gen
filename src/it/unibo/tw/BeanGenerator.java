package it.unibo.tw;

import java.io.File;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Map.Entry;

public class BeanGenerator {
	private final String pkgFolder, pkg, date = "import java.util.Date;\n";
	private final String newFolder;
	
	public BeanGenerator(String pkgFolder, String pkg, String newFolder) {
		this.newFolder = newFolder.toLowerCase().trim();
		this.pkg = "package " + pkg + "." + this.newFolder + ";\n";
		this.pkgFolder = pkgFolder;
	}
	
	private String getHelpers(Entry<String, String> field) {
		String pub = "\tpublic ", eofMethod = ";\n\t}\n\n";
		String type = field.getValue(), name = field.getKey();
		
		String lcFirstName = Utils.LcFirst(name),
			   ucFirstName = Utils.UcFirst(name);
		
		// Getter
		StringBuilder sb = new StringBuilder(pub);
		sb.append(type);
		sb.append(" get");
		sb.append(ucFirstName);
		sb.append("(){\n\t\treturn ");
		sb.append(lcFirstName);
		sb.append(eofMethod);
		
		// Setter
		sb.append(pub);
		sb.append("void set");
		sb.append(ucFirstName);
		sb.append("(");
		sb.append(type);
		sb.append(" ");
		sb.append(name);
		sb.append(") {\n\t\tthis.");
		sb.append(lcFirstName);
		sb.append(" = ");
		sb.append(name);
		sb.append(eofMethod);
		return sb.toString();
	}
	
	public void WriteBean(String tableName, Map<String, String> fields) throws Exception {
		// Folder
		String dest = pkgFolder + "/" + this.newFolder + "/";
		// dao require special naming (e.g. StudentDTO instead of Student)
		tableName =  tableName + (this.newFolder.equals("dao") ? "DTO" : "");
		
		File beansFolder = new File(dest);
		if(! beansFolder.exists()) {
			beansFolder.mkdir();
		}
		StringBuilder sb = new StringBuilder(pkg);
		sb.append("\nimport java.io.Serializable;");
		
		if(fields.containsValue("Date")) {
			sb.append("\n");
			sb.append(date);
		}
		
		sb.append("\n\npublic class ");
		sb.append(tableName);
		sb.append(" implements Serializable {\n");
		sb.append("\n\tprivate static final long serialVersionUID = 1L;\n\n");
		// fields
		// Create a Queue since I need to modify the Map in the iteration
		// and I can't do this if i'm iterating the map entrySet
		Queue<Entry<String, String>> queue = new LinkedList<Entry<String, String>>(fields.entrySet());
		for(Entry<String, String> field : queue) {
			String type = field.getValue(), name = field.getKey();
			sb.append("\tprivate ");
			String refTable = ""; // see last statement of for
			if(type.indexOf("FK REFERENCES") != -1) {
				type = "Long"; // Reference primary key
				String[] tmp = name.split(" ");
				String refType = Utils.UcFirst(tmp[tmp.length-1].trim());
				String tmpName = "id" +refType;
				//update value in set
				fields.remove(name);
				fields.put(tmpName, type);
				name = tmpName;
				refTable = " // FK REFERENCES " + refType;
			}
			sb.append(type);
			sb.append(" ");
			sb.append(Utils.LcFirst(name));
			sb.append(";"+refTable +"\n");
		}
		sb.append("\n");
		
		// methods
		//helpers
		for(Entry<String, String> field : fields.entrySet()) {
			sb.append(getHelpers(field));
		}
		sb.append("\n}");
		
		// write file
		String filename = dest + tableName + ".java";
		Utils.WriteFile(filename, sb.toString());

	}
}
