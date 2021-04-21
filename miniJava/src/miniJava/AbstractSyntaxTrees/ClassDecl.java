/**
 * miniJava Abstract Syntax Tree classes
 * @author prins
 * @version COMP 520 (v2.2)
 */
package miniJava.AbstractSyntaxTrees;

import  miniJava.SyntacticAnalyzer.SourcePosition;
import java.util.*;

public class ClassDecl extends Declaration {

  public ClassDecl(String cn, Identifier id, FieldDeclList fdl, MethodDeclList mdl, SourcePosition posn) {
	  super(cn, new ClassType(id, id.posn), posn);
	  fieldDeclList = fdl;
	  methodDeclList = mdl;
	  memberTable = new HashMap<>();
  }
  
  public <A,R> R visit(Visitor<A, R> v, A o) {
      return v.visitClassDecl(this, o);
  }
      
  public FieldDeclList fieldDeclList;
  public MethodDeclList methodDeclList;
  //
  
  public HashMap<String, Declaration> memberTable;
}
