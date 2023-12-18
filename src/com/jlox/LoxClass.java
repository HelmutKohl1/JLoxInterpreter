package com.jlox;

import java.util.List;

public class LoxClass implements LoxCallable{

	final String name;
	
	LoxClass(String name){
		this.name = name;
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
