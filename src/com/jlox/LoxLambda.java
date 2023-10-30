package com.jlox;

import java.util.List;

public class LoxLambda implements LoxCallable {
	
	final Expr.Lambda declaration;
	final Environment closure;

	LoxLambda(Expr.Lambda declaration, Environment closure){
		this.declaration = declaration;
		this.closure = closure;
	}
	
	@Override
	public int arity() {
		return declaration.params.size();
	}

	@Override
	public Object call(Interpreter interpreter, List<Object> arguments) {
		/*create a new environment for the lambda function's own scope.
		 * define the arguments passed to the parameters*/
		Environment local = new Environment(closure);
		for (int i = 0; i < declaration.params.size(); i++) {
			local.define(declaration.params.get(i).lexeme, arguments.get(i));
		}
		
		try {
			interpreter.executeBlock(declaration.body, local);
		}catch(Return returnValue) {
			return returnValue.value;
		}
		return null;
	}

	@Override
	public String toString() {
		return "<lambda " + declaration.params.toString() + ">";
	}
}
