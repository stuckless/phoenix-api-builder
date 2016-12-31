package phoenix.apibuilder;

import java.util.ArrayList;
import java.util.List;

public class APIClass {
	public String name;
	public String instanceClass = null;
	
	public boolean isProxy = false;
	public String proxyResolver = null;
	public String proxyPrefix = null;
	public String javaDoc = null;
	
	public List<APIMethod> methods = new ArrayList<APIMethod>();
}
