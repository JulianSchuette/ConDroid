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
import java.util.ListIterator;
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
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import soot.ArrayType;
import soot.Body;
import soot.CharType;
import soot.JimpleClassSource;
import soot.Local;
import soot.MethodOrMethodContext;
import soot.Modifier;
import soot.PatchingChain;
import soot.Printer;
import soot.RefLikeType;
import soot.RefType;
import soot.Scene;
import soot.SootClass;
import soot.SootField;
import soot.SootMethod;
import soot.SootMethodRef;
import soot.SourceLocator;
import soot.Type;
import soot.Unit;
import soot.Value;
import soot.VoidType;
import soot.javaToJimple.IInitialResolver.Dependencies;
import soot.javaToJimple.LocalGenerator;
import soot.jimple.AssignStmt;
import soot.jimple.ClassConstant;
import soot.jimple.IntConstant;
import soot.jimple.InvokeExpr;
import soot.jimple.Jimple;
import soot.jimple.JimpleBody;
import soot.jimple.SpecialInvokeExpr;
import soot.jimple.StringConstant;
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
import soot.util.Chain;

/**
 * Helper class for instrumenting bytecode artifacts.
 * 
 * @author Julian Schuette (julian.schuette@aisec.fraunhofer.de)
 * 
 */
public class InstrumentationHelper {
	
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
	private static HashSet<String> mainActivities = new HashSet<String>();

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
	public static HashSet<SootMethod> getOnClickFromLayouts(Pattern filter) throws ParserConfigurationException, SAXException, IOException, XPathExpressionException{
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
			System.out.println("Checking layout file " + layoutFileName);
			
			File layoutFile = new File("decoded/res/layout/" + layoutFileName + ".xml");
			Document doc = dBuilder.parse(layoutFile);
			
			NodeList onClickNodes = (NodeList) onClickExpr.evaluate(doc, XPathConstants.NODESET);
						
			for (int i = 0; i < onClickNodes.getLength(); i++){
				String onClickMethod = "void " + ((Attr) onClickNodes.item(i)).getValue() + "(android.view.View)";
				String className = layoutsToActivity.get(layoutFileName).get(0).getName(); 
				SootMethod oncmeth = Scene.v().getMethod("<"+className+": " + onClickMethod + ">");
				if (filter.matcher(oncmeth.getSignature()).matches()) {
					onClickListeners.add(oncmeth);
					System.out.println("XML-defined onclick handler: " + oncmeth.getSignature());
				} else {
					System.out.println("Does not match filter: " + oncmeth.getSignature() + " :: " + filter.pattern());
				}
			}
		}
		result = onClickListeners;
		
		System.out.println("Found " + result.size() + " android:onClickListeners in layout XML file.");
		
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
	private static HashMap<String, List<SootClass>> getLayoutToActivityAssociation() throws SAXException, IOException, XPathExpressionException, ParserConfigurationException {
		HashMap<String, List<SootClass>> layoutNameToClass = new HashMap<String, List<SootClass>>();

		HashMap<SootClass, List<String>> classesToLayoutIDs= new HashMap<SootClass, List<String>>();
		
		//Collect all layout ids which are used in setContentView
		SootMethod setContentView = Scene.v().getMethod("<android.app.Activity: void setContentView(int)>");
		Iterator<Edge> callers = Scene.v().getCallGraph().edgesInto(setContentView);
		while (callers.hasNext()) {
			Edge e = callers.next();
			MethodOrMethodContext caller = e.getSrc();
			Body callerBody = caller.method().getActiveBody();
			PatchingChain<Unit> callerUnits = callerBody.getUnits();
			for (Unit u:callerUnits) {
				if (u instanceof JInvokeStmt) {
					SootMethod calledMethod = ((JInvokeStmt) u).getInvokeExpr().getMethod();;
					if (((JInvokeStmt) u).getInvokeExpr().getMethod().equals(setContentView)) {
						Value arg = ((JInvokeStmt) u).getInvokeExpr().getArg(0);
						if (arg instanceof IntConstant) {
							int id = ((IntConstant) arg).value;
							System.out.println("ID: "+Integer.toHexString(id));
							if (!classesToLayoutIDs.containsKey(caller.method().getDeclaringClass())) {
								classesToLayoutIDs.put(caller.method().getDeclaringClass(), new ArrayList<String>());
							}
							classesToLayoutIDs.get(caller.method().getDeclaringClass()).add("0x"+Integer.toHexString(id));
						} else {
							System.err.println("Sorry, could not resolve references layout: " + u.toString());
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
	        	Node n = nodes.item(i);
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

	public SootMethod getDefaultOnCreate() {
		List<SootMethod> entrypoints = Scene.v().getEntryPoints();
		SootMethod result = null;
		List<SootMethod> realEntryPoints = new ArrayList<SootMethod>();
		realEntryPoints.addAll(entrypoints);
		ListIterator<SootMethod> entryIt = realEntryPoints.listIterator();
		while (entryIt.hasNext()){
			SootMethod m = entryIt.next();
			if (m.getSignature().equals("<dummyMainClass: void dummyMainMethod()>")) {
				//When in dummyMain, add "real" entrypoints
				for (SootMethod more:MethodUtils.getCalleesOf(m)) {
					entryIt.add(more);
					entryIt.previous();
				}
			}
			if (isMainActivity(m.getDeclaringClass().getName()) && m.getSubSignature().equals("void onCreate(android.os.Bundle)"))
			{
				result = m;
				break;
			}
		}
		
		return result;
	}
	
	/**
	 * Inserts calls to all lifecycle methods at the end of the onCreate()
	 * method.
	 * @throws Exception if no onCreate(android.os.Bundle) method is found
	 */
	public void insertCallsToLifecycleMethods(SootMethod toInstrument) throws Exception{
		/* we now need to find the main activity among a) the entry points or,
		 * b) if its a dummy entry point among the callees
		 */	
		if (toInstrument == null){
			System.err.println("No onCreate method found in app; this should not happen. Call graph not yet built?");
			throw new Exception("No onCreate() found!");
		} else {
			System.out.println("Injecting calls to Android lifecycle methods into " + toInstrument.getSignature());
		}
				
		Body body = toInstrument.getActiveBody();
		PatchingChain<Unit> units = body.getUnits();
		LocalGenerator generator = new LocalGenerator(body);
		
		//TODO: find the return statement and insert our stuff before that:
		Iterator<Unit> it = units.iterator();
		
		/**
		 * We need to insert all statements right before the return
		 */
		Unit returnstmt = null;
		while (it.hasNext()){
			Unit tmp = it.next();
			if (tmp.toString().equals(Jimple.v().newReturnVoidStmt().toString()))
				returnstmt = tmp;
		}
		System.out.println("Found return statement: " + returnstmt.toString());
				
		
		Set<String> classesToScan = new HashSet<String>();
		classesToScan.add(toInstrument.getDeclaringClass().getName());
		
		//we need this one to have classes instantiated for us
		CAndroidEntryPointCreator aep = new CAndroidEntryPointCreator(classesToScan);
		
		/* 
		 * new battle plan:
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
		
		System.out.println("Found " + listenerFields.size() + " fields that are references to Objects implementing UI callback methods.");
		
		HashSet<SootMethod> onClickHandlerMethods = new HashSet<SootMethod>();
		
		//Limit injection to default main
		Pattern defaultMain = Pattern.compile("<"+Pattern.quote(toInstrument.getDeclaringClass().getName())+": .*");
		
		//Get onclick handlers registered via XML
		onClickHandlerMethods = getOnClickFromLayouts(defaultMain);
				
		//Get entry methods of default main
		onClickHandlerMethods.addAll(getEntryMethods(null));
		
		if (listenerFields.size() < 1 && onClickHandlerMethods.size() < 1)
			return;
		
		///reference to class on which to call findViewById:
	    
	    Local thisRefLocal = body.getThisLocal();
	    
	    //get us the context local:
	    Local ctxLocal = null;
	    for (Local l : body.getLocals())
	    	if (l.getType().equals(RefType.v("android.content.Context"))){
	    		ctxLocal = l;
	    		System.out.println("Found Ctx local: " + ctxLocal.getName() + " of type " + ctxLocal.getType().toString());
	    	}
	    // if there is no context local, generate one - we will need it
	    if (ctxLocal == null){
	    	ctxLocal = generator.generateLocal(RefType.v("android.content.Context"));
	    	units.insertBefore(
	    			Jimple.v().newAssignStmt(ctxLocal, Jimple.v().newVirtualInvokeExpr(thisRefLocal, Scene.v().getSootClass("android.content.Context").getMethod("android.content.Context getApplicationContext()").makeRef()))
	    		, returnstmt);
	    	System.out.println("Generated Ctx Local of type " + ctxLocal.getType().toString());
	    }

		// add references to the fields to call on them:
		for (SootField f : listenerFields){			
			System.out.println("Processing field " + f.getName() + " of reference type " + f.getType().toString());
			Local g = generator.generateLocal(RefType.v(f.getType().toString()));
			units.insertBefore(
					Jimple.v().newAssignStmt(g, Jimple.v().newInstanceFieldRef(thisRefLocal, f.makeRef()))
			, returnstmt);
			/*
			 *  we have a reference (g) to the field (f), now 1) find the
			 *  interface's predefined methods and 2) call them on the reference (g)
			 */
			System.out.println("Number of callback methods for field " + f.getName() + ": " + uiListeners.get(f.getType().toString()).length);
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
		for (SootMethod m : onClickHandlerMethods)
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
		
		for (SootClass activity:otherActivities) {
				System.out.println("Injecting call to " + activity);
				//Start other activities
				
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

				
//				// startActivity(i)
				InvokeExpr expr = Jimple.v().newVirtualInvokeExpr(thisRefLocal, Scene.v().getMethod("<android.app.Activity: void startActivity(android.content.Intent)>").makeRef(), intentLocal);

				//Reverse order
				body.getUnits().insertBefore(assignStmt, returnstmt);
				body.getUnits().insertBefore(Jimple.v().newInvokeStmt(invok), returnstmt);
				body.getUnits().insertBefore(new JInvokeStmt(expr), returnstmt);
				
//				aep.buildMethodCall(m, body, thisRefLocal, generator, returnstmt);				
		}
				
		/*
		 * Now we only have the methods defined as android:onClickListener
		 * which are in the MainActivity. Add some clals now.
		 */
		
		//TODO In default main, inject startActivity to invoke all other activities
		
		//TODO in each activity, inject calls to their onclickhandlers at the end of oncreate
//		for (SootMethod m : onClickHandlerMethods) {
//			Body b = m.getDeclaringClass().getMethod("void onCreate(android.os.Bundle)").getActiveBody();
//			for (Unit u:b.getUnits()) {
//				System.out.println(u);
//			}
//		}
		
		
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
