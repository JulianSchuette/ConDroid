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
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/* 
   flip at the branch at the index depth in the trace 
   of the execution whose index id is seedId.
   In other words, the trace of this path will match up
   (modulo divergence) with that of the seed execution
   for the initial (depth-1) branches unless this path is
   extended. If extended path, the trace of this path is
   going to match entirely to the trace of the seeId run.
*/
public class Path
{
	private static final Logger log = LoggerFactory.getLogger(Path.class);
    private static final String TRACE = "trace";
    private static final String PC = "pc";
    private static final String BRANCH_MARKER = "E/A3T_BRANCH";
    private static final String PC_MARKER = "E/A3T_PC";
    private static final String APP_MARKER = "E/A3T_APP";
    private static final String MAPPING_MARKER = "E/A3T_MAPPING";
    private static final String ITER_MARKER = "E/A3T_ITER";
    private static final String RW_MARKER = "E/A3T_RW";
    private static final String WW_MARKER = "E/A3T_WW";
    private static final String READ_MARKER = "E/A3T_READ";
    private static final String WRITE_MARKER = "E/A3T_WRITE";
    private static final String AREAD_MARKER = "E/A3T_AREAD";
    private static final String AWRITE_MARKER = "E/A3T_AWRITE";
    private static final String METH_MARKER = "E/A3T_METHS";
    static final String Z3_OUT = "z3out.";
    private static final String Z3_ERR = "z3err.";

    private int seedId;

    private int depth;
    
    private int id;

    private boolean extended;

    public Path() {
    	this(null, -1, 0);
    }
    
	/**
	   this constructor is used for generation-0 paths
	 */
	public Path(MonkeyScript seedScript)
	{
		this(seedScript, -1, 0);
	}

	/**
	   this constructor is used for extended paths
	 */
	public Path(MonkeyScript seedScript, int seedId, int depth, boolean extended)
    {
        this(seedId, depth);
		this.extended = extended;
//        seedScript.saveAs(Main.newOutFile(Emulator.SCRIPT_TXT+"."+id));
    }

	/**
	   this constructor is used for repeat executions
	 */
	public Path(MonkeyScript seedScript, int seedId, int depth)
    {
		this(seedScript, seedId, depth, false);
    }

	/**
	   this constructor is used for fuzzed paths
	 */
    private Path(int seedId, int depth)
    {
		this.seedId = seedId;
		this.depth = depth;
        this.id = PathQueue.nextPathId();
    	log.trace("New path {} with new seed {} and depth {}",id, seedId, depth);
    }

    public int id()
    {
        return id;
    }
    
    /**
     * This is where a new monkey script is generated which leads to different execution path.
     * 
     * @return
     * @throws IOException
     */
    MonkeyScript generateScript() throws IOException
    {
    	File scriptToRun = Main.newOutFile(Emulator.SCRIPT_TXT+"."+id);    	

        File pcDeclFile = Main.newOutFile(pcDeclFileNameFor(seedId));
        File smtFile = Main.newOutFile(smtFileNameFor(id));

//		if(scriptToRun.exists()) {
//	        System.out.println("Script exists: " + scriptToRun.getAbsolutePath());
//			//if this is a path that is gen-0 path or 
//			//generated from extension or repeatation
//	        MonkeyScript ms = new ElementaryMonkeyScript(scriptToRun);
//			//ms.generate(Main.newOutFile(Emulator.SCRIPT_TXT));
//			return ms;
//	    }
        log.debug("Generating new script");
        log.debug("	Smt file        : {} - {} ", smtFile.exists(), smtFile.getAbsolutePath());
        log.debug("	pc file         : {} - {} ", Main.newOutFile(pcFileNameFor(seedId)).exists(), new File(pcFileNameFor(seedId)).getAbsolutePath());
        log.debug("	pcDecl file     : {} - {} ", pcDeclFile.exists(), pcDeclFile.getAbsolutePath());
        log.debug("	scriptToRun file: {} - {} ", scriptToRun.exists(), scriptToRun.getAbsolutePath());
        log.debug("	Depth: {}", depth);
        log.debug("	id: {}", id);
        log.debug("	seedId: {}", seedId);

        
        if (pcDeclFile.exists())
        	copy(pcDeclFile, smtFile);

        if (Main.newOutFile(pcFileNameFor(seedId)).exists()) {
	        PrintWriter smtWriter = Main.newWriter(smtFile, true);
	        BufferedReader pcReader = Main.newReader(pcFileNameFor(seedId));
	        
	        String line = pcReader.readLine();
	        int i = 1;
	        while(i < depth){
	            char c = line.charAt(0);
	            if(c != '*')
	                i++;
	            else
	                line = line.substring(1);
	            smtWriter.println("(assert "+line+")");
	            line = pcReader.readLine();
	        }
	        char c = line.charAt(0);
	        while(c == '*'){
	            smtWriter.println("(assert "+line.substring(1)+")");
	            line = pcReader.readLine();
	            c = line.charAt(0);
	        }
	        
	        //Invert last condition
	        log.debug("Inverting " + line);
	        smtWriter.println("(assert (not "+line+"))");
	
	        smtWriter.println("(check-sat)");
	        smtWriter.println("(get-model)");
	        
	        pcReader.close();
	        smtWriter.close();
        }
        if (smtFile.exists()) {
	        File z3OutFile = Main.newOutFile(Z3_OUT+id);
	        File z3ErrFile = Main.newOutFile(Z3_ERR+id);
	        log.trace("Z3 Out file {}", z3OutFile.getAbsolutePath());
	        log.trace("Z3 Err file {}", z3ErrFile.getAbsolutePath());
	        new Z3Task().exec(z3OutFile, z3ErrFile, smtFile.getAbsolutePath());
	        
	        Z3Model model = Z3StrModelReader.read(z3OutFile);
	        if(model != null){
	        	log.info("** Solved model for path id {} **",id);
	        	model.print();
	            File seedMonkeyScript = Main.newOutFile(Emulator.SCRIPT_TXT+"."+seedId);
	            MonkeyScript newMonkeyScript = new FuzzedMonkeyScript(seedMonkeyScript, model);
				newMonkeyScript.addComment("Fuzzed from " + Emulator.SCRIPT_TXT+"."+seedId);
	            newMonkeyScript.saveAs(scriptToRun);
				return newMonkeyScript;
	        }
        } else {
        	log.debug("No smt file exists, nothing to solve");
        	MonkeyScript ms = new ElementaryMonkeyScript();
			ms.generate(Main.newOutFile(Emulator.SCRIPT_TXT));
			return ms;
        }
        return null;
    }

    /*
       Dump the entire trace. (Called after each execution)
       This trace will be read when this execution will be used as seed execution in future.
       Trace contains events of following form:
        1. E/A3T_BRANCH : [T|F] <sid> #<did>
           Printed in assume whenever meta-data AND symValue in that meta-data is non-null.
       Ex: E/A3T_BRANCH : F24292 #42

        2. E/A3T_PC : <constraint>
           Printed in assume whenever meta-data AND symValue in that meta-data is non-null.
           Also printed (with *) in assumeDet.
       Ex: E/A3T_PC : (and (and (< $I$2 480) (>= $I$2 0)) (and (< $I$3 800) (>= $I$3 38)))

        3. E/A3T_APP : [T|F]<sid>
		   Printed in assume whenever an app code branch AND meta-data is null.
       Ex: E/A3T_APP : F24292

        4. E/A3T_APP : [T|F]<sid> <did>
		   Printed in assume whenever an app code branch AND meta-data is non-null.
       Ex: E/A3T_APP : F24292 1456
    */
    ExecResult postProcess() throws IOException
    {		
		File logCatFile = Main.newOutFile(Emulator.LOGCAT_OUT+id);
		File monkeyFile = Main.newOutFile(Emulator.MONKEY_OUT+id);
		File syslogFile = Main.newOutFile(Emulator.SYSLOG_OUT+id);

		if(checkForSWB(syslogFile)) {
			log.error("******** Something went bad! runid = " + id + " **********");
			return ExecResult.SWB;
		}
        List<String> expectedList = getExpectedTrace();


        PrintWriter traceWriter = Main.newWriter(traceFileNameFor(id));
        PrintWriter pcWriter = Main.newWriter(pcFileNameFor(id));
        Z3DeclWriter declWriter = new Z3DeclWriter(Main.newOutFile(pcDeclFileNameFor(id)));
//        CoverageMonitor covMonitor = new CoverageMonitor(id);
        RWAnalyzer rwAnalyzer = new RWAnalyzer();
		ReadOnlyLastTapDetector roltDetector = new ReadOnlyLastTapDetector();

        int count = 0, numEvents = 0, expectedSize = expectedList.size();
        boolean diverged = false;
        String line, prevLine="";
        if(!(logCatFile.length() > 0)) throw new Error("Empty trace file");
        
        declWriter.printComment("Iteration " + id + " Count " + count + " Path depth" + expectedSize);
        
        BufferedReader reader = Main.newReader(logCatFile);
        while ((line = reader.readLine()) != null) {
            if (line.startsWith(BRANCH_MARKER)) {
            	log.debug("Found branch marker! " + line + " | " + prevLine);
                int i = line.indexOf(':');
                String bid = line.substring(i+2).trim();
                if (!diverged && count < expectedSize) {
                    String expBid = expectedList.get(count);
                    //Strip comment from Branch ID
                    if (expBid.contains("//"))
                    	expBid=expBid.substring(0,expBid.indexOf("//")-1).trim();
                    if (!expBid.equals(bid)) {
                        log.info("********* DIVERGED at index: " + count +
                            " (expected=" + expBid + " got=" + bid + ") " + "runid = " + id + " **********");
                        diverged = true;
                    }
                }
                traceWriter.println(bid + "  // " + prevLine);
                // XXX depInfo.process(did);
                count++;
				//} else if (line.startsWith(APP_MARKER)) {
                //line = line.substring(line.indexOf(':')+2).trim();
                //covMonitor.process(line);
            } else if (line.startsWith(PC_MARKER)) {
                line = line.substring(line.indexOf(':')+2).trim();
//                System.out.println("    Writing PC_MARKER to PC file " + pcFileNameFor(id) + " and decl file " + pcDeclFileNameFor(id));
                pcWriter.println(line);
                declWriter.process(line);
				//panelDetector.process(pc);
            } else if (line.startsWith(ITER_MARKER)) {
				line = line.substring(line.indexOf(':')+2).trim();
                traceWriter.println("");      //empty line to separate out branches coming from diff events
				rwAnalyzer.iter(line);
				roltDetector.iter();
                numEvents++;
            } else if (line.startsWith(WRITE_MARKER)) {
				line = line.substring(line.indexOf(':')+2).trim();
				roltDetector.process(line);
            } else if (line.startsWith(RW_MARKER)) {
				line = line.substring(line.indexOf(':')+2).trim();
				rwAnalyzer.rw(line);
			} else if (line.startsWith(WW_MARKER)) {
				line = line.substring(line.indexOf(':')+2).trim();
				rwAnalyzer.rw(line);
			} else if (line.startsWith(AREAD_MARKER)) {
				line = line.substring(line.indexOf(':')+2).trim();
				rwAnalyzer.aread(line);
			} else if (line.startsWith(AWRITE_MARKER)) {
				line = line.substring(line.indexOf(':')+2).trim();
				rwAnalyzer.awrite(line);
				roltDetector.processArray(line);
			}
            prevLine = line;
        }

        if (!diverged && count < expectedSize) {
            log.info("******* DIVERGED: expected length>=" + expectedSize + " got=" + count + " runid = " + id + " **********");
            diverged = true;
        }
        reader.close();
        traceWriter.close();
        pcWriter.close();
        declWriter.finish();
        //covMonitor.finish();
		rwAnalyzer.finish();
		roltDetector.finish();

		if(diverged) {
			return ExecResult.DIVERGED;
		}

		generateNextGenPaths(count);
		/*
		  int transited = -1;
		  if(checkTransition){
		  transited = ScreenTransition.check(id);
		  System.out.println("screen transition: " + (transited == 0 ? "none" : (transited == -1 ? "return" : "call")));
		  }
		*/
		boolean endsWithPanelClick = roltDetector.readOnlyLastTap();//panelDetector.lastTapOnPanel();
		PathInfo info = new PathInfo(numEvents, 
									 count, 
									 roltDetector.writeSet(), 
									 rwAnalyzer.rwSet(), 
									 endsWithPanelClick);
		info.dump(Main.newOutFile(PathInfo.fileName(id)));
		return ExecResult.OK;
    }

    /**
     * Returns the sequence of expected branch markers for the current path.
     * TODO expected is only set by ActevePathExplorer.
     * @return
     * @throws IOException
     */
    private List<String> getExpectedTrace() throws IOException {
        List<String> pathTxtList = new ArrayList<String>();
		if (seedId >= 0) {
            assert depth > 0;
            String line;
            // trace file of seed execution must have at least "depth" number of lines
            String traceFile = traceFileNameFor(seedId);
            BufferedReader reader = Main.newReader(traceFile);
            for (int i = 0; i < depth - 1;) {
                line = reader.readLine();
                if(!line.equals("")){
                    i++;
                    //pathTxtWriter.println(line);
                    pathTxtList .add(line);
                }
            }
			do {
				line = reader.readLine();
			} while(line !=null && line.equals(""));
			if (line!=null) {
				if(extended) {
					pathTxtList.add(line);
				} else {
					char c = line.charAt(0);
					assert c == 'T' || c == 'F' : "unexpected " + line;
					String flip = (c == 'T' ? "F" : "T") + line.substring(1);
					//pathTxtWriter.println(flip);
					pathTxtList.add(flip);
				}
			}

            reader.close();
        }
        return pathTxtList;
	}

	void generateNextGenPaths(int traceLength)
    {
        for(int i = traceLength;  i > depth; i--){
        	log.trace("in generateNextGenPaths: traceLength {}, depth {}", traceLength, i);
            Path newPath = new Path(id, i);
            PathQueue.addPath(newPath);
        }
    }

	Path getRepeatPath()
	{
		String divergingScript = Emulator.SCRIPT_TXT+"."+id;
		MonkeyScript seedScript = new ElementaryMonkeyScript(Main.newOutFile(divergingScript));
		seedScript.addComment("Repeat of " + divergingScript);
		log.debug("Creating repeat path. SeedId {}, depth {}",seedId, depth);
		return new Path(seedScript, seedId, depth);
	}

	private boolean checkForSWB(File sysLog)
	{
		try{
			BufferedReader reader = Main.newReader(sysLog);
			String line;
			while((line = reader.readLine()) != null){
				if(line.indexOf("ANR") >= 0) {  //TODO: make the pattern more specific
					log.warn("**** WARN: " + line);
					reader.close();
					return true;
				} else if(line.indexOf("thread exiting with uncaught exception") >= 0) {
					return true;
				}
			}
			reader.close();
		}catch(Exception e){
			throw new Error(e);
		}
		return false;
	}
    
    public static String traceFileNameFor(int id)
    {
        return TRACE + "." + id;
    }

    public static String smtFileNameFor(int id)
    {
        return PC + "." + id + ".smt2";
    }

    public static String pcFileNameFor(int id)
    {
        return PC + "." + id;
    }

    public static String pcDeclFileNameFor(int id)
    {
        return PC + "." + id + ".decl";
    }

    static void copy(File srcFile, File dstFile)
    {
    	log.trace("Copying from " + srcFile.getAbsolutePath() + " to " + dstFile.getAbsolutePath());
        try{
            FileInputStream in = new FileInputStream(srcFile);
            FileOutputStream out = new FileOutputStream(dstFile);
            
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0){
                out.write(buf, 0, len);
            }
            in.close();
            out.close();
            //System.out.println("File copied.");
        }
        catch(IOException e){
            throw new Error(e);
        }
    }
    
}
