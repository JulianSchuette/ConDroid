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
import java.io.PrintWriter;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class PathsRepo
{
	private static final BlockingQueue<Path> allPaths = new LinkedBlockingQueue();
	private static final AtomicInteger globalId = new AtomicInteger(0);

	static int pathsCount()
	{
		return globalId.get();
	}

	static int nextPathId()
	{
		return globalId.getAndIncrement();
	}
	
	static void addPath(Path p)
	{
		allPaths.add(p);
	}
	
	static Path getNextPath()
	{
		//if(allPaths.isEmpty())
		//	return null;
		//return allPaths.remove(0);
		try{
			return allPaths.poll(2L, TimeUnit.SECONDS);
		}catch(InterruptedException e){
			throw new Error(e);
		}
	}

	/*
	static void saveState(Properties props, Path swbPath)
	{
		StringBuilder builder = new StringBuilder();
		builder.append(swbPath.id() + " " + swbPath.seedId() + " " + swbPath.depth() + ",");
		for(Path p : allPaths) {
			builder.append(p.id() + " " + p.seedId() + " " + p.depth() + ",");
		}
		props.setProperty("toexecute", builder.toString());
		
		props.setProperty("globalid", String.valueOf(globalId));
	}
	
	static void restoreState(Properties props)
	{
		globalId = Integer.parseInt(props.getProperty("globalid"));

		for(String toExecute : props.getProperty("toexecute").split(",")) {
			String[] s = toExecute.split(" ");
			allPaths.add(new Path(Integer.parseInt(s[0]), Integer.parseInt(s[1]), Integer.parseInt(s[2])));
		}
	}
	*/
}

