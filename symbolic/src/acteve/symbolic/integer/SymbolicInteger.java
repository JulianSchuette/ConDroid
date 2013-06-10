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

import acteve.symbolic.SException;

public class SymbolicInteger extends IntegerExpression
{
    private static int count = 0;

    //public SymbolicInteger()
    //{
    //    this("int",null);
    //}

    public SymbolicInteger(int type, int seed)
    {
		this(type, null, seed);
    }

    public SymbolicInteger(int type, String name, int seed) 
    {
		super();
		
		switch(type){
		case Types.CHAR:
			exprString = makeName("$C$", name);
			break;
		case Types.BYTE:
			exprString = makeName("$B$", name);
			break;
		case Types.SHORT:
			exprString = makeName("$S$", name);
			break;
		case Types.INT:
			exprString = makeName("$I$", name);
			break;
		case Types.BOOLEAN:
			exprString = makeName("$Z$", name);
			break;
		default:
			throw new RuntimeException("unexpected type " + type + " " + name);
		}
		this.seed = seed;
    }
	
    private String makeName(String type, String name)
    {
		if(name == null)
			return type+count++;
		else
			return type+name;
    }

    static String makeName()
    {
		return "$I$"+count++;
    }
	
    public String toString() 
    {
		return exprString + "[" + seed + "]";
    }
    
    public String toYicesString()
    {
		return exprString;
    }
    
}
