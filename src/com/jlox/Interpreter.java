package com.jlox;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;


public class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<Void>{
	
	/* This interpreter is doing a post-order traversal, i.e. it evaluates a node's children
	 * before itself.
	 * 
	 * Essentially, this class converts Stmts/Exprs to Objects.
	 * 
	 * */
	
	final Environment globals = new Environment();
	private Environment environment = globals;
	private final Map<Expr, Integer> locals = new HashMap<>();
	
	private boolean breakActive = false;
	private boolean breakInsideBlockStmt = false;
	
	Interpreter(){
		globals.define("clock", new LoxCallable() {
		@Override
		public int arity() {
			return 0;
		}
		
		@Override
		public Object call(Interpreter interpreter, List<Object> arguments) {
			return (double)System.currentTimeMillis()/1000.0;
		}
		
		@Override
		public String toString() {
			return "<native fn>";
		}
		});
	}
	
	void interpret(List<Stmt> statements) {
		try {
			for (Stmt statement : statements) {
				execute(statement);
			}
		}catch(RuntimeError e) {
			Lox.runtimeError(e);
		}
	}
	
	private Object lookUpVariable(Token name, Expr expr) {
		Integer distance = locals.get(expr);
		if (distance != null) {
			return environment.getAt(distance.intValue(), name.lexeme);
		} else {
			return globals.get(name);
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
	public Object visitSetExpr(Expr.Set expr) {
		Object object = evaluate(expr.object);
		if (!(object instanceof LoxInstance)) {
			throw new RuntimeError(expr.name, "Only instances have fields.");
		}
		
		Object value = evaluate(expr.value);
		((LoxInstance)object).set(expr.name, value);
		return value;
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
		return lookUpVariable(expr.name, expr);
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
	public Object visitCallExpr(Expr.Call expr) {
		Object callee = null;
		try {
			callee = evaluate(expr.callee);
		}catch(RuntimeError e) {
		}
		List<Object> arguments = new ArrayList<>();
		for(Expr argument : expr.arguments) {
			arguments.add(evaluate(argument));//how should a lambda expr evaluate?
		}
		
		if( !(callee instanceof LoxCallable)) {
			throw new RuntimeError(expr.paren, "Can only call functions and classes.");
		}
		LoxCallable function = (LoxCallable)callee;
		if(arguments.size() != function.arity()) {
			throw new RuntimeError(expr.paren, "Expected " + function.arity() + " arguments, but got "
					+ arguments.size() + ".");
		}
		
		return function.call(this, arguments);
	}
	
	@Override
	public Object visitGetExpr(Expr.Get expr) {
		Object object = evaluate(expr.object);
		if (object instanceof LoxInstance) {
			return ((LoxInstance)object).get(expr.name);
		}
		
		throw new RuntimeError(expr.name, "Only instances have properties.");
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
	
	@Override
	public Object visitLambdaExpr(Expr.Lambda expr) {
		return new LoxLambda(expr, environment);
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
	
	protected void resolve(Expr expr, int depth) {
		locals.put(expr, depth);
	}
	
	protected void executeBlock(List<Stmt> statements, Environment environment) {
		Environment previous = this.environment;
		try {
			this.environment = environment;
			for(Stmt stmt : statements) {
				if(breakActive) {
					breakInsideBlockStmt = true;
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
	public Void visitClassStmt(Stmt.Class stmt) {
		/*This two-stage binding process allows references to 
		 * the class itself inside the class's methods*/
		environment.define(stmt.name.lexeme, null);
		LoxClass klass = new LoxClass(stmt.name.lexeme);
		environment.assign(stmt.name, klass);
		return null;
	}
	
	@Override
	public Void visitExpressionStmt(Stmt.Expression stmt) {
		evaluate(stmt.expression);
		return null;
	}
	
	@Override
	public Void visitFunctionStmt(Stmt.Function stmt) {
		LoxFunction function = new LoxFunction(stmt, environment);
		environment.define(stmt.name.lexeme, function);
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
	public Void visitReturnStmt(Stmt.Return stmt) {
		Object value = null;
		if (stmt.value != null) {
			value = evaluate(stmt.value);
		}
		throw new Return(value);
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
				breakActive = false;
				if (breakInsideBlockStmt) {
					breakInsideBlockStmt = false;
					break;
				}
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
		Integer distance = locals.get(expr);
		if (distance != null) {
			environment.assignAt(distance, expr.name, value);
		} else {
			globals.assign(expr.name, value);
		}
		
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
