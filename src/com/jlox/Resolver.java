package com.jlox;

import java.util.HashMap;

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
		// TODO Auto-generated method stub
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
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Void visitLambdaExpr(Lambda expr) {
		// TODO Auto-generated method stub
		return null;
	}

}
