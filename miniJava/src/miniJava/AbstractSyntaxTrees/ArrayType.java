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
	    
	    // PA 4
	    int length; 
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
	    	// rhs is BaseType
	    	else if(t instanceof BaseType) {
	    		if(this.eltType instanceof BaseType && this.eltType.typeKind == t.typeKind) {
	    			return true;
	    		}
	    		else {
	    			return false;
	    		}
	    	}
	    	//rhs is ClassType
	    	else if(t instanceof ClassType) {
	    		if(this.eltType instanceof ClassType) {
	    			if(((ClassType) this.eltType).className.spelling.equals(((ClassType) t).className.spelling)){
		    			return true;
		    		}
	    			else return false;
	    		}
	    		else return false;
	    	}
	    	// rhs is ArrayType[]
	    	else if(t instanceof ArrayType) {
	    		//ArrayType[ClassType]
	    		if(this.eltType instanceof ClassType && ((ArrayType) t).eltType instanceof ClassType) {
		    		if(((ClassType) this.eltType).className.spelling.equals(((ClassType) ((ArrayType) t).eltType).className.spelling)){
		    			return true;
		    		}
		    		else return false;
	    		}
	    		//ArrayType[BaseTypeType]
	    		else if(this.eltType instanceof BaseType && ((ArrayType) t).eltType instanceof BaseType && this.eltType.typeKind == ((ArrayType) t).eltType.typeKind) {
	    			return true;
	    		}
	    		
		    	else{
		    		return false;
		    	}
	    	}
	    	else return false;
	    
	}
}

