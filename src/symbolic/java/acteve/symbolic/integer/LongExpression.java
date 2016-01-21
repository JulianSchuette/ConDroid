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

public abstract class LongExpression extends Expression implements Algebraic, Bitwise
{  
    public long seed;

    public long seed()
    {
		return seed;
    }

	protected final String toYicesString(String str)
    {
		String newName = SymbolicLong.makeName();
        Expression.pc.printConstraint("(= " + newName + " " + str + ")");
        return newName;
    }

    public Expression _cmp(Expression right)
    {
		return LCMP.apply(this, right);
    }
	
    public Expression _plus(Expression e)
    {
		return LADD.apply(this, e);
    }
	
    public Expression _minus(Expression e)
    {
		return LSUB.apply(this, e);
    }
    
    public Expression _mul(Expression e)
    {
		return LMUL.apply(this, e);
    }
	
    public Expression _div(Expression e)
    {
		return LDIV.apply(this, e);
    }
    
    public Expression _rem(Expression e)
    {
		return LREM.apply(this, e);
    }
    
    public Expression _neg()
    {
		return LNEG.apply(this);
    }
    
    public Expression _or(Expression e)
    {
		return LOR.apply(this, e);
    }
	
    public Expression _and(Expression e)
    {
		return LAND.apply(this, e);
    }
    
    public Expression _xor(Expression e)
    {
		return LXOR.apply(this, e);
    }
    
    public Expression _shiftL(Expression e)
    {
		return LSHL.apply(this, e);
    }

    public Expression _shiftR(Expression e)
    {
		return LSHR.apply(this, e);
    }
    
    public Expression _shiftUR(Expression e)
    {
		return LUSHR.apply(this, e);
    }
	
    public Expression _cast(int type)
    {
		if(type == Types.INT)
			return L2I.apply(this);
		if(type == Types.FLOAT)
			return L2F.apply(this);
		if(type == Types.DOUBLE)
			return L2D.apply(this);
		throw new RuntimeException("unexpected type " + type);
    }
	
    public static final BinaryOperator LADD  = Operations.v.ladd();
    public static final BinaryOperator LSUB  = Operations.v.lsub();
    public static final BinaryOperator LMUL  = Operations.v.lmul();
    public static final BinaryOperator LDIV  = Operations.v.ldiv();
    public static final BinaryOperator LREM  = Operations.v.lrem();
    public static final UnaryOperator  LNEG  = Operations.v.lneg();
    public static final BinaryOperator LOR   = Operations.v.lor();
    public static final BinaryOperator LAND  = Operations.v.land(); 
    public static final BinaryOperator LXOR  = Operations.v.lxor();
    public static final BinaryOperator LSHR  = Operations.v.lshr();
    public static final BinaryOperator LSHL  = Operations.v.lshl();
    public static final BinaryOperator LUSHR = Operations.v.lushr();
    public static final BinaryOperator LCMP  = Operations.v.lcmp();

    public static final UnaryOperator L2I = Operations.v.l2i();
    public static final UnaryOperator L2F = Operations.v.l2f();
    public static final UnaryOperator L2D = Operations.v.l2d();
}





