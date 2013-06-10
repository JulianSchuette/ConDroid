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

//import android.util.Slog;
import acteve.symbolic.Mylog;

public class PathCondition
{
    private boolean inited = false;
    private int printST  = 8;

    public PathCondition()
    {	
    }

	
    public void assumeDet(Expression e)
    {
		printConstraint(e, true, true);
    }

    public void assumeTru(Expression e)
    {
		printConstraint(e, true, false);
    }

	public void assumeFls(Expression e)
	{
		printConstraint(e, false, false);  // TODO: negate
	}

    private void printConstraint(Expression e, boolean flag, boolean det)
    {
		//Mylog.e("A3T_DEBUG", "e = " + e + " flag = " + flag + " det = " + det);

		//printST();
		if(!flag){
			if(e instanceof BooleanExpression){
				e = BooleanExpression.NEGATION.apply(e);
			}
			else 
				assert false;
			//str = "BNOT("+str+")";
			//str = "(not " + str + ")";
		}
		String str = e.toYicesString();
		if(det)
			str = "*"+str;
		Mylog.e("A3T_PC", str);
    }

	public void printConstraint(String constraint)
	{
		Mylog.e("A3T_PC", "*"+constraint);
	}

	/*
    public void recordSeed(long l, String name)
    {
		if(!inited)
			init();
		printer.println("(assert+ (= " + name + " " + l + "))"); 
    }
	*/
    
    private void printST()
    {
		StackTraceElement[] elems = new Throwable().getStackTrace();
		int len = elems.length;
		for(int i = 0; i < printST; i++){
			StackTraceElement e = elems[i];
			Mylog.e("A3T_ST", e.toString());
		}	
    }
}
