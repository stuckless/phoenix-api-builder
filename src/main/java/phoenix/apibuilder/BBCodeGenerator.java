package phoenix.apibuilder;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import org.apache.commons.lang.StringUtils;

public class BBCodeGenerator implements IAPIGenerator {
	File api = null; 
	PrintWriter pw = null;
	
	@Override
	public void begin(File outDir) throws IOException {
		// ignore the outDir, since we need to create this in the target area
		api = new File("target/api.txt");
		pw = new PrintWriter(new FileWriter(api));
		pw.println("[b]Phoenix API[/b]");
		
		System.out.println("Creating Api Text: " + api);
	}

	@Override
	public void end() {
		pw.flush();
		pw.close();
	}

	@Override
	public void handleGroup(APIGroup group) throws IOException {
		System.out.println("Processing Group " + group.groupId + " for BBCode API Doc");
		pw.printf("[b]API Group: %s[/b]\n", group.groupId);
		for (APIClass cl : group.classes) {
			for (APIMethod m : cl.methods) {
				pw.printf("phoenix_%s_%s(%s): %s\n", group.groupId, StringUtils.capitalize(m.name), buildArgs(m), buildReturn(m));
			}
		}
		
		pw.println();
	}

	private String buildArgs(APIMethod m) {
		if (m.parameters.size()==0) return "";
		StringBuffer sb = new StringBuffer();
		for (APIParameter p : m.parameters) {
			if (sb.length()>0) sb.append(", ");
			sb.append(p.type).append(" ").append(p.name);
		}
		return sb.toString();
	}

	private String buildReturn(APIMethod m) {
		if (m.returnType==null) return "Void";
		return m.returnType;
	}
}
