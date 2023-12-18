package com.jlox;

public class LoxInstance {

	private LoxClass klass;
	
	LoxInstance(LoxClass klass){
		this.klass = klass;
	}
	
	@Override
	public String toString() {
		return "instance of: " + klass.toString();
	}
}
