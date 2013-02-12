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

import java.util.*;
import java.io.*;

public class Indep
{
	public static boolean check(int prefixId, PathInfo prefixPI, int suffixId, PathInfo suffixPI, int numEvents)
	{
		String outDir = Config.g().outDir;
		String[] trace1 = getTraceStr(numEvents, prefixId, outDir);
		String[] trace2 = getTraceStr(numEvents, suffixId, outDir);
		return check(prefixId, prefixPI, trace1, suffixId, suffixPI, trace2, numEvents);
	}

	private static boolean check(int prefixId, 
								 PathInfo prefixPI, 
								 String[] prefixTraceStr, 
								 int suffixId, 
								 PathInfo suffixPI, 
								 String[] suffixTraceStr,
								 int numEvents)
	{
		for(CommutativeIndices indices : checkCommutative(prefixId, prefixTraceStr, suffixId, suffixTraceStr, numEvents)) {
			int prefixLen = indices.prefixLen;
			int commuteIndex = indices.commuteIndex;
			boolean indep = true;

			//System.out.println("prefixLen = " + prefixLen);
			//System.out.println("commuteIndex = " + commuteIndex);
			
			//System.out.println("testing indep between " + prefixId + " " + suffixId);
			
			List<Set<Integer>> rwSet = unbox(prefixPI.rwSet);
			Set<Integer> rws = new HashSet();
			int i = numEvents - 1;
			for(; i > commuteIndex; i--) 
				rws.addAll(rwSet.get(i));
			//for(Integer x : rws) { System.out.print(x + " "); } System.out.println("");
			for(; indep && i > prefixLen; i--) {
				if(rws.contains(i)) { 
					indep = false;
				}
			}
			
			if(!indep) {
				//System.out.println("dep1 " + i);
				continue;
			}
			
			rwSet = unbox(suffixPI.rwSet);
			rws = new HashSet();
			final int j = prefixLen + numEvents - commuteIndex - 1;
			i = numEvents - 1;
			for(; i > j; i--) 
				rws.addAll(rwSet.get(i));
			
			//for(Integer y : rws) { System.out.print(y + " "); }  System.out.println("");
			for(; indep && i > prefixLen; i--) {
				if(rws.contains(i)) {
					indep = false;
				}
			}
			
			if(!indep) {
				//System.out.println("dep2 " + i);
				continue;
			}

			System.out.println("indep pair " + prefixId + " " + suffixId);
			return true;
		}
		
		return false;
	}
	
	private static List<Set<Integer>> unbox(List<Set<RWRecord>> rwSet)
	{
		List<Set<Integer>> result = new ArrayList();
		for(Set<RWRecord> rws : rwSet) {
			Set<Integer> set = new HashSet();
			result.add(set);
			for(RWRecord rw : rws) {
				set.add(rw.id);
			}
		}
		return result;
	}

	private static class CommutativeIndices {
		int prefixLen;
		int commuteIndex;
		
		CommutativeIndices(int prefixLen, int commuteIndex) {
			this.prefixLen = prefixLen;
			this.commuteIndex = commuteIndex;
		}
	}

	/*
	  (p,q) is in the resulting list if 
          trace1[i] = trace2[i], 0 <= i <= p
          trace1[j] = trace2[j], ...
	 */
	private static List<CommutativeIndices> checkCommutative(int id1, String[] trace1, int id2, String[] trace2, int numEvents) {
		
		List<CommutativeIndices> result = new ArrayList();
		for(int prefixLen = -1; prefixLen < (numEvents-2); prefixLen++) {
			if(prefixLen >= 0) {
				if(!trace1[prefixLen].equals(trace2[prefixLen])) 
					break;
			}
			for(int j = prefixLen+1; j < (numEvents-1); j++) {
				boolean matched = true;
				int k = numEvents-1;
				for(int i = j; matched && i > prefixLen; i--, k--) 
					matched = trace1[i].equals(trace2[k]);
				for(int i = numEvents-1; matched && i > j; i--, k--) 
					matched = trace1[i].equals(trace2[k]);
				if(matched) {
					result.add(new CommutativeIndices(prefixLen, j));
					
					//System.out.println("trace " + id1 + ": ");
					//for(String s : trace1) System.out.print(s+" ");
					//System.out.println("");

					//System.out.println("trace2 " + id2 + ": ");
					//for(String s : trace2) System.out.print(s+" ");
					//System.out.println("");
					
					//System.out.println("matched " + "prefixLen: " + prefixLen + " commuteIndex: " + j);
				}
			}
		}
		return result;
	}

	private static String[] getTraceStr(int numEvents, int id, String outDir) {
		String[] trace = new String[numEvents];
        StringBuilder builder = null;
        try {
            BufferedReader reader = new BufferedReader(new FileReader(new File(outDir, Path.traceFileNameFor(id))));
            String line;
			int count = 0;
            while ((line = reader.readLine()) != null) {
                if (line.equals("")) {
					if(builder != null){
						trace[count++] = builder.toString();
					}
					builder = new StringBuilder();
				}
				else{
					builder.append(line);
				}
            }
            reader.close();
			trace[count] = builder.toString();
			assert (count+1) == numEvents : count + " " + numEvents + " " + id;
        } catch(IOException e) {
            throw new Error(e);
        }
        return trace;
    }

	public static void main(String[] args)
	{
		String outDir = args[0];

		int prefixId = Integer.parseInt(args[1]);
		int suffixId = Integer.parseInt(args[2]);
		
		PathInfo prefixPI = PathInfo.load(new File(outDir, PathInfo.fileName(prefixId)));
		PathInfo suffixPI = PathInfo.load(new File(outDir, PathInfo.fileName(suffixId)));

		int numEvents = prefixPI.numEvents;
		assert suffixPI.numEvents == numEvents;

		String[] prefixTrace = getTraceStr(numEvents, prefixId, outDir);
		String[] suffixTrace = getTraceStr(numEvents, suffixId, outDir);

		if(check(prefixId, prefixPI, prefixTrace, suffixId, suffixPI, suffixTrace, numEvents))
			System.out.println("yes indep");
		else
			System.out.println("no indep");
	}

}