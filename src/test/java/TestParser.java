import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseException;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class TestParser {

	/**
	 * @param args
	 * @throws IOException 
	 * @throws ParseException 
	 */
	public static void main(String[] args) throws ParseException, IOException {
		CompilationUnit cu = JavaParser.parse(new File("../Phoenix/src/main/java/phoenix/CommercialAPI.java"));
		new MethodVisitor().visit(cu, null);
		listMethods(cu);
	}
	
	
    private static void listMethods(CompilationUnit cu) {
        List<TypeDeclaration> types = cu.getTypes();
        for (TypeDeclaration type : types) {
        	for (AnnotationExpr ae : type.getAnnotations()) {
        		System.out.println("ANNOTATION: " + ae.getName().getName());
        	}
        	System.out.println("*** BEGIN Type: " + type.getName());
            List<BodyDeclaration> members = type.getMembers();
            for (BodyDeclaration member : members) {
            	// members include code (fields) inside the class, and methods.
            	
            	System.out.println("+++ BEGIN: BodyDecl");
            	System.out.println(member);
            	System.out.println("+++   END: BodyDecl");
            }
        	System.out.println("***   END Type: " + type.getName());
        }
    }

	
    /**
     * Simple visitor implementation for visiting MethodDeclaration nodes. 
     */
    private static class MethodVisitor extends VoidVisitorAdapter {
        @Override
        public void visit(MethodDeclaration n, Object arg) {
            // here you can access the attributes of the method.
            // this method will be called for all methods in this 
            // CompilationUnit, including inner class methods
            System.out.println(n.getName());
            System.out.println("=============================================================");
        }
    }


}
