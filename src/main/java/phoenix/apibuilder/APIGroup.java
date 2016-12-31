package phoenix.apibuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class APIGroup {
	public String groupId = null;
	public String groupClassName = null;
	public String groupPackage = null;
	public List<APIClass> classes = new ArrayList<APIClass>();
	public Set<String> imports = new TreeSet<String>();
}
