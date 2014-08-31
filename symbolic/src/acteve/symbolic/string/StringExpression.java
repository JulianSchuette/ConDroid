package acteve.symbolic.string;

import acteve.symbolic.integer.Equality;
import acteve.symbolic.integer.Expression;
import acteve.symbolic.integer.PathCondition;

public abstract class StringExpression extends Expression implements String, Equality
{
    public static final PathCondition pc = new PathCondition();
    
    protected java.lang.String exprString;
    
    public java.lang.String seed;

    public java.lang.String seed()
    {
		return seed;
    }
    
    public java.lang.String toYicesString(java.lang.String str) {
        Expression.pc.printConstraint("(WHAZZAT " + seed+ " " +exprString + ")");
        java.lang.String newName = SymbolicString.makeName();
        Expression.pc.printConstraint("(= " + newName + " " + str + ")");
        return "YicesString of StringExpr";
    }

    //this should be faster at the cost of memory
    public java.lang.String exprString()
    {
	if(exprString == null)
	    exprString = toYicesString();
	return exprString;
    }
    
    public Expression _eq(Expression e)
    {
		System.out.println("CONTAINSCONTAINS. StringExpression._eq called with expression " + e.toYicesString());
		return null;

    }

}
