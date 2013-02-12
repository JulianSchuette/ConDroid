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
import java.io.File;
import java.io.PrintWriter;
import java.io.IOException;
import java.io.BufferedReader;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Target;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;

public class Emulator extends Task
{
	private final Project project;

	private static final String WORK_DIR = "a3t_workdir";
	private static final String DEVICE_DIR = "/sdcard";
	public static final String MONKEY_OUT = "monkeyout.";
	public static final String LOGCAT_OUT = "logcatout.";
	public static final String SYSLOG_OUT = "syslog.";
	public static final String SCRIPT_TXT = "script.txt";
	public static final String PKG_TXT = "pkg.txt";
	private static final String SETTINGS_TXT = "settings.txt";

	private List<Task> subtasks = new ArrayList();
	private Target target = new Target();

	private AdbTask logcatBegin;
	private AdbTask logcatEnd;
	private AdbTask runMonkey;
	private AdbTask pushPkgNameFile;
	private AdbTask pushMonkeyScript;
	private AdbTask startActivity;
	private KillerTask killActivity;
	private AdbTask unlockPhone;
	private PullLogCatTask pullLogCat;
	private AdbTask pushSettingsFile;
	private KillTask initialKillActivity;
	private ClearHistoryTask clearHistory;

	private boolean isFirst = true;
	private File tmpLogCatFile;
	private File settingsFile;
	private File scriptFile;

	private final int port;

	private void createTasks(String appPkgName, String mainActivity, String activityArgs)
	{
		this.pushPkgNameFile = new AdbTask(port, "push " + Main.newOutFile(PKG_TXT).getAbsolutePath() + " " + DEVICE_DIR + "/" + PKG_TXT);
		subtasks.add(pushPkgNameFile);

		this.settingsFile = Main.newOutFile(settingsFileName());
		this.pushSettingsFile = new AdbTask(port, "push " + settingsFile.getAbsolutePath() + " " + DEVICE_DIR + "/" + SETTINGS_TXT);
		subtasks.add(pushSettingsFile);

		this.clearHistory = new ClearHistoryTask(port, appPkgName);
		subtasks.add(clearHistory);

		this.tmpLogCatFile = Main.newOutFile(tmpLogCatFileName());
		this.pullLogCat = new PullLogCatTask(port, appPkgName, tmpLogCatFile);
		subtasks.add(pullLogCat);

		this.scriptFile = Main.newOutFile(scriptFileName());
		String scriptTxt = DEVICE_DIR+"/"+SCRIPT_TXT;
		this.pushMonkeyScript = new AdbTask(port, "push " + scriptFile.getAbsolutePath() + " " + scriptTxt);
		subtasks.add(pushMonkeyScript);
		

		String amArgs = "-W -n " + appPkgName + "/" + mainActivity;
		if(activityArgs != null)
			amArgs +=  " " + activityArgs;
		this.startActivity = new AdbTask(port, "shell am start " + amArgs); 
		subtasks.add(startActivity);
		
		this.runMonkey = new AdbTask(port, "shell monkey -v -v -v -f " + scriptTxt + " " + "1");
		subtasks.add(runMonkey);
		
		this.killActivity = new KillerTask(port);
		subtasks.add(killActivity);
		
		this.unlockPhone = new AdbTask(port, "shell input keyevent 82");
		subtasks.add(unlockPhone);		

		this.logcatBegin = new AdbTask(port, "shell logcat -c");
		subtasks.add(logcatBegin);
		
		this.logcatEnd = new AdbTask(port, "shell logcat -d");		
		subtasks.add(logcatEnd);				

		this.initialKillActivity = new KillTask(port, appPkgName);
		subtasks.add(initialKillActivity);
	}

	public void execute()
	{
		execute(logcatBegin);
		if (isFirst) {
			execute(unlockPhone);
			execute(initialKillActivity);
			execute(pushPkgNameFile);
			isFirst = false;
		}
		execute(clearHistory);
		execute(pushSettingsFile);
		execute(pushMonkeyScript);
		execute(startActivity);

		killActivity.prepare();
		pullLogCat.prepare();

		execute(runMonkey);
		execute(killActivity);
		execute(pullLogCat);
		execute(logcatEnd);
	}

	public void exec(int executingId, MonkeyScript script)
	{
		script.generate(scriptFile);

		StringBuilder builder = new StringBuilder();
		builder.append("numevents="+script.length());
		writeToFile(settingsFile, builder.toString());

		//int executingId = path.id();
		File logCatFile = Main.newOutFile(LOGCAT_OUT+executingId);
		File monkeyFile = Main.newOutFile(MONKEY_OUT+executingId);
		File syslogFile = Main.newOutFile(SYSLOG_OUT+executingId);
		runMonkey.setOutput(monkeyFile);
		logcatEnd.setOutput(syslogFile);
		
		target.execute();

		Path.copy(tmpLogCatFile, logCatFile);
		tmpLogCatFile.delete();
		
		//try{
		//	return path.postProcess(logCatFile, monkeyFile, syslogFile, checkTransition);
		//}catch(IOException e){
		//	throw new BuildException(e);
		//}
	}
	
	public Emulator(int port, String appPkgName, String mainActivity, String activityArgs)
	{
		this.port = port;

		createTasks(appPkgName, mainActivity, activityArgs);

		this.project = new Project();
		this.setProject(project);

		target = new Target();
		target.setName("anexec");
		target.addTask(this);
		project.addTarget(target);
		target.setProject(project);

		for(Task subtask : subtasks){
			subtask.setProject(project);
			//subtask.setResultProperty("a3t.taskresult");
		}

		project.init();
	}	

	private void execute(Task task)
	{
		//project.setProperty("a3t.taskresult", null);
		task.execute();
		//String result = project.getProperty("a3t.taskresult");
		//int res = Integer.parseInt(result);
		//if(res != 0)
		//		System.out.println("Error occurred executing task: " + task.getClass());
	}

	public String toString()
	{
		return "emulator-"+port;
	}
	
	private String settingsFileName()
	{
		return SETTINGS_TXT+".emu"+port;
	}

	private String tmpLogCatFileName()
	{
		return LOGCAT_OUT+"emu"+port;
	}

	private String scriptFileName()
	{
		return SCRIPT_TXT + ".emu"+port;
	}

	static void writeToFile(File file, String content)
	{
		try{
			PrintWriter writer = Main.newWriter(file);
			writer.println(content);
			writer.close();
		}catch(IOException e){
			throw new BuildException(e);
		}
	}
}
