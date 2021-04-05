/**
 * miniJava Abstract Syntax Tree classes
 * @author prins
 * @version COMP 520 (v2.2)
 */
package miniJava.AbstractSyntaxTrees;

import miniJava.SyntacticAnalyzer.SourcePosition;

public class ClassType extends TypeDenoter
{
    public ClassType(Identifier cn, SourcePosition posn){
        super(TypeKind.CLASS, posn);
        className = cn;
    }
            
    public <A,R> R visit(Visitor<A,R> v, A o) {
        return v.visitClassType(this, o);
    }

    public Identifier className;
    
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
    	else if(t instanceof ClassType && this.className.spelling.equals(((ClassType) t).className.spelling)) {
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
