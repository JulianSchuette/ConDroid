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

import java.io.FileWriter;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.File;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

public class MonkeyScript
{
	protected static final String EVENT_KEYWORD_TAP = "Tap";
	protected static int defaultUserWait;

	protected List<Event> events = new ArrayList();
	protected List<String> comments = new ArrayList();
	protected int userWait = defaultUserWait;

	/* 
	   wait in seconds before and after each event
	 */
	static void setup(int wait)
	{
		defaultUserWait = wait*1000;
	}

	protected void addEvent(Event ev)
	{
		events.add(ev);
	}

	protected void addComment(String comment)
	{
		comments.add(comment);
	}

	protected void copyCommentsFrom(MonkeyScript other)
	{
		comments.addAll(other.comments);
	}

	protected Iterator<Event> eventIterator()
	{
		return events.iterator();
	}

	public void saveAs(File file) 
	{
		try{
			BufferedWriter writer = new BufferedWriter(new FileWriter(file));
			for(String c : comments) 
				writer.write("//"+c+"\n");

			for(Event event : events){
				writer.write(event.toString());
				writer.newLine();
			}
			writer.close();
		}catch(IOException e){
			throw new Error(e);
		}
	}

	public int length()
	{
		return events.size();
	}
	
	public void generate(File file) 
	{
		try{
			BufferedWriter writer = new BufferedWriter(new FileWriter(file));
						
			//write prelude
			writer.write("count = " + (1+1+events.size()) + "\n"); 
			writer.write("speed = 1000\n");
			writer.write("start data >>\n");
//			writer.write("DispatchKey(0,0,0,82,0,0,0,0)\n");

			for(Event event : events){
				writer.write("UserWait("+userWait+")\n");
				writer.write(event.toString()+"\n");
			}
			writer.write("UserWait("+userWait+")\n");
			writer.close();
		}catch(IOException e){
			throw new Error(e);
		}
	}
}

abstract class Event
{
}
  
class TapEvent extends Event
{
	private float mX = -1;
	private float mY = -1;

	TapEvent(float x, float y)
	{
		this.mX = x;
		this.mY = y;
	}

	float x()
	{
		return this.mX;
	}

	float y()
	{
		return this.mY;
	}
	
	public String toString()
	{
		return MonkeyScript.EVENT_KEYWORD_TAP+"("+mX+","+mY+")";
	}
}
