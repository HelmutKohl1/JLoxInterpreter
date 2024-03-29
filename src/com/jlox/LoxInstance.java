package com.jlox;

import java.util.Map;
import java.util.HashMap;

public class LoxInstance {

	private LoxClass klass;
	private final Map<String, Object> fields = new HashMap<>();
	
	LoxInstance(LoxClass klass){
		this.klass = klass;
	}
	
	Object get(Token name) {
		/*Since we look for fields first, that implies that fields shadow methods.*/
		if (fields.containsKey(name.lexeme)) {
			return fields.get(name.lexeme);
		}
		
		LoxFunction method = null;
		if (klass.klass != null) {
			if (klass.klass.findMethod(name.lexeme) != null) {
				throw new RuntimeError(name, "Cannot access static method " + name.lexeme +  " through an instance.");
			}
		}else {
			method = klass.findMethod(name.lexeme);
		}
		
		//LoxFunction method = klass.findMethod(name.lexeme);
		if (method != null) return method.bind(this);
		
		throw new RuntimeError(name, "Undefined property '" + name.lexeme + "'.");
	}
	
	void set(Token name, Object value) {
		fields.put(name.lexeme, value);
	}
	
	@Override
	public String toString() {
		return "instance of: " + klass.toString();
	}
}
