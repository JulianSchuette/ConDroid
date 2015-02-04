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

import java.io.FileReader;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.File;

public class ElementaryMonkeyScript extends MonkeyScript
{
	public ElementaryMonkeyScript(File script)
	{
		try{
			if (!script.exists())
				return;
			BufferedReader reader = new BufferedReader(new FileReader(script));
			String line = reader.readLine();
			while(line != null){
				line = line.trim();
				if(line.startsWith("//")) {
					line = line.replace("//","").trim();
					if(line.startsWith("Repeat of "))
						userWait += defaultUserWait;
					addComment(line);
				}
				else {
					Event ev = processLine(line);
					addEvent(ev);
				}
				line = reader.readLine();
			}
			reader.close();	
		}catch(IOException e){
			throw new Error(e);
		}
	}

    public ElementaryMonkeyScript() {
	}

	private Event processLine(String line) 
	{
        int index1 = line.indexOf('(');
        int index2 = line.indexOf(')');

        if (index1 < 0 || index2 < 0) {
			assert false;
        }

        String[] args = line.substring(index1 + 1, index2).split(",");

        for (int i = 0; i < args.length; i++) {
            args[i] = args[i].trim();
        }

		Event ev = createEvent(line, args);
		return ev;
    }

	private Event createEvent(String line, String[] args)
	{
		if(line.startsWith(EVENT_KEYWORD_TAP)){
			try{
				float x = Float.parseFloat(args[0]);
				float y = Float.parseFloat(args[1]);
				TapEvent ev = new TapEvent(x, y);
				return ev;
            } catch (NumberFormatException e) {
				throw new Error(e);
            }
		}
		throw new RuntimeException("only tap events are supported. " + line);
	}
	
}

