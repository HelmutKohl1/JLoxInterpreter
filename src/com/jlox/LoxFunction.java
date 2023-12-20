package com.jlox;

import java.util.List;

import com.jlox.Stmt.Function;

public class LoxFunction implements LoxCallable {

	private final Stmt.Function declaration;
	private final Environment closure;
	private final boolean isInitializer;
	
	
	LoxFunction(Stmt.Function declaration, Environment closure, boolean isInitializer){
		this.declaration = declaration;
		this.closure = closure;
		this.isInitializer = isInitializer;
	}
	
	LoxFunction bind(LoxInstance instance) {
		Environment environment = new Environment(closure);
		environment.define("this", instance);
		return new LoxFunction(declaration, environment, isInitializer);
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
			/*If an empty return statement exists inside a class's
			 * initializer, it should return 'this' rather than 'nil'*/
			if (isInitializer) {
				return closure.getAt(0, "this");
			}
			return returnValue.value;
		}
		
		if (isInitializer) return closure.getAt(0, "this");
		
		return null;
	}

	@Override
	public String toString() {
		return "<fn " + declaration.name.lexeme + ">";
	}
}
