/* 
 * Copyright (c) 2014, Julian Schuette, Rafael Fedler, Fraunhofer AISEC
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import soot.ArrayType;
import soot.Body;
import soot.BooleanType;
import soot.ByteType;
import soot.CharType;
import soot.DoubleType;
import soot.FloatType;
import soot.IntType;
import soot.JimpleClassSource;
import soot.Local;
import soot.LongType;
import soot.MethodOrMethodContext;
import soot.Modifier;
import soot.PatchingChain;
import soot.Printer;
import soot.RefLikeType;
import soot.RefType;
import soot.Scene;
import soot.ShortType;
import soot.SootClass;
import soot.SootField;
import soot.SootMethod;
import soot.SootMethodRef;
import soot.SourceLocator;
import soot.Type;
import soot.Unit;
import soot.UnknownType;
import soot.Value;
import soot.VoidType;
import soot.javaToJimple.IInitialResolver.Dependencies;
import soot.javaToJimple.LocalGenerator;
import soot.jimple.ArrayRef;
import soot.jimple.AssignStmt;
import soot.jimple.ClassConstant;
import soot.jimple.FieldRef;
import soot.jimple.IntConstant;
import soot.jimple.InvokeExpr;
import soot.jimple.InvokeStmt;
import soot.jimple.Jimple;
import soot.jimple.JimpleBody;
import soot.jimple.ReturnStmt;
import soot.jimple.ReturnVoidStmt;
import soot.jimple.SpecialInvokeExpr;
import soot.jimple.StringConstant;
import soot.jimple.VirtualInvokeExpr;
import soot.jimple.infoflow.entryPointCreators.CAndroidEntryPointCreator;
import soot.jimple.internal.JAssignStmt;
import soot.jimple.internal.JIdentityStmt;
import soot.jimple.internal.JInvokeStmt;
import soot.jimple.internal.JNewExpr;
import soot.jimple.internal.JSpecialInvokeExpr;
import soot.jimple.internal.JimpleLocal;
import soot.jimple.parser.lexer.LexerException;
import soot.jimple.parser.parser.ParserException;
import soot.jimple.toolkits.callgraph.Edge;
import soot.options.Options;
import soot.tagkit.DoubleConstantValueTag;
import soot.tagkit.FloatConstantValueTag;
import soot.tagkit.IntegerConstantValueTag;
import soot.tagkit.LongConstantValueTag;
import soot.tagkit.StringConstantValueTag;
import soot.util.Chain;
import soot.util.HashChain;

/**
 * Helper class for instrumenting bytecode artifacts.
 * 
 * @author Julian Schuette (julian.schuette@aisec.fraunhofer.de)
 * 
 */
public class InstrumentationHelper {
	public static Logger log = LoggerFactory.getLogger(InstrumentationHelper.class);
	
	/**
	 * All listener classes defined inside Android.view. We need those to
	 * determine which fields of activity classes are listeners to invoke them
	 * during instrumentation.
	 */
	private static HashMap<String, String[]> uiListeners = new HashMap<String, String[]>();
	static {
		HashMap<String, String[]> aMap = new HashMap<String, String[]>();
		aMap.put("android.view.ActionProvider$VisibilityListener", new String[] {"void onActionProviderVisibilityChanged(boolean)"});
		aMap.put("android.view.GestureDetector$OnDoubleTapListener", new String[] {"boolean onDoubleTap(android.view.MotionEvent)", "boolean onDoubleTapEvent(android.view.MotionEvent)", "boolean onSingleTapConfirmed(android.view.MotionEvent)" });
		aMap.put("android.view.GestureDetector$OnGestureListener", new String[]{"boolean onDown(MotionEvent)", "boolean onFling(android.view.MotionEvent, android.view.MotionEvent, float, float)", "void onLongPress(android.view.MotionEvent)", "boolean onScroll(android.view.MotionEvent, android.view.MotionEvent, float, float)", "void onShowPress(android.view.MotionEvent)", "boolean onSingleTapUp(android.view.MotionEvent)"});
		aMap.put("android.view.MenuItem$OnActionExpandListener", new String[]{"boolean onMenuItemActionCollapse(android.view.MenuItem)", "boolean onMenuItemActionExpand(android.view.MenuItem)"}) ;
		aMap.put("android.view.MenuItem$OnMenuItemClickListener", new String[]{"boolean onMenuItemClick(android.view.MenuItem)"});
		aMap.put("android.view.ScaleGestureDetector$OnScaleGestureListener", new String[]{"boolean onScale(android.view.ScaleGestureDetector)", "boolean onScaleBegin(android.view.ScaleGestureDetector)", "void onScaleEnd(android.view.ScaleGestureDetector)"});
		aMap.put("android.view.TextureView$SurfaceTextureListener", new String[]{"void onSurfaceTextureAvailable(android.graphics.SurfaceTexture, int, int)", "boolean onSurfaceTextureDestroyed(android.graphics.SurfaceTexture)", "void onSurfaceTextureSizeChanged(android.graphics.SurfaceTexture, int, int)", "void onSurfaceTextureUpdated(android.graphics.SurfaceTexture)"});
		aMap.put("android.view.View$OnAttachStateChangeListener", new String[]{"void onViewAttachedToWindow(android.view.View)", "void onViewDetachedFromWindow(android.view.View)"});
		aMap.put("android.view.View$OnClickListener", new String[]{"void onClick(android.view.View)"});
		aMap.put("android.view.View$OnCreateContextMenuListener", new String[]{"void onCreateContextMenu(android.view.ContextMenu, android.view.View, android.view.ContextMenu.ContextMenuInfo)"});
		aMap.put("android.view.View$OnDragListener", new String[]{"boolean onDrag(android.view.View, android.view.DragEvent)"});
		aMap.put("android.view.View$OnFocusChangeListener", new String[]{"void onFocusChange(android.view.View, boolean)"});
		aMap.put("android.view.View$OnGenericMotionListener", new String[]{"boolean onGenericMotion(android.view.View, MotionEvent)"});
		aMap.put("android.view.View$OnHoverListener", new String[]{"boolean onHover(android.view.View, MotionEvent)"});
		aMap.put("android.view.View$OnKeyListener", new String[]{"boolean onKey(android.view.View, int, KeyEvent)"});
		aMap.put("android.view.View$OnLayoutChangeListener", new String[]{"void onLayoutChange(android.view.View, int, int, int, int, int, int, int, int)"});
		aMap.put("android.view.View$OnLongClickListener", new String[]{"boolean onLongClick(android.view.View)"});
		aMap.put("android.view.View$OnSystemUiVisibilityChangeListener", new String[]{"void onSystemUiVisibilityChange(int)"});
		aMap.put("android.view.View$OnTouchListener", new String[]{"boolean onTouch(android.view.View, android.view.MotionEvent)"});
		aMap.put("android.view.ViewGroup$OnHierarchyChangeListener", new String[]{"void onChildViewAdded(android.view.View, android.view.View)", "void onChildViewRemoved(android.view.View, android.view.View)"});
		aMap.put("android.view.ViewStub$OnInflateListener", new String[]{"void onInflate(android.view.ViewStub, android.view.View)"});
		aMap.put("android.view.ViewTreeObserver$OnDrawListener", new String[]{"void onDraw()"});
		aMap.put("android.view.ViewTreeObserver$OnGlobalFocusChangeListener", new String[]{"void onGlobalFocusChanged(android.view.View, android.view.View)"});
		aMap.put("android.view.ViewTreeObserver$OnGlobalLayoutListener", new String[]{"void onGlobalLayout()"});
		aMap.put("android.view.ViewTreeObserver$OnPreDrawListener", new String[]{"boolean onPreDraw()"}); //TODO: do we really need ViewTreeObserver?
		aMap.put("android.view.ViewTreeObserver$OnScrollChangedListener", new String[]{"void onScrollChanged()"});
		aMap.put("android.view.ViewTreeObserver$OnTouchModeChangeListener", new String[]{"void onTouchModeChanged(boolean)"});
		aMap.put("android.view.ViewTreeObserver$OnWindowAttachListener", new String[]{"void onWindowAttached()", "void onWindowDetached()"});
		aMap.put("android.view.ViewTreeObserver$OnWindowFocusChangeListener", new String[]{"void onWindowFocusChanged(boolean)"});
		uiListeners = aMap;
	};

	/**
	 * Clear up stuff after use
	 */
	private static final boolean CLEAR_AFTER_USER = true;
	private String manifest;
	private String packagename;
	private static LinkedHashSet<String> mainActivities = new LinkedHashSet<String>();

	private InstrumentationHelper() { }

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
			log.info("Decoding " + apkFile.getAbsolutePath());
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
        packagename = ((Attr) nodes.item(0)).getValue();

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
	 * Returns the package name of the app.
	 * @return
	 */
	public String getPackagename() {
		return packagename;
	}
	
	/**
	 * Returns a LinkedHashSet of main activities.
	 * 
	 * Usually, there will be only one main Activity, but Android still allows to register several activities as the default one.
	 * @return
	 */
	public Iterable<String> getDefaultActivities() {
		return mainActivities;
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
			log.info("Inserting at begin of " + body.getMethod().getName());
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
	 * Returns a (filtered) list of entrypoint methods.
	 * 
	 * @param filter
	 * @return
	 */
	public HashSet<SootMethod> getEntryMethods(Pattern filter)  {
		HashSet<SootMethod> result = new HashSet<SootMethod>();
		List<SootMethod> eps = Scene.v().getEntryPoints();
		if (eps.size()==1 && eps.get(0).getSignature().equals("<dummyMainClass: void dummyMainMethod()>")) {
			eps = MethodUtils.getCalleesOf(eps.get(0));
		}
		for (SootMethod m:eps) {			
			if (filter==null || filter.matcher(m.getSignature()).matches()) {
				result.add(m);
			}
		}
		return result;
	}
	
	/**
	 * Please not that this method assumes the decoded APK to be in the
	 * directory "decoded".
	 * @return
	 * @throws ParserConfigurationException 
	 * @throws IOException 
	 * @throws SAXException 
	 * @throws XPathExpressionException 
	 */
	public HashSet<SootMethod> getOnClickFromLayouts(Pattern filter) throws ParserConfigurationException, SAXException, IOException, XPathExpressionException{
		HashSet<SootMethod> result = new HashSet<SootMethod>();
		
		/*
		 * Get layout names (activity_main) and the referencing activities (x.y.z.MainActivity)
		 */
		HashMap<String, List<SootClass>> layoutsToActivity = getLayoutToActivityAssociation();

		/* 
		 * Now find all layout files that are referenced in these public files.
		 * Make sure no duplicates are gathered by using a HashSet.
		 */
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		
		XPath xpath = XPathFactory.newInstance().newXPath();	
		
		/*
		 * We now have all layout files of the app. Now find all
		 * android:onClick elements inside them.
		 */
		XPathExpression onClickExpr = xpath.compile("//RelativeLayout/Button[@onClick]/@onClick");
		
		HashSet<SootMethod> onClickListeners = new HashSet<SootMethod>();
		for (String layoutFileName : layoutsToActivity.keySet()){
			log.debug("Checking layout file " + layoutFileName);
			
			File layoutFile = new File("decoded/res/layout/" + layoutFileName + ".xml");
			Document doc = dBuilder.parse(layoutFile);
			
			NodeList onClickNodes = (NodeList) onClickExpr.evaluate(doc, XPathConstants.NODESET);
						
			for (int i = 0; i < onClickNodes.getLength(); i++){
				String onClickMethod = "void " + ((Attr) onClickNodes.item(i)).getValue() + "(android.view.View)";
				String className = layoutsToActivity.get(layoutFileName).get(0).getName(); 
				SootMethod oncmeth = Scene.v().getMethod("<"+className+": " + onClickMethod + ">");
				if (filter.matcher(oncmeth.getSignature()).matches()) {
					onClickListeners.add(oncmeth);
					log.debug("XML-defined onclick handler: " + oncmeth.getSignature());
				} else {
					log.debug("Does not match filter: " + oncmeth.getSignature() + " :: " + filter.pattern());
				}
			}
		}
		result = onClickListeners;
		
		log.debug("Found " + result.size() + " android:onClickListeners in layout XML file.");
		
		return result;
	}
	
	/**
	 * Returns a mapping from layout strings ("activity_main") to SootClasses using the respective layout.
	 * @return
	 * @throws SAXException
	 * @throws IOException
	 * @throws XPathExpressionException
	 * @throws ParserConfigurationException
	 */
	public HashMap<String, List<SootClass>> getLayoutToActivityAssociation() throws SAXException, IOException, XPathExpressionException, ParserConfigurationException {
		HashMap<String, List<SootClass>> layoutNameToClass = new HashMap<String, List<SootClass>>();

		HashMap<SootClass, List<String>> classesToLayoutIDs= new HashMap<SootClass, List<String>>();
		
		//Collect all layout ids which are used in setContentView
		SootMethod setContentView = Scene.v().getMethod("<android.app.Activity: void setContentView(int)>");
		Iterator<Edge> callers = Scene.v().getCallGraph().edgesInto(setContentView);
		log.debug("Size of cg is " + Scene.v().getCallGraph().size());
		while (callers.hasNext()) {
			Edge e = callers.next();
			MethodOrMethodContext caller = e.getSrc();
			Body callerBody = caller.method().getActiveBody();
			PatchingChain<Unit> callerUnits = callerBody.getUnits();
			for (Unit u:callerUnits) {
				if (u instanceof JInvokeStmt) {
					if (((JInvokeStmt) u).getInvokeExpr().getMethod().equals(setContentView)) {
						Value arg = ((JInvokeStmt) u).getInvokeExpr().getArg(0);
						if (arg instanceof IntConstant) {
							int id = ((IntConstant) arg).value;
							if (!classesToLayoutIDs.containsKey(caller.method().getDeclaringClass())) {
								classesToLayoutIDs.put(caller.method().getDeclaringClass(), new ArrayList<String>());
							}
							classesToLayoutIDs.get(caller.method().getDeclaringClass()).add("0x"+Integer.toHexString(id));
						} else {
							log.error("Sorry, could not resolve references layout: " + u.toString());
						}
					}
				}
			}
		}
		
		//Resolve layout ids to file names
		HashMap<String, String> idsToLayoutNames = new HashMap<String, String>();	//Hex ids to names
		File folder = new File("decoded/res");
		File[] listOfFiles = folder.listFiles();
		ArrayList<File> valueFolders = new ArrayList<File>();

		for (int i = 0; i < listOfFiles.length; i++) {
			if (listOfFiles[i].isDirectory() && listOfFiles[i].getName().startsWith("values"))
				valueFolders.add(listOfFiles[i]);
		}
		
		//now collect all "public.xml" files:
		ArrayList<File> publicFiles = new ArrayList<File>();
		for (File f : valueFolders){
			listOfFiles = f.listFiles();
			for (File ff : listOfFiles){
				if (ff.isFile() && ff.getName().equals("public.xml"))
					publicFiles.add(ff);
			}
		}
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();

		//Read layout ids and names from public xml
		for (File pf : publicFiles){
			Document doc = dBuilder.parse(pf);
			doc.getDocumentElement().normalize();
			
			XPath xpath = XPathFactory.newInstance().newXPath();
	        XPathExpression expr1 = xpath.compile("//*/@id|//*/@name");
	        NodeList nodes = (NodeList)expr1.evaluate(doc, XPathConstants.NODESET);
	        
	        String id=null, name=null;
	        for (int i=0;i<nodes.getLength();i++) {
	        	if (((Attr) nodes.item(i)).getName().equals("id")) {
	        		id = ((Attr) nodes.item(i)).getValue();
	        	}
	        	if (((Attr) nodes.item(i)).getName().equals("name")) {
	        		name= ((Attr) nodes.item(i)).getValue();
	        	}
	        	if (id!=null && name!=null) {
	        		idsToLayoutNames.put(id,name);
	        		id = null;
	        		name = null;
	        	}
	        }
		}
		
		
		//Now map layout to activities
		for (String layoutId:idsToLayoutNames.keySet()) {
			for (SootClass c:classesToLayoutIDs.keySet()) {
				for (String id:classesToLayoutIDs.get(c)) {
					if (id.equals(layoutId)) {
						if (!layoutNameToClass.containsKey(idsToLayoutNames.get(layoutId))) {
							layoutNameToClass.put(idsToLayoutNames.get(layoutId), new ArrayList<SootClass>());
						}
						layoutNameToClass.get(idsToLayoutNames.get(layoutId)).add(c);
					}
				}
			}
		}

		return layoutNameToClass;
	}

	public HashMap<Integer,SootClass> getClassOfViewId() throws SAXException, IOException, XPathExpressionException, ParserConfigurationException {
		HashMap<Integer, SootClass> layoutIdToClass = new HashMap<Integer, SootClass>();		
		HashMap<String, String> layoutNamesToClass= new HashMap<String, String>();
		
		//Resolve layout ids to file names
		HashMap<String,Integer> layoutNamesToIds = new HashMap<String,Integer>();	//Hex ids to names
		File folder = new File("decoded/res");
		File[] listOfFiles = folder.listFiles();
		ArrayList<File> valueFolders = new ArrayList<File>();

		for (int i = 0; i < listOfFiles.length; i++) {
			if (listOfFiles[i].isDirectory() && listOfFiles[i].getName().startsWith("values"))
				valueFolders.add(listOfFiles[i]);
		}
		
		//now collect all "public.xml" files:
		ArrayList<File> publicFiles = new ArrayList<File>();
		for (File f : valueFolders){
			listOfFiles = f.listFiles();
			for (File ff : listOfFiles){
				if (ff.isFile() && ff.getName().equals("public.xml"))
					publicFiles.add(ff);
			}
		}
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();

		//Read layout ids and names from public xml
		for (File pf : publicFiles){
			Document doc = dBuilder.parse(pf);
			doc.getDocumentElement().normalize();
			
			XPath xpath = XPathFactory.newInstance().newXPath();
	        XPathExpression expr1 = xpath.compile("//*/@id|//*/@name");
	        NodeList nodes = (NodeList)expr1.evaluate(doc, XPathConstants.NODESET);
	        
	        String name=null;
	        int id=-1;
	        for (int i=0;i<nodes.getLength();i++) {
	        	if (((Attr) nodes.item(i)).getName().equals("id")) {
	        		id = Integer.parseInt(((Attr) nodes.item(i)).getValue().replace("0x", ""), 16);
	        	}
	        	if (((Attr) nodes.item(i)).getName().equals("name")) {
	        		name= ((Attr) nodes.item(i)).getValue();
	        	}
	        	if (name!=null && id!=-1) {
	        		layoutNamesToIds.put(name, new Integer(id));
	        		name = null;
	        	}
	        }
		}
		
		
		//Resolve layout ids to file names
		folder = new File("decoded/res/layout");
		listOfFiles = folder.listFiles();
		dbFactory = DocumentBuilderFactory.newInstance();
		dBuilder = dbFactory.newDocumentBuilder();

		//Read layout ids and names from public xml
		for (File pf : listOfFiles){
			Document doc = dBuilder.parse(pf);
			doc.getDocumentElement().normalize();
			
			XPath xpath = XPathFactory.newInstance().newXPath();
	        XPathExpression expr1 = xpath.compile("//*[@id]");
	        NodeList nodes = (NodeList)expr1.evaluate(doc, XPathConstants.NODESET);
	        
	        String name=null;
	        for (int i=0;i<nodes.getLength();i++) {
	        	Node node = nodes.item(i);
	        	Attr attr = (Attr) node.getAttributes().getNamedItem("android:id");
	        	String layoutName = attr.getValue().replace("@id/", "");
	        	System.out.println(layoutName);
	        	if (layoutNamesToIds.keySet().contains(layoutName)) {
	        		name = node.getNodeName();
	        		int id = layoutNamesToIds.get(layoutName);
	        		if (Scene.v().containsClass(name)) {
	        			SootClass clazz = Scene.v().getSootClass(name);
	        			layoutIdToClass.put(new Integer(id), clazz);
	        		}
	        		layoutNamesToClass.put(layoutName,name);
	        		name = null;
	        	}
	        }
		}
		
		return layoutIdToClass;
	}
	
	public SootMethod getDefaultOnResume() {
		assert mainActivities.size()>0:"No default activities in AndroidManifest.xml";
			
		SootClass mainAct = Scene.v().getSootClass(mainActivities.iterator().next());
		SootMethod onResume;
		try {
			onResume = mainAct.getMethod("void onResume()");
		} catch (RuntimeException rte) {
			onResume=null;
		}
		return onResume;
	}
	
	public SootMethod getDefaultOnCreate() {
		SootMethod defaultOnCreate = null;		
		for (String mainAct:mainActivities) {
			if (Scene.v().containsClass(mainAct)) {
				SootClass mainClass = Scene.v().getSootClass(mainAct);
				if (mainClass.declaresMethod("void onCreate(android.os.Bundle)")) {
					return mainClass.getMethod("void onCreate(android.os.Bundle)");
				}
			} else {
				throw new RuntimeException("Unexpected: Main activity class not present in Scene: " + mainAct);
			}
		}
		
		return defaultOnCreate;
	}
	
	/**
	 * Returns a list of units representing all return stmts in a method body.
	 * 
	 * @param b
	 * @return
	 */
	private List<Unit> getReturnUnits(Body b) {
		List<Unit> returns = new ArrayList<Unit>();
		for (Unit u:b.getUnits()) {
			if (u instanceof ReturnStmt || u instanceof ReturnVoidStmt) {
				returns.add(u);
			}
		}
		return returns;
	}
	
	/**
	 * Returns chain of units which should be inserted at the according position.
	 * These units take care of field initialization and a good place to put them
	 * is a constructor.
	 * 
	 * Side effect: for a method call, ONE local will be injected by us and one
	 * by GenerateMethodCall into the SootMethod sm's locals. All other locals
	 * will be inserted into the return pair.
	 * @param field
	 * @param sm
	 * @return
	 */
	public static Pair<PatchingChain<Unit>, Chain<Local>> generateUnitsToInitializeFieldIfNotInitialized(SootField field, SootMethod sm){
		Type type = field.getType();
		PatchingChain<Unit> newUnits = new PatchingChain<Unit>(new HashChain<Unit>());
		HashChain<Local> newLocals = new HashChain<Local>();
		Local thisLocal = sm.getActiveBody().getThisLocal();
		Body body = sm.getActiveBody(); 
		
		//check all units in method to see if field is written to. if so, return.
		for (Unit u : sm.getActiveBody().getUnits()){
			if (u instanceof AssignStmt)
				if (((AssignStmt) u).getLeftOp() instanceof FieldRef)
					if (( (FieldRef) ((AssignStmt) u).getLeftOp()).getField().getSignature().equals(field.getSignature()))
						return new ImmutablePair<PatchingChain<Unit>, Chain<Local>>(newUnits, newLocals);
		}
		
		if (type instanceof soot.IntType || type instanceof soot.ByteType || type instanceof ShortType || type instanceof BooleanType || type instanceof soot.CharType) {
			field.addTag(new IntegerConstantValueTag(1));
			newUnits.addLast(Jimple.v().newAssignStmt(
					Jimple.v().newInstanceFieldRef(thisLocal, field.makeRef()),
					soot.jimple.IntConstant.v(1)));        	
		}
		else if (type instanceof soot.DoubleType) {
			field.addTag(new DoubleConstantValueTag(3.14));
			newUnits.addLast(Jimple.v().newAssignStmt(
					Jimple.v().newInstanceFieldRef(thisLocal, field.makeRef()),
					soot.jimple.DoubleConstant.v(3.14d))); 
		}
		else if (type instanceof soot.FloatType) {
			field.addTag(new FloatConstantValueTag(3.14f));
			newUnits.addLast(Jimple.v().newAssignStmt(
					Jimple.v().newInstanceFieldRef(thisLocal, field.makeRef()),
					soot.jimple.FloatConstant.v(3.14f))); 
		}
		else if (type instanceof soot.LongType) {
			field.addTag(new LongConstantValueTag(1));
			newUnits.addLast(Jimple.v().newAssignStmt(
					Jimple.v().newInstanceFieldRef(thisLocal, field.makeRef()),
					soot.jimple.LongConstant.v(1))); 
		}
        else if (type instanceof soot.RefType) {
        	RefType fieldType = (RefType) type;
            SootClass fieldClass = fieldType.getSootClass();
            String className = fieldClass.getName();
        	if (fieldClass.getName().equals("java.lang.String")){
        		field.addTag(new StringConstantValueTag("Lorem ipsum"));
        		newUnits.addLast(Jimple.v().newAssignStmt(
    					Jimple.v().newInstanceFieldRef(thisLocal, field.makeRef()),
    					soot.jimple.StringConstant.v("Lorem ipsum"))); 
        	} else {
        		SootMethod primitiveCtor = getPrimitiveConstructor(className);
        		
        		if (primitiveCtor == null){
        			//we need to make the private ctor w/o any arguments public
        			Pair<PatchingChain<Unit>, Chain<Local>> tPair = makePrivateCtorPublic(className, sm);
        			//insert units and locals:
        			newUnits.insertAfter(tPair.getLeft(), newUnits.getLast());
        			newLocals.addAll(tPair.getRight());
        			
        			//now reassign the primitiveCtor variable to the formerly private ctor:
        			primitiveCtor = Scene.v().getSootClass(className).getMethod("<init>()");
        		}
        		
        		CAndroidEntryPointCreator aep = new CAndroidEntryPointCreator(new ArrayList<String>());
        		Local tmp = generateLocalAndDontInsert(body, type);
        		//need to insert tmp now so method call can be built:
        		body.getLocals().add(tmp);
        		newUnits.addLast(aep.buildMethodCall(primitiveCtor, body, tmp, new LocalGenerator(body), newUnits.getLast()));
        		newUnits.addLast(Jimple.v().newAssignStmt(
    					Jimple.v().newInstanceFieldRef(thisLocal, field.makeRef()),
    					tmp)); 
        		
        	}
        	
        }
		return new ImmutablePair<PatchingChain<Unit>, Chain<Local>>(newUnits, newLocals);
	}
	
	/*
	 * constructs an instance of an object which fulfills all conditions downstream
	 * of this statement
	 * 
	 * After calling this function, you should also call
	 * generateUnitsToInitializeFieldIfNotInitialized and insert its units and locals
	 * into the primitive ctor which 
	 */
	public static Pair<PatchingChain<Unit>, Chain<Local>> generateObjectInstance(Local local, Unit unit, SootMethod meth){	
		// J: To support testing, return unit chain of stmts here instead of injecting them immediately before unit.
		// RF: problem is we need locals in the body which need to be inserted anyway
		assert local.getType() instanceof RefType:"No object generation for primitive types.";
		
		PatchingChain<Unit> newUnits = new PatchingChain<Unit>(new HashChain<Unit>());
		HashChain<Local> newLocals = new HashChain<Local>();
		
		String className = ((RefType) local.getType()).getClassName();
		
		//check if we can construct this object from primitive types:
		SootMethod primitiveCtor = getPrimitiveConstructor(className);
		boolean noPrimitiveCtorFound = (primitiveCtor == null);
		
		Body body = meth.getActiveBody();
		
		SootMethod prototypeCtor = null;
		if (primitiveCtor == null){
			//we need to make the private ctor w/o any arguments public
			Pair<PatchingChain<Unit>, Chain<Local>> tPair = makePrivateCtorPublic(className, meth);
			newUnits.addAll(tPair.getLeft());
			newLocals.addAll(tPair.getRight());
			
			//now reassign the primitiveCtor variable to the formerly private ctor:
			primitiveCtor = Scene.v().getSootClass(className).getMethod("<init>()");
			
			// will be needed later:
			for (Unit u : body.getUnits()){
				if (u instanceof InvokeStmt){
					if (((InvokeStmt) u).getInvokeExpr().getMethod().getSignature().contains("<init>(") )
						prototypeCtor = ((InvokeStmt) u).getInvokeExpr().getMethod();
				}
			}
		}
		
		// for correct program flow, we would need to fulfill certain explicit
		// and also implicit constrains on objects. However, as we cannot deduce
		// programmer intention from statements (e.g., a programmer might WANT
		// a null pointer exception to appear so he can carry out actions in
		// a catch-stmt or similarly stupid things), we pursue the following
		// approach for now:
		// 1. instantiate object in memory
		// -> primitive fields will be instantiated automatically no matter
		//    what was stored memory region before. Certain expected values
		//    for these fields will be inserted by Z3.
		// 2. Run code correctly BUT translate exceptions into path constraints
		//    and solve these using ConDroid/Z3.
		// As a consequence, we will construct each object 8and its fields)
		// such that they once fulfill implicit and/or explicit constraints,
		// and once such that they don't. This makes sure that the execution
		// path the programmer intended is followed at least once, no matter
		// whether an exception was his intention or not.
		//
		// our alternative, old and - for now - abandoned approach:
		/// 1. gather all conditions which have to be fulfilled by this object from here on
		///   - Get all uses of the object in downstream code (better: in next basic blocks, as two basic blocks may expect different object instances, even different types).
		///   - Check uses for references to methods (in invoke-stmts), static fields (in load/store-stmts) or instance fields (aka attributes).
		/// 
		/// 2. set all fields accordingly via sun.misc.unsafe JS <-- Why via unsafe? java.lang.reflect should do
		/// Add class of generated object instance to the modelled classes (as if it would have been stated in models.dat). This way, ConDroid will generate solutions for its concrete values.
		/// (Make sure that this instrumentation here is done BEFORE the remaining instrumentation with the solution injection
		//
		
		// now call ctor to construct object. ctor can now either be a valid
		// one or the formerly private one we made public:
		CAndroidEntryPointCreator aep = new CAndroidEntryPointCreator(new ArrayList<String>());
		AssignStmt assignStmt = (AssignStmt) aep.buildMethodCall(primitiveCtor, body, local, new LocalGenerator(body), unit);
		newUnits.addLast(assignStmt);
		
		//now add initializations to the ctor we just made public:
		for (SootField field : Scene.v().getSootClass(className).getFields()){
			Pair<PatchingChain<Unit>, Chain<Local>> tPair2 = generateUnitsToInitializeFieldIfNotInitialized(field, primitiveCtor);
			primitiveCtor.getActiveBody().getUnits().addAll(tPair2.getLeft());
			primitiveCtor.getActiveBody().getLocals().addAll(tPair2.getRight());
			
		}
		
		// if there was no public ctor to instantiate: 
		// VERY simple heuristic for getting closer to a well-formed program:
		// pick one public constructor and scan it for initializations of
		// primitive fields. It would be useful to do the same initializations
		// as the constructor that is called inside the method where we
		// instantiate the object -- if there is one. These initializations
		// then need to be carried out on our new object.
		/*
		if (noPrimitiveCtorFound){
			if (prototypeCtor == null) //if we didnt find one in the method before inserting our own:
				for (SootMethod methy :  Scene.v().getSootClass(className).getMethods()){
					if (meth.getSubSignature().startsWith("<init>(")){
						prototypeCtor = methy;
						break;
					}
				}
			
			//scan ctor for initializations with constants:
			for (Unit ctorUnit : prototypeCtor.getActiveBody().getUnits()){
				if (ctorUnit instanceof AssignStmt){
					if (((AssignStmt) ctorUnit).getLeftOp() instanceof FieldRef) //if we initialize a field... 
						if (( (FieldRef) ((AssignStmt) ctorUnit).getLeftOp()).getField().getDeclaringClass().getName().equals(className) && isPrimitive(( (FieldRef) ((AssignStmt) ctorUnit).getLeftOp()).getField().getType()) ){ // ... in this class which is primitive ...
							if (((AssignStmt) ctorUnit).getRightOp() instanceof Constant){ //... with a constant primitive
								
							} 
						}
				}
			}
		}*/ 
		return new ImmutablePair<PatchingChain<Unit>, Chain<Local>>(newUnits, newLocals);
		
	}
	
	public static Local generateLocalAndDontInsert(Body body, Type type){
		String namePrefix;
		if (type instanceof IntType)
			namePrefix = "$i";
		else if (type instanceof CharType)
			namePrefix = "$c";
		else if (type instanceof VoidType)
			namePrefix = "$v";
		else if (type instanceof ByteType)
			namePrefix = "$b";
		else if (type instanceof ShortType)
			namePrefix = "$s";
		else if (type instanceof BooleanType)
			namePrefix = "$b";
		else if (type instanceof DoubleType)
			namePrefix = "$d";
		else if (type instanceof FloatType)
			namePrefix = "$f";
		else if (type instanceof LongType)
			namePrefix = "$l";
		else if (type instanceof RefLikeType)
			namePrefix = "$r";
		else if (type instanceof UnknownType)
			namePrefix = "$u";
		else
			namePrefix = "$WTF";
		
		Chain<Local> existingLocals = body.getLocals();
		String name;
		while (true){
			boolean nameAlreadyTaken = false;
			name = namePrefix + RandomStringUtils.random(20);
			for (Local l : existingLocals){
				if (l.getName().equals(name)){
					nameAlreadyTaken = true;
				}
			}
			if (!nameAlreadyTaken)
				break;
		}
		//name now unique
				
		return soot.jimple.Jimple.v().newLocal(name, type);
	}
	
	/**
	 * This method inserts code before unit to make the private, no-argument
	 * constructor of a class publicly accessible.
	 * @param className
	 * @param unit
	 * @param meth
	 */
	public static Pair<PatchingChain<Unit>, Chain<Local>> makePrivateCtorPublic(String className, SootMethod meth){
		Body body = meth.getActiveBody();

		PatchingChain<Unit> resultUnits = new PatchingChain<Unit>(new HashChain<Unit>());
		HashChain<Local> resultLocals = new HashChain<Local>();
		
		/*
		 * Constructor<Foo> constructor= (Constructor<Foo>) Foo.class.getDeclaredConstructors()[0];
		 * constructor.setAccessible(true); 
		 */
		Local classObjToCallOn = generateLocalAndDontInsert(body, RefType.v("java.lang.Class"));
		resultLocals.add(classObjToCallOn);
		Local ctorLocal = generateLocalAndDontInsert(body, RefType.v("java.lang.reflect.Constructor"));
		resultLocals.add(ctorLocal);
		Local ctorArray = generateLocalAndDontInsert(body, RefType.v("java.lang.reflect.Constructor").makeArrayType());
		resultLocals.add(ctorArray);
		AssignStmt getClass = Jimple.v().newAssignStmt(classObjToCallOn, Jimple.v().newVirtualInvokeExpr(classObjToCallOn, Scene.v().getSootClass(className).getMethod("java.lang.Class getClass()").makeRef()));
		resultUnits.addLast(getClass);
		
		//now that we have the class object, make the call to getDeclaredConstructors:
		AssignStmt getCtors = Jimple.v().newAssignStmt(ctorArray, Jimple.v().newVirtualInvokeExpr(classObjToCallOn, Scene.v().getSootClass("java.lang.Class").getMethod("java.lang.reflect.Constructor[] getDeclaredConstructors()").makeRef()));
		resultUnits.addLast(getCtors);
		
		//now get index 0 for the no-argument ctor:
		((ArrayRef) ctorArray).setIndex(IntConstant.v(0)); //TODO: is this the right way to get [0]?
		AssignStmt getCtorZero = Jimple.v().newAssignStmt(ctorLocal, ((ArrayRef) ctorArray)); 
		resultUnits.addLast(getCtorZero);
		
		//now make accessible:	
		VirtualInvokeExpr setAccessible = Jimple.v().newVirtualInvokeExpr(ctorLocal, Scene.v().getSootClass("java.lang.reflect.Constructor").getMethod("void setAccessible(boolean)").makeRef(), IntConstant.v(1));
		resultUnits.addLast(Jimple.v().newInvokeStmt(setAccessible));
		
		return new ImmutablePair<PatchingChain<Unit>, Chain<Local>>(resultUnits, resultLocals);
	}
	
	public static boolean isPrimitive(Type t){
		return (t instanceof IntType || t instanceof ByteType || t instanceof CharType
				|| t instanceof ShortType || t instanceof DoubleType || t instanceof FloatType
				|| t instanceof LongType || t instanceof BooleanType);
	}
	
	/**
	 * This method checks recursively if there is a public ctor for a class
	 * which can be invoked using only primitive types. If the ctor expects
	 * another object, this method is called recursively on this object's ctor.
	 * 
	 *  If we find a ctor, we return it. We try to return the constructor
	 *  with the minimum number of objects that need to be passed.
	 * @param className
	 * @return
	 */
	public static SootMethod getPrimitiveConstructor(String className){
		SootClass sc = Scene.v().getSootClass(className);
		
		//get all publicly accessible constructors:
		List<SootMethod> ctors = new ArrayList<SootMethod>();
		for (SootMethod sm : sc.getMethods()){
			if (sm.isConstructor() && sm.isPublic())
				ctors.add(sm);
		}
		
		int minNumNonPrimitiveTypes = Integer.MAX_VALUE;
		SootMethod candidateCtor = null;
		
		for (SootMethod ctor : ctors){
			
			//all object parameters to this ctor can be constructed from primitive types
			boolean allObjectParamsArgumentsPrimitive = true;
			
			int numNonPrimitiveArgs = 0;
			
			for (Type t : ctor.getParameterTypes()){
				if (!isPrimitive(t))
				{
					numNonPrimitiveArgs++;
					/*
					 * okay, apparently an object needs to be passed. lets see if we can
					 * construct that object from primitive types:
					 */
					if (t instanceof RefType){ //this should now hold anyway
						String argClassName = ((RefType) t).getClassName();
						if (getPrimitiveConstructor(argClassName) == null)
							allObjectParamsArgumentsPrimitive = false;
					} else {
						allObjectParamsArgumentsPrimitive = false; //no RefType - don't know how to construct
					}
				}
			}
			if (numNonPrimitiveArgs == 0){ //best possible outcome, trivial to instantiate -- return directly
				return ctor;
			}
			else if (allObjectParamsArgumentsPrimitive && numNonPrimitiveArgs < minNumNonPrimitiveTypes){
				minNumNonPrimitiveTypes = numNonPrimitiveArgs;
				candidateCtor = ctor;
			}

		}
		return candidateCtor;
	}
	
	/**
	 * Inserts calls to all lifecycle methods at the end of the given method.
	 * @throws IOException 
	 * @throws SAXException 
	 * @throws ParserConfigurationException 
	 * @throws XPathExpressionException 
	 * 
	 */
	public void insertCallsToLifecycleMethods(SootMethod toInstrument) throws XPathExpressionException, ParserConfigurationException, SAXException, IOException {
		/* we now need to find the main activity among a) the entry points or,
		 * b) if its a dummy entry point among the callees
		 */					
		Body body = toInstrument.getActiveBody();
		PatchingChain<Unit> units = body.getUnits();
		LocalGenerator generator = new LocalGenerator(body);
		
		//find return statement and insert our stuff before it:
		List<Unit> returns = getReturnUnits(body);
		if (returns.size()<=0) {
			throw new RuntimeException("Unexpected: Body does not contain return stmt: " + body.toString());
		}
		Unit returnstmt = returns.get(0); //TODO Consider (inject before) all return stmts, not just the first one
		
		Set<String> classesToScan = new HashSet<String>();
		classesToScan.add(toInstrument.getDeclaringClass().getName());
		
		//we need this one to have classes instantiated for us
		CAndroidEntryPointCreator aep = new CAndroidEntryPointCreator(classesToScan);
		
		/* 
		 * 1. find all fields
		 * 2. check if they are references to UI interface implementing classes
		 * 3. get their references
		 * 4. call their interface method
		 */
		ArrayList<SootField> listenerFields = new ArrayList<SootField>();
		for (SootField f : toInstrument.getDeclaringClass().getFields()){
			//test if field is a reference to an object implementing a UI interface class
			for (String iface : uiListeners.keySet()){
				if (f.getType().toString().equals(iface))
					listenerFields.add(f);
			}
		}
				
		HashSet<SootMethod> onClickHandlerMethods = new HashSet<SootMethod>();
		
		//Limit injection to default main
		Pattern defaultMain = Pattern.compile("<"+Pattern.quote(toInstrument.getDeclaringClass().getName())+": .*");
		
		//Get onclick handlers registered via XML
		onClickHandlerMethods = getOnClickFromLayouts(defaultMain);
				
		//Get entry methods of default main
		onClickHandlerMethods.addAll(getEntryMethods(Pattern.compile(".*onClick.*")));
		
		if (listenerFields.size() < 1 && onClickHandlerMethods.size() < 1)
			return;
		
		///reference to class on which to call findViewById:
	    
	    Local thisRefLocal = body.getThisLocal();
	    
	    //get us the context local:
	    Local ctxLocal = null;
	    for (Local l : body.getLocals())
	    	if (l.getType().equals(RefType.v("android.content.Context"))){
	    		ctxLocal = l;
	    	}
	    // if there is no context local, generate one - we will need it
	    if (ctxLocal == null){
	    	ctxLocal = generator.generateLocal(RefType.v("android.content.Context"));
	    	units.insertBefore(
	    			Jimple.v().newAssignStmt(ctxLocal, Jimple.v().newVirtualInvokeExpr(thisRefLocal, Scene.v().getSootClass("android.content.Context").getMethod("android.content.Context getApplicationContext()").makeRef()))
	    		, returnstmt);
	    }

		// add references to the fields to call on them:
		for (SootField f : listenerFields){			
			Local g = generator.generateLocal(RefType.v(f.getType().toString()));
			units.insertBefore(
					Jimple.v().newAssignStmt(g, Jimple.v().newInstanceFieldRef(thisRefLocal, f.makeRef()))
			, returnstmt);
			/*
			 *  we have a reference (g) to the field (f), now 1) find the
			 *  interface's predefined methods and 2) call them on the reference (g)
			 */
			for (String callbackSubsig : uiListeners.get(f.getType().toString())){
				SootMethod callbackMethod = Scene.v().getSootClass(f.getType().toString()).getMethod(callbackSubsig);
				aep.buildMethodCall(callbackMethod, body, g, generator, returnstmt); //note: this already ADDS the method call to Units
			}
		}
		
		/* 
		 * After being done with fields, we now deal with onClick-elements
		 * defined in layout files.
		 */
		
		
		//For now, we filter for those which are defined in the main activity ONLY:
		Set<SootClass> otherActivities = new HashSet<SootClass>();
		for (SootMethod m : onClickHandlerMethods) {
			if (toInstrument.getDeclaringClass().equals(m.getDeclaringClass()) && !m.getName().contains("init>")) {
				//Check if method requires parameters we cannot just make up
				boolean canSetParameter = true;
				List<Type> argTypes = m.getParameterTypes();
				for (Type argType:argTypes) {
					if (!argType.equals(RefType.v("android.content.Context")) && !argType.equals(RefType.v("android.view.View")))
						canSetParameter=false;						
				}
				if (canSetParameter)
					aep.buildMethodCall(m, body, thisRefLocal, generator, returnstmt);
			} else if (!m.getName().contains("init>") && !m.getDeclaringClass().getName().contains("$")) {
				otherActivities.add(m.getDeclaringClass());				
			}
		}
		Unit comment = new soot.jimple.internal.JNopStmt();
		comment.addTag(new StringConstantValueTag("Artificial calls to other activities"));
		body.getUnits().insertBefore(comment, returnstmt);
		for (SootClass activity:otherActivities) {				
				//Intent i = new Intent(this, bla.Blubb.class)
				String classToInvoke = activity.getName().replace('.', '/');
				Local intentLocal = generator.generateLocal(RefType.v("android.content.Intent"));			
				AssignStmt assignStmt = Jimple.v().newAssignStmt(intentLocal,Jimple.v().newNewExpr(RefType.v("android.content.Intent")));
				
				SootMethodRef initRef = Scene.v().makeMethodRef(
						Scene.v().getSootClass("android.content.Intent"),
						"<init>", 
						Arrays.asList(new Type[] { RefType.v("android.content.Context"), RefType.v("java.lang.Class") }), 
						VoidType.v(),
						false);
				SpecialInvokeExpr invok = Jimple.v().newSpecialInvokeExpr(
						intentLocal, 
						initRef, 
						Arrays.asList(thisRefLocal,ClassConstant.v(classToInvoke)));
				InvokeStmt invokStmt = Jimple.v().newInvokeStmt(invok);
				
				// startActivity(i)
				InvokeExpr expr = Jimple.v().newVirtualInvokeExpr(thisRefLocal, Scene.v().getMethod("<android.app.Activity: void startActivity(android.content.Intent)>").makeRef(), intentLocal);

				//Reversed order
				body.getUnits().insertBefore(assignStmt, returnstmt);
				body.getUnits().insertBefore(invokStmt, returnstmt);
				body.getUnits().insertBefore(new JInvokeStmt(expr), returnstmt);
		}
		
		//TODO in each activity, inject calls to their onclickhandlers at the end of oncreate
	}


	/**
	 * Inserts statements at the end of a method, but ensures that thrown
	 * exceptions are still handled properly.
	 * 
	 * @param body
	 * @param stmts
	 */
	public static void insertAtEndOfMethod(Body body, Chain<Unit> stmts) {
		if (Main.DEBUG) {
			log.info("Inserting at end of " + body.getMethod().getName());
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
			log.info("Decoding " + apkFile.getAbsolutePath());
		}
		File decodedDir = new File ("decoded");
		if (decodedDir.exists() && decodedDir.isDirectory()) {
			FileUtils.deleteDirectory(decodedDir);
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
			log.error("Something went wrong when packing " + apkFile.getAbsolutePath());
			return null;
		}

		if (CLEAR_AFTER_USER) {
			File decoded = new File("decoded");
			if (decoded.isDirectory())
				decoded.delete();
			else
				log.error("Unexpected: decoded is not a directory");
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
			log.error("New manifest: \n" + manifestString);
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
			log.error("Dumping class to " + fileName);
		}
		OutputStream streamOut = new FileOutputStream(fileName);
		PrintWriter writerOut = new PrintWriter(new OutputStreamWriter(streamOut));
		Printer.v().printTo(sClass, writerOut);
		writerOut.flush();
		streamOut.close();
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
			log.info("Replacing class " + clazz + " by child " + child);
		}
		SootClass newClass = Scene.v().getSootClass(child);

		if (newClass.isAbstract())
			log.error("WARN: Replacement class" + child + " is abstract");

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
						log.error("WARN: NOT IMPLEMENTED: " + expr);
					}
				}
			}
		}

	}

}
