/**
 * miniJava Abstract Syntax Tree classes
 * @author prins
 * @version COMP 520 (v2.2)
 */
package miniJava.AbstractSyntaxTrees;

import miniJava.SyntacticAnalyzer.SourcePosition;

public class BaseType extends TypeDenoter
{
    public BaseType(TypeKind t, SourcePosition posn){
        super(t, posn);
    }
    
    public <A,R> R visit(Visitor<A,R> v, A o) {
        return v.visitBaseType(this, o);
    }
    
    public boolean equals(TypeDenoter t) {
    	if(this.typeKind == TypeKind.UNSUPPORTED || this.typeKind == TypeKind.ERROR) {
    		return false;
    	}
    	else if(t == null) {
    		return false;
    	}
    	else if(t.typeKind == TypeKind.UNSUPPORTED || t.typeKind == TypeKind.ERROR) {
    		return false;
    	}
    	else if(t instanceof BaseType && this.typeKind == t.typeKind) {
    		return true;
    	}
    	else if(this.typeKind == TypeKind.NULL || t.typeKind == TypeKind.NULL) {
    		return true;
    	}
    	else {
    		return false;
    	}
    	
    }
}
