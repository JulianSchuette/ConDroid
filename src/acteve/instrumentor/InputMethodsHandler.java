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
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import soot.Body;
import soot.BooleanType;
import soot.Local;
import soot.Modifier;
import soot.PrimType;
import soot.RefType;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Type;
import soot.Unit;
import soot.Value;
import soot.VoidType;
import soot.jimple.IdentityStmt;
import soot.jimple.IntConstant;
import soot.jimple.NullConstant;
import soot.jimple.ParameterRef;
import soot.jimple.Stmt;
import soot.jimple.StringConstant;
import soot.util.Chain;

/**
 * Input methods inject test values which are used for a test run of the program.
 *
 */
public class InputMethodsHandler
{
	static void instrument(String fileName)
	{
		if(fileName == null)
			return;

		File file = new File(fileName);
		if (!file.exists()) {
			System.out.println("input_methods.txt not found.");
			return;
		}
		try{
			BufferedReader reader = new BufferedReader(new FileReader(file));
			String line = reader.readLine();
			while (line != null) {
				int index = line.indexOf(' ');
				String className = line.substring(0, index);
				if (Scene.v().containsClass(className)) {
					String methodSig = line.substring(index+1).trim();
					SootClass declClass = Scene.v().getSootClass(className);
					if (declClass.declaresMethod(methodSig)) {
						SootMethod method = declClass.getMethod(methodSig);
						apply(method);
					}
				}
				line = reader.readLine();
			}
			reader.close();
		}
		catch(IOException e){
			throw new Error(e);
		}
	}

	static void apply(SootMethod method)
	{
		Body body = method.retrieveActiveBody();
		List<Local> params = new ArrayList<Local>();
		Chain<Unit> units = body.getUnits().getNonPatchingChain();
		//Step through statements of this method
		for (Unit u : units) {
			Stmt s = (Stmt) u;
			if (Instrumentor.paramOrThisIdentityStmt(s)) {
				//Collect parameters of this method
				if (((IdentityStmt) s).getRightOp() instanceof ParameterRef) {
					//that is, dont add ThisRef's to params
					params.add((Local) ((IdentityStmt) s).getLeftOp());
				}
				continue;
			}
			else {
				//Immediately after $this assignment in method body, insert an invocation of the new injector.
				SootMethod injector = addInjector(method);
				units.insertBefore(G.jimple.newInvokeStmt(G.staticInvokeExpr(injector.makeRef(), params)), s);
				return;
			}
		}		
	}

	/**
	 * Create a new method "static private void Klass.myMethod$sym(T bla, U blubb)" for a given method "Klass.myMethod(T bla, U blubb)".
	 * 
	 * The new method looks like this, for example:
	 * 
	 *     
	 <pre>
	 private static void doBlubb$sym(android.view.View)
     {
        acteve.symbolic.integer.Expression _sym_tmp_2;
        android.view.View _sym_tmp_1;

        _sym_tmp_1 := @parameter0: android.view.View;
        _sym_tmp_2 = staticinvoke &lt;models.android.view.View$A3TInvoke: acteve.symbolic.integer.Expression Landroid_view_View_2(java.lang.Object,java.lang.String)&gt;(_sym_tmp_1, "doBlubb$android$view$View$0");
        staticinvoke &lt;acteve.symbolic.Util: void argpush(int,acteve.symbolic.integer.Expression,acteve.symbolic.integer.Expression)&gt;(-1, null, _sym_tmp_2);
        return;
     }
     </pre>
	 * 
	 * i.e., it forwards the original parameters to symbolic invocators models.my.Type$A3TInvoke which can generate symbolic expressions and put them on the stack.
	 * @param method
	 * @return the injector method. 
	 */
	public static SootMethod addInjector(SootMethod method) 
	{
		StringBuilder builder = new StringBuilder();
		builder.append(method.getName());
		for (Iterator pit = method.getParameterTypes().iterator(); pit.hasNext();) {
			Type ptype = (Type) pit.next();
			builder.append("$");
			builder.append(ptype.toString().replace('.','$'));
		}

		String id = builder.toString();

		SootMethod injector = new SootMethod(method.getName()+"$sym",
				new ArrayList<Type>(method.getParameterTypes()),
				VoidType.v(),
				Modifier.STATIC | Modifier.PRIVATE);
		method.getDeclaringClass().addMethod(injector);

		G.addBody(injector);
		List<Local> paramLocals = G.paramLocals(injector);

		List<Value> symArgs = new ArrayList<Value>();
		symArgs.add(IntConstant.v(-1));

		if (!method.isStatic())
			symArgs.add(NullConstant.v());

		int i = 0;
		for (Iterator<Type> pit = method.getParameterTypes().iterator(); pit.hasNext();) {
			Type ptype = pit.next();
			//If param is not Boolean:
			if (((ptype instanceof PrimType) && !(ptype instanceof BooleanType)) || ptype instanceof RefType) {
				SootMethod m = G.symValueInjectorFor(ptype);
				Local l = G.newLocal(G.EXPRESSION_TYPE);
				String symbol = id + "$" + i;
				G.assign(l, G.staticInvokeExpr(m.makeRef(), (Local) paramLocals.get(i), StringConstant.v(symbol)));
				symArgs.add(l);
			} else {
				symArgs.add(NullConstant.v());
			}
			i++;
		}
		G.invoke(G.staticInvokeExpr(G.argPush[symArgs.size()-1], symArgs));
		G.retVoid();
		G.debug(injector, Main.DEBUG);
		return injector;
	}
}
