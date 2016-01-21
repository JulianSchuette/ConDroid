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

package models.android.view;

import java.lang.reflect.Field;
import acteve.symbolic.integer.SymbolicLong;
import acteve.symbolic.integer.SymbolicInteger;
import acteve.symbolic.integer.Types;
import acteve.symbolic.integer.Expression;

public class KeyEvent
{
	private static int count = 0;

	private static Class[] fldTypes = new Class[] {
		long.class, long.class, int.class, int.class, int.class, int.class, int.class
	};

	private static String[] fldNames = new String[] {
		"mDownTime", "mEventTime", "mAction", "mKeyCode", "mRepeatCount", "mMetaState", "mScanCode"
	};

	public static Expression Landroid_view_KeyEvent_2(java.lang.Object seed, java.lang.String name)
    {
		System.out.println("symbolic key event injected");
		android.view.KeyEvent event = (android.view.KeyEvent) seed;
		Class cls = event.getClass();

		try {
			for (int i = 0; i < fldTypes.length; i++) {
				String fldName = fldNames[i];
				Field symFld = cls.getDeclaredField(fldName+"$sym");
				Field conFld = cls.getDeclaredField(fldName);
			
				symFld.setAccessible(true);
				conFld.setAccessible(true);
			
				Object conVal = conFld.get(event);
				if (conVal == null)
					continue;
			
				Expression symVal;
				String symName = name + "$" + count++;
				Class fldType = fldTypes[i];
				if (fldType == int.class)
					symVal = new SymbolicInteger(Types.INT, symName, 0);
				else if (fldType == long.class)
					symVal = new SymbolicLong(symName, 0);
				else
					throw new Error();
				symFld.set(event, symVal);
			}
			return null;
		} catch (NoSuchFieldException e) {
			throw new Error(e);
		} catch (IllegalAccessException e) {
			throw new Error(e);
		}
    }
}

