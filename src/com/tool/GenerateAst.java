package com.tool;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.io.PrintWriter;
import java.io.BufferedWriter;

/**A helper script to generate all the necessary expression subclasses for the Abstract Syntax Tree.
 * */

public class GenerateAst {

	public static void main(String[] args) throws IOException{
		if (args.length != 1) {
			System.err.println("Usage: generate_ast <output directory>");
			System.exit(64);
		}
		
		String outputDir = args[0];
		defineAst(outputDir, "Expr", Arrays.asList(
				"Binary: Expr left, Token operator, Expr right",
				"Grouping: Expr expression",
				"Literal: Object value",
				"Unary: Token operator, Expr right"));
	}

	private static void defineAst(String outputDir, String baseName, List<String> types) throws IOException {
		
		String path = outputDir + "/" + baseName + ".java";
		PrintWriter writer = new PrintWriter(path, "UTF-8");
		BufferedWriter buffer = new BufferedWriter(writer);
		
		buffer.write("/**This file has been automatically generated by: GenerateAst.java*/"); buffer.newLine();
		buffer.newLine();
		
		buffer.write("package com.jlox;"); buffer.newLine();
		buffer.newLine();
		buffer.write("import java.util.List;"); buffer.newLine();
		buffer.newLine();
		buffer.write("abstract class " + baseName + " {"); buffer.newLine();
		
		// the abstract accept() method defined in the superclass, baseName
		buffer.newLine();
		buffer.write("	abstract <R> R accept(Visitor<R> visitor);"); buffer.newLine();
		buffer.newLine();
		
		defineVisitor(buffer, baseName, types);
		
		// The AST classes
		for (String type : types) {
			String className = type.split(":")[0].trim();
			String fields = type.split(":")[1].trim();
			defineType(buffer, baseName, className, fields);
		}
		
		buffer.write("}"); buffer.newLine();
		buffer.close();
	}

	private static void defineType(BufferedWriter buffer, String baseName, String className, String fieldList) throws IOException {
		/* A method to write the subclasses which inherit from the abstract superclass Expr, including
		 * an override for the abstract method accept(Visitor visitor)*/
		
		buffer.write("static class " + className + " extends " + baseName + " {"); buffer.newLine();
		
		// Fields
		buffer.newLine();
		String[] fields = fieldList.split(", ");
		for (String field : fields) {
			buffer.write("	final "+ field +";"); buffer.newLine();
		}		
		buffer.newLine();
		
		// Constructor
		buffer.write("	" + className + " (" + fieldList +") {"); buffer.newLine();
		
		// Store parameters in fields:
		for (String field : fields) {
			String name = field.split(" ")[1];
			buffer.write("		this." + name + " = " + name + ";"); buffer.newLine();
		}		
		buffer.newLine();	
		buffer.write("	}"); buffer.newLine();
		
		// Implementing the abstract method accept inherited from the superclass
		buffer.newLine();
		buffer.write("	@Override"); buffer.newLine();
		buffer.write("	<R> R accept(Visitor<R> visitor){"); buffer.newLine();
		buffer.write("		return visitor.visit" + className + baseName +"(this);"); buffer.newLine();
		buffer.write("	}"); buffer.newLine();
		
		buffer.write("}"); buffer.newLine();
	}
	
	private static void defineVisitor(BufferedWriter buffer, String baseName, List<String> types) throws IOException {
		/* A method to write the visitor interface, defining the signature for the visit methods
		 * for each type defined earlier.*/
		
		buffer.write("	interface Visitor<R> {"); buffer.newLine();
		
		for (String type : types) {
			String typeName = type.split(":")[0].trim();
			buffer.write("	R visit" + typeName + baseName + "(" + typeName + " " + baseName.toLowerCase() + ");"); 
			buffer.newLine();
			
		}
		
		buffer.write("	}"); buffer.newLine();
	}


}