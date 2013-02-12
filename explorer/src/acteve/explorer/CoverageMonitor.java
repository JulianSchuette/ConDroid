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

import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.Iterator;
import java.io.File;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.IOException;

public class CoverageMonitor
{
    // Set of <sid>'s of conds in app code whose T branch was taken in some run at least once.
    private static Set<Integer> trueBranches = new HashSet();
    // Set of <sid>'s of conds in app code whose F branch was taken in some run at least once.
    private static Set<Integer> falseBranches = new HashSet();
    // Set of <sid>'s of conds in app code whose exactly one branch (T or F) was taken in some
    // run at least once and whose other branch was never taken in any run.
    private static Set<Integer> dangBranches = new HashSet();

    private int gain;
    private int count;
    private int prevDanglingBranches;
    
    CoverageMonitor(int runId)
    {
        prevDanglingBranches = dangBranches.size();
    }

    /*
        Called once for each A3T_APP, i.e., each time assume is called on a cond in app code.
        There are two cases:
        1. If meta-data of cond is null: entry is of form "[T|F]<sid>"
        2. If meta-data of cond is non-null: entry is of form "[T|F]<sid> <did>"
        This method ignores the <did> in case 2 above.
    */
    void process(String entry)
    {
        count++;
        int i = entry.indexOf(' ');
        if (i < 0) {    // no metadata
            i = entry.length(); 
        }
        String bidStr = entry.substring(0, i);
        char decision = bidStr.charAt(0);
        int bid = Integer.parseInt(bidStr.substring(1));
        switch (decision) {
        case 'T':
            if(addBranch(bid, trueBranches, falseBranches))
                gain++;
            break;
        case 'F':
            if(addBranch(bid, falseBranches, trueBranches))
                gain++;
            break;
        default:
            throw new RuntimeException("unexpected " + entry);
        }
    } 

    void finish()
    {
        int n = dangBranches.size() - prevDanglingBranches;
        System.out.println("branch cov. = " + count + " (" + gain + " new)");
        System.out.println("change in dang. branches = " + (n > 0 ? "+" : "") + n + " (" + dangBranches.size() + " total)");
    }

    private boolean addBranch(int bid, Set a, Set b)
    {
        boolean ret = a.add(bid);
        if(ret){
            if(b.contains(bid)){
                //the other branch of this stmt was covered earlier
                dangBranches.remove(bid);
            }
            else{
                dangBranches.add(bid);
            }
        }
        return ret;
    }
    
    // Goes over each cond in given file (presumably file of all static conds in app code)
    // and if the <sid> of that cond is in dangBranches (i.e., one of the T|F branches
    // of that cond was taken in at least some run and the other was never taken in any
    // run), then prints that <sid> along with a list of lists:
    static void printDangBranches(String cond_mapTxtFileName)
    {
        if(cond_mapTxtFileName == null)
            return;
        File file = new File(cond_mapTxtFileName);
        if(!file.exists())
            return;
        System.out.println("=========== Dangling branches ===========");
        try{
            BufferedReader reader = new BufferedReader(new FileReader(file));
            String line = reader.readLine();
            if (line == null) {
                System.out.println("empty cond map " + cond_mapTxtFileName);
                return;
            }
            int id = Integer.parseInt(line.substring(0,line.indexOf(":")));
            while(line != null){
                if(dangBranches.contains(id)){
                    String target;
                    if(trueBranches.contains(id)){
                        target = "F"+id;
                        System.out.println("F " + line);
                    }
                    else if(falseBranches.contains(id)){
                        target = "T"+id;
                        System.out.println("T " + line);
                    }
                    else
                        throw new Error("unexpected " + id);
                }
                id++;
                line = reader.readLine();
            }
            reader.close();
        }catch(IOException e){
            throw new Error(e);
        }
    }

}
