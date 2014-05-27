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
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.zip.ZipException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;

import org.xml.sax.SAXException;

import soot.JimpleClassSource;
import soot.Modifier;
import soot.PackManager;
import soot.Scene;
import soot.SceneTransformer;
import soot.SootClass;
import soot.SootMethod;
import soot.SourceLocator;
import soot.Transform;
import soot.javaToJimple.IInitialResolver.Dependencies;
import soot.options.Options;
import soot.util.Chain;

public class Main extends SceneTransformer {
	private static Config config;
	private static List<SootClass> classes = new ArrayList<SootClass>();
	private static Map<String, List<String>> uninstrumentedClasses = new HashMap<String, List<String>>();
	private static final String dummyMainClassName = "acteve.symbolic.DummyMain";
	static boolean DEBUG = true;
	public final static boolean DUMP_JIMPLE = false	; //default: false. Set to true to create Jimple code instead of APK
	public final static boolean VALIDATE = false; //Set to true to apply some consistency checks. Set to false to get past validation exceptions and see the generated code. Note: these checks are more strict than the Dex verifier and may fail at some obfuscated, though valid classes
	private static boolean LIMIT_TO_CALL_PATH = true; //Limit instrumentation to methods along the CP to reflection use?
	private final static String androidJAR = "./libs/android-14.jar"; //required for CH resolution
	private final static String libJars = "./jars/a3t_symbolic.jar"; //libraries
	private final static String modelClasses = "./mymodels/src"; //Directory where user-defined model classes reside.
	private final static String apk = "./de.fhg.aisec.concolicexample.apk";
	private static boolean OMIT_MANIFEST_MODIFICATION = false;
	
	/**
	 * Classes to exclude from instrumentation (all acteve, dalvik classes, plus some android SDK classes which are used by the instrumentation itself).
	 */
	//TODO Exclude these classes from instrumentation
	private static Pattern excludePat = Pattern
			.compile("(acteve\\..*)|(java\\..*)|(dalvik\\..*)|(android\\.os\\.(Parcel|Parcel\\$.*))|(android\\.util\\.Slog)|(android\\.util\\.(Log|Log\\$.*))");

	@Override
	protected void internalTransform(String phaseName, @SuppressWarnings("rawtypes") Map options) {
		if (DEBUG)
			printClasses("bef_instr.txt");

		ModelMethodsHandler.readModelMethods(config.modelMethsFile);
		Instrumentor ci = new Instrumentor(config.rwKind, config.outDir, config.sdkDir, config.fldsWhitelist,
				config.fldsBlacklist, config.methsWhitelist, config.instrAllFields);
		ci.instrument(classes);
//		InputMethodsHandler.instrument(config.inputMethsFile);
		ModelMethodsHandler.addInvokerBodies();
		Scene.v().getApplicationClasses().remove(Scene.v().getSootClass(dummyMainClassName));

		if (DEBUG)
			printClasses("aft_instr.txt");
	}

	
	private void printClasses(String fileName) {
		try {
			PrintWriter out = new PrintWriter(new FileWriter(fileName));
			for (SootClass klass : classes) {
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
		config = Config.g();

		Options.v().set_soot_classpath("./libs/android-19.jar"+":"+libJars+":"+modelClasses);

		Options.v().set_whole_program(true);	//Implicitly "on" when instrumenting Android, AFAIR.
		Options.v().setPhaseOption("cg", "on");	//"On" by default.
		Options.v().setPhaseOption("cg", "verbose");
	    Options.v().set_keep_line_number(true);
		Options.v().set_keep_offset(true);

		// replace Soot's printer with our logger
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
//		Options.v().set_app(true);  //Dunno what this is
		Options.v().set_process_dir(Collections.singletonList(apk));
		Options.v().set_force_android_jar(androidJAR);
		Options.v().set_android_jars(libJars);
		Options.v().set_src_prec(Options.src_prec_apk);
		Options.v().set_debug(true);
		// Options.v().set_exclude(Arrays.asList(new String[] {"javax.xml"}));

		// All packages which are not already in the app's transitive hull but
		// are required by the injected code need to be marked as dynamic.
		Options.v().set_dynamic_package(
				Arrays.asList(new String[] { "acteve.symbolic.", "models.","com.android", "org.json", "org.apache", "org.w3c",
						"org.xml", "junit", "javax", "javax.crypto"}));

		Scene.v().loadNecessaryClasses();

		//Register all application classes for instrumentation
		System.out.println("Application classes");
		Chain<SootClass> appclasses = Scene.v().getApplicationClasses();
		for (SootClass c:appclasses) {
			System.out.println("   class: " + c.getName() + " - " + c.resolvingLevel());
		}

		//Collect additional classes which will be injected into the app
		List<String> libClassesToInject = SourceLocator.v().getClassesUnder("./jars/a3t_symbolic.jar");		
		for (String s:libClassesToInject) {
			Scene.v().addBasicClass(s, SootClass.BODIES);
			Scene.v().loadClassAndSupport(s);
			SootClass clazz = Scene.v().forceResolve(s, SootClass.BODIES);
			clazz.setApplicationClass();
		}
		
		PackManager.v().getPack("cg").apply();
		
		PackManager.v().getPack("wjtp").add(new Transform("wjtp.acteve", new Main()));
		
		// -------------------------------- BEGIN RAFAEL ----------------------------------------------
		if (LIMIT_TO_CALL_PATH ) {
			//TODO: get callgraph and extract all classes on the path to any calls for class loading via reflection
			List<SootMethod> entryPoints = MethodUtils.findApplicationEntryPoints();
			
			List<SootMethod> methodsWithReflectiveClassLoading = MethodUtils.findReflectiveLoadingMethods();
			System.out.println("Found the following reflective class loading methods:");
			for (SootMethod m : methodsWithReflectiveClassLoading){
				System.out.println("Signature: " + m.getSignature());
			}
			
			//we have all SootMethods now which might be used to load classes at runtime. Now get the classes on the paths to them:
			HashSet<SootClass> classesAlongTheWay = new HashSet<SootClass>();
			for (SootMethod sm : methodsWithReflectiveClassLoading){
				//add the declaring class: TODO: do we actually need to instrument that to? probably not.
				classesAlongTheWay.add(sm.getDeclaringClass());
				//and all the classes on the way to the call:
				for (SootMethod caller : MethodUtils.findTransitiveCallersOf(sm))
					classesAlongTheWay.add(caller.getDeclaringClass());
			}
			
			classes = new ArrayList<SootClass>(classesAlongTheWay);
			
			if(DEBUG){
				System.out.println("Found " + classesAlongTheWay.size() + " classes that declare methods on the path to reflection, i.e. that need to be instrumented.");
				for (SootClass c : classes)
					System.out.println("Class: " + c.getName());
			}
		}
		// -------------------------------- END RAFAEL ----------------------------------------------

		PackManager.v().runPacks();
		PackManager.v().writeOutput();
		
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

			System.out.println("Done. Have fun with " + f.getAbsolutePath());
		} else {
			System.out.println("ERROR: " + outputApk + " does not exist");
		}
	}


	private static void signAPK(String apk) {
		try {
			// jarsigner is part of the Java SDK
			System.out.println("Signing " + apk + " ...");
			String cmd = "jarsigner -verbose -digestalg SHA1 -sigalg MD5withRSA -storepass android -keystore "+System.getProperty("user.home")+"/.android/debug.keystore "
					+ apk + " androiddebugkey";
			System.out.println("Calling " + cmd);
			Process p = Runtime.getRuntime().exec(cmd);
			printProcessOutput(p);

			// zipalign is part of the Android SDK
			System.out.println("Zipalign " + apk + " ...");
			cmd = "zipalign -v 4 " + apk + " " + new File(apk).getName() + "_signed.apk";
			System.out.println(cmd);
			p = Runtime.getRuntime().exec(cmd);
			printProcessOutput(p);
		} catch (IOException e) {
			e.printStackTrace();
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
		return classes.contains(klass);
	}

	
	/**
	 * By Julian 
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
			Dependencies dep = jcs.resolve(sc);

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
