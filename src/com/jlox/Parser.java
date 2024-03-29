package com.jlox;

import java.util.List;

import com.jlox.Stmt.Function;

import java.util.Arrays;
import java.util.ArrayList;

import static com.jlox.TokenType.*;

class Parser {
	
	/* A class to convert a list of tokens to an appropriately-nested series
	 * of Expr objects. That is, an Abstract Syntax Tree.*/
	
	/* A simple sentinel class to unwind the parser, since some parse errors should
	 * not cause synchronising. Hence an error is returned, not thrown.*/
	private static class ParseError extends RuntimeException{}
	
	private final List<Token> tokens;
	private enum BinaryOps {
			AND, OR, PLUS, MINUS, SLASH, STAR, BANG_EQUAL,
			EQUAL, EQUAL_EQUAL,
			GREATER, GREATER_EQUAL,
			LESS, LESS_EQUAL
	}
	private int current = 0;
	private boolean insideLoop = false;
	
	Parser(List<Token> tokens){
		this.tokens = tokens;
	}
	
	// Initial method to begin parsing:
	List<Stmt> parse() {
		List<Stmt> statements = new ArrayList<>();
		
		while(!isAtEnd()) {
			statements.add(declaration());
		}
		
		return statements;
	}

	private Stmt statement() {
		if (match(FOR)) {
			return forStatement();
		}
		if (match (IF)) {
			return ifStatement();
		}
		if (match(PRINT)) {
			return printStatement();
		}
		if (match(RETURN)) {
			return returnStatement();
		}
		if (match(WHILE)) {
			return whileStatement();
		}
		if (match(LEFT_BRACE)) {
			return new Stmt.Block(block());
		}
		if (match(BREAK)) {
			return breakStatement();
		}
		return expressionStatement();
	}

	private Stmt breakStatement() {		
		if(insideLoop) {
			consume(SEMICOLON, "Expect ';' after 'break' statement.");
			return new Stmt.Break(null);
		}
		throw error(peek(), "'break' statement cannot occur outside a loop.");
	}
	
	private Stmt forStatement() {
		consume(LEFT_PAREN, "Expect '(' after 'for'.");
		Stmt initializer;
		if (match(SEMICOLON)) {
			initializer = null;
		}
		else if (match(VAR)) {
			initializer = variableDeclaration();
		}
		else {
			initializer = expressionStatement();
		}
		
		Expr condition = null;
		if (!check(SEMICOLON)) {//use check() not match() here because we can't have an empty condition
			condition = expression();
		}
		consume(SEMICOLON, "Expect ';' after loop condition.");

		Expr increment = null;
		if (!check(RIGHT_PAREN)) {
			increment = expression();
		}
		consume(RIGHT_PAREN, "Expect ')' after for clauses.");
		
		insideLoop = true;
		Stmt body = statement();
		
		if (increment != null) {
			body = new Stmt.Block(Arrays.asList(body, new Stmt.Expression(increment)));
			
		}
		if (condition == null) condition = new Expr.Literal(true);
		body = new Stmt.While(condition, body);
		if (initializer != null) {
			body = new Stmt.Block(Arrays.asList(initializer, body));
		}
		
		insideLoop = false;
		return body;
	}
	
	private Stmt ifStatement() {
		consume(LEFT_PAREN, "Expect '(' after 'if'");
		Expr condition = expression();
		consume(RIGHT_PAREN, "Expect ') after 'if' condition");
		
		Stmt thenBranch = statement();
		Stmt elseBranch = null;
		if (match(ELSE)) {
			elseBranch = statement();
		}
		
		return new Stmt.If(condition, thenBranch, elseBranch);
	}
	
	private List<Stmt> block() {
		List<Stmt> statements = new ArrayList<Stmt>();
		while(!check(RIGHT_BRACE) && !isAtEnd()) {
			statements.add(declaration());
		}
		
		consume(RIGHT_BRACE, "Expect '}' after block");
		
		return statements;
	}

	private Stmt printStatement() {
		Expr value = expression();
		consume(SEMICOLON, "Expect ';' after value.");
		return new Stmt.Print(value);
	}
	
	private Stmt returnStatement() {
		Token keyword = previous();
		Expr value = null;
		if (!check(SEMICOLON)) {
			value = expression();
		}
		consume(SEMICOLON, "Expect ';' after return value.");
		return new Stmt.Return(keyword, value);
	}
	
	private Stmt variableDeclaration() {
		Token name = consume(IDENTIFIER, "Expect a variable identifier.");
		Expr initializer = null;
		if (match(EQUAL)){
			initializer = expression();
		}
		
		consume(SEMICOLON, "Expect ';' after variable declaration.");
		return new Stmt.Var(name, initializer);
	}

	private Stmt whileStatement() {
		insideLoop = true;
		consume(LEFT_PAREN, "Expect '(' after 'while' keyword.");
		Expr condition = expression();
		consume(RIGHT_PAREN, "Expect ')' after condition.");
		Stmt body = statement();
		
		insideLoop = false;
		return new Stmt.While(condition, body);
	}
	
	private Stmt expressionStatement() {
		Expr expression = expression();
		consume(SEMICOLON, "Expect ';' after expression");
		return new Stmt.Expression(expression);
	}
	
	private Stmt function(String kind) {
		Token name = consume(IDENTIFIER, "Expect " + kind + " name.");
		consume(LEFT_PAREN, "Expect '(' after " + kind + " name.");
		List<Token> params = new ArrayList<>();
		
		if (!check(RIGHT_PAREN)) {
			do {
				if (params.size() >= 255) {
					error(peek(), "Cannot have more than 255 arguments.");
				}
				
				params.add(consume(IDENTIFIER, "Expect parameter name."));
			} while (match(COMMA));
		}
		
		consume (RIGHT_PAREN, "Expect ')' after " + kind +  " parameters.");		
		consume(LEFT_BRACE, "Expect '{ before " + kind + " body.");
		List<Stmt> body = block();
		return new Stmt.Function(name, params, body);
	}

	/*Methods for each expression type*/
	
	private Expr expression() {
		if (match(FUN)) {
			return lambdaFunction();
		}
		return assignment();
		//return comma();
	}
	
	private Expr lambdaFunction() {
		consume(LEFT_PAREN, "Expect '(' after lambda function declaration.");
		List<Token> params = new ArrayList<>();
		
		if (!check(RIGHT_PAREN)) {
			do {
				if (params.size() >= 255) {
					error(peek(), "Cannot have more than 255 arguments.");
				}
				
				params.add(consume(IDENTIFIER, "Expect parameter name."));
			} while (match(COMMA));
		}
		
		consume (RIGHT_PAREN, "Expect ')' after lambda function parameters.");		
		consume(LEFT_BRACE, "Expect '{ before lambda function body.");
		List<Stmt> body = block();
		return new Expr.Lambda(params, body);
	}
	
	private Expr assignment() {
		//Expr expression = comma();
		Expr expression = or();
		
		if (match(EQUAL)) {
			Token equals = previous();
			Expr value = assignment();
			
			// ensures the lhs is a valid assignment target
			if (expression instanceof Expr.Variable) {
				Token name = ((Expr.Variable)expression).name;
				// converting the r-value expression into an l-value representation
				/* this is great when we have assignment expressions where the assignment target is
				 * also a stand-alone variable expression.*/
				return new Expr.Assign(name, value);
			}
			else if (expression instanceof Expr.Get) {
				Expr.Get get = (Expr.Get)expression;
				return new Expr.Set(get.object, get.name, value);
			}			
			
			error(equals, "Invalid assignment target.");
		}
		
		return expression;
	}
	
	private Expr or() {
		Expr expr = and();
		while (match(OR)) {
			Token operator = previous();
			Expr right = and();
			expr = new Expr.Logical(expr, operator, right);
		}
		return expr;
	}
	
	private Expr and() {
		Expr expr = comma();
		
		while (match(AND)) {
			Token operator = previous();
			Expr right = comma();
			expr = new Expr.Logical(expr, operator, right);
		}
		return expr;
	}
	
	private Stmt declaration() {
		/* Method for parsing a variable or function declaration */
		try {
			if (match(CLASS)) {
				return classDeclaration();
			}
			if (match(FUN)) {
				return function("function");					
			}
			if(match(VAR)) { 
				return variableDeclaration();
			}
			
			return statement();
		} catch(ParseError e) {
			synchronize();
			return null;
		}
	}

	private Stmt classDeclaration() {
		Token name = consume(IDENTIFIER, "Expect Class name.");
		
		Expr.Variable superclass = null;
		if (match(LESS)) {
			consume(IDENTIFIER, "Expect superclass name after '<'");
			superclass = new Expr.Variable(previous());
		}
		
		consume(LEFT_BRACE, "Expect '{' before Class body.");
		
		List<Stmt.Function> methods = new ArrayList<>();
		List<Stmt.Function> staticMethods = new ArrayList<>();
		while (!check(RIGHT_BRACE) && !isAtEnd()) {
			if (match(CLASS)) {
				staticMethods.add((Function) function("static method"));
			} else {
				methods.add((Function) function("method"));
			}
		}
		consume(RIGHT_BRACE, "Expect '}' after Class body.");
		if (staticMethods.isEmpty()) {
			return new Stmt.Class(name, superclass, methods, null);
		}else {
			return new Stmt.Class(name, superclass, methods, new Stmt.Class(name, null, staticMethods, null));
		}
	}
	
	private Expr comma() {
		/* Method added to support the comma (,) operator from C/C++ , pursuant to Exercise 1
		 * of Parsing Expressions. */
		//Expr expr = equality();
		Expr expr = ternary();
		while (match(COMMA)) {
			Token operator = previous();
			Expr left = equality();
			expr = new Expr.Binary(left, operator, expr);
		}
		
		return expr;
		
	}
	
	private Expr ternary() {
		/* Method to add support for the ternary conditional operator a ? b : c from C/C++ , pursuant to 
		 * Exercise 2 from Parsing Expressions. 
		 * 
		 *  - The ? is above equality in precedence, and the : is above term, except when nesting.
		 *  - The ? operator is right-associative, i.e. the following: a ? b : c ? e : f
		 *  is equivalent to: a ? b : (c ? e : f), NOT: (a ? b : c) ? e : f
		 * */
		
		Expr expr = equality();
		while (match(QMARK)) {
			Token qmark = previous();
			Expr left = ternary();//this can perhaps be made recursive by swapping it with ternary()
			if (match(COLON)) {
				Expr right = ternary();//this can perhaps be made recursive by swapping it with ternary()
				expr = new Expr.Ternary(qmark, expr, left, right);
			}
			else {
				throw error(peek(), "Colon expected as part of ternary expression");
			}			
		}
		
		return expr;
	}
	
	private Expr equality() {
		Expr expr = comparison();
		
		while (match(BANG_EQUAL, EQUAL_EQUAL)) {
			Token operator = previous();
			Expr right = comparison();
			// Creates left-associated nested tree of binary expressions:
			expr = new Expr.Binary(expr, operator, right);
		} 
		
		return expr;
	}
	
	private Expr comparison() {
		Expr expr = term();
		
		while (match(LESS, LESS_EQUAL, GREATER, GREATER_EQUAL)) {
			Token operator = previous();
			Expr right = term();
			expr = new Expr.Binary(expr, operator, right);
		}
		
		return expr;
	}
	
	private Expr term() {
		Expr expr = factor();
		
		while (match(PLUS, MINUS)) {
			Token operator = previous();
			Expr right = factor();
			expr = new Expr.Binary(expr, operator, right);
		}
		
		return expr;
	}
	
	private Expr factor() {
		Expr expr = unary();
		
		while (match(STAR, SLASH)) {
			Token operator = previous();
			Expr right = unary();
			expr = new Expr.Binary(expr, operator, right);
		}
		
		return expr;
	}
	
	private Expr unary() {
		while (match(BANG, MINUS)) {
			Token operator = previous();
			Expr right = primary();
			return new Expr.Unary(operator, right);
		}
		
		return call();
	}
	
	private Expr call() {
		Expr expr = primary();
		
		while (true) {
			if (match(LEFT_PAREN)) {
				expr = finishCall(expr);
			}
			else if (match(DOT)) {
				Token name = consume(IDENTIFIER, "Expected property name after '.'");
				expr = new Expr.Get(expr, name);
			}
			else {
				break;
			}
		}
		
		return expr;
	}
	
	private Expr finishCall(Expr callee) {
		List<Expr> arguments = new ArrayList<>();
		if (!check(RIGHT_PAREN)) {
			do {
				if (arguments.size() >= 255) {
					error(peek(), "Cannot have more than 255 arguments.");
				}
				arguments.add(expression());
			}while(match(COMMA));
		}
		
		Token paren = consume(RIGHT_PAREN, "Expect ')' after arguments.");
		return new Expr.Call(callee, paren, arguments);
	}
	
	private Expr primary() {
		
		boolean binErrorCond = match(PLUS) || match(MINUS) || match(SLASH) || match(STAR)
				|| match(EQUAL) || match(EQUAL_EQUAL) || match(BANG_EQUAL) || match(LESS)
				|| match(LESS_EQUAL) || match(GREATER) || match(GREATER_EQUAL) || match(AND) 
				|| match(OR);
				
		if (match(FALSE)) {
			return new Expr.Literal(false);
		}
		else if (match(TRUE)) {
			return new Expr.Literal(true);
		}
		else if (match(NIL)) {
			return new Expr.Literal(null);
		}
		else if (match(NUMBER, STRING)) {
			return new Expr.Literal(previous().literal);
		}
		else if (match(LEFT_PAREN)) {
			Expr expr = expression();
			consume(RIGHT_PAREN, "Expect ')' after expression.");
			return new Expr.Grouping(expr);
		} 
		else if (match(SUPER)) {
			Token keyword = previous();
			consume(DOT, "Expect '.' after 'super'.");
			Token method = consume(IDENTIFIER, "Expect superclass method name.");
			return new Expr.Super(keyword, method);
		}
		else if (match(THIS)) {
			return new Expr.This(previous());
		}
		else if (match(IDENTIFIER)) {
			return new Expr.Variable(previous());
		}
		else if (match(BREAK)) {
			throw error(peek(), "Break statement cannot be used outside of a loop.");
		}
		else if (binErrorCond){//changed from 'else' to make error throw reachable
			//condition needs revising - dirty but functional atm
			return binaryError();
		}
		
		// If the parser has found a token that cannot start a statement:
		throw error(peek(), "Expression expected");
	}
	
	private Expr binaryError() { 
		Token operator = peek();
		advance();
		try {
			Expr right = primary();
			return new Expr.BinaryError(operator, right);
		}catch(java.lang.Exception e) {
			throw error(peek(), "Expression expected");
		}	

	}
	
	private boolean match(TokenType... types) {
		// Checks type of current token and consumes it if it matches
		for (TokenType type : types) {
			if (check(type)) {
				advance();
				return true;
			}
		}
		return false;
	}
	
	private Token consume(TokenType type, String message) {
		if (check(type)) return advance();
		
		throw error(peek(), message);
	}
	
	private boolean check(TokenType type) {
		if (isAtEnd()) return false;
		return peek().type == type;
	}
	
	private Token advance() {
		if(!isAtEnd()) current++;
		return previous();
	}
	
	private boolean isAtEnd() {
		return peek().type == EOF;
	}
	
	private Token peek() {
		return tokens.get(current);
	}
	
	private Token previous() {
		return tokens.get(current - 1);
	}
	
	private ParseError error(Token token, String message) {
		Lox.error(current, message);
		return new ParseError();
	}
	
	private void synchronize() {
		/*A method to discard tokens until it detects a statement boundary*/
		advance();
		
		while (!isAtEnd()) {
			if (previous().type == SEMICOLON) return;
			
			switch(peek().type) {
			case CLASS:
			case FUN:
			case VAR:
			case FOR:
			case IF:
			case WHILE:
			case RETURN:
				return;
			}
			
			advance();
		}
	}
}

