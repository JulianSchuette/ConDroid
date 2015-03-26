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

package acteve.explorer;

import java.util.Map;
import java.util.HashMap;

public class Z3Model
{
	Map<String,Object> vals = new HashMap();
	
	void put(String varName, Object value)
	{
		vals.put(varName, value);
	}
	
	public Object get(String varName)
	{
		return vals.get(varName);
	}

	public void print()
	{
		for(Map.Entry<String,Object> e : vals.entrySet()){
			System.out.println(e.getKey() + " " + e.getValue());
		}
	}

	public static class Array
	{
		private Map<Integer,Number> vals = new HashMap();
		private Number defaultVal;
		
		void put(Integer index, Number value)
		{
			vals.put(index, value);
		}
		
		public Number get(int index)
		{
			Number elem = vals.get(index);
			if(elem != null)
				return elem;
			return defaultVal;
		}
		
		void setDefaultValue(Number defaultVal)
		{
			this.defaultVal = defaultVal;
		}
		
		public String toString()
		{
			StringBuilder buf = new StringBuilder();
			for(Map.Entry<Integer,Number> e : vals.entrySet()){
				buf.append("("+e.getKey()+", " + e.getValue()+") ");
			}
			buf.append("(?, "+defaultVal+")");
			return buf.toString();
		}
	}
}