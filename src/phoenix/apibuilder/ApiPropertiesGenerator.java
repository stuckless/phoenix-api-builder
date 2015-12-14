package phoenix.apibuilder;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Properties;

import org.apache.commons.lang.StringUtils;

public class ApiPropertiesGenerator implements IAPIGenerator {
	Properties props = new Properties();
	
	@Override
	public void begin(File outputDir) throws IOException {
	}

	@Override
	public void end() throws IOException {
		File propFile = new File("target/api.properties");
		if (propFile.getParentFile()==null) return;
		propFile.getParentFile().mkdirs();
		System.out.println("** PROP FILE: " + propFile.getAbsolutePath());
		props.store(new FileWriter(propFile), "Phoenix API property list");
	}

	@Override
	public void handleGroup(APIGroup group) throws IOException {
		for (APIClass cl : group.classes) {
			for (APIMethod m : cl.methods) {
				props.setProperty("phoenix_" + group.groupId + "_" + StringUtils.capitalize(m.name), "true");
			}
		}
	}
}
