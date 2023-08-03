/**This file has been automatically generated by: GenerateAst.java*/

package com.jlox;

import java.util.List;

abstract class Stmt {

	abstract <R> R accept(Visitor<R> visitor);

	interface Visitor<R> {
	R visitExpressionStmt(Expression stmt);
	R visitPrintStmt(Print stmt);
	}
static class Expression extends Stmt {

	final Expr expression;

	Expression (Expr expression) {
		this.expression = expression;

	}

	@Override
	<R> R accept(Visitor<R> visitor){
		return visitor.visitExpressionStmt(this);
	}
}
static class Print extends Stmt {

	final Expr expression;

	Print (Expr expression) {
		this.expression = expression;

	}

	@Override
	<R> R accept(Visitor<R> visitor){
		return visitor.visitPrintStmt(this);
	}
}
}
