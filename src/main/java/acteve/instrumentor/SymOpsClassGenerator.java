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
import java.util.List;

import soot.ArrayType;
import soot.BooleanType;
import soot.ByteType;
import soot.CharType;
import soot.DoubleType;
import soot.FloatType;
import soot.IntType;
import soot.Local;
import soot.LongType;
import soot.Modifier;
import soot.PrimType;
import soot.RefType;
import soot.Scene;
import soot.ShortType;
import soot.SootClass;
import soot.SootMethod;
import soot.SootMethodRef;
import soot.Type;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.Jimple;
import soot.jimple.NullConstant;
import soot.jimple.Stmt;
import soot.jimple.StringConstant;

public class SymOpsClassGenerator
{
    private static SootClass klass;
    private static final int mod = Modifier.PUBLIC | Modifier.STATIC | Modifier.FINAL;
    private static final String PACKAGE_NAME = "acteve.symbolic.";
    private static final String Equality    = "integer.Equality";
    private static final String Algebraic   = "integer.Algebraic";
    private static final String CMP         = "integer.CMP";
    private static final String Bitwise     = "integer.Bitwise";
    private static final String LongCMP     = "integer.LongExpression";
    private static final String IntCMP      = "integer.IntegerExpression";
	private static final String Array       = "array.Array";
	private static final String Strings     = "string.String";

    static SootClass generate()
    {
		klass = new SootClass(G.SYMOPS_CLASS_NAME, Modifier.PUBLIC);
		klass.setSuperclass(Scene.v().getSootClass("java.lang.Object"));
		klass.setApplicationClass();
		
		addAlgebraicMethods();
		addEqualityMethods();
		addCMPMethods();
		addBitwiseMethods();
		addLongCMPMethod();
		addIntCMPMethods();
		addArrayMethods();
		addStringMethods();

		return klass;
    }
    
    static void addAlgebraicMethods()
    {
		SootClass algebraicInterface = Scene.v().getSootClass(PACKAGE_NAME+Algebraic);
		String[] binops = new String[]{"+", "-", "*", "/", "%"};
		PrimType[] primTypes = new PrimType[]{IntType.v(), LongType.v(), FloatType.v(), DoubleType.v()};
		
		addBinaryMethods(binops, primTypes, algebraicInterface);

		addUnaryMethod(G.negMethodName, new Type[0], algebraicInterface);
		addUnaryMethod(G.castMethodName, new Type[]{IntType.v()}, algebraicInterface);
	}
	    
    static void addEqualityMethods()
    {
		SootClass equalityInterface = Scene.v().getSootClass(PACKAGE_NAME+Equality);
		addBinaryMethods(new String[]{"==", "!="}, new Type[]{IntType.v(), RefType.v("java.lang.Object")}, equalityInterface);
    }
	
    static void addStringMethods()
    {
		SootClass stringsInterface = Scene.v().getSootClass(PACKAGE_NAME+Strings);
		addBinaryMethods(new String[]{"<java.lang.String: boolean contains(java.lang.CharSequence)>"}, new Type[]{RefType.v("java.lang.String")}, stringsInterface);
    }
	
    static void addCMPMethods()
    {
		SootClass cmpInterface = Scene.v().getSootClass(PACKAGE_NAME+CMP);
		String[] binops = new String[]{Jimple.CMPL, Jimple.CMPG};
		PrimType[] primTypes = new PrimType[]{FloatType.v(), DoubleType.v()};
		addBinaryMethods(binops, primTypes, cmpInterface);
    }
	
    static void addBitwiseMethods()
    {
		SootClass bitwiseInterface = Scene.v().getSootClass(PACKAGE_NAME+Bitwise);
		String[] binops = new String[]{"&", "|", "^"};
		PrimType[] primTypes = new PrimType[]{IntType.v(), LongType.v()};	
		addBinaryMethods(binops, primTypes, bitwiseInterface);
		
		String[] shiftops = new String[]{">>", "<<",  ">>>"};
		for (String op : shiftops) {
			String opMethodName = G.binopSymbolToMethodName.get(op);
			for (Type type : primTypes) {
				addMethod(opMethodName, new Type[]{type, IntType.v()}, bitwiseInterface);
			}
		}		
    }
	
    static void addLongCMPMethod()
    {
		SootClass longCMPClass = Scene.v().getSootClass(PACKAGE_NAME+LongCMP);
		String[] binops = new String[]{Jimple.CMP};
		PrimType[] primTypes = new PrimType[]{LongType.v()};	
		addBinaryMethods(binops, primTypes, longCMPClass);
    }
    
    static void addIntCMPMethods()
    {
		SootClass intCMPClass = Scene.v().getSootClass(PACKAGE_NAME+IntCMP);
		String[] binops = new String[]{">", "<", ">=", "<="};
		PrimType[] primTypes = new PrimType[]{IntType.v()};	
		addBinaryMethods(binops, primTypes, intCMPClass);
    }
    
    static void addArrayMethods()
    {
		SootClass arrayClass = Scene.v().getSootClass(PACKAGE_NAME+Array);
		Type[] types = new Type[]{IntType.v(), 
								  ShortType.v(),
								  CharType.v(),
								  ByteType.v(),
								  BooleanType.v(),
								  LongType.v(), 
								  FloatType.v(), 
								  DoubleType.v()};

		String getMethodName = G.arrayGetMethodName; 
		String setMethodName = G.arraySetMethodName; 
		String lenMethodName = G.arrayLenMethodName;
		for (Type type : types) {
			ArrayType arrType = ArrayType.v(type,1);
			addMethod(setMethodName, new Type[]{arrType, IntType.v(), type}, arrayClass);
			addMethod(getMethodName, new Type[]{arrType, IntType.v()}, arrayClass);
		}
		addUnaryMethod(lenMethodName, new Type[0], arrayClass);
    }

    static void addBinaryMethods(String[] binops, Type[] types, SootClass operatorClass)
    {
		for (String op : binops) {
			String opMethodName = G.binopSymbolToMethodName.get(op);
	    	System.out.println("Adding binop method " + opMethodName);
			for (Type type : types) {
				addMethod(opMethodName, new Type[]{type, type}, operatorClass);
			}
		}
    }
     
	static void addUnaryMethod(String opMethodName, Type[] paramTypes, SootClass operatorClass)
	{
		List paramTypesList = new ArrayList();
		paramTypesList.add(G.EXPRESSION_TYPE);
		for(int i = 0; i < paramTypes.length; i++){
			paramTypesList.add(paramTypes[i]);
		}

		SootMethod method = new SootMethod(opMethodName, paramTypesList, G.EXPRESSION_TYPE, mod);
		klass.addMethod(method);	
		G.addBody(method);

		List<Local> paramLocals = G.paramLocals(method);
		Local op1 = paramLocals.remove(0);
		Local result = G.newLocal(G.EXPRESSION_TYPE);

		Local op1Cast = G.newLocal(operatorClass.getType());
		Stmt op1CastAssignment = G.jimple.newAssignStmt(op1Cast, G.jimple.newCastExpr(op1, op1Cast.getType()));
		G.iff(G.neExpr(op1, NullConstant.v()), op1CastAssignment);

		G.ret(NullConstant.v());
		G.insertStmt(op1CastAssignment);

		SootMethodRef opMethod = operatorClass.getMethodByName(opMethodName).makeRef();
		InstanceInvokeExpr ie = operatorClass.isInterface() ?
			G.jimple.newInterfaceInvokeExpr(op1Cast, opMethod, paramLocals) :
			G.jimple.newVirtualInvokeExpr(op1Cast, opMethod, paramLocals);
		G.assign(result, ie);
		G.ret(result);
		G.debug(method, Main.DEBUG);		
	}

	static void addMethod(String opMethodName, Type[] paramTypes, SootClass operatorClass)
    {
		int numOperands = paramTypes.length;

		List paramTypesList = new ArrayList();
		for (int i = 0; i < numOperands; i++) {
			paramTypesList.add(G.EXPRESSION_TYPE);
		}
		for (int i = 0; i < paramTypes.length; i++) {
			paramTypesList.add(paramTypes[i]);
		}
		SootMethod method = new SootMethod(opMethodName, paramTypesList, G.EXPRESSION_TYPE, mod);
		System.out.println("Adding " + method.getName());
		klass.addMethod(method);	
		G.addBody(method);

        List<Local> paramLocals = G.paramLocals(method);
		Local op1 = paramLocals.get(0);
		Local result = G.newLocal(G.EXPRESSION_TYPE);
		Local op1Cast = G.newLocal(operatorClass.getType());
		Local op1Concrete = paramLocals.get(numOperands);
		SootClass constClass = exprConstClassFor(op1Concrete.getType());		
		Stmt makeExpr1 = G.jimple.newAssignStmt(op1Cast, 
			G.staticInvokeExpr(constClass.getMethodByName("get").makeRef(), op1Concrete));
		Stmt op1CastAssignment = G.jimple.newAssignStmt(op1Cast, G.jimple.newCastExpr(op1, op1Cast.getType()));

		Local locString = G.newLocal( RefType.v("java.lang.String"));
		G.invoke(G.staticInvokeExpr(Scene.v().getMethod("<android.util.Log: int d(java.lang.String,java.lang.String)>").makeRef(), StringConstant.v("CONDROID"),StringConstant.v(opMethodName + " called")));
		
		G.iff(G.neExpr(op1, NullConstant.v()), op1CastAssignment);
		
		for(int i = 1; i < numOperands; i++){
			Local operand = paramLocals.get(i);
			G.iff(G.neExpr(operand, NullConstant.v()), makeExpr1);
		}		
		G.invoke(G.staticInvokeExpr(Scene.v().getMethod("<android.util.Log: int d(java.lang.String,java.lang.String)>").makeRef(), StringConstant.v("CONDROID"),StringConstant.v(opMethodName + " returns null")));

		G.ret(NullConstant.v());
		G.insertStmt(op1CastAssignment);
		Stmt nop = G.jimple.newNopStmt();
		G.gotoo(nop);
		G.insertStmt(makeExpr1);
		G.insertStmt(nop);

		List<Local> args = new ArrayList();
		for(int i = 1; i < numOperands; i++){
			Local operand = paramLocals.get(i);
			Local operandConcrete = paramLocals.get(i+numOperands);
			
			nop = G.jimple.newNopStmt();
			G.iff(G.neExpr(operand, NullConstant.v()), nop);
			constClass = exprConstClassFor(operandConcrete.getType());
			G.assign(operand, G.staticInvokeExpr(constClass.getMethodByName("get").makeRef(), operandConcrete));
			G.insertStmt(nop);
			args.add(operand);
		}

		SootMethodRef opMethod = operatorClass.getMethodByName(opMethodName).makeRef();
		InstanceInvokeExpr ie = operatorClass.isInterface() ?
			G.jimple.newInterfaceInvokeExpr(op1Cast, opMethod, args) :
			G.jimple.newVirtualInvokeExpr(op1Cast, opMethod, args);
        G.assign(result, ie);
		G.invoke(G.staticInvokeExpr(Scene.v().getMethod("<android.util.Log: int d(java.lang.String,java.lang.String)>").makeRef(), StringConstant.v("CONDROID"),StringConstant.v(opMethodName + " returns")));

		G.ret(result);
		G.debug(method, Main.DEBUG);
    }
	
    private static SootClass exprConstClassFor(Type type)
    {
		String name = null;
		if (type instanceof PrimType) {
			type = Type.toMachineType(type);
			if (type.equals(IntType.v()))
				name = "IntegerConstant";
			else if (type.equals(LongType.v()))
				name = "LongConstant";
			else if (type.equals(FloatType.v()))
				name = "FloatConstant";
			else if (type.equals(DoubleType.v()))
				name = "DoubleConstant";
			else
				assert false : type;
			name = "integer."+name;
		}
		else if (type instanceof RefType) {
			if (((RefType) type).getSootClass().getName().equals("java.lang.String")) {
				name = "string.StringConstant";
			} else {
				name = "integer.RefConstant";
			}
		}
		else if (type instanceof ArrayType) {
			if (type.equals(ArrayType.v(BooleanType.v(),1)))
				name = "BooleanArrayConstant";
			else if (type.equals(ArrayType.v(CharType.v(),1)))
				name = "CharArrayConstant";
			else if (type.equals(ArrayType.v(ByteType.v(),1)))
				name = "ByteArrayConstant";
			else if (type.equals(ArrayType.v(ShortType.v(),1)))
				name = "ShortArrayConstant";
			else if (type.equals(ArrayType.v(IntType.v(),1)))
				name = "IntegerArrayConstant";
			else if (type.equals(ArrayType.v(LongType.v(),1)))
				name = "LongArrayConstant";
			else if (type.equals(ArrayType.v(FloatType.v(),1)))
				name = "FloatArrayConstant";
			else if (type.equals(ArrayType.v(DoubleType.v(),1)))
				name = "DoubleArrayConstant";
			else
				assert false : type;
			name = "array."+name;
		}
		else 
			assert false : type;
		return Scene.v().getSootClass(PACKAGE_NAME+name);
    }

}
