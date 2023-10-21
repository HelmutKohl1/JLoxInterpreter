package com.jlox;

import java.util.List;

import com.jlox.Stmt.Function;

public class LoxFunction implements LoxCallable {

	private final Stmt.Function declaration;
	private final Environment closure;
	
	
	LoxFunction(Stmt.Function declaration, Environment closure){
		this.declaration = declaration;
		this.closure = closure;
	}
	
	
	@Override
	public int arity() {
		return declaration.params.size();
	}

	@Override
	public Object call(Interpreter interpreter, List<Object> arguments) {
		/* create a new environment for the function *call's* own scope, with the enclosing scope 
		 * set to that which lexically encloses it. this allows closures to be possible.
		 * 
		 * This creates an environment chain that matches the lexical scoping of the
		 * function's code.
		 * Giving each call its own environment allows recursion to happen.
		*/
		Environment environment = new Environment(closure);
		for(int i = 0; i < declaration.params.size(); i++) {
			environment.define(declaration.params.get(i).lexeme, arguments.get(i));
		}
		
		try {
			interpreter.executeBlock(declaration.body, environment);	
		}catch(Return returnValue) {
			return returnValue.value;
		}
		return null;
	}

	@Override
	public String toString() {
		return "<fn " + declaration.name.lexeme + ">";
	}
}
