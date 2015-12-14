package phoenix.apibuilder;

import japa.parser.JavaParser;
import japa.parser.ParseException;
import japa.parser.ast.CompilationUnit;
import japa.parser.ast.ImportDeclaration;
import japa.parser.ast.body.BodyDeclaration;
import japa.parser.ast.body.ClassOrInterfaceDeclaration;
import japa.parser.ast.body.MethodDeclaration;
import japa.parser.ast.body.Parameter;
import japa.parser.ast.body.TypeDeclaration;
import japa.parser.ast.expr.AnnotationExpr;
import japa.parser.ast.expr.BooleanLiteralExpr;
import japa.parser.ast.expr.Expression;
import japa.parser.ast.expr.IntegerLiteralExpr;
import japa.parser.ast.expr.MemberValuePair;
import japa.parser.ast.expr.NormalAnnotationExpr;
import japa.parser.ast.expr.StringLiteralExpr;
import japa.parser.ast.type.ClassOrInterfaceType;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.math.NumberUtils;

public class PhoenixAPIBuilder {
	private static final String ANN_APICLASS = "API";
	private static final String ANN_APIMETHOD = "APIMethod";
	
	private File sourceDir = null;
	private File outputDir = null;
	
	private List<SkippedFile> skippedList = new ArrayList<SkippedFile>();
	private List<File> processedList = new ArrayList<File>();
	private List<ImplementsFile> implementsList = new ArrayList<ImplementsFile>(); 
	
	private Map<String, APIGroup> groups = new TreeMap<String, APIGroup>();
	private boolean debug=false;
	private File target = new File("target");

	private class ImplementsFile {
		public File file;
		public AnnotationExpr ae;
		public String type;
	}
	
	private class SkippedFile {
		public String type;
		public File file;
	}
	
	/**
	 * @param args
	 * @throws IOException
	 * @throws ParseException
	 */
	public static void main(String[] args) throws ParseException, IOException {
		PhoenixAPIBuilder builder = new PhoenixAPIBuilder();
		if (args == null || args.length == 0)
			help();

		for (int i = 0; i < args.length; i++) {
			String s = args[i];
			if (args.length == 0 || "--help".equals(s) || "-?".equals(s) || "/?".equals(s)) {
				help();
			}

			if ("--dir".equals(s)) {
				builder.setSourceDir(new File(args[++i]));
			}

			if ("--output".equals(s)) {
				builder.setOutputDir(new File(args[++i]));
			}

			if ("--debug".equals(s)) {
				builder.setDebug(true);
			}
		}

		// process the files
		builder.process();
	}

	public void setDebug(boolean b) {
		this.debug=b;
	}
	
	public boolean isDebugEnabled() {
		return debug;
	}

	public void setOutputDir(File file) {
		this.outputDir = file;
	}

	public void setSourceDir(File file) {
		this.sourceDir = file;
	}

	public void process() throws ParseException, IOException {
		System.out.println("Processing APIS in " + sourceDir);
		Iterator x = FileUtils.iterateFiles(sourceDir, new String[] { "java" }, true);
		while (x.hasNext()) {
			File f = (File) x.next();
			processJavaFileForAPI(f);
		}
		
		while (implementsList.size()>0) {
			List<ImplementsFile> files = new ArrayList<ImplementsFile>(implementsList);
			// post processing implements list.
			for (Iterator<ImplementsFile> i=files.listIterator();i.hasNext();) {
				ImplementsFile f = i.next();
				implementsList.remove(f);
				
				SkippedFile sf = getSkippedFile(f.type);
				if (sf==null) {
					System.out.println("Already Processed: " + f.type);
					continue;
				}
				
				if (processedList.contains(sf.file)) {
					System.out.println("Already Processed: " + f.file);
					continue;
				}
				
				processJavaFileForAPI(sf.file, f.ae);
			}
		}

		if (groups.size() == 0) {
			throw new RuntimeException("Failed to find any APIs");
		}

		if (!outputDir.exists()) {
			if (!outputDir.mkdirs()) {
				throw new IOException("Unable to create output dir: " + outputDir);
			}
		}
		
		List<IAPIGenerator> generators = new ArrayList<IAPIGenerator>();
		generators.add(new JavaGenerator(this));
		generators.add(new ApiPropertiesGenerator());
		generators.add(new BBCodeGenerator());
		generators.add(new WikiCodeGenerator());
		//generators.add(new SearchReplaceGenerator());
		
		for (IAPIGenerator gen: generators) {
			System.out.println("Begin: " + gen.getClass().getName());
			gen.begin(outputDir);
			for (APIGroup g : groups.values()) {
				gen.handleGroup(g);
			}
			gen.end();
			System.out.println("End: " + gen.getClass().getName() + "\n");
		}
	}

	private void processJavaFileForAPI(File f) throws ParseException, IOException {
		System.out.println("Processing file: " + f);
		CompilationUnit cu = JavaParser.parse(f);

		List<TypeDeclaration> types = cu.getTypes();
		for (TypeDeclaration type : types) {
			if (type.getAnnotations() == null) {
				addSkippedFile(f, type.getName());
				break;
			}
			
			boolean processed = false;
			for (AnnotationExpr ae : type.getAnnotations()) {
				if (ANN_APICLASS.equals(ae.getName().getName())) {
					processAPI(f, cu, ae, type);
					processed=true;
				}
			}
			
			if (!processed) {
				addSkippedFile(f, type.getName());
			}
		}
	}

	private void processJavaFileForAPI(File f, AnnotationExpr ae) throws ParseException, IOException {
		System.out.println("Processing Sub file: " + f);
		CompilationUnit cu = JavaParser.parse(f);

		List<TypeDeclaration> types = cu.getTypes();
		for (TypeDeclaration type : types) {
			processAPI(f, cu, ae, type);
		}
	}
	
	private void addSkippedFile(File f, String name) {
		if (getSkippedFile(f)!=null) {
			// already have it
			return;
		}
		
		SkippedFile cu = new SkippedFile();
		cu.file=f;
		cu.type=name;
		skippedList.add(cu);
	}

	private SkippedFile getSkippedFile(File f) {
		for (SkippedFile cu : skippedList) {
			if (f.equals(cu.file)) return cu;
		}
		return null;
	}

	private SkippedFile getSkippedFile(String type) {
		for (SkippedFile cu : skippedList) {
			//System.out.printf("*** comparing: %s == %s *** \n", type, cu.type);
			if (type.equals(cu.type)) return cu;
		}
		return null;
	}

	private void processAPI(File f, CompilationUnit cu, AnnotationExpr ae, TypeDeclaration type) {
		if (processedList.contains(f)) {
			return;
		}

		System.out.println("Process API: " + type.getName());
		
		APIClass apiClass = new APIClass();
		APIGroup apiGroup = null;
		// we have an API class, process it.
		if (ae instanceof NormalAnnotationExpr) {
			NormalAnnotationExpr nae = (NormalAnnotationExpr) ae;
			List<MemberValuePair> l = nae.getPairs();
			String group = null;
			if (l != null) {
				for (MemberValuePair p : l) {
					if ("group".equals(p.getName())) {
						group = getStringLiteral(p.getValue());
					} else if ("resolver".equals(p.getName())) {
						apiClass.proxyResolver = getStringLiteral(p.getValue());
					} else if ("prefix".equals(p.getName())) {
						apiClass.proxyPrefix = getStringLiteral(p.getValue());
					} else if ("proxy".equals(p.getName())) {
						apiClass.isProxy = getBooleanLiteral(p.getValue());
					} else {
						throw new RuntimeException("Unhandled Annotation Attribute: " + p.getName() + " for API annotation!");
					}
				}
			}

			if (group == null)
				throw new RuntimeException("Missing 'group' for Annotation: " + ae.getName() + " on file " + f);
			apiGroup = groups.get(group);
			if (apiGroup == null) {
				apiGroup = new APIGroup();
				apiGroup.groupId = group;
				groups.put(group, apiGroup);
			}
		}

		if (apiGroup == null)
			throw new RuntimeException("Failed to pull api group for: " + f);

		// add the imports
		if (cu.getImports() != null) {
			for (ImportDeclaration imp : cu.getImports()) {
				apiGroup.imports.add(imp.getName().toString());
			}
		}
		
		// add this section to the group
		apiGroup.classes.add(apiClass);

		if (type instanceof ClassOrInterfaceDeclaration) {
			ClassOrInterfaceDeclaration ctype = (ClassOrInterfaceDeclaration) type;

			apiClass.name = ctype.getName();

			// add our package level imports, so that we can resolve stuff local to us
			apiGroup.imports.add(cu.getPackage().getName().toString() + ".*");
			
			if (apiClass.isProxy) {
				// if we are proxy interfaces, then we need to
				// process
				// parent interfaces
				if (ctype.getExtends() != null) {
					for (ClassOrInterfaceType t : ctype.getExtends()) {
						// add this class to list of files to process using the same annotation
						ImplementsFile imf = new ImplementsFile();
						imf.ae=ae;
						imf.file=f;
						imf.type=t.getName();
						implementsList.add(imf);
					}
				}
			} else {
				apiClass.instanceClass = cu.getPackage().getName().toString() + "." + type.getName();
			}

			if (ctype.getJavaDoc() != null) {
				apiClass.javaDoc = ctype.getJavaDoc().getContent();
			}

			// process methods
			List<BodyDeclaration> members = type.getMembers();
			for (BodyDeclaration member : members) {
				if (member instanceof MethodDeclaration) {
					if (!Modifier.isPublic(((MethodDeclaration) member).getModifiers())) continue;
					
					
// TODO: Process method level annotation					
//					MethodDeclaration mdec = (MethodDeclaration) member;
//					if (mdec.getAnnotations()!=null) {
//						for (AnnotationExpr annExpr : mdec.getAnnotations()) {
//							if (ANN_APIMETHOD.equals(annExpr.getName().getName())) {
//								
//							}
//						}
//					}

					
					APIMethod item = new APIMethod();
					if (member.getJavaDoc() != null) {
						item.javadoc = member.getJavaDoc().getContent();
					}

					if (apiClass.isProxy) {
						// add our type as the first arg
						APIParameter p = new APIParameter();
						p.type = apiClass.name;
						p.name = apiClass.name.toLowerCase();
						item.parameters.add(p);
					}

					MethodDeclaration mdecl = (MethodDeclaration) member;
					item.name = mdecl.getName();
					item.returnType = mdecl.getType().toString();
					if (mdecl.getThrows() != null) {
						item.hasThrows = true;
					}

					if (mdecl.getParameters() != null) {
						for (Parameter p : mdecl.getParameters()) {
							APIParameter ap = new APIParameter();
							ap.name = p.getId().getName();
							ap.type = p.getType().toString();
							item.parameters.add(ap);
						}
					}
					
					// System.out.println(member.getClass().getName());
					apiClass.methods.add(item);
				}
			}
		}
		
		processedList.add(f);
	}

	private Map<String, Object> getAnnations(AnnotationExpr ae) {
		Map<String, Object> map = new HashMap<String, Object>();
		if (ae instanceof NormalAnnotationExpr) {
			NormalAnnotationExpr nae = (NormalAnnotationExpr) ae;
			List<MemberValuePair> l = nae.getPairs();
			if (l != null) {
				for (MemberValuePair p : l) {
					String key = p.getName();
					Object value = null;
					Expression ex = p.getValue();
					if (ex instanceof BooleanLiteralExpr) {
						value = getBooleanLiteral(ex);
					} else if (ex instanceof IntegerLiteralExpr) {
						value = getIntLiteral(ex);
					} else if (ex instanceof StringLiteralExpr) {
						value = getStringLiteral(ex);
					} else {
						throw new RuntimeException("Unhandled Data Type: " + ex + " for annotation: " + ae);
					}
					map.put(key, value);
				}
			}
		}
		return map;
	}
	
	private boolean getBooleanLiteral(Expression value) {
		return ((BooleanLiteralExpr) value).getValue();
	}

	private String getStringLiteral(Expression value) {
		return ((StringLiteralExpr) value).getValue();
	}

	private int getIntLiteral(Expression value) {
		return NumberUtils.toInt(((IntegerLiteralExpr) value).getValue());
	}

	public static void help() {
		System.out.println("PhoenixAPIBuilder --dir SourceDir [--help]");
		System.exit(1);
	}

	public void setTarget(File target) {
		if (target!=null) {
			this.target =target;
		}
	}
}
