package com.jlox;

import java.util.List;

import com.jlox.Stmt.Function;

public class LoxFunction implements LoxCallable {

	private final Stmt.Function declaration;
	
	LoxFunction(Stmt.Function declaration){
		this.declaration = declaration;
	}
	
	
	@Override
	public int arity() {
		return declaration.params.size();
	}

	@Override
	public Object call(Interpreter interpreter, List<Object> arguments) {
		/* create a new environment for the function *call's* own scope, with the global scope as the
		enclosing scope. 
		Giving each call its own environment allows recursion to happen.
		*/
		Environment environment = new Environment(interpreter.globals);
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
