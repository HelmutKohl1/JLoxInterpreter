package com.jlox;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class LoxClass extends LoxInstance implements LoxCallable{
	
	/*Members from LoxInstance*/
	public LoxClass klass;//Field which stores the class's metaclass
	final LoxClass superclass;
	private final Map<String, Object> fields = null;//null since the class itself has no fields.
	
	final String name;
	private final Map<String, LoxFunction> methods;
	/*Methods are owned by the class itself, but accessed through instances.
	 * Instances store state.
	 * */
	
	LoxClass(LoxClass klass, LoxClass superclass, String name, Map<String, LoxFunction> methods){
		super(klass);
		this.superclass = superclass;
		this.name = name;
		this.methods = methods;
		System.out.println("LoxClass created. klass: " + klass + " name: " + name + " with " + methods.size() + " methods.");
	}
	
	LoxFunction findMethod(String name) {
		if (methods.containsKey(name)) {
			return methods.get(name);	
		}
		
		if (superclass != null) {
			return superclass.findMethod(name);
		}
		return null;
	}
	
	@Override
	public int arity() {
		if (findMethod("init") == null) {
			return 0;
		}
		return findMethod("init").arity();
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
		LoxFunction initializer = findMethod("init");
		if (initializer != null) {
			initializer.bind(instance).call(interpreter, arguments);
		}
		return instance;
	}
	
	@Override
	public String toString() {
		return name;
	}
}
