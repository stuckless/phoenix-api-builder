package phoenix.apibuilder;

import java.util.ArrayList;
import java.util.List;

public class APIMethod {
	public String javadoc;
	public String returnType;
	public List<APIParameter> parameters = new ArrayList<APIParameter>();
	public String name;
	public boolean hasThrows = false;
}
