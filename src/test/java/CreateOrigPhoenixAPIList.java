package test;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;

public class CreateOrigPhoenixAPIList {
	public static void main(String args[]) throws IOException {
		//String s = "public static java.lang.Object GetProperty (java.lang.String arg0, java.lang.Object arg1) {";
		Pattern p = Pattern.compile("public\\s+static\\s+[^ ]+\\s+([^\\( ]+)");
		Properties props = new Properties();
		File f = new File("../Phoenix/src/main/java/phoenix/api.java");
		for (LineIterator li = FileUtils.lineIterator(f); li.hasNext(); ) {
			Matcher m = p.matcher(li.nextLine());
			if (m.find()) {
				props.setProperty("phoenix_api_" + m.group(1).trim(), "?");
			}
		}
		props.store(new FileWriter("replace.properties"), "OLD Base API");
	}
}
