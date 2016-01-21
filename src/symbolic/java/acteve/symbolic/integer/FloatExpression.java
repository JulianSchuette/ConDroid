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

public abstract class FloatExpression extends Expression implements CMP, Algebraic
{  
    public float seed;

    public float seed()
    {
		return seed;
    }

	protected final String toYicesString(String str)
    {
		String newName = SymbolicFloat.makeName();
        Expression.pc.printConstraint("(= " + newName + " " + str + ")");
        return newName;
    }

	private Expression _realeq(Expression e)
	{
		return REQ.apply(this, e);
	}

	private Expression _realne(Expression e)
	{
		return RNE.apply(this, e);
	}

    public Expression _cmpl(Expression right)
    {
		return FCMPL.apply(this, right);
    }

    public Expression _cmpg(Expression right)
    {
		return FCMPG.apply(this, right);
    }

    public Expression _plus(Expression e)
    {
		return FADD.apply(this, e);
    }

    public Expression _minus(Expression e)
    {
		return FSUB.apply(this, e);
    }
    
    public Expression _mul(Expression e)
    {
		return FMUL.apply(this, e);
    }

    public Expression _div(Expression e)
    {
		return FDIV.apply(this, e);
    }
    
    public Expression _rem(Expression e)
    {
		return FREM.apply(this, e);
    }
    
    public Expression _neg()
    {
		return FNEG.apply(this);
    }
        
    public Expression _cast(int type)
    {
		if(type == Types.INT){
			if(F2I == null){
				SymbolicInteger i = new SymbolicInteger(Types.INT, (int) seed);
				FloatExpression e = (FloatExpression) IntegerExpression.I2F.apply(i);
				Expression.pc.assumeDet(e._realeq(this));
				return i;
			}
			else{
				return F2I.apply(this);
			}
		}
		if(type == Types.LONG){
			if(F2L == null){
				SymbolicLong i = new SymbolicLong((long) seed);
				FloatExpression e = (FloatExpression) LongExpression.L2F.apply(i);
				Expression.pc.assumeDet(e._realeq(this));
				return i;
			}
			else{
				return F2L.apply(this);
			}
		}
		if(type == Types.DOUBLE)
			return F2D.apply(this);
		throw new RuntimeException("unexpected type " + type);
    }

    public static final BinaryOperator FADD  = Operations.v.fadd();
    public static final BinaryOperator FSUB  = Operations.v.fsub();
    public static final BinaryOperator FMUL  = Operations.v.fmul();
    public static final BinaryOperator FDIV  = Operations.v.fdiv();
    public static final BinaryOperator FREM  = Operations.v.frem();
    public static final BinaryOperator FCMPL = Operations.v.fcmpl();
    public static final BinaryOperator FCMPG = Operations.v.fcmpg();
    public static final UnaryOperator  FNEG  = Operations.v.fneg();
    
    public static final UnaryOperator F2I = Operations.v.f2i();
    public static final UnaryOperator F2L = Operations.v.f2l();
    public static final UnaryOperator F2D = Operations.v.f2d();

	public static final BinaryOperator REQ = Operations.v.req();
	public static final BinaryOperator RNE = Operations.v.rne();
}





