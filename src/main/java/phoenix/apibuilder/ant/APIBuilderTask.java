package phoenix.apibuilder.ant;

import com.github.javaparser.ParseException;

import java.io.File;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

import phoenix.apibuilder.PhoenixAPIBuilder;

/**
 * Simple Ant taks for building Phoenix API
 * 
 * @author sean
 *
 */
public class APIBuilderTask extends Task {
	private File source;
	private File output;
	private File target;
	private boolean debug;
	
	public APIBuilderTask() {
	}

	@Override
	public void execute() throws BuildException {
		PhoenixAPIBuilder builder = new PhoenixAPIBuilder();
		builder.setOutputDir(output);
		builder.setSourceDir(source);
		builder.setTarget(target);
		builder.setDebug(debug);
		try {
			builder.process();
		} catch (Throwable e) {
			if (builder.isDebugEnabled()) {
				e.printStackTrace(System.err);
			}
			throw new BuildException("Phoenix API Builder Failed", e);
		}
	}
	
	public File getSource() {
		return source;
	}
	public void setSource(File source) {
		this.source = source;
	}
	public File getOutput() {
		return output;
	}
	public void setOutput(File output) {
		this.output = output;
	}
	public boolean isDebug() {
		return debug;
	}
	public void setDebug(boolean debug) {
		this.debug = debug;
	}
	
	public File getTarget() {
		return target;
	}

	public void setTarget(File target) {
		this.target = target;
	}
}
