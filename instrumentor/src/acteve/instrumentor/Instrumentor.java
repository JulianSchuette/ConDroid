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

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.Collections;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Arrays;
import java.io.PrintWriter;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.File;
import java.io.IOException;
import java.util.regex.Pattern;

import acteve.symbolic.A3TInstrumented;
import acteve.symbolic.A3TNative;

import soot.Scene;
import soot.Body;
import soot.Immediate;
import soot.Local;
import soot.Modifier;
import soot.PrimType;
import soot.RefType;
import soot.RefLikeType;
import soot.SootClass;
import soot.SootField;
import soot.SootMethod;
import soot.Type;
import soot.Value;
import soot.Unit;
import soot.VoidType;
import soot.ArrayType;
import soot.IntType;
import soot.SootMethodRef;
import soot.FastHierarchy;
import soot.jimple.AbstractStmtSwitch;
import soot.jimple.ArrayRef;
import soot.jimple.AssignStmt;
import soot.jimple.BinopExpr;
import soot.jimple.CastExpr;
import soot.jimple.CaughtExceptionRef;
import soot.jimple.Constant;
import soot.jimple.FieldRef;
import soot.jimple.GotoStmt;
import soot.jimple.IdentityStmt;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.InstanceOfExpr;
import soot.jimple.IntConstant;
import soot.jimple.InstanceFieldRef;
import soot.jimple.InterfaceInvokeExpr;
import soot.jimple.InvokeExpr;
import soot.jimple.LengthExpr;
import soot.jimple.NegExpr;
import soot.jimple.EqExpr;
import soot.jimple.NewArrayExpr;
import soot.jimple.NewExpr;
import soot.jimple.NewMultiArrayExpr;
import soot.jimple.NullConstant;
import soot.jimple.ReturnStmt;
import soot.jimple.SpecialInvokeExpr;
import soot.jimple.StaticFieldRef;
import soot.jimple.StaticInvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.VirtualInvokeExpr;
import soot.util.Chain;
import soot.PatchingChain;
import soot.jimple.ConditionExpr;
import soot.jimple.IfStmt;
import soot.jimple.NullConstant;
import soot.jimple.TableSwitchStmt;
import soot.jimple.LookupSwitchStmt;
import soot.tagkit.SourceFileTag;
import soot.tagkit.LineNumberTag;
import soot.tagkit.SourceLineNumberTag;
import soot.tagkit.SourceLnPosTag;
import soot.tagkit.BytecodeOffsetTag;
import soot.toolkits.graph.PostDominatorAnalysis;
import soot.toolkits.graph.DirectedGraph;
import soot.toolkits.graph.UnitGraph;
import soot.toolkits.graph.ExceptionalUnitGraph;


public class Instrumentor extends AbstractStmtSwitch {
	private final RWKind rwKind;
	private final String outDir;
	private final String sdkDir;
    private final MethodSubsigNumberer methSubsigNumberer;
    private final MethodSigNumberer methSigNumberer;
    private final FieldSigNumberer fieldSigNumberer;
	private final Filter fieldsWhitelist;
	private final Filter fieldsBlacklist;
	private final Filter methodsWhitelist;
	private final boolean instrAllFields;
	private final Map<SootField, SootField> fieldsMap;
	private final Map<SootField, SootField> idFieldsMap;
    private final List<String> condIdStrList;
	//private final Map<Integer, Set<Integer>> writeMap;
	// map from a original local to its corresponding shadow local
	private final Map<Local, Local> localsMap;
	//private Set<Integer> currentWriteSet;
	private SootMethod currentMethod;
	private int sigIdOfCurrentMethod;

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

	public void instrument(List<SootClass> classes) {
		for (SootClass klass : classes) {
			klass.setApplicationClass();
			addSymbolicFields(klass);
			System.out.println(klass.getName());
		}

		loadFiles();

		for (SootClass klass : classes) {
			List<SootMethod> origMethods = klass.getMethods();
			for (SootMethod m : origMethods) {
				if (!m.isConcrete())
					continue;

				if (ModelMethodsHandler.modelExistsFor(m)) {
					// do not instrument method if a model for it exists
					System.out.println("skipping instrumentation of " + m);
					continue;
				}
				instrument(m);
			}
		}

		saveFiles();
	}

	private void addSymbolicFields(SootClass c) {
		for (Iterator<SootField> it = c.getFields().snapshotIterator(); it.hasNext();) {
			SootField origField = (SootField) it.next();
			if(addSymLocationFor(origField.getType())) {
				SootField symField = new SootField(origField.getName()+"$sym",
												   G.EXPRESSION_TYPE, origField.getModifiers());
				c.addField(symField);
				fieldsMap.put(origField, symField);
			}

			// XXX: idField for tracking writes
            if(rwKind == RWKind.ID_FIELD_WRITE && doRW(origField)){
				SootField idField = new SootField(origField.getName()+"$a3tid", IntType.v(), origField.getModifiers());
				c.addField(idField);
				idFieldsMap.put(origField, idField);
            }
		}
	}

	private boolean hasNativeMethod(SootClass klass) {
		for (SootMethod m : klass.getMethods()) {
			if (m.isNative()) {
				return true;
			}
		}
		return false;
	}

	private void instrument(SootMethod method) {
		SwitchTransformer.transform(method);
 		localsMap.clear();
		//currentWriteSet = new HashSet<Integer>();
		//writeMap.put(sigIdOfCurrentMethod, currentWriteSet);

		System.out.println("Instrumenting " + method);

		Body body = method.retrieveActiveBody();		
		G.editor.newBody(body, method);
		addSymLocals(body);
		List<Local> params = new ArrayList();
	
		currentMethod = method;
		sigIdOfCurrentMethod = methSigNumberer.getOrMakeId(method);

		while (G.editor.hasNext()) {
			Stmt s = G.editor.next();
			if (paramOrThisIdentityStmt(s)) {
				params.add((Local) ((IdentityStmt) s).getLeftOp());
			} else if (s.containsInvokeExpr()) {
				handleInvokeExpr(s);
			} else if (!s.branches())
				s.apply(this);
		}

		//it is done at the end for a good reason
		insertPrologue(body, params);
		
		instrumentConds(body);
		
		G.debug(method, G.DEBUG);
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

	private void instrumentConds(Body body) {
		String methodSigAndFileStr = getMethodSigAndFileStr(body);
        int entryCondId = condIdStrList.size();

        // collect all conditional branches in this method
        List<IfStmt> conds = new ArrayList<IfStmt>();
		Chain<Unit> units = body.getUnits();
        for (Unit u : units) {
            if (u instanceof IfStmt) {
                conds.add((IfStmt) u);
                String str = getStr(u, methodSigAndFileStr);
                condIdStrList.add(str);
            } else if (u instanceof LookupSwitchStmt || u instanceof TableSwitchStmt) {
                throw new RuntimeException("Unexpected branch stmt kind: " + u);
            }
        }

        if (conds.size() <= 0) {
            //no branches in the method
            return;
        }

        Local symVar = G.newLocal(G.EXPRESSION_TYPE);
        for (int i = 0; i < conds.size(); i++) {
			IfStmt ifStmt = conds.get(i);
			int absCondId = entryCondId + i;
			ConditionExpr condExp = (ConditionExpr) ifStmt.getCondition();
			if (condExp.getOp1() instanceof Constant && condExp.getOp2() instanceof Constant)
				continue;
			IntConstant condId = IntConstant.v(absCondId);

			// Assign symbolic value of concrete expr 'condExp' to local var 'symVar'.
			Value v = handleBinopExpr(condExp, false, localsMap);
			if(v == null)
				v = NullConstant.v();
			Stmt symAsgnStmt = G.jimple.newAssignStmt(symVar, v);

			Stmt assumeFlsStmt, assumeTruStmt;
			assumeFlsStmt = G.jimple.newInvokeStmt(G.staticInvokeExpr(G.assume,
				Arrays.asList(new Immediate[]{symVar, condId, IntConstant.v(0)})));
			assumeTruStmt = G.jimple.newInvokeStmt(G.staticInvokeExpr(G.assume,
				Arrays.asList(new Immediate[]{symVar, condId, IntConstant.v(1)})));

			Stmt oldTarget = ifStmt.getTarget();

			units.insertBefore(symAsgnStmt, ifStmt);
			units.insertAfter(assumeFlsStmt, ifStmt);
			Stmt gotoOldTargetStmt1 = G.jimple.newGotoStmt(oldTarget);
			Stmt gotoOldTargetStmt2 = G.jimple.newGotoStmt(oldTarget);
			((PatchingChain) units).insertBeforeNoRedirect(gotoOldTargetStmt2, oldTarget);
			((PatchingChain) units).insertBeforeNoRedirect(assumeTruStmt, gotoOldTargetStmt2);
			((PatchingChain) units).insertBeforeNoRedirect(gotoOldTargetStmt1, assumeTruStmt);
			ifStmt.setTarget(assumeTruStmt);
		}
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
				boolean isSymbolic = Annotation.isSymbolicMethod(currentMethod);

				if (isSymbolic) {
					System.out.println("symbolic = " + isSymbolic);
					SootMethod injector = InputMethodsHandler.addInjector(currentMethod);
					List ps = new ArrayList(params);
					if (!currentMethod.isStatic()) {
						ps.remove(0);
					}
					units.insertBefore(G.jimple.newInvokeStmt(G.staticInvokeExpr(injector.makeRef(), ps)), s);
				}
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

	private void handleInvokeExpr(Stmt s)
	{
		InvokeExpr ie = s.getInvokeExpr();
		List symArgs = new ArrayList();
		SootMethod callee = ie.getMethod();
		int subSig = methSubsigNumberer.getOrMakeId(callee);
		
		//pass the subsig of the callee
		symArgs.add(IntConstant.v(subSig));
		
		List args = new ArrayList();
		if (ie instanceof InstanceInvokeExpr) {
			Immediate base = (Immediate) ((InstanceInvokeExpr) ie).getBase();
			args.add(base);
			//symArgs.add(symLocalfor(base));
			symArgs.add(NullConstant.v());
		}
		for (Iterator it = ie.getArgs().iterator(); it.hasNext();) {
			Immediate arg = (Immediate) it.next();
			args.add(arg);
			symArgs.add(addSymLocationFor(arg.getType()) ? symLocalfor(arg) : NullConstant.v());
		}

		G.invoke(G.staticInvokeExpr(G.argPush[symArgs.size()-1], symArgs));

		if (s instanceof AssignStmt) {
			Local retValue = (Local) ((AssignStmt) s).getLeftOp();
			if(addSymLocationFor(retValue.getType())) {
				G.editor.insertStmtAfter(G.jimple.newAssignStmt(symLocalfor(retValue),
																G.staticInvokeExpr(G.retPop, IntConstant.v(subSig))));
				
				SootMethod modelInvoker = ModelMethodsHandler.getModelInvokerFor(callee);
				if (modelInvoker != null) {
					G.editor.insertStmtAfter(G.jimple.newInvokeStmt(G.staticInvokeExpr(modelInvoker.makeRef(), args)));
				}
			}
		}
	}

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
		}
	}

	public void caseIdentityStmt(IdentityStmt is)
	{
		if (!(is.getRightOp() instanceof CaughtExceptionRef))
			assert false : "unexpected " + is;
		
		//Local leftOp = (Local) is.getLeftOp();
		//Local leftOp_sym = localsMap.get(leftOp);
		////TODO: track meta data for exceptional objects
		//G.editor.insertStmtAfter(G.jimple.newAssignStmt(leftOp_sym, NullConstant.v()));
	}

	void handleBinopStmt(Local leftOp, BinopExpr binExpr)
	{
		Local leftOp_sym = localsMap.get(leftOp);
		Value rightOp_sym = handleBinopExpr(binExpr, false, localsMap);
		if (rightOp_sym == null)
			rightOp_sym = NullConstant.v();
		G.assign(leftOp_sym, rightOp_sym);
	}

	void handleNegStmt(Local leftOp, NegExpr negExpr)
	{
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

	void handleSimpleAssignStmt(Local leftOp, Immediate rightOp)
	{
		if(!addSymLocationFor(leftOp.getType()))
			return;
		G.assign(symLocalfor(leftOp), symLocalfor(rightOp));
	}

	void handleStoreStmt(FieldRef leftOp, Immediate rightOp) 
	{
		Immediate base;
		if (leftOp instanceof StaticFieldRef) {
			base = NullConstant.v();
		} else {
			base = (Local) ((InstanceFieldRef) leftOp).getBase();
		}

		SootField fld = leftOp.getField();
		if (!Main.isInstrumented(fld.getDeclaringClass())) 
			return;

		if(addSymLocationFor(fld.getType())) {
			SootField fld_sym = fieldsMap.get(fld);
			assert fld_sym != null : fld + " " + fld.getDeclaringClass();
			FieldRef leftOp_sym;
			if (leftOp instanceof StaticFieldRef) {
				leftOp_sym = G.staticFieldRef(fld_sym.makeRef());
			} else {
				leftOp_sym = G.instanceFieldRef(base, fld_sym.makeRef());
			}
			G.assign(leftOp_sym, symLocalfor(rightOp));
		} 

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

		//currentWriteSet.add(fld_id);

		//else if(Instrument.useW) {
            //G.invoke(G.staticInvokeExpr(G.write1, 
			//							IntConstant.v(methSigNumberer.getOrMakeId(method)), 
			//							IntConstant.v(fieldSigNumberer.getOrMakeId(fld))));
		//}
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
		if (!Main.isInstrumented(fld.getDeclaringClass())) {
			if(leftOp_sym != null)
				G.assign(leftOp_sym, NullConstant.v());
			return;
		}
		
		if(addSymLocationFor(fld.getType())) {
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

	void handleNewMultiArrayStmt(Local leftOp, NewMultiArrayExpr rightOp)
	{
		//Local leftOp_sym = localsMap.get(leftOp);
		//G.editor.insertStmtAfter(G.jimple.newAssignStmt(leftOp_sym, NullConstant.v()));
	}

	public void caseReturnStmt(ReturnStmt rs)
	{
		Immediate retValue = (Immediate) rs.getOp();
		if(!addSymLocationFor(retValue.getType()))
			return;
		int subSig = methSubsigNumberer.getOrMakeId(currentMethod);
		G.invoke(G.staticInvokeExpr(G.retPush, IntConstant.v(subSig), symLocalfor(retValue)));
	}

	public void caseReturnVoidStmt(ReturnStmt rs)
	{	
	}

	void handleCastExpr(Local leftOp, CastExpr rightOp)
	{
		if(!addSymLocationFor(leftOp.getType()))
			return;
		Local leftOp_sym = localsMap.get(leftOp);
		Immediate op = (Immediate) rightOp.getOp();
		Type type = rightOp.getCastType();
		if (op instanceof Constant) {
           	G.assign(leftOp_sym, NullConstant.v());
		} else {
			Local op_sym = localsMap.get((Local) op);
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

	void handleArrayLoadStmt(Local leftOp, ArrayRef rightOp)
	{
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

	void handleArrayLengthStmt(Local leftOp, LengthExpr rightOp)
	{
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
		
		//currentWriteSet.add(-1);
		//else if(Instrument.useW) {
            //G.invoke(G.staticInvokeExpr(G.write1, 
			//							IntConstant.v(methSigNumberer.getOrMakeId(method)), 
			//							IntConstant.v(-1)));
		//}
	}

	void handleInstanceOfStmt(Local leftOp, InstanceOfExpr expr)
	{
		Local leftOp_sym = localsMap.get(leftOp);
		if(leftOp_sym != null)
			G.assign(leftOp_sym, NullConstant.v());
	}

	private void addSymLocals(Body body)
	{
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
	
	private boolean addSymLocationFor(Type type)
	{
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
			   className.equals("java.lang.Cloneable"))
				return true;
		}
		return false; //because arrays are subtypes of object
	}
	
	private Immediate symLocalfor(Immediate v)
	{
		if (v instanceof Constant)
			return NullConstant.v();
		else {
			Local l = localsMap.get((Local) v);
			return l == null ? NullConstant.v() : l;
		}
	}

	public static boolean paramOrThisIdentityStmt(Stmt s)
	{
		if (!(s instanceof IdentityStmt))
			return false;
		return !(((IdentityStmt) s).getRightOp() instanceof CaughtExceptionRef);
	}

    private Value handleBinopExpr(BinopExpr binExpr, boolean negate, Map<Local,Local> localsMap)
    {
		Immediate op1 = (Immediate) binExpr.getOp1();
        Immediate op2 = (Immediate) binExpr.getOp2();
		
		if(!(op1.getType() instanceof PrimType))
			return null;

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

		Immediate symOp1 = op1 instanceof Constant ? NullConstant.v() : localsMap.get((Local) op1);
		Immediate symOp2 = op2 instanceof Constant ? NullConstant.v() : localsMap.get((Local) op2);
		
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
			
			/*
			out = new PrintWriter(new File(outDir + "/" + WRITEMAP_FILENAME));
			if (sdkDir != null) {
				BufferedReader in = new BufferedReader(new FileReader(sdkDir + "/" + WRITEMAP_FILENAME));
				String s;
				while ((s = in.readLine()) != null)
					out.println(s);
				in.close();
			}
			for (Map.Entry<Integer, Set<Integer>> e : writeMap.entrySet()) {
				Set<Integer> set = e.getValue();
				if (!set.isEmpty()) {
					Integer m = e.getKey();
					Iterator<Integer> it = set.iterator();
					out.print(m + "@" + it.next());
					while (it.hasNext())
						out.print(" " + it.next());
					out.println();
				}
			}
			out.close();
			*/
        } catch (IOException ex) {
            ex.printStackTrace();
            System.exit(1);
        }
    }
}

