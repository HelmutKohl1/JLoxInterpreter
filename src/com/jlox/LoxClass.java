package com.jlox;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class LoxClass implements LoxCallable{

	final String name;
	private final Map<String, LoxFunction> methods;
	/*Methods are owned by the class itself, but accessed through instances.
	 * Instances store state.
	 * */
	
	LoxClass(String name, Map<String, LoxFunction> methods){
		this.name = name;
		this.methods = methods;
	}
	
	LoxFunction findMethod(String name) {
		if (methods.containsKey(name)) {
			return methods.get(name);	
		}
		return null;
	}
	
	@Override
	public int arity() {
		return 0;
	}
	
	@Override
	public Object call(Interpreter interpreter, List<Object> arguments) {
		/*This is essentially the default constructor for a Lox Class, syntax is:
		 * 
		 * class Bagel{}
		 * var myBagel = Bagel();
		 * 
		 * i.e. The constructor is called by calling the class name itself.
		 * */
		LoxInstance instance = new LoxInstance(this);
		return instance;
	}
	
	@Override
	public String toString() {
		return name;
	}
}
