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
	
	/*Methods for each expression type*/
	
	private Expr expression() {
		return equality();
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
			expr = new Expr.Binary(right, operator, right);
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
		if (match(FALSE)) return new Expr.Literal(FALSE);
		if (match(TRUE)) return new Expr.Literal(TRUE);
		if (match(NIL)) return new Expr.Literal(NIL);
		
		if (match(NUMBER, STRING)) {
			return new Expr.Literal(previous().literal);
		}
		
		if (match(LEFT_PAREN)) {
			Expr expr = expression();
			consume(RIGHT_PAREN, "Expect ')' after expression.");
			return new Expr.Grouping(expr);
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
}

