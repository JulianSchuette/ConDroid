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

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.taskdefs.ExecTask;
import org.apache.tools.ant.types.Commandline;

import java.io.File;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.regex.Pattern;

public class KillTask extends AdbTask
{
	private static final String PS_OUTPUT = "ps-output.tmp";

	private Pattern pkgName;
	//private String pkgName;

	public KillTask(int port, String pkgName)
	{
		super(port, "shell ps");
		setOutput(Main.newOutFile(PS_OUTPUT+"."+port));
		this.pkgName = Pattern.compile(pkgName);
		//this.pkgName = pkgName;
	}
	
	public void execute()
	{
		super.execute();
		
		File file = Main.newOutFile(PS_OUTPUT+"."+port);
		if(!file.exists())
			throw new BuildException("file " + PS_OUTPUT+"."+port + " does not exist.");

	   
		int pid = -1;
		try{
			BufferedReader reader = new BufferedReader(new FileReader(file));
			String line = reader.readLine();
			while(line != null){
				//System.out.println(line);
				if(pkgName.matcher(line).find()){
					log.debug("process found: " + line);
					char[] cs = line.toCharArray();
					int i = 0;
					while(!Character.isWhitespace(cs[i]))
						i++;
					int begin = i+1;
					while(Character.isWhitespace(cs[begin]))
						begin++;
					int end = begin+1;
					while(!Character.isWhitespace(cs[end]))
						end++;
					try{
						pid = Integer.parseInt(line.substring(begin, end));
					}catch(NumberFormatException e){
						throw new BuildException(e);
					}
					break;
				}
				line = reader.readLine();
			}
			reader.close();
		}catch(IOException e){
			throw new BuildException(e);
		}

		if(pid == -1){
			//System.out.println("the process has already died...did it crash?");
			return;
		}

		try{
			String[] cmd = new String[]{"adb", "-s", "emulator-"+port, "shell", "kill", String.valueOf(pid)};
			//= new String[]{"adb", "shell", "exec", "app_process", "/sdcard", "edu.gatech.symbolic.Hello", String.valueOf(pid)};
			//= new String[]{"adb", "shell", "exec", "app_process", "/sdcard", "edu.gatech.symbolic.Hello", pkgName};
			Process process = Runtime.getRuntime().exec(cmd); 
			process.waitFor();
			int exitValue =  process.exitValue();
			if(exitValue != 0){
				//throw new BuildException("could not kill process " + pid);
				throw new BuildException("could not kill process ");
			}
			else{
				log.debug("killed process " + pid);
			}
		}catch(IOException e){
			throw new BuildException(e);
		}catch(InterruptedException e){
			throw new BuildException(e);
		}		
	}

}
