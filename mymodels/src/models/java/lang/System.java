package models.java.lang;

import acteve.symbolic.integer.*;

public class System
{
//    public static Expression currentTimeMillis____J(java.lang.Object obj, Expression obj$sym)
//    {
//    	return new acteve.symbolic.integer.SymbolicLong(0);
//    }
    public static Expression currentTimeMillis____J()
    {
    	java.lang.System.out.println("MyModels: Generating new symbolic long");
    	return new acteve.symbolic.integer.SymbolicLong(42);
    }
}
