package com.ruuhkis.jobfuscator;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import org.objectweb.asm.ClassReader;
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
	
	public ObfuscatedClass(ClassNode node) {
		super();
		this.node = node;
		this.fields = new HashMap<String, String>();
		this.methods = new HashMap<String, String>();
	}

	public ClassNode getNode() {
		return node;
	}

	public void generateNewNames() {
		int counter = 0;
		for(FieldNode field: (List<FieldNode>)node.fields) {
			String originalName = field.name;
			//TODO check for existance
			String newName = ObfuscationContext.getNewName(counter);

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
			
			String newName = ObfuscationContext.getNewName(counter);

			methods.put(originalName, newName);
			
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
	
	public void updateClass(ObfuscationContext context) {
		for(FieldNode field: (List<FieldNode>)node.fields) {
			if(field.desc.startsWith("L")) {
				Type type = Type.getType(field.desc);
				System.out.println();
				
				String newName = context.getClassNames().get(type.getInternalName());
				
				if(newName != null) {
					String newDescriptor = "L" + newName + ";";
					System.out.println("Setting " + field.desc + " to " + newDescriptor);
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
							System.out.println("Renaming " + fieldIns.desc + " field to use " + newName);
							fieldIns.desc = newName;
						} else {
							System.out.println(fieldIns.desc + " haven't been renamed.. " + type.getInternalName());
	
						}
					} else {
					}
					
					String newOwnerName = context.getClassNames().get(fieldIns.owner);
					
					if(newOwnerName != null) {
						System.out.println("Renaming " + fieldIns.owner + " field to use " + newOwnerName);
						fieldIns.owner = newOwnerName;
					} else {
						System.out.println("owner " + fieldIns.owner + " haven't changed :e");
					}
					
					ObfuscatedClass clazz = context.getClass(fieldIns.owner);
					if(clazz == null) {
//						System.out.println(fieldIns.name + " " + fieldIns.owner);
					} else {
						String newFieldName = clazz.getFields().get(fieldIns.name);
						if(newFieldName != null) {
							fieldIns.name = newFieldName;
						} else {
//							System.out.println(fieldIns.name + " doesn't have new name");
						}
					}
					break;
				case AbstractInsnNode.METHOD_INSN:
					MethodInsnNode methodIns = (MethodInsnNode) ins;
					
					newOwnerName = context.getClassNames().get(methodIns.owner);
					
					if(newOwnerName != null) {
						System.out.println("Renaming " + methodIns.owner + " method to use " + newOwnerName);
						methodIns.owner = newOwnerName;
					} else {
						System.out.println("owner " + methodIns.owner + " haven't changed :e");
					}
					

					clazz = context.getClass(methodIns.owner);
					if(clazz == null) {
						System.err.println(methodIns.name + " " + methodIns.owner + "@@@@");
					}
					
					if(clazz == null) {
						System.err.println(methodIns.name + " " + methodIns.owner + "@@@@");
					} else {
						String newFieldName = clazz.getMethods().get(methodIns.name);
//						System.out.println("Figured that " + newFieldName + " is new field name of " + methodIns.name);
						if(newFieldName != null) {
							methodIns.name = newFieldName;
						} else {
//							System.out.println(methodIns.name + " doesn't have new name");
						}

					}
					break;
					
				case AbstractInsnNode.TYPE_INSN:
					TypeInsnNode typeIns = (TypeInsnNode) ins;
					String newDescName = context.getClassNames().get(typeIns.desc);
					
					if(newDescName != null) {
						System.out.println("Renaming typeins " + typeIns.desc + " to use new name " + newDescName);
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
	
	
	
//	public static void main(String[] args) {
//		new ObfuscatedClass(null);
//		for(int i = 0; i < 28; i++) {
//			System.out.println(new ObfuscatedClass(null).getCharacter(i));
//		}
//	}
	
}
