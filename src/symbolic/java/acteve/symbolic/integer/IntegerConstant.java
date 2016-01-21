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
import java.util.*;

public class IntegerConstant extends IntegerExpression implements Constant
{
    protected IntegerConstant(int seed)
    {
		this.seed = seed;
    }
    
    public String toString () 
    {
		return String.valueOf(seed);
    }
    
    public String toYicesString()
    {
		return Operations.v.intConstant(seed);
    }
    
    public static IntegerConstant get(int c)
    {
		return cache.get(c);
    }
    
    public int seed()
    {
		return seed;
    }

	public Expression _eq(Expression e)
	{
		if(e instanceof BooleanExpression)
			return ((BooleanExpression) e)._eq(this);
		else 
			return super._eq(e);
	}
    
	public Expression _ne(Expression e)
	{
		if(e instanceof BooleanExpression)
			return ((BooleanExpression) e)._ne(this);
		else 
			return super._ne(e);
	}

    private static LRUCacheInteger cache = new LRUCacheInteger();
}

class LRUCacheInteger extends LinkedHashMap<Integer,IntegerConstant>
{
    private final int MAX_SIZE = 50;
    private final IntegerConstant ZERO = new IntegerConstant(0);
    private final IntegerConstant ONE = new IntegerConstant(1);
    
    protected boolean removeEldestEntry(Map.Entry eldest)
    {
		return size() > MAX_SIZE;
    }
    
    public IntegerConstant get(int c)
    {
		if(c == 0) return ZERO;
		if(c == 1) return ONE;
		return get(Integer.valueOf(c));
    }
    
    public IntegerConstant get(Integer num)
    {
		IntegerConstant constant = super.get(num);
		if(constant == null){
			constant = new IntegerConstant(num.intValue());
			this.put(num, constant);
		}
		return constant;
    }
}
