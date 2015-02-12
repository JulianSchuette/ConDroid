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

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConcolicExecutor
{
	private static final Logger log = LoggerFactory.getLogger(ConcolicExecutor.class);
	private static ConcolicExecutor v;

	private final BlockingQueue<Emulator> availableEmus = new LinkedBlockingQueue<Emulator>();
	private final int numEmus;
	private final int divergenceThreshold;
	private final int wildEmusThreshold;

	private final AtomicInteger numExecs = new AtomicInteger(0);
	private final AtomicInteger divergenceCount = new AtomicInteger(0);
	private final AtomicInteger ignoredCount = new AtomicInteger(0);
	private final AtomicInteger feasibleCount = new AtomicInteger(0);
	private final AtomicInteger wildEmusCount = new AtomicInteger(0);
	private List<Integer> currentRoundRunIds;

	static void setup(String emuPorts, 
					  String fileName,
					  String appPkgName, 
					  String mainActivity, 
					  String activityArgs, 
					  int divergenceThreshold,
					  int wildEmusThreshold)
	{
		v = new ConcolicExecutor(emuPorts, fileName, appPkgName, mainActivity, activityArgs, divergenceThreshold, wildEmusThreshold);
	}

	static ConcolicExecutor v()
	{
		return v;
	}
	
	private ConcolicExecutor(String emuPorts, 
					  String fileName,
					 String appPkgName, 
					 String mainActivity, 
					 String activityArgs, 
					 int divergenceThreshold,
					 int wildEmusThreshold)
	{
		String[] ports = emuPorts.split(",");
		numEmus = ports.length;
		for(String p : ports) {
			log.debug("using emulator running on port " + p);
			Emulator emu = new Emulator(Integer.parseInt(p), fileName, appPkgName, mainActivity, activityArgs);
			availableEmus.add(emu);
		}
		Emulator.writeToFile(Main.newOutFile(Emulator.PKG_TXT), appPkgName);
		this.divergenceThreshold = divergenceThreshold;
		this.wildEmusThreshold = wildEmusThreshold;
	}

	/**
	 * Starts plain symbolic execution.
	 * 
	 * Returns true if all emulators are free and paths are done.
	 * 
	 * @param currentRoundRunIds
	 * @return
	 */
	public boolean exec(List<Integer> currentRoundRunIds) {
		this.currentRoundRunIds = Collections.synchronizedList(currentRoundRunIds);
		int maxExecs = Config.g().maxExecs;
		
		while(maxExecs > 0)  {
			Path path = null;
			
			//Wait for new path to be assigned to a free emulator
			while(true) {
				path = PathQueue.getNextPath();
				if(path == null) {
					int n = availableEmus.size();
					if(n == numEmus)
						return true;
				} else {
					break;
				}
			}

			MonkeyScript script = null;
			try {
				script = path.generateScript();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			if(script == null) {
				//infeasible path
				numExecs.incrementAndGet();
				continue;
			}
			
			if(wildEmusCount.get() >= wildEmusThreshold)
			break;

			Emulator emu;
			try {
				emu = availableEmus.take();
			}catch(InterruptedException e){
				throw new Error(e);
			}
			if(numExecs.get() >= maxExecs)
				break;
			
			Worker w = new Worker(emu, path, script);
			w.setName("Worker-"+path.id());
			w.start();
		}
		return false;
	}

	void printStats() {
        log.info("(stat) Number of feasible runs = " + feasibleCount.get());
        log.info("(stat) Number of divergent runs = " + divergenceCount.get());
        log.info("(stat) Total number of runs = " + numExecs.get());
	}

	private final class Worker extends Thread {
		private Path path;
		private MonkeyScript script;
		private Emulator emu;
		private boolean emuGoneWild;

		/**
		 * 
		 * @param emu
		 * @param path
		 * @param script May be null, if no monkey script is used.
		 */
		Worker(Emulator emu, Path path, MonkeyScript script) {
			this.emu = emu;
			this.path = path;
			this.script = script;
		}

		public void run() {
			log.info("\n\n\nStarting new path ID {}",path.id());
			ExecResult result = executePath();
			switch(result) {			
				case DIVERGED:
					if(numDivergence(path.id()) <= divergenceThreshold){
						handleDivergence(path);
					} else {
						//ignore this path because it diverged too many times
						ignoredCount.incrementAndGet();
						log.debug("Ignored path: " + path.id());
					}
					break;
				case OK:
					currentRoundRunIds.add(path.id());
					feasibleCount.incrementAndGet();
					numExecs.incrementAndGet();
					break;
				case SWB:
					PathQueue.addPath(path.getRepeatPath());
					break;
			}
			if(!emuGoneWild) {
				try{
					//give the emu an breather
					sleep(2000);
				}catch(InterruptedException e){
					throw new Error("Error occurred while executing " + path.id() + " on " + emu, e);
				}
				availableEmus.add(emu);
			} else {
				wildEmusCount.incrementAndGet();
			}
		}	
		
		private void handleDivergence(Path path2) {
			PathQueue.addPath(path2.getRepeatPath());
			divergenceCount.incrementAndGet();
			feasibleCount.incrementAndGet();
			numExecs.incrementAndGet();
		}

		private ExecResult executePath()
		{
			log.debug("Executing path " + path.id() + " on " + emu);
			ExecResult result = null;
			try{
				emu.exec(path.id(), script);
				log.info("Finished executing path " + path.id() + " on " + emu);
			}catch(EmuGoneWildException e) {
				log.error("Emulator gone wild: {}", e.port());
				result = ExecResult.SWB;
			}catch(Exception e){
				log.error("Error occurred while executing " + path.id() + " on " + emu, e);
				result = ExecResult.SWB;
			}
			try{
				result = path.postProcess();
			}catch(IOException e){
				log.error("Error occurred while post-processing " + path.id() + " on " + emu,e);
				result = ExecResult.SWB;
			}
			log.info("Result of path " + path.id() + " execution is " + result);
			return result;
		}
		
		/**
		 * Returns the number of repetitions this path has already been tested.
		 * 
		 * @param pathId
		 * @return
		 */
		private int numDivergence(int pathId)
		{
			try{
				String script = Emulator.SCRIPT_TXT+"."+pathId;
				//TODO store num of repetitions elsewhere to support useMonkeyScript=false
				BufferedReader reader = Main.newReader(Main.newOutFile(script));
				String line;
				int count = 0;
				while((line = reader.readLine()) != null){
					if(!line.startsWith("//"))
						break;
					if(line.indexOf("Repeat of ") > 0){
						count++;
					} else {
						//count only the trailing consecutive repeats
						count = 0;
					}
				}
				reader.close();
				return count;
			}catch(Throwable e){
				log.error(e.getMessage(),e );
			}
			return 0;
		}
		
	}
	
	
}