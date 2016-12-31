package phoenix.apibuilder;

import java.io.File;
import java.io.IOException;

public interface IAPIGenerator {
	/**
	 * begin is called once for the entire collection of files
	 * @throws IOException 
	 */
	public void begin(File outputDir) throws IOException;
	
	/**
	 * called once for each group
	 * 
	 * @param group
	 * @throws IOException
	 */
	public void handleGroup(APIGroup group) throws IOException;
	
	/**
	 * end is called when all groups have been handled
	 * @throws IOException 
	 */
	public void end() throws IOException;
}
