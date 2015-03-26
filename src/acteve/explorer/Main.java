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
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Properties;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;

import org.xml.sax.SAXException;

public class Main
{
	public static final boolean DEBUG = true;

	public static void main(String[] args) throws XPathExpressionException, IOException, InterruptedException, ParserConfigurationException, SAXException
	{
		Config config = Config.g();
		Utils.deleteDir(new File("results"));
		new File("results").mkdir();
		
		if (args.length>2 && new File(args[0]).exists()) {
			config.fileName = args[0];
			config.appPkgName = args[1];
			config.mainActivity = args[2];
		} else {
			System.out.println("Usage: explorer <apk file> <package name> <mainActivity>");
			System.exit(-1);
		}

        MonkeyScript.setup(config.userWait);
        ConcolicExecutor.setup(config.emulatorPort, 
        			   config.fileName,
					   config.appPkgName, 
					   config.mainActivity, 
					   config.activityArgs, 
					   config.divergenceThreshold,
					   config.wildEmusThreshold);
		System.out.println("Setting up Z3 with " + config.z3Path);
        Z3Task.setup(config.z3Path);
		BlackListedFields.setup(config.fieldSigsFile, config.blackListedFieldsFile);
		
//		ExplorationStrategy explorer = new ActevePathsExplorer();
		ExplorationStrategy explorer = new ConcolicPathsExplorer();
		if(config.restart) {
			//Properties props = loadProperties();
			//PathsRepo.restoreState(props);
			//explorer.restoreState(props);
			throw new Error();
		}

//		explorer.perform(config.K, config.monkeyScript, config.checkReadOnly, config.checkIndep, config.pruneAfterLastStep);
		explorer.perform(config);

//         CoverageMonitor.printDangBranches(config.condMapFile);
	}

	public static Properties loadProperties() {
		Properties props = new Properties();
		try{
			props.load(new FileInputStream(newOutFile("a3tstate.txt")));
		}catch(Exception e){
			throw new Error(e);
		}
		return props;
	}
	
	public static void saveProperties(Properties props) {
		try{
			props.store(new FileOutputStream(newOutFile("a3tstate.txt")), "");
		}catch(IOException e){
			throw new Error(e);
		}
	}

	/**
	 * Creates a new file in OUT directory.
	 * 
	 * @param name
	 * @return
	 */
	public static File newOutFile(String name) {
		return new File(Config.g().outDir, name);
	}

	public static PrintWriter newWriter(File file) throws IOException {
		return new PrintWriter(new BufferedWriter(new FileWriter(file)));
	}

	public static PrintWriter newWriter(File file, boolean append) throws IOException {
		return new PrintWriter(new BufferedWriter(new FileWriter(file, append)));
	}

	public static PrintWriter newWriter(String name) throws IOException {
		return newWriter(newOutFile(name));
	}

	public static BufferedReader newReader(String name) throws FileNotFoundException {
		return newReader(newOutFile(name));
	}

	public static BufferedReader newReader(File file) throws FileNotFoundException {
		return new BufferedReader(new FileReader(file));
	}
}
