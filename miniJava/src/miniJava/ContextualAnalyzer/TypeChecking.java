package miniJava.ContextualAnalyzer;
import miniJava.ErrorReporter;
import miniJava.AbstractSyntaxTrees.*;
import miniJava.AbstractSyntaxTrees.Package;
import miniJava.SyntacticAnalyzer.SourcePosition;

public class TypeChecking implements Visitor<Object, TypeDenoter>  {
	private ErrorReporter reporter;
	private AST ast;
	public static IdentificationTable table;
	
	public TypeChecking(Package ast, ErrorReporter reporter, IdentificationTable Idtable) {
		this.reporter = reporter;
		this.ast = ast;
		this.table = Idtable;
		ast.visit(this, null);
	}
	@Override
	public TypeDenoter visitPackage(Package prog, Object arg) {
		// TODO Auto-generated method stub
		for(ClassDecl cd: prog.classDeclList) {
			cd.visit(this, null);
		}
		return null;
	}

	@Override
	public TypeDenoter visitClassDecl(ClassDecl cd, Object arg) {
		// TODO Auto-generated method stub
		for(FieldDecl fd: cd.fieldDeclList) {
			fd.visit(this, null);
		}
		
		for(MethodDecl md: cd.methodDeclList) {
			md.visit(this, null);
		}
		return null;
	}

	@Override
	public TypeDenoter visitFieldDecl(FieldDecl fd, Object arg) {
		// TODO Auto-generated method stub
		if(fd.type == null || fd.type.typeKind == null) {
			reporter.reportError(""+fd.posn+" Can't find the type or typeKind of the field");
			return null;
		}
		fd.type = fd.type.visit(this, null);
		
		if(fd.type.typeKind == TypeKind.VOID) {
			reporter.reportError("Field is of type VOID");
		}
		return null;
	}

	@Override
	public TypeDenoter visitMethodDecl(MethodDecl md, Object arg) {
		// boolean hasReturned = false;
		// TODO Auto-generated method stub
		for(ParameterDecl pd: md.parameterDeclList) {
			pd.visit(this, null);
		}
		for(Statement s: md.statementList) {
			s.visit(this, null);
			// A return statement
			if(s.returnType != null) {
				if(!md.type.equals(s.returnType)) {
					reporter.reportError(" "+ md.posn.toString()+ "the true return type doesn't match the type of the method");
				}
				return null;
			}
		}
		
		// no return statement
		if(md.type.typeKind != TypeKind.VOID) {
			reporter.reportError(""+ md.posn.toString() + "method is not void but there is no return statement");
		}
		return null;
	}

	@Override
	public TypeDenoter visitParameterDecl(ParameterDecl pd, Object arg) {
		// TODO Auto-generated method stub
		if(pd.type == null || pd.type.typeKind == null) {
			reporter.reportError(""+pd.posn+" Can't find the type or typeKind of the paramter");
			return null;
		}
		if (pd.type.typeKind == TypeKind.VOID) {
            reporter.reportError(""+ pd.posn.toString()+ "variable can't be of type VOID");
        }
		pd.type = pd.type.visit(this, null);
		return pd.type;
	}

	@Override
	public TypeDenoter visitVarDecl(VarDecl decl, Object arg) {
		// TODO Auto-generated method stub
		
		decl.type = decl.type.visit(this, null);
		return decl.type;
	}

	@Override
	public TypeDenoter visitBaseType(BaseType type, Object arg) {
		// TODO Auto-generated method stub
		return type;
	}

	@Override
	public TypeDenoter visitClassType(ClassType type, Object arg) {
		// TODO Auto-generated method stub
		// if(type.id.spelling == "String") {
		if(type.className.spelling.equals("String")) {
			TypeDenoter new_type = new BaseType(TypeKind.UNSUPPORTED, type.posn);
			return new_type;
		}
		return type;
	}

	@Override
	public TypeDenoter visitArrayType(ArrayType type, Object arg) {
		// TODO Auto-generated method stub
		TypeDenoter t = type.eltType.visit(this, arg);
        return new ArrayType(t, type.posn);
	}

	@Override
	public TypeDenoter visitBlockStmt(BlockStmt stmt, Object arg) {
		// TODO Auto-generated method stub
		for(Statement s: stmt.sl) {
			s.visit(this, null);
			if(s.returnType != null) {
				stmt.returnType = s.returnType;
				return null;
			}
		}
		return null;
	}

	@Override
	public TypeDenoter visitVardeclStmt(VarDeclStmt stmt, Object arg) {
		// TODO Auto-generated method stub
		TypeDenoter t1 = stmt.varDecl.visit(this, null);
		TypeDenoter t2 = stmt.initExp.visit(this, null);
		if(!t1.equals(t2)) {
			reporter.reportError(""+ stmt.posn.toString()+ "Found ERROR type or UNSUPPORTED type or types on both sides are incompatible");
		}
		return null;
	}

	@Override
	public TypeDenoter visitAssignStmt(AssignStmt stmt, Object arg) {
		// TODO Auto-generated method stub
		TypeDenoter t1 = stmt.ref.visit(this, null);
		TypeDenoter t2 = stmt.val.visit(this, null);
		if(!t1.equals(t2)) {
			reporter.reportError(""+ stmt.posn.toString()+ "Found ERROR type or UNSUPPORTED type or types on both sides are incompatible");
		}
		return null;
	}

	@Override
	public TypeDenoter visitIxAssignStmt(IxAssignStmt stmt, Object arg) {
		// TODO Auto-generated method stub
		TypeDenoter t1 = stmt.ref.visit(this, null);
		TypeDenoter t2 = stmt.exp.visit(this, null);
		TypeDenoter ixt = stmt.ix.visit(this, null);
		if(ixt.typeKind != TypeKind.INT) {
			reporter.reportError(stmt.exp.posn.toString() +"indexExpr is not of INT type ");
			
		}
		if(!t1.equals(t2)) {
			reporter.reportError(""+ stmt.posn.toString()+ "Found ERROR type or UNSUPPORTED type or types on both sides are incompatible");
		}
		return null;
	}

	@Override
	public TypeDenoter visitCallStmt(CallStmt stmt, Object arg) {
		// TODO Auto-generated method stub
		/*ClassType ct = (ClassType) stmt.methodRef.id.decl.type;
		if(ct == null) {
			reporter.reportError(""+stmt.posn+" : "+"The class associated with the method is not found");
		}
		
		String classname = ct.className.spelling;
		ClassDecl cd = (ClassDecl) table.retrieve(classname);
		if(cd == null) {
			reporter.reportError(""+stmt.posn+" : "+"The class associated with the method is not found");
		}
		
		MethodDecl md = (MethodDecl) cd.memberTable.get(stmt.methodRef.id.spelling);*/
		if(!(stmt.methodRef.decl instanceof MethodDecl)) {
			reporter.reportError("" + stmt.methodRef.posn.toString() + "Error -- not a method or uncallable");
			return null;
		}
		MethodDecl md = (MethodDecl) stmt.methodRef.decl;
		int param_length = md.parameterDeclList.size();
		int arg_length = stmt.argList.size();
		if(param_length != arg_length) {
			reporter.reportError(""+ stmt.posn.toString()+ "Error: number are incompatible with the number of paramters that should be in the method");
		}
		
		for(int i = 0; i < arg_length; i++) {
			TypeDenoter t1 = stmt.argList.get(i).visit(this, null);
			TypeDenoter t2 = md.parameterDeclList.get(i).type.visit(this, null);
			if(!t1.equals(t2)) {
				reporter.reportError(""+ stmt.argList.get(i).posn.toString() + "Found ERROR type or UNSUPPORTED type or types of arguments are incompatible");
			}
		}
		
		
		return null;
	}

	@Override
	public TypeDenoter visitReturnStmt(ReturnStmt stmt, Object arg) {
		// TODO Auto-generated method stub
		TypeDenoter t1;
        if (stmt.returnExpr == null) {
            t1 = new BaseType(TypeKind.VOID, stmt.posn);
        } else {
            t1 = stmt.returnExpr.visit(this, arg);
        }
		stmt.returnType = t1;
		return null;
	}

	@Override
	public TypeDenoter visitIfStmt(IfStmt stmt, Object arg) {
		// TODO Auto-generated method stub
		TypeDenoter conditionType;
        if (stmt.cond != null) {
        	conditionType = stmt.cond.visit(this, arg);
            if (conditionType.typeKind != TypeKind.BOOLEAN) {
                reporter.reportError("" + stmt.cond.posn.toString() + "Type Checking Error: Condition is not of Boolean type");
            }
        }
        
        stmt.thenStmt.visit(this, null);
        if(stmt.elseStmt != null) {
        	stmt.elseStmt.visit(this, null);
        }
		return null;
	}

	@Override
	public TypeDenoter visitWhileStmt(WhileStmt stmt, Object arg) {
		// TODO Auto-generated method stub
		TypeDenoter conditionType;
        if (stmt.cond != null) {
        	conditionType = stmt.cond.visit(this, arg);
            if (conditionType.typeKind != TypeKind.BOOLEAN) {
                reporter.reportError("" + stmt.cond.posn.toString() + "Type Checking Error: Condition is not of Boolean type");
            }
        }
        
        stmt.body.visit(this, null);
		return null;
	}

	@Override
	public TypeDenoter visitUnaryExpr(UnaryExpr expr, Object arg) {
		// TODO Auto-generated method stub
		String op = expr.operator.spelling;
		TypeDenoter t1 = expr.expr.visit(this, null);
		if(op.equals("!")) {
			if(t1.typeKind != TypeKind.BOOLEAN) {
				reporter.reportError("" + expr.posn.toString() + "Type Checking Error: expecting Boolean type expression after unary symbol !");
				return new BaseType(TypeKind.ERROR, new SourcePosition());
			}
			else return new BaseType(TypeKind.BOOLEAN, new SourcePosition());
		}
		else if(op.equals("-")) {
			if(t1.typeKind != TypeKind.INT) {
				reporter.reportError("" + expr.posn.toString() + "Type Checking Error: expecting INT type expression after unary symbol -");
				return new BaseType(TypeKind.ERROR, new SourcePosition());
			}
			else return new BaseType(TypeKind.INT, new SourcePosition());
		}
		else {
			reporter.reportError("" + expr.posn.toString() + "Type Checking Error: invalid unary symbol found");
			return new BaseType(TypeKind.ERROR, new SourcePosition());
		}
	}

	@Override
	public TypeDenoter visitBinaryExpr(BinaryExpr expr, Object arg) {
		// TODO Auto-generated method stub
		String op = expr.operator.spelling;
		TypeDenoter left = expr.left.visit(this, null);
		TypeDenoter right = expr.right.visit(this, null);
		if(op.equals("||") || op.equals("&&")) {
			if(left.typeKind != TypeKind.BOOLEAN || right.typeKind != TypeKind.BOOLEAN || !left.equals(right)) {
				reporter.reportError("" + expr.posn.toString() + "Type Checking Error: expecting Boolean type expression on the two sides of || or &&");
				return new BaseType(TypeKind.ERROR, new SourcePosition());
			}
			else {
				return new BaseType(TypeKind.BOOLEAN, new SourcePosition());
			}
		}
		else if(op.equals("==") || op.contentEquals("!=")) {
			if(left.equals(right)) {
				return new BaseType(TypeKind.BOOLEAN, new SourcePosition());
			}
			else {
				reporter.reportError("" + expr.posn.toString() + "Type Checking Error: expecting same types of expression on the two sides of == or !=");
				return new BaseType(TypeKind.ERROR, new SourcePosition());
			}
		}
		else if(op.equals("<=") || op.equals(">=") || op.equals(">") || op.equals("<")) {
			if(left.typeKind == TypeKind.INT && right.typeKind == TypeKind.INT) {
				return new BaseType(TypeKind.BOOLEAN, new SourcePosition());
			}
			else {
				reporter.reportError("" + expr.posn.toString() + "Type Checking Error: expecting two Integer types of expression on the two sides of <= or >= or > or <");
				return new BaseType(TypeKind.ERROR, new SourcePosition());
			}
		}
		else if (op.equals("+") || op.equals("-") || op.equals("*") || op.equals("/")) {
			if(left.typeKind == TypeKind.INT && right.typeKind == TypeKind.INT) {
				return new BaseType(TypeKind.INT, new SourcePosition());
			}
			else {
				reporter.reportError("" + expr.posn.toString() + "Type Checking Error: expecting two Integer types of expression on the two sides of <= or >= or > or <");
				return new BaseType(TypeKind.ERROR, new SourcePosition());
			}
		}
		else {
			reporter.reportError("" + expr.posn.toString() + "Type Checking Error: binary operator not found");
			return new BaseType(TypeKind.ERROR, new SourcePosition());
		}
	}

	@Override
	public TypeDenoter visitRefExpr(RefExpr expr, Object arg) {
		// TODO Auto-generated method stub
		if (expr.ref.decl instanceof MethodDecl) {
            reporter.reportError("" + expr.posn.toString() +"Cannot use method name as an expression");
        }
        return expr.ref.visit(this, arg);
	}

	@Override
	public TypeDenoter visitIxExpr(IxExpr expr, Object arg) {
		// TODO Auto-generated method stub
		TypeDenoter t1 = expr.ref.id.decl.type;
		TypeDenoter ix1 = expr.ixExpr.visit(this, null);
		if(t1.typeKind != TypeKind.ARRAY) {
			reporter.reportError("" + expr.posn.toString() + "Ref is not of ARRAY type ");
			return new BaseType(TypeKind.ERROR, new SourcePosition());
		}
		if(ix1.typeKind != TypeKind.INT) {
			reporter.reportError("" + expr.posn.toString() + "indexExpr is not of INT type ");
			return new BaseType(TypeKind.ERROR, new SourcePosition());
		}
		
		return ((ArrayType) expr.ref.id.decl.type).eltType;
	}

	@Override
	public TypeDenoter visitCallExpr(CallExpr expr, Object arg) {
		// TODO Auto-generated method stub
		TypeDenoter type = null;
		if (expr.functionRef.decl instanceof MethodDecl) {
            MethodDecl methodDecl = (MethodDecl) expr.functionRef.decl;
            if (expr.argList.size() != methodDecl.parameterDeclList.size()) {
                reporter.reportError("" + expr.posn.toString()+"Method wrong number of arugments");
                return new BaseType(TypeKind.ERROR, new SourcePosition());
            } 
            else {
            	for (int i = 0; i < expr.argList.size(); i++) {
                    TypeDenoter t1 = expr.argList.get(i).visit(this, arg);
                    TypeDenoter t2 = methodDecl.parameterDeclList.get(i).visit(this, arg);
                    if(!t1.equals(t2)) {
                    	reporter.reportError("" + expr.posn + "Arguments are incompatible with the method declaration");
                    	return new BaseType(TypeKind.ERROR, new SourcePosition());
                    }
            	}
            }
            type = methodDecl.type;
		}
		else {
			reporter.reportError("" + expr.posn.toString()+" Reference is not callable ");
			return new BaseType(TypeKind.ERROR, new SourcePosition());
		}
		return type;
	}

	@Override
	public TypeDenoter visitLiteralExpr(LiteralExpr expr, Object arg) {
		// TODO Auto-generated method stub
		return expr.lit.visit(this, null);
	}

	@Override
	public TypeDenoter visitNewObjectExpr(NewObjectExpr expr, Object arg) {
		// TODO Auto-generated method stub
		return expr.classtype.visit(this, null);
	}

	@Override
	public TypeDenoter visitNewArrayExpr(NewArrayExpr expr, Object arg) {
		// TODO Auto-generated method stub
		TypeDenoter elt = expr.eltType.visit(this, null);
		TypeDenoter size = expr.sizeExpr.visit(this, null);
		return new ArrayType(elt, elt.posn);
	}

	@Override
	public TypeDenoter visitThisRef(ThisRef ref, Object arg) {
		// TODO Auto-generated method stub
		if(ref.id == null) {
			return ref.decl.type.visit(this, null);
		}
		return ref.id.visit(this, null);
	}

	@Override
	public TypeDenoter visitIdRef(IdRef ref, Object arg) {
		// TODO Auto-generated method stub
		return ref.id.visit(this, null);
	}

	@Override
	public TypeDenoter visitQRef(QualRef ref, Object arg) {
		// TODO Auto-generated method stub
		return ref.id.visit(this, null);
	}

	@Override
	public TypeDenoter visitIdentifier(Identifier id, Object arg) {
		// TODO Auto-generated method stub
		if(id.decl == null) {
			reporter.reportError("" + id.posn.toString()+" Identifier's declaration is not detected ");
			return new BaseType(TypeKind.ERROR, new SourcePosition());
		}
		if(id.decl.type == null) {
			reporter.reportError("" + id.posn.toString()+" Identifier's declaration's type is not clear ");
			return new BaseType(TypeKind.ERROR, new SourcePosition());
		}
		else if(id.decl instanceof ClassDecl) {
			return id.decl.type;
			// return new ClassType(id, id.posn);
		}
		else if(id.decl instanceof VarDecl || id.decl instanceof FieldDecl || id.decl instanceof ParameterDecl) {
			if(id.decl.type.typeKind == TypeKind.UNSUPPORTED || id.decl.type.typeKind == TypeKind.ERROR) {
				reporter.reportError(""+ id.posn.toString()+ "Found ERROR type or UNSUPPORTED type");
				return new BaseType(TypeKind.ERROR, new SourcePosition());
			}
			return id.decl.type;
		}
		else {
			reporter.reportError("" + id.posn.toString() +"Identifier is unknown or incompatible");
			return new BaseType(TypeKind.ERROR, new SourcePosition());
		}
		
	}

	@Override
	public TypeDenoter visitOperator(Operator op, Object arg) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public TypeDenoter visitIntLiteral(IntLiteral num, Object arg) {
		// TODO Auto-generated method stub
		return new BaseType(TypeKind.INT, num.posn);
	}

	@Override
	public TypeDenoter visitBooleanLiteral(BooleanLiteral bool, Object arg) {
		// TODO Auto-generated method stub
		return new BaseType(TypeKind.BOOLEAN, bool.posn);
	}
	@Override
	public TypeDenoter visitNullLiteral(NullLiteral nullLiteral, Object arg) {
		// TODO Auto-generated method stub
		return new BaseType(TypeKind.NULL, nullLiteral.posn);
	}

	
}
