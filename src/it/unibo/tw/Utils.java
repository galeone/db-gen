package it.unibo.tw;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

public class Utils {
	private static int createdFiles = 1;
	
	public static String UcFirst(String sString) {
		return Character.toString(sString.charAt(0)).toUpperCase()+sString.substring(1);
	}
	
	public static String LcFirst(String sString) {
		return Character.toString(sString.charAt(0)).toLowerCase()+sString.substring(1);
	}
	
	public static void WriteFile(String filename, String content) throws IOException {
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filename)));
		writer.write(content);
		writer.close();
		System.out.println("[" + createdFiles + "] Created: " + filename);
		createdFiles++;
	}
}
