package acteve.symbolic.string;

import acteve.symbolic.integer.Expression;

public interface String
{
    public Expression _contains(Expression e);
    public Expression _concat(Expression e);
    public Expression _startswith(Expression e);
    public Expression _endswith(Expression e);
}
