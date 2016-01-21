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

import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PullLogCatTask extends Task
{
	private static final Logger log = LoggerFactory.getLogger(PullLogCatTask.class);
	private AdbTask pullKilledProcFile;
	private AdbTask pullLogCatFile;
	private AdbTask rmDeviceLogCatFile;
	private AdbTask rmDeviceKilledProcFile;
	private File killedFile;
	private final int port;
	private File tmpLogCatFile;

    private static final String LOG_FILE = "/mylog.txt";
    private static final String LOG_DIR_PREFIX = "/data/data/";
	private static final String KILLED_FILE = "a3t_killed_proc";
	private static final int MAX_TRY = 5;

	PullLogCatTask(int port, String appPkgName, File tmpLogCatFile)
	{
		this.port = port;
		String deviceLogCatFilePath = "/sdcard"+LOG_FILE;//LOG_DIR_PREFIX+appPkgName+LOG_FILE;
		String deviceKilledFilePath = LOG_DIR_PREFIX+appPkgName+"/"+KILLED_FILE;

		killedFile = Main.newOutFile(KILLED_FILE+".emu"+port);
		this.tmpLogCatFile = tmpLogCatFile;
		pullKilledProcFile = new AdbTask(port, "pull " + deviceKilledFilePath + " " + killedFile.getAbsolutePath());
		pullLogCatFile = new AdbTask(port, "pull " + deviceLogCatFilePath  + " " + tmpLogCatFile.getAbsolutePath());
		rmDeviceLogCatFile = new AdbTask(port, "shell rm " + deviceLogCatFilePath);
		rmDeviceKilledProcFile = new AdbTask(port, "shell rm " + deviceKilledFilePath);
	}

	void prepare()
	{
		if(killedFile.exists() && !killedFile.delete())
			throw new Error("cannot delete " +killedFile.getAbsolutePath());
		rmDeviceKilledProcFile.execute();
		rmDeviceLogCatFile.execute();
	}
	
	public void execute()
	{
		int count = 0;
		while(!killedFile.exists() && count < MAX_TRY) {
			try{
				pullKilledProcFile.execute();
			}catch(Exception e){
				count++;
				log.trace("waiting for emulator-"+port+ " to finish.");
			}
			try{
				Thread.sleep(count*500);
			}catch(InterruptedException e){
				throw new Error("thread sleep");
			}
		}
		if(!killedFile.exists()) {
			throw new EmuGoneWildException(port);
		} else {
			System.out.println("");
			pullLogCatFile.execute();
//			try {
//				BufferedReader r = new BufferedReader(new FileReader(tmpLogCatFile));
//				String line = null;
//				System.out.println("Logcat file follows. Does it contain BRANCH?");
//				while ((line=r.readLine())!=null) {
//					System.out.println("   " + line);
//				}
//			} catch (NullPointerException | IOException npe) {
//				npe.printStackTrace();
//			}
		}
		
	}

	public void setProject(Project pr)
	{
		pullKilledProcFile.setProject(pr);
		pullLogCatFile.setProject(pr);
		rmDeviceLogCatFile.setProject(pr);
		rmDeviceKilledProcFile.setProject(pr);
	}

}
