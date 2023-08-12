package com.jlox;

import java.util.List;

import com.jlox.Expr.Binary;
import com.jlox.Expr.BinaryError;
import com.jlox.Expr.Grouping;
import com.jlox.Expr.Literal;
import com.jlox.Expr.Ternary;
import com.jlox.Expr.Unary;

public class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<Void>{
	
	/* This interpreter is doing a post-order traversal, i.e. it evaluates a node's children
	 * before itself.
	 * 
	 * Essentially, this class converts Stmts/Exprs to Objects.
	 * 
	 * */
	
	private Environment environment = new Environment();
	
	void interpret(List<Stmt> statements) {
		try {
			for (Stmt statement : statements) {
				execute(statement);
			}
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
	public Object visitVariableExpr(Expr.Variable expr) {
		return environment.get(expr.name);
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
			if ((double)right == 0) {
				throw new RuntimeError(expr.operator, "Division by zero.");
			}
			return (double)left / (double)right;
		case PLUS:
			// Dynamic type-checking
			if (left instanceof Double && right instanceof Double) {
				return (double)left + (double)right;
			}
			if (left instanceof String || right instanceof String) {
				if (left instanceof String) {
					return left + stringify(right);
				}else {
					return stringify(left) + right;
				}
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
	
	private Void execute(Stmt statement) {
		/* Calls the statement's accept method on the interpreter, which in turn calls the correct 
		 * visit method depending on the type of 'statement' */
		return statement.accept(this);
	}
	
	@Override
	public Void visitExpressionStmt(Stmt.Expression stmt) {
		evaluate(stmt.expression);
		return null;
	}
	
	@Override
	public Void visitPrintStmt(Stmt.Print stmt) {
		Object value = evaluate(stmt.expression);
		System.out.println(stringify(value));
		return null;
	}

	@Override
	public Void visitVarStmt(Stmt.Var stmt) {
		Object value = null;
		if (stmt.initializer != null ) {
			value = evaluate(stmt.initializer);
		}		
		environment.define(stmt.name.lexeme, value);
		return null;
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
