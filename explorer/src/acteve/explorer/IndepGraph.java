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
import java.util.Map;
import java.util.HashMap;

public class IndepGraph
{
	private final Map<Integer,Set<Integer>> edges = new HashMap();

	public void newNode(int n)
	{
		assert n >= 0;
		assert !edges.containsKey(n);
		edges.put(n, new HashSet());
	}
	
	public void addEdge(int m, int n)
	{
		edges.get(m).add(n);
		edges.get(n).add(m);
	}

	public Set<Integer> findGreedyMinVertexCover() {
		Set<Integer> cover = new HashSet();

		//add 0-degree nodes to cover
		for(Map.Entry<Integer,Set<Integer>> entry : edges.entrySet()) {
			Integer node = entry.getKey();
			if(entry.getValue().size() == 0) 
				cover.add(node);
		}
		for(Integer node : cover)
			edges.remove(node);
	
		int maxDegreeNode;
		while(true) {
			maxDegreeNode = findMaxDegreeNode();
			if(maxDegreeNode < 0)
				break;
			cover.add(maxDegreeNode);
			removeNode(maxDegreeNode);
		}
		return cover;
	}

	private int findMaxDegreeNode() {
		int maxDegree = 0;
		int maxNode = -1;
		for(Map.Entry<Integer,Set<Integer>> entry : edges.entrySet()) {
			Integer node = entry.getKey();
			Set<Integer> nbrs = entry.getValue();
			int size = nbrs.size();
			if(size > maxDegree) {
				maxDegree = size;
				maxNode = node;
			}
		}
		return maxNode;
	}

	private void removeNode(int node) {
		Set<Integer> nbrs = edges.remove(node);
		for(Integer nbr : nbrs) { 
			Set<Integer> nbrs2 = edges.get(nbr);
			nbrs2.remove(node);
		}
	}
}