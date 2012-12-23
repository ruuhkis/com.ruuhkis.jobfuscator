package com.ruuhkis.jobfuscator;

import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import org.apache.log4j.Logger;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;


public class ObfuscatedClass {
	
	private ClassNode node;

	private Map<String, String> fields;
	private Map<String, String> methods;

	private ObfuscationContext context;
	private Logger logger;
	
	public ObfuscatedClass(ObfuscationContext context, ClassNode node) {
		super();
		this.node = node;
		this.context = context;
		this.fields = new HashMap<String, String>();
		this.methods = new HashMap<String, String>();
		this.logger = Logger.getLogger(ObfuscatedClass.class);
	}

	public ClassNode getNode() {
		return node;
	}

	public void generateNewNames() {
		int counter = 0;
		//generating new names for fields
		for(FieldNode field: (List<FieldNode>)node.fields) {
			String originalName = field.name;
			//TODO check for existance
			String newName = ObfuscationContext.getNewName(counter);
			logger.debug("Generated new name to " + node.name + "'s field " + originalName + " and renaming it to " + newName);

			fields.put(originalName, newName);
			
			field.name = newName;
			
			counter++;
		}
		counter = 0;
		//generating new names for methods
		for(MethodNode method: (List<MethodNode>)node.methods) {
			String methodName = context.getSuperMethodName(node.superName, method.name);
			
			for(String interfaceName: (List<String>)node.interfaces) {
				String interfaceMethodName = context.getInterfaceMethodName(interfaceName, method.name);
				if(interfaceMethodName != null) {
					methodName = interfaceMethodName;
					break;
				} else {
					
				}
			}
			
			if(methodName != null) { //exists in superclass
				continue;
			}
			
			String originalName = method.name;
			
			String newName = ObfuscationContext.getNewName(counter);
			
			logger.debug("Generated new name to " + node.name + "'s method " + originalName + " and renaming it to " + newName);

			

			methods.put(originalName, newName);
			
			method.name = newName;
			
			counter++;
		}
		
	}
	
	//updating methods according to superclasses renaming
	public void updateSuperMethods() {

		for(MethodNode method: (List<MethodNode>)node.methods) {
			String methodName = context.getSuperMethodName(node.superName, method.name);
			
			for(String interfaceName: (List<String>)node.interfaces) {
				String interfaceMethodName = context.getInterfaceMethodName(interfaceName, method.name);
				if(interfaceMethodName != null) {
					methodName = interfaceMethodName;
					break;
				} else {
				}
			}
			
			if(methodName != null) { //exists in superclass
				methods.put(method.name, methodName);
				method.name = methodName;
				logger.debug("Found out that superclasses/interfaces method of " + node.name + " have changed from " + method.name + " to " + methodName);
				continue;
			} else {
//				System.err.println(method.name + " doesn't exist in superclass :e");
			}
		}
	}
	
	public void updateClass() {
		for(FieldNode field: (List<FieldNode>)node.fields) {
			if(field.desc.startsWith("L")) {
				Type type = Type.getType(field.desc);
				
				String newName = context.getClassNames().get(type.getInternalName());
				
				if(newName != null) {
					String newDescriptor = "L" + newName + ";";
					field.desc = newDescriptor;
				}
			}
		}
		
		for(MethodNode method: (List<MethodNode>)node.methods) {
			ListIterator<AbstractInsnNode> it = method.instructions.iterator();
			
			while(it.hasNext()) {
				AbstractInsnNode ins = it.next();
				
				switch(ins.getType()) {
				case AbstractInsnNode.FIELD_INSN:
					FieldInsnNode fieldIns = (FieldInsnNode) ins;
					
					if(fieldIns.desc.startsWith("L")) {
						Type type = Type.getType(fieldIns.desc);
						
						
						
						String newName = context.getClassNames().get(type.getInternalName());
						
						if(newName != null) {
							newName = "L" + newName + ";";
							fieldIns.desc = newName;
						} else {
						}
					} else {
					}
					
					String newOwnerName = context.getClassNames().get(fieldIns.owner);
					
					if(newOwnerName != null) {
						fieldIns.owner = newOwnerName;
					} else {
					}
					
					ObfuscatedClass clazz = context.getClass(fieldIns.owner);
					if(clazz == null) {
					} else {
						String newFieldName = clazz.getFields().get(fieldIns.name);
						if(newFieldName != null) {
							fieldIns.name = newFieldName;
						} else {
						}
					}
					break;
				case AbstractInsnNode.METHOD_INSN:
					MethodInsnNode methodIns = (MethodInsnNode) ins;
					
					newOwnerName = context.getClassNames().get(methodIns.owner);
					
					if(newOwnerName != null) {
						methodIns.owner = newOwnerName;
					} else {
					}
					

					clazz = context.getClass(methodIns.owner);
					
					if(clazz != null) {
						String newMethodName = clazz.getMethods().get(methodIns.name);
						
						String superMethodName = context.getSuperMethodName(clazz.getNode().superName, methodIns.name);
						
						if(superMethodName != null) {
							newMethodName = superMethodName;
						}
						
						
						if(newMethodName != null) {
							methodIns.name = newMethodName;
						} else {
						}

					}
					break;
					
				case AbstractInsnNode.TYPE_INSN:
					TypeInsnNode typeIns = (TypeInsnNode) ins;
					String newDescName = context.getClassNames().get(typeIns.desc);
					
					if(newDescName != null) {
						typeIns.desc = newDescName;
					}
					
					break;
				}
			}
		}
	}



	public Map<String, String> getFields() {
		return fields;
	}

	public Map<String, String> getMethods() {
		return methods;
	}
	
}
