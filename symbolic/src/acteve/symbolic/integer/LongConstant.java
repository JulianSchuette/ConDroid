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

public final class LongConstant extends LongExpression implements Constant
{
    protected LongConstant(long seed)
    {
	this.seed = seed;
    }
    
    public String toString () 
    {
	return seed+"l";
    }
    
    public String toYicesString()
    {
	return Operations.v.longConstant(seed);
    }
    
    public static LongConstant get(long c)
    {
	return cache.get(c);
    }

    public long seed()
    {
	return seed;
    }
    
    private static LRUCacheLong cache = new LRUCacheLong();

}

class LRUCacheLong extends LinkedHashMap<Long,LongConstant>
{
    private final int MAX_SIZE = 50;
    private final LongConstant ZERO = new LongConstant(0L);
    private final LongConstant ONE = new LongConstant(1L);
    
    protected boolean removeEldestEntry(Map.Entry eldest)
    {
	return size() > MAX_SIZE;
    }
    
    public LongConstant get(long c)
    {
	if(c == 0L) return ZERO;
	if(c == 1L) return ONE;
	return get(Long.valueOf(c));
    }
    
    public LongConstant get(Long num)
    {
	LongConstant constant = super.get(num);
	if(constant == null){
	    constant = new LongConstant(num.longValue());
	    this.put(num, constant);
	}
	return constant;
    }
}
