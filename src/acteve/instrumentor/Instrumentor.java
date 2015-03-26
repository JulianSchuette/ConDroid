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

import soot.ArrayType;
import soot.Body;
import soot.Immediate;
import soot.IntType;
import soot.Local;
import soot.PatchingChain;
import soot.PrimType;
import soot.RefLikeType;
import soot.RefType;
import soot.Scene;
import soot.SootClass;
import soot.SootField;
import soot.SootMethod;
import soot.SootMethodRef;
import soot.Type;
import soot.Unit;
import soot.Value;
import soot.jimple.AbstractStmtSwitch;
import soot.jimple.ArrayRef;
import soot.jimple.AssignStmt;
import soot.jimple.BinopExpr;
import soot.jimple.CastExpr;
import soot.jimple.CaughtExceptionRef;
import soot.jimple.ConditionExpr;
import soot.jimple.Constant;
import soot.jimple.FieldRef;
import soot.jimple.IdentityStmt;
import soot.jimple.IfStmt;
import soot.jimple.InstanceFieldRef;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.InstanceOfExpr;
import soot.jimple.IntConstant;
import soot.jimple.InvokeExpr;
import soot.jimple.InvokeStmt;
import soot.jimple.Jimple;
import soot.jimple.LengthExpr;
import soot.jimple.LookupSwitchStmt;
import soot.jimple.MonitorStmt;
import soot.jimple.NegExpr;
import soot.jimple.NewArrayExpr;
import soot.jimple.NewExpr;
import soot.jimple.NewMultiArrayExpr;
import soot.jimple.NullConstant;
import soot.jimple.ReturnStmt;
import soot.jimple.StaticFieldRef;
import soot.jimple.StaticInvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.StringConstant;
import soot.jimple.TableSwitchStmt;
import soot.jimple.internal.JReturnVoidStmt;
import soot.jimple.internal.JVirtualInvokeExpr;
import soot.tagkit.BytecodeOffsetTag;
import soot.tagkit.LineNumberTag;
import soot.tagkit.SourceFileTag;
import soot.tagkit.SourceLineNumberTag;
import soot.tagkit.SourceLnPosTag;
import soot.tagkit.StringTag;
import soot.toolkits.graph.BriefUnitGraph;
import soot.toolkits.graph.UnitGraph;
import soot.toolkits.scalar.SimpleLocalDefs;
import soot.util.Chain;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Instrumentor extends AbstractStmtSwitch {
	public static Logger log = LoggerFactory.getLogger(Instrumentor.class);
	private final RWKind rwKind;
	private final String outDir;
	private final String sdkDir;
    private final MethodSubsigNumberer methSubsigNumberer;
    private final MethodSigNumberer methSigNumberer;
    private final FieldSigNumberer fieldSigNumberer;
	private final Filter fieldsWhitelist;
	private final Filter fieldsBlacklist;
	@SuppressWarnings("unused")
	private final Filter methodsWhitelist;
	private final boolean instrAllFields;
	private final Map<SootField, SootField> fieldsMap;
	private final Map<SootField, SootField> idFieldsMap;
    private final List<String> condIdStrList;
	// map from a original local to its corresponding shadow local
	private final Map<Local, Local> localsMap;
	private SootMethod currentMethod;
	private int sigIdOfCurrentMethod;
	
	//TODO Read targets from configurable targets.txt file
	private static HashSet<String> TARGET_METHODS = new HashSet<String>();
	static {
		HashSet<String> aHashSet = new HashSet<String>();
		aHashSet.addAll( Arrays.asList(new String[] {
				"<java.lang.Class: T newInstance()>",
				 "<java.lang.Class: newInstance()>",
				 "<java.lang.Object: newInstance()>",
				 "<java.lang.Class: T newInstance(java.lang.Object...)>",
				 "<java.lang.Class: newInstance(java.lang.Object...)>",
				 "<java.lang.Class: java.lang.reflect.Constructor<T> getConstructor(java.lang.Class<?>...)>",
				 "<java.lang.Class: java.lang.reflect.Constructor getConstructor(java.lang.Class...)>",
				 "<java.lang.Class: java.lang.reflect.Constructor<?>[] getConstructors()>",
				 "<java.lang.Class: java.lang.reflect.Constructor[] getConstructors()>",
				 "<java.lang.Class: java.lang.reflect.Constructor<?>[] getDeclaredConstructors()>",
				 "<java.lang.Class: java.lang.reflect.Constructor[] getDeclaredConstructors()>",
				 "<java.lang.Class: java.lang.Class<T> forName(java.lang.String)>",
				 "<java.lang.Class: java.lang.Class forName(java.lang.String)>",

				 "<java.lang.ClassLoader: java.lang.Class<T> loadClass(java.lang.String)>",
				 "<java.lang.ClassLoader: java.lang.Class loadClass(java.lang.String)>",
				 "<java.lang.ClassLoader: java.lang.Class<T> loadClass(java.lang.String,boolean)>",
				 "<java.lang.ClassLoader: java.lang.Class loadClass(java.lang.String,boolean)>",
				 "<java.lang.ClassLoader: void <init>()>",
				 "<java.lang.ClassLoader: void <init>(java.lang.ClassLoader)>",
				 "<java.lang.ClassLoader: java.lang.ClassLoader getSystemClassLoader()>",

				 "<java.net.URLClassLoader: void <init>(java.net.URL[])>",
				 "<java.net.URLClassLoader: void <init>(java.net.URL[],java.lang.ClassLoader)>",
				 "<java.net.URLClassLoader: void <init>(java.net.URL[],java.lang.ClassLoader,java.net.URLStreamHandlerFactory)>",

				 "<java.security.SecureClassLoader: void <init>()>",
				 "<java.security.SecureClassLoader: void <init>(java.lang.ClassLoader)>",
				 
				"<dalvik.system.BaseDexClassLoader: void <init>(Java.lang.String,java.io.File,java.lang.String,java.lang.ClassLoader)>",

				"<dalvik.system.DexClassLoader: void <init>(java.lang.String,java.lang.String,java.lang.String,java.lang.ClassLoader)>",

				"<dalvik.system.PathClassLoader: void <init>(Java.lang.String,java.lang.ClassLoader)>",
				 "<dalvik.system.PathClassLoader: void <init>(Java.lang.String,Java.lang.String,java.lang.ClassLoader)>"
				}));
		TARGET_METHODS = aHashSet;
	};

	private boolean doRW() {
		return doRW(null);
	}

	private boolean doRW(SootField fld) {
		if (rwKind == RWKind.NONE)
			return false;
		if (sdkDir != null) {
			//instrumenting app
			return true;
		}
		if (fld == null) {
			//ignore array elems read/write in sdk code
			return false; 
		}
		if(instrAllFields) {
			return fld.getDeclaringClass().getName().startsWith("android.");
		}
		
		String fldSig = fld.getSignature();
		if(fieldsWhitelist == null || fieldsWhitelist.matches(fldSig)) {
			return fieldsBlacklist == null ? true : !fieldsBlacklist.matches(fldSig);
		} 
		return false;
	}

	// sdkDir == null iff we are instrumenting framework
	public Instrumentor(RWKind _rwKind, 
						String _outDir, 
						String _sdkDir, 
						String _fldsWLFile, 
						String _fldsBLFile, 
						String _methsWLFile,
						boolean _instrAllFields) {
		assert (_outDir != null);
		rwKind = _rwKind;
		outDir = _outDir;
		sdkDir = _sdkDir;
 		fieldsMap = new HashMap<SootField, SootField>();
		idFieldsMap = (rwKind == RWKind.ID_FIELD_WRITE) ? new HashMap<SootField, SootField>() : null;
 		condIdStrList = new ArrayList<String>();
		localsMap = new HashMap<Local, Local>();
		//writeMap = new HashMap<Integer, Set<Integer>>();
		methSubsigNumberer = new MethodSubsigNumberer();
		methSigNumberer = new MethodSigNumberer();
		fieldSigNumberer = new FieldSigNumberer();
		
		fieldsWhitelist = _fldsWLFile != null ? new Filter(_fldsWLFile) : null;
		fieldsBlacklist = _fldsBLFile != null ? new Filter(_fldsBLFile) : null;
		methodsWhitelist = _methsWLFile != null ? new Filter(_methsWLFile) : null;
		instrAllFields = _instrAllFields;
	}

	public void instrument(Set<SootClass> classes) {
		for (SootClass klass : classes) {
			klass.setApplicationClass();
			addSymbolicFields(klass);
		}

		loadFiles();

		for (SootClass klass : classes) {
			List<SootMethod> origMethods = klass.getMethods();
			for (SootMethod m : origMethods) {
				if (!m.isConcrete())
					continue;

				//BY JULIAN: For Debugging, attach the subsig number to each method.
				int subSig = methSubsigNumberer.getOrMakeId(m);
				m.addTag(new StringTag(String.valueOf(subSig)));
				
				
				if (ModelMethodsHandler.modelExistsFor(m)) {
					// do not instrument method if a model for it exists
					log.debug("skipping instrumentation of " + m + " (model exists)");
					continue;
				}
				instrument(m);
			}
		}

		saveFiles();
	}

	/**
	 * Add symbolic counterparts to fields in a class.
	 * 
	 * @param c
	 */
	private void addSymbolicFields(SootClass c) {
		for (Iterator<SootField> it = c.getFields().snapshotIterator(); it.hasNext();) {
			SootField origField = (SootField) it.next();
			if(addSymLocationFor(origField.getType())) {
				SootField symField = new SootField(origField.getName()+"$sym",
												   G.EXPRESSION_TYPE, origField.getModifiers());
				if (!c.declaresFieldByName(symField.getName()))
					c.addField(symField);
				fieldsMap.put(origField, symField);
			}

            if(rwKind == RWKind.ID_FIELD_WRITE && doRW(origField)){
				SootField idField = new SootField(origField.getName()+"$a3tid", IntType.v(), origField.getModifiers());
				log.debug("Adding field " + idField.getName() + " for " + origField.getName() + " in " + c.getName());
				if (!c.declaresFieldByName(idField.getName()))
					c.addField(idField);
				idFieldsMap.put(origField, idField);
            }
		}
	}

	private void instrument(SootMethod method) {
		SwitchTransformer.transform(method);
 		localsMap.clear();

		Body body = method.retrieveActiveBody();		
		G.editor.newBody(body, method);
		addSymLocals(body);
		List<Local> params = new ArrayList<Local>();
	
		currentMethod = method;
		sigIdOfCurrentMethod = methSigNumberer.getOrMakeId(method);

		//Iterate over stmts of current method and apply symbolic instrumentation
		while (G.editor.hasNext()) {
			Stmt s = G.editor.next();
			if (paramOrThisIdentityStmt(s)) {
				params.add((Local) ((IdentityStmt) s).getLeftOp());
			} else if (s.containsInvokeExpr()) {
				if (s instanceof AssignStmt && isSupportedByModel(s.getInvokeExpr())) {
					//Treat like binop stmt
					handleModelledInvokeExpr((AssignStmt) s);
				} else {
					handleInvokeExpr(s);
				}
			} else if (!s.branches())
				s.apply(this);
		}

		//it is done at the end for a good reason
		insertPrologue(body, params);
		
		instrumentConds(body);
		
		G.debug(method, Main.DEBUG);
	}
	
	/**
	 * Handles invokes supported by the model, e.g. String.contains.
	 * s is expected to be an assignStmt with an InvokeExpr.
	 */
	private void handleModelledInvokeExpr(AssignStmt s) {
		if (!s.containsInvokeExpr()) {
			System.err.println("Unexpected: handleModelledInvokeExpr with non-invoke stmt: " + s.toString());
			return;
		}
		
        if (!(s.getLeftOp() instanceof Immediate)) {
			System.err.println("Unexpected: handleModelledInvokeExpr with non-immediate local: " + s.toString());
        	return;
        }
        
		//TODO Consider negate
        JVirtualInvokeExpr rightOp = (JVirtualInvokeExpr) s.getRightOp();
        Immediate op1 = (Immediate) rightOp.getBase();
        Immediate op2 = (Immediate) s.getInvokeExpr().getArg(0);
        
		Immediate symOp1 = op1 instanceof Constant ? NullConstant.v() : localsMap.get((Local) op1);
		Immediate symOp2 = op2 instanceof Constant ? NullConstant.v() : localsMap.get((Local) op2);
		
		String methodName = G.binopSymbolToMethodName.get(s.getInvokeExpr().getMethod().getSignature()); //TODO add java.lang.string.contains to binopsymboltomethodname
		String methodSig = G.EXPRESSION_CLASS_NAME + " " + methodName + "(" + G.EXPRESSION_CLASS_NAME + "," + 
				G.EXPRESSION_CLASS_NAME + "," + RefType.v("java.lang.String") + "," + RefType.v("java.lang.String") + ")";
		SootMethodRef ref = G.symOpsClass.getMethod(methodSig).makeRef();
		StaticInvokeExpr rhs = G.staticInvokeExpr(ref, Arrays.asList(new Immediate[]{symOp1, symOp2, op1, op2}));

		G.assign(symLocalfor((Immediate) s.getLeftOp()), rhs);
	}
	
	/**
	 * Returns true if invocation should be reflected in Z3 model.
	 * 
	 */
	private boolean isSupportedByModel(InvokeExpr expr) {
		String m = expr.getMethod().getSignature();
		if (m.equals("<java.lang.String: boolean contains(java.lang.CharSequence)>")) {
			return true;
		}
		return false;
	}
	


    private static String getStr(Unit h, String methodSigAndFileStr) {
        int bci;
        if (h.hasTag("BytecodeOffsetTag"))
            bci = ((BytecodeOffsetTag) h.getTag("BytecodeOffsetTag")).getBytecodeOffset();
        else
            bci = -1;
        int lineNum;
        if (h.hasTag("LineNumberTag"))
            lineNum = ((LineNumberTag) h.getTag("LineNumberTag")).getLineNumber();
        else if (h.hasTag("SourceLineNumberTag"))
            lineNum = ((SourceLineNumberTag) h.getTag("SourceLineNumberTag")).getLineNumber();
        else if (h.hasTag("SourceLnPosTag"))
            lineNum = ((SourceLnPosTag) h.getTag("SourceLnPosTag")).startLn();
        else
            lineNum = 0;
        return bci + "!" + methodSigAndFileStr + lineNum + ")";
    }

    private static String getMethodSigAndFileStr(Body body) {
        SootMethod m = body.getMethod();
        SootClass c = m.getDeclaringClass();
        String fileName;
        if (c.hasTag("SourceFileTag"))
            fileName = ((SourceFileTag) c.getTag("SourceFileTag")).getSourceFile();
        else
            fileName = "unknown_file";
        return m.getSignature() + " (" + fileName + ":";
    }

	/**
	 * Instrument conditions (if branches).
	 * 
	 * @param body
	 */
    private void instrumentConds(Body body) {
		String methodSigAndFileStr = getMethodSigAndFileStr(body);
        int entryCondId = condIdStrList.size();

        // collect all conditional branches in this method
        List<IfStmt> conds = new ArrayList<IfStmt>();
		Chain<Unit> units = body.getUnits();
		Unit lastParamStmt = units.getFirst();
		HashSet<Value> toInitialize = new HashSet<Value>(body.getLocalCount());
        for (Unit u : units) {
        	//Collect all register used in monitor stmts to initialize them to avoid possible VRFY errors
        	if (u instanceof MonitorStmt) {        		
        		Value s = ((MonitorStmt) u).getOp();
        		if (s instanceof Local)
        			toInitialize.add(s);
        	}
        	if (u instanceof IfStmt) {
                conds.add((IfStmt) u);
                String str = getStr(u, methodSigAndFileStr);
                condIdStrList.add(str);
            } else if (u instanceof LookupSwitchStmt || u instanceof TableSwitchStmt) {
                throw new RuntimeException("Unexpected branch stmt kind: " + u);
            }
        }

        if (conds.size() <= 0) {
            //no branches in method -> done
            return;
        }

        Local symVar = G.newLocal(G.EXPRESSION_TYPE);
        for (int i = 0; i < conds.size(); i++) {
			IfStmt ifStmt = conds.get(i);
			int absCondId = entryCondId + i;
			ConditionExpr condExp = (ConditionExpr) ifStmt.getCondition();
			if (condExp.getOp1() instanceof Constant && condExp.getOp2() instanceof Constant) {
				// Only constants are compared. No need for symbolic tracing
				continue;
			}
			//Check UD chain for references to String methods TODO UD chain is only intraprocedural. Extend to interproc. backwards propagation
			Value realV = null;
			if (condExp.getOp1() instanceof Local) {
				List<Unit> defs = generateUseDefChain(body, ifStmt, (Local) condExp.getOp1());
				ListIterator<Unit> it = defs.listIterator(defs.size());
				while (it.hasPrevious()) {
					Unit def = it.previous();
					System.out.println(def instanceof AssignStmt);
					
					if (def instanceof AssignStmt && ((AssignStmt) def).containsInvokeExpr()) {
						Unit symDef = units.getPredOf(def);
						if (symDef instanceof AssignStmt && ((AssignStmt) symDef).containsInvokeExpr() && ((AssignStmt) symDef).getInvokeExpr().getMethod().getDeclaringClass().getName().equals(G.SYMOPS_CLASS_NAME)) {
							SootMethod m = ((AssignStmt) symDef).getInvokeExpr().getMethod();
							
							if (m.getName().equals("_contains")) { //TODO Handle more operations supported by Z3 here
								realV = ((AssignStmt) symDef).getLeftOp();
								break;
							}
						} else {
							log.warn("No symbolic counterpart found for " + def);
						}
					}
				}
			}
			IntConstant condId = IntConstant.v(absCondId);

			// Assign symbolic value of concrete expr 'condExp' to local var 'symVar'.
			Value v = handleBinopExpr(condExp, false, localsMap);

			if(v == null)
				v = NullConstant.v();
			if (realV !=null) { //TODO fixme! Assign leftOp of stmt before "stuff", if stmt is staticinvoke symbolicoperations._contains
				v = realV;
			}
			Stmt symAsgnStmt = G.jimple.newAssignStmt(symVar, v);

			Stmt assumeFlsStmt, assumeTruStmt;
			assumeFlsStmt = G.jimple.newInvokeStmt(G.staticInvokeExpr(G.assume,
				Arrays.asList(new Immediate[]{symVar, condId, IntConstant.v(0)})));
			assumeTruStmt = G.jimple.newInvokeStmt(G.staticInvokeExpr(G.assume,
				Arrays.asList(new Immediate[]{symVar, condId, IntConstant.v(1)})));

			Stmt oldTarget = ifStmt.getTarget();
			
			//Insert symbolic condition before concrete if-statement
			units.insertBefore(symAsgnStmt, ifStmt);
						
			//Insert symbolic "false" assumption immediately after concrete if-statement
			units.insertAfter(assumeFlsStmt, ifStmt);
			
			/* 
			 * The layout of statements will be reordered like this:
			 * 
			 * 			symAsgnStmt
			 *			if <condition> goto assumeTrue
			 *			assume false
			 *			goto oldTarget			  (gotoOldTargetStmt1)
			 * assumeTrue:
			 *			assume true				  (assumeTruStmt)
			 * 			goto oldTarget            (gotoOldTargetStmt2)
			 * oldTarget:
			 *          <original true branch>
			 */
			Stmt gotoOldTargetStmt1 = G.jimple.newGotoStmt(oldTarget);
			Stmt gotoOldTargetStmt2 = G.jimple.newGotoStmt(oldTarget);
			System.out.println("Insert before old target: " + gotoOldTargetStmt2.toString());
			((PatchingChain) units).insertBeforeNoRedirect(gotoOldTargetStmt2, oldTarget);
			
			System.out.println("Insert before old target: " + assumeTruStmt.toString());
			((PatchingChain) units).insertBeforeNoRedirect(assumeTruStmt, gotoOldTargetStmt2);
			
			System.out.println("Insert before old target: " + gotoOldTargetStmt1.toString());
			((PatchingChain) units).insertBeforeNoRedirect(gotoOldTargetStmt1, assumeTruStmt);
			
			//Let if-statement jump to "assume: true" assumption 
			ifStmt.setTarget(assumeTruStmt);
		}

        //Insert variable initialization at begin of method to avoid VRFY error
    	for (Value v: toInitialize) {
    		Stmt initStmt = G.jimple.newAssignStmt(v, NullConstant.v());
            units.insertAfter(initStmt, lastParamStmt);
    	}
        Stmt symInitStmt = G.jimple.newAssignStmt(symVar, NullConstant.v());    	
    	units.insertAfter(symInitStmt, lastParamStmt);

	}

	private void insertPrologue(Body body, List<Local> params)
	{
		Chain<Unit> units = body.getUnits().getNonPatchingChain();
		for (Unit u : units) {
			Stmt s = (Stmt) u;
			if (paramOrThisIdentityStmt(s)) {
				continue;
			}
			else {
				Local symArgsArray = G.jimple.newLocal(new String("a3targs$symargs"), ArrayType.v(G.EXPRESSION_TYPE, 1));
				body.getLocals().addFirst(symArgsArray);
				int subsigId = methSubsigNumberer.getOrMakeId(currentMethod);
				units.insertBefore(G.jimple.newAssignStmt(symArgsArray, G.staticInvokeExpr(G.argPop,
					IntConstant.v(subsigId), IntConstant.v(sigIdOfCurrentMethod), IntConstant.v(params.size()))), s);
				for(int i = 0; i < params.size(); i++){
					Local l = params.get(i);
					if(addSymLocationFor(l.getType())) {
						units.insertBefore(G.jimple.newAssignStmt(symLocalfor(l),
																  G.jimple.newArrayRef(symArgsArray,IntConstant.v(i))), s);
					}
				}
				break;
			}
		}
	}

	private void handleInvokeExpr(Stmt s) {
		InvokeExpr ie = s.getInvokeExpr();
		List symArgs = new ArrayList();
		SootMethod callee = ie.getMethod();
		int subSig = methSubsigNumberer.getOrMakeId(callee);
		
		// Insert TARGET marker, if invocation of target method
		if (TARGET_METHODS.contains(ie.getMethod().getSignature())) {
			Local targetName = G.newLocal(RefType.v("java.lang.String"));
			G.assign(targetName, StringConstant.v(ie.getMethod().getSignature()));
			G.invoke(G.staticInvokeExpr(G.targetHit, targetName));
		}
		
		//pass the subsig of the callee
		symArgs.add(IntConstant.v(subSig));
		
		List args = new ArrayList();
		
		//Handle symbolic base register (if any)
		if (ie instanceof InstanceInvokeExpr) {
			Immediate base = (Immediate) ((InstanceInvokeExpr) ie).getBase();
			args.add(base);
			symArgs.add(symLocalfor(base));
			//symArgs.add(NullConstant.v());
		}
		
		//Handle symbolic arguments (if any)
		for (Iterator it = ie.getArgs().iterator(); it.hasNext();) {
			Immediate arg = (Immediate) it.next();
			args.add(arg);
			symArgs.add(addSymLocationFor(arg.getType()) ? symLocalfor(arg) : NullConstant.v());
		}

		G.invoke(G.staticInvokeExpr(G.argPush[symArgs.size()-1], symArgs));

		if (s instanceof AssignStmt) {
			Local retValue = (Local) ((AssignStmt) s).getLeftOp();
			if(addSymLocationFor(retValue.getType())) {
				// Force solution value to drive execution down the new path.
				SootMethod modelInvoker = ModelMethodsHandler.getModelInvokerFor(callee);
				if (modelInvoker != null) {
					G.editor.insertStmtAfter(G.jimple.newAssignStmt(retValue, G.staticInvokeExpr(G.getSolution_long, StringConstant.v(toSymbolicVarName(callee)))));
				}

				G.editor.insertStmtAfter(G.jimple.newAssignStmt(symLocalfor(retValue),
																G.staticInvokeExpr(G.retPop, IntConstant.v(subSig))));
				
				if (modelInvoker != null) {
					G.editor.insertStmtAfter(G.jimple.newInvokeStmt(G.staticInvokeExpr(modelInvoker.makeRef(), args)));
				}
			}
		}
	}

	/**
	 * Converts Soot method name to symbolic variable representing its return value.
	 * 
	 * @param callee
	 * @return
	 */
	private String toSymbolicVarName(SootMethod callee) {
		String name =callee.getBytecodeSignature();
		name = name.replace(".", "_");
		name = name.replace(": ", "_");
		name = name.replace('(', '_');
		name = name.replace(')', '_');
		name = name.replace(',', '_');
		name = name.replace("<","").replace(">","");
		
		String t = callee.getReturnType().toString();
		if (t.equals("long")) {
			t = "L";
		} else if (t.equals("int")) {
			t = "I";
		} //TODO handle more types.
		
		return "$"+t+"$sym_"+name;
	}
	

	/**
	 * Converts field name to symbolic variable name.
	 * 
	 * @param fld
	 * @return
	 */
	private String toSymbolicVarName(SootField fld) {
		String t = fld.getType().toString();
		if (t.equals("java.lang.String")) {
			t = "X";
		}  else if (t.equals("int")) {
			t = "I";
		} //TODO handle more types
		String name = fld.getSignature();
		name = name.replace('.', '_');
		name = name.replace(':', '_');
		name = name.replace(' ', '_');
		name = name.replace(',', '_');
		name = name.replace("<","").replace(">","");
		return "$"+t+"$sym_"+name;
	}

	

	/**
	 * Called by soot.util.Switchable.apply()
	 */
	@Override
	public void caseAssignStmt(AssignStmt as)
	{
		Value rightOp = as.getRightOp();
		Value leftOp = as.getLeftOp();

		if (rightOp instanceof BinopExpr) {
			handleBinopStmt((Local) leftOp, (BinopExpr) rightOp);
		}
		if (rightOp instanceof NegExpr) {
			handleNegStmt((Local) leftOp, (NegExpr) rightOp);
		}
		else if (leftOp instanceof FieldRef) {
			handleStoreStmt((FieldRef) leftOp, (Immediate) rightOp);
		}
		else if (rightOp instanceof FieldRef) {
			handleLoadStmt((Local) leftOp, (FieldRef) rightOp);
		}
		else if (leftOp instanceof ArrayRef) {
			handleArrayStoreStmt((ArrayRef) leftOp, (Immediate) rightOp);
		}
		else if (rightOp instanceof ArrayRef) {
			handleArrayLoadStmt((Local) leftOp, (ArrayRef) rightOp);
		}
		else if (rightOp instanceof LengthExpr) {
			handleArrayLengthStmt((Local) leftOp, (LengthExpr) rightOp);
		}
		else if (rightOp instanceof InstanceOfExpr) {
			handleInstanceOfStmt((Local) leftOp, (InstanceOfExpr) rightOp);
		}
		else if (rightOp instanceof CastExpr) {
			handleCastExpr((Local) leftOp, (CastExpr) rightOp);
		}
		else if (rightOp instanceof NewExpr) {
			handleNewStmt((Local) leftOp, (NewExpr) rightOp);
		}
		else if (rightOp instanceof NewArrayExpr) {
			handleNewArrayStmt((Local) leftOp, (NewArrayExpr) rightOp);
		}
		else if (rightOp instanceof NewMultiArrayExpr) {
			handleNewMultiArrayStmt((Local) leftOp, (NewMultiArrayExpr) rightOp);
		}
		else if (rightOp instanceof Immediate && leftOp instanceof Local) {
			handleSimpleAssignStmt((Local) leftOp, (Immediate) rightOp);
		} else {
			System.out.println("Unhandled assign stmt: " + as);
		}
	}

	@Override
	public void caseIdentityStmt(IdentityStmt is) {
		if (!(is.getRightOp() instanceof CaughtExceptionRef))
			assert false : "unexpected " + is;
	}

	void handleBinopStmt(Local leftOp, BinopExpr binExpr) {
		Local leftOp_sym = localsMap.get(leftOp);
		Value rightOp_sym = handleBinopExpr(binExpr, false, localsMap);
		if (rightOp_sym == null)
			rightOp_sym = NullConstant.v();
		G.assign(leftOp_sym, rightOp_sym);
	}

	void handleNegStmt(Local leftOp, NegExpr negExpr) {
		Local lefOp_sym = localsMap.get(leftOp);
		Immediate operand = (Immediate) negExpr.getOp();
		Value rightOp_sym;
		if (operand instanceof Constant) {
			rightOp_sym = NullConstant.v();
		} else {
			String methodSig = G.EXPRESSION_CLASS_NAME + " " + G.negMethodName +
				"(" + G.EXPRESSION_CLASS_NAME + ")";
			SootMethodRef ref = G.symOpsClass.getMethod(methodSig).makeRef();
			Local operand_sym = (Local) localsMap.get(operand);
			rightOp_sym = G.staticInvokeExpr(ref, operand_sym);
       	}
		G.assign(lefOp_sym, rightOp_sym);
	}

	void handleSimpleAssignStmt(Local leftOp, Immediate rightOp) {
		if(!addSymLocationFor(leftOp.getType()))
			return;
		G.assign(symLocalfor(leftOp), symLocalfor(rightOp));
		
		// Handle system constant which should be replaced by solutions.
		Local retValue = (Local) leftOp;
	}

	void handleStoreStmt(FieldRef leftOp, Immediate rightOp) {
		Immediate base;
		if (leftOp instanceof StaticFieldRef) {
			base = NullConstant.v();
		} else {
			base = (Local) ((InstanceFieldRef) leftOp).getBase();
		}

		SootField fld = leftOp.getField();
		if (!Main.isInstrumented(fld.getDeclaringClass())&&!fld.getDeclaringClass().getName().contains("android.os")) 
			return;

		if(addSymLocationFor(fld.getType()) && fieldsMap.containsKey(fld)) {
			
			SootField fld_sym = fieldsMap.get(fld);
			assert fld_sym != null : "No sym var for " + fld + " " + fld.getDeclaringClass();
			FieldRef leftOp_sym;
			if (leftOp instanceof StaticFieldRef) {
				leftOp_sym = G.staticFieldRef(fld_sym.makeRef());
			} else {
				leftOp_sym = G.instanceFieldRef(base, fld_sym.makeRef());
			}
			G.assign(leftOp_sym, symLocalfor(rightOp));
		} 

		//Fields defined in application will be considered by symbolic rw/ww ops
		if (doRW(fld)) {
			int fld_id = fieldSigNumberer.getOrMakeId(fld);
			if (rwKind == RWKind.ID_FIELD_WRITE) {
				SootField idFld = idFieldsMap.get(fld);
				if(idFld != null) {
					FieldRef leftOp_id, leftOp_id1; 
					if (leftOp instanceof StaticFieldRef) {
						leftOp_id = G.staticFieldRef(idFld.makeRef());
						leftOp_id1 = G.staticFieldRef(idFld.makeRef());
					}
					else {
						leftOp_id = G.instanceFieldRef(base, idFld.makeRef());
						leftOp_id1 = G.instanceFieldRef(base, idFld.makeRef());
					}
					Local tmp = G.newLocal(IntType.v());
					G.assign(tmp, leftOp_id);
					G.invoke(G.staticInvokeExpr(G.ww, tmp, IntConstant.v(fld_id)));
					G.assign(tmp, G.staticInvokeExpr(G.eventId));
					G.assign(leftOp_id1, tmp);
				}
				G.invoke(G.staticInvokeExpr(G.only_write, IntConstant.v(fld_id)));
			} else if (rwKind == RWKind.EXPLICIT_WRITE) {
				G.invoke(G.staticInvokeExpr(G.explicit_write, base, IntConstant.v(fld_id)));
			} else if (rwKind == RWKind.ONLY_WRITE) {
				G.invoke(G.staticInvokeExpr(G.only_write, IntConstant.v(fld_id)));
			}
		}
	}
	
	
	/**
	 * Create symbolic counterpart of fields, even if not part of application.
	 * 
	 * @param fld
	 * @return
	 */
	private SootField retrieveSymbolicStringField(SootField fld) {
		SootClass c = null;
		String modelledClassName = "models."+fld.getDeclaringClass().getName();
		
		if (!Scene.v().containsClass(modelledClassName)) {
			c = MethodUtils.createClass(modelledClassName);
		} else {
			c = Scene.v().getSootClass(modelledClassName);
		}
		String fldName = fld.getName()+"$sym";
		SootField symField = null;
		if (c.declaresFieldByName(fldName)) {
			symField = c.getFieldByName(fldName);
		} else {
			symField = new SootField(fldName, G.EXPRESSION_TYPE, fld.getModifiers());
			c.addField(symField);
		}		
			
		//Assign non-clinit Method value to field TODO Possibly unneeded. Already created by G.createClass()
		SootMethod clinitMethod = null;
		  if (!c.declaresMethod("<clinit>", new ArrayList(), soot.VoidType.v())) {
			//Add static initializer
		    clinitMethod = new soot.SootMethod("<clinit>", new ArrayList(), soot.VoidType.v(), soot.Modifier.STATIC, new ArrayList<SootClass>());                
		    clinitMethod.setActiveBody(Jimple.v().newBody(clinitMethod));
		    c.addMethod(clinitMethod);
		} else {
		    clinitMethod = c.getMethod("<clinit>", new ArrayList(), soot.VoidType.v());
		}		  
		Body initializer = clinitMethod.retrieveActiveBody();
		PatchingChain<Unit> units = initializer.getUnits();

		//Initialize field in static initializer
		if (symField.isStatic()) {				
			RefType fieldType = RefType.v("acteve.symbolic.integer.Expression");
			RefType fieldSpecificType = RefType.v("acteve.symbolic.string.SymbolicString");
			
			Local loc = Jimple.v().newLocal(toSymbolicVarName(fld),fieldType);
			initializer.getLocals().add(loc);
			
			SootMethodRef ctorRef = fieldSpecificType.getSootClass().getMethod("void <init>(java.lang.String)").makeRef();

			AssignStmt newStrCnstStmt = Jimple.v().newAssignStmt(loc, Jimple.v().newNewExpr(RefType.v("acteve.symbolic.string.SymbolicString")));
			InvokeStmt invokeCnstrctrStmt = Jimple.v().newInvokeStmt(Jimple.v().newSpecialInvokeExpr(loc, ctorRef,StringConstant.v(toSymbolicVarName(fld))));
			AssignStmt assignToSymFld = Jimple.v().newAssignStmt(Jimple.v().newStaticFieldRef(symField.makeRef()), loc);
		
			Iterator<Unit> it = units.iterator();
			Unit insertPoint = null;
			if (units.size()>0) {
				while (it.hasNext() && !((insertPoint = it.next()) instanceof JReturnVoidStmt)) { /* fast forward until return ... */ }
			} else {
				insertPoint = Jimple.v().newReturnVoidStmt();
				units.add(insertPoint);
			}
			units.insertBefore(newStrCnstStmt, insertPoint);
			units.insertBefore(invokeCnstrctrStmt, insertPoint);
			units.insertBefore(assignToSymFld, insertPoint);
		
		} else {
			log.error("Field is not static " + fld);
		}		
		return symField;
	}
	
	
	void handleLoadStmt(Local leftOp, FieldRef rightOp) 
	{
		Immediate base;
		if (rightOp instanceof StaticFieldRef) {
			base = NullConstant.v();
		} else {
			base = (Local) ((InstanceFieldRef) rightOp).getBase();
		}

		SootField fld = rightOp.getField();
		Local leftOp_sym = localsMap.get(leftOp);
		if (!Main.isInstrumented(fld.getDeclaringClass()) && !Config.g().fieldsToModel.contains(fld.toString())) {
			if(leftOp_sym != null)
				G.assign(leftOp_sym, NullConstant.v());
			return;
		}
		System.out.println("Tracing field "+fld.toString());
		if(addSymLocationFor(fld.getType())) {
			
			if (!fieldsMap.containsKey(fld)) {
				//Create new symbolic field
				SootField symField = retrieveSymbolicStringField(fld);
				fieldsMap.put(fld, symField);
			}
			
			SootField fld_sym = fieldsMap.get(fld);
			assert fld_sym != null : fld + " " + fld.getDeclaringClass();
			FieldRef rightOp_sym;
			if (rightOp instanceof StaticFieldRef) {
				rightOp_sym = G.staticFieldRef(fld_sym.makeRef());
			} else {
				rightOp_sym = G.instanceFieldRef(base, fld_sym.makeRef());
			}
			G.assign(leftOp_sym, rightOp_sym);
		} else if(leftOp_sym != null) {
			G.assign(leftOp_sym, NullConstant.v());
		}

        if (doRW(fld)) {
			if (rwKind == RWKind.ID_FIELD_WRITE) {
				SootField fld_id = idFieldsMap.get(fld);
				if(fld_id != null) {
					FieldRef rightOp_id;
					if (rightOp instanceof StaticFieldRef)
						rightOp_id = G.staticFieldRef(fld_id.makeRef());
					else
						rightOp_id = G.instanceFieldRef(base, fld_id.makeRef());
					Local tmp = G.newLocal(IntType.v());
					G.assign(tmp, rightOp_id);
					int id = fieldSigNumberer.getOrMakeId(fld);
					G.invoke(G.staticInvokeExpr(G.rw, tmp, IntConstant.v(id)));
					//G.invoke(G.staticInvokeExpr(G.id_field_read, IntConstant.v(id)));
				}
			} else if (rwKind == RWKind.EXPLICIT_WRITE) {
				// TODO
			}
        }

		//Overwrite concrete value with solution
		if (ModelMethodsHandler.modelExistsFor(fld)) { //Modelled?
			if (fld.getType() instanceof PrimType || fld.getType().toString().equals("java.lang.String")) { //Supported type?
				G.editor.insertStmtAfter(Jimple.v().newAssignStmt(leftOp, G.staticInvokeExpr(G.getSolution_string, StringConstant.v(toSymbolicVarName(fld)))));
			} else {
				log.error("Modelled field of non-supported type: " + fld.getName() + " : " + fld.getType());
			}
		} else {
			log.debug("Not modelled: " + fld.toString());
		}
	}


	void handleNewStmt(Local leftOp, NewExpr rightOp)
	{
		Local leftOp_sym = localsMap.get(leftOp);
		if(leftOp_sym != null)
			G.editor.insertStmtAfter(G.jimple.newAssignStmt(leftOp_sym, NullConstant.v()));
	}

	void handleNewArrayStmt(Local leftOp, NewArrayExpr rightOp)
	{
		Local leftOp_sym = localsMap.get(leftOp);
		if(leftOp_sym != null)
			G.editor.insertStmtAfter(G.jimple.newAssignStmt(leftOp_sym, NullConstant.v()));
	}

	void handleNewMultiArrayStmt(Local leftOp, NewMultiArrayExpr rightOp) {
		//Local leftOp_sym = localsMap.get(leftOp);
		//G.editor.insertStmtAfter(G.jimple.newAssignStmt(leftOp_sym, NullConstant.v()));
	}
	
	@Override
	public void caseReturnStmt(ReturnStmt rs) {
		Immediate retValue = (Immediate) rs.getOp();
		if(!addSymLocationFor(retValue.getType()))
			return;
		int subSig = methSubsigNumberer.getOrMakeId(currentMethod);
		G.invoke(G.staticInvokeExpr(G.retPush, IntConstant.v(subSig), symLocalfor(retValue)));
	}

	public void caseReturnVoidStmt(ReturnStmt rs) {	
	}

	void handleCastExpr(Local leftOp, CastExpr castExpr) {
		if(!addSymLocationFor(leftOp.getType()))
			return;
		Local leftOp_sym = localsMap.get(leftOp);
		Immediate rightOp = (Immediate) castExpr.getOp();
		Type type = castExpr.getCastType();
		if (rightOp instanceof Constant) {
           	G.assign(leftOp_sym, NullConstant.v());
		} else {
			Local op_sym = localsMap.get((Local) rightOp);
			if (op_sym != null) {
				if (type instanceof PrimType) {
					SootMethodRef ref = G.symOpsClass.getMethodByName(G.castMethodName).makeRef();
					Integer t = G.typeMap.get(type);
					if(t == null)
						throw new RuntimeException("unexpected type " + type);
					G.assign(leftOp_sym, G.staticInvokeExpr(ref, op_sym, IntConstant.v(t.intValue())));
				} else {
					//TODO: now sym values corresponding non-primitive types
					//flow through cast operations similar to assignment operation
					G.assign(leftOp_sym, op_sym);
				}
			}
		}
	}

	void handleArrayLoadStmt(Local leftOp, ArrayRef rightOp) {
		Local base = (Local) rightOp.getBase();
		Immediate index = (Immediate) rightOp.getIndex();
		
		Local base_sym = localsMap.get(base);
		Local leftOp_sym = localsMap.get(leftOp);
		if(base_sym != null) {
			Immediate index_sym = index instanceof Constant ? NullConstant.v() : localsMap.get((Local) index);
			Type[] paramTypes = new Type[]{G.EXPRESSION_TYPE, G.EXPRESSION_TYPE, base.getType(), IntType.v()};
			SootMethodRef ref = G.symOpsClass.getMethod(G.arrayGetMethodName, Arrays.asList(paramTypes)).makeRef();
			G.assign(leftOp_sym, G.staticInvokeExpr(ref, Arrays.asList(new Immediate[]{base_sym, index_sym, base, index})));
		} else if(leftOp_sym != null){
			G.assign(leftOp_sym, NullConstant.v());
		}
		if (doRW()) {
			if (rwKind == RWKind.ID_FIELD_WRITE || rwKind == RWKind.EXPLICIT_WRITE)
				G.invoke(G.staticInvokeExpr(G.readArray, base, index));
        }
	}

	void handleArrayLengthStmt(Local leftOp, LengthExpr rightOp) {
		Local leftOp_sym = localsMap.get(leftOp);
		Local base = (Local) rightOp.getOp();
		if(addSymLocationFor(base.getType())){
			Local base_sym = localsMap.get(base);
			SootMethodRef ref = G.symOpsClass.getMethodByName(G.arrayLenMethodName).makeRef();
			G.assign(leftOp_sym, G.staticInvokeExpr(ref, base_sym));
		} else {
			G.assign(leftOp_sym, NullConstant.v());
		}
	}

	void handleArrayStoreStmt(ArrayRef leftOp, Immediate rightOp)
	{
		Local base = (Local) leftOp.getBase();
		Immediate index = (Immediate) leftOp.getIndex();

		Local base_sym = localsMap.get(base);
		if(base_sym != null){
			Immediate index_sym = index instanceof Constant ? NullConstant.v() : localsMap.get((Local) index);
			
			Immediate rightOp_sym = rightOp instanceof Constant ? NullConstant.v() : localsMap.get((Local) rightOp);
			
			Type[] paramTypes = new Type[]{G.EXPRESSION_TYPE, G.EXPRESSION_TYPE, G.EXPRESSION_TYPE,
										   base.getType(), IntType.v(), ((ArrayType) base.getType()).baseType};
			SootMethodRef ref = G.symOpsClass.getMethod(G.arraySetMethodName, Arrays.asList(paramTypes)).makeRef();
			G.invoke(G.staticInvokeExpr(ref, Arrays.asList(new Immediate[]{base_sym, index_sym,
																		   rightOp_sym, base, index, rightOp})));
		}
		if (doRW()) {
			if (rwKind == RWKind.ID_FIELD_WRITE || rwKind == RWKind.EXPLICIT_WRITE)
            	G.invoke(G.staticInvokeExpr(G.writeArray, base, index));
			else if (rwKind == RWKind.ONLY_WRITE)
            	G.invoke(G.staticInvokeExpr(G.only_write, IntConstant.v(-1)));
        }
	}

	void handleInstanceOfStmt(Local leftOp, InstanceOfExpr expr) {
		Local leftOp_sym = localsMap.get(leftOp);
		if(leftOp_sym != null)
			G.assign(leftOp_sym, NullConstant.v());
	}

	/**
	 * Create symbolic counterparts for (some) registers in a method's body.
	 * 
	 * @param body
	 */
	private void addSymLocals(Body body) {
		Chain<Local> locals = body.getLocals();
		Iterator lIt = locals.snapshotIterator();
		while (lIt.hasNext()) {
			Local local = (Local) lIt.next();
			if(!addSymLocationFor(local.getType()))
				continue;
			Local newLocal = G.newLocal(G.EXPRESSION_TYPE, local.getName()+"$sym");
			localsMap.put(local, newLocal);
		}
	}
	
	/**
	 * Returns <code>true</code> if the requested <code>type</code> should be represented by a symbolic counterpart.
	 *  
	 * @param type
	 * @return
	 */
	private boolean addSymLocationFor(Type type) {
		if(type instanceof PrimType)
			return true;
		if(type instanceof ArrayType){
			ArrayType atype = (ArrayType) type;
			return atype.numDimensions == 1 && atype.baseType instanceof PrimType;
		}
		if(type instanceof RefType){
			if(type.equals(G.OBJECT_TYPE))
				return true;
			String className = ((RefType) type).getSootClass().getName();
			if(className.equals("java.io.Serializable") ||
			   className.equals("java.lang.Cloneable")
			   ||  className.equals("java.lang.String")
			   )
				return true;
		}
		return false; //because arrays are subtypes of object
	}
	
	/**
	 * Returns the symbolic counterpart of a register.
	 * @param v
	 * @return
	 */
	private Immediate symLocalfor(Immediate v) {
		if (v instanceof Constant)
			return NullConstant.v();
		else {
			Local l = localsMap.get((Local) v);
			return l == null ? NullConstant.v() : l;
		}
	}

	public static boolean paramOrThisIdentityStmt(Stmt s) {
		if (!(s instanceof IdentityStmt))
			return false;
		return !(((IdentityStmt) s).getRightOp() instanceof CaughtExceptionRef);
	}

    private Value handleBinopExpr(BinopExpr binExpr, boolean negate, Map<Local,Local> localsMap) {
		Immediate op1 = (Immediate) binExpr.getOp1();
        Immediate op2 = (Immediate) binExpr.getOp2();
		
		String binExprSymbol = binExpr.getSymbol().trim();
		if (negate) {
			binExprSymbol = G.negationMap.get(binExprSymbol);
		}
		if (op1 instanceof Constant && op2 instanceof Constant) {
			return null;
		}

		Type op1Type = op1.getType();
		op1Type = op1Type instanceof RefLikeType ? RefType.v("java.lang.Object") : Type.toMachineType(op1Type);

		Type op2Type = op2.getType();
		op2Type = op2Type instanceof RefLikeType ? RefType.v("java.lang.Object") : Type.toMachineType(op2Type);

		Immediate symOp1 = op1 instanceof Constant || op1==null ? NullConstant.v() : localsMap.get((Local) op1);
		Immediate symOp2 = op2 instanceof Constant || op2==null ? NullConstant.v() : localsMap.get((Local) op2);
		
		//TODO There are no symbolics for fields at this point.
		if (symOp1==null)
			symOp1 = NullConstant.v();
		if (symOp2==null)
			symOp2 = NullConstant.v();
		
		String methodName = G.binopSymbolToMethodName.get(binExprSymbol);
		String methodSig = G.EXPRESSION_CLASS_NAME + " " + methodName + "(" + G.EXPRESSION_CLASS_NAME + "," + 
				G.EXPRESSION_CLASS_NAME + "," + op1Type + "," + op2Type + ")";
		SootMethodRef ref = G.symOpsClass.getMethod(methodSig).makeRef();
		return G.staticInvokeExpr(ref, Arrays.asList(new Immediate[]{symOp1, symOp2, op1, op2}));
    }


	private static final String CONDMAP_FILENAME = "condmap.txt";
	private static final String WRITEMAP_FILENAME = "writemap.txt";
	private static final String METH_SUBSIGS_FILENAME = "methsubsigs.txt";
	private static final String METH_SIGS_FILENAME = "methsigs.txt";
	private static final String FIELD_SIGS_FILENAME = "fieldsigs.txt";

	private void loadFiles() {
		if (sdkDir == null)
			return;
		methSubsigNumberer.load(sdkDir + "/" + METH_SUBSIGS_FILENAME);
		methSigNumberer.load(sdkDir + "/" + METH_SIGS_FILENAME);
		fieldSigNumberer.load(sdkDir + "/" + FIELD_SIGS_FILENAME);
		try {
			BufferedReader in = new BufferedReader(new FileReader(sdkDir + "/" + CONDMAP_FILENAME));
			String s;
			while ((s = in.readLine()) != null)
				condIdStrList.add(s);
			in.close();
		} catch (IOException ex) {
			ex.printStackTrace();
			System.exit(1);
		}
	}

	private void saveFiles() {
		methSubsigNumberer.save(outDir + "/" + METH_SUBSIGS_FILENAME);
		methSigNumberer.save(outDir + "/" + METH_SIGS_FILENAME);
		fieldSigNumberer.save(outDir + "/" + FIELD_SIGS_FILENAME);
        try {
            PrintWriter out;

            out = new PrintWriter(new File(outDir + "/" + CONDMAP_FILENAME));
            for (int i = 0; i < condIdStrList.size(); i++) {
                String s = condIdStrList.get(i);
                out.println(s);
            }
            out.close();
        } catch (IOException ex) {
            log.error(ex.getMessage(),ex);
            System.exit(1);
        }
    }
	
	private List<Unit> generateUseDefChain(Body body, Unit u, Local l) {
		UnitGraph unitGraph = new BriefUnitGraph(body);
		SimpleLocalDefs simpleLocalDefs = new SimpleLocalDefs(unitGraph);
		List<Unit> defUnits = simpleLocalDefs.getDefsOfAt(l, u);
		return defUnits;
	}
}

