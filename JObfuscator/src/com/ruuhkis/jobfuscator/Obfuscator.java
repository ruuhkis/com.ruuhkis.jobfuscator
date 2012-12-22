package com.ruuhkis.jobfuscator;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

public class Obfuscator {
	
	private ObfuscationContext context;

	public Obfuscator() {
		this.context = new ObfuscationContext();
	}
	
	public void obfuscateJar(String jarLocation) throws IOException {
		JarFile file = new JarFile(new File(jarLocation));
		Enumeration<JarEntry> entries = file.entries();
		while(entries.hasMoreElements()) {
			JarEntry entry = entries.nextElement();
			if(entry.getName().endsWith(".class")) {
				ClassReader reader = new ClassReader(file.getInputStream(entry));
				ClassNode cn = new ClassNode();
				reader.accept(cn, 0);
				
				String className = reader.getClassName();
				
				context.getClasses().add(new ObfuscatedClass(cn));
				
				System.out.println(className);
				
			}
		}
		file.close();
		
		obfuscateClasses();
		
		updateClasses();
	}
	
	private void updateClasses() {
		for(ObfuscatedClass clazz: context.getClasses()) {
			clazz.updateMethods(context);
		}
	}

	private void obfuscateClasses() {
		for(ObfuscatedClass clazz: context.getClasses()) {
			clazz.generateNewNames();
		}
	}

	public void saveFiles() throws FileNotFoundException, IOException {
		for(ObfuscatedClass clazz: context.getClasses()) {
			ClassWriter cw = new ClassWriter(0);
			if(clazz.getNode() == null)
				System.out.println("!!!");
			clazz.getNode().accept(cw);
			File outputFile = new File(clazz.getNode().name + ".class");
			if(outputFile.getParentFile() != null) {
				outputFile.getParentFile().mkdirs();
			}
			BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(outputFile));
			byte[] data = cw.toByteArray();
			bos.write(data);
			bos.close();
			
		}
		
		
	}

	

	public static void main(String[] args) {
		if(args.length > 0) {
			Obfuscator obfuscator = new Obfuscator();
			try {
				obfuscator.obfuscateJar(args[0]);
				obfuscator.saveFiles();
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else {
			System.out.println("Obfuscator requires jar as argument");
		}
	}
	
}
