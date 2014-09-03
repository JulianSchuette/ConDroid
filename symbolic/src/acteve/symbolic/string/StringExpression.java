package acteve.symbolic.string;

import acteve.symbolic.integer.BinaryBooleanExpression;
import acteve.symbolic.integer.BooleanBinaryOperator;
import acteve.symbolic.integer.BooleanExpression;
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
		//TODO string eq
		return null;
    }
    
	@Override
	public Expression _contains(Expression e) {
		//Negated because x = a.contains(b) && (x==0) -> not contained 
		return BooleanExpression.NEGATION.apply(new BinaryBooleanExpression(new BooleanBinaryOperator("Contains"), this, e));
	}

	@Override
	public Expression _startswith(Expression e) {
		return BooleanExpression.NEGATION.apply(new BinaryBooleanExpression(new BooleanBinaryOperator("StartsWith"), this, e));
	}

	@Override
	public Expression _endswith(Expression e) {
		return BooleanExpression.NEGATION.apply(new BinaryBooleanExpression(new BooleanBinaryOperator("EndsWith"), this, e));
	}
	
	@Override
	public Expression _concat(Expression e) {
		return new BinaryStringExpression(new BooleanBinaryOperator("Concat"), this, e);
	}
}
