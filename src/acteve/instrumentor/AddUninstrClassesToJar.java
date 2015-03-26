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

package acteve.instrumentor;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.Target;
import org.apache.tools.ant.taskdefs.Jar;
import org.apache.tools.ant.types.ZipFileSet;

/**
   copies the uninstrumented classes from the original
   jar file to the instrumented jar file
*/
public class AddUninstrClassesToJar extends Jar
{
	public AddUninstrClassesToJar(Map<String,List<String>> uninstrumentedClasses,
								  String instrumentedJarName)
	{
		super();
		
		setDestFile(new File(instrumentedJarName));
		setUpdate(true);

		for (Map.Entry<String,List<String>> e : uninstrumentedClasses.entrySet()) {
			String originalJarName = e.getKey();
			ZipFileSet originalJar = new ZipFileSet();
			originalJar.setSrc(new File(originalJarName));

			List<String> classes = e.getValue();
			int numFilesToCopy = classes.size();
			String[] array = new String[numFilesToCopy];
			int i = 0;
			for (String className : classes) {
				className = className.replace('.', File.separatorChar) + ".class";
				array[i++] = className;
			}
			originalJar.appendIncludes(array);

			addZipfileset(originalJar);
		}
	}

	public void apply()
	{
		Project project = new Project();
		setProject(project);

		Target target = new Target();
		target.setName("addtojar");
		target.addTask(this);
		project.addTarget(target);
		target.setProject(project);

		project.init();
		target.execute();
	}

	public static void main(String[] args)
	{
		String originalJarName = args[0];
		String instrumentedJarName = args[1];
		List uninstrumentedClasses = new ArrayList();
		
		for(int i = 2; i < args.length; i++)
			uninstrumentedClasses.add(args[i]);

		Map map = new HashMap();
		map.put(originalJarName, uninstrumentedClasses);
		new AddUninstrClassesToJar(map, instrumentedJarName).apply();
	}
}
