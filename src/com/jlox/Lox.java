package com.jlox;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class Lox {
	
	private static final Interpreter interpreter = new Interpreter();
	
	static boolean hadError = false;
	static boolean hadRuntimeError = false;

	public static void main(String[] args) throws IOException{
		
		if (args.length > 1) {
			System.out.println("Usage: jlox [script]");
			System.exit(64);
		} else if (args.length == 1) {
			runFile(args[0]);
		} else {
			runPrompt();
		}
		
	}
	
	private static void runFile(String path) throws IOException {
		byte[] bytes = Files.readAllBytes(Paths.get(path));
		run(new String(bytes, Charset.defaultCharset()));
		
		// Indicate an error in the exit code
		if (hadError) System.exit(65);
		if (hadRuntimeError) System.exit(70);
	}

	private static void runPrompt() throws IOException {
		InputStreamReader input = new InputStreamReader(System.in);
		BufferedReader reader = new BufferedReader(input);
		
		for(;;) {
			System.out.print("> ");
			String line = reader.readLine();
			if (line == null) break;
			run(line);
			
			// Reset error flag if user makes a mistake
			hadError = false;
		}
	}
	
	private static void run(String source) {
		Scanner scanner = new Scanner(source);
		List<Token> tokens = scanner.scanTokens(); 
		
		Parser parser = new Parser(tokens);
		List<Stmt> statements = parser.parse();
		/*
		AstPrinter printer = new AstPrinter();
		for(Stmt statement : statements) {
			System.out.println(printer.print(statement));
		}*/
		
		if(hadError) return;
		
		interpreter.interpret(statements);
		//System.out.println(new AstPrinter().print(expression));
	}
	
	private static void printTokens(List<Token> list) {		
		for (int i = 0; i < list.size(); i++) {
			System.out.println(list.get(i));
		}
	}
	
	static void error(int line, String message) {
		report(line, "", message);
	}
	
	static void runtimeError(RuntimeError error) {
		System.err.println(error.getMessage() + "\n[line " + error.token.line + "]");
		hadRuntimeError = true;
	}
	
	private static void report(int line, String where, String message) {
		System.err.println("[line " + line + "] Error " + where + " : " + message);
		hadError = true;
	}
	
	static void error(Token token, String message) {
		if(token.type == TokenType.EOF) {
			report(token.line, " at end", message);
		} else {
			report(token.line, " at '" + token.lexeme + "'", message);
		}
	}
	
	private static void demoScanner(String source) {
		Scanner scanner = new Scanner(source);
		List<Token> tokens = scanner.scanTokens(); 
		printTokens(tokens); // print the tokens
	}
	
	private static void printAST() {
		
	}
	
	private static void demoRPNFormatter() {
		/* Method to demo the Reverse Polish Notation Formatter class. */
		Expr expression = new Expr.Binary(
				new Expr.Grouping(new Expr.Binary(
						new Expr.Literal(1),
						new Token(TokenType.PLUS, "+", null, 1),
						new Expr.Literal(2))),
			    new Token(TokenType.STAR, "*", null, 1),
			    new Expr.Grouping(new Expr.Binary(
			    		new Expr.Literal(4),
			    		new Token(TokenType.MINUS, "-", null, 1),
			    		new Expr.Literal(3))));
		
		Expr expression1 = new Expr.Unary(new Token(TokenType.MINUS, "-", null, 1), new Expr.Grouping(new Expr.Literal(5)));
			    		
		System.out.println(new RPNFormatter().print(expression));
	}
}
