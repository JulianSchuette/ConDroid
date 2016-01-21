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

package models.android.graphics;

import acteve.symbolic.integer.Expression;
import acteve.symbolic.integer.IntegerExpression;
import acteve.symbolic.integer.BooleanExpression;
import acteve.symbolic.integer.IntegerConstant;
import acteve.symbolic.integer.SymbolicInteger;
import acteve.symbolic.integer.Types;
import android.util.Slog;

public class Rect
{
	public static Expression contains__II__Z(java.lang.Object obj, Expression obj$sym, 
											 int x, Expression x$sym,
											 int y, Expression y$sym)
	{
		if(x$sym == null && y$sym == null)
			return null;
		
		android.graphics.Rect rect = (android.graphics.Rect) obj;
		
		Slog.e("A3T_DEBUG", "left = " + rect.left + " right = " + rect.right +
			   "top = " + rect.top + " bottom = " + rect.bottom);
		Slog.e("A3T_DEBUG", "x = " + x + " y = " + y);

		Expression xExpr = null;
		Expression yExpr = null;
		if(x$sym != null){
			IntegerExpression x$symb = (IntegerExpression) x$sym;
			xExpr = ((BooleanExpression) x$symb._lt(IntegerConstant.get(rect.right)))._conjunct(x$symb._ge(IntegerConstant.get(rect.left)));     
		}
		if(y$sym != null){
			IntegerExpression y$symb = (IntegerExpression) y$sym;
			yExpr = ((BooleanExpression) y$symb._lt(IntegerConstant.get(rect.bottom)))._conjunct(y$symb._ge(IntegerConstant.get(rect.top)));
		}
		if(xExpr == null)
		    return yExpr;
		if(yExpr == null)
		    return xExpr;
		return ((BooleanExpression) xExpr)._conjunct(yExpr);
	}
}
