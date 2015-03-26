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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import soot.Body;
import soot.BooleanType;
import soot.ByteType;
import soot.CharType;
import soot.DoubleType;
import soot.FloatType;
import soot.Immediate;
import soot.IntType;
import soot.Local;
import soot.LongType;
import soot.PrimType;
import soot.RefType;
import soot.Scene;
import soot.ShortType;
import soot.SootClass;
import soot.SootFieldRef;
import soot.SootMethod;
import soot.SootMethodRef;
import soot.Type;
import soot.Unit;
import soot.Value;
import soot.jimple.AddExpr;
import soot.jimple.AssignStmt;
import soot.jimple.CastExpr;
import soot.jimple.ConditionExpr;
import soot.jimple.EqExpr;
import soot.jimple.IdentityRef;
import soot.jimple.IdentityStmt;
import soot.jimple.InstanceFieldRef;
import soot.jimple.InstanceOfExpr;
import soot.jimple.IntConstant;
import soot.jimple.InterfaceInvokeExpr;
import soot.jimple.InvokeExpr;
import soot.jimple.Jimple;
import soot.jimple.NeExpr;
import soot.jimple.NewExpr;
import soot.jimple.ReturnStmt;
import soot.jimple.ReturnVoidStmt;
import soot.jimple.SpecialInvokeExpr;
import soot.jimple.StaticFieldRef;
import soot.jimple.StaticInvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.SubExpr;
import soot.jimple.VirtualInvokeExpr;
import acteve.symbolic.integer.Types;

/**
   G as in Global
*/
public class G
{
    public static final boolean DEBUG = false;

    public static final Jimple jimple = Jimple.v();

    public static final BodyEditor editor = new BodyEditor();

    public static final String castMethodName     = "_cast";
    public static final String negMethodName      = "_neg";
	public static final String arrayGetMethodName = "_aget";
	public static final String arraySetMethodName = "_aset";
	public static final String arrayLenMethodName = "_alen";

    public static final Map<String, String> binopSymbolToMethodName = new HashMap<String, String>();
    public static final Map<String, String> negationMap = new HashMap<String, String>();
	public static final Map<PrimType, Integer> typeMap = new HashMap<PrimType, Integer>();

    static final String OBJECT_CLASS_NAME = "java.lang.Object";
    static final String STRING_CLASS_NAME = "java.lang.String";
    static final String EXPRESSION_CLASS_NAME = "acteve.symbolic.integer.Expression";
    static final String SYMOPS_CLASS_NAME = "acteve.symbolic.SymbolicOperations";
    static final String SYMUTIL_CLASS_NAME = "acteve.symbolic.Util";
    static final String MODELS_PKG_PREFIX = "models.";

    static final Type OBJECT_TYPE;
	static final Type EXPRESSION_TYPE;
    static final SootClass symUtilClass;
    static SootClass symOpsClass;
    static final SootMethodRef[] argPush;
    static final SootMethodRef argPop;
    static final SootMethodRef retPush;
    static final SootMethodRef retPop;
    static final SootMethodRef assume;
    static final SootMethodRef targetHit;

    static final SootMethodRef id_field_read;
    static final SootMethodRef explicit_read;
    static final SootMethodRef id_field_write;
	static final SootMethodRef explicit_write;
    static final SootMethodRef rw;
    static final SootMethodRef ww;
    static final SootMethodRef eventId;
    static final SootMethodRef readArray;
    static final SootMethodRef writeArray;
    static final SootMethodRef only_write;
    static final SootMethodRef getSolution_int;	//Method which returns a new solution for a given variable
    static final SootMethodRef getSolution_long;	//Method which returns a new solution for a given variable
    static final SootMethodRef getSolution_string;	//Method which returns a new solution for a given variable

    static private final java.io.PrintWriter printer = new java.io.PrintWriter(System.out);

    static{
		EXPRESSION_TYPE = RefType.v(EXPRESSION_CLASS_NAME);
		OBJECT_TYPE = RefType.v(OBJECT_CLASS_NAME);
		symUtilClass = Scene.v().getSootClass(SYMUTIL_CLASS_NAME);

		Map map = binopSymbolToMethodName;
		map.put("+",   "_plus");
		map.put("-",   "_minus");
		map.put("*",   "_mul");
		map.put("/",   "_div");
		map.put(">>",  "_shiftR");
		map.put("<<",  "_shiftL");
		map.put(">>>", "_shiftUR");
		map.put("&",   "_and");
		map.put("|",   "_or");
		map.put("^",   "_xor");
		map.put("%",   "_rem");
		
		map.put("==",  "_eq");
		map.put("!=",  "_ne");
		map.put(">=",  "_ge");
		map.put(">",   "_gt");
		map.put("<=",  "_le");
		map.put("<",   "_lt");
		
		map.put(Jimple.CMP, "_cmp");
		map.put(Jimple.CMPL, "_cmpl");
		map.put(Jimple.CMPG, "_cmpg");

		map.put("contains", "java.lang.String: boolean contains(java.lang.CharSequence)");

		negationMap.put("==", "!=");
		negationMap.put("!=", "==");
		negationMap.put(">=", "<");
		negationMap.put(">", "<=");
		negationMap.put("<=", ">");
		negationMap.put("<", ">=");
		
		typeMap.put(BooleanType.v(), Types.BOOLEAN);
		typeMap.put(ByteType.v(), Types.BYTE);
		typeMap.put(CharType.v(), Types.CHAR);
		typeMap.put(ShortType.v(), Types.SHORT);
		typeMap.put(IntType.v(), Types.INT);
		typeMap.put(LongType.v(), Types.LONG);
		typeMap.put(FloatType.v(), Types.FLOAT);
		typeMap.put(DoubleType.v(), Types.DOUBLE);
		
		binopSymbolToMethodName.put("<java.lang.String: boolean contains(java.lang.CharSequence)>", "_contains");
									   		
		int count = 31;
		String argPushRetType = "void";

		argPush = new SootMethodRef[count];
		argPush[0] = symUtilClass.getMethod(argPushRetType+" argpush(int)").makeRef(); //int for the subsig id
		String paramSig = "int,"+EXPRESSION_CLASS_NAME;  //int for the subsig id
		for (int i = 1; i < count; i++) {
			argPush[i] = symUtilClass.getMethod(argPushRetType+" argpush("+paramSig+")").makeRef();
			paramSig = paramSig + "," + EXPRESSION_CLASS_NAME;
		}

		//first int to pass subsig id of the currently executing method
		//second int to pass sig id of the method (used to measure coverage)
		//third int to pass the number of args
		argPop = symUtilClass.getMethod(EXPRESSION_CLASS_NAME+"[] argpop(int,int,int)").makeRef();
		retPush = symUtilClass.getMethod("void retpush(int,"+EXPRESSION_CLASS_NAME+")").makeRef();
		retPop = symUtilClass.getMethod(EXPRESSION_CLASS_NAME+" retpop(int)").makeRef();
		assume = symUtilClass.getMethod("void assume(" + EXPRESSION_CLASS_NAME + ",int,boolean)").makeRef();
		targetHit = symUtilClass.getMethod("void targetHit(" + OBJECT_CLASS_NAME + ")").makeRef();
		explicit_read = symUtilClass.getMethod("void read(" + OBJECT_CLASS_NAME + ",int)").makeRef();
		explicit_write = symUtilClass.getMethod("void write(" + OBJECT_CLASS_NAME + ",int)").makeRef();
		id_field_read = symUtilClass.getMethod("void read(int)").makeRef();
		id_field_write = symUtilClass.getMethod("void write(int)").makeRef();
		rw = symUtilClass.getMethod("void rw(int,int)").makeRef();
		ww = symUtilClass.getMethod("void ww(int,int)").makeRef();
		eventId = symUtilClass.getMethod("int eventId()").makeRef();
		readArray = symUtilClass.getMethod("void readArray(" + OBJECT_CLASS_NAME + ",int)").makeRef();
		writeArray = symUtilClass.getMethod("void writeArray(" + OBJECT_CLASS_NAME + ",int)").makeRef();
		only_write = symUtilClass.getMethod("void only_write(int)").makeRef();
		getSolution_int = symUtilClass.getMethod("int getSolution_int("+OBJECT_CLASS_NAME+")").makeRef();
		getSolution_long = symUtilClass.getMethod("long getSolution_long("+OBJECT_CLASS_NAME+")").makeRef();
		getSolution_string = symUtilClass.getMethod("java.lang.String getSolution_string("+OBJECT_CLASS_NAME+")").makeRef();

		symOpsClass = SymOpsClassGenerator.generate();
    }
	
    static String modelClassNameFor(String className)
    {
		String modelClassName = MODELS_PKG_PREFIX+className;
		return modelClassName;
    }
	
    static String modelInvokerClassNameFor(String className)
    {
		return MODELS_PKG_PREFIX+className+"$A3TInvoke";
    }

    /**
     * Returns a reference to the method which creates a new symbolic variable of the given tyoe.
     * 
     * The method name is Util.symbolic_<type>
     * @param type
     * @return
     */
    static SootMethod symValueInjectorFor(Type type)
    {
    	if (type instanceof PrimType) {
			return symUtilClass.getMethodByName("symbolic_"+type.toString());
		}
		else if (type instanceof RefType) {
			SootClass klass = ((RefType) type).getSootClass();
			return ModelMethodsHandler.getSymbolInjectorFor(klass);
		}
		else{
			assert false : "TODO";
			return null;
		}
    }

    public static Body addBody(SootMethod method)
    {
		Body body = jimple.newBody(method);
		method.setActiveBody(body);
        editor.newEmptyBody(body);
		return body;
    }
	
    public static void insertStmt(Stmt stmt)
    {
		editor.insertStmt(stmt);
    }
	
    public static Local newLocal(Type type)
    {
		return editor.freshLocal(type);
    }
	
    public static Local newLocal(Type type, String name)
    {
		return editor.freshLocal(type, name);
    }    
	
    public static void assign(Value lhs, Value rhs)
    {
		AssignStmt as = jimple.newAssignStmt(lhs, rhs);
		editor.insertStmt(as);
    }
	
    public static void identity(Local lhs, IdentityRef rhs)
    {
		IdentityStmt is = jimple.newIdentityStmt(lhs, rhs);
		editor.insertStmt(is);
    }

    public static void ret(Value v)
    {
		ReturnStmt rs = jimple.newReturnStmt(v);
		editor.insertStmt(rs);
    }
    
    public static void iff(ConditionExpr ce, Stmt target)
    {
		editor.insertStmt(jimple.newIfStmt(ce, target));
    }
	
    public static void invoke(InvokeExpr ie)
    {
		editor.insertStmt(jimple.newInvokeStmt(ie));
    }
	
    public static void retVoid()
    {
		ReturnVoidStmt rs = jimple.newReturnVoidStmt();
		editor.insertStmt(rs);
    }
    
    public static void gotoo(Stmt stmt)
    {
		editor.insertStmt(jimple.newGotoStmt(stmt));
    }

    public static void throww(Local throwable)
    {
		editor.insertStmt(jimple.newThrowStmt(throwable));
    }
    
    public static void enterMonitor(Local monitor)
    {
		editor.insertStmt(jimple.newEnterMonitorStmt(monitor));
    }
	
    public static void exitMonitor(Local monitor)
    {
		editor.insertStmt(jimple.newExitMonitorStmt(monitor));
    }

    public static VirtualInvokeExpr virtualInvokeExpr(Local local, SootMethodRef mref)
    {
		return jimple.newVirtualInvokeExpr(local, mref);
    }
        
    public static VirtualInvokeExpr virtualInvokeExpr(Local local, SootMethodRef mref, Immediate arg1)
    {
		return jimple.newVirtualInvokeExpr(local, mref, arg1);
    }
	
    public static VirtualInvokeExpr virtualInvokeExpr(Local local, SootMethodRef mref, Immediate arg1, Immediate arg2)
    {
		return jimple.newVirtualInvokeExpr(local, mref, arg1, arg2);
    }
	
    public static VirtualInvokeExpr virtualInvokeExpr(Local local, String methodSig)
    {
		SootMethodRef mref = Scene.v().getMethod(methodSig).makeRef();
		return virtualInvokeExpr(local, mref);
    }
	
    public static VirtualInvokeExpr virtualInvokeExpr(Local local, String methodSig, Immediate arg1)
    {
		SootMethodRef mref = Scene.v().getMethod(methodSig).makeRef();
		return virtualInvokeExpr(local, mref, arg1);
    }
	
    public static VirtualInvokeExpr virtualInvokeExpr(Local local, String methodSig, Immediate arg1, Immediate arg2)
    {
		SootMethodRef mref = Scene.v().getMethod(methodSig).makeRef();
		return virtualInvokeExpr(local, mref, arg1, arg2);
    }
	
    public static SpecialInvokeExpr specialInvokeExpr(Local local, SootMethodRef mref, Immediate arg1)
    {
		return jimple.newSpecialInvokeExpr(local, mref, arg1);
    }
	
    public static SpecialInvokeExpr specialInvokeExpr(Local local, SootMethodRef mref, Immediate arg1, Immediate arg2)
    {
		return jimple.newSpecialInvokeExpr(local, mref, arg1, arg2);
    }
	
    public static StaticInvokeExpr staticInvokeExpr(SootMethodRef mref)
    {
		return jimple.newStaticInvokeExpr(mref);
    }
	
    public static StaticInvokeExpr staticInvokeExpr(SootMethodRef mref, Immediate arg1)
    {
		return jimple.newStaticInvokeExpr(mref, arg1);
    }
	
    public static StaticInvokeExpr staticInvokeExpr(SootMethodRef mref, Immediate arg1, Immediate arg2)
    {
		return jimple.newStaticInvokeExpr(mref, arg1, arg2);
    }
	
    public static StaticInvokeExpr staticInvokeExpr(SootMethodRef mref, Immediate arg1, Immediate arg2, Immediate arg3)
    {
		return jimple.newStaticInvokeExpr(mref, arg1, arg2, arg3);
    }
	
    public static StaticInvokeExpr staticInvokeExpr(SootMethodRef mref, List args)
    {
		return jimple.newStaticInvokeExpr(mref, args);
    }

    public static InterfaceInvokeExpr interfaceInvokeExpr(Local local, SootMethodRef mref)
    {
		return jimple.newInterfaceInvokeExpr(local, mref);
    }
        
    public static InterfaceInvokeExpr interfaceInvokeExpr(Local local, SootMethodRef mref, Immediate arg1)
    {
		return jimple.newInterfaceInvokeExpr(local, mref, arg1);
    }
	
    public static InterfaceInvokeExpr interfaceInvokeExpr(Local local, SootMethodRef mref, Immediate arg1, Immediate arg2)
    {
		return jimple.newInterfaceInvokeExpr(local, mref, arg1, arg2);
    }
    
    public static CastExpr castExpr(Local l, Type t)
    {
		return jimple.newCastExpr(l, t);
    }

    public static InstanceOfExpr instanceOfExpr(Local l, Type t)
    {
		return jimple.newInstanceOfExpr(l, t);
    }
    
    public static EqExpr eqExpr(Immediate i1, Immediate i2)
    {
		return jimple.newEqExpr(i1, i2);
    }
	
    public static NeExpr neExpr(Immediate i1, Immediate i2)
    {
		return jimple.newNeExpr(i1, i2);
    }    

    public static NewExpr newExpr(RefType type)
    {
		return jimple.newNewExpr(type);
    }
    
    public static AddExpr addExpr(Immediate i1, Immediate i2)
    {
		return jimple.newAddExpr(i1, i2);
    }
	
    public static SubExpr subExpr(Immediate i1, Immediate i2)
    {
		return jimple.newSubExpr(i1, i2);
    }
    
    public static InstanceFieldRef instanceFieldRef(Immediate base, SootFieldRef f)
    {
		return jimple.newInstanceFieldRef(base, f);
    }
	
    public static StaticFieldRef staticFieldRef(SootFieldRef f)
    {
		return jimple.newStaticFieldRef(f);
    }
	
    public static Local paramLocal(SootMethod method, int paramIndex)
    {
		Local p = newLocal(method.getParameterType(paramIndex));
		identity(p, jimple.newParameterRef(p.getType(), paramIndex));
		return p;
    }
    
    public static List<Local> paramLocals(SootMethod method)
    {
		List<Local> params = new ArrayList();
		int pc = method.getParameterCount();
		for (int i = 0; i < pc; i++) {
			params.add(paramLocal(method, i));
		}
		return params;
    }
	
    public static Local thisLocal(SootMethod method)
    {
		Local p = newLocal(method.getDeclaringClass().getType());
		identity(p, jimple.newThisRef((RefType) p.getType()));
		return p;
    }
    
    public static List booleanConstants(int i)
    {
		List bools = new ArrayList(i);
		for (; i >= 0; i--) {
			bools.add(IntConstant.v(0));
		}
		return bools;
    }
    
    public static void debug(SootMethod m, boolean flag)
    {
		if (flag) {
			try{
				Printer.v().printTo(m.retrieveActiveBody(), printer);
			}catch(RuntimeException e) {
				System.err.println("Failed to validate: " + m.getSignature());
				for (Unit u : m.retrieveActiveBody().getUnits()) {
					System.out.println("\t"+u);
				}
				printer.flush();
				throw e;
			}
			printer.flush();
		}
    }

    public static void debug(String msg, boolean flag)
    {
		if (flag) {
			System.out.println(msg);
		}
    }
}
