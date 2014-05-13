package it.unibo.tw;

public class Utils {
	public static String UcFirst(String sString) {
		return Character.toString(sString.charAt(0)).toUpperCase()+sString.substring(1);
	}
	
	public static String LcFirst(String sString) {
		return Character.toString(sString.charAt(0)).toLowerCase()+sString.substring(1);
	}
}
