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
package acteve.symbolic.integer;

import acteve.symbolic.integer.operation.Operations;

public abstract class DoubleExpression extends Expression implements CMP, Algebraic
{  
    public double seed;

    public double seed()
    {
		return seed;
    }

	protected final String toYicesString(String str)
    {
		String newName = SymbolicDouble.makeName();
        Expression.pc.printConstraint("(= " + newName + " " + str + ")");
        return newName;
    }

    public Expression _cmpl(Expression right)
    {
		return DCMPL.apply(this, right);
    }

    public Expression _cmpg(Expression right)
    {
	return DCMPG.apply(this, right);
    }    

    public Expression _plus(Expression e)
    {
	return DADD.apply(this, e);
    }

    public Expression _minus(Expression e)
    {
	return DSUB.apply(this, e);
    }
    
    public Expression _mul(Expression e)
    {
	return DMUL.apply(this, e);
    }

    public Expression _div(Expression e)
    {
	return DDIV.apply(this, e);
    }
    
    public Expression _rem(Expression e)
    {
	return DREM.apply(this, e);
    }
    
    public Expression _neg()
    {
	return DNEG.apply(this);
    }
    
    public Expression _cast(int type)
    {
	if(type == Types.INT)
	    return D2I.apply(this);
	if(type == Types.LONG)
	    return D2L.apply(this);
	if(type == Types.FLOAT)
	    return D2F.apply(this);
	throw new RuntimeException("unexpected type " + type);
    }

    public static final BinaryOperator DADD  = Operations.v.dadd();
    public static final BinaryOperator DSUB  = Operations.v.dsub();
    public static final BinaryOperator DMUL  = Operations.v.dmul();
    public static final BinaryOperator DDIV  = Operations.v.ddiv();
    public static final BinaryOperator DREM  = Operations.v.drem();
    public static final BinaryOperator DCMPL = Operations.v.dcmpl();
    public static final BinaryOperator DCMPG = Operations.v.dcmpg();
    public static final UnaryOperator  DNEG  = Operations.v.dneg();
    
    public static final UnaryOperator D2I = Operations.v.d2i();
    public static final UnaryOperator D2L = Operations.v.d2l();
    public static final UnaryOperator D2F = Operations.v.d2f();
    
}





