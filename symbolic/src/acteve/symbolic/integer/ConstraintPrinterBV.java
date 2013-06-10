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
package acteve.symbolic.integer;

public class ConstraintPrinterBV extends ConstraintPrinter
{
    public String intConstant(int i)
    { 
	if(i < 0){
	    i = Integer.MAX_VALUE + (i + 1);
	    return "(concat bit1 bv"+i+"[31])";
	}
	else
	    return "bv"+i+"[32]";
    }
    
    public String longConstant(long l)
    {
	if(l < 0){
	    l = Long.MAX_VALUE + (l + 1);
	    return "(concat bit1 bv"+l+"[63])";
	}
	else
	    return "bv"+l+"[64]"; 
    }
    
    public String floatConstant(float f){ return String.valueOf(f); }
    
    public String doubleConstant(double d){ return String.valueOf(d); }
    
    public String lt(){ return "bvslt"; }
    
    public String le(){ return "bvsle"; }
    
    public String gt(){ return "bvsgt"; }
    
    public String ge(){ return "bvsge"; }
    
    public String iadd(){ return "bvadd"; }
    
    public String isub(){ return "bvsub"; }
    
    public String imul(){ return "bvmul"; }
    
    public String idiv(){ return "bvsdiv"; }
    
    public String irem(){ return "bvsrem"; }

    public String ineg(){ return "bvneg"; }

    public String ior(){ return "bvor"; }
    
    public String iand(){ return "bvand"; }
    
    public String ixor(){ return "bvxor"; }
    
    public String ishr(){ return "bvashr"; }  //arithmetic shift right

    public String ishl(){ return "bvshl"; }
    
    public String iushr(){ return "bvlshr"; } //logical shift right
    
    public String i2s(){ return "i2s"; }
    
    public String i2b(){ return "i2b"; }
    
    public String i2c(){ return "i2c"; }
    
    public String i2l(){ return "i2l"; }
    
    public String i2d(){ return "i2d"; }
    
    public String i2f(){ return "i2f"; }

    public String ladd(){ return "bvadd"; }
    
    public String lsub(){ return "bvsub"; }

    public String lmul(){ return "bvmul"; }
    
    public String ldiv(){ return "bvsdiv"; }
    
    public String lrem(){ return "bvsrem"; }

    public String lneg(){ return "bvneg"; }

    public String lor(){ return "bvor"; }
    
    public String land(){ return "bvand"; }
    
    public String lxor(){ return "bvxor"; }
    
    public String lshr(){ return "bvashr"; }

    public String lshl(){ return "bvshl"; }
    
    public String lushr(){ return "bvlshr"; }
    
    public String l2i(){ return "l2i"; }
    
    public String l2f(){ return "l2f"; }
    
    public String l2d(){ return "l2d"; }
}
