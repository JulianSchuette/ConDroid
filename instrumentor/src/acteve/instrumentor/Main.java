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
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Pattern;
import java.util.zip.ZipException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;

import org.apache.tools.ant.taskdefs.Mkdir;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import acteve.explorer.Z3Model.Array;
import soot.Body;
import soot.BodyTransformer;
import soot.JimpleClassSource;
import soot.MethodOrMethodContext;
import soot.Modifier;
import soot.PackManager;
import soot.Scene;
import soot.SceneTransformer;
import soot.SootClass;
import soot.SootMethod;
import soot.SootResolver;
import soot.SourceLocator;
import soot.Transform;
import soot.javaToJimple.IInitialResolver.Dependencies;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.ReachableMethods;
import soot.options.Options;
import soot.util.Chain;
import soot.util.queue.QueueReader;

public class Main extends SceneTransformer {
	private static Config config;
	private static List<SootClass> classes = new ArrayList<SootClass>();
	private static Map<String, List<String>> uninstrumentedClasses = new HashMap<String, List<String>>();
	private static final String dummyMainClassName = "acteve.symbolic.DummyMain";
	static boolean DEBUG = true;
	public final static boolean DUMP_JIMPLE = true; //default: false. Set to true to create Jimple code instead of APK
	public final static boolean VALIDATE = false; //Set to true to apply some consistency checks. Set to false to get past validation exceptions and see the generated code. Note: these checks are more strict than the Dex verifier and may fail at some obfuscated, though valid classes
	private static boolean LIMIT_TO_CALL_PATH = false; //Limit instrumentation to methods along the CP to reflection use?
	private final static String androidJAR = "./libs/android-14.jar"; //required for CH resolution
	private final static String libJars = "./jars/a3t_symbolic.jar"; //libraries
	private final static String modelClasses = "./mymodels/src"; //Directory where user-defined model classes reside.
//	private final static String apk = "./de.fhg.aisec.classloadtest.apk"; //Example app USING REFLECTIVE LOADING to instrument
//	private final static String apk = "./SkeletonApp/lib/SkeletonApp.apk"; //Example app to instrument
	private final static String apk = "/home/julian/workspace/acteve/de.fhg.aisec.concolicexample.apk";
//	private final static String apk = "/home/fedler/android-concolic-execution/android-concolic-execution/de.fhg.aisec.concolicexample.apk";
	private final static String jimpleFolder = "./acteve-util-jimple/";
	private final static String acteveSymbolicUtilityJimple = jimpleFolder + "acteve.symbolic.Util.jimple";

	
	// private static Pattern includePat =
	// Pattern.compile("(android.view.ViewGroup)|(android.graphics.Rect)");
	// Pattern.compile("android\\..*|com\\.android\\..*|com\\.google\\.android\\..*");
	
	/**
	 * Classes to exclude from instrumentation (all acteve, dalvik classes, plus some android SDK classes which are used by the instrumentation itself).
	 */
	private static Pattern excludePat = Pattern
			.compile("(acteve\\..*)|(java\\..*)|(dalvik\\..*)|(android\\.os\\.(Parcel|Parcel\\$.*))|(android\\.util\\.Slog)|(android\\.util\\.(Log|Log\\$.*))");

	@Override
	protected void internalTransform(String phaseName, Map options) {
		if (DEBUG)
			printClasses("bef_instr.txt");

		ModelMethodsHandler.readModelMethods(config.modelMethsFile);
		Instrumentor ci = new Instrumentor(config.rwKind, config.outDir, config.sdkDir, config.fldsWhitelist,
				config.fldsBlacklist, config.methsWhitelist, config.instrAllFields);
		ci.instrument(classes);
		InputMethodsHandler.instrument(config.inputMethsFile);
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

	public static void main(String[] args) throws ZipException, XPathExpressionException, IOException, InterruptedException, ParserConfigurationException, SAXException {
		config = Config.g();

		Options.v().set_soot_classpath("/home/julian/workspace/acteve/android-concolic-execution/libs/android-19.jar"+":"+libJars+":"+modelClasses);
//		Options.v().set_soot_classpath("/home/fedler/android-concolic-execution/android-concolic-execution/libs/android-19.jar"+":"+libJars);

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
		
		//loadOtherClasses();
		
		/*
		 * loading only this one file was in fact NOT enough:
		//load acteve.symbolic.Util body from jimple:
		SootClass acteveSymbolicUtilSC = loadFromJimple(new File(acteveSymbolicUtilityJimple), "acteve.symbolic.Util");
		Scene.v().addClass(acteveSymbolicUtilSC);
		*/
		
		//load ALL acteve jimples:
//		File folder = new File(jimpleFolder);
//		File[] listOfFiles = folder.listFiles();
//		for (int i = 0; i < listOfFiles.length; i++) {
//			if (listOfFiles[i].isFile()) {
//				SootClass tmp = loadFromJimple(listOfFiles[i], listOfFiles[i].getName().replace(".jimple", ""));
//				Scene.v().addClass(tmp);
//			}
//		}
		
		PackManager.v().getPack("cg").apply();
		
		PackManager.v().getPack("wjtp").add(new Transform("wjtp.acteve", new Main()));
//		PackManager.v().getPack("jtp").add(new Transform("jtp.acteve", new Main()));
		
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

		// new AddUninstrClassesToJar(uninstrumentedClasses,
		// config.outJar).apply();

		String outputApk = "sootOutput/"+new File(apk).getName();
		if (new File(outputApk).exists()) {
			File f = new File(outputApk);
			
			//add WRITE_EXTERNAL_STORAGE to manifest:
			//addExternalStoragePermission(f.getAbsolutePath());
			
			//Sign the APK
			signAPK(f.getAbsolutePath());

			System.out.println("Done. Have fun with " + f.getAbsolutePath());
		} else {
			System.out.println("ERROR: " + outputApk + " does not exist");
		}
	}

	private static void copyFileUsingChannel(File source, File dest) throws IOException {
		FileChannel sourceChannel = null;
		FileChannel destChannel = null;
		try {
			sourceChannel = new FileInputStream(source).getChannel();
			destChannel = new FileOutputStream(dest).getChannel();
			destChannel.transferFrom(sourceChannel, 0, sourceChannel.size());
		} finally {
			sourceChannel.close();
			destChannel.close();
		}
	}

	private static File pack(File apkFile) throws ZipException, IOException, InterruptedException,
			ParserConfigurationException, SAXException, XPathExpressionException {
		// unpack
		System.out.println("Decoding " + apkFile.getAbsolutePath());
		Process p = Runtime.getRuntime().exec(
				"java -jar libs/apktool.jar d -s -f " + apkFile.getAbsolutePath() + " decoded");
		int processExitCode = p.waitFor();
		if (processExitCode != 0) {
			System.out.println("Something went wrong when unpacking " + apkFile.getAbsolutePath());
			return null;
		}

		File dex = new File("sootOutput/classes.dex");
		File newDex = new File("decoded/classes.dex");
		Main.copyFileUsingChannel(dex, newDex);

		// Now pack again
		File newFile = new File(apkFile.getAbsolutePath().replace(".apk", "") + "_modified.apk");
		p = Runtime.getRuntime().exec("java -jar libs/apktool.jar b decoded/ " + newFile.getAbsolutePath());
		processExitCode = p.waitFor();
		if (processExitCode != 0) {
			System.out.println("Something went wrong when packing " + apkFile.getAbsolutePath());
			return null;
		}

		return newFile;
	}
	
	private static String readFile(String path) throws IOException 
	{
		byte[] encoded = Files.readAllBytes(Paths.get(path));
		return new String(encoded);
	}
	
	private static void addExternalStoragePermission(String apkpath) throws ParserConfigurationException, SAXException{
		try {
			//need to unpack APK, overwrite manifest, and write back to APK
			System.out.println("Replacing " + apkpath + " manifest to contain WRITE_EXTERNAL_STORAGE permission.");
			Process p = Runtime.getRuntime().exec(
					"java -jar libs/apktool.jar d -s -f " + apkpath + " unpacked");
			int processExitCode = p.waitFor();
			if (processExitCode != 0) {
				System.out.println("Something went wrong when unpacking " + apkpath);
				//return null;
			}
			
			String manifestString = readFile("unpacked/AndroidManifest.xml");
			
			//the following does NOT work cause we cannot write it back properly:
			/*
			ManifestData manifest = AndroidManifestParser.parse(new ByteArrayInputStream(manifestString.getBytes("UTF-8")));
			manifest.addUsesPermission("android.permission.WRITE_EXTERNAL_STORAGE");
			
			//do some string action now:
			boolean hasWriteExtPermission = manifestString.contains("WRITE_EXTERNAL_STORAGE");
			if (hasWriteExtPermission)
				return; //nothing to do here
			
			//find use permissions block:
			int pos = manifestString.lastIndexOf("<uses-permission android:name=");
			//find closing tag of the last uses-permission:
			int pos2 = manifestString.indexOf("</uses-permission>", pos) + "</uses-permission>".length();
			if (pos2 < pos) //this may seem stupid at first but just means we didnt find above string behind pos
				pos2 = manifestString.indexOf("/>", pos) + "/>".length();
			//insert new permission:
			String outManifestString = manifestString.substring(0, pos2) + "\n<uses-permission android:name=\"android.permission.WRITE_EXTERNAL_STORAGE\"></uses-permission>\n" + manifestString.substring(pos2);
			
			System.out.println("Writing out new manifest now:");
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
			          new FileOutputStream("unpacked/AndroidManifest.xml"), "utf-8"));
			writer.write(outManifestString);
			writer.close();
			System.out.println("New manifest file written. Now rebuilding APK with new manifest.");
			*/
			
			
		    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		    DocumentBuilder builder = factory.newDocumentBuilder();
		    Document document = builder.parse( new File("unpacked/AndroidManifest.xml") );
		    
		    Node entryNode = document.getFirstChild(); //this should be the root node
		    System.out.println("Entry node: " + entryNode.getNodeName());
			
			
			
			p = Runtime.getRuntime().exec("java -jar libs/apktool.jar b unpacked/ " + apkpath);
			processExitCode = p.waitFor();
			if (processExitCode != 0) {
				System.out.println("Something went wrong when packing " + apkpath);
//				return null;
			}
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	private static void signAPK(String apk) {
		try {
			// jarsigner is part of the Java SDK
			System.out.println("Signing " + apk + " ...");
			String cmd = "jarsigner -verbose -digestalg SHA1 -sigalg MD5withRSA -storepass android -keystore /home/julian/.android/debug.keystore "
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

	@Deprecated
	public static void main_org(String[] args) {
		config = Config.g();

		String appToInstrument = "./SkeletonApp.apk";
		System.setProperty("a3t.in.jars", appToInstrument);
		Scene.v().setSootClassPath(config.inJars + File.pathSeparator + config.libJars);
		// Scene.v().setSootClassPath(appToInstrument + File.pathSeparator +
		// "./bin:libs/core.jar:libs/ext.jar:libs/junit.jar:libs/bouncycastle.jar:libs/android-14.jar");

		loadClassesToInstrument();
		loadOtherClasses();
		PackManager.v().getPack("wjtp").add(new Transform("wjtp.acteve", new Main()));

		StringBuilder builder = new StringBuilder();
		builder.append("-w -p cg off -keep-line-number -keep-bytecode-offset ");
		builder.append("-dynamic-class ");
		builder.append("acteve.symbolic.Util ");
		builder.append("-soot-classpath ");
		builder.append(config.inJars + File.pathSeparator + config.libJars + " ");
		builder.append("-dynamic-package ");
		builder.append("acteve.symbolic.integer. ");
		builder.append("-dynamic-package ");
		builder.append("models. ");
		builder.append("-outjar -d ");
		builder.append(config.outJar + " ");
		builder.append("-O ");
		builder.append("-validate ");
		builder.append(dummyMainClassName);
		String[] sootArgs = builder.toString().split(" ");
		soot.Main.main(sootArgs);

		new AddUninstrClassesToJar(uninstrumentedClasses, config.outJar).apply();
	}

	private static void loadClassesToInstrument() {
	}
	
	private static SootClass resolveSootClass(String name) {
		Scene.v().addBasicClass(name, SootClass.SIGNATURES);
		Scene.v().forceResolve(name, SootClass.SIGNATURES);
		SootClass klass = Scene.v().loadClassAndSupport(name);
		return klass;
	}
	
	
	private static void loadClassesToInstrument_old(String jarFile) {
		for (String pname : jarFile.split(File.pathSeparator)) {
			if (pname.endsWith(".jar") || pname.endsWith(".apk")) {
				// System.out.println("pname "+pname);
				JarFile jar = null;
				try {
					jar = new JarFile(pname);
				} catch (IOException e) {
					throw new RuntimeException(e.getMessage() + " " + pname);
				}
				if (jar == null)
					continue;
				for (Enumeration<JarEntry> e = jar.entries(); e.hasMoreElements();) {
					JarEntry entry = e.nextElement();
					String name = entry.getName();
					if (name.endsWith(".class")) {
						name = name.replace(".class", "").replace(File.separatorChar, '.');
						if (!toBeInstrumented(name)) {
							System.out.println("Skipped instrumentation of class: " + name);
							addUninstrumentedClass(pname, name);
							continue;
						}
						try {
							Scene.v().addBasicClass(name, SootClass.SIGNATURES);
							Scene.v().forceResolve(name, SootClass.SIGNATURES);
//							SootResolver.v().resolveClass(name, SootClass.SIGNATURES);
							SootClass klass = Scene.v().loadClassAndSupport(name);
							if (!classes.contains(klass))
								classes.add(klass);
						} catch (RuntimeException ex) {
							System.out.println("Failed to load class: " + name);
							addUninstrumentedClass(pname, name);
							if (ex.getMessage().startsWith("couldn't find class:")) {
								System.out.println(ex.getMessage());
							} else
								throw ex;
						}
					}
				}
			}
		}
	}

	private static void loadOtherClasses() {
		String[] classNames = new String[] { "acteve.symbolic.array.BooleanArrayConstant",
				"acteve.symbolic.array.ShortArrayConstant", "acteve.symbolic.array.ByteArrayConstant",
				"acteve.symbolic.array.CharArrayConstant", "acteve.symbolic.array.IntegerArrayConstant",
				"acteve.symbolic.array.LongArrayConstant", "acteve.symbolic.array.FloatArrayConstant",
				"acteve.symbolic.array.DoubleArrayConstant" };
		
		

		for (String cname : classNames)
			Scene.v().loadClassAndSupport(cname);

		if (!config.isSDK())
			Scene.v().loadClassAndSupport(G.SYMOPS_CLASS_NAME);
	}

	private static void addUninstrumentedClass(String jarName, String className) {
		List<String> cs = uninstrumentedClasses.get(jarName);
		if (cs == null) {
			cs = new ArrayList();
			uninstrumentedClasses.put(jarName, cs);
		}
		cs.add(className);
	}

	static boolean isInstrumented(SootClass klass) {
		return classes.contains(klass);
	}

	/**
	 * Returns true if a class should be instrumented.
	 * 
	 * @param className
	 * @return
	 */
	private static boolean toBeInstrumented(String className) {
		if (true/* includePat.matcher(className).matches() */) {
			return excludePat.matcher(className).matches() ? false : true;
		}
		return false;
	}

	@Deprecated 
	protected void internalTransform(Body b, String phaseName, Map<String, String> options) {
		if (DEBUG)
			printClasses("bef_instr.txt");

		ModelMethodsHandler.readModelMethods(config.modelMethsFile);
		Instrumentor ci = new Instrumentor(config.rwKind, config.outDir, config.sdkDir, config.fldsWhitelist,
				config.fldsBlacklist, config.methsWhitelist, config.instrAllFields);
//		ci.instrument(b.getMethod());
//		InputMethodsHandler.instrument(config.inputMethsFile);
		InputMethodsHandler.apply(b.getMethod()); //Instrument current method //TODO Probably need to check if current method should be instrumented at all
		ModelMethodsHandler.addInvokerBodies();
//		Scene.v().getApplicationClasses().remove(Scene.v().getSootClass(dummyMainClassName));

		if (DEBUG)
			printClasses("aft_instr.txt");
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
