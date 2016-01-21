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

import java.util.List;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Set;
import java.util.Map;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Properties;
import java.util.Iterator;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.File;
import java.io.FilenameFilter;

class PathInfo
{
	private static final String PATHINFO = "pathinfo.";

	Set<Integer> writeSet;
	List<Set<RWRecord>> rwSet;
	boolean endsWithPanelClick;
	int traceLength;
	int numEvents;

	void dump(File file)
	{
		Properties props = new Properties();
		props.setProperty("numevents", String.valueOf(numEvents));
		props.setProperty("tracelength", String.valueOf(traceLength));
		props.setProperty("endswithpanelclick", String.valueOf(endsWithPanelClick));

		StringBuilder builder = new StringBuilder();
		for(Integer fid : writeSet)
			builder.append(fid+" ");
		props.setProperty("writeset", builder.toString());

		builder = new StringBuilder();
		for(Set<RWRecord> rws : rwSet){
			for(RWRecord eid : rws)
				builder.append(eid.id+";"+eid.fldId + " ");
			builder.append(",");
		}
		props.setProperty("rwset", builder.toString());

		try{
			FileOutputStream fos = new FileOutputStream(file);
			props.store(fos, "");
			fos.close();
		}catch(IOException e){
			throw new Error(e);
		}
	}

	static PathInfo load(File file)
	{
		Properties props = new Properties();
		try{
			FileInputStream fis = new FileInputStream(file);
			props.load(fis);
			fis.close();
		}catch(Exception e){
			throw new Error(e);
		}

		int numEvents = Integer.parseInt(props.getProperty("numevents"));
		int traceLength = Integer.parseInt(props.getProperty("tracelength"));
		boolean endsWithPanelClick = Boolean.parseBoolean(props.getProperty("endswithpanelclick"));

		String writeSetStr = props.getProperty("writeset").trim();
		Set<Integer> writeSet = new HashSet();
		if(writeSetStr.length() > 0){
			String[] tokens = writeSetStr.split(" ");
			for(String token : tokens){
				writeSet.add(Integer.parseInt(token));
			}
		}

		String rwSetStr = props.getProperty("rwset").trim();
		List<Set<RWRecord>> rwSet = new ArrayList();
		String[] tokens1 = split(rwSetStr, ',');
		for(int i = 0; i < numEvents; i++){
			Set rws = new HashSet();
			rwSet.add(rws);
			String tk1 = tokens1[i];
			if(tk1.length() == 0)
				continue;
			String[] tokens2 = tk1.split(" ");
			int m = tokens2.length;
			for(int j = 0; j < m; j++){
				String tk2 = tokens2[j];
				if(tk2.length() == 0)
					continue;
				String[] tokens3 = tk2.split(";");
				rws.add(new RWRecord(Integer.parseInt(tokens3[0]), Integer.parseInt(tokens3[1])));
			}
		}

		return new PathInfo(numEvents, traceLength, writeSet, rwSet, endsWithPanelClick);
	}

	private static String[] split(String s, char c) 
	{
		int n = s.length();
		List<String> result = new LinkedList();
		int from = 0;
		while(from < n) {
			int next = s.indexOf(c, from);
			if(next < 0) {
				result.add(s.substring(from));
				break;
			}
			if(next == from) 
				result.add("");
			else 
				result.add(s.substring(from, next));
			from = next + 1;
		}
		return result.toArray(new String[0]);
	}

	PathInfo(int numEvents,
			 int traceLength, 
			 Set<Integer> writeSet,
			 List<Set<RWRecord>> rwSet,
			 boolean endsWithPanelClick)
	{
		this.writeSet = writeSet;
		this.rwSet = rwSet;
		this.endsWithPanelClick = endsWithPanelClick;
		this.traceLength = traceLength;
		this.numEvents = numEvents;
	}

	static String fileName(int id) {
		return PATHINFO+id;
	}

}
