package com.ruuhkis.jobfuscator;

import java.util.ArrayList;
import java.util.List;

public class ObfuscationContext {

	private List<ObfuscatedClass> classes;

	public ObfuscationContext() {
		this.classes = new ArrayList<>();
	}
	
	public ObfuscatedClass getClass(String name) {
		for(ObfuscatedClass clazz: classes) {
			if(clazz.getNode().name.equals(name))
				return clazz;
		}
		return null;
	}
	
	public List<ObfuscatedClass> getClasses() {
		return classes;
	}
	
}
