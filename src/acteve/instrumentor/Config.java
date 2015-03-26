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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Config {
	Logger log = LoggerFactory.getLogger(Config.class);
	public final String inJars;
	public final String outJar;
	public final String libJars;
	public final String inputMethsFile;
    public final String modelMethsFile;
    public final String modelFieldsFile;
	public final String outDir;
	public final String sdkDir;
    public final RWKind rwKind;
	public final String fldsWhitelist;
	public final String fldsBlacklist;
	public final String methsWhitelist;
	public final boolean instrAllFields;
	public final Set<String> fieldsToModel;

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
		inJars = props.getProperty("android.jar", "libs/android-14.jar");
		outJar = props.getProperty("out.jar", "instrumented.jar");
        libJars = props.getProperty("lib.jars", "jars/a3t_symbolic.jar:jars/a3t_stubs.jar:jars/a3t_models.jar:libs/core.jar:libs/ext.jar:libs/junit.jar:libs/bouncycastle.jar");
		inputMethsFile = props.getProperty("inputmeths.file", "inputs.dat");
		modelMethsFile = props.getProperty("modelmeths.file", "models.dat");
		modelFieldsFile = props.getProperty("modelfields.file", "fieldsToModel.txt");
		outDir = props.getProperty("out.dir", "out");
		sdkDir = props.getProperty("sdk.dir", "out");
		String s = props.getProperty("rw.kind", "id_field_write");
		if (s.equals("none")) 
			rwKind = RWKind.NONE;
		else if (s.equals("id_field_write"))
			rwKind = RWKind.ID_FIELD_WRITE;
		else if (s.equals("explicit_write"))
			rwKind = RWKind.EXPLICIT_WRITE;
		else if (s.equals("only_write"))
			rwKind = RWKind.ONLY_WRITE;
		else
			throw new RuntimeException("Unknown value for rw.kind: " + s);
		
		fldsWhitelist = props.getProperty("whiteflds.file", null);
		fldsBlacklist = props.getProperty("blackflds.file", null);
		methsWhitelist = props.getProperty("whitemeths.file", null);
		instrAllFields = Boolean.getBoolean(props.getProperty("instrflds.all"));

		fieldsToModel = new HashSet<String>();
		File fieldModels = new File("fieldsToModel.txt");
		try {
			BufferedReader br = new BufferedReader(new FileReader(fieldModels));
			String line = "";
			while ((line = br.readLine())!=null) {
				fieldsToModel.add(line);
			}
			br.close();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
				
		log.debug("in.jars=" + inJars);
		log.debug("out.jar=" + outJar);
		log.debug("lib.jars=" + libJars);
		log.debug("inputmeths.file=" + inputMethsFile);
		log.debug("modelmeths.file=" + modelMethsFile);
		log.debug("out.dir=" + outDir);
		log.debug("sdk.dir=" + sdkDir + " (SDK if null)");
		log.debug("rw.kind=" + rwKind);
		log.debug("whiteflds.file=" + fldsWhitelist);
		log.debug("blackflds.file=" + fldsBlacklist);
		log.debug("whitemeths.file=" + methsWhitelist);
		log.debug("instrflds.all="+ instrAllFields);
	}

	public boolean isSDK() {
		// somewhat counterintuitive: we are instrumenting sdk if
		// sdkDir is null.
		return sdkDir == null;
	}
}

