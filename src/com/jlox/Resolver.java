package com.jlox;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import com.jlox.Expr.Assign;
import com.jlox.Expr.Binary;
import com.jlox.Expr.BinaryError;
import com.jlox.Expr.Call;
import com.jlox.Expr.Grouping;
import com.jlox.Expr.Lambda;
import com.jlox.Expr.Literal;
import com.jlox.Expr.Logical;
import com.jlox.Expr.Ternary;
import com.jlox.Expr.Unary;
import com.jlox.Expr.Variable;
import com.jlox.Expr.Visitor;
import com.jlox.Stmt.Block;
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
	
	private void beginScope() {
		scopes.push(new HashMap<String, Boolean>());
	}
	
	private void endScope() {
		scopes.pop();
	}
	
	private void declare(Token name) {
		if (scopes.isEmpty()) return;
		
		Map<String, Boolean> scope = scopes.peek();
		/*setting the value to false marks the identifier as 'not ready',
		 * i.e. its initializer is not resolved, or it is declared but
		 * not yet defined.*/
		scope.put(name.lexeme, false);
	}
	
	private void define(Token name) {
		if(scopes.isEmpty()) return;
		scopes.peek().put(name.lexeme, true);
	}
	
	private void resolveLocal(Expr expr, Token name) {
		for(int i = scopes.size() - 1; i >= 0; i--) {
			if (scopes.get(i).containsKey(name.lexeme)) {
				
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
	public Void visitExpressionStmt(Expression stmt) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Void visitFunctionStmt(Function stmt) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Void visitIfStmt(If stmt) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Void visitPrintStmt(Print stmt) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Void visitReturnStmt(Return stmt) {
		// TODO Auto-generated method stub
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
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Void visitBreakStmt(Break stmt) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Void visitTernaryExpr(Ternary expr) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Void visitAssignExpr(Assign expr) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Void visitBinaryExpr(Binary expr) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Void visitBinaryErrorExpr(BinaryError expr) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Void visitCallExpr(Call expr) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Void visitGroupingExpr(Grouping expr) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Void visitLiteralExpr(Literal expr) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Void visitLogicalExpr(Logical expr) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Void visitUnaryExpr(Unary expr) {
		// TODO Auto-generated method stub
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
		// TODO Auto-generated method stub
		return null;
	}

}
