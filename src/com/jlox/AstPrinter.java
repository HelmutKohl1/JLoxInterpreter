package com.jlox;

import com.jlox.Expr.Ternary;
import com.jlox.Expr.This;

import java.util.List;

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
import com.jlox.Expr.Super;
import com.jlox.Expr.Unary;
import com.jlox.Expr.Variable;
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

public class AstPrinter implements Expr.Visitor<String>, Stmt.Visitor<String> {

	String print(Expr expr) {
		return expr.accept(this);
	}
	
	String print(Stmt stmt) {
		return stmt.accept(this);
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
	
	public String visitLogicalExpr(Logical expr) {
		return parenthesize(expr.operator.lexeme, expr.left, expr.right);
	}
	
	@Override
	public String visitCallExpr(Call expr) {	
		return parenthesize("call " + expr.callee.accept(this), expr.arguments.toArray(new Expr[1]));
	}
	
	// Uses var args to accept different numbers of arguments of the same type
	private String parenthesize(String name, Expr... expr) {
		StringBuilder builder = new StringBuilder();
		
		builder.append("(").append(name);
		for (Expr ex : expr) {
			if (ex != null) {
			builder.append(" ");
			builder.append(ex.accept(this)); // This is where the recursion happens, meaning the whole AST can be traversed		
			}
		}
		builder.append(")");
		return builder.toString();
	}
	
	private String parenthesizeStmt(String name, Stmt... stmt) {
		StringBuilder builder = new StringBuilder();
		
		builder.append("(").append(name);
		for(Stmt st : stmt) {
			builder.append(" ");
			builder.append(st.accept(this));
		}
		builder.append(")");
		return builder.toString();
	}
	
	private String parenthesizeStmt(String name, List<Stmt> statements) {
		StringBuilder builder = new StringBuilder();
		
		builder.append("(").append(name);
		for(Stmt st : statements) {
			if (st != null) {
				builder.append(" ");
				builder.append(st.accept(this));
			}
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
		return parenthesize(expr.name.lexeme);
	}

	@Override
	public String visitAssignExpr(Assign expr) {
		return parenthesize(expr.name.lexeme, expr.value);
	}
	
	@Override
	public String visitLambdaExpr(Lambda expr) {
		String declaration = parenthesize("lambda: " + expr.params.toString());
		return parenthesizeStmt(declaration, expr.body);
	}

	@Override
	public String visitBlockStmt(Block stmt) {
		return parenthesizeStmt("block: ", stmt.statements);
	}

	@Override 
	public String visitClassStmt(Class stmt) {
		return parenthesize("class declaration: " + stmt.name.lexeme);
	}
	
	@Override
	public String visitExpressionStmt(Expression stmt) {
		return parenthesize("expr: ", stmt.expression);
	}

	@Override
	public String visitIfStmt(If stmt) {
		String topline =  parenthesize("if: ", stmt.condition);
		if (stmt.elseBranch != null) {
			return parenthesizeStmt(topline, stmt.thenBranch, stmt.elseBranch);
		} else {
			return parenthesizeStmt(topline, stmt.thenBranch);
		}
	}

	@Override
	public String visitPrintStmt(Print stmt) {
		return parenthesize("print: ", stmt.expression);
	}

	@Override
	public String visitVarStmt(Var stmt) {
		if (stmt.initializer != null) {
			return parenthesize("var: " + stmt.name.lexeme, stmt.initializer);
		} else {
			return parenthesize("var: " + stmt.name.lexeme);
		}

	}

	@Override
	public String visitWhileStmt(While stmt) {
		String topline = parenthesize("while: ", stmt.condition);
		return parenthesizeStmt(topline, stmt.body);
	}

	@Override
	public String visitBreakStmt(Break stmt) {
		return "break";
	}

	@Override
	public String visitFunctionStmt(Function stmt) {
		String declaration = parenthesize("fun declaration <" + stmt.name.lexeme + "> " + stmt.params.toString());
		return parenthesizeStmt(declaration, stmt.body);
	}

	@Override
	public String visitReturnStmt(Return stmt) {
		return parenthesize(stmt.keyword.lexeme, stmt.value);
	}

	@Override
	public String visitGetExpr(Get expr) {
		return parenthesize("get: " + expr.object.toString() + "." + expr.name.lexeme);
	}

	@Override
	public String visitSetExpr(Set expr) {
		return parenthesize("set: " + expr.object.toString() + "." + expr.name.lexeme);
	}

	@Override
	public String visitThisExpr(This expr) {
		return parenthesize("this");
	}

	@Override
	public String visitSuperExpr(Super expr) {
		return parenthesize("super");
	}
}
