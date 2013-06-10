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

package acteve.symbolic;

import java.io.File;
import java.io.BufferedWriter;
import java.io.PrintWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.FileReader;

public class Mylog
{
	private static final String MYLOG = "/mylog.txt";
	private static final String LOG_DIR_PREFIX = "/data/data/";
	private static final String PKG_FILE = "/sdcard/pkg.txt";
	private static final String KILLED_FILE = "/a3t_killed_proc";
	
	private static PrintWriter writer;

	static {
		new ShutDownHook().start();
	}

	private static PrintWriter writer()
	{
		if(writer == null) {
			try{
				writer = new PrintWriter(new BufferedWriter(new FileWriter("/sdcard"+MYLOG)));
			}catch(IOException e){
				throw new Error(e);
			}
		}
		return writer;
	}

	public static void e(String tag, String msg)
	{
		writer().println("E/"+tag+" : "+msg);
	}
	
	public static void println(String msg)
	{
		writer().println(msg);
	}

	private static class ShutDownHook extends Thread
	{
		ShutDownHook()
		{
		}

		public void run()
		{
			File file = new File("/sdcard/a3t_kill_proc");
			while(file.exists()){
				try{
					sleep(500);
				}catch(InterruptedException e){
					break;
				}
			}
			// e("A3T_METHS", Util.reachedMethsStr());
			android.util.Slog.e("Mylog", "Shutting down");
			
			if(writer != null) {
				if(writer.checkError())
					throw new Error("error in writing to mylog.txt");
				writer.flush();
				writer.close();
			}

			try{
				String pkg = new BufferedReader(new FileReader(PKG_FILE)).readLine();
				File killedfile = new File(LOG_DIR_PREFIX+pkg+KILLED_FILE);
				BufferedWriter writer = new BufferedWriter(new FileWriter(killedfile));
				writer.write(0);
				writer.close();
			}catch(IOException e){
				throw new Error(e);
			}
			android.util.Slog.e("Mylog", "About to exit.");
			
			System.exit(0);
		}
	}
}
