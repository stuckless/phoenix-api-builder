package phoenix.apibuilder;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.lang.StringUtils;

public class SearchReplaceGenerator implements IAPIGenerator {
	Properties props = new Properties();
	
	public SearchReplaceGenerator() {
	}

	@Override
	public void begin(File outputDir) throws IOException {
		File f = new File("replace.properties");
		if (f.exists()) {
			props.load(new FileReader(f));
		}
	}

	@Override
	public void end() throws IOException {
		props.store(new FileWriter(new File("target/replace.properties")), "Phoenix API Search/Replace");
		
		for (Map.Entry me: props.entrySet()) {
			if ("?".equals(me.getValue())) {
				System.out.println("Missing Replace: " + me.getKey());
			}
		}
	}

	@Override
	public void handleGroup(APIGroup group) throws IOException {
		for (APIClass cl : group.classes) {
			for (APIMethod m: cl.methods) {
				props.setProperty(getOrigAPIMethod(group, cl, m), getNewAPIMethod(group, cl, m));
				
			}
		}
	}

	private String getNewAPIMethod(APIGroup group, APIClass cl, APIMethod m) {
		return "phoenix_"+ group.groupId +"_" + StringUtils.capitalize(m.name);
	}

	private String getOrigAPIMethod(APIGroup group, APIClass cl, APIMethod m) {
		if (StringUtils.isEmpty(cl.proxyPrefix)) {
			return "phoenix_api_" + StringUtils.capitalize(m.name);
		} else {
			String name = StringUtils.capitalize(m.name);
            if (name.startsWith("Get")) {
                name = "Get" + cl.proxyPrefix + name.substring(3);
            } else if (name.startsWith("Set")) {
                name = "Set" + cl.proxyPrefix + name.substring(3);
            } else if (name.startsWith("Is")) {
                name = "Is" + cl.proxyPrefix + name.substring(2);
            }
            return "phoenix_api_" + name;
		}
	}
}
