package com.jlox;

import java.util.List;



public class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<Void>{
	
	/* This interpreter is doing a post-order traversal, i.e. it evaluates a node's children
	 * before itself.
	 * 
	 * Essentially, this class converts Stmts/Exprs to Objects.
	 * 
	 * */
	
	private Environment environment = new Environment();
	private boolean breakActive = false;
	
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
	public Object visitLiteralExpr(Expr.Literal expr) {
		return expr.value;
	}
	
	@Override
	public Object visitLogicalExpr(Expr.Logical expr) {
		Object left = evaluate(expr.left);
		
		// Logical short-circuiting
		if (expr.operator.type == TokenType.OR) {
			if (isTruthy(left)) return left;
		} 
		else {
			if (!isTruthy(left)) return left;
		}
		
		return evaluate(expr.right);
		// the expression "hi" or 2 returns "hi", not true.
	}
	
	@Override
	public Object visitGroupingExpr(Expr.Grouping expr) {
		return evaluate(expr.expression);
	}
	
	@Override
	public Object visitUnaryExpr(Expr.Unary expr) {
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
	public Object visitBinaryExpr(Expr.Binary expr) {
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
	public Object visitTernaryExpr(Expr.Ternary expr) {
		Object condition = evaluate(expr.cond);
		if (isTruthy(condition)) {
			return evaluate(expr.left);
		}
		return evaluate(expr.right);
	}

	@Override
	public Object visitBinaryErrorExpr(Expr.BinaryError expr) {
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
	
	private void executeBlock(List<Stmt> statements, Environment environment) {
		Environment previous = this.environment;
		try {
			this.environment = environment;
			for(Stmt stmt : statements) {
				if(breakActive) {
					//System.out.println("breakActive = false - executeBlock");
					//breakActive = false;
					break;
				}
				execute(stmt);
			}
		} finally {
			this.environment = previous;
		}
	}
	
	@Override
	public Void visitBlockStmt(Stmt.Block stmt) {
		executeBlock(stmt.statements, new Environment(environment));
		return null;
	}
	
	@Override
	public Void visitExpressionStmt(Stmt.Expression stmt) {
		evaluate(stmt.expression);
		return null;
	}
	
	@Override
	public Void visitIfStmt(Stmt.If stmt) {
		if (isTruthy(evaluate(stmt.condition))) {
			execute(stmt.thenBranch);
		} else if (stmt.elseBranch != null) {
			execute(stmt.elseBranch);
		}
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
	
	@Override
	public Void visitWhileStmt(Stmt.While stmt) {
		while (isTruthy(evaluate(stmt.condition))) {
			if (breakActive) { 
				System.out.println("breakActive = false");
				breakActive = false;
				//break;
				return null;
			}
			else {			
				execute(stmt.body);
			}
		}
		return null;
	}
	
	@Override
	public Void visitBreakStmt(Stmt.Break stmt) {
		System.out.println("breakActive = true");
		breakActive = true;
		return null;
	}
	
	@Override 
	public Object visitAssignExpr(Expr.Assign expr) {
		Object value =  evaluate(expr.value);
		environment.assign(expr.name, value);
		return value;
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
