package acteve.symbolic.string;


import acteve.symbolic.integer.Expression;

public class SymbolicString extends StringExpression
{
    private static int count = 0;

    public SymbolicString(java.lang.String name) {
    	this.seed = "Seeed";
    	this.exprString = name;
    }

    static java.lang.String makeName()
    {
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
	public Expression _ne(Expression other) {
		// TODO Auto-generated method stub
		return null;
	}
    
}
