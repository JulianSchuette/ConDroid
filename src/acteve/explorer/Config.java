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
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Config {
	Logger log = LoggerFactory.getLogger(Config.class);
    private static final int DEFAULT_MAX_EXECS = 1000;
    private static final String DEFAULT_EMU_PORT = "5554";

	public final boolean useMonkeyScript;
	public final String monkeyScript;
	public String appPkgName;
	public String mainActivity;
	public final String activityArgs;
	public final String z3Path;
	public final int maxExecs;
	public String outDir;
	public final String emulatorPort;
	public final int userWait;
	public final int K;
	public final boolean checkIndep;
	public final boolean checkReadOnly;
	public final String condMapFile;
	//public final String writeMapFile;
	public final String fieldSigsFile;
	public final String blackListedFieldsFile;
	public final boolean restart;
	public final boolean pruneAfterLastStep;
	public final int divergenceThreshold;
	public final int wildEmusThreshold;
	public String fileName;

	private static Config config;

	public static Config g() {
		if (config == null)
			config = new Config();
		return config;
	}

	private Config() {
		Properties props = new Properties();
		try {
			props.load(new FileInputStream("config.properties"));
		} catch (IOException e) {
			e.printStackTrace();
		}
		
        fileName = props.getProperty("filename", null);
        useMonkeyScript = Boolean.parseBoolean(props.getProperty("useMonkeyScript", "false"));
        monkeyScript = props.getProperty("monkey", "monkey_script.txt");
        appPkgName = props.getProperty("pkg", null);
        mainActivity = props.getProperty("mainact", null);
		activityArgs = props.getProperty("actargs");
        z3Path = props.getProperty("env.Z3_BIN", "/opt/Z3-str_20140720/Z3-str.py");
        maxExecs = Integer.valueOf(props.getProperty("max.iters", String.valueOf(DEFAULT_MAX_EXECS)));
        outDir = props.getProperty("results.dir", "./results/");
        emulatorPort = props.getProperty("port", DEFAULT_EMU_PORT);
        userWait = Integer.valueOf(props.getProperty("userwait", String.valueOf(4)));
        K = Integer.valueOf(props.getProperty("K",String.valueOf(5)));
        checkIndep = Boolean.valueOf(props.getProperty("indep"));
		checkReadOnly = Boolean.valueOf(props.getProperty("readonly"));
        condMapFile = props.getProperty("condmap.file", "bin/a3t/condmap.txt");
		fieldSigsFile = props.getProperty("fieldsigs.file", "bin/a3t/fieldsigs.txt");
		String a3tDir = props.getProperty("env.A3T_DIR");
		blackListedFieldsFile = props.getProperty("blackfields.file", null);
		restart = Boolean.valueOf(props.getProperty("restart"));
		pruneAfterLastStep = Boolean.valueOf(props.getProperty("prune.last"));
		divergenceThreshold = Integer.valueOf(props.getProperty("diverge.threshold", String.valueOf(3)));
		wildEmusThreshold = Integer.valueOf(props.getProperty("wildemus.threshold", String.valueOf(6)));

		log.debug("useMonkeyScript={}", useMonkeyScript);
		log.debug("monkey={}", monkeyScript);
		log.debug("pkg={}", appPkgName);
		log.debug("mainact={}", mainActivity);
		log.debug("actargs={}", activityArgs);
		log.debug("max.iters={}", maxExecs);
		log.debug("out.dir={}", outDir);
		log.debug("port={}", emulatorPort);
		log.debug("userwait={}", userWait);
		log.debug("K={}", K);
		log.debug("indep={}", checkIndep);
		log.debug("readonly={}", checkReadOnly);
		log.debug("condmap.file={}", condMapFile);
		log.debug("fieldsigs.file={}", fieldSigsFile);
		log.debug("blackfields.file={}", blackListedFieldsFile);
		log.debug("restart={}",restart);
		log.debug("prune.last={}", pruneAfterLastStep);
		log.debug("diverge.threshold={}", divergenceThreshold);
		log.debug("wildemus.threshold={}", wildEmusThreshold);

		if (outDir != null && !restart) {
			File d = new File(outDir);
			if(d.exists()) 
				Utils.deleteDir(d);
            d.mkdirs();
		}
	}
}

