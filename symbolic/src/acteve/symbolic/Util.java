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

package acteve.symbolic;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import acteve.symbolic.array.SymbolicDoubleArray;
import acteve.symbolic.array.SymbolicFloatArray;
import acteve.symbolic.array.SymbolicIntegerArray;
import acteve.symbolic.integer.Expression;
import acteve.symbolic.integer.SymbolicDouble;
import acteve.symbolic.integer.SymbolicFloat;
import acteve.symbolic.integer.SymbolicInteger;
import acteve.symbolic.integer.SymbolicLong;
import acteve.symbolic.integer.Types;

//import dalvik.system.VMStack;
//import android.util.Slog;

public class Util
{
    private static boolean[] reachedMeths = new boolean[50000];
	private static long latestSolution = 0l;	//Timestamp of latest solution file
	private static HashMap<String, String> solutionMap = new HashMap<String, String>();


    public static String reachedMethsStr() {
        StringBuffer sb = new StringBuffer(15000);
        for (int i = 0; i < reachedMeths.length; i++)
            if (reachedMeths[i])
                sb.append(i).append(',');
        return sb.toString();
    }

	private static boolean readConfTxt = true; //By JULIAN: Switch off reading /sdcard/settings.txt with "numevents=n"

	// Flag used to determine whether we are monitoring (i.e., logging) for this process
	private static boolean monitor = true;

	// Flag used to determine whether testing has begun
	// invariant: started => monitor
	private static boolean started = true;

	private static File settingsFile = new File("/sdcard/settings.txt");

	private static int numEvents;

	// static count of number of conditionals in the SDK.
	private static int numSDKConds;

	private static int eventId;

	// global data
	private static class SymArgs {
		Expression[] symArgs;
		int subSignatureId;
		long threadId;
		
		SymArgs(int subsig, long threadId, Expression[] sa) {
			this.symArgs = sa;
			this.subSignatureId = subsig;
			this.threadId = threadId;
		}
	}
	private static class SymRet {
		Expression symRet;
		int subSignatureId;
		long threadId;
		
		SymRet(int subsig, long threadId, Expression sr) {
			this.symRet = sr;
			this.subSignatureId = subsig;
			this.threadId = threadId;
		}
	}

	private static SymArgs symArgs;
	private static SymRet symRet;

	private static void argpush0(int subsig, Expression[] sa)
	{
		symRet = null;
		long threadId = Thread.currentThread().getId();
		symArgs = new SymArgs(subsig, threadId, sa);
//		Mylog.e("PRE_A3T_ARGPUSH", subsig + " " + Arrays.toString(sa));
	}

	public static void argpush(int subsig)
	{
		argpush0(subsig, new Expression[0]);
	}

	public static void argpush(int subsig, Expression a0)
	{
		Expression[] symArgs = new Expression[]{ 
			a0
		};
		argpush0(subsig, symArgs);
	}

	public static void argpush(int subsig, Expression a0, Expression a1)
	{
		Expression[] symArgs = new Expression[]{ 
			a0, a1
		};
		argpush0(subsig, symArgs);
	}

	public static void argpush(int subsig, Expression a0, Expression a1, Expression a2)
	{
		Expression[] symArgs = new Expression[]{ 
			a0, a1, a2
		};
		argpush0(subsig, symArgs);
	}

	public static void argpush(int subsig, Expression a0, Expression a1, Expression a2, Expression a3)
	{
		Expression[] symArgs = new Expression[]{ 
			a0, a1, a2, a3
		};
		argpush0(subsig, symArgs);
	}

	public static void argpush(int subsig, Expression a0, Expression a1, Expression a2, Expression a3, Expression a4)
	{
		Expression[] symArgs = new Expression[]{ 
			a0, a1, a2, a3, a4
		};
		argpush0(subsig, symArgs);
	}

	public static void argpush(int subsig, Expression a0, Expression a1, Expression a2, Expression a3, 
							   Expression a4, Expression a5)
	{
		Expression[] symArgs = new Expression[]{ 
			a0, a1, a2, a3, a4, a5
		};
		argpush0(subsig, symArgs);
	}

	public static void argpush(int subsig, Expression a0, Expression a1, Expression a2, Expression a3, 
							   Expression a4, Expression a5, Expression a6)
	{
		Expression[] symArgs = new Expression[]{ 
			a0, a1, a2, a3, a4, a5, a6
		};
		argpush0(subsig, symArgs);
	}

	public static void argpush(int subsig, Expression a0, Expression a1, Expression a2, Expression a3, 
							   Expression a4, Expression a5, Expression a6, Expression a7)
	{
		Expression[] symArgs = new Expression[]{ 
			a0, a1, a2, a3, a4, a5, a6, a7
		};
		argpush0(subsig, symArgs);
	}

    public static void argpush(int subsig, Expression a0, Expression a1, Expression a2, Expression a3, 
							   Expression a4, Expression a5, Expression a6, Expression a7, Expression a8)
    {
		Expression[] symArgs = new Expression[]{ 
			a0, a1, a2, a3, a4, a5, a6, a7, a8
		};
		argpush0(subsig, symArgs);
	}

	public static void argpush(int subsig, Expression a0, Expression a1, Expression a2, Expression a3, 
							   Expression a4, Expression a5, Expression a6, Expression a7, 
							   Expression a8, Expression a9)
    {
		Expression[] symArgs = new Expression[]{ 
			a0, a1, a2, a3, a4, a5, a6, a7, a8, 
			a9
		};
		argpush0(subsig, symArgs);
	}

	public static void argpush(int subsig, Expression a0, Expression a1, Expression a2, Expression a3, 
								 Expression a4, Expression a5, Expression a6, Expression a7, 
								 Expression a8, Expression a9, Expression a10)
    {
		Expression[] symArgs = new Expression[]{ 
			a0, a1, a2, a3, a4, a5, a6, a7, a8, 
			a9, a10
		};
		argpush0(subsig, symArgs);
	}

	public static void argpush(int subsig, Expression a0, Expression a1, Expression a2, Expression a3, 
								 Expression a4, Expression a5, Expression a6, Expression a7, 
								 Expression a8, Expression a9, Expression a10, Expression a11)
    {
		Expression[] symArgs = new Expression[]{ 
			a0, a1, a2, a3, a4, a5, a6, a7, a8, 
			a9, a10, a11
		};
		argpush0(subsig, symArgs);
	}

	public static void argpush(int subsig, Expression a0, Expression a1, Expression a2, Expression a3, 
								 Expression a4, Expression a5, Expression a6, Expression a7, 
								 Expression a8, Expression a9, Expression a10, Expression a11,
								 Expression a12)
    {
		Expression[] symArgs = new Expression[]{ 
			a0, a1, a2, a3, a4, a5, a6, a7, a8, 
			a9, a10, a11, a12
		};
		argpush0(subsig, symArgs);
	}

	public static void argpush(int subsig, Expression a0, Expression a1, Expression a2, Expression a3, 
								 Expression a4, Expression a5, Expression a6, Expression a7, 
								 Expression a8, Expression a9, Expression a10, Expression a11,
								 Expression a12, Expression a13)
    {
		Expression[] symArgs = new Expression[]{ 
			a0, a1, a2, a3, a4, a5, a6, a7, a8, 
			a9, a10, a11, a12, a13
		};
		argpush0(subsig, symArgs);
	}

	public static void argpush(int subsig, Expression a0, Expression a1, Expression a2, Expression a3, 
								 Expression a4, Expression a5, Expression a6, Expression a7, 
								 Expression a8, Expression a9, Expression a10, Expression a11,
								 Expression a12, Expression a13, Expression a14)
    {
		Expression[] symArgs = new Expression[]{ 
			a0, a1, a2, a3, a4, a5, a6, a7, a8, 
			a9, a10, a11, a12, a13, a14 
		};
		argpush0(subsig, symArgs);
	}

	public static void argpush(int subsig, Expression a0, Expression a1, Expression a2, Expression a3, 
								 Expression a4, Expression a5, Expression a6, Expression a7, 
								 Expression a8, Expression a9, Expression a10, Expression a11,
								 Expression a12, Expression a13, Expression a14, Expression a15)
    {
		Expression[] symArgs = new Expression[]{ 
			a0, a1, a2, a3, a4, a5, a6, a7, a8, 
			a9, a10, a11, a12, a13, a14, a15
		};
		argpush0(subsig, symArgs);
	}

	public static void argpush(int subsig, Expression a0, Expression a1, Expression a2, Expression a3, 
								 Expression a4, Expression a5, Expression a6, Expression a7, 
								 Expression a8, Expression a9, Expression a10, Expression a11,
								 Expression a12, Expression a13, Expression a14, Expression a15, 
								 Expression a16)
    {
		Expression[] symArgs = new Expression[]{ 
			a0, a1, a2, a3, a4, a5, a6, a7, a8, 
			a9, a10, a11, a12, a13, a14, a15, a16
		};
		argpush0(subsig, symArgs);
	}

	public static void argpush(int subsig, Expression a0, Expression a1, Expression a2, Expression a3, 
								 Expression a4, Expression a5, Expression a6, Expression a7, 
								 Expression a8, Expression a9, Expression a10, Expression a11,
								 Expression a12, Expression a13, Expression a14, Expression a15, 
								 Expression a16, Expression a17)
    {
		Expression[] symArgs = new Expression[]{ 
			a0, a1, a2, a3, a4, a5, a6, a7, a8, 
			a9, a10, a11, a12, a13, a14, a15, a16,
			a17
		};
		argpush0(subsig, symArgs);
	}

	public static void argpush(int subsig, Expression a0, Expression a1, Expression a2, Expression a3, 
								 Expression a4, Expression a5, Expression a6, Expression a7, 
								 Expression a8, Expression a9, Expression a10, Expression a11,
								 Expression a12, Expression a13, Expression a14, Expression a15, 
								 Expression a16, Expression a17, Expression a18)
    {
		Expression[] symArgs = new Expression[]{ 
			a0, a1, a2, a3, a4, a5, a6, a7, a8, 
			a9, a10, a11, a12, a13, a14, a15, a16,
			a17, a18
		};
		argpush0(subsig, symArgs);
	}

	public static void argpush(int subsig, Expression a0, Expression a1, Expression a2, Expression a3, 
								 Expression a4, Expression a5, Expression a6, Expression a7, 
								 Expression a8, Expression a9, Expression a10, Expression a11,
								 Expression a12, Expression a13, Expression a14, Expression a15, 
								 Expression a16, Expression a17, Expression a18, Expression a19)
    {
		Expression[] symArgs = new Expression[]{ 
			a0, a1, a2, a3, a4, a5, a6, a7, a8, 
			a9, a10, a11, a12, a13, a14, a15, a16,
			a17, a18, a19
		};
		argpush0(subsig, symArgs);
	}

	public static void argpush(int subsig, Expression a0, Expression a1, Expression a2, Expression a3, 
								 Expression a4, Expression a5, Expression a6, Expression a7, 
								 Expression a8, Expression a9, Expression a10, Expression a11,
								 Expression a12, Expression a13, Expression a14, Expression a15, 
								 Expression a16, Expression a17, Expression a18, Expression a19,
								 Expression a20)
    {
		Expression[] symArgs = new Expression[]{ 
			a0, a1, a2, a3, a4, a5, a6, a7, a8, 
			a9, a10, a11, a12, a13, a14, a15, a16,
			a17, a18, a19, a20
		};
		argpush0(subsig, symArgs);
	}

	public static void argpush(int subsig, Expression a0, Expression a1, Expression a2, Expression a3, 
								 Expression a4, Expression a5, Expression a6, Expression a7, 
								 Expression a8, Expression a9, Expression a10, Expression a11,
								 Expression a12, Expression a13, Expression a14, Expression a15, 
								 Expression a16, Expression a17, Expression a18, Expression a19,
								 Expression a20, Expression a21)
    {
		Expression[] symArgs = new Expression[]{ 
			a0, a1, a2, a3, a4, a5, a6, a7, a8, 
			a9, a10, a11, a12, a13, a14, a15, a16,
			a17, a18, a19, a20, a21
		};
		argpush0(subsig, symArgs);
	}

	public static void argpush(int subsig, Expression a0, Expression a1, Expression a2, Expression a3, 
								 Expression a4, Expression a5, Expression a6, Expression a7, 
								 Expression a8, Expression a9, Expression a10, Expression a11,
								 Expression a12, Expression a13, Expression a14, Expression a15, 
								 Expression a16, Expression a17, Expression a18, Expression a19,
								 Expression a20, Expression a21, Expression a22)
    {
		Expression[] symArgs = new Expression[]{ 
			a0, a1, a2, a3, a4, a5, a6, a7, a8, 
			a9, a10, a11, a12, a13, a14, a15, a16,
			a17, a18, a19, a20, a21, a22
		};
		argpush0(subsig, symArgs);
	}

	public static void argpush(int subsig, Expression a0, Expression a1, Expression a2, Expression a3, 
								 Expression a4, Expression a5, Expression a6, Expression a7, 
								 Expression a8, Expression a9, Expression a10, Expression a11,
								 Expression a12, Expression a13, Expression a14, Expression a15, 
								 Expression a16, Expression a17, Expression a18, Expression a19,
								 Expression a20, Expression a21, Expression a22, Expression a23)
    {
		Expression[] symArgs = new Expression[]{ 
			a0, a1, a2, a3, a4, a5, a6, a7, a8, 
			a9, a10, a11, a12, a13, a14, a15, a16,
			a17, a18, a19, a20, a21, a22, a23
		};
		argpush0(subsig, symArgs);
	}

	public static void argpush(int subsig, Expression a0, Expression a1, Expression a2, Expression a3, 
								 Expression a4, Expression a5, Expression a6, Expression a7, 
								 Expression a8, Expression a9, Expression a10, Expression a11,
								 Expression a12, Expression a13, Expression a14, Expression a15, 
								 Expression a16, Expression a17, Expression a18, Expression a19,
								 Expression a20, Expression a21, Expression a22, Expression a23,
								 Expression a24)
    {
		Expression[] symArgs = new Expression[]{ 
			a0, a1, a2, a3, a4, a5, a6, a7, a8, 
			a9, a10, a11, a12, a13, a14, a15, a16,
			a17, a18, a19, a20, a21, a22, a23, a24
		};
		argpush0(subsig, symArgs);
	}

	public static void argpush(int subsig, Expression a0, Expression a1, Expression a2, Expression a3, 
								 Expression a4, Expression a5, Expression a6, Expression a7, 
								 Expression a8, Expression a9, Expression a10, Expression a11,
								 Expression a12, Expression a13, Expression a14, Expression a15, 
								 Expression a16, Expression a17, Expression a18, Expression a19,
								 Expression a20, Expression a21, Expression a22, Expression a23,
								 Expression a24, Expression a25)
    {
		Expression[] symArgs = new Expression[]{ 
			a0, a1, a2, a3, a4, a5, a6, a7, a8, 
			a9, a10, a11, a12, a13, a14, a15, a16,
			a17, a18, a19, a20, a21, a22, a23, a24,
			a25
		};
		argpush0(subsig, symArgs);
	}

	public static void argpush(int subsig, Expression a0, Expression a1, Expression a2, Expression a3, 
								 Expression a4, Expression a5, Expression a6, Expression a7, 
								 Expression a8, Expression a9, Expression a10, Expression a11,
								 Expression a12, Expression a13, Expression a14, Expression a15, 
								 Expression a16, Expression a17, Expression a18, Expression a19,
								 Expression a20, Expression a21, Expression a22, Expression a23,
								 Expression a24, Expression a25, Expression a26)
    {
		Expression[] symArgs = new Expression[]{ 
			a0, a1, a2, a3, a4, a5, a6, a7, a8, 
			a9, a10, a11, a12, a13, a14, a15, a16,
			a17, a18, a19, a20, a21, a22, a23, a24,
			a25, a26
		};
		argpush0(subsig, symArgs);
	}

	public static void argpush(int subsig, Expression a0, Expression a1, Expression a2, Expression a3, 
								 Expression a4, Expression a5, Expression a6, Expression a7, 
								 Expression a8, Expression a9, Expression a10, Expression a11,
								 Expression a12, Expression a13, Expression a14, Expression a15, 
								 Expression a16, Expression a17, Expression a18, Expression a19,
								 Expression a20, Expression a21, Expression a22, Expression a23,
								 Expression a24, Expression a25, Expression a26, Expression a27)
    {
		Expression[] symArgs = new Expression[]{ 
			a0, a1, a2, a3, a4, a5, a6, a7, a8, 
			a9, a10, a11, a12, a13, a14, a15, a16,
			a17, a18, a19, a20, a21, a22, a23, a24,
			a25, a26, a27
		};
		argpush0(subsig, symArgs);
	}

	public static void argpush(int subsig, Expression a0, Expression a1, Expression a2, Expression a3, 
								 Expression a4, Expression a5, Expression a6, Expression a7, 
								 Expression a8, Expression a9, Expression a10, Expression a11,
								 Expression a12, Expression a13, Expression a14, Expression a15, 
								 Expression a16, Expression a17, Expression a18, Expression a19,
								 Expression a20, Expression a21, Expression a22, Expression a23,
								 Expression a24, Expression a25, Expression a26, Expression a27,
								 Expression a28)
    {
		Expression[] symArgs = new Expression[]{ 
			a0, a1, a2, a3, a4, a5, a6, a7, a8, 
			a9, a10, a11, a12, a13, a14, a15, a16,
			a17, a18, a19, a20, a21, a22, a23, a24,
			a25, a26, a27, a28
		};
		argpush0(subsig, symArgs);
	}

	public static void argpush(int subsig, Expression a0, Expression a1, Expression a2, Expression a3, 
								 Expression a4, Expression a5, Expression a6, Expression a7, 
								 Expression a8, Expression a9, Expression a10, Expression a11,
								 Expression a12, Expression a13, Expression a14, Expression a15, 
								 Expression a16, Expression a17, Expression a18, Expression a19,
								 Expression a20, Expression a21, Expression a22, Expression a23,
								 Expression a24, Expression a25, Expression a26, Expression a27,
								 Expression a28, Expression a29)
    {
		Expression[] symArgs = new Expression[]{ 
			a0, a1, a2, a3, a4, a5, a6, a7, a8, 
			a9, a10, a11, a12, a13, a14, a15, a16,
			a17, a18, a19, a20, a21, a22, a23, a24,
			a25, a26, a27, a28, a29
		};
		argpush0(subsig, symArgs);
	} 

	/*
	  the returned array contains metadata for parameters
	 */
	public static Expression[] argpop(int subSig, int sig, int argCount)
	{
//		Mylog.e("PRE_A3T_ARGPOP", subSig + " " + sig + " "+argCount);

        if (sig >= 0) { // -1 for models method
			if(eventId == numEvents)
				reachedMeths[sig] = true;
		}

		SymArgs sa = symArgs;
		if(sa != null){
			long ti = sa.threadId;
			if(ti == Thread.currentThread().getId()) {
				int ss = sa.subSignatureId;
				if (subSig < 0 || ss == subSig) {
					//ss is negative for @Symbolic annotated methods 
					//and model invoker methods
					return sa.symArgs;
				}
			}
			//bad things are probably happening.			
			//Mylog.e("A3T_WARN", "bad: " + "subSig = " + subSig + " ss = " + ss);
		}
		else{
			;//Mylog.e("A3T_WARN", "bad: " + "sa is null");
		}
		//unless we are in the main method, when symArgs will be actually null
		//just return nulls as symargs
		return new Expression[argCount];
	}
	
	public static void retpush(int subSig, Expression i)
	{
		SymRet sr = new SymRet(subSig, Thread.currentThread().getId(), i);		
		symRet = sr;
//		Mylog.e("PRE_A3T_RETPUSH", "Pushing for subsig " +symRet.subSignatureId);
	}
	
	public static Expression retpop(int subSig)
	{
//		Mylog.e("PRE_A3T_RETPUSH", "Popping for subsig " + subSig);
		SymRet sr = symRet;
		if(sr != null){
			long ti = sr.threadId;
			if(ti == Thread.currentThread().getId()) {
				int ss = sr.subSignatureId;
				if (ss < 0 || ss == subSig) {
					//ss is negative for @Symbolic annotated methods 
					//and model invoker methods
//					Mylog.e("PRE_A3T_RETPUSH", "Pop returns " + sr.symRet);
					return sr.symRet;
				}
			}
		}		
		return null;
	}

	public static int newEvent()
	{
		eventId++;
		return eventId;
	}
	
	public static int eventId()
	{
		return eventId;
	}

	/**
	 * @param	e	Symbolic value of conditional.
	 *				It is the original value of the conditional, not the one obtained by xor-ing
	 *				it with the result of calling {@link #hijack()} just before the conditional.
	 * @param	branchId	Static ID of conditional.
	 * @param	b	Concrete value of conditional.  
	 */
	public static void assume(Expression e, int branchId, boolean b) 
	{
		if (e == null) {
			Mylog.e("PRE_A3T_BRANCH", "Assume called with null symbolic expression on branch id " + branchId + " and conditional value " + b);
			return;
		}

		Mylog.e("PRE_A3T_BRANCH", e.toYicesString() + "  " + branchId + "  " + b);

		if(!readConfTxt())
			return;

		if (started == false) {
			started = true;  // let testing begin
		}
		
		if (e != null) {
			if (b)
				Expression.pc.assumeTru(e);
			else
				Expression.pc.assumeFls(e);
			String s = (b ? "T" : "F") + branchId;
			Mylog.e("A3T_BRANCH", s);
		}
		return;
	}

	public static void read(Object obj, int fldId) {
		// TODO
	}

	public static void write(Object obj, int fldId) {
		if (started)
			Mylog.e("A3T_WRITE", obj.getClass() + " " + String.valueOf(fldId));
	}

	public static void read(int fldId) {
		if (started)
			Mylog.e("A3T_READ", String.valueOf(fldId));
	}

	public static void write(int fldId) {
		if (eventId > 0 && started)
			Mylog.e("A3T_WRITE", String.valueOf(fldId));
	}

	////only one of the following write and the one 
	////above must be used
	//public static void write(int methId, int fldId) {
	//	if(started)
	//		Mylog.e("A3_WRITE", methId + " " + fldId);
	//}

	public static void rw(int evId, int fldId) {
		Mylog.e("A3T_PRE_RW", (evId-1) + " " + fldId);
		if(evId > 0 && evId != eventId && started){
			Mylog.e("A3T_RW", (evId-1) + " " + fldId);
		}
	}

	public static void ww(int evId, int fldId) {
		Mylog.e("A3T_PRE_WW", (evId-1) + " " + fldId);
		if(evId > 0 && evId != eventId && started){
			Mylog.e("A3T_WW", (evId-1) + " " + fldId);
		}
	}

	public static void readArray(Object obj, int index) {
		if(eventId > 0 && started)
			Mylog.e("A3T_AREAD", System.identityHashCode(obj) + " " + index);
	}
	
	public static void targetHit(Object obj) {
		Mylog.e("A3T_TARGET", obj.toString());
	}

	public static void writeArray(Object obj, int index) {
		if(eventId > 0 && started)
			Mylog.e("A3T_AWRITE", System.identityHashCode(obj) + " " + index);
	}

	public static void only_write(int fldId) {
 		// fldId == -1 => array element write
 	 	if (started && eventId == numEvents)
 			Mylog.e("A3T_WRITE", String.valueOf(fldId));
 	}
	
	private static boolean readConfTxt() {
		Mylog.e("PRE_A3T_in_readconf", "starting");
		if(readConfTxt)
			return monitor;
		readConfTxt = true;
		if(settingsFile.exists()) {
			Mylog.e("PRE_A3T_in_readconf"," exists");

			android.util.Slog.e("A3T", "FOUND FILE: " + settingsFile + " pid = " + android.os.Process.myPid());
			try {
				BufferedReader reader = new BufferedReader(new FileReader(settingsFile));
				String s;
				while ((s = reader.readLine()) != null) {
					String[] a = s.split("=");
					if (a.length != 2) {
						Mylog.e("A3T", "Malformed line in settings.txt; ignoring: " + s);
						continue;
					}
					if (a[0].equals("numevents")) {
						numEvents = Integer.parseInt(a[1]);
					}
				}
				reader.close();
				settingsFile.delete(); //so that other process (e.g., keyboard) know
				monitor = true;
			} catch(IOException e) {
				throw new Error(e);
			}
		} else {
			
			//this is a process such as keyboard that we dont want to trace
			android.util.Slog.e("A3T", "FOUND not FILE: " + settingsFile + " pid = " + android.os.Process.myPid());
			monitor = false;
		}		
		return monitor;
	}

	public static void e(String tag, String msg)
	{
		if(readConfTxt())
			Mylog.e(tag, msg);
	}

	// Methods to create symbolic integers, arrays    
	public static Expression symbolic_int(int seed, String name)
	{
		Expression e = new SymbolicInteger(Types.INT, name, seed);
		return e;
	}

	public static Expression symbolic_boolean(int seed, String name)
	{
		Expression e = new SymbolicInteger(Types.BOOLEAN, name, seed);
		return e;
	}

	public static Expression symbolic_char(char seed, String name)
	{
		Expression ret = new SymbolicInteger(Types.CHAR, name, seed);
		return ret;
	}

	public static Expression symbolic_byte(byte seed, String name)
	{
		Expression ret = new SymbolicInteger(Types.BYTE, name, seed);
		return ret;
	}

	public static Expression symbolic_long(long seed, String name)
	{
		Expression ret = new SymbolicLong(name, seed);
		return ret;
	}

	public static Expression symbolic_float(float seed, String name)
	{
		Expression ret = new SymbolicFloat(name, seed);
		return ret;
	}

	public static Expression symbolic_double(double seed, String name)
	{
		Expression ret = new SymbolicDouble(name, seed);
		return ret;
	}

	public static Expression symbolic_intArray(String name)
	{
		Expression e = new SymbolicIntegerArray(Types.INT, name);
		return e;
	}

	public static Expression symbolic_charArray(String name)
	{
		Expression ret = new SymbolicIntegerArray(Types.CHAR, name);
		return ret;
	}

	public static Expression symbolic_byteArray(String name)
	{
		Expression ret = new SymbolicIntegerArray(Types.BYTE, name);
		return ret;
	}

	public static Expression symbolic_floatArray(String name)
	{
		Expression ret = new SymbolicFloatArray(name);
		return ret;
	}

	public static Expression symbolic_doubleArray(String name)
	{
		Expression ret = new SymbolicDoubleArray(name);
		return ret;
	}
	
	/**
	 * Returns the value which must be enforced for the given variable, according to the current solution.
	 * 
	 */
	public static int getSolution_int(Object local) {
		System.out.println("request for new solution for "+local);
		readLatestSolution();
		if (solutionMap.containsKey(local.toString())) {
			System.out.println(local + " -> " + Integer.parseInt(solutionMap.get(local.toString())));
			return Integer.parseInt(solutionMap.get(local.toString()));
		} else {
			System.err.println("Hm. Solution for " + local + " required but not available. This is the current solutionMap: ");
			for (String key: solutionMap.keySet()) {
				System.out.println("   " + key + " : " + solutionMap.get(key));
			}
			return 123;	
		}
	}

	public static long getSolution_long(Object local) {
		System.out.println("request for new solution for "+local);
		readLatestSolution();
		if (solutionMap.containsKey(local.toString())) {
			System.out.println(local + " -> " + Long.parseLong(solutionMap.get(local.toString())));
			return Long.parseLong(solutionMap.get(local.toString()));
		} else {
			System.err.println("Hm. Solution for " + local + " required but not available. This is the current solutionMap: ");
			for (String key: solutionMap.keySet()) {
				System.out.println("   " + key + " : " + solutionMap.get(key));
			}
			return 123;	
		}
	}

	
	public static java.lang.String getSolution_string(Object local) {
		System.out.println("request for new solution for "+local);
		readLatestSolution();
		if (solutionMap.containsKey(local.toString())) {
			System.out.println(local + " -> " + solutionMap.get(local.toString()));
			return solutionMap.get(local.toString());
		} else {
			System.err.println("Hm. Solution for " + local + " required but not available. This is the current solutionMap: ");
			for (String key: solutionMap.keySet()) {
				System.out.println("   " + key + " : " + solutionMap.get(key));
			}
			return "";	
		}
	}
	
	/**
	 * Check if /sdcard/solution.txt has been modified since last time and reload solutions.
	 *
	 * The solution file looks like this:
	 * sat
	 *  (model 
  	 *  (define-fun $L$0 () Int
     *	  34)
  	 * 	  (define-fun $I$0 () Int
     *	   34)
	 *	 )

	 */
	private static synchronized void readLatestSolution() {
		File solutionFile = new File("/sdcard/solution.txt");		
		if (solutionFile.exists() && solutionFile.canRead()) {
			if (latestSolution < solutionFile.lastModified()) {
				latestSolution  = solutionFile.lastModified();
				solutionMap.clear();
				try {
					BufferedReader br = new BufferedReader(new FileReader(solutionFile));
					
					String line = "";
					
//					* v-ok
//					************************
//					>> SAT
//					------------------------
//					$Xsym_android_os_Build__java_lang_String_BOARD : string -> "q"
//					$I$0 : int -> -18
//					$I$1 : int -> 0
//					$L$sym_java_lang_System_currentTimeMillis__J : int -> -18
//					************************
//					
					while ((line = br.readLine())!=null) {
						System.out.println("SOLUTION: " + line);
						if (line.startsWith("*") || line.startsWith("-"))
							continue;
						else if (line.startsWith(">>")) {
							if (!line.equals(">> SAT")) {
								System.err.println("Not SAT: ("+solutionFile.getName()+")" + line);
								br.close();
								return;
							}				
						} else if (line.contains("->")) {
							Pattern pat = Pattern.compile("([^\\s]*?)\\s+:\\s+(.*?)->\\s+(.*)");
							Matcher mat = pat.matcher(line);
							if (!mat.matches() || mat.groupCount()!=3) {
								br.close();
								throw new Error("Unexpected line format: " + line);
							}
							String varname = mat.group(1);
							String vartype = mat.group(2);
							//TODO handle array and float types
							String varvalue = mat.group(3);
							System.out.println("Varvalue is " + varvalue);
							if ("\"null@0\"".equals(varvalue)) {
								varvalue = null;
								System.out.println("There is a null string in solution map");
							}
							solutionMap.put(varname, varvalue);
						}
					}
					br.close();
					return;
				} catch (IOException ioe) {
					ioe.printStackTrace();
				}
			} else {
				System.out.println("Solution file not modified. No update required.");
			}
		} else {
			System.err.println("Cannot read from solution file " + solutionFile.getAbsolutePath());
		}
	}
}

