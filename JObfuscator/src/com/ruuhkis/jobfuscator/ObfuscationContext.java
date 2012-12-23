package com.ruuhkis.jobfuscator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;

public class ObfuscationContext {

	private List<ObfuscatedClass> classes;
	private Map<String, String> classNames;

	private String mainName;
	
	public ObfuscationContext(String mainName) {
		this.mainName = mainName;
		this.classes = new ArrayList<>();
		this.classNames = new HashMap<String, String>();
	}
	
	public ObfuscatedClass getClass(String name) {
		for(ObfuscatedClass clazz: classes) {
			if(clazz.getNode().name.equals(name)/* || clazz.getNode().name.equals(classNames.get(name))*/)
				return clazz;
		}
		return null;
	}
	
	public List<ObfuscatedClass> getClasses() {
		return classes;
	}
	
	public void generateClassNames() {
		int counter = 100;
		for(ObfuscatedClass clazz: classes) {
			if(mainName.equals(clazz.getNode().name)) {
				continue;
			}
			int lastIndexOf = clazz.getNode().name.lastIndexOf('/') + 1;
			
			String packagePrefix = "";
			if(lastIndexOf > 0) {
				packagePrefix = clazz.getNode().name.substring(0, lastIndexOf);
			}
//			System.out.println(packagePrefix);
			String newClassName = getNewName(counter);
			String oldName = clazz.getNode().name;
			
//			System.out.println("Renaming " + oldName + " to " + packagePrefix + newClassName);
			classNames.put(oldName, packagePrefix + newClassName);
			
			clazz.getNode().name = packagePrefix + newClassName;
			
			counter++;
		}
		for(ObfuscatedClass clazz: classes) {
			String newSuperName = classNames.get(clazz.getNode().superName);
			if(newSuperName != null) {
				clazz.getNode().superName = newSuperName;
//				System.out.println("Updated " + clazz.getNode().name + "'s supercalss to " + newSuperName);
			}
		}
	}
	
	
	
	public Map<String, String> getClassNames() {
		return classNames;
	}



	private static final String chars = "abcdefghijklmnopqrstuvwxyz";

	public static String getNewName(int counter) {
		String newName = counter == 0 ? "a" : "";
		int remainder = counter;
		while(remainder != 0) {
			int current = remainder % chars.length();
			remainder = remainder / chars.length();
			newName += chars.charAt(current);
		}
		return newName;
	}

	public String getSuperMethodName(String superName, String origMethodName) {
		ObfuscatedClass clazz = getClass(superName);
		ClassNode cn = null;
		if(clazz != null) {
			cn = clazz.getNode();
		} else {
			return origMethodName;
			//method haven't been changed because its not found in class list
		}
		
		for(Entry<String, String> entry: clazz.getMethods().entrySet()) {
			if(entry.getKey().equals(origMethodName)) {
				System.err.println(entry.getKey() + " - " + entry.getValue() + " is returned for " + origMethodName);
				return entry.getValue();
			} else {
				System.err.println(entry.getKey() + " - " + entry.getValue() + " isnt right for " + origMethodName);
				
			}
		}
		return cn.superName == null ? null : getSuperMethodName(cn.superName, origMethodName);
	}
	
}
