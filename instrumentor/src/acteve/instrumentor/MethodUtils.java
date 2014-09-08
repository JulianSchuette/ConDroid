/* 
 * Copyright (c) 2014, Julian Schuette, Fraunhofer AISEC
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

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import soot.Body;
import soot.Kind;
import soot.MethodOrMethodContext;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.jimple.Jimple;
import soot.jimple.Stmt;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;
import soot.jimple.toolkits.callgraph.ReachableMethods;
import soot.tagkit.AnnotationTag;
import soot.tagkit.GenericAttribute;
import soot.tagkit.InnerClassTag;
import soot.tagkit.Tag;
import soot.tagkit.VisibilityAnnotationTag;
import soot.util.HashChain;
import soot.util.dot.DotGraph;
import soot.util.dot.DotGraphEdge;
import soot.util.dot.DotGraphNode;
import soot.util.queue.QueueReader;

/**
 * Helper for soot methods
 */
public class MethodUtils {

	private static final String ATOMIC_ANNOTATION_SIGNATURE = "Lorg/deuce/Atomic;";
	private static final String MAIN_SIGNATURE = "void main(java.lang.String[])";
	private static final String RUN_SIGNATURE = "void run()";
	
	/**
	 * These are our pseudo-entry points. They consist of both actual Android
	 * life cycle entry points and of UI methods, i.e. ones that can be
	 * triggered by user UI interaction. Note: We only consider subsignatures
	 * here.
	 */
	private static final String[] LIFECYCLE_AND_UI_SUBSIGS = {
		//life cycle:
		"void onCreate(android.os.Bundle)", //every UI element has its onCreate() called, thus we don't need to explicitly list anything downstream from here
		"void onRestart()",
		"void onStart()",
		"void onResume()",
		"void onPause()",
		"void onStop()",
		"void onDestroy()",
		"void onReceive(android.content.Context, android.content.Intent)",
		"boolean onCreate()", //for example used by ContentProviders
		//UI events:
		"void onClick(android.view.View)",
		"void onTabSelected(android.app.ActionBar.Tab, android.app.FragmentTransaction)",
		"void onTabReselected(android.app.ActionBar.Tab, android.app.FragmentTransaction)",
		"void onTabUnselected(android.app.ActionBar.Tab, android.app.FragmentTransaction)",
		"void onLongClick(android.view.View)",
		"void onFocusChange(android.view.View, boolean)",
		"void onKey(android.view.View, int, android.view.KeyEvent)",
		"void onTouch(android.view.View, android.view.MotionEvent)",
		"void onCreateContextMenu(android.view.ContextMenu, android.view.View, android.view.ContextMenu.ContextMenuInfo)",
		"void onKeyDown(int, android.view.KeyEvent)",
		"void onKeyUp(int, android.view.KeyEvent)",
		"boolean onOptionsItemSelected(android.view.MenuItem)", //TODO: there seems to be a lot more...
		"boolean onCreateOptionsMenu(android.view.Menu)", //TODO: there seems to be a lot more...
		"void onTrackballEvent(android.view.MotionEvent)",
		"void onTouchEvent(android.view.MotionEvent)",
		"void onFocusChanged(boolean, int, android.graphics.Rect)",
		"void onInterceptTouchEvent(android.view.MotionEvent)",
	};
	
	private static final String[] REFLECTIVE_LOADING_SIGS = {
		"<java.lang.Class: T newInstance()>", //how to deal with generics in signatures? TODO
		"<java.lang.Class: newInstance()>", //afaik dynamic types are "type-erased" at runtime, so removing the type ought to yield the correct signature
		"<java.lang.Class: java.lang.Object newInstance()>",
		"<java.lang.Class: T newInstance(java.lang.Object...)>",
		"<java.lang.Class: newInstance(java.lang.Object...)>",
		"<java.lang.ClassLoader: java.lang.Class<T> loadClass(java.lang.String)>",
		"<java.lang.ClassLoader: java.lang.Class loadClass(java.lang.String)>",
		"<java.lang.ClassLoader: java.lang.Class<T> loadClass(java.lang.String,boolean)>",
		"<java.lang.ClassLoader: java.lang.Class loadClass(java.lang.String,boolean)>",
		"<java.lang.ClassLoader: void <init>()>", //constructors are a bit of a problem because we cannot know the signatures of all Classloaders that might be created by developers, e.g. by inheriting from ClassLoader
		"<java.lang.ClassLoader: void <init>(java.lang.ClassLoader)>",
		"<java.lang.ClassLoader: java.lang.ClassLoader getSystemClassLoader()>",
		"<java.lang.Class: java.lang.reflect.Constructor<T> getConstructor(java.lang.Class<?>...)>",
		"<java.lang.Class: java.lang.reflect.Constructor getConstructor(java.lang.Class...)>",
		"<java.lang.Class: java.lang.reflect.Constructor<?>[] getConstructors()>",
		"<java.lang.Class: java.lang.reflect.Constructor[] getConstructors()>",
		"<java.lang.Class: java.lang.reflect.Constructor<?>[] getDeclaredConstructors()>",
		"<java.lang.Class: java.lang.reflect.Constructor[] getDeclaredConstructors()>",
		"<java.net.URLClassLoader: void <init>(java.net.URL[])>",
		"<java.net.URLClassLoader: void <init>(java.net.URL[],java.lang.ClassLoader)>",
		"<java.net.URLClassLoader: void <init>(java.net.URL[],java.lang.ClassLoader,java.net.URLStreamHandlerFactory)>",
		"<java.security.SecureClassLoader: void <init>()>",
		"<java.security.SecureClassLoader: void <init>(java.lang.ClassLoader)>",
		"<java.lang.Class: java.lang.Class<T> forName(java.lang.String)>",
		"<java.lang.Class: java.lang.Class forName(java.lang.String)>",
		"<dalvik.system.BaseDexClassLoader: void <init>(Java.lang.String,java.io.File,java.lang.String,java.lang.ClassLoader)>",
		"<dalvik.system.DexClassLoader: void <init>(java.lang.String,java.lang.String,java.lang.String,java.lang.ClassLoader)>",
		"<dalvik.system.PathClassLoader: void <init>(Java.lang.String,java.lang.ClassLoader)>",
		"<dalvik.system.PathClassLoader: void <init>(Java.lang.String,Java.lang.String,java.lang.ClassLoader)>"
	};
	
	private static HashMap<String,String[]> REFLECTIVE_LOADING_MAP = new HashMap<String,String[]>();
	static {
		HashMap<String,String[]> aHashSet = new HashMap<String,String[]>();
		aHashSet.put("java.lang.Class", new String[]
				{"T newInstance()",
				 "newInstance()",
				 "java.lang.Object newInstance()",
				 "T newInstance(java.lang.Object...)",
				 "newInstance(java.lang.Object...)",
				 "java.lang.reflect.Constructor<T> getConstructor(java.lang.Class<?>...)",
				 "java.lang.reflect.Constructor getConstructor(java.lang.Class...)",
				 "java.lang.reflect.Constructor<?>[] getConstructors()",
				 "java.lang.reflect.Constructor[] getConstructors()",
				 "java.lang.reflect.Constructor<?>[] getDeclaredConstructors()",
				 "java.lang.reflect.Constructor[] getDeclaredConstructors()",
				 "java.lang.Class<T> forName(java.lang.String)",
				 "java.lang.Class forName(java.lang.String)"
				});
		aHashSet.put("java.lang.ClassLoader", new String[]
				{"java.lang.Class<T> loadClass(java.lang.String)",
				 "java.lang.Class loadClass(java.lang.String)",
				 "java.lang.Class<T> loadClass(java.lang.String,boolean)",
				 "java.lang.Class loadClass(java.lang.String,boolean)",
				 "void <init>()",
				 "void <init>(java.lang.ClassLoader)",
				 "java.lang.ClassLoader getSystemClassLoader()"
				});
		aHashSet.put("java.net.URLClassLoader", new String[]
				{"void <init>(java.net.URL[])",
				 "void <init>(java.net.URL[],java.lang.ClassLoader)",
				 "void <init>(java.net.URL[],java.lang.ClassLoader,java.net.URLStreamHandlerFactory)"
				});
		aHashSet.put("java.security.SecureClassLoader", new String[]
				{"void <init>()",
				 "void <init>(java.lang.ClassLoader)"
				});
		aHashSet.put("dalvik.system.BaseDexClassLoader", new String[]
				{"void <init>(Java.lang.String,java.io.File,java.lang.String,java.lang.ClassLoader)"
				});
		aHashSet.put("dalvik.system.DexClassLoader", new String[]
				{"void <init>(java.lang.String,java.lang.String,java.lang.String,java.lang.ClassLoader)"
				});
		aHashSet.put("dalvik.system.PathClassLoader", new String[]
				{"void <init>(Java.lang.String,java.lang.ClassLoader)",
				 "void <init>(Java.lang.String,Java.lang.String,java.lang.ClassLoader)"
				});
		REFLECTIVE_LOADING_MAP = aHashSet;
	};
	
	/**
	 * Tests whether a method's signature is among the well-known method
	 * signatures for reflective class loading defined in the JRE classes.
	 * However, it does NOT recognize methods gained by inheritance from these
	 * JRE classes. Use {@link isReflectiveLoading} instead.
	 * @param method
	 * @return
	 */
	@Deprecated
	public static boolean isReflectiveLoadingOld(SootMethod method){
		for (String s : REFLECTIVE_LOADING_SIGS)
			if (method.getSignature().equals(s))
				return true;
		return false;
	}
	
	/**
	 * Supersedes {@link isReflectiveLoadingOld}. Tests whether a method
	 * has a signature as defined in {@link REFLECTIVE_LOADING_MAP},
	 * additionally taking inheritance into account. For example, if a 
	 * declaring class is a subclass of any of the JRE classes and overrides
	 * or inherits a reflective loading method, it will be found.
	 * @param method
	 * @return
	 */
	public static boolean isReflectiveLoading(SootMethod method){
		SootClass declaringClass = method.getDeclaringClass();
		for (String key : REFLECTIVE_LOADING_MAP.keySet())
			if (isOrExtendsClass(declaringClass.getName(), key))
				for (String meth : REFLECTIVE_LOADING_MAP.get(key))
					if(method.getSubSignature().equals(meth))
						return true;
		return false;
	}
	
	public static boolean isOrExtendsClass(String className, String superClassName){
		SootClass klass = Scene.v().getSootClass(className);
		SootClass supperKlass = Scene.v().getSootClass(superClassName);
		
		if (klass.getName().equals(supperKlass.getName()))
			return true;
		while (klass.hasSuperclass()){
			if (klass.getSuperclass().getName().equals(superClassName))
				return true;
			klass = klass.getSuperclass();
		}
		return false;
	}
	
	public static boolean isPseudoEntryPoint(SootMethod method){
		for (String s : LIFECYCLE_AND_UI_SUBSIGS)
			if (method.getSubSignature().equals(s))
				return true;
		return false;
	}
	
	public static List<SootMethod> findReflectiveLoadingMethods(Collection<SootMethod> startingPoints){
		List<SootMethod> list = new ArrayList<SootMethod>(); 
		for (SootMethod sm : findTransitiveCalleesOf(startingPoints)){
			if (isReflectiveLoading(sm)) {
				list.add(sm);
			}
		}
		return list;
	}
	
	public static List<SootMethod> getAllReachableMethods(){
		List<SootMethod> result = new ArrayList<SootMethod>();
		ReachableMethods reachableMethods = Scene.v().getReachableMethods();
		QueueReader<MethodOrMethodContext> listener = reachableMethods.listener();
		while (listener.hasNext()) {
			MethodOrMethodContext next = listener.next();
			SootMethod method = next.method();
			result.add(method);
		}
		return result;
	}
	
	public static SootMethod findMethodWithSignature(String signature){
		SootMethod result = null;
		ReachableMethods reachableMethods = Scene.v().getReachableMethods();
		QueueReader<MethodOrMethodContext> listener = reachableMethods.listener();
		while (listener.hasNext()) {
			MethodOrMethodContext next = listener.next();
			SootMethod method = next.method();
			if (method.getSignature().equals(signature)) {
				result = method;
			}
		}

		return result;
	}

	public static boolean methodIsCtor(SootMethod method) {
		return methodIsInstanceCtor(method) || methodIsStaticCtor(method);
	}

	public static boolean methodIsStaticCtor(SootMethod method) {
		return method.getName().equals(SootMethod.staticInitializerName);
	}

	public static boolean methodIsInstanceCtor(SootMethod method) {
		return method.getName().equals(SootMethod.constructorName);
	}

	public static boolean methodIsEntryPoint(SootMethod method) {
		return methodIsMain(method) || methodIsRun(method);
	}

	public static boolean methodIsRun(SootMethod method) {
		String sig = method.getSubSignature();
		return !method.isStatic() && sig.equals(RUN_SIGNATURE);
	}

	public static boolean methodIsMain(SootMethod method) {
		String sig = method.getSubSignature();
		return method.isStatic() && sig.equals(MAIN_SIGNATURE);
	}

	public static boolean methodIsAtomic(SootMethod method) {

		Tag tag = method.getTag("VisibilityAnnotationTag");
		if (tag == null)
			return false;
		VisibilityAnnotationTag visibilityAnnotationTag = (VisibilityAnnotationTag) tag;
		ArrayList<AnnotationTag> annotations = visibilityAnnotationTag.getAnnotations();
		for (AnnotationTag annotationTag : annotations) {
			if (annotationTag.getType().equals(ATOMIC_ANNOTATION_SIGNATURE))
				return true;
		}
		return false;
	}

	public static List<SootMethod> findNonAtomicEntryPoints() {
		List<SootMethod> col = new ArrayList<SootMethod>();
		ReachableMethods reachableMethods = Scene.v().getReachableMethods();
		QueueReader<MethodOrMethodContext> listener = reachableMethods.listener();
		while (listener.hasNext()) {
			MethodOrMethodContext next = listener.next();
			SootMethod method = next.method();
			if (methodIsEntryPoint(method) && !methodIsAtomic(method)) {
				col.add(method);
			}
		}

		return col;
	}

	public static List<SootMethod> findApplicationEntryPoints() {
		List<SootMethod> col = new ArrayList<SootMethod>();
		ReachableMethods reachableMethods = Scene.v().getReachableMethods();
		QueueReader<MethodOrMethodContext> listener = reachableMethods.listener();
		while (listener.hasNext()) {
			MethodOrMethodContext next = listener.next();
			SootMethod method = next.method();
			if (methodIsEntryPoint(method)) {
				if (method.getDeclaringClass().isApplicationClass()) {
					col.add(method);
				}
			}
		}

		return col;
	}
	
	/**
	 * Returns life cycle determined entry points, i.e. callback methods, and
	 * UI event handlers, i.e. any "user-induced" entry point.
	 * @return
	 */
	public static List<SootMethod> findApplicationPseudoEntryPoints() {
		List<SootMethod> col = new ArrayList<SootMethod>();
		ReachableMethods reachableMethods = Scene.v().getReachableMethods();
		QueueReader<MethodOrMethodContext> listener = reachableMethods.listener();
		while (listener.hasNext()) {
			MethodOrMethodContext next = listener.next();
			SootMethod method = next.method();
			if (isPseudoEntryPoint(method)) {
				//if (method.getDeclaringClass().isApplicationClass()) {
					col.add(method);
				//}
			}
		}

		return col;
	}

	public static List<SootMethod> findApplicationAtomicMethods() {
		List<SootMethod> col = new ArrayList<SootMethod>();
		ReachableMethods reachableMethods = Scene.v().getReachableMethods();
		QueueReader<MethodOrMethodContext> listener = reachableMethods.listener();
		while (listener.hasNext()) {
			MethodOrMethodContext next = listener.next();
			SootMethod method = next.method();
			if (method.getDeclaringClass().isApplicationClass()) {
				if (methodIsAtomic(method)) {
					col.add(method);
				}
			}
		}

		return col;
	}

	public static List<SootMethod> getCallersOf(SootMethod method) {
		CallGraph callGraph = Scene.v().getCallGraph();
		Iterator<Edge> edgesInto = callGraph.edgesInto(method);
		List<SootMethod> list = new ArrayList<SootMethod>();
		while (edgesInto.hasNext()) {
			Edge edge = edgesInto.next();
			if (edge.isExplicit()) {
				SootMethod targetMethod = edge.getSrc().method();
				list.add(targetMethod);
			}
		}
		return list;
	}

	public static List<SootMethod> getCalleesOf(SootMethod method) {
		CallGraph callGraph = Scene.v().getCallGraph();
		Iterator<Edge> edgesInto = callGraph.edgesOutOf(method);

		List<SootMethod> list = new ArrayList<SootMethod>();
		while (edgesInto.hasNext()) {
			Edge edge = edgesInto.next();
			if (edge.isExplicit()) {
				SootMethod targetMethod = edge.getTgt().method();
				list.add(targetMethod);
			}
		}
		return list;
	}

	public static List<SootMethod> getCalleesOf(SootMethod callerMethod, Stmt invocation) {
		CallGraph callGraph = Scene.v().getCallGraph();

		Iterator<Edge> edgesOut = callGraph.edgesOutOf(invocation);
		List<SootMethod> list = new ArrayList<SootMethod>();
		while (edgesOut.hasNext()) {
			Edge edge = edgesOut.next();
			if (edge.isExplicit()) {
				SootMethod targetMethod = edge.getTgt().method();
				list.add(targetMethod);
			}
		}
		return list;
	}

	public static Set<SootMethod> findTransitiveCalleesOf(SootMethod sootMethod) {
		CallGraph callGraph = Scene.v().getCallGraph();
		Set<SootMethod> transitiveTargets = new LinkedHashSet<SootMethod>();
		HashChain<SootMethod> unprocessedTargets = new HashChain<SootMethod>();
		unprocessedTargets.add(sootMethod);
		while (!unprocessedTargets.isEmpty()) {
			sootMethod = unprocessedTargets.getFirst();
			unprocessedTargets.removeFirst();
			Iterator<Edge> edgesOutOf = callGraph.edgesOutOf(sootMethod);
			while (edgesOutOf.hasNext()) {
				Edge edge = edgesOutOf.next();
				if (edge.isExplicit()) {
					SootMethod target = edge.getTgt().method();
					if (!transitiveTargets.contains(target)) {
						transitiveTargets.add(target);
						unprocessedTargets.add(target);
					}
				}
			}
		}
		return transitiveTargets;
	}
	
	/**
	 * Finds the set of Android entry point methods (lifecycle callback methods)
	 * from which the method is transitively reachable.
	 * 
	 * @param sootMethod
	 * @return
	 */
	public static Set<SootMethod> findEntryPointsOf(SootMethod sootMethod){
		CallGraph callGraph = Scene.v().getCallGraph();
		Set<SootMethod> transitiveSources = new LinkedHashSet<SootMethod>();
		Set<SootMethod> entrypoints = new LinkedHashSet<SootMethod>();
		HashChain<SootMethod> unprocessedSources = new HashChain<SootMethod>();
		unprocessedSources.add(sootMethod);
		while (!unprocessedSources.isEmpty()) {
			sootMethod = unprocessedSources.getFirst();
			unprocessedSources.removeFirst();
			Iterator<Edge> edgesInto = callGraph.edgesInto(sootMethod);
			while (edgesInto.hasNext()) {
				Edge edge = edgesInto.next();
				if (edge.isExplicit()) {
					SootMethod source = edge.getSrc().method();
					if (!transitiveSources.contains(source)) {
						transitiveSources.add(source);
						unprocessedSources.add(source);
						// check if this method is invoked from dummy main:
						Iterator<Edge> sourceEdges = callGraph.edgesInto(source);
						//if any incoming edge is from dummy main...
						while (sourceEdges.hasNext()){
							Edge edge2 = sourceEdges.next();
							if (edge2.src().method().getSignature().equals(Scene.v().getMethod("<dummyMainClass: void dummyMainMethod()>").getSignature()))
								// ... add the method
								entrypoints.add(source);
						}
					}
				}
			}
		}
		return entrypoints;
	}
	

	/**
	 * Returns the set of transitive methods from which the given method is
	 * reachable.
	 * 
	 * That is, this methods returns all methods, intermediate and entrypoints,
	 * from which a call chain to the given method exists. The returned set is
	 * not ordered, i.e. its order does not imply a call chain.
	 * 
	 * @param sootMethod
	 * @return
	 */
	public static CallGraph findTransitiveCallersOf(SootMethod sootMethod) {
		Pattern actevePat = Pattern.compile("dummyMainClass|(acteve\\..*)");

		System.out.println("Finding transitive callers of " + sootMethod.getSignature());
		String dotFile = "goals_"+sootMethod.getDeclaringClass().getName() + sootMethod.getName();
		CallGraph callGraph = Scene.v().getCallGraph();
		CallGraph subGraph = new CallGraph();
		Set<SootMethod> transitiveSources = new LinkedHashSet<SootMethod>();	//TODO not needed anymore. Remove
		HashChain<SootMethod> unprocessedSources = new HashChain<SootMethod>();
		unprocessedSources.add(sootMethod);
		while (!unprocessedSources.isEmpty()) {
			sootMethod = unprocessedSources.getFirst();
			unprocessedSources.removeFirst();
			Iterator<Edge> edgesInto = callGraph.edgesInto(sootMethod);
			while (edgesInto.hasNext()) {
				Edge edge = edgesInto.next();
				if (!actevePat.matcher(edge.getSrc().method().getDeclaringClass().getName()).matches()) {
					if (!edge.getSrc().method().equals(Scene.v().getMethod("<dummyMainClass: void dummyMainMethod()>")))
						subGraph.addEdge(edge);
					SootMethod source = edge.getSrc().method();
					
					if (MethodUtils.getCalleesOf(Scene.v().getMethod("<dummyMainClass: void dummyMainMethod()>")).contains(source)
							&& !source.getName().equals("<init>")) {
						source.addTag(new GenericAttribute("entrymethod", new byte[0]));
						System.out.println("This is an entrypoint: " + source);
					} else if (Scene.v().getActiveHierarchy().isClassSubclassOf(source.getDeclaringClass(), Scene.v().getSootClass("android.view.View"))
							&& (source.getSubSignature().equals("void <init>(android.content.Context)")
									|| source.getSubSignature().equals("void <init>(android.content.Context,android.util.AttributeSet)")
									|| source.getSubSignature().equals("void <init>(android.content.Context,android.util.AttributeSet,int)"))) { 
						source.addTag(new GenericAttribute("entrymethod", new byte[0]));					
						System.out.println("This is a view constructor and a potential entrypoint: " + source);
					}else {
						System.out.println("Not an entrypoint: " + source.getSignature());
					}
					if (!transitiveSources.contains(source)) {
						transitiveSources.add(source);
						unprocessedSources.add(source);
					}
				}
			}
		}
		
		if (Main.DEBUG)
			printCGtoDOT(subGraph, dotFile);
		return subGraph;
	}

	public static Set<SootMethod> findTransitiveCalleesOf(Collection<SootMethod> sootMethods) {
		Set<SootMethod> transitiveTargets = new LinkedHashSet<SootMethod>();
		for (SootMethod sootMethod : sootMethods) {
			Set<SootMethod> transitiveCallees = findTransitiveCalleesOf(sootMethod);
			transitiveTargets.addAll(transitiveCallees);
		}
		return transitiveTargets;
	}

	public static void printCGtoDOT(CallGraph cg, String prefix) {
		DotGraph dg = new DotGraph("Call Graph");
		QueueReader<Edge> edges = cg.listener();
		while (edges.hasNext()) {
			Edge e = edges.next();
			SootMethod src = e.getSrc().method();
			SootMethod tgt = e.getTgt().method();
			DotGraphNode srcNode = dg.getNode(src.getSignature());
			if (src.hasTag("entrymethod")) {
				srcNode.setAttribute("color", "deeppink");
				srcNode.setAttribute("shape", "box");
			}
			DotGraphEdge dgEdge = dg.drawEdge(src.getSignature(), tgt.getSignature());
			if (e.kind() == Kind.CLINIT) {
				dgEdge.setAttribute("style", "dashed");
			} else {
				dgEdge.setAttribute("style", "bold");
			}
		}
		dg.plot(prefix + "_cg.dot");
	}
	
    /**
     * Creates a new class with empty static initializer block, adds it to Scene and returns it.s
     * @param name Name of the class.
     * @return
     */
    public static SootClass createClass(String name) {
		System.out.println("Creating class " + name);
    	String outerName = null;
    	SootClass outerClass = null;
		if (name.contains("$")) {
    		outerName = name.substring(0,name.lastIndexOf("$"));
    		outerClass = createClass(outerName);
    	}
			
    	SootClass c = Scene.v().loadClassAndSupport(name);
    	c.setSuperclass(Scene.v().getSootClass("java.lang.Object"));
    	c.setModifiers(Modifier.PUBLIC);
    	
    	//TODO Generate method bodies. see com.example.de.fhg.aisec.concolicexample.Test$VERSION.jimple
//		SootMethod clinitMethod = null;
//		  if (!c.declaresMethod("<clinit>", new ArrayList(), soot.VoidType.v())) {
//			//Add static initializer
//		    clinitMethod = new soot.SootMethod("<clinit>", new ArrayList(), soot.VoidType.v(), soot.Modifier.STATIC, new ArrayList<SootClass>());                
//		    clinitMethod.setActiveBody(Jimple.v().newBody(clinitMethod));
//		    c.addMethod(clinitMethod);
//		} else {
//		    clinitMethod = c.getMethod("<clinit>", new ArrayList(), soot.VoidType.v());
//		}		  
//		
//		SootMethod initMethod = null;
//		  if (!c.declaresMethod("<init>", new ArrayList(), soot.VoidType.v())) {
//			//Add static initializer
//			  initMethod = new soot.SootMethod("<init>", new ArrayList(), soot.VoidType.v(), soot.Modifier.PUBLIC, new ArrayList<SootClass>());                
//			  initMethod.setActiveBody(Jimple.v().newBody(initMethod));
//		    c.addMethod(initMethod);
//		} else {
//			initMethod = c.getMethod("<init>", new ArrayList(), soot.VoidType.v());
//		}		  
			
			
		
    	
    	if (outerName!=null && outerClass!=null) {
    		outerClass.addTag(new InnerClassTag(name,outerName, name.substring(name.lastIndexOf("$")+1), Modifier.PUBLIC));
    		c.addTag(new InnerClassTag(name,outerName, name.substring(name.lastIndexOf("$")+1), Modifier.PUBLIC));
    	}
    	c.setApplicationClass();	
		
		Scene.v().addBasicClass(c.getName(), SootClass.BODIES);
		Scene.v().forceResolve(c.getName(), SootClass.BODIES);
		

		return c;
    }
}
