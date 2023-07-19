package com.jlox;

import com.jlox.Expr.Binary;
import com.jlox.Expr.Grouping;
import com.jlox.Expr.Literal;
import com.jlox.Expr.Ternary;
import com.jlox.Expr.Unary;
import com.jlox.Expr.Visitor;

public class RPNFormatter implements Visitor<String> {

	public String print(Expr expr) {
		// .accept(this) calls the appropriate visitType method with the RPNformatter instance as the argument
		return expr.accept(this); 
	}
	
	@Override
	public String visitTernaryExpr(Ternary expr) {
		return revPolish(expr.qmark.lexeme, expr.cond, expr.left, expr.right);
	}
	
	@Override
	public String visitBinaryExpr(Binary expr) {
		return revPolish(expr.operator.lexeme, expr.left, expr.right);
	}

	@Override
	public String visitGroupingExpr(Grouping expr) {
		return revPolish("group", expr.expression);
	}

	@Override
	public String visitLiteralExpr(Literal expr) {
		if (expr.value == null) return "nil";
		return revPolish(expr.value.toString());
	}

	@Override
	public String visitUnaryExpr(Unary expr) {
		return revPolish(expr.operator.lexeme, expr.right);
	}

	private String revPolish(String name, Expr... exprs) {
		StringBuilder builder = new StringBuilder();

		for(Expr expr : exprs) {
			builder.append(expr.accept(this));
			if (name != "group") builder.append(" ");
		}
		if (name != "group") builder.append(name);
		
		return builder.toString();
		
	}
}
