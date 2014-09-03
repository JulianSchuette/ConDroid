package acteve.symbolic.string;


import acteve.symbolic.integer.BinaryBooleanExpression;
import acteve.symbolic.integer.BooleanBinaryOperator;
import acteve.symbolic.integer.BooleanExpression;
import acteve.symbolic.integer.Expression;

public class SymbolicString extends StringExpression
{
    private static int count = 0;

    //public SymbolicInteger()
    //{
    //    this("int",null);
    //}


    public SymbolicString(java.lang.String name) {
    	this.seed = "Seeed";
    	this.exprString = name;
    }

    static java.lang.String makeName()
    {
    	System.out.println("SymbolicString: making a new name: $S$"+(count+1));
    	Thread.dumpStack();
		return "$S$" + count++;
    }
	
    public java.lang.String toString() 
    {
		return exprString + "[" + seed + "]";
    }
    
    public java.lang.String toYicesString()
    {
		return exprString;
    }

	@Override
	public Expression _contains(Expression e) {
		System.out.println("CONTAINSCONTAINS. SymbolicString._contains called with expression " + e.getClass().getName() + " , " + e.exprString() + " , " + e.toYicesString());
		return BooleanExpression.NEGATION.apply(new BinaryBooleanExpression(new BooleanBinaryOperator("Contains"), this, e));
	}

	@Override
	public Expression _ne(Expression other) {
		// TODO Auto-generated method stub
		return null;
	}
    
}
