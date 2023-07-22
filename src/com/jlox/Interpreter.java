package com.jlox;

import com.jlox.Expr.Binary;
import com.jlox.Expr.BinaryError;
import com.jlox.Expr.Grouping;
import com.jlox.Expr.Literal;
import com.jlox.Expr.Ternary;
import com.jlox.Expr.Unary;
import com.jlox.Expr.Visitor;

public class Interpreter implements Visitor<Object> {
	/* This interpreter is doing a post-order traversal, i.e. it evaluates a node's children
	 * before itself.
	 * 
	 * Essentially, this class converts Exprs in Objects.
	 * 
	 * */
	
	void interpret(Expr expression) {
		try {
			Object value = evaluate(expression);
			System.out.println(stringify(value));
		}catch(RuntimeError e) {
			Lox.runtimeError(e);
		}
	}
	

	@Override
	public Object visitLiteralExpr(Literal expr) {
		return expr.value;
	}
	
	@Override
	public Object visitGroupingExpr(Grouping expr) {
		return evaluate(expr.expression);
	}
	
	@Override
	public Object visitUnaryExpr(Unary expr) {
		Object right = evaluate(expr.right);
		
		switch (expr.operator.type) {
		case MINUS:
			checkNumberOperand(expr.operator, right);
			return -(double)right;
		case BANG:
			return !isTruthy(right);
		}
		
		//Unreachable
		return null;
	}

	@Override
	public Object visitBinaryExpr(Binary expr) {
		/* Note that the operands are evaluated left to right, so if they have
		 * side-effects, this will matter. 
		 * 
		 * Note also that the both operands are evaluated before they are type-checked.
		 * */
		
		Object left = evaluate(expr.left);
		Object right = evaluate(expr.right);
		
		switch(expr.operator.type) {
		case MINUS:
			checkNumberOperand(expr.operator, left, right);
			return (double)left - (double)right;
		case STAR:
			checkNumberOperand(expr.operator, left, right);
			return (double)left * (double)right;
		case SLASH:
			checkNumberOperand(expr.operator, left, right);
			return (double)left / (double)right;
		case PLUS:
			// Dynamic type-checking
			if (left instanceof Double && right instanceof Double) {
				return (double)left + (double)right;
			}
			if (left instanceof String && right instanceof String) {
				return (String)left + (String)right;
			}
			
			throw new RuntimeError(expr.operator, "Operands must be either two numbers or two strings");
			
		case GREATER:
			checkNumberOperand(expr.operator, left, right);
			return (double)left > (double)right;
		case LESS:
			checkNumberOperand(expr.operator, left, right);
			return (double)left < (double)right;
		case GREATER_EQUAL:
			checkNumberOperand(expr.operator, left, right);
			return (double)left >= (double)right;
		case LESS_EQUAL:
			checkNumberOperand(expr.operator, left, right);
			return (double)left <= (double)right;
		case EQUAL_EQUAL:
			return isEqual(left, right);
		case BANG_EQUAL:
			return !isEqual(left, right);
		}
		
		//Unreachable
		return null;
	}
	
	@Override
	public Object visitTernaryExpr(Ternary expr) {
		Object condition = evaluate(expr.cond);
		if (isTruthy(condition)) {
			return evaluate(expr.left);
		}
		return evaluate(expr.right);
	}

	@Override
	public Object visitBinaryErrorExpr(BinaryError expr) {
		throw new RuntimeError(expr.operator, "\'" + expr.operator.lexeme + "\' requires two operands.");
	}
	
	private void checkNumberOperand(Token operator, Object operand) {
		if (operand instanceof Double) return;
		throw new RuntimeError(operator, "Operand must be a number.");		
	}
	
	private void checkNumberOperand(Token operator, Object left, Object right) {
		if (left instanceof Double && right instanceof Double) return;
		throw new RuntimeError(operator, "Operands must be numbers");
	}
	
	private boolean isEqual(Object a, Object b) {
		if (a == null && b == null) return true;
		if (a == null) return false;
		return a.equals(b);
	}
	
	private boolean isTruthy(Object object) {
		if (object == null) return false;
		if (object instanceof Boolean) return (boolean)object;
		return true;
	}
	
	private Object evaluate(Expr expr) {
		/* Sends the expression back to the interpreter's visitor method for whatever type expr is. */
		return expr.accept(this);
	}

	private String stringify(Object object) {
		if (object == null) return "nil";
		if (object instanceof Double) {
			String text = object.toString();
			if(text.endsWith(".0")) {
				text = text.substring(0, text.length() - 2);
			}
			
			return text;
		}
		
		return object.toString();
	}
}
