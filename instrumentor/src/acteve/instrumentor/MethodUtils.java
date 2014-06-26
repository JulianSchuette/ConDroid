package acteve.instrumentor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import soot.MethodOrMethodContext;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.jimple.Stmt;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;
import soot.jimple.toolkits.callgraph.ReachableMethods;
import soot.tagkit.AnnotationTag;
import soot.tagkit.Tag;
import soot.tagkit.VisibilityAnnotationTag;
import soot.util.HashChain;
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
	
	public static boolean isReflectiveLoading(SootMethod method){
		for (String s : REFLECTIVE_LOADING_SIGS)
			if (method.getSignature().equals(s))
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
	public static Set<SootMethod> findTransitiveCallersOf(SootMethod sootMethod) {
		CallGraph callGraph = Scene.v().getCallGraph();
		Set<SootMethod> transitiveSources = new LinkedHashSet<SootMethod>();
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
					}
				}
			}
		}
		return transitiveSources;
	}

	public static Set<SootMethod> findTransitiveCalleesOf(Collection<SootMethod> sootMethods) {
		Set<SootMethod> transitiveTargets = new LinkedHashSet<SootMethod>();
		for (SootMethod sootMethod : sootMethods) {
			Set<SootMethod> transitiveCallees = findTransitiveCalleesOf(sootMethod);
			transitiveTargets.addAll(transitiveCallees);
		}
		return transitiveTargets;
	}

}
