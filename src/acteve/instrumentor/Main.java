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
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.zip.ZipException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import soot.JimpleClassSource;
import soot.MethodOrMethodContext;
import soot.Modifier;
import soot.PackManager;
import soot.Scene;
import soot.SceneTransformer;
import soot.SootClass;
import soot.SootMethod;
import soot.SourceLocator;
import soot.Transform;
import soot.jimple.infoflow.android.SetupApplication;
import soot.jimple.infoflow.android.data.AndroidMethod;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.options.Options;
import soot.util.Chain;
import acteve.explorer.Utils;

public class Main extends SceneTransformer {
	public static Logger log = LoggerFactory.getLogger(Main.class);
	private static Config config;
	private static Set<SootClass> classesToInstrument = new HashSet<SootClass>();
	private static Map<String, List<String>> uninstrumentedClasses = new HashMap<String, List<String>>();
	private static final String dummyMainClassName = "acteve.symbolic.DummyMain";
	static boolean DEBUG = true;
	public final static boolean DUMP_JIMPLE = false; //default: false. Set to true to create Jimple code instead of APK
	public final static boolean VALIDATE = false; //Set to true to apply some consistency checks. Set to false to get past validation exceptions and see the generated code. Note: these checks are more strict than the Dex verifier and may fail at some obfuscated, though valid classes
	private final static String androidJAR = "./libs/android-14.jar"; //required for CH resolution
	private final static String libJars = "./jars/a3t_symbolic.jar"; //libraries
	private final static String modelClasses = "./mymodels/src"; //Directory where user-defined model classes reside.
	private static String apk = null;
	private static boolean OMIT_MANIFEST_MODIFICATION = false;
	private static boolean LIMIT_TO_CALL_PATH = true; //Limit instrumentation to methods along the CP to reflection use?
	private static boolean SKIP_CONCOLIC_INSTRUMENTATION = false;
	private static boolean SKIP_ALL_INSTRUMENTATION = false;	//For debugging
	private static boolean SKIP_CG_EXTENTION=false;

	/**
	 * Classes to exclude from instrumentation (all acteve, dalvik classes, plus some android SDK classes which are used by the instrumentation itself).
	 */
	//Exclude these classes from instrumentation
	protected final static Pattern excludePat = Pattern
			.compile("dummyMainClass|(acteve\\..*)|(java\\..*)|(dalvik\\..*)|(android\\.os\\.(Parcel|Parcel\\$.*))|(android\\.util\\.Slog)|(android\\.util\\.(Log|Log\\$.*))");
	private static final boolean DUMP_CG_TO_DOT = false;
	protected static InstrumentationHelper ih;
	
	@Override
	protected void internalTransform(String phaseName, @SuppressWarnings("rawtypes") Map options) {
		if (DEBUG)
			printClasses("bef_instr.txt");

		ModelMethodsHandler.readModeledMethods(config.modelMethsFile);
		ModelMethodsHandler.readModeledFields(config.modelFieldsFile);
		Instrumentor ci = new Instrumentor(config.rwKind,
										   config.outDir,
										   config.sdkDir, 
										   config.fldsWhitelist,
										   config.fldsBlacklist, 
										   config.methsWhitelist, 
										   config.instrAllFields);
		ci.instrument(classesToInstrument);
//		InputMethodsHandler.instrument(config.inputMethsFile);
		ModelMethodsHandler.addInvokerBodies();
		Scene.v().getApplicationClasses().remove(Scene.v().getSootClass(dummyMainClassName));

		if (DEBUG)
			printClasses("aft_instr.txt");
	}

	
	private void printClasses(String fileName) {
		try {
			PrintWriter out = new PrintWriter(new FileWriter(fileName));
			for (SootClass klass : classesToInstrument) {
				Printer.v().printTo(klass, out);
			}
			out.close();
		} catch (IOException ex) {
			ex.printStackTrace();
			System.exit(1);
		}
	}

	/**
	 * Main entry point of the Instrumentor.
	 * 
	 * @param args
	 * @throws ZipException
	 * @throws XPathExpressionException
	 * @throws IOException
	 * @throws InterruptedException
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 */
	public static void main(String[] args) throws ZipException, XPathExpressionException, IOException, InterruptedException, ParserConfigurationException, SAXException {
		soot.G.reset();
		config = Config.g();
		
		//Clear output dir
		Utils.deleteDir(new File("sootOutput"));
		
		if (args.length<=0 || !new File(args[0]).exists()) {
			printUsage();
			System.exit(-1);
		}
		apk = args[0];

		Options.v().set_soot_classpath("./libs/android-19.jar"+":"+libJars+":"+modelClasses + ":" + apk);
		
		// inject correct dummy main:
		SetupApplication setupApplication = new SetupApplication(androidJAR, apk);
		try {
			/** ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! !
			 *  ! NOTE: calculateSourcesSinksEntrypoints() calls soot.G.reset()
			 *  , i.e. it clears all global settings! ! ! ! ! ! ! ! ! ! ! ! ! !
			 */ 
			setupApplication.calculateSourcesSinksEntrypoints(new HashSet<AndroidMethod>(), new HashSet<AndroidMethod>());
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		//restore the class path because of soot.G.reset() in calculateSourcesSinksEntrypoints:
		Options.v().set_soot_classpath("./libs/android-19.jar"+":"+libJars+":"+modelClasses + ":" + apk);
		Scene.v().setSootClassPath("./libs/android-19.jar"+":"+libJars+":"+modelClasses + ":" + apk);
		
		Options.v().set_no_bodies_for_excluded(true);
		Options.v().set_src_prec(Options.src_prec_apk);
		
		Options.v().set_whole_program(true);	//Implicitly "on" when instrumenting Android, AFAIR.
		Options.v().setPhaseOption("cg", "on");	//"On" by default.
		Options.v().setPhaseOption("cg", "verbose:true");
		Options.v().setPhaseOption("cg", "safe-newinstance:true");
		Options.v().setPhaseOption("cg", "safe-forname:true");
	    Options.v().set_keep_line_number(true);
		Options.v().set_keep_offset(true);

		// replace Soot's printer with our logger (will be overwritten by G.reset(), though)
		// G.v().out = new PrintStream(new LogStream(Logger.getLogger("SOOT"),
		// Level.DEBUG), true);

		Options.v().set_allow_phantom_refs(true);
		Options.v().set_prepend_classpath(true);
		Options.v().set_validate(VALIDATE);

		if (DUMP_JIMPLE) {
			Options.v().set_output_format(Options.output_format_jimple);
		} else {
			Options.v().set_output_format(Options.output_format_dex);
		}
		Options.v().set_process_dir(Collections.singletonList(apk));
		Options.v().set_force_android_jar(androidJAR);
		Options.v().set_android_jars(libJars);
		Options.v().set_src_prec(Options.src_prec_apk);
		Options.v().set_debug(true);
		
		//Create dummy main method referencing all entry points
		SootMethod dummyMain = setupApplication.getEntryPointCreator().createDummyMain();

		Scene.v().setEntryPoints(Collections.singletonList(dummyMain));
		Scene.v().addBasicClass(dummyMain.getDeclaringClass().getName(), SootClass.BODIES);
		// All packages which are not already in the app's transitive hull but
		// are required by the injected code need to be marked as dynamic.
		Options.v().set_dynamic_package(
				Arrays.asList(new String[] { "acteve.symbolic.", "com.android", "models.", "org.json", "org.apache", "org.w3c",
						"org.xml", "junit", "javax", "javax.crypto"}));


		Scene.v().loadNecessaryClasses();
		
		//Register all application classes for instrumentation
		Chain<SootClass> appclasses = Scene.v().getApplicationClasses();
		classesToInstrument.addAll(appclasses);
		
		//Collect additional classes which will be injected into the app
		List<String> libClassesToInject = SourceLocator.v().getClassesUnder("./jars/a3t_symbolic.jar");		
		for (String s:libClassesToInject) {
			Scene.v().addBasicClass(s, SootClass.BODIES);
			Scene.v().loadClassAndSupport(s);
			SootClass clazz = Scene.v().forceResolve(s, SootClass.BODIES);
			clazz.setApplicationClass();
		}

		//Get the lifecycle method to instrument
		ih = new InstrumentationHelper(new File(apk));
		SootMethod lcMethodToExtend = ih.getDefaultOnResume();
		if (lcMethodToExtend==null) {
			lcMethodToExtend = ih.getDefaultOnCreate();
		}
		
		assert lcMethodToExtend!=null:"No default activity found";
		
		if (!SKIP_CG_EXTENTION) {			
			PackManager.v().getPack("wjtp").add(new Transform("wjtp.android", new AndroidCGExtender()));
		}

		if (!SKIP_CONCOLIC_INSTRUMENTATION && !SKIP_ALL_INSTRUMENTATION) {
			PackManager.v().getPack("wjtp").add(new Transform("wjtp.acteve", new Main()));
		}

			
		// -------------------------------- BEGIN RAFAEL ----------------------------------------------
		if (LIMIT_TO_CALL_PATH ) {
			/* 
			 * 1.	Find all entry points (i.e. "real" entry points according to
			 *      Android life cycle model that get automatically called by OS)
			 * 1a.	Add all constructors from View lifecycles (are not provided by EntryPointAnalysis)
			 * 2)   Find all reachable /target/ methods 
			 * 3)   Determine all paths from methods in 1) to methods in 2)
			 * 4)   Instrument only on those paths 
			 */
			
			PackManager.v().getPack("cg").apply();

			//1)
			HashSet<SootMethod> entryPoints = new HashSet<SootMethod>(MethodUtils.getCalleesOf(dummyMain));	
			
			//1a)
			SootClass viewClass = Scene.v().getSootClass("android.view.View");
			List<SootClass> views = Scene.v().getActiveHierarchy().getSubclassesOf(viewClass);
			for (SootClass v:views) {
				if (!excludePat.matcher(v.getJavaPackageName()).matches()) {
					try { 	SootMethod immediateConstructor = v.getMethod("void <init>(android.content.Context)"); 
						  	entryPoints.add(immediateConstructor); } catch (RuntimeException rte) {}
					try {	SootMethod inflatingConstructor = v.getMethod("void <init>(android.content.Context,android.util.AttributeSet)");
							entryPoints.add(inflatingConstructor);} catch (RuntimeException rte) {}
					try {	SootMethod inflatingConstructorWStyle = v.getMethod("void <init>(android.content.Context,android.util.AttributeSet,int)");
							entryPoints.add(inflatingConstructorWStyle);} catch (RuntimeException rte) {}
				}
			}

			if (log.isDebugEnabled()) {
				for (SootMethod m : entryPoints){
					log.debug("Entrypoint: {}", m.getSignature());
				}
			}

			//2)
			List<SootMethod> goalMethods = MethodUtils.findReflectiveLoadingMethods(entryPoints);
			if (DEBUG) {
				log.debug("Found the following goal methods:");
				for (SootMethod m : goalMethods){
					log.debug("  Signature: {}", m.getSignature());
				}
			}
			
			//we have all SootMethods now which might be used to load classes at runtime. Now get the classes on the paths to them:
			HashMap<SootMethod, List<SootClass>> pathsToGoal = new HashMap<SootMethod, List<SootClass>>();
			for (SootMethod goalMeth : goalMethods){
				List<SootClass> path = new ArrayList<SootClass>();
				//add the declaring class because developers might inherit & extend from base class loaders
				if(!excludePat.matcher(goalMeth.getDeclaringClass().getName()).matches())
					path.add(goalMeth.getDeclaringClass());
				//and all the classes on the way to the call:
				CallGraph subGraph = MethodUtils.findTransitiveCallersOf(goalMeth);
				Iterator<MethodOrMethodContext> methodsAlongThePath = subGraph.sourceMethods();
				while (methodsAlongThePath.hasNext()) {
					SootMethod methodAlongThePath = methodsAlongThePath.next().method();
					if(!excludePat.matcher(methodAlongThePath.getDeclaringClass().getName()).matches()){
						path.add(methodAlongThePath.getDeclaringClass());
					}
				}
				pathsToGoal.put(goalMeth, path);
			}
			
			if (log.isDebugEnabled()) {
				for (SootMethod goal:pathsToGoal.keySet()) {
					log.debug("{} classes along the path to {}", pathsToGoal.get(goal).size(), goal.getSignature());
					List<SootClass> along = pathsToGoal.get(goal);
					for (SootClass c:along) {
						log.debug("  {}", c.getName());
					}
				}
			}
			
			classesToInstrument = new HashSet<SootClass>();
			for (List<SootClass> path:pathsToGoal.values()) {
				classesToInstrument.addAll(path);
			}			
		}
		// -------------------------------- END RAFAEL ----------------------------------------------

		
		if (!SKIP_ALL_INSTRUMENTATION) {
			try {
				ih.insertCallsToLifecycleMethods(lcMethodToExtend);
			} catch (Exception e) {
				log.error("Exception while inserting calls to lifecycle methods:", e);
			}
			
			//build new call graph now that we have paths to UI-induced method calls:
			PackManager.v().getPack("cg").apply();
		}
		
		//dump all methods for debugging:
		if (log.isDebugEnabled()) {
			List<SootMethod> allMethods = MethodUtils.getAllReachableMethods();
			log.debug("All methods in the scene:");
			for (SootMethod m : allMethods)
				log.debug("\t{}", m.getSignature());
		}

		PackManager.v().runPacks();
		PackManager.v().writeOutput();
		
		//Just nice to have: Print Callgraph to a .dot file
		if (DUMP_CG_TO_DOT) {
			log.debug("Printing call graph to .dot file");
			MethodUtils.printCGtoDOT(Scene.v().getCallGraph(), "main");
		}
		
		String outputApk = "sootOutput/"+new File(apk).getName();
		if (new File(outputApk).exists()) {
			File f = new File(outputApk);
			
			//Add permission to access sdcard
			if (!OMIT_MANIFEST_MODIFICATION) {
				HashMap<String, String> replacements = new HashMap<String, String>();
				replacements.put("</manifest>", "<uses-permission android:name=\"android.permission.WRITE_EXTERNAL_STORAGE\" /> </manifest>");
				f = InstrumentationHelper.adaptManifest(f.getAbsolutePath(), replacements);				
			}
			
			//Sign the APK
			signAPK(f.getAbsolutePath());
			
			log.info("Done. Have fun with {}", f.getAbsolutePath());
			
			//Handover to explorer
			String appPkgName = ih.getPackagename();
			String mainActivity = ih.getDefaultActivities().iterator().next();
			acteve.explorer.Main.main(new String[] {f.getAbsolutePath(), appPkgName, mainActivity});
		} else {
			log.error("ERROR: " + outputApk + " does not exist");
		}
	}


	private static void printUsage() {
		System.out.println("Usage: instrumenter <apk>");
		System.out.println("  apk:    APK file to prepare");
	}


	private static void signAPK(String apk) {
		try {
			// jarsigner is part of the Java SDK
			log.info("Signing {} ...", apk);
			String cmd = "jarsigner -verbose -digestalg SHA1 -sigalg MD5withRSA -storepass android -keystore "+System.getProperty("user.home")+"/.android/debug.keystore "
					+ apk + " androiddebugkey";
			log.debug("Calling {}", cmd);
			Process p = Runtime.getRuntime().exec(cmd);
			printProcessOutput(p);

			// zipalign is part of the Android SDK
			log.info("Zipalign ...", apk);
			cmd = "zipalign -v 4 " + apk + " " + new File(apk).getName() + "_signed.apk";
			log.debug(cmd);
			p = Runtime.getRuntime().exec(cmd);
			printProcessOutput(p);
		} catch (IOException e) {
			log.error(e.getMessage(), e);
		}
	}
	
	private static void printProcessOutput(Process p) throws IOException{
		String line;
		BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));
		while ((line = input.readLine()) != null) {
		  System.out.println(line);
		}
		input.close();
		
		input = new BufferedReader(new InputStreamReader(p.getErrorStream()));
		while ((line = input.readLine()) != null) {
		  System.out.println(line);
		}
		input.close();
	}

	static boolean isInstrumented(SootClass klass) {
		return classesToInstrument.contains(klass);
	}

	
	/**
	 * Load a soot class from Jimple. 
	 * 
	 * @param f
	 * @param className
	 * @return
	 */
	public static SootClass loadFromJimple(File f, String className) {
		try {
			// Create FIP for jimple file
			FileInputStream fip = new FileInputStream(f);

			// Load from Jimple file
			JimpleClassSource jcs = new JimpleClassSource(className, fip);
			SootClass sc = new SootClass(className, Modifier.PUBLIC);
			jcs.resolve(sc);			
			return sc;
		} catch (Throwable t) {
			t.printStackTrace();
		}
		return null;
	}
	
	/**
	 * Load class from jimple files in a directory.
	 * 
	 * @param dirname
	 * @return
	 */
	public static List<SootClass> loadFromJimples(String dirname) {
		File dir = new File(dirname);
		if (!dir.exists() || !dir.isDirectory())
			return null;
		List<SootClass> jimples = new ArrayList<SootClass>();
		for (File f : dir.listFiles())
			jimples.add(loadFromJimple(f, f.getName().replace(".jimple", "")));
		return jimples;
	}
}
