package acteve.instrumentor;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;

import org.xml.sax.SAXException;

import soot.Body;
import soot.Kind;
import soot.Scene;
import soot.SceneTransformer;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.AssignStmt;
import soot.jimple.IntConstant;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;

/**
 * Extends the callgraph by the following android specific pseudo edges:
 * 
 * - in A.a findViewById(x) where x=id of a view of class B:
 *   A.a -> B.<init>(android.content.Context,android.util.AttributeSet,int)
 * 
 * @author julian.schuette@aisec.fraunhofer.de
 *
 */
public class AndroidCGExtender extends SceneTransformer {

	@Override
	protected void internalTransform(String phaseName,
			Map<String, String> options) {
		CallGraph cg = Scene.v().getCallGraph();
		
		assert cg!=null:"Call Graph not available";
		for (SootClass cl:Scene.v().getApplicationClasses()) {
			for (SootMethod me:cl.getMethods()) {
				if (me.hasActiveBody()) {
					Body b = me.getActiveBody();
					for (Unit u:b.getUnits()) {
						if (u.toString().contains("android.view.View findViewById(int)")) {
							System.out.println("Found a call to findViewById: " + u.toString());
							
							// Extract constant parameter from findViewByID
							if (u instanceof AssignStmt) {
								Value val = ((AssignStmt) u).getInvokeExpr().getArg(0);
								if (val instanceof IntConstant) {
									int id = ((IntConstant) val).value;
									
									System.out.println("Now we need to find out which class is registered for " + "0x"+Integer.toHexString(id) +" (" +id + "). Is referenced from "+b.getMethod().getSignature());
									
									// retrieve class mapped to id
									try {
										HashMap<Integer, SootClass> id2cls = Main.ih.getClassOfViewId();
										SootClass clazz = id2cls.get(new Integer(id));
										System.out.println("This is my class: " + clazz);
										if (clazz!=null) {
											// insert implicit edge into CG
											SootMethod constr = clazz.getMethod("void <init>(android.content.Context,android.util.AttributeSet,int)");
											Edge newEdge = new Edge(b.getMethod(), u, constr, Kind.CLINIT);
											cg.addEdge(newEdge);
											constr = clazz.getMethod("void <init>(android.content.Context,android.util.AttributeSet)");
											newEdge = new Edge(b.getMethod(), u, constr, Kind.CLINIT);
											System.out.println("Extending CG by " + b.getMethod().getSignature() + " --> " + constr.getSignature());
											cg.addEdge(newEdge);							
										} else {
											System.err.println("Could not find class for view id " + ((IntConstant) val).value);
										}						
									} catch (XPathExpressionException | SAXException
											| IOException | ParserConfigurationException e) {
										e.printStackTrace();
									}
								} else {
									System.err.println("Don't know how to handle non-constant parameter :" + u.toString());
								}
							}
						}
					}
				}
			}
		}
		Scene.v().setCallGraph(cg);
	}

}
