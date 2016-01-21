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
import java.io.FileReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Z3StrModelReader
{
	public static Z3Model read(File file)
	{
		System.out.println("Reading from file " + file.getAbsolutePath());
		try{
			BufferedReader br = new BufferedReader(new FileReader(file));
			String line = "";
			
//			* v-ok
//			************************
//			>> SAT
//			------------------------
//			$Xsym_android_os_Build__java_lang_String_BOARD : string -> "q"
//			$I$0 : int -> -18
//			$I$1 : int -> 0
//			$L$sym_java_lang_System_currentTimeMillis__J : int -> -18
//			************************
//			
			Z3Model model = new Z3Model();
			while ((line = br.readLine())!=null) {
				if (line.startsWith("*") || line.startsWith("-"))
					continue;
				else if (line.startsWith(">>")) {
					if (!line.equals(">> SAT")) {
						System.err.println("Not SAT: ("+file.getName()+")" + line);
						br.close();
						return null;
					}				
				} else if (line.contains("->")) {
					Pattern pat = Pattern.compile("([^\\s]*?)\\s+:\\s+(.*?)->\\s+(.*)");
					Matcher mat = pat.matcher(line);
					if (!mat.matches() || mat.groupCount()!=3) {
						br.close();
						throw new Error("Unexpected line format: " + line);
					}
					String varname = mat.group(1);
					String vartype = mat.group(2);
					//TODO handle array and float types
					String varvalue = mat.group(3);
					model.put(varname, varvalue);
				}
			}
			br.close();
			return model;
		}catch(Exception e){
			throw new Error(e);
		}
	}
	
	public static void main(String[] args) 
	{
		String dirName = args[0];
		File dir = new File(dirName);
		if(!dir.exists())
			throw new RuntimeException();

		File[] z3OutFiles;
		if(args.length > 1) {
			z3OutFiles = new File[1];
			z3OutFiles[0] = new File(dir, "z3out."+args[1]);
		}
		else{
			z3OutFiles = dir.listFiles(new java.io.FileFilter() {
					public boolean accept(File f) {
						return f.getName().startsWith("z3out.");
					}
				});
		}
		
		for(File f : z3OutFiles) {
			System.out.println("reading " + f.getName());
			read(f);
		}
	}
}