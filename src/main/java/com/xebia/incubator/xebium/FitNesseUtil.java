package com.xebia.incubator.xebium;

import java.io.File;

public class FitNesseUtil {

	private FitNesseUtil() {
		// Static class
	}

	/**
	 * scriptName is something like
	 * '<a href="http://some.url/files/selenium/Suite">http://files/selenium/Suite</a>'.
	 * 
	 * @param scriptName
	 * @return
	 */
	public static String removeAnchorTag(String scriptName) {
		if (scriptName.startsWith("<a") && scriptName.endsWith("</a>")) {
			scriptName = scriptName.split(">", 2)[1].split("<", 2)[0];
		}
		return scriptName;
	}
	
	
	/**
	 * Obtain the script name from a wiki url. The URL may be in the format
	 * <code>http://files/selenium/Suite</code> and 
	 * <code>&lt;a href="/files/selenium/Suite"&gt;http://files/selenium/Suite&lt;/a&gt;</code>
	 * 
	 * @param scriptName
	 * @return a sane path name. Relative to the CWD.
	 */
	public static File asFile(final String scriptName) {
		String fileName = removeAnchorTag(scriptName).replaceAll("http:/", "FitNesseRoot");
		
		return new File(fileName);
	}

	
	/**
	 * Construct a <code>String</code> representation of a <code>String[]</code> the same way Selenium IDE does.
	 */
	public static String stringArrayToString(String[] stringArray) {
		StringBuilder b = new StringBuilder(64);
		boolean comma = false;
		for (String s: stringArray) {
			if (comma) {
				b.append(',');
			} else {
				comma = true;
			}
			b.append(s.replace("\\", "\\\\").replace(",", "\\,"));
		}
		return b.toString();
	}
	
}
