package it.unibo.tw;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;

public class Utils {
	private static int createdFiles = 1;

	public static String UcFirst(String sString) {
		return Character.toString(sString.charAt(0)).toUpperCase()
				+ sString.substring(1);
	}

	public static String LcFirst(String sString) {
		return Character.toString(sString.charAt(0)).toLowerCase()
				+ sString.substring(1);
	}

	public static void WriteFile(String filename, String content)
			throws IOException {
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
				new FileOutputStream(filename)));
		writer.write(content);
		writer.close();
		System.out.println("[" + createdFiles + "] Created: " + filename);
		createdFiles++;
	}

	// Support for java7
	public static String joinString(String delimiter, String[] strings) {
		ArrayList<String> list = new ArrayList<String>(Arrays.asList(strings));
		list.removeAll(Arrays.asList("", null));

		int len = list.size();
		if (len == 1) {
			return list.get(0);
		}

		StringBuilder sb = new StringBuilder();
		--len;
		for (int i = 0; i < len; i++) {
			sb.append(list.get(i));
			sb.append(delimiter);
		}
		sb.append(list.get(len));

		return sb.toString();
	}
}
