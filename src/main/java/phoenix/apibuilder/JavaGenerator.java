package phoenix.apibuilder;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Calendar;

import org.apache.commons.lang.StringUtils;

public class JavaGenerator implements IAPIGenerator {
	private File dir = null;
	private String date;
	private boolean checkNull = true;
	private PhoenixAPIBuilder builder = null;
	
	public JavaGenerator(PhoenixAPIBuilder phoenixAPIBuilder) {
		this.builder=phoenixAPIBuilder;
	}

	@Override
	public void begin(File outDir) throws IOException {
		this.dir=outDir;
		date = Calendar.getInstance().getTime().toString();
	}

	@Override
	public void end() {
		System.out.println("** Finished generating java api **");
	}

	@Override
	public void handleGroup(APIGroup group) throws IOException {
		// each group is file
		File out = getFile(group);
		PrintWriter pw = new PrintWriter(new FileWriter(out));
		beginFile(pw, group);
		for (APIClass cl : group.classes) {
			processClass(pw, group, cl);
		}
		endFile(pw, group);
		pw.flush();
		pw.close();
	}

	private void processClass(PrintWriter pw, APIGroup group, APIClass cl) {
		System.out.printf("Generating: %s : %s\n", group.groupId, cl.name);
		if (cl.isProxy) {
			processProxyClass(pw, group, cl);
			return;
		}
		
		pw.printf("   private static %s %s = new %s();\n", cl.instanceClass, cl.name.toLowerCase(), cl.instanceClass);
		for (APIMethod m : cl.methods) {
			if (m.javadoc!=null) {
				pw.println("   /**");
				pw.println(m.javadoc);
				pw.println("    */");
			}
			pw.printf("   public static %s %s(%s) {\n", getReturn(m), getMethodName(m), getMethodArgs(cl, m, false));
			pw.printf("      %s %s.%s(%s);\n", getReturning(m), cl.name.toLowerCase(), m.name, getArgsOnly(cl, m));
			pw.println("   }\n");
		}
	}

	private String getReturning(APIMethod m) {
		if (m.returnType==null || "void".equals(m.returnType)) return "";
		return "return";
	}

	private String getMethodArgs(APIClass apiClass, APIMethod m, boolean useObjectAsFirstArg) {
		if (m.parameters.size()==0) return "";
		StringBuffer sb = new StringBuffer();
		for (int i=0;i<m.parameters.size();i++) {
			APIParameter p = m.parameters.get(i);
			if (sb.length()>0) sb.append(", ");
			if (useObjectAsFirstArg && i==0) {
				sb.append("Object").append(" ").append(p.name);
			} else {
				sb.append(p.type).append(" ").append(p.name);
			}
		}
		return sb.toString();
	}

	private Object getArgsOnly(APIClass apiClass, APIMethod m) {
		if (m.parameters.size()==0) return "";
		StringBuffer sb = new StringBuffer();
		if (apiClass.isProxy) {
			for (int i=1;i<m.parameters.size();i++) {
				APIParameter p = m.parameters.get(i);
				if (sb.length()>0) sb.append(", ");
				sb.append(p.name);
			}
		} else {
			for (APIParameter p : m.parameters) {
				if (sb.length()>0) sb.append(", ");
				sb.append(p.name);
			}
		}
		return sb.toString();
	}

	private String getMethodName(APIMethod m) {
		return StringUtils.capitalize(m.name);
	}

	private String getReturn(APIMethod m) {
		if (m.returnType==null) return "void";
		return m.returnType;
	}

	private void processProxyClass(PrintWriter pw, APIGroup group, APIClass cl) {
		for (APIMethod m : cl.methods) {
			if (m.javadoc!=null) {
				pw.println("   /**");
				pw.println(m.javadoc);
				pw.println("    */");
			}
			pw.printf("   public static %s %s(%s) {\n", getReturn(m), getMethodName(m), getMethodArgs(cl, m, false));
			if (builder.isDebugEnabled()) {
				pw.println("      try {");
			}
			if (checkNull) {
				pw.printf("      if (%s == null) {\n", m.parameters.get(0).name);
				pw.printf("         return %s;\n", getDefaultReturn(m));
				pw.printf("      }\n");
			}
			pw.printf("      %s %s.%s(%s);\n", getReturning(m), m.parameters.get(0).name, m.name, getArgsOnly(cl, m));
			
			if (builder.isDebugEnabled()) {
				pw.println("      } catch (Throwable t) {");
				pw.println("         t.printStackTrace();");
				pw.printf ("         return %s;\n", getDefaultReturn(m));
				pw.println("      }");
			}

			pw.println("   }\n");
			
			if (!StringUtils.isEmpty(cl.proxyResolver)) {
				if (cl.proxyResolver.contains("phoenix.api.")) {
					System.out.printf("WARN: Using deprecated resolver %s for %s\n", cl.proxyResolver, cl.name);
				}
				// add in a generic resolver method
				pw.println("   /**");
				pw.printf("    * Convenience method that will convert the incoming object parameter to a %s type using %s, and then allow the API call to work as intended on the resolved object.\n", cl.name, cl.proxyResolver);
				pw.println("    */");
				pw.printf("   public static %s %s(%s) {\n", getReturn(m), getMethodName(m), getMethodArgs(cl, m, true));
				if (builder.isDebugEnabled()) {
					pw.println("      try {");
				}
				if (checkNull) {
					pw.printf("      if (%s == null) {\n", m.parameters.get(0).name);
					pw.printf("         return %s;\n", getDefaultReturn(m));
					pw.printf("      }\n");
				}
				pw.printf("      %s proxy = %s(%s);\n", cl.name, cl.proxyResolver, m.parameters.get(0).name);
				pw.printf("      if (proxy==null) {\n");
				pw.printf("         return %s; // do nothing\n", getDefaultReturn(m));
				pw.printf("      }\n");
				pw.printf("      %s proxy.%s(%s);\n", getReturning(m), m.name, getArgsOnly(cl, m));
				if (builder.isDebugEnabled()) {
					pw.println("      } catch (Throwable t) {");
					pw.println("         t.printStackTrace();");
					pw.printf ("         return %s;\n", getDefaultReturn(m));
					pw.println("      }");
				}
				pw.println("   }\n");
			}
		}
	}

	private String getDefaultReturn(APIMethod m) {
		if (m.returnType==null || "void".equals(m.returnType)) return "";
		if ("boolean".equalsIgnoreCase(m.returnType)) return "false";
		if ("int".equalsIgnoreCase(m.returnType)) return "0";
		if ("Integer".equalsIgnoreCase(m.returnType)) return "0";
		if ("long".equalsIgnoreCase(m.returnType)) return "0";
		if ("float".equalsIgnoreCase(m.returnType)) return "0";
		if ("double".equalsIgnoreCase(m.returnType)) return "0";
		if ("char".equalsIgnoreCase(m.returnType)) return "0";
		if ("byte".equalsIgnoreCase(m.returnType)) return "0";
		return "null";
	}

	private void endFile(PrintWriter pw, APIGroup group) {
		pw.println("}");
		pw.println();
		pw.flush();
	}

	private void beginFile(PrintWriter pw, APIGroup group) {
		pw.println("package phoenix;");
		pw.println();
		for (String s: group.imports) {
			pw.printf("import %s;\n", s);
		}
		pw.println();
		pw.println("/**");
		pw.printf(" * API Generated: %s<br/>\n", date);
		for (APIClass cl : group.classes) {
			if (cl.javaDoc!=null) {
				pw.printf(" * API Source: {@link %s}<br/>\n", cl.name);
				pw.println(cl.javaDoc);
			}
		} 
		pw.println("  */");
		pw.printf("public final class %s {\n", group.groupId);
	}

	private File getFile(APIGroup group) {
		return new File(dir, group.groupId + ".java");
	}
}
