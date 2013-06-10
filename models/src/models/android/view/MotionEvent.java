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
import acteve.symbolic.array.SymbolicFloatArray;
import acteve.symbolic.integer.Expression;
import acteve.symbolic.integer.SymbolicFloat;
import android.util.Slog;
import acteve.symbolic.Util;

public class MotionEvent
{
	private static int count = 0;
	private static final String[] fldNames = {"mDownTimeNano", "mAction", "mXOffset", 
											  "mYOffset", "mXPrecision", "mYPrecision", 
											  "mEdgeFlags", "mMetaState", "gRecyclerUsed",
											  "mFlags", "mNumPointers", "mNumSamples",
											  "mLastDataSampleIndex", "mLastEventTimeNanoSampleIndex",
											  "mPointerIdentifiers", "mNumSamples"};

	public static Expression Landroid_view_MotionEvent_2(java.lang.Object seed, java.lang.String name)
    {
        int id = count++;
        if (id % 2 == 0) {
            //XXX: tap specific trick. each tap calls this method twice (UP and DOWN)
			Util.newEvent();
           	Util.e("A3T_ITER", String.valueOf(id));
        }
		Slog.e("A3T_DEBUG", "symbolic motion event injected " + id);
		android.view.MotionEvent event = (android.view.MotionEvent) seed;
		Class cls = event.getClass();

		try {
			String fldName = "mDataSamples";
			Field conFld = cls.getDeclaredField(fldName);			
			conFld.setAccessible(true);
			float[] conVal = (float[]) conFld.get(event);
			Expression symVal = (conVal == null) ? null : new SymbolicFloatArray(name+"$"+id, new int[]{0, 1});
			setSymField(event, cls, fldName, symVal);

			for(String fName : fldNames)
				setSymField(event, cls, fName, null);
			
			return null;
		 } catch (NoSuchFieldException e) {
			 throw new Error(e);
		 } catch (IllegalAccessException e) {
			 throw new Error(e);
		 }
	 }

	
	private static void setSymField(android.view.MotionEvent event, Class cls, String fldName, Expression symVal) 
		throws NoSuchFieldException, IllegalAccessException
	{
		Field symFld = cls.getDeclaredField(fldName+"$sym");
		symFld.setAccessible(true);
		symFld.set(event, symVal);
	}
}
