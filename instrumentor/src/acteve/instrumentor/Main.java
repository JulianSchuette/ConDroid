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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Pattern;
import java.util.zip.ZipException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;

import org.xml.sax.SAXException;

import soot.Body;
import soot.BodyTransformer;
import soot.JimpleClassSource;
import soot.Modifier;
import soot.PackManager;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Transform;
import soot.javaToJimple.IInitialResolver.Dependencies;
import soot.options.Options;

public class Main extends BodyTransformer {
	private static Config config;
	private static List<SootClass> classes = new ArrayList();
	private static Map<String, List<String>> uninstrumentedClasses = new HashMap();
	private static final String dummyMainClassName = "acteve.symbolic.DummyMain";
	private static boolean DEBUG = true;
	public final static boolean DUMP_JIMPLE = false; //default: false. Set to true to create Jimple code instead of APK
	public final static boolean VALIDATE = false; //Set to true to apply some consistency checks. Set to false to get past validation exceptions and see the generated code. Note: these checks are more strict than the Dex verifier and may fail at some obfuscated, though valid classes
	private final static String androidJAR = "./libs/android-14.jar"; //required for CH resolution
	private final static String libJars = ""; //libraries
	private final static String apk = "./TestApp2.apk"; //Example app to instrument

	
	// private static Pattern includePat =
	// Pattern.compile("(android.view.ViewGroup)|(android.graphics.Rect)");
	// Pattern.compile("android\\..*|com\\.android\\..*|com\\.google\\.android\\..*");
	private static Pattern excludePat = Pattern
			.compile("(java\\..*)|(dalvik\\..*)|(android\\.os\\.(Parcel|Parcel\\$.*))|(android\\.util\\.Slog)|(android\\.util\\.(Log|Log\\$.*))");

	@Deprecated
	protected void internalTransform(String phaseName, Map options) {
//		if (DEBUG)
//			printClasses("bef_instr.txt");
//
//		ModelMethodsHandler.readModelMethods(config.modelMethsFile);
//		Instrumentor ci = new Instrumentor(config.rwKind, config.outDir, config.sdkDir, config.fldsWhitelist,
//				config.fldsBlacklist, config.methsWhitelist, config.instrAllFields);
//		ci.instrument(classes);
//		InputMethodsHandler.instrument(config.inputMethsFile);
//		ModelMethodsHandler.addInvokerBodies();
//		Scene.v().getApplicationClasses().remove(Scene.v().getSootClass(dummyMainClassName));
//
//		if (DEBUG)
//			printClasses("aft_instr.txt");
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

//		Scene.v().setSootClassPath(androidJAR + File.pathSeparator + libJars);

		Options.v().set_whole_program(true);
//		Options.v().setPhaseOption("cg", "off");
	    Options.v().set_keep_line_number(true);
		Options.v().set_keep_offset(true);
		
		//Dynamic classes are not in soot classpath but considered to be available at runtime
		Options.v().set_dynamic_class(Arrays.asList(new String[] { "acteve.symbolic.Util" }));

		//Dynamic packages are not in soot classpath but considered to be available at runtime		
		Options.v().set_dynamic_package(Arrays.asList(new String[] { "acteve.symbolic.integer. ", "models. " }));

		// replace Soot's printer with our logger
		// G.v().out = new PrintStream(new LogStream(Logger.getLogger("SOOT"),
		// Level.DEBUG), true);

		Options.v().set_allow_phantom_refs(true);
		Options.v().set_whole_program(true);
		Options.v().set_prepend_classpath(true);
		Options.v().set_validate(true);

		if (DUMP_JIMPLE) {
			Options.v().set_output_format(Options.output_format_jimple);
		} else {
			Options.v().set_output_format(Options.output_format_dex);
		}
		Options.v().set_app(true);
		Options.v().set_process_dir(Collections.singletonList(apk));
		Options.v().set_force_android_jar(androidJAR);
		Options.v().set_android_jars(libJars);
		Options.v().set_src_prec(Options.src_prec_apk);
		Options.v().set_debug(true);
		// Options.v().set_exclude(Arrays.asList(new String[] {"javax.xml"}));

		// All packages which are not already in the app's transivite hull but
		// are required by the injected code need to be marked as dynamic.
		Options.v().set_dynamic_package(
				Arrays.asList(new String[] { "acteve", "android.", "com.android", "org.json", "org.apache", "org.w3c",
						"org.xml", "junit", "javax", "javax.crypto"}));
		// Make sure all classes to be added to the apk are in Soot classpath
		// Options.v().set_soot_classpath(androidJAR+":./bin"); //OWN
		Options.v().set_soot_classpath(androidJAR + File.pathSeparator + libJars);


		// A better way to resolve dependency classes is to add them to the
		// library path
		// Scene.v().addBasicClass("org.simalliance.openmobileapi.Session",SootClass.SIGNATURES);

		//Force load Util class which is not present in the app (yet)
		//TODO The class must be available in soot classpath. Probably it has to be in Jimple format
		SootClass util = Scene.v().getSootClass("acteve.symbolic.Util");
		Scene.v().forceResolve(util.getName(), SootClass.BODIES);
		util.setApplicationClass();
		System.out.println("Printing methods of acteve.symbolic.Util");
		for (SootMethod m: util.getMethods() ) {
			System.out.println("   DEBUG in MAIN: Method in Util: " + m.getDeclaration());
		}

		
		Scene.v().loadNecessaryClasses();
		loadClassesToInstrument();
		loadOtherClasses();
//		PackManager.v().getPack("wjtp").add(new Transform("wjtp.acteve", new Main()));
		PackManager.v().getPack("jtp").add(new Transform("jtp.acteve", new Main()));

		// Scene.v().forceResolve(TOAST_CLASS, SootClass.BODIES);
		// Scene.v().forceResolve("instrumentation.Main", SootClass.BODIES);

		PackManager.v().runPacks();
		PackManager.v().writeOutput();

		// new AddUninstrClassesToJar(uninstrumentedClasses,
		// config.outJar).apply();

		String outputApk = "sootOutput/"+new File(apk).getName();
		if (new File(outputApk).exists()) {
			File f = new File(outputApk);
			
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

	private static void signAPK(String apk) {
		try {
			// jarsigner is part of the Java SDK
			System.out.println("Signing " + apk + " ...");
			String cmd = "jarsigner -verbose -digestalg SHA1 -sigalg MD5withRSA -storepass android -keystore /home/julian/.android/debug.keystore "
					+ apk + " androiddebugkey";
			System.out.println("Calling " + cmd);
			Runtime.getRuntime().exec(cmd);

			// zipalign is part of the Android SDK
			System.out.println("Zipalign " + apk + " ...");
			cmd = "zipalign -v 4 " + apk + " " + new File(apk).getName() + "_signed.apk";
			System.out.println(cmd);
			Runtime.getRuntime().exec(cmd);
		} catch (IOException e) {
			e.printStackTrace();
		}
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
	
	@Deprecated
	private static void loadClassesToInstrument_old() {
		for (String pname : config.inJars.split(File.pathSeparator)) {
			if (pname.endsWith(".jar")) {
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
							SootClass klass = Scene.v().loadClassAndSupport(name);
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

	private static boolean toBeInstrumented(String className) {
		if (true/* includePat.matcher(className).matches() */) {
			return excludePat.matcher(className).matches() ? false : true;
		}
		return false;
	}

	@Override
	protected void internalTransform(Body b, String phaseName, Map<String, String> options) {
		if (DEBUG)
			printClasses("bef_instr.txt");

		ModelMethodsHandler.readModelMethods(config.modelMethsFile);
		Instrumentor ci = new Instrumentor(config.rwKind, config.outDir, config.sdkDir, config.fldsWhitelist,
				config.fldsBlacklist, config.methsWhitelist, config.instrAllFields);
		ci.instrument(b.getMethod());
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
	public SootClass loadFromJimple(File f, String className) {
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
}
