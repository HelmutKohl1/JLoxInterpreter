package com.jlox;

import com.jlox.Expr.Ternary;
import com.jlox.Expr.Binary;
import com.jlox.Expr.BinaryError;
import com.jlox.Expr.Grouping;
import com.jlox.Expr.Literal;
import com.jlox.Expr.Unary;
import com.jlox.Expr.Variable;

public class AstPrinter implements Expr.Visitor<String> {

	String print(Expr expr) {
		return expr.accept(this);
	}
	
	@Override
	public String visitTernaryExpr(Ternary expr) {
		return parenthesize(expr.qmark.lexeme, expr.cond, expr.left, expr.right);
	}
	
	@Override
	public String visitBinaryExpr(Binary expr) {
		return parenthesize(expr.operator.lexeme, expr.left, expr.right);
	}
	
	@Override
	public String visitBinaryErrorExpr(BinaryError expr) {
		return parenthesize(expr.operator.lexeme, expr.right);
	}

	@Override
	public String visitGroupingExpr(Grouping expr) {
		return parenthesize("group", expr.expression);
	}

	@Override
	public String visitLiteralExpr(Literal expr) {
		if (expr.value == null) return "nil";
		return expr.value.toString();
	}

	@Override
	public String visitUnaryExpr(Unary expr) {
		return parenthesize(expr.operator.lexeme, expr.right);
	}
	
	// Uses varargs to accept different numbers of arguments of the same type
	private String parenthesize(String name, Expr... expr) {
		StringBuilder builder = new StringBuilder();
		
		builder.append("(").append(name);
		for (Expr ex : expr) {
			builder.append(" ");
			builder.append(ex.accept(this)); // This is where the recursion happens, meaning the whole AST can be traversed
		}
		builder.append(")");
		return builder.toString();
	}
	
	// Dummy method to just generate an example AST and print it out
	public static void demo() {
		Expr expression = new Binary(
				new Expr.Unary(
						new Token(TokenType.MINUS, "-", null, 0), 
						new Expr.Literal(2)),
				new Token(TokenType.PLUS, "+", null, 0),
				new Expr.Grouping(
						new Expr.Literal(45.23)));
		
		System.out.println(new AstPrinter().print(expression));
	}

	@Override
	public String visitVariableExpr(Variable expr) {
		return expr.name.lexeme;
	}

}
