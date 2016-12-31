package phoenix.apibuilder;

import java.io.File;
import java.io.FileReader;
import java.util.Calendar;
import java.util.Map;
import java.util.Properties;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;

public class SearchReplaceTool {
	private static final String NAME = "Search Replace Tool";
	private static String VERSION = "1.1"; 
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		if (args==null || args.length == 0) {
			help("Missing Required Args");
		}

		String apiName = "api.properties";
		String propName = "replace.properties";
		String stvName = null;
		
		for (int i=0;i<args.length;i++) {
			if ("--replace".equals(args[i])) {
				propName = args[++i];
				continue;
			}

			if ("--api".equals(args[i])) {
				apiName = args[++i];
				continue;
			}
			
			if ("--stv".equals(args[i])) {
				stvName = args[++i];
				continue;
			}

			if ("-?".equals(args[i]) || "--help".equals(args[i])) {
				help(NAME + " - " + VERSION);
			}
		}

		File propFile = new File(propName);
		if (!propFile.exists()) {
			help("Missing: " + propName);
		}

		File apiFile = new File(apiName);
		if (!apiFile.exists()) {
			help("Missing: " + apiName);
		}
		
		File stvFile = new File(stvName);
		if (!stvFile.exists()) {
			help("Missing: " + stvName);
		}
		
		try {
			searchReplace(propFile, apiFile, stvFile);
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

	private static void out(String msg) {
		System.out.println(msg);
	}
	
	private static void searchReplace(File propFile, File apiFile, File stvFile) throws Throwable {
		out(NAME + " - " + VERSION);
		out("Reading current api properties");
		Properties api = new Properties();
		api.load(new FileReader(apiFile));

		out("Reading search/replace properties");
		Properties props = new Properties();
		props.load(new FileReader(propFile));
		
		out("Reading stv file: " + stvFile);
		String stv = FileUtils.readFileToString(stvFile);
		
		boolean updated = false;
		TreeSet<String> notused = new TreeSet<String>();
		TreeSet<String> manualremove = new TreeSet<String>();
		
		out("Scanning...");
		for (Map.Entry me : props.entrySet()) {
			if ("?".equals(me.getValue())) {
				notused.add((String) me.getKey());
				out("WARN: Ignored " + me.getKey());
				continue;
			}

			if ("-".equals(me.getValue())) {
				manualremove.add((String) me.getKey());
				out("WARN: Requires Manual Removal, API has removed: " + me.getKey());
				continue;
			}
			
			if (!api.containsKey(me.getValue())) {
				throw new Exception("Invalid NEW API Name: " + me.getValue() + " for legacy entry: " + me.getKey());
			}
			
			//out("Checking " + me.getKey());
			
			String newSTV = stv;
			String oldapi = (String) me.getKey()+"\\s*\\(";
			String newapi = (String) me.getValue() + "(";			
			newSTV = newSTV.replaceAll(oldapi, newapi);

			oldapi = (String) me.getKey()+"\\s*\\&quot;";
			newapi = (String) me.getValue() + "&quot;";			
			newSTV = newSTV.replaceAll(oldapi, newapi);

			oldapi = (String) me.getKey()+"\\s*\"";
			newapi = (String) me.getValue() + "\"";			
			newSTV = newSTV.replaceAll(oldapi, newapi);
			
			if (!newSTV.equals(stv)) {
				stv = newSTV;
				out("Replaced " + me.getKey() + " with " + me.getValue());
				updated=true;
			}
		}
		
		if (notused.size()>0) {
			out("\n=== No Substitutions for the following ===");
			for (String s: notused) {
				System.out.println(s);
			}
			out("");
		}

		if (manualremove.size()>0) {
			out("\n=== The Following APIs are no longer in use and have to be manually removed from the STV ===");
			for (String s: manualremove) {
				System.out.println(s);
			}
			out("");
		}
		
		if (updated) {
			File backupFile= new File(stvFile.getAbsolutePath() + "." + Calendar.getInstance().getTimeInMillis());
			out("Backing up stv file to: " + backupFile);
			FileUtils.copyFile(stvFile, backupFile);
			
			out("\nWriting changes to file: " + stvFile);
			FileUtils.writeStringToFile(stvFile, stv);
		} else {
			out("\nNo API changes were performed on file: " + stvFile);
		}
		
		out("\nFinding legacy api uses...");
		TreeSet<String> apis = new TreeSet<String>();
		Pattern p = Pattern.compile("(phoenix\\_api\\_[a-zA-Z0-9_]+)((\\s*\\()|(\\&quot;))");
		int i=0;
		for (LineIterator li = FileUtils.lineIterator(stvFile); li.hasNext();i++) {
			Matcher m = p.matcher(li.nextLine());
			if (m.find()) {
				System.out.printf("Legacy Phoenix API:  %s at line %s\n",m.group(1), i);
				apis.add(m.group(1));
			}
		}

		if (apis.size()>0) {
			out("Your STV contains invalid/deprecated Phoenix APIs :(");
			out("Consider adding the following to your replace.properties");
			for (String s: apis) {
				out(s + "=?");
			}
		} else {
			out("No legacy apis found in your stv :)");
		}
		
		
		out("\nValidating Phoenix api uses in STV file");
		boolean invalid = false;
		p = Pattern.compile("(phoenix\\_[^_]+\\_[a-zA-Z0-9_]+)((\\s*\\()|(\\&quot;))");
		i=0;
		for (LineIterator li = FileUtils.lineIterator(stvFile); li.hasNext();i++) {
			Matcher m = p.matcher(li.nextLine());
			if (m.find()) {
				String papi = m.group(1);
				if (!api.containsKey(papi)) {
					System.out.printf("Invalid Phoenix API: %s at line %s\n", papi, i);
					invalid=true;
				}
			}
		}
		if (invalid) {
			out("Your STV contained some invalid non-existent apis :(");
		} else {
			out("No invalid apis found in your stv :)");
		}

		out("\ndone.");
	}

	private static void help(String string) {
		System.out.println(string);
		System.out.println("java -jar SearchReaplceTool.jar --stv STV_FILE  [--replace REPLACE_FILE]");
		System.exit(1);
	}
}
