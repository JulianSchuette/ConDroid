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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ActevePathsExplorer implements ExplorationStrategy
{
	Logger log = LoggerFactory.getLogger(Config.class);
	private List<Integer> currentRoundRunIds = new ArrayList<Integer>();
	private int currentK = 1;

	@Override
	public void perform(Config config) {
		perform(config.K,config.monkeyScript,config.checkReadOnly,config.checkIndep,config.pruneAfterLastStep);
	}
	/**
	 * Explores paths.
	 * 
	 * @param K Maximum path depth
	 * @param monkeyScript Initial monkey script.
	 * @param checkReadOnly Prune RO events.
	 * @param checkIndep
	 * @param pruneAfterLastStep
	 */
	public void perform(int K, String monkeyScript, boolean checkReadOnly, boolean checkIndep, boolean pruneAfterLastStep)
	{
		MonkeyScript seedScript = new ElementaryMonkeyScript(new File(monkeyScript));
		PathQueue.addPath(new Path(seedScript));
		assert K > 0 : String.valueOf(K);
		perform(K, checkReadOnly, checkIndep, pruneAfterLastStep);
		ConcolicExecutor.v().printStats();
	}	

	private void perform(int K, boolean checkReadOnly, boolean checkIndep, boolean pruneAfterLastStep)
	{
		if(!vanillaSymEx()) 
			return;
		System.out.println("First symbolic exec finished. Continue?");
		try { System.in.read();		} catch (IOException e) { e.printStackTrace(); }
		currentK++;
		for (; currentK <= K ; currentK++) {
			List<Integer> prefixes = currentRoundRunIds;
			log.info("(stat) No. of tests generated in round " + (currentK-1) + " = " + prefixes.size());
			prefixes = prune(currentK-1, prefixes, checkReadOnly, checkIndep);
			log.info("(stat) No. of tests to be extended in round " + currentK + " = " + prefixes.size());
			int count = 0;
			for (Integer prefixId : prefixes) {
				extend(prefixId);
			}
			currentRoundRunIds = new ArrayList();
			boolean more = vanillaSymEx();
			System.out.println("Symbolic exec finished. Continue?");
			try { System.in.read();		} catch (IOException e) { e.printStackTrace(); }
			if(!more) {
				return;
			}
		}
		log.info("(stat) No. of tests generated in round {} = {}",(currentK-1), currentRoundRunIds.size());
		if(pruneAfterLastStep) {
			List<Integer> prefixes = currentRoundRunIds;
			prefixes = prune(currentK-1, prefixes, checkReadOnly, checkIndep);
			log.info("(stat) No. of tests to be extended in round {} = {}",  currentK, prefixes.size());
		}
	}

	private List<Integer> prune1(int numEvents, List<Integer> prefixes, boolean checkReadOnly)
	{
		List<Integer> result = new ArrayList();
		for (Integer prefixId : prefixes) {
			PathInfo pinfo = PathInfo.load(Main.newOutFile(PathInfo.fileName(prefixId)));
			if(checkReadOnly && pinfo.endsWithPanelClick){
				log.debug("read-only pruning: {}", prefixId);
				continue;
			}
			result.add(prefixId);
		}
		log.debug("(stat) after read-only pruning = {}", (prefixes.size()-result.size()));
		return result;
	}

	private List<Integer> prune(int numEvents, List<Integer> prefixes, boolean checkReadOnly, boolean checkIndep)
	{
		log.debug("Pruning.");
		if(numEvents == 1)
			return prune1(numEvents, prefixes, checkReadOnly);

		IndepGraph graph = new IndepGraph();
		Map<Integer,PathInfo> idToPInfo = new HashMap();
		for (Integer prefixId : prefixes) {
			PathInfo pinfo = PathInfo.load(Main.newOutFile(PathInfo.fileName(prefixId)));
			if(checkReadOnly && pinfo.endsWithPanelClick) {
				log.debug("read-only pruning: {}", prefixId);
				continue;
			}
			idToPInfo.put(prefixId, pinfo);
			graph.newNode(prefixId);
		}
		log.info("(stat) No of tests pruned by read-only opt. = {}", (prefixes.size()-idToPInfo.size()));		

		if(!checkIndep) {
			List<Integer> result = new ArrayList();
			for(Integer id : idToPInfo.keySet())
				result.add(id);
			return result;
		}
		
		Set<Integer> ids = idToPInfo.keySet();
		Map<Integer,Set<Integer>> checked = new HashMap();
		for(Integer prefixId : ids) {
			for(Integer suffixId : ids) {
				if(suffixId == prefixId)
					continue;
				Set<Integer> is = checked.get(suffixId);
				if(is != null && is.contains(prefixId))
					continue;
				is = checked.get(prefixId);
				if(is == null) {
					is = new HashSet();
					checked.put(prefixId, is);
				}
				is.add(suffixId);

				if(Indep.check(prefixId, idToPInfo.get(prefixId), suffixId, idToPInfo.get(suffixId), numEvents))
					graph.addEdge(prefixId, suffixId);
			}
		}

		List<Integer> result = new ArrayList(graph.findGreedyMinVertexCover());
		log.debug("(stat) No of tests pruned by indep opt. = {}", (idToPInfo.size()-result.size()));		
		return result;
	}

	private void extend(int prefixRunId) {
		log.debug("extended exploration for run {}", prefixRunId);
		PathInfo pinfo = PathInfo.load(Main.newOutFile(PathInfo.fileName(prefixRunId)));
		String prefixScript = Emulator.SCRIPT_TXT+"."+prefixRunId;
		String suffixScript = Emulator.SCRIPT_TXT+".0";

		MonkeyScript preScript = new ElementaryMonkeyScript(Main.newOutFile(prefixScript));
        MonkeyScript sufScript = new ElementaryMonkeyScript(Main.newOutFile(suffixScript));
        MonkeyScript seedScript = new SplicedMonkeyScript(preScript, sufScript);
		seedScript.addComment("Extended from " + prefixScript);

		PathQueue.addPath(new Path(seedScript, prefixRunId, pinfo.traceLength, true));
	}

    private boolean vanillaSymEx() {
		log.debug("Vanilla symex with currentRoundRunIds {}", currentRoundRunIds.toArray().toString());
		return ConcolicExecutor.v().exec(currentRoundRunIds);


    }


}

