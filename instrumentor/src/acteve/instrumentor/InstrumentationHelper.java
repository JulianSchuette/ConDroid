package acteve.instrumentor;



import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;


import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import soot.ArrayType;
import soot.Body;
import soot.CharType;
import soot.Immediate;
import soot.JimpleClassSource;
import soot.Local;
import soot.Modifier;
import soot.Printer;
import soot.RefLikeType;
import soot.RefType;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.SourceLocator;
import soot.Type;
import soot.Unit;
import soot.Value;
import soot.VoidType;
import soot.javaToJimple.IInitialResolver.Dependencies;
import soot.javaToJimple.LocalGenerator;
import soot.jimple.InvokeExpr;
import soot.jimple.Jimple;
import soot.jimple.JimpleBody;
import soot.jimple.StringConstant;
import soot.jimple.internal.ImmediateBox;
import soot.jimple.internal.JAssignStmt;
import soot.jimple.internal.JIdentityStmt;
import soot.jimple.internal.JInvokeStmt;
import soot.jimple.internal.JNewExpr;
import soot.jimple.internal.JSpecialInvokeExpr;
import soot.jimple.internal.JVirtualInvokeExpr;
import soot.jimple.internal.JimpleLocal;
import soot.jimple.parser.lexer.LexerException;
import soot.jimple.parser.parser.ParserException;
import soot.options.Options;
import soot.util.Chain;

/**
 * Helper class for insturmenting bytecode artifacts.
 * 
 * @author Julian Schuette (julian.schuette@aisec.fraunhofer.de)
 * 
 */
public class InstrumentationHelper {

	/**
	 * Clear up stuff after use
	 */
	private static final boolean CLEAR_AFTER_USER = true;
	private String manifest;
	private HashSet<String> mainActivities = new HashSet<String>();

	private InstrumentationHelper() {
		// Don't call me, I'm private
	}

	/**
	 * 
	 * @param apkFile
	 *            APK File to load
	 * @throws IOException
	 * @throws InterruptedException
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 * @throws XPathExpressionException 
	 */
	public InstrumentationHelper(File apkFile) throws IOException, InterruptedException, ParserConfigurationException,
			SAXException, XPathExpressionException {
		// unpack
		if (Main.DEBUG) {
			System.out.println("Decoding " + apkFile.getAbsolutePath());
		}
		Process p = Runtime.getRuntime().exec(
				"java -jar libs/apktool.jar d -s -f " + apkFile.getAbsolutePath() + " decoded");
		int processExitCode = p.waitFor();
		if (processExitCode != 0) {
			throw new RuntimeException("Something went wrong during unpacking");
		}
		BufferedReader br = new BufferedReader(new FileReader("decoded/AndroidManifest.xml"));
		StringBuffer sb = new StringBuffer();
		String line = "";
		while ((line = br.readLine()) != null) {
			sb.append(line);
			sb.append("\n");
		}
		br.close();
		manifest = sb.toString();

		// Do some XML parsing
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		Document doc = dBuilder.parse(new ByteArrayInputStream(manifest.getBytes()));
		doc.getDocumentElement().normalize();

		XPath xpath = XPathFactory.newInstance().newXPath();
        XPathExpression expr1 = xpath.compile("//manifest/@package");
        NodeList nodes = (NodeList)expr1.evaluate(doc, XPathConstants.NODESET);
        String packagename = ((Attr) nodes.item(0)).getValue();
        
		xpath = XPathFactory.newInstance().newXPath();
        expr1 = xpath.compile("//manifest/application/activity[intent-filter/action[@name='android.intent.action.MAIN']]/@name");
        nodes = (NodeList)expr1.evaluate(doc, XPathConstants.NODESET);
        for (int i=0;i<nodes.getLength();i++) {
        	Node n = nodes.item(i);
        	String classname = ((Attr) n).getValue();
        	if (classname.startsWith("."))
        		classname = packagename + classname;
        	mainActivities.add(classname);
        }
	}

	/**
	 * Example of creating a new class from scratch.
	 * 
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public SootClass createTrackerClass() {
		SootClass clazz = new SootClass("Tracker", Modifier.PUBLIC);
		clazz.setSuperclass(Scene.v().getSootClass("java.lang.Object"));

		List params = Arrays.asList(new Type[] { ArrayType.v(RefType.v("java.lang.String"), 1) });
		SootMethod m = new SootMethod("print", params, VoidType.v(), Modifier.PUBLIC | Modifier.STATIC);

		JimpleBody body = Jimple.v().newBody(m);
		m.setActiveBody(body);

		// Create the method body
		{
			m.setActiveBody(body);
			Chain units = body.getUnits();
			Local arg, tmpRef;

			// Add some locals, java.lang.String l0
			arg = Jimple.v().newLocal("l0", ArrayType.v(RefType.v("java.lang.String"), 1));
			body.getLocals().add(arg);

			// Add locals, java.io.printStream tmpRef
			tmpRef = Jimple.v().newLocal("tmpRef", RefType.v("java.io.PrintStream"));
			body.getLocals().add(tmpRef);

			// add "l0 = @parameter0"
			units.add(Jimple.v().newIdentityStmt(arg,
					Jimple.v().newParameterRef(ArrayType.v(RefType.v("java.lang.String"), 1), 0)));

			// add "tmpRef = java.lang.System.out"
			units.add(Jimple.v().newAssignStmt(
					tmpRef,
					Jimple.v().newStaticFieldRef(
							Scene.v().getField("<java.lang.System: java.io.PrintStream out>").makeRef())));

			// insert "tmpRef.println("Hello world!")"
			{
				SootMethod toCall = Scene.v().getMethod("<java.io.PrintStream: void println(java.lang.String)>");
				units.add(Jimple.v().newInvokeStmt(
						Jimple.v().newVirtualInvokeExpr(tmpRef, toCall.makeRef(), StringConstant.v("Hello world!"))));
			}

			// insert "return"
			units.add(Jimple.v().newReturnVoidStmt());

		}

		clazz.addMethod(m);
		
		return clazz;
	}

	/**
	 * Inserts statements at the begin of method, but ensures to put the
	 * inserted code after super() calls.
	 * 
	 * @param body
	 * @param stmts
	 */
	public static void insertAtBeginOfMethod(Body body, Chain<Unit> stmts) {
		if (Main.DEBUG) {
			System.out.println("Inserting at begin of " + body.getMethod().getName());
		}

		Iterator<Unit> i = body.getUnits().snapshotIterator();
		Unit u = i.next();
		while (u instanceof JIdentityStmt
				|| (u instanceof JInvokeStmt && ((JInvokeStmt) u).getInvokeExpr().getMethod().getName()
						.equals(body.getMethod().getName())))
			u = i.next(); // Skip super()

		body.getUnits().insertBefore(stmts, u);
	}
	

	/**
	 * Inserts a chain of statements before every Jimple line matching the given regex.
	 * 
	 * @param body
	 * @param stmts
	 * @param regex
	 * @throws ParserException
	 * @throws LexerException
	 * @throws IOException
	 */
	public static void insertBefore(Body body, Chain<Unit> stmts, String regex) throws ParserException, LexerException, IOException {
		Pattern expr = Pattern.compile(regex);
		Iterator<Unit> i = body.getUnits().snapshotIterator();
		while (i.hasNext()) {
			Unit u = i.next();
			Matcher m=expr.matcher(u.toString());
			if (m.matches()) {
				body.getUnits().insertBefore(stmts, u);
			}
		}
		
	}

	/**
	 * Inserts a chain of statements after every Jimple line matching the given regex.
	 * 
	 * @param body
	 * @param stmts
	 * @param regex
	 * @throws ParserException
	 * @throws LexerException
	 * @throws IOException
	 */
	public static void insertAfter(Body body, Chain<Unit> stmts, String regex) throws ParserException, LexerException, IOException {
		Pattern expr = Pattern.compile(regex);
		Iterator<Unit> i = body.getUnits().snapshotIterator();
		while (i.hasNext()) {
			Unit u = i.next();
			Matcher m=expr.matcher(u.toString());
			if (m.matches()) {
				body.getUnits().insertAfter(stmts, u);
			}
		}
		
	}

	/**
	 * Inserts statements at the end of a method, but ensures that thrown
	 * exceptions are still handled properly.
	 * 
	 * TODO UNTESTED
	 * 
	 * @param body
	 * @param stmts
	 */
	public static void insertAtEndOfMethod(Body body, Chain<Unit> stmts) {
		if (Main.DEBUG) {
			System.out.println("Inserting at end of " + body.getMethod().getName());
		}
		body.getUnits().insertAfter(stmts, body.getUnits().getLast());
	}

	public static Local generateNewLocal(Body body, Type type) {
		LocalGenerator lg = new LocalGenerator(body);
		return lg.generateLocal(type);
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


	public boolean isMainActivity(String cls) {
		return mainActivities.contains(cls);
	}

	/**
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
			Dependencies dep = jcs.resolve(sc);

			return sc;
		} catch (Throwable t) {
			t.printStackTrace();
		}
		return null;
	}

	/**
	 * Adds a file into an existing zip.
	 * 
	 * @param zipFile
	 * @param fileName
	 * @param fileContents
	 * @return
	 * @throws IOException
	 */
	private ZipFile addFileToExistingZip(File zipFile, String fileName, String fileContents) throws IOException {
		// get a temp file
		File tempFile = new File(zipFile.getName() + "_modified.apk");
		tempFile.createNewFile();

		byte[] buf = new byte[4096 * 1024];

		ZipInputStream zin = new ZipInputStream(new FileInputStream(zipFile));
		ZipOutputStream out = new ZipOutputStream(new FileOutputStream(tempFile));

		ZipEntry entry = zin.getNextEntry();
		while (entry != null) {
			String name = entry.getName();
			boolean toBeDeleted = false;
			if (fileName.indexOf(name) != -1) {
				toBeDeleted = true;
			}
			if (!toBeDeleted) {
				// Add ZIP entry to output stream.
				out.putNextEntry(new ZipEntry(name));
				// Transfer bytes from the ZIP file to the output file
				int len;
				while ((len = zin.read(buf)) > 0) {
					out.write(buf, 0, len);
				}
			}
			entry = zin.getNextEntry();
		}
		// Close the streams
		zin.close();
		// Add ZIP entry to output stream.
		out.putNextEntry(new ZipEntry(fileName));
		// Transfer bytes from the file to the ZIP file
		out.write(fileContents.getBytes());
		// Complete the entry
		out.closeEntry();
		// Complete the ZIP file
		out.close();
		// tempFile.delete();

		return new ZipFile(tempFile);
	}
	


	/**
	 * Replaces the AndroidManifest.xml in an Apk file by a custom version.
	 * 
	 * @param apkFile
	 * @param newManifest
	 * @throws ZipException
	 * @throws IOException
	 * @throws InterruptedException
	 * 
	 * @return modified APK
	 */  //TODO Param apkFile not needed anymore
	public File replaceManifest(File apkFile, String newManifest) throws ZipException, IOException,
			InterruptedException {
		// unpack
		if (Main.DEBUG) {
			System.out.println("Decoding " + apkFile.getAbsolutePath());
		}
		Process p = Runtime.getRuntime().exec(
				"java -jar libs/apktool.jar d -s -f " + apkFile.getAbsolutePath() + " decoded");
		int processExitCode = p.waitFor();
		if (processExitCode != 0) {
			System.out.println("Something went wrong when unpacking " + apkFile.getAbsolutePath());
			return null;
		}

		// Replace content of AndroidManifest.xml
		FileOutputStream fos = new FileOutputStream("decoded/AndroidManifest.xml");
		fos.write(newManifest.getBytes());
		fos.close();

		// Now pack again
		File newFile = new File(apkFile.getAbsolutePath().replace(".apk", "") + "_modified.apk");
		p = Runtime.getRuntime().exec("java -jar libs/apktool.jar b decoded/ " + newFile.getAbsolutePath());
		processExitCode = p.waitFor();
		if (processExitCode != 0) {
			System.err.println("Something went wrong when packing " + apkFile.getAbsolutePath());
			return null;
		}

		if (CLEAR_AFTER_USER) {
			File decoded = new File("decoded");
			if (decoded.isDirectory())
				decoded.delete();
			else
				System.err.println("Unexpected: decoded is not a directory");
		}
		return newFile;
	}

	/**
	 * Returns the manifest data as a String.
	 * 
	 * 
	 * @return
	 * @throws ZipException
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public String getManifest() throws ZipException, IOException, InterruptedException {
		return manifest;
	}
	
	/**
	 * Replaces substrings in the manifest file and creates a new APK containing the modified AndroidManifest file.
	 * 
	 * Use this method as a convenience method for {@link InstrumentationHelper.replaceManifest}.
	 * 
	 * Note that the new APK file is not zipaligned or jarsigned.
	 * 
	 * @param apkFile
	 * @param replacementMap
	 * @return
	 * @throws ZipException
	 * @throws IOException
	 * @throws InterruptedException
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 * @throws XPathExpressionException
	 */
	public static File adaptManifest(String apkFile, Map<String, String> replacementMap) throws ZipException, IOException, InterruptedException, ParserConfigurationException, SAXException, XPathExpressionException {
		InstrumentationHelper h = new InstrumentationHelper(new File(apkFile));
		String manifestString = h.getManifest();
		for (String key:replacementMap.keySet()) {
			assert replacementMap.get(key)!=null;
			manifestString = manifestString.replaceFirst(key, replacementMap.get(key));		
		}
		File newFile = h.replaceManifest(new File(apkFile), manifestString);
		
		if (Main.DEBUG) {
			System.out.println("New manifest: \n" + manifestString);
		}
		return newFile;
		
	}

	/**
	 * Writes a class to a jimple file.
	 * 
	 * @param sClass
	 * @throws IOException
	 */
	public void writeClassToFile(SootClass sClass) throws IOException {
		String fileName = SourceLocator.v().getFileNameFor(sClass, Options.output_format_jimple);
		if (Main.DEBUG) {
			System.out.println("Dumping class to " + fileName);
		}
		OutputStream streamOut = new FileOutputStream(fileName);
		PrintWriter writerOut = new PrintWriter(new OutputStreamWriter(streamOut));
		Printer.v().printTo(sClass, writerOut);
		// JasminClass jasminClass = new soot.jimple.JasminClass(sClass);
		// jasminClass.print(writerOut);
		writerOut.flush();
		streamOut.close();
	}

	/**
	 * Before invokations of a method, wrap its parameter by a method call to
	 * another method.
	 * 
	 * Note: TODO Not finished yet.
	 * 
	 * @param body
	 * @param methSig
	 *            The Soot signature of the method call to apply. For example:
	 *            <code><de.fhg.aisec.gudjcetest.Utility: char[] wrapChar(char[])></code>
	 */
	public static void applyFunctionToMethodParams(Body body, String methSig) {
		Iterator<Unit> i = body.getUnits().snapshotIterator();
		while (i.hasNext()) {
			Unit u = i.next();

			if (u instanceof JInvokeStmt) {
				InvokeExpr invok = ((JInvokeStmt) u).getInvokeExpr();
				// TODO Considers only the first parameter
				if (invok.getArgCount() > 0 && invok.getArg(0).getType() instanceof RefLikeType) {

					// Get argument to wrap
					System.out
							.println(invok.getArg(0).toString() + " Has type " + invok.getArg(0).getType().toString());

					// TODO condition should be configurable
					if (invok.getArg(0).getType().toString().equals("char[]")) {

						// // tmp = new char[]
						Local lCharA = generateNewLocal(body, ArrayType.v(CharType.v(), 1));

						// arg = util.wrapChar(tmp)
						Unit tmpU = Jimple.v()
								.newAssignStmt(
										lCharA,
										Jimple.v().newStaticInvokeExpr(Scene.v().getMethod(methSig).makeRef(),
												invok.getArg(0)));
						body.getUnits().insertBefore(tmpU, u);

						// now use (wrapped) arg instead of original
						((JInvokeStmt) u).getInvokeExpr().setArg(0, lCharA);
					}
				}
			}
		}
	}

	/**
	 * Replaces all method invocations of class <code>clazz</code> with those of
	 * class <code>child</code>.
	 * 
	 * Make sure that clazz is a superclass of child and that both are in Soot's
	 * classpath.
	 * 
	 * @param body
	 * @param clazz
	 * @param child
	 */
	public static void replaceClassByChild(Body body, String clazz, String child) {
		if (Main.DEBUG) {
			System.out.println("Replacing class " + clazz + " by child " + child);
		}
		SootClass parent = Scene.v().getSootClass(clazz);
		SootClass newClass = Scene.v().getSootClass(child);

		if (newClass.isAbstract())
			System.err.println("WARN: Replacement class" + child + " is abstract");

		HashMap<String, Local> localsReplace = new HashMap<String, Local>();

		// Replace local variables of type clazz with with child type
		Iterator<Local> locals = body.getLocals().iterator();
		while (locals.hasNext()) {
			Local l = locals.next();
			if (l.getType().toString().equals(clazz)) {
				String newName = "replaced_" + l.getName();
				l.setName(newName);
				l.setType(RefType.v(child));
				localsReplace.put(l.getName(), l);
			}
		}

		// Replace instantiations
		Iterator<Unit> i = body.getUnits().snapshotIterator();
		while (i.hasNext()) {
			Unit u = i.next();

			// Iterate over all units in the method
			if (u.toString().contains(clazz)) {

				// Change X = new clazz() to replaced_X = new child()
				if (u instanceof JAssignStmt) {
					Value right = ((JAssignStmt) u).getRightOp();
					if (right instanceof JNewExpr) {
						Value left = ((JAssignStmt) u).getLeftOp();
						Local lLocal = null;
						if (left instanceof JimpleLocal) {
							lLocal = localsReplace.get(((JimpleLocal) left).getName());
						} else {
							System.err.println("WARN: NOT IMPLEMENTED " + left.toString() + " is used as a " + clazz
									+ " but is not a local register");
						}
						if (lLocal == null)
							System.err.println("WARN: No local variable found for " + left.toString());
						Unit generated = Jimple.v().newAssignStmt(lLocal, Jimple.v().newNewExpr(RefType.v(child)));
						body.getUnits().insertAfter(generated, u);
						body.getUnits().remove(u);
					}

				} else if (u instanceof JInvokeStmt) {
					// Change all method invocations from clazz.<init> to
					// child.<init>
					InvokeExpr expr = ((JInvokeStmt) u).getInvokeExpr();
					if (expr instanceof JSpecialInvokeExpr) {
						String newMethSig = ((JSpecialInvokeExpr) expr).getMethodRef().getSignature()
								.replace(clazz, child);
						SootMethod newMeth = Scene.v().getMethod(newMethSig);
						Local base = localsReplace.get(((JSpecialInvokeExpr) expr).getBase().toString());
						if (base == null)
							System.err.println("WARN: null base");

						Unit generated = Jimple.v().newInvokeStmt(
								Jimple.v().newSpecialInvokeExpr(base, newMeth.makeRef(), expr.getArgs()));
						body.getUnits().insertAfter(generated, u);
						body.getUnits().remove(u);
					} else {
						System.err.println("WARN: NOT IMPLEMENTED: " + expr);
					}
				}
			}
		}

	}

}
