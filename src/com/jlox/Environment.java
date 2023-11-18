package com.jlox;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

class Environment {
	
	final Environment enclosing;
	private final Map<String, Object> values = new HashMap<>();
	private List<String> declaredOnly = new ArrayList<String>();
	
	Environment(){
		enclosing = null;
	}
	
	Environment(Environment enclosing){
		this.enclosing = enclosing;
	}
	
	Object get(Token name) {
		if (declaredOnly.contains(name.lexeme)) {
			throw new RuntimeError(name, "Uninitialized variable '" + name.lexeme + "'.");
		}
		else if(values.containsKey(name.lexeme)) {
			return values.get(name.lexeme);
		}
		if (enclosing != null) {
			return enclosing.get(name);
		}
		throw new RuntimeError(name, "Undefined variable '" + name.lexeme + "'.");
	}
	
	void define(String name, Object value) {
		/* when a variable is declared, but not defined, we put it in a separate
		 * list, rather than in the values Map*/
		if (value != null) {
			values.put(name, value);
			//System.out.println(name + "added to values");
		}
		else {
			declaredOnly.add(name);
		}
	}
	
	Object getAt(int distance, String name) {
		return ancestor(distance).values.get(name);
	}
	
	Environment ancestor(int distance) {
		Environment environment = this;
		for(int i = 0; i < distance; i++) {
			environment = environment.enclosing;
		}
		return environment;
	}
	
	void assignAt(int distance, Token name, Object value) {
		ancestor(distance).assign(name, value);
	}
	
	void assign(Token name, Object value) {
		if (values.containsKey(name.lexeme)) {
			values.put(name.lexeme, value);
			return;
		}
		else if (declaredOnly.contains(name.lexeme)) {
			/* if the token has already been  declared but not defined, we move it
			 * from the list of declaredOnly and add it to the main values Map*/
			declaredOnly.remove(name.lexeme);
			values.put(name.lexeme, value);
			return;
		}
		
		if (enclosing != null) {
			enclosing.assign(name, value);
			return;
		}
		throw new RuntimeError(name, "Undefined variable '" + name.lexeme + "'.");
	}
}
