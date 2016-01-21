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

package acteve.symbolic.string;

import acteve.symbolic.Constant;
import acteve.symbolic.integer.BinaryBooleanExpression;
import acteve.symbolic.integer.BooleanBinaryOperator;
import acteve.symbolic.integer.Expression;
import acteve.symbolic.integer.operation.Operations;


public class StringConstant extends StringExpression implements Constant
{
	int count = 0;
	public StringConstant() {
		this.seed = "SEED";
	}
	
    protected StringConstant(java.lang.String seed)
    {
		this.seed = seed;
    }
    
    public java.lang.String toString () 
    {
		return java.lang.String.valueOf(seed);
    }
    
    public java.lang.String toYicesString()
    {
		return Operations.v.stringConstant(seed);
    }
    
    
    
    public static StringConstant get(java.lang.String c)
    {
    	return new StringConstant(c);
    }
    
    public java.lang.String seed()
    {
		return seed;
    }
//
//	public Expression _eq(Expression e)
//	{
//		if(e instanceof BooleanExpression)
//			return ((BooleanExpression) e)._eq(this);
//		else 
//			return super._eq(e);
//	}
//    
	public Expression _ne(Expression e)
	{
		System.out.println("CONTAINSCONTAINS. StringConstant._ne called with expression " + e.getClass().getName() + " , " + e.exprString() + " , " + e.toYicesString());
		return this;

	}

	@Override
	public Expression _contains(Expression e) {
		System.out.println("CONTAINSCONTAINS. StringConstant._contains called with expression " + e.getClass().getName() + " , " + e.exprString() + " , " + e.toYicesString());
		return new BinaryBooleanExpression(new BooleanBinaryOperator("Contains"), this, e);
	}
}
