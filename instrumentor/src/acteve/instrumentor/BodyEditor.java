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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import soot.Body;
import soot.Local;
import soot.SootMethod;
import soot.Trap;
import soot.Type;
import soot.Unit;
import soot.jimple.Jimple;
import soot.jimple.Stmt;
import soot.util.Chain;

class BodyEditor
{
	Iterator stmtIterator;
	int freshCount;
	Chain locals;
	Stmt originalStmt;
	Stmt currentStmt;
	Stmt firstInsertedStmt;
	Chain stmts;
	Chain traps;

	SootMethod originalMethod; //store the method from which this body is copied from

	// fields to count # of stmts changed
	boolean removed;
	String lastStmt;
	int stmtsChanged = 0;
	int totalStmts = 0;

	Map beginUnitToTraps = new HashMap();
	Map endUnitToTraps = new HashMap();

	public void newBody(Body body, SootMethod originalMethod)
	{
		//wrapTraps(body);
		newBody(body, originalMethod, body.getUnits().snapshotIterator());
		/*
	for (Iterator tit = traps.iterator(); tit.hasNext();) {
	    Trap trap = (Trap) tit.next();
	    Unit beginUnit = trap.getBeginUnit();
	    Set ts = (Set) beginUnitToTraps.get(beginUnit);
	    if (ts == null) {
		ts = new HashSet();
		beginUnitToTraps.put(beginUnit, ts);
	    }
	    ts.add(trap);
	    Unit endUnit = trap.getEndUnit();
	    ts = (Set) endUnitToTraps.get(endUnit);
	    if (ts == null) {
		ts = new HashSet();
		endUnitToTraps.put(endUnit, ts);
	    }
	    ts.add(trap);
	    }*/
	}

	public void newEmptyBody( Body body )
	{
		newBody(body, null, null);
	}

	private void newBody( Body body, SootMethod originalMethod, Iterator stmtIterator )
	{
		this.freshCount = 0;
		this.locals = body.getLocals();
		this.stmtIterator = stmtIterator;
		this.stmts = body.getUnits().getNonPatchingChain();
		this.traps = body.getTraps();
		this.lastStmt = null;
		this.originalMethod = originalMethod;
	}

	public Local freshLocal( Type type )
	{
		freshCount++;
		Local freshL  = Jimple.v().newLocal( new String( "_sym_tmp_" + freshCount ), type );
		locals.addFirst( freshL );
		return freshL;
	}

	public Local freshLocal(Type type, String name)
	{
		Local freshL  = Jimple.v().newLocal(name, type);
		locals.addFirst(freshL);
		return freshL;
	}

	public boolean hasNext()
	{
		if (lastStmt != null)
			checkIfLastStmtChanged();
		return this.stmtIterator.hasNext();
	}

	public Stmt next()
	{
		removed = false;
		currentStmt = originalStmt = (Stmt) this.stmtIterator.next();
		lastStmt = originalStmt.toString();
		firstInsertedStmt = null;
		return originalStmt;
	}

	public void insertStmt(Stmt newStmt, boolean redirect)
	{
		if (stmtIterator == null) {
			stmts.addLast(newStmt);
			return;
		}
		//if (originalStmt instanceof NopStmt) {
		//    System.out.println("NOP " + newStmt);
		//}
		stmts.insertBefore( newStmt, originalStmt );
		// if it is the first stmt inserted while processing originalStmt
		if ( originalStmt == currentStmt ) {
			/*
		  Set beginTraps = (Set) beginUnitToTraps.get(originalStmt);
		  Set endTraps = (Set) endUnitToTraps.get(originalStmt);
		  if (beginTraps != null && endTraps != null)
		  assert false;
		  if (beginTraps != null) {
		  newStmt = Jimple.v().newNopStmt();
		  stmts.insertBefore(newStmt, originalStmt);
		  }
		  if (endTraps != null) {
		  newStmt = Jimple.v().newNopStmt();
		  stmts.insertAfter(newStmt, originalStmt);
		  }
			 */

			firstInsertedStmt = newStmt;


			// all jumps to originalStmt now jump to newStmt
			if (redirect) {
				//System.out.println("redirect " + originalStmt);
				originalStmt.redirectJumpsToThisTo( newStmt );
			}


			/*
			//but dont adjust the traps, redirectJumpsToThisTo does. so lets undo it
			Set traps = (Set) beginUnitToTraps.get(originalStmt);
			if (traps != null) {
			//insert a nop
			for (Iterator tit = traps.iterator(); tit.hasNext();) {
		    Trap t = (Trap) tit.next();
		    t.setBeginUnit(originalStmt);
			}
			}
			traps = (Set) endUnitToTraps.get(originalStmt);
			if (traps != null) {
			for (Iterator tit = traps.iterator(); tit.hasNext();) {
		    Trap t = (Trap) tit.next();
		    t.setEndUnit(originalStmt);
			}
			}
			 */
		}
		if (redirect) {
			currentStmt = newStmt;
		}
	}

	public void insertStmtAfter(Stmt newStmt)
	{
		stmts.insertAfter(newStmt, originalStmt);
	}

	public void insertStmt(Stmt newStmt)
	{
		//System.out.println("insert " + newStmt);
		insertStmt(newStmt, true);
	}

	/** rule is: While processing a stmt, first add all new stmts.
	Finally delete the original one if necessary.
	 */
	public void removeOriginalStmt()
	{
		//if ( originalStmt == currentStmt )
		//    throw new RuntimeException( "a stmt is deleted without inserting any in place of it!" );

		removed = true;

		/*
	Set btraps = (Set) beginUnitToTraps.get(originalStmt);	
	Set etraps = (Set) endUnitToTraps.get(originalStmt);
	boolean trapTarget = btraps != null || etraps != null;
	if (trapTarget) {
	    if (btraps != null) {
		for (Iterator tit = btraps.iterator(); tit.hasNext();) {
		    Trap t = (Trap) tit.next();
		    t.setBeginUnit(firstInsertedStmt);
		}
	    }

	    if (etraps != null) {
		Stmt nop = Jimple.v().newNopStmt();
		stmts.insertBefore(nop, originalStmt);
		for (Iterator tit = etraps.iterator(); tit.hasNext();) {
		    Trap t = (Trap) tit.next();
		    t.setEndUnit(nop);
		}
	    }
	}
		 */
		//System.out.println("remove " + originalStmt);
		stmts.remove( originalStmt );
	}

	public void addTrap(Trap newTrap)
	{
		traps.addLast(newTrap);
	}


	private void checkIfLastStmtChanged()
	{
		if (removed)
			stmtsChanged++;
		else{
			if (!lastStmt.equals(originalStmt.toString()))
				stmtsChanged++;
		}
		totalStmts++;
		return;
	}

	public Stmt originalStmt()
	{
		return originalStmt;
	}

	public SootMethod originalMethod()
	{
		return originalMethod;
	}

	public int getTotalStmts() { return totalStmts; }

	public int getChangedStmts() { return stmtsChanged; }

	void wrapTraps(Body body)
	{
		Chain stmts = body.getUnits().getNonPatchingChain();
		for (Iterator tit = body.getTraps().iterator(); tit.hasNext();) {
			Trap trap = (Trap) tit.next();
			Unit beginUnit = trap.getBeginUnit();
			Unit endUnit = trap.getEndUnit();

			//insert a nop before beginUnit
			Stmt beginNop = Jimple.v().newNopStmt();
			stmts.insertBefore(beginNop, beginUnit);
			trap.setBeginUnit(beginNop);
			Set ts = (Set) beginUnitToTraps.get(beginNop);
			if (ts == null) {
				ts = new HashSet();
				beginUnitToTraps.put(beginNop, ts);
			}
			ts.add(trap);
			//insert a nop after endUnit
			Stmt endNop = Jimple.v().newNopStmt();
			stmts.insertAfter(endNop, endUnit);
			trap.setEndUnit(endNop);
			ts = (Set) endUnitToTraps.get(endNop);
			if (ts == null) {
				ts = new HashSet();
				endUnitToTraps.put(endNop, ts);
			}
			ts.add(trap);
		}	
		//body.validate();
	}
}
