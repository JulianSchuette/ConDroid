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
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

public class SigNumberer {
	private final Map<String, Integer> sigToId = new HashMap<String, Integer>();

	public int getNumber(String s) {
		Integer i = sigToId.get(s);
		if (i != null)
			return i.intValue();
		int id = sigToId.size();
		sigToId.put(s, id);
		return id;
	}

	public void save(String fileName) {
		String[] a = new String[sigToId.size()];
		for (Map.Entry<String, Integer> e : sigToId.entrySet()) {
			int id = e.getValue();
			a[id] = e.getKey();
		}
        try {
            PrintWriter out = new PrintWriter(new File(fileName));
			for (String s : a)
				out.println(s);
            out.close();
        } catch (IOException ex) {
            ex.printStackTrace();
            System.exit(1);
        }
	}

	public void load(String fileName) {
        try {
			File file = new File(fileName);
			if (!file.exists())
				return;
            BufferedReader reader = new BufferedReader(new FileReader(file));
            String s;
			for (int id = 0; (s = reader.readLine()) != null; id++) {
				sigToId.put(s, id);
			}
			reader.close();
        } catch (IOException ex) {
            ex.printStackTrace();
            System.exit(1);
        }
	}
}
