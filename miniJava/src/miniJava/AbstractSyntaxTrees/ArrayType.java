/**
 * miniJava Abstract Syntax Tree classes
 * @author prins
 * @version COMP 520 (v2.2)
 */

package miniJava.AbstractSyntaxTrees;

import miniJava.SyntacticAnalyzer.SourcePosition;

public class ArrayType extends TypeDenoter {

	    public ArrayType(TypeDenoter eltType, SourcePosition posn){
	        super(TypeKind.ARRAY, posn);
	        this.eltType = eltType;
	    }
	        
	    public <A,R> R visit(Visitor<A,R> v, A o) {
	        return v.visitArrayType(this, o);
	    }

	    public TypeDenoter eltType;
	    
	    public boolean equals(TypeDenoter t) {
	    	if(eltType.typeKind ==  TypeKind.UNSUPPORTED || eltType.typeKind == TypeKind.ERROR) {
	    		return false;
	    	}
	    	else if(t == null) {
	    		return false;
	    	}
	    	else if(t.typeKind == TypeKind.NULL) {
	    		return true;
	    	}
	    	else if(!(t instanceof ArrayType) || ((ArrayType) t).eltType == null) {
	    		return false;
	    	}
	    	else if(this.eltType instanceof ClassType && ((ArrayType) t).eltType instanceof ClassType) {
	    		if(((ClassType) this.eltType).className.spelling.equals(((ClassType) ((ArrayType) t).eltType).className.spelling)){
	    			return true;
	    		}
	    		else return false;
	    	}
	    	else if(this.eltType.typeKind == ((ArrayType) t).eltType.typeKind) {
	    		return true;
	    	}
	    	else{
	    		return false;
	    	}
	    }
	}

