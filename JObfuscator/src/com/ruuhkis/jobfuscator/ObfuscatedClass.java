package com.ruuhkis.jobfuscator;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;


public class ObfuscatedClass {
	private final String chars = "abcdefghijklmnopqrstuvwxyz";
	
	private ClassNode node;

	private Map<String, String> fields;
	private Map<String, String> methods;
	
	public ObfuscatedClass(ClassNode node) {
		super();
		this.node = node;
		this.fields = new HashMap<String, String>();
	}

	public ClassNode getNode() {
		return node;
	}

	public void generateNewNames() {
		int counter = 0;
		for(FieldNode field: (List<FieldNode>)node.fields) {
			String originalName = field.name;
			
			String newName = getCharacter(counter);

			fields.put(originalName, newName);
			
			field.name = newName;
			
			counter++;
		}
		counter = 0;
		for(MethodNode method: (List<MethodNode>)node.methods) {
			if(isSuperMethod(method, node)) {
				continue;
			}
			System.out.println(node.superName + " " + method.desc + " " + method.name + " " + (method.invisibleAnnotations != null ? method.invisibleAnnotations.size() : "null") + " " + (method.visibleAnnotations == null ? "null" : method.visibleAnnotations.size()));
			
			
			String originalName = method.name;
			
			String newName = getCharacter(counter);

			fields.put(originalName, newName);
			
			method.name = newName;
			
			counter++;
		}
	}

	private boolean isSuperMethod(MethodNode method, ClassNode node) {
		if(node.superName == null)
			return false;
		boolean exists = false;
		ClassNode cn = null;
		try {
			ClassReader cr = new ClassReader(node.superName);
			cn = new ClassNode();
			cr.accept(cn, 0);
		} catch (IOException e) {
			e.printStackTrace();
		}
		for(MethodNode superMethod: (List<MethodNode>)cn.methods) {
			boolean sameSignature = (superMethod.signature == null && method.signature == null) || superMethod.signature.equals(method.signature);
			if(superMethod.name.equals(method.name) && sameSignature) {
				System.out.println(superMethod.name + " exists in parent class(" + cn.name + ")");
				exists = true;
				break;
			}
		}
		return exists || isSuperMethod(method, cn);
	}
	
	public void updateMethods(ObfuscationContext context) {
		for(MethodNode method: (List<MethodNode>)node.methods) {
			ListIterator<AbstractInsnNode> it = method.instructions.iterator();
			
			while(it.hasNext()) {
				AbstractInsnNode ins = it.next();
				
				switch(ins.getType()) {
				case AbstractInsnNode.FIELD_INSN:
					FieldInsnNode fieldIns = (FieldInsnNode) ins;
					ObfuscatedClass clazz = context.getClass(fieldIns.owner);
					if(clazz == null)
						System.out.println(fieldIns.name + " " + fieldIns.owner);
					else {
						String newFieldName = clazz.getFields().get(fieldIns.name);
						if(newFieldName != null) {
							fieldIns.name = newFieldName;
						} else {
							System.out.println(fieldIns.name + " doesn't have new name");
						}
					}
					break;
				case AbstractInsnNode.METHOD_INSN:
					MethodInsnNode methodIns = (MethodInsnNode) ins;
					clazz = context.getClass(methodIns.owner);
					
					if(clazz == null)
						System.out.println(methodIns.name + " " + methodIns.owner);
					else {
						String newFieldName = clazz.getFields().get(methodIns.name);
						System.out.println("Figured that " + newFieldName + " is new field name of " + methodIns.name);
						if(newFieldName != null) {
							methodIns.name = newFieldName;
						} else {
							System.out.println(methodIns.name + " doesn't have new name");
						}
					}
					break;
				}
			}
		}
	}

	public String getCharacter(int counter) {
		String newName = counter == 0 ? "a" : "";
		int remainder = counter;
		while(remainder != 0) {
			int current = remainder % chars.length();
			remainder = remainder / chars.length();
			newName += chars.charAt(current);
		}
		return newName;
	}

	public Map<String, String> getFields() {
		return fields;
	}
	
	
	
//	public static void main(String[] args) {
//		new ObfuscatedClass(null);
//		for(int i = 0; i < 28; i++) {
//			System.out.println(new ObfuscatedClass(null).getCharacter(i));
//		}
//	}
	
}
