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

import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

public class RWAnalyzer
{
	/*
	  if i^{th} event reads what j^{th} (j < i) wrote then
	  i^{th} set in the rwSet list contains j.
	 */
	private final List<Set<RWRecord>> rwSet = new ArrayList();

	private final List<Set<RWRecord>> arrayWriteSet = new ArrayList();
	private Set<RWRecord> curRWSet;
	private Set<RWRecord> curArrayWriteSet;

	RWAnalyzer()
	{		
	}

	void iter(String iter)
	{
		curRWSet = new HashSet();
		rwSet.add(curRWSet);
		
		curArrayWriteSet = new HashSet();
		arrayWriteSet.add(curArrayWriteSet);
	}

	void rw(String str)
	{
		String[] tokens = str.split(" ");
		int writeEventId = Integer.parseInt(tokens[0]);
		int fldId = Integer.parseInt(tokens[1]);
		if(BlackListedFields.check(fldId))
			return;
		curRWSet.add(new RWRecord(writeEventId, fldId));
	}
	
	void aread(String str)
	{
		String[] tokens = str.split(" ");
		int arrayObjId = Integer.parseInt(tokens[0]);
		int index = Integer.parseInt(tokens[1]);
		RWRecord rec = new RWRecord(arrayObjId, index);		
		int count = findLastWrite(rec);
		if(count >= 0)
			curRWSet.add(new RWRecord(count, -1));
	}

	void awrite(String str)
	{
		String[] tokens = str.split(" ");
		int arrayObjId = Integer.parseInt(tokens[0]);
		int index = Integer.parseInt(tokens[1]);
		RWRecord rec = new RWRecord(arrayObjId, index);
		int count = findLastWrite(rec);
		if(count >= 0)
			curRWSet.add(new RWRecord(count, -1));
		curArrayWriteSet.add(rec);
	}

	private int findLastWrite(RWRecord rec) 
	{
		int count = arrayWriteSet.size() - 1;		
		if(arrayWriteSet.get(count).contains(rec))
			return -1;
		count--;
		for(; count >= 0; count--){
			if(arrayWriteSet.get(count).contains(rec))
				break;
		}
		return count;
	}

	void finish()
	{
	}
	
	List<Set<RWRecord>> rwSet()
	{
		return rwSet;
	}
}

