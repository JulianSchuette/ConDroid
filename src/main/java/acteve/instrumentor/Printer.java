/* Soot - a J*va Optimization Framework
 * Copyright (C) 2003 Ondrej Lhotak
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the
 * Free Software Foundation, Inc., 59 Temple Place - Suite 330,
 * Boston, MA 02111-1307, USA.
 */

/*
 * Modified by the Sable Research Group and others 1997-1999.  
 * See the 'credits' file distributed with Soot for the complete list of
 * contributors.  (Soot is distributed at http://www.sable.mcgill.ca/soot)
 */

package acteve.instrumentor;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import soot.AttributesUnitPrinter;
import soot.Body;
import soot.BriefUnitPrinter;
import soot.LabeledUnitPrinter;
import soot.Local;
import soot.Modifier;
import soot.NormalUnitPrinter;
import soot.Scene;
import soot.SootClass;
import soot.SootField;
import soot.SootMethod;
import soot.Trap;
import soot.Type;
import soot.Unit;
import soot.UnitPrinter;
import soot.options.Options;
import soot.tagkit.JimpleLineNumberTag;
import soot.tagkit.Tag;
import soot.toolkits.graph.UnitGraph;
import soot.util.Chain;
import soot.util.DeterministicHashMap;

/**
* Prints out a class and all its methods.
*/
public class Printer {
	private static Printer printer = new Printer();
	private Printer() { }
    public static Printer v() {
        return printer;
    }

    final private static char fileSeparator =
        System.getProperty("file.separator").charAt(0);

    public static final int USE_ABBREVIATIONS = 0x0001, ADD_JIMPLE_LN = 0x0010;

    public boolean useAbbreviations() {
        return (options & USE_ABBREVIATIONS) != 0;
    }

    public boolean addJimpleLn() {
        return (options & ADD_JIMPLE_LN) != 0;
    }

    int options = 0;
    public void setOption(int opt) {
        options |= opt;
    }
    public void clearOption(int opt) {
        options &= ~opt;
    }

    int jimpleLnNum = 0; // actual line number

    public int getJimpleLnNum() {
        return jimpleLnNum;
    }
    public void setJimpleLnNum(int newVal) {
        jimpleLnNum = newVal;
    }
    public void incJimpleLnNum() {
        jimpleLnNum++;
    }

    public void printTo(SootClass cl, PrintWriter out) {
        // add jimple line number tags
        setJimpleLnNum(1);

        // Print class name + modifiers
        {
            StringTokenizer st =
                new StringTokenizer(Modifier.toString(cl.getModifiers()));
            while (st.hasMoreTokens()) {
                String tok = st.nextToken();
                if( cl.isInterface() && tok.equals("abstract") ) continue;
                out.print(tok + " ");
            }

            String classPrefix = "";

            if (!cl.isInterface()) {
                classPrefix = classPrefix + " class";
                classPrefix = classPrefix.trim();
            }

            out.print(
                classPrefix + " " + Scene.v().quotedNameOf(cl.getName()) + "");
        }

        // Print extension
        {
            if (cl.hasSuperclass())
                out.print(
                    " extends "
                        + Scene.v().quotedNameOf(cl.getSuperclass().getName())
                        + "");
        }

        // Print interfaces
        {
            Iterator interfaceIt = cl.getInterfaces().iterator();

            if (interfaceIt.hasNext()) {
                out.print(" implements ");

                out.print(
                    ""
                        + Scene.v().quotedNameOf(
                            ((SootClass) interfaceIt.next()).getName())
                        + "");

                while (interfaceIt.hasNext()) {
                    out.print(",");
                    out.print(
                        " "
                            + Scene.v().quotedNameOf(
                                ((SootClass) interfaceIt.next()).getName())
                            + "");
                }
            }
        }

        out.println();
        incJimpleLnNum();
        out.println("{");
        incJimpleLnNum();
        if (Options.v().print_tags_in_output()){
            Iterator cTagIterator = cl.getTags().iterator();
            while (cTagIterator.hasNext()) {
                Tag t = (Tag) cTagIterator.next();
                out.print("/*");
                out.print(t.toString());
                out.println("*/");
            }
        }

        // Print fields
        {
            Iterator fieldIt = cl.getFields().iterator();

            if (fieldIt.hasNext()) {
                while (fieldIt.hasNext()) {
                    SootField f = (SootField) fieldIt.next();

                    if (f.isPhantom())
                        continue;

                    if (Options.v().print_tags_in_output()){
                        Iterator fTagIterator = f.getTags().iterator();
                        while (fTagIterator.hasNext()) {
                            Tag t = (Tag) fTagIterator.next();
                            out.print("/*");
                            out.print(t.toString());
                            out.println("*/");
                        }
                    }
                    out.println("    " + f.getDeclaration() + ";");
                    if (addJimpleLn()) {
                        setJimpleLnNum(addJimpleLnTags(getJimpleLnNum(), f));		
                    }
                }
            }
        }

        // Print methods
        {
            Iterator methodIt = cl.methodIterator();

            if (methodIt.hasNext()) {
                if (cl.getMethodCount() != 0) {
                    out.println();
                    incJimpleLnNum();
                }

                while (methodIt.hasNext()) {
                    SootMethod method = (SootMethod) methodIt.next();

                    if (method.isPhantom())
                        continue;

                    if (!Modifier.isAbstract(method.getModifiers()) && !Modifier.isNative(method.getModifiers())) {
                        if (Options.v().print_tags_in_output()){
                        	Iterator mTagIterator = method.getTags().iterator();
                        	while (mTagIterator.hasNext()) {
                        		Tag t = (Tag) mTagIterator.next();
                        		out.print("/*");
                        		out.print(t.toString());
                        		out.println("*/");
                        	}
                        }
                    	Body body = method.retrieveActiveBody();
                        printTo(body, out);

                        if (methodIt.hasNext()) {
                            out.println();
                            incJimpleLnNum();
                        }
                    } else {
                           
                        if (Options.v().print_tags_in_output()){
                            Iterator mTagIterator = method.getTags().iterator();
                            while (mTagIterator.hasNext()) {
                                Tag t = (Tag) mTagIterator.next();
                                out.print("/*");
                                out.print(t.toString());
                                out.println("*/");
                            }
                        }
                        
                        out.print("    ");
                        out.print(method.getDeclaration());
                        out.println(";");
                        incJimpleLnNum();
                        if (methodIt.hasNext()) {
                            out.println();
                            incJimpleLnNum();
                        }
                    }
                }
            }
        }
        out.println("}");
        incJimpleLnNum();
    }

    /**
     *   Prints out the method corresponding to b Body, (declaration and body),
     *   in the textual format corresponding to the IR used to encode b body.
     *
     *   @param out a PrintWriter instance to print to.
     */
    public void printTo(Body b, PrintWriter out) {
        if (Main.VALIDATE) {
        	b.validate();
        }

        boolean isPrecise = !useAbbreviations();

        String decl = b.getMethod().getDeclaration();

        out.println("    " + decl);
    
        if (addJimpleLn()) {
            setJimpleLnNum(addJimpleLnTags(getJimpleLnNum(), b.getMethod()));		
        }

        out.println("    {");
        incJimpleLnNum();
        
        UnitGraph unitGraph = new soot.toolkits.graph.BriefUnitGraph(b);

        LabeledUnitPrinter up;
        if( isPrecise ) up = new NormalUnitPrinter(b);
        else up = new BriefUnitPrinter(b);

        if (addJimpleLn()) {
            up.setPositionTagger( new AttributesUnitPrinter(getJimpleLnNum()) );
        }
	
        printLocalsInBody(b, up);

        printStatementsInBody(b, out, up, unitGraph);

        out.println("    }");
        incJimpleLnNum();

    }

    /** Prints the given <code>JimpleBody</code> to the specified <code>PrintWriter</code>. */
    private void printStatementsInBody(Body body, java.io.PrintWriter out, LabeledUnitPrinter up, UnitGraph unitGraph ) {
    	Chain units = body.getUnits();
        Iterator unitIt = units.iterator();
        Unit currentStmt = null, previousStmt;

        while (unitIt.hasNext()) {

            previousStmt = currentStmt;
            currentStmt = (Unit) unitIt.next();

            // Print appropriate header.
            {
                // Put an empty line if the previous node was a branch node, the current node is a join node
                //   or the previous statement does not have body statement as a successor, or if
                //   body statement has a label on it

                if (currentStmt != units.getFirst()) {
                    if (unitGraph.getSuccsOf(previousStmt).size() != 1
                        || unitGraph.getPredsOf(currentStmt).size() != 1
                        || up.labels().containsKey(currentStmt)) {
                        up.newline();
                    } else {
                        // Or if the previous node does not have body statement as a successor.

                        List succs = unitGraph.getSuccsOf(previousStmt);

                        if (succs.get(0) != currentStmt) {
                            up.newline();
                        }
                    }
                }

                if (up.labels().containsKey(currentStmt)) {
                    up.unitRef( currentStmt, true );
                    up.literal(":");
                    up.newline();
                }

                if (up.references().containsKey(currentStmt)) {
                    up.unitRef( currentStmt, false );
                }
            }

            up.startUnit(currentStmt);
            currentStmt.toString(up);
            up.endUnit(currentStmt);

            up.literal(";");
            up.newline();

            // only print them if not generating attributes files 
            // because they mess up line number
            //if (!addJimpleLn()) {
            if (Options.v().print_tags_in_output()){
                Iterator tagIterator = currentStmt.getTags().iterator();
                while (tagIterator.hasNext()) {
                    Tag t = (Tag) tagIterator.next();
                    up.noIndent();
                    up.literal("/*");
                    up.literal(t.toString());
                    up.literal("*/");
                    up.newline();
                }
                /*Iterator udIt = currentStmt.getUseAndDefBoxes().iterator();
                while (udIt.hasNext()) {
                    ValueBox temp = (ValueBox)udIt.next();
                    Iterator vbtags = temp.getTags().iterator();
                    while (vbtags.hasNext()) {
                        Tag t = (Tag) vbtags.next();
                        up.noIndent();
                        up.literal("VB Tag: "+t.toString());
                        up.newline();
                    }
                }*/
            }
        }

        out.print(up.toString());
		if (addJimpleLn()){
			setJimpleLnNum(up.getPositionTagger().getEndLn());
		}


        // Print out exceptions
        {
            Iterator trapIt = body.getTraps().iterator();

            if (trapIt.hasNext()) {
                out.println();
                incJimpleLnNum();
            }

            while (trapIt.hasNext()) {
                Trap trap = (Trap) trapIt.next();

                out.println(
                    "        catch "
                        + Scene.v().quotedNameOf(trap.getException().getName())
                        + " from "
                        + up.labels().get(trap.getBeginUnit())
                        + " to "
                        + up.labels().get(trap.getEndUnit())
                        + " with "
                        + up.labels().get(trap.getHandlerUnit())
                        + ";");

                incJimpleLnNum();

            }
        }

    }

    private int addJimpleLnTags(int lnNum, SootMethod meth) {
    	meth.addTag(new JimpleLineNumberTag(lnNum));
	lnNum++;
	return lnNum;
    }
    
    private int addJimpleLnTags(int lnNum, SootField f) {
    	f.addTag(new JimpleLineNumberTag(lnNum));
	lnNum++;
	return lnNum;
    }

    /** Prints the given <code>JimpleBody</code> to the specified <code>PrintWriter</code>. */
    private void printLocalsInBody(
        Body body,
        UnitPrinter up) {
        // Print out local variables
        {
            Map<Type, List> typeToLocals =
                new DeterministicHashMap(body.getLocalCount() * 2 + 1, 0.7f);

            // Collect locals
            {
                Iterator localIt = body.getLocals().iterator();

                while (localIt.hasNext()) {
                    Local local = (Local) localIt.next();

                    List<Local> localList;

                    Type t = local.getType();

                    if (typeToLocals.containsKey(t))
                        localList = typeToLocals.get(t);
                    else {
                        localList = new ArrayList<Local>();
                        typeToLocals.put(t, localList);
                    }

                    localList.add(local);
                }
            }

            // Print locals
            {
                Iterator<Type> typeIt = typeToLocals.keySet().iterator();

                while (typeIt.hasNext()) {
                    Type type = typeIt.next();

                    List localList = typeToLocals.get(type);
                    Object[] locals = localList.toArray();
                    up.type( type );
                    up.literal( " " );

                    for (int k = 0; k < locals.length; k++) {
                        if (k != 0)
                            up.literal( ", " );

                        up.local( (Local) locals[k] );
                    }

                    up.literal(";");
                    up.newline();
                }
            }

            if (!typeToLocals.isEmpty()) {
                up.newline();
            }
        }
    }
}
