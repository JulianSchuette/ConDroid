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
import org.apache.tools.ant.Target;
import org.apache.tools.ant.taskdefs.ExecTask;
import org.apache.tools.ant.types.Commandline;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Z3Task extends ExecTask
{
	private static final Logger log = LoggerFactory.getLogger(Z3Task.class);
	private static String args;
	private final Target target = new Target();

	public Z3Task()
	{
		setExecutable("/bin/sh");

		Project project = new Project();
		setProject(project);

		target.setName("runZ3");
		target.addTask(this);
		project.addTarget(target);
		target.setProject(project);

		project.init();
	}
	
	public void exec(File outFile, File errFile, String file)
	{		
		log.trace("Executing Z3Task with outfile " + outFile.getAbsolutePath() + " and errFile " + errFile.getAbsolutePath() + " and file " + file);
		Commandline.Argument cmdLineArgs = createArg();
		String args2 = args + file;

		args2 += " 1>"+outFile;
		args2 += " 2>"+errFile;

		args2 = args2 + "\"";

		cmdLineArgs.setLine(args2);

		//setError(errFile);
		//setOutput(outFile);
		log.debug("Running Z3 " + args2);

		target.execute();
	}

	public void execute()
	{
		super.execute();
	}
	
	static void setup(String z3Path)
	{
		if(z3Path == null)
			throw new Error("z3Path is null");
		File file = new File(z3Path);
		if(!file.exists())
			throw new Error("z3Path does not exist. " + z3Path);
		args = "-c \"" + file.getAbsolutePath() + " -f ";		
	}
}
