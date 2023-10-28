package com.jlox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.jlox.TokenType.*;

class Scanner {
	
	/*A class to scan the input source given as a string and return a list of tokens.
	 * */

	private final String source;
	private final List<Token> tokens = new ArrayList<>();
	private int start = 0;
	private int current = 0;
	private int line = 1;
	private static final Map<String, TokenType> keywords;
	static {
		keywords = new HashMap<String, TokenType>();
		keywords.put("and", AND);
		keywords.put("break", BREAK);
		keywords.put("class", CLASS);
		keywords.put("else", ELSE);
		keywords.put("false", FALSE);
		keywords.put("for", FOR);
		keywords.put("fun", FUN);
		keywords.put("if", IF);
		keywords.put("nil", NIL);
		keywords.put("or", OR);
		keywords.put("print", PRINT);
		keywords.put("return", RETURN);
		keywords.put("super", SUPER);
		keywords.put("this", THIS);
		keywords.put("true", TRUE);
		keywords.put("var", VAR);
		keywords.put("while", WHILE);
	}
	
	Scanner (String source){
		this.source = source;
	}
	
	List<Token> scanTokens(){
		// In each cycle of this loop, we scan one token.
		while (!isAtEnd()) {
			// We are at beginning of the next lexeme
			start = current;
			scanToken();
		}
		
		tokens.add(new Token(EOF, "", null, line));
		return tokens;
	}
	
	private void scanToken() {
		char c = advance();
		switch (c) {
			// Starting with single-character lexemes
			case '(': addToken(LEFT_PAREN); break;
			case ')': addToken(RIGHT_PAREN); break;
			case '{': addToken(LEFT_BRACE); break;
			case '}': addToken(RIGHT_BRACE); break;
			case ',': addToken(COMMA); break;
			case '.': addToken(DOT); break;
			case '-': addToken(MINUS); break;
			case '+': addToken(PLUS); break;
			case ';': addToken(SEMICOLON); break;
			case '*': addToken(STAR); break;
			case '?': addToken(QMARK); break;
			case ':': addToken(COLON); break;
			
			// Two-character lexemes
			case '!': 
				addToken(match('=')? BANG_EQUAL : BANG);
				break;
			case '<':
				addToken(match('=')? LESS_EQUAL : LESS);
				break;
			case '>':
				addToken(match('=')? GREATER_EQUAL : GREATER);
				break;
			case '=':
				addToken(match('=')? EQUAL_EQUAL : EQUAL);
				break;
				
			// Handling '/'
			case '/':
				if (match('/')) {
					// A comment goes until the end of the line
					while (peek() != '\n' && !isAtEnd()) {
						advance();
					}
				}else if (match('*')){
					// Multi-line comment
					while (!(match('*') && peek() == '/') && !isAtEnd()) {
						advance();
					}
				}
				else {
					addToken(SLASH);
				}
				
			// Ignore whitespace
			case ' ':
			case '\r':
			case '\t':
				break;
			case '\n':
				// Move on a line when encountering the newline character
				line++;
				break;
				
			// String literals
			case '"': string(); break;
			
			// Unexpected character handling
			default:
				// Handling number literals
				if (isDigit(c)) {
					number();
				} else if (isAlpha(c)) { 
					// Handling identifiers
					identifier();
				} else {		
					Lox.error(line, "Unexpected character.");
				}
				break;
		}
		
	}
	
	private void identifier() {
		while(isAlphaNumeric(peek())) advance();
		String text = source.substring(start, current);
		TokenType type = keywords.get(text);
		if (type == null) type = IDENTIFIER;
		
		addToken(type);
	}

	private void number() {
		while(isDigit(peek())) advance();
		
		//Look for fractional part
		if (peek() == '.' && isDigit(peekNext())) {
			advance();
			// Consume fractional part
			while(isDigit(peek())) advance();
		}		
		addToken(NUMBER, Double.parseDouble(source.substring(start, current)));
	}

	private void string() {
		while (peek() != '"' && !isAtEnd()) {
			if (peek() == '\n') line++;
			advance();
		}
		
		if (isAtEnd()) {
			Lox.error(line, "Unterminated string.");
			return;
		}
		
		// To close the string
		advance();
		
		// Trim the surrounding quotes
		String value = source.substring(start + 1, current - 1);
		addToken(STRING, value);
		return;
	}
	
	private boolean match(char expected) {
		/* Looks ahead one character and consumes it */
		if (isAtEnd()) return false;
		if (source.charAt(current) != expected) return false;
		
		current++;
		return true;
	}
	
	private char peek() {
		/* Lookahead function (using one-character lookahead) */
		if (isAtEnd()) return '\0';
		return source.charAt(current);
	}
	
	private char peekNext() {
		/* A function to provide a second character of lookahead*/
		if (current + 1 >= source.length()) return '\0';
		return source.charAt(current + 1);
	}
	
	private boolean isAlpha(char c) {
		return Character.isLetter(c);
	}
	
	private boolean isAlphaNumeric(char c) {
		return isAlpha(c) || isDigit(c);
	}
	
	private boolean isDigit(char c) {
		return c >= '0' && c <= '9';
	}
	
	private boolean isAtEnd() {
		return current >= source.length();
	}
	
	private char advance() {
		/* Consume next character without looking ahead*/
		return source.charAt(current++);
	}
	
	private void addToken(TokenType type) {
		addToken(type, null);
	}
	
	private void addToken(TokenType type, Object literal) {
		String text = source.substring(start, current);
		tokens.add(new Token(type, text, literal, line));
	}
}
