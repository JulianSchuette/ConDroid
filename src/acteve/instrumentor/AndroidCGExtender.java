/* 
 * Copyright (c) 2014, Julian Schütte, Fraunhofer AISEC
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
 * - in A.a findViewById(x) where x=id of a view of class B: A.a ->
 * B.<init>(android.content.Context,android.util.AttributeSet,int)
 * 
 * @author julian.schuette@aisec.fraunhofer.de
 * 
 */
public class AndroidCGExtender extends SceneTransformer {

	@Override
	protected void internalTransform(String phaseName, Map<String, String> options) {
		CallGraph cg = Scene.v().getCallGraph();

		assert cg != null : "Call Graph not available";
		for (SootClass cl : Scene.v().getApplicationClasses()) {
			for (SootMethod me : cl.getMethods()) {
				if (me.hasActiveBody()) {
					Body b = me.getActiveBody();
					for (Unit u : b.getUnits()) {
						if (u.toString().contains("android.view.View findViewById(int)")) {
							// Extract constant parameter from findViewByID
							if (u instanceof AssignStmt) {
								Value val = ((AssignStmt) u).getInvokeExpr().getArg(0);
								if (val instanceof IntConstant) {
									int id = ((IntConstant) val).value;

									// retrieve class mapped to id
									try {
										HashMap<Integer, SootClass> id2cls = Main.ih.getClassOfViewId();
										SootClass clazz = id2cls.get(new Integer(id));
										if (clazz != null) {
											// insert implicit edge into CG
											SootMethod constr = null;
											Edge newEdge = null;
											try {
												constr = clazz.getMethod("void <init>(android.content.Context,android.util.AttributeSet,int)");
												newEdge = new Edge(b.getMethod(), u, constr, Kind.CLINIT);
												System.out.println("Extending CG by " + b.getMethod().getSignature() + " --> " + constr.getSignature());
												cg.addEdge(newEdge);
											} catch (RuntimeException rte) {
												// Method does not exist. continue
											}
											try {
												constr = clazz.getMethod("void <init>(android.content.Context,android.util.AttributeSet)");
												newEdge = new Edge(b.getMethod(), u, constr, Kind.CLINIT);
												System.out.println("Extending CG by " + b.getMethod().getSignature() + " --> " + constr.getSignature());
												cg.addEdge(newEdge);
											} catch (RuntimeException rte) {
												// method does not exist. continue
											}
										}
									} catch (XPathExpressionException | SAXException | IOException | ParserConfigurationException e) {
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
