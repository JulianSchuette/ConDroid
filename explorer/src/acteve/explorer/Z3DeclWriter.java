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

import java.io.File;
import java.io.FileWriter;
import java.io.BufferedWriter;
import java.io.PrintWriter;
import java.io.IOException;
import java.util.Set;
import java.util.HashSet;
import java.util.regex.Pattern;
import java.util.regex.Matcher;


public class Z3DeclWriter
{
	private static final Pattern varNamePat = Pattern.compile("\\$[^ \\)]+( |\\))");

	private PrintWriter pcDeclWriter;
	private Set<String> varNames = new HashSet();

	Z3DeclWriter(File file)
	{
		try{
			pcDeclWriter = new PrintWriter(new BufferedWriter(new FileWriter(file)));
		}catch(IOException e){
			throw new Error(e);
		}
	}
	
	void printComment(String comment) {
		pcDeclWriter.println(";"+comment);
	}

	void process(String pc)
	{
		//find the vars
		Matcher matcher = varNamePat.matcher(pc);
		while(matcher.find()){
			String varName = matcher.group();
			varName = varName.substring(0, varName.length()-1);
			if(varNames.contains(varName))
				continue;
			varNames.add(varName);

			char ch = varName.charAt(0);
			assert ch == '$';
		
			boolean array = false;
			int i = 1;
			ch = varName.charAt(i);
			if(ch == '!'){
				array = true;
				i++;
			}
			String type = null;
			char typeCode = varName.charAt(i);
			switch(typeCode){
			case 'I':
			case 'B':
			case 'S':
			case 'C':
			case 'L':			
				type = "Int";
      		break;
			case 'F':
			case 'D':
				type = "Real";
		    break;
			case 'X':
				type = "String";
				break;
			default:
				throw new RuntimeException(varName);
			}
			
			if(array){
				type = "(Array Int " + type + ")";
			}
			
			String decl = "(declare-const " + varName + " " + type + ")";	
			pcDeclWriter.println(decl);	
		}
	}

	private void addConstraintsAtLeast38()
	{
		for(String varName : varNames){
			if(varName.startsWith("$!F$deliverPointerEvent$android$view$MotionEvent$0$")){
				String constr = "(assert (>= (select " + varName + " 1) 39.0))";
				pcDeclWriter.println(constr);
			}
		}
	}

	private void addToIntDecl()
	{
		pcDeclWriter.println("(define-fun my_to_int ((x Real)) Int (if (>= x 0.0) (to_int x) (- (to_int (- x)))))");
		pcDeclWriter.println("(define-fun l2i ((x Int)) Int x)");
		pcDeclWriter.println("(define-fun l2f ((x Int)) Real (to_real x))");
		pcDeclWriter.println("(define-fun l2d ((x Int)) Real (to_real x))");
	}
	
	void finish()
	{
		addToIntDecl();
		addConstraintsAtLeast38();
		pcDeclWriter.close();
	}
}