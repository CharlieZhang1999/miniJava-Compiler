package miniJava.AbstractSyntaxTrees;

import miniJava.SyntacticAnalyzer.SourcePosition;

public class FieldInitialization extends FieldDecl {

	public FieldInitialization(boolean isPrivate, boolean isStatic, TypeDenoter t, String name, SourcePosition posn) {
		super(isPrivate, isStatic, t, name, posn);
		// TODO Auto-generated constructor stub
	}
	
	public FieldInitialization(boolean isPrivate, boolean isStatic, TypeDenoter t, String name, SourcePosition posn, Expression e) {
		super(isPrivate, isStatic, t, name, posn);
		this.initialization = e;
		// TODO Auto-generated constructor stub
	}
	
	public <A, R> R visit(Visitor<A, R> v, A o) {
        return v.visitFieldInitialization(this, o);
    }
	public Expression initialization;
}
