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

import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;
import java.io.File;

public class FuzzedMonkeyScript extends MonkeyScript
{
	private static final Map<String,String> symbolPrefixMap = new HashMap();

	private Z3Model model;
	private MonkeyScript seedScript;
	private int tapEventCount = 0;

	static {
		symbolPrefixMap.put(EVENT_KEYWORD_TAP, 
							"$!F$deliverPointerEvent$android$view$MotionEvent$0$");
	}

	public FuzzedMonkeyScript(File seedScript, Z3Model model)
	{
		this.model = model;
		this.seedScript = new ElementaryMonkeyScript(seedScript);
		fuzz();
	}

	private void fuzz()
	{
		copyCommentsFrom(seedScript);

		for(Iterator<Event> it = seedScript.eventIterator(); it.hasNext();) {
			Event ev = it.next();
			addEvent(fuzzEvent((Event) ev));
		}
	}

	private Event fuzzEvent(Event event)
	{
		if(event instanceof TapEvent) {
			try {
				TapEvent ev = (TapEvent) event;

				String varName = symbolPrefixMap.get(EVENT_KEYWORD_TAP);
				varName += (tapEventCount++ * 2);

				float x, y;
				String alias = (String) model.get(varName);
				//System.out.println("varName " + varName + " alias " + alias);
				if(alias != null) {
					Z3Model.Array array = (Z3Model.Array) model.get(alias);
					x = ((Number) array.get(0)).floatValue();
					y = ((Number) array.get(1)).floatValue();
				}
				else {
					x = ev.x();
					y = ev.y();
				}
				return new TapEvent(x, y);
            } catch (NumberFormatException e) {
				throw new Error(e);
            }
		}
		throw new RuntimeException("only tap event supported. " + event.getClass().getName());
	}

}
