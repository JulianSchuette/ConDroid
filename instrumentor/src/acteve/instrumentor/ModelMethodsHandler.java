/*
  Copyright (c) 2011,2012, 
   Saswat Anand (saswat@gatech.edu)
   Mayur Naik  (naik@cc.gatech.edu)
  All rights reserved.
  
  Redistribution and use in source and binary forms, with or without
  modification, are permitted provided that the following conditions are met: 
  
  1. Redistributions of source code must retain the above copyright notice, this
  list of conditions and the following disclaimer. 
  2. Redistributions in binary form must reproduce the above copyright notice,
  this list of conditions and the following disclaimer in the documentation
  and/or other materials provided with the distribution. 
  
  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
  ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
  DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
  ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
  (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
  LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
  ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
  SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
  
  The views and conclusions contained in the software and documentation are those
  of the authors and should not be interpreted as representing official policies, 
  either expressed or implied, of the FreeBSD Project.
*/

package acteve.instrumentor;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import soot.ArrayType;
import soot.Local;
import soot.Modifier;
import soot.PrimType;
import soot.RefType;
import soot.Scene;
import soot.SootClass;
import soot.SootField;
import soot.SootMethod;
import soot.SourceLocator;
import soot.Type;
import soot.VoidType;
import soot.jimple.IntConstant;
import soot.jimple.InvokeExpr;
import soot.jimple.NullConstant;

public class ModelMethodsHandler {
	private static final Set<SootMethod> methodsWithModels = new HashSet();
	private static final Map<SootClass, Set<String>> klassToModelMethodSubsigs = new HashMap<SootClass, Set<String>>();
	private static final Set<SootField> fieldsWithModels = new HashSet();

	static boolean modelExistsFor(SootMethod method) {
		return methodsWithModels.contains(method);
	}
	
	static boolean modelExistsFor(SootField field) {
		return fieldsWithModels.contains(field);
	}

	public static SootMethod getModelInvokerFor(SootMethod method) {
		if (!modelExistsFor(method)) 
			return null;

		SootClass klass = method.getDeclaringClass();
		String modelMethodName = modelMethodNameFor(method);
		SootClass invokerClass = getModelInvokerClassFor(klass);
		if (invokerClass.declaresMethodByName(modelMethodName)) {
			return invokerClass.getMethodByName(modelMethodName);
		}
		System.out.println("Model invoker class does not define method " + modelMethodName + " I will create a stub");

		List paramTypes = new ArrayList();
		if (!method.isStatic())
			paramTypes.add(klass.getType());
		paramTypes.addAll(method.getParameterTypes());
		Type retType = VoidType.v();
		String modelSubsignature = getModelMethodSubsignatureFor(method);
		return addInvokerMethod(klass, invokerClass, modelMethodName, paramTypes, retType, modelSubsignature);
	}

	public static void addInvokerBodies()
	{
		for (Map.Entry<SootClass, Set<String>> e : klassToModelMethodSubsigs.entrySet()) {
			SootClass klass = e.getKey();
			SootClass model = getModelClassFor(klass);
			SootClass invokerClass = getModelInvokerClassFor(klass);
			for (String modelSubsignature : e.getValue()) {
				if (!addInvokerBody(invokerClass, model, modelSubsignature)) {
					System.out.println("\nMissing model");
					System.out.println("Method: " + getJNIMethodName(modelSubsignature)+getJNISignature(modelSubsignature));
					System.out.println("Model method sig:  " + modelSubsignature);
					System.out.println("Model class: " + G.modelClassNameFor(klass.getName()) + "\n");
				}
			}
		}
	}

	/**
	 * A symbolic injector returns a symbolic expression which is seeded with the last concrete value.
	 * 
	 * @param klass
	 * @return
	 */
	public static SootMethod getSymbolInjectorFor(SootClass klass)
	{
		String modelMethodName = getJNITypeCode(klass.getName());
		SootClass invokerClass = getModelInvokerClassFor(klass);
		
		//If invoker for model method already exists, return it
		if (invokerClass.declaresMethodByName(modelMethodName)) {
			return invokerClass.getMethodByName(modelMethodName);
		}	
		
		//Else, create model method
		List paramTypes = Arrays.asList(new Type[]{RefType.v(G.OBJECT_CLASS_NAME), 
												   RefType.v(G.STRING_CLASS_NAME)});
		Type retType = G.EXPRESSION_TYPE;
		String modelSubsignature = G.EXPRESSION_CLASS_NAME + " " + 
			modelMethodName + "(" + G.OBJECT_CLASS_NAME + "," + G.STRING_CLASS_NAME + ")";

		//Create invoker for model method and return it
		return addInvokerMethod(klass, invokerClass, modelMethodName, paramTypes, retType, modelSubsignature);
	}

	private static String getModelMethodSubsignatureFor(SootMethod m) {
		String modelMethodName = modelMethodNameFor(m);
		int paramCount = m.getParameterCount();	
		boolean voidRetType = m.getReturnType() instanceof VoidType;
		StringBuilder builder = new StringBuilder();
		builder.append(voidRetType ? "void" : G.EXPRESSION_CLASS_NAME);
		builder.append(" ");
		builder.append(modelMethodName);
		builder.append("(");

		if (!m.isStatic()) {
			//to pass the symbolic value corresponding to the receiver
			builder.append(G.OBJECT_CLASS_NAME);       //to pass the actual value
			builder.append(",");
			builder.append(G.EXPRESSION_CLASS_NAME);   //to pass the symbolic value
			if (paramCount > 0)
				builder.append(",");
		}

		for (int i = 0; i < (paramCount-1); i++) {
			Type ptype = m.getParameterType(i);
			builder.append(ptype instanceof PrimType ? ptype : G.OBJECT_TYPE); //to pass the actual value
			builder.append(",");
			builder.append(G.EXPRESSION_CLASS_NAME);   //to pass the symbolic value
			builder.append(",");
		}
		if (paramCount > 0) {
			Type ptype = m.getParameterType(paramCount-1);
			builder.append(ptype instanceof PrimType ? ptype : G.OBJECT_TYPE);
			builder.append(",");
			builder.append(G.EXPRESSION_CLASS_NAME);
		}
		builder.append(")");
		return builder.toString();
	}

	private static SootMethod addInvokerMethod(SootClass klass, SootClass invokerClass,
			String modelMethodName, List paramTypes, Type retType, String modelSubsignature)
	{
		SootMethod invokerMethod = new SootMethod(modelMethodName,
												  paramTypes,
												  retType,
												  Modifier.PUBLIC | Modifier.STATIC,
												  Collections.EMPTY_LIST);
		invokerClass.addMethod(invokerMethod);

		Set<String> ss = klassToModelMethodSubsigs.get(klass);
		if (ss == null) {
			ss = new HashSet<String>();
			klassToModelMethodSubsigs.put(klass, ss);
		}
		ss.add(modelSubsignature);
		return invokerMethod;
	}


	/**
	 * Retrieves the body (or creates a stub body) of a modeled class.
	 * 
	 * @param modelInvokerClass Invoking class of the model class.
	 * @param modelClass The model class.
	 * @param modelSubsignature Method subsignature of the modeled method.
	 * @return False, if no model body exists (= a stub has been created), true if a model body exists.
	 */
	private static boolean addInvokerBody(SootClass modelInvokerClass, SootClass modelClass, 
			String modelSubsignature)
	{
		SootMethod modelMethod = null;
		if (modelClass != null && modelClass.declaresMethod(modelSubsignature)) {
			modelMethod = modelClass.getMethod(modelSubsignature);
		}

		String modelMethodName = modelSubsignature.substring(modelSubsignature.indexOf(' ')+1,
				modelSubsignature.indexOf('('));
		SootMethod invokerMethod = modelInvokerClass.getMethodByName(modelMethodName);
		G.addBody(invokerMethod);

		Type invokerMethodRetType = invokerMethod.getReturnType();
		System.out.println("Model class: " + modelClass);
		System.out.println("Model method: " + modelMethod);
		System.out.println("Model invoker class " + modelInvokerClass);
		System.out.println("Return type of " + invokerMethod.getDeclaringClass().getName() + "." + invokerMethod.getName() + " is " + invokerMethodRetType.toString());
		if (invokerMethodRetType.equals(G.EXPRESSION_TYPE)) {
			//it is the special method that injects symbolic value in methods annotated with @Symbolic
			if (!invokerMethod.getSubSignature().equals(modelSubsignature))
				assert false : invokerMethod.getSubSignature() + " " + modelSubsignature;
			Local concreteValue = G.paramLocal(invokerMethod, 0);
			Local symbol = G.paramLocal(invokerMethod, 1);
			if (modelMethod != null) {
				Local ret = G.newLocal(G.EXPRESSION_TYPE);
				G.assign(ret, G.staticInvokeExpr(modelMethod.makeRef(), concreteValue, symbol));
				G.ret(ret);
			}
			else {
				G.ret(NullConstant.v());
			}

			G.debug(invokerMethod, Main.DEBUG);
			return modelMethod != null;
		}

		if (!invokerMethodRetType.equals(VoidType.v()))
			assert false : modelSubsignature;

		List<Local> paramLocals = G.paramLocals(invokerMethod);
		int paramCount = paramLocals.size();
		Local symArgsArray = G.newLocal(ArrayType.v(G.EXPRESSION_TYPE, 1));
		//subsig=-1  -> Modeled method
		//sig=-1     -> Modeled class
		G.assign(symArgsArray, G.staticInvokeExpr(G.argPop, 
												  IntConstant.v(-1), 
												  IntConstant.v(-1), 
												  IntConstant.v(paramCount)));

		List<Local> args = new ArrayList<Local>();
		int i = 0;
		for (; i < paramCount; i++) {
			args.add(paramLocals.get(i));
			Local arg = G.newLocal(G.EXPRESSION_TYPE);
			G.assign(arg, G.jimple.newArrayRef(symArgsArray,IntConstant.v(i)));
			args.add(arg);
		}
		
		//call the model method if user has provided one
		//and pushes the ret value if any
		boolean voidRetType = modelSubsignature.substring(0, modelSubsignature.indexOf(' ')).equals("void");
		if (modelMethod != null) {
			InvokeExpr ie = G.staticInvokeExpr(modelMethod.makeRef(), args);
			if (voidRetType) {
				G.invoke(ie);
			} else {
				Local ret = G.newLocal(G.EXPRESSION_TYPE);
				G.assign(ret, ie);
				G.invoke(G.staticInvokeExpr(G.retPush, IntConstant.v(-1), ret));
			}
		}
		else if (!voidRetType) {
			G.invoke(G.staticInvokeExpr(G.retPush, IntConstant.v(-1), NullConstant.v()));
		}
		G.retVoid();
		G.debug(invokerMethod, G.DEBUG);
		return modelMethod != null;
	}

	private static SootClass getModelInvokerClassFor(SootClass klass)
	{
		String invokerClassName = G.modelInvokerClassNameFor(klass.getName());
		SootClass invoker = null;
		if (Scene.v().containsClass(invokerClassName))
			invoker = Scene.v().getSootClass(invokerClassName);
		if (invoker == null) {
			invoker = new SootClass(invokerClassName, Modifier.PUBLIC);
			invoker.setSuperclass(Scene.v().getSootClass(G.OBJECT_CLASS_NAME));
			Scene.v().addClass(invoker);
			invoker.setApplicationClass();
		}
		System.out.println("Returning model invoker class " + invoker.getName());
		return invoker;
	}

	public static SootClass getModelClassFor(SootClass klass)
	{
		String modelClassName = G.modelClassNameFor(klass.getName());
		SootClass model = null;
		if (Scene.v().containsClass(modelClassName))
			model = Scene.v().getSootClass(modelClassName);
		if (model == null) {
			//check if it is in classpath
			//user may have already specified a (possibly partial) model 
			if (SourceLocator.v().getClassSource(modelClassName) != null) {
				model = Scene.v().loadClassAndSupport(modelClassName);
			}
		}
		return model;
	}

	public static String modelMethodNameFor(SootMethod m)
	{
		List paramTypes = m.getParameterTypes();
		int paramCount = paramTypes.size();
		String name = m.getName();
		StringBuilder  s = new StringBuilder(name.length() + (paramCount*16));
		if (name.equals("<init>"))
			name = "_4init_4";
		s.append(name);
		s.append("__");
		for (Iterator pit = paramTypes.iterator(); pit.hasNext();) {
			Type ptype = (Type) pit.next();
			s.append(getJNITypeCode(ptype.toString()));
		}
		s.append("__");
		s.append(getJNITypeCode(m.getReturnType().toString()));
		String modelMethodName = s.toString();
		return modelMethodName;
	}

	/* This code is from Java Pathfinder.
       because every parameter of model method is Expression type
       the name is mangled to encode the parameter types
	 */
	public static String getJNITypeCode(String type) 
	{
		StringBuilder sb = new StringBuilder(32);
		int  l = type.length() - 1;

		for (; type.charAt(l) == ']'; l -= 2) {
			sb.append("_3");
		}

		type = type.substring(0, l + 1);

		if (type.equals("int")) {
			sb.append('I');
		} else if (type.equals("long")) {
			sb.append('J');
		} else if (type.equals("boolean")) {
			sb.append('Z');
		} else if (type.equals("char")) {
			sb.append('C');
		} else if (type.equals("byte")) {
			sb.append('B');
		} else if (type.equals("short")) {
			sb.append('S');
		} else if (type.equals("double")) {
			sb.append('D');
		} else if (type.equals("float")) {
			sb.append('F');
		} else if (type.equals("void")) {  // for return types
			sb.append('V');
		} else {
			sb.append('L');	    
			for (int i = 0; i < type.length(); i++) {
				char c = type.charAt(i);
				switch (c) {
				case '.':
					sb.append('_');
					break;
				case '_':
					sb.append("_1");
					break;
				default:
					sb.append(c);
				}
			}   
			sb.append("_2");
		}	
		return sb.toString();
	}
	
	public static void readModeledFields(String fileName) {
		if(fileName == null)
			return;

		File file = new File(fileName);
		if (!file.exists()) {
			System.out.println("model methods file not found. " + fileName);
			return;
		}
		try{
			BufferedReader reader = new BufferedReader(new FileReader(file));
			String line = reader.readLine();
			while (line != null) {
				if (!line.startsWith("#")) {	//Skip comments
					String className = line.trim();									
					if (Scene.v().containsField(className)) {
						fieldsWithModels.add(Scene.v().getField(className));
					}
				}
			line = reader.readLine();
			}
			reader.close();
		}
		catch(IOException e){
			throw new Error(e);
		}

	}

		
	/**
	 * Reads model definitions from a file.
	 * 
	 * Models replace an original method implementation during symbolic execution. If neither a 
	 * method's body nor a respective model is available, the symbolic return value of the method 
	 * will always be null at it will be disregarded during the symbolic execution. That is, you 
	 * should ensure that all APIs and native methods which are relevant for the symbolic execution 
	 * are available as a model. The syntax of the model file is as follows:
	 * 
	 * <class name> <method signature>
	 * 
	 * @param fileName
	 */
	public static void readModeledMethods(String fileName)
	{
		if(fileName == null)
			return;

		File file = new File(fileName);
		if (!file.exists()) {
			System.out.println("model methods file not found. " + fileName);
			return;
		}
		try{
			BufferedReader reader = new BufferedReader(new FileReader(file));
			String line = reader.readLine();
			while (line != null) {
				int index = line.indexOf(' ');
				if (!line.startsWith("#") && index >=0) {	//Skip comments
					String className = line.substring(0, index);				
					if (Scene.v().containsClass(className)) {
						String subsig = line.substring(index+1).trim();
						SootClass declClass = Scene.v().getSootClass(className);
						if (declClass.declaresMethod(subsig)) {
							SootMethod method = declClass.getMethod(subsig);
							methodsWithModels.add(method);
							System.out.println("model method requested for: " + declClass.getName() + "." + method.getSignature() + " which is available in scene");
						} else if (declClass.declaresField(subsig)) {
							fieldsWithModels.add(declClass.getField(subsig));
						}
						else {
							System.out.println("will not model method: " + line);
						}
					}
				}
			line = reader.readLine();
			}
			reader.close();
		}
		catch(IOException e){
			throw new Error(e);
		}
	}

	/**  THIS CODE IS FROM JAVA PATHFINDER
	 *
	 * get the argument type part of the signature out of a
	 * JNI mangled method name.
	 * Note this is not the complete signature, since we don't have a
	 * return type (which is superfluous since it's not overloading,
	 * but unfortunately part of the signature in the class file)
	 */
	public static String getJNISignature (String mangledName) {
		int    i = mangledName.indexOf("__");
		String sig = null;

		if (i > 0) {
			int k = 0;      
			int r = mangledName.indexOf("__", i+2); // maybe there is a return type part
			boolean gotReturnType = false;
			int len = mangledName.length();
			char[] buf = new char[len + 2];

			buf[k++] = '(';

			for (i += 2; i < len; i++) {

				if (i == r) { // here comes the return type part (that's not JNI, only MJI
					if ((i + 2) < len) {
						i++;
						buf[k++] = ')';
						gotReturnType = true;
						continue;
					} else {
						break;
					}
				}

				char c = mangledName.charAt(i);
				if (c == '_') {
					i++;

					if (i < len) {
						c = mangledName.charAt(i);

						switch (c) {
						case '1':
							buf[k++] = '_';

							break;

						case '2':
							buf[k++] = ';';

							break;

						case '3':
							buf[k++] = '[';

							break;

						default:
							buf[k++] = '/';
							buf[k++] = c;
						}
					} else {
						buf[k++] = '/';
					}
				} else {
					buf[k++] = c;
				}
			}

			if (!gotReturnType) {
				// if there was no return type spec, assume 'void'
				buf[k++] = ')';
				buf[k++] = 'V';
			}

			sig = new String(buf, 0, k);
		}

		// Hmm, maybe we should return "()V" instead of null, but that seems a bit too assuming
		return sig;
	}

	/** THIS CODE IS FROM JAVA PATHFINDER
	 *
	 * return the name part of a JNI mangled method name (which is of
	 * course not completely safe - you should only use it if you know
	 * this is a JNI name)
	 */
	public static String getJNIMethodName (String mangledName) {
		// note that's the first '__' group, which marks the beginning of the arg types
		int i = mangledName.indexOf("__");

		if (i > 0) {
			return mangledName.substring(0, i);
		} else {
			return mangledName;
		}
	}

}
