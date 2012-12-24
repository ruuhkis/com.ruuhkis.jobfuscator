package com.ruuhkis.jobfuscator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

public class ObfuscationContext {

	private List<ObfuscatedClass> classes;
	private Map<String, String> classNames;

	private String mainName;
	
	private static Logger logger = Logger.getLogger(ObfuscationContext.class);
	
	public ObfuscationContext(String mainName) {
		this.mainName = mainName;
		this.classes = new ArrayList<>();
		this.classNames = new HashMap<String, String>();
		logger.setLevel(Level.ALL);
		
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
		for(ObfuscatedClass clazz: classes) {
			if(mainName.equals(clazz.getNode().name)) {
				continue;
			}
			int lastIndexOf = clazz.getNode().name.lastIndexOf('/') + 1;
			
			String packagePrefix = "";
			if(lastIndexOf > 0) {
				packagePrefix = clazz.getNode().name.substring(0, lastIndexOf);
			}
			String newClassName = getNewName();
			String oldName = clazz.getNode().name;
			
			classNames.put(oldName, packagePrefix + newClassName);
			
			logger.debug("Renaming " + clazz.getNode().name + " to " + packagePrefix + newClassName);
			
			clazz.getNode().name = packagePrefix + newClassName;
			
		}
		for(ObfuscatedClass clazz: classes) {
			String newSuperName = classNames.get(clazz.getNode().superName);
			if(newSuperName != null) {
				clazz.getNode().superName = newSuperName;
			}
			
			
			for(int i = 0; i < clazz.getNode().interfaces.size(); i++) {
				String interfaceName = (String) clazz.getNode().interfaces.get(i);
				String newInterfaceName = classNames.get(interfaceName);
				if(newInterfaceName != null) {
					clazz.getNode().interfaces.set(i, newInterfaceName);
				}
			}
		}
	}
	
	
	
	public Map<String, String> getClassNames() {
		return classNames;
	}



	private static final String chars = "abcdefghijklmnopqrstuvwxyz";

	public static int counter = 0;
	
	public static String getNewName() {
		String newName = counter == 0 ? "a" : "";
		int remainder = counter;
		while(remainder != 0) {
			int current = remainder % chars.length();
			remainder = remainder / chars.length();
			newName += chars.charAt(current);
		}
		counter++;
		return newName;
	}
	
	public String getInterfaceMethodName(String interfaceName, String origMethodName) {
		ObfuscatedClass clazz = getClass(interfaceName);
		ClassNode cn = null;
		if(clazz != null) {
			cn = clazz.getNode();
		} else {
			return null;
		}
		
		for(Entry<String, String> entry: clazz.getMethods().entrySet()) {
			if(entry.getKey().equals(origMethodName)) {
				logger.debug(origMethodName + " have changed to " + entry.getValue() + " in " + interfaceName);
				return entry.getValue();
			} else {
				
			}
		}
		
		for(MethodNode method: (List<MethodNode>)clazz.getNode().methods) {
			if(origMethodName.equals(method.name)) {
				logger.debug(origMethodName + " is still the same in " + interfaceName);
				return method.name;
			}
		}
		
		return null;
	}
	

	
	public String getSuperFieldName(String superName, String origFieldName) {
		ObfuscatedClass clazz = getClass(superName);
		ClassNode cn = null;
		if(clazz != null) {
			cn = clazz.getNode();
		} else {
			try {
				ClassReader cr = new ClassReader(superName);
				cn = new ClassNode();
				cr.accept(cn, 0);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		if(clazz != null) {
			for(Entry<String, String> entry: clazz.getFields().entrySet()) {
				if(entry.getKey().equals(origFieldName)) {
					logger.debug("Field " + origFieldName + " have changed to " + entry.getValue() + " in " + superName);
					return entry.getValue();
				} else {
					
				}
			}
		}

		for(FieldNode field: (List<FieldNode>)cn.fields) {
			if(field.name.equals(origFieldName)) {
				logger.debug("Field " + origFieldName + " is still the same in " + superName);

				return field.name;
			}
		}
		
		return cn.superName == null ? null : getSuperFieldName(cn.superName, origFieldName);
	}

	public String getSuperMethodName(String superName, String origMethodName) {
		ObfuscatedClass clazz = getClass(superName);
		ClassNode cn = null;
		if(clazz != null) {
			cn = clazz.getNode();
		} else {
			try {
				ClassReader cr = new ClassReader(superName);
				cn = new ClassNode();
				cr.accept(cn, 0);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		if(clazz != null) {
			for(Entry<String, String> entry: clazz.getMethods().entrySet()) {
				if(entry.getKey().equals(origMethodName)) {
					logger.debug("Method" + origMethodName + " have changed to " + entry.getValue() + " in " + superName);
					return entry.getValue();
				} else {
					
				}
			}
		}

		
		for(String interfaceName: (List<String>)cn.interfaces) {
			String interfaceMethodName = getInterfaceMethodName(interfaceName, origMethodName);
			if(interfaceMethodName != null) {
				logger.debug("Found " + origMethodName + "s name change from interface " + interfaceName + " to " + interfaceMethodName);
				return interfaceMethodName;
			}
		}

		for(MethodNode method: (List<MethodNode>)cn.methods) {
			if(method.name.equals(origMethodName)) {
				logger.debug(origMethodName + " is still the same in " + superName);

				return method.name;
			}
		}
		
		return cn.superName == null ? null : getSuperMethodName(cn.superName, origMethodName);
	}
	
}
