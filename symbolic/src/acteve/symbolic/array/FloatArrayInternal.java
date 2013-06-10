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
package acteve.symbolic.array;

import acteve.symbolic.integer.IntegerConstant;
import acteve.symbolic.integer.Expression;
import acteve.symbolic.integer.FloatExpression;
import acteve.symbolic.integer.IntegerExpression;

class FloatArrayInternal extends ArrayInternal
{
	private int[] symIndices;

	FloatArrayInternal()
	{
		super();
	}

	FloatArrayInternal(String name)
	{
		super(name);
	}

	FloatArrayInternal(String name, int[] symIndices)
	{
		super(name);
		this.symIndices = symIndices;
	}

	public Expression get(Expression index)
	{
		if(symIndices != null){
			if(index instanceof IntegerConstant){
				int ind = ((IntegerConstant) index).seed();
				for(int i = 0; i < symIndices.length; i++){
					if(symIndices[i] == ind)
						return new FloatArrayElem(this, (IntegerExpression) index);
				} 
			}
		}
		return new FloatArrayElem(this, (IntegerExpression) index);
	}
	
	public ArrayInternal set(Expression index, Expression value)
	{
		return new UpdatedFloatArrayInternal(this, (IntegerExpression) index, (FloatExpression) value);
	}
}
