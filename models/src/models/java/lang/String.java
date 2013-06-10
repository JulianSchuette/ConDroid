package models.java.lang;

import acteve.symbolic.integer.*;

public class String
{
    public static Expression equals__Ljava_lang_Object_2__Z(Object thisString, 
							    Expression thisString$sym, 
							    Object otherString, 
							    Expression otherString$sym)
    {
	if(thisString$sym == null || otherString$sym == null)
	    return null;
	return equals.apply(thisString$sym, otherString$sym);
    }

    public static Expression  Ljava_lang_String_2(java.lang.String seed, java.lang.String name)
    {
	System.out.println("hello");
	return new SymbolicString(seed, name);
    }
    
    public static Expression get(java.lang.String constant)
    {
	return new StringConstant(constant);
    }
    
    public static Expression charAt(Expression thisString, Expression index)
    {
	return charAt.apply(thisString, index);
    }
    
    public static Expression concat(Expression thisString, Expression otherString)
    {
	return concat.apply(thisString, otherString);
    }
    
    private static final BinaryOperator equals = 
	new BinaryOperator("java.lang.String.equals"){
	    public Expression apply(Expression leftOp, Expression rightOp)
	    {
		System.out.println(((StringExpression) leftOp).seed + " " + ((StringExpression) rightOp).seed);
		//boolean r = ((StringExpression) leftOp).seed.equals(((StringExpression) rightOp).seed);
		return new BinaryIntegerExpression(this, leftOp, rightOp);
	    }
	};

    private static final BinaryOperator charAt =
	new BinaryOperator("java.lang.String.charAt"){
	    public Expression apply(Expression leftOp, Expression rightOp)
	    {
			//char r = ((StringExpression) leftOp).seed.charAt(((IntegerExpression) rightOp).seed);
		return new BinaryIntegerExpression(this, leftOp, rightOp);
	    }
	};

    private static final BinaryOperator concat =
	new BinaryOperator("java.lang.String.concat"){
	    public Expression apply(Expression leftOp, Expression rightOp)
	    {
		java.lang.String r = ((StringExpression) leftOp).seed.concat(((StringExpression) rightOp).seed);
		return new BinaryStringExpression(this, leftOp, rightOp, r);
	    }
	};
}

abstract class StringExpression extends Expression
{
    java.lang.String seed;
    
    StringExpression(java.lang.String seed)
    {
	super();
	this.seed = seed;
    }
}

class SymbolicString extends StringExpression
{
    SymbolicString(java.lang.String seed, java.lang.String name)
    {
	super(seed);
	this.exprString = name;
    }
    
    public java.lang.String toYicesString()
    {
	return exprString;
    }
}

class StringConstant extends StringExpression
{
    StringConstant(java.lang.String seed)
    {
	super(seed);
	this.exprString = "\""+seed+"\"";
    }
    
    public java.lang.String toYicesString()
    {
	return exprString;
    }
}

class BinaryStringExpression extends StringExpression
{
    Expression left;
    Expression right;
    BinaryOperator op;
    
    BinaryStringExpression(BinaryOperator op, Expression left, Expression right, java.lang.String seed)
    {
	super(seed);
	this.left = left;
	this.right = right;
	this.op = op;
    }
    
    public java.lang.String toString () 
    {
	return "(" + left.toString() + " " + op.toString() + right.toString() + ")"; // + "[" + seed + "]";
    }
    
    public java.lang.String toYicesString()
    {
	return op.toYicesString(left.exprString(), right.exprString()); // +"[" + seed + "]";
    }
}
