package com.jlox;

import java.util.List;

import static com.jlox.TokenType.*;

class Parser {
	
	/* A class to convert a list of tokens to an appropriately-nested series
	 * of Expr objects. That is, an Abstract Syntax Tree.*/
	
	/* A simple sentinel class to unwind the parser, since some parse errors should
	 * not cause synchronising. Hence an error is returned, not thrown.*/
	private static class ParseError extends RuntimeException{}
	
	private final List<Token> tokens;
	private int current = 0;
	
	Parser(List<Token> tokens){
		this.tokens = tokens;
	}
	
	// Initial method to begin parsing:
	Expr parse() {
		try {
			return expression();
		}catch(ParseError error) {
			return null;
		}
	}
	
	/*Methods for each expression type*/
	
	private Expr expression() {
		return comma();
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
		 *  is equivalent to: a ? b : (c ? e : f)
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
		
		return primary();
	}
	
	private Expr primary() {
		if (match(FALSE)) {
			return new Expr.Literal(FALSE);
		}
		else if (match(TRUE)) {
			return new Expr.Literal(TRUE);
		}
		else if (match(NIL)) {
			return new Expr.Literal(NIL);
		}
		else if (match(NUMBER, STRING)) {
			return new Expr.Literal(previous().literal);
		}
		else if (match(LEFT_PAREN)) {
			Expr expr = expression();
			consume(RIGHT_PAREN, "Expect ')' after expression.");
			return new Expr.Grouping(expr);
		} else {
			return binaryError();
		}
		
		// If the parser has found a token that cannot start a statement:
		//throw error(peek(), "Expression expected");
	}
	
	private Expr binaryError() { 
		Token operator = peek();
		advance();
		try {
			Expr right = primary();
			// Might want to consider a re-imp of BinaryError...
			return new Expr.BinaryError(operator.lexeme + "E", right);
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

