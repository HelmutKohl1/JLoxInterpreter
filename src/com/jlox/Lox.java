package com.jlox;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import com.jlox.Expr.Grouping;

public class Lox {
	
	static boolean hadError = false;

	public static void main(String[] args) throws IOException{
		
		if (args.length > 1) {
			System.out.println("Usage: jlox [script]");
			System.exit(64);
		} else if (args.length == 1) {
			runFile(args[0]);
		} else {
			//runPrompt();
			demoRPNFormatter();
		}
		
	}
	
	private static void runFile(String path) throws IOException {
		byte[] bytes = Files.readAllBytes(Paths.get(path));
		run(new String(bytes, Charset.defaultCharset()));
		
		// Indicate an error in the exit code
		if (hadError) System.exit(65);
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
		printTokens(tokens); // print the tokens
	}
	
	private static void printTokens(List<Token> list) {		
		for (int i = 0; i < list.size(); i++) {
			System.out.println(list.get(i));
		}
	}
	
	static void error(int line, String message) {
		report(line, "", message);
	}
	
	private static void report(int line, String where, String message) {
		System.err.println("[line " + line + "] Error " + where + " : " + message);
		hadError = true;
	}
	
	// Method to demo the Reverse Polish Notation Formatter class
	private static void demoRPNFormatter() {
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
