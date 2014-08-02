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
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Pattern;

import soot.Scene;
import soot.PackManager;
import soot.Scene;
import soot.SootClass;
import soot.SceneTransformer;
import soot.Transform;

public class Main extends SceneTransformer {
	private static Config config;
    private static List<SootClass> classes = new ArrayList();
	private static Map<String,List<String>> uninstrumentedClasses = new HashMap();
    private static final String dummyMainClassName = "acteve.symbolic.DummyMain";
	private static boolean DEBUG = false;

	//private static Pattern includePat = Pattern.compile("(android.view.ViewGroup)|(android.graphics.Rect)");
	//Pattern.compile("android\\..*|com\\.android\\..*|com\\.google\\.android\\..*");
	private static Pattern excludePat = Pattern.compile(
		 "(java\\..*)|(dalvik\\..*)|(android\\.os\\.(Parcel|Parcel\\$.*))|(android\\.util\\.Slog)|(android\\.util\\.(Log|Log\\$.*))");

    protected void internalTransform(String phaseName, Map options)
    {
		if (DEBUG) printClasses("bef_instr.txt");

		ModelMethodsHandler.readModelMethods(config.modelMethsFile);
		Instrumentor ci = new Instrumentor(config.rwKind, 
										   config.outDir, 
										   config.sdkDir, 
										   config.fldsWhitelist, 
										   config.fldsBlacklist, 
										   config.methsWhitelist, 
										   config.instrAllFields);
		ci.instrument(classes);
		InputMethodsHandler.instrument(config.inputMethsFile);
		ModelMethodsHandler.addInvokerBodies();
		Scene.v().getApplicationClasses().remove(Scene.v().getSootClass(dummyMainClassName));
		
		if (DEBUG) printClasses("aft_instr.txt");
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
	
    public static void main(String[] args)
    {
		config = Config.g();
		
		Scene.v().setSootClassPath(config.inJars + File.pathSeparator + config.libJars);
		
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
		builder.append(config.outJar+" ");
		builder.append("-O ");
		builder.append("-validate ");
		builder.append(dummyMainClassName);
		String[] sootArgs = builder.toString().split(" ");
		soot.Main.main(sootArgs);

		new AddUninstrClassesToJar(uninstrumentedClasses, config.outJar).apply();
    }
	
	private static void loadClassesToInstrument()
    {
		for (String pname : config.inJars.split(File.pathSeparator)) {
			if (pname.endsWith(".jar")) {
				//System.out.println("pname "+pname);
				JarFile jar = null;
				try {
					jar = new JarFile(pname);
				} catch(IOException e) {
					throw new RuntimeException(e.getMessage() + " " + pname);
				}
				if (jar == null)
					continue;
				for (Enumeration<JarEntry> e = jar.entries(); e.hasMoreElements();) {
					JarEntry entry = e.nextElement();
					String name = entry.getName();
					if (name.endsWith(".class")) {
						name = name.replace(".class", "").replace(File.separatorChar, '.');
						if(!toBeInstrumented(name)){
							System.out.println("Skipped instrumentation of class: " + name);
							addUninstrumentedClass(pname, name);
							continue;
						}
						try{
							SootClass klass = Scene.v().loadClassAndSupport(name);
							classes.add(klass);
						}catch(RuntimeException ex) {
							System.out.println("Failed to load class: " + name);
							addUninstrumentedClass(pname, name);
							if (ex.getMessage().startsWith("couldn't find class:")) {
								System.out.println(ex.getMessage());
							}
							else
								throw ex;
						}
					}
				}
			}
		}
    }

	private static void loadOtherClasses()
	{
		String[] classNames = new String[]{
			"acteve.symbolic.array.BooleanArrayConstant",
			"acteve.symbolic.array.ShortArrayConstant",
			"acteve.symbolic.array.ByteArrayConstant",
			"acteve.symbolic.array.CharArrayConstant",
			"acteve.symbolic.array.IntegerArrayConstant",
			"acteve.symbolic.array.LongArrayConstant",
			"acteve.symbolic.array.FloatArrayConstant",
			"acteve.symbolic.array.DoubleArrayConstant"
		};
		
		for(String cname : classNames)
			Scene.v().loadClassAndSupport(cname);
		
		if(!config.isSDK())
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
		
	static boolean isInstrumented(SootClass klass)
	{
		return classes.contains(klass);
	}

	private static boolean toBeInstrumented(String className)
	{
		if (true/*includePat.matcher(className).matches()*/) {
			return excludePat.matcher(className).matches() ? false : true;
		}
		return false;
	}
}
