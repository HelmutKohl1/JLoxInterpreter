package com.jlox;

import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Stack;

import com.jlox.Expr.Assign;
import com.jlox.Expr.Binary;
import com.jlox.Expr.BinaryError;
import com.jlox.Expr.Call;
import com.jlox.Expr.Get;
import com.jlox.Expr.Grouping;
import com.jlox.Expr.Lambda;
import com.jlox.Expr.Literal;
import com.jlox.Expr.Logical;
import com.jlox.Expr.Set;
import com.jlox.Expr.Ternary;
import com.jlox.Expr.This;
import com.jlox.Expr.Unary;
import com.jlox.Expr.Variable;
import com.jlox.Expr.Visitor;
import com.jlox.Stmt.Block;
import com.jlox.Stmt.Class;
import com.jlox.Stmt.Break;
import com.jlox.Stmt.Expression;
import com.jlox.Stmt.Function;
import com.jlox.Stmt.If;
import com.jlox.Stmt.Print;
import com.jlox.Stmt.Return;
import com.jlox.Stmt.Var;
import com.jlox.Stmt.While;

public class Resolver implements Visitor<Void>, com.jlox.Stmt.Visitor<Void> {
	
	private final Interpreter interpreter;
	/*The scopes stack stores only local block scopes, i.e. not the
	 * global scope.*/
	private Stack<Map<String, Boolean>> scopes = new Stack<>();
	private Map<Token, Boolean> localsUsed = new HashMap<>();
	private FunctionType currentFunction = FunctionType.NONE;
	private ClassType currentClass = ClassType.NONE;
	
	private enum FunctionType{
		NONE,
		FUNCTION,
		METHOD,
		STATIC_METHOD,//unsure if actually needed
		INITIALIZER
	}
	
	private enum ClassType{
		CLASS,
		NONE
	}
	
	Resolver(Interpreter interpreter){
		this.interpreter = interpreter;
	}

	void resolve(List<Stmt> stmts) {
		for (Stmt stmt : stmts) {
			resolve(stmt);
		}
	}
	
	private void resolve(Stmt stmt) {
		stmt.accept(this);
	}

	private void resolve(Expr expr) {
		expr.accept(this);
	}
	
	private void resolveFunction(Function stmt, FunctionType type) {
		FunctionType enclosingFunction = currentFunction;
		currentFunction = type;
		
		beginScope();
		for (Token param : stmt.params) {
			declare(param);
			define(param);
		}
		resolve(stmt.body);
		endScope();
		currentFunction = enclosingFunction;
	}
	
	private void beginScope() {
		scopes.push(new HashMap<String, Boolean>());
		localsUsed.clear();
	}
	
	private void endScope() {
		scopes.pop();
		for (Token local : localsUsed.keySet()) {
			if (localsUsed.get(local) == false) {
				Lox.error(local, "Unused local variable: '" + local.lexeme + "'");
			}
		}
	}
	
	private void declare(Token name) {
		if (scopes.isEmpty()) return;
		
		Map<String, Boolean> scope = scopes.peek();
		if (scope.containsKey(name.lexeme)) {
			Lox.error(name, "Already a variable with this name in this scope.");
		}
		
		/*setting the value to false marks the identifier as 'not ready',
		 * i.e. its initializer is not resolved, or it is declared but
		 * not yet defined.*/
		scope.put(name.lexeme, false);
		localsUsed.put(name, false);
	}
	
	private void define(Token name) {
		if(scopes.isEmpty()) return;
		scopes.peek().put(name.lexeme, true);
	}
	
	private void resolveLocal(Expr expr, Token name) {
		for(int i = scopes.size() - 1; i >= 0; i--) {
			if (scopes.get(i).containsKey(name.lexeme)) {
				interpreter.resolve(expr, scopes.size() - 1 - i);
				localsUsed.put(name, true);
				return;
			}
		}
	}
	
	@Override
	public Void visitBlockStmt(Block stmt) {
		beginScope();
		resolve(stmt.statements);
		endScope();
		return null;
	}

	@Override
	public Void visitClassStmt(Class stmt) {		
		ClassType enclosingClass = currentClass;
		currentClass = ClassType.CLASS;
		declare(stmt.name);
		define(stmt.name);
		
		if (stmt.metaclass != null) {
			declare(stmt.metaclass.name);
			define(stmt.metaclass.name);
		}
	
		beginScope();
		//should the static methods go in this scope?
		//how to handle the metaclass's scope generally.
		/* should the metaclass's methods (i.e. the class's static methods) be resolved into 
		 * a separate scope? - could go in the same scope so that static methods can access
		 * 'this'.
		 * */
		/*we could also resolve the name of the class to it's metaclass so that when it's
		* called, the name refers to the metaclass.*/
		
		/*We declare 'this' as an implicit field of the class in its
		 * own scope which is a closure for the class's methods.*/
		scopes.peek().put("this", true);
		
		for (Stmt.Function method : stmt.methods) {
			FunctionType declaration = FunctionType.METHOD;
			if (method.name.lexeme.equals("init")) {
				declaration = FunctionType.INITIALIZER;
			}
			resolveFunction(method, declaration);
		}
		
		if (stmt.metaclass != null) {
			for(Stmt.Function staticMethod : stmt.metaclass.methods) {
				FunctionType declaration = FunctionType.STATIC_METHOD;
				resolveFunction(staticMethod, declaration);
			}			
		}
		endScope();
		currentClass = enclosingClass;
		return null;
	}
	
	@Override
	public Void visitExpressionStmt(Expression stmt) {
		resolve(stmt.expression);
		return null;
	}

	@Override
	public Void visitFunctionStmt(Function stmt) {
		/*function name is declared immediately so that the body can
		 * recursively refer to it.*/
		declare(stmt.name);
		define(stmt.name);
		
		resolveFunction(stmt, FunctionType.FUNCTION);
		return null;
	}

	@Override
	public Void visitIfStmt(If stmt) {
		resolve(stmt.condition);
		resolve(stmt.thenBranch);
		if (stmt.elseBranch != null) {
			resolve(stmt.elseBranch);
		}
		return null;
	}

	@Override
	public Void visitPrintStmt(Print stmt) {
		resolve(stmt.expression);
		return null;
	}

	@Override
	public Void visitReturnStmt(Return stmt) {
		if (currentFunction == FunctionType.NONE) {
			Lox.error(stmt.keyword, "Can't return from top-level code.");
		}
		else if (currentFunction == FunctionType.INITIALIZER) {
			Lox.error(stmt.keyword, "Can't return a value from an initializer.");
		}
		
		if (stmt.value != null) {
			resolve(stmt.value);
		}
		return null;
	}

	@Override
	public Void visitVarStmt(Var stmt) {
		declare(stmt.name);
		if (stmt.initializer != null) {
			resolve(stmt.initializer);
		}
		define(stmt.name);
		return null;
	}

	@Override
	public Void visitWhileStmt(While stmt) {
		resolve(stmt.condition);
		resolve(stmt.body);
		return null;
	}

	@Override
	public Void visitBreakStmt(Break stmt) {
		// Nothing to resolve.
		return null;
	}

	@Override
	public Void visitTernaryExpr(Ternary expr) {
		resolve(expr.cond);
		resolve(expr.left);
		resolve(expr.right);
		return null;
	}

	@Override
	public Void visitAssignExpr(Assign expr) {
		resolve(expr.value);
		resolveLocal(expr, expr.name);
		return null;
	}

	@Override
	public Void visitBinaryExpr(Binary expr) {
		resolve(expr.left);
		resolve(expr.right);
		return null;
	}

	@Override
	public Void visitBinaryErrorExpr(BinaryError expr) {
		resolve(expr.right);
		return null;
	}

	@Override
	public Void visitCallExpr(Call expr) {
		resolve(expr.callee);
		for(Expr arg : expr.arguments) {
			resolve(arg);
		}
		return null;
	}

	@Override
	public Void visitGetExpr(Get expr) {
		/*Member lookup is dynamic since the member name isn't resolved here.*/
		resolve(expr.object);
		return null;
	}
	
	@Override
	public Void visitGroupingExpr(Grouping expr) {
		resolve(expr.expression);
		return null;
	}

	@Override
	public Void visitLiteralExpr(Literal expr) {
		// Nothing to resolve in a literal.
		return null;
	}

	@Override
	public Void visitLogicalExpr(Logical expr) {
		resolve(expr.left);
		resolve(expr.right);
		return null;
	}
	
	@Override
	public Void visitSetExpr(Set expr) {
		/*As for the get expression, we don't resolve the 
		 * property's name as they are dynamically evaluated.*/
		resolve(expr.value);
		resolve(expr.object);
		return null;
	}
	
	@Override
	public Void visitThisExpr(This expr) {
		if (currentClass == ClassType.NONE) {
			Lox.error(expr.keyword, "Cannot use 'this' keyword outside of a class.");
			return null;
		}
		resolveLocal(expr, expr.keyword);
		return null;
	}

	@Override
	public Void visitUnaryExpr(Unary expr) {
		resolve(expr.right);
		return null;
	}

	@Override
	public Void visitVariableExpr(Variable expr) {
		if (!scopes.isEmpty() && scopes.peek().get(expr.name.lexeme) == Boolean.FALSE) {
			Lox.error(expr.name, "Can't read local variable in its own initializer.");
		}
		
		resolveLocal(expr, expr.name);
		return null;
	}

	@Override
	public Void visitLambdaExpr(Lambda expr) {
		/*This will need checking.*/
		beginScope();
		for (Token param : expr.params) {
			declare(param);
			define(param);
		}
		resolve(expr.body);
		endScope();
		return null;
	}

}
