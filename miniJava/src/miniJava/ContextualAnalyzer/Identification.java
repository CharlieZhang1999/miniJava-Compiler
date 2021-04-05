package miniJava.ContextualAnalyzer;
import miniJava.AbstractSyntaxTrees.*;
// Since Package is a library in Java, we have to load the specific Package from AST so that Java knows which one we are using
import miniJava.AbstractSyntaxTrees.Package;
import miniJava.ErrorReporter;

public class Identification implements Visitor<Object, Object> {
	public IdentificationTable table;
	private ErrorReporter reporter;
	public ClassDecl currentClassDecl;
	
	public Identification(Package ast, ErrorReporter reporter) {
		this.reporter = reporter;
		this.table = new IdentificationTable(reporter);
		ast.visit(this, null);
	}
	
	// Package
	public Object visitPackage(Package prog, Object obj) {
		table.openScope();
		// add all the classes to the table.
		for(ClassDecl cd: prog.classDeclList) {
			if(table.nameExistsCurrentScope(cd.name)) {
				reporter.reportError(""+cd.posn.toString()+" Class name already exists.");
				System.exit(4);
			}
			table.enter(cd);
			addMembers(cd);
		}
		//then visit classes
		for(ClassDecl cd: prog.classDeclList) {
			currentClassDecl = cd;
			cd.visit(this, null);
		}
		table.closeScope();
		return null;
	}
	public void addMembers(ClassDecl cd) {
		for(FieldDecl fd: cd.fieldDeclList) {
			cd.memberTable.put(fd.name, fd);
		}
		for(MethodDecl md: cd.methodDeclList) {
			cd.memberTable.put(md.name, md);
		}
		
	}
	
	// ClassDeclarations
	public Object visitClassDecl(ClassDecl cd, Object obj) {
		// currentClass = cd;
		
		// add members so all fields and members are visible
		table.openScope();
		for(FieldDecl fd: cd.fieldDeclList) {
			if(table.nameExistsCurrentScope(fd.name)) {
				reporter.reportError(""+fd.posn.toString()+" Field name already exists.");
				System.exit(4);
			}
			table.enter(fd);
			//cd.memberTable.put(fd.name, fd);
		}
		for(MethodDecl md: cd.methodDeclList) {
			if(table.nameExistsCurrentScope(md.name)) {
				reporter.reportError(""+md.posn.toString()+" Method name already exists.");
				System.exit(4);
			}
			table.enter(md);
			//cd.memberTable.put(md.name, md);
		}
		// visit all members
		for(FieldDecl fd: cd.fieldDeclList) {
			fd.visit(this, null);
		}
		
		for(MethodDecl md: cd.methodDeclList) {
			md.visit(this, null);
		}
		
		table.closeScope();
		return null;
	}

	@Override
	public Object visitFieldDecl(FieldDecl fd, Object arg) {
		// TODO Auto-generated method stub
		fd.type.visit(this, null);
		return null;
	}

	@Override
	public Object visitMethodDecl(MethodDecl md, Object arg) {
		// TODO Auto-generated method stub
		md.type.visit(this, null);
		// level 3
		table.openScope();
		for(ParameterDecl pd: md.parameterDeclList) {
			pd.visit(this, null);
		}
		
		// level 4
		table.openScope();
		for(Statement st: md.statementList) {
			st.visit(this, null);
		}
		table.closeScope();
		table.closeScope();
		return null;
	}

	@Override
	public Object visitParameterDecl(ParameterDecl pd, Object arg) {
		// TODO Auto-generated method stub
		if(table.nameExistsLocalScope(pd.name)) {
			reporter.reportError(""+pd.posn.toString()+" Variable name already exists in this local scope.");
			System.exit(4);
		}
		table.enter(pd);	
		return null;
	}

	@Override
	public Object visitVarDecl(VarDecl decl, Object arg) {
		// TODO Auto-generated method stub
		if(table.nameExistsLocalScope(decl.name)) {
			reporter.reportError(""+decl.posn.toString()+" Variable name already exists in this local scope.");
			System.exit(4);
		}
		table.enter(decl);
		return null;
	}

	@Override
	public Object visitBaseType(BaseType type, Object arg) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visitClassType(ClassType type, Object arg) {
		// TODO Auto-generated method stub
		
		type.className.visit(this, null);
		return null;
	}

	@Override
	public Object visitArrayType(ArrayType type, Object arg) {
		// TODO Auto-generated method stub
		type.eltType.visit(this, null);
		return null;
	}

	@Override
	public Object visitBlockStmt(BlockStmt stmt, Object arg) {
		// TODO Auto-generated method stub
		
		// level 4+
		table.openScope();
		for(Statement s: stmt.sl) {
			s.visit(this, null);
		}
		table.closeScope();
		return null;
	}

	@Override
	public Object visitVardeclStmt(VarDeclStmt stmt, Object arg) {
		// TODO Auto-generated method stub
		stmt.initExp.visit(this, null);
		if(stmt.initExp instanceof RefExpr && ((RefExpr)stmt.initExp).ref instanceof ThisRef) {
			stmt.varDecl.thisRef = true;
		}
		stmt.varDecl.visit(this, null);

		
		return null;
	}

	@Override
	public Object visitAssignStmt(AssignStmt stmt, Object arg) {
		// TODO Auto-generated method stub
		stmt.ref.visit(this, null);
		stmt.val.visit(this, null);
		return null;
	}

	@Override
	public Object visitIxAssignStmt(IxAssignStmt stmt, Object arg) {
		// TODO Auto-generated method stub
		stmt.ref.visit(this, null);
		stmt.ix.visit(this, null);
		stmt.exp.visit(this, null);
		return null;
	}

	@Override
	public Object visitCallStmt(CallStmt stmt, Object arg) {
		// TODO Auto-generated method stub
		stmt.methodRef.visit(this, null);
		
		for(Expression e: stmt.argList) {
			e.visit(this, null);
		}
		return null;
	}

	@Override
	public Object visitReturnStmt(ReturnStmt stmt, Object arg) {
		// TODO Auto-generated method stub
		if(stmt.returnExpr != null) {
			stmt.returnExpr.visit(this, null);
		}
		return null;
	}

	@Override
	public Object visitIfStmt(IfStmt stmt, Object arg) {
		// TODO Auto-generated method stub
		stmt.cond.visit(this, null);
		// thenstmt
		table.openScope();
		stmt.thenStmt.visit(this, null);
		table.closeScope();
		// elsestmt
		if(stmt.elseStmt != null) {
			table.openScope();
			stmt.elseStmt.visit(this, null);
			table.closeScope();
		}
		
		return null;
	}

	@Override
	public Object visitWhileStmt(WhileStmt stmt, Object arg) {
		// TODO Auto-generated method stub
		stmt.cond.visit(this, null);
		table.openScope();
		stmt.body.visit(this, null);
		table.closeScope();
		return null;
	}

	@Override
	public Object visitUnaryExpr(UnaryExpr expr, Object arg) {
		// TODO Auto-generated method stub
		expr.operator.visit(this, null);
		expr.expr.visit(this, null);
		return null;
	}

	@Override
	public Object visitBinaryExpr(BinaryExpr expr, Object arg) {
		// TODO Auto-generated method stub
		expr.operator.visit(this, null);
		expr.left.visit(this, null);
		expr.right.visit(this, null);
		return null;
	}

	@Override
	public Object visitRefExpr(RefExpr expr, Object arg) {
		// TODO Auto-generated method stub
		expr.ref.visit(this, null);
		return null;
	}

	@Override
	public Object visitIxExpr(IxExpr expr, Object arg) {
		// TODO Auto-generated method stub
		expr.ref.visit(this, null);
		expr.ixExpr.visit(this, null);
		return null;
	}

	@Override
	public Object visitCallExpr(CallExpr expr, Object arg) {
		// TODO Auto-generated method stub
		expr.functionRef.visit(this, null);
		for(Expression e: expr.argList) {
			e.visit(this, null);
		}
		return null;
	}

	@Override
	public Object visitLiteralExpr(LiteralExpr expr, Object arg) {
		// TODO Auto-generated method stub
		expr.lit.visit(this, null);
		return null;
	}

	@Override
	public Object visitNewObjectExpr(NewObjectExpr expr, Object arg) {
		// TODO Auto-generated method stub
		expr.classtype.visit(this, null);
		return null;
	}

	@Override
	public Object visitNewArrayExpr(NewArrayExpr expr, Object arg) {
		// TODO Auto-generated method stub
		expr.eltType.visit(this, null);
		expr.sizeExpr.visit(this, null);
		return null;
	}

	@Override
	public Object visitThisRef(ThisRef ref, Object arg) {
		// TODO Auto-generated method stub
		ref.decl = currentClassDecl;
		// ref.id.decl = currentClassDecl;
		
		return null;
	}

	@Override
	public Object visitIdRef(IdRef ref, Object arg) {
		// TODO Auto-generated method stub
		ref.id.visit(this, null);
		Declaration temp = table.retrieve(ref.id.spelling);
		if(temp == null) {
			reporter.reportError(""+ref.id.posn.toString()+" " + ref.id.spelling + " is not declared or found");
			System.exit(4);
		}
		ref.decl = table.retrieve(ref.id.spelling);
		return null;
	}

	@Override
	public Object visitQRef(QualRef ref, Object arg) {
		// TODO Auto-generated method stub
		Reference subref = ref.ref;
		Identifier subid = ref.id;
		subref.visit(this, null);
		
		// debug
		// case: a.b.c (where a is an object)
		// first get current identifier(eg: a)'s classname and then retrieve the class declaration from the table
		// then go to that class declaration to look for the next identifier(eg: b)
		String name = null;
		
		// If subref itself is a ThisRef
		if(subref.id == null) {
			if(subref instanceof ThisRef) {
				// subref.decl will be ClassDecl
				name = subref.decl.name;
			}
			else {
				reporter.reportError("" + subref.posn.toString() + "Can't identify subref's id ");
				System.exit(4);
			}
		}
		else {
			// If it's a class, then find its class name directly
			if(subref.id.decl instanceof ClassDecl) {
				name = subref.id.decl.name;
			}
			// If its' an instance of a class, find its class name via ClassType.className
			else if(subref.id.decl instanceof VarDecl) {
				ClassType c = (ClassType) subref.id.decl.type;
				if(c == null) {
					reporter.reportError("" + subref.id.posn.toString() + "Error casting to type(ClassType) or not found");
					System.exit(4);
				}
				name = c.className.spelling;
			}
			// edge case: not a class nor an instance of the class
			else {
				reporter.reportError("" + subref.id.posn.toString() + "Error finding the classname of the variable");
				System.exit(4);
			}
		}
		
		
		Declaration temp = table.retrieve(name);
		if(temp == null) {
			reporter.reportError(""+subref.posn.toString()+" " + "class or instance is not declared or found");
			System.exit(4);
		}
		ClassDecl cd = (ClassDecl) table.retrieve(name);
		if(cd == null) {
			reporter.reportError("" + subref.posn.toString() + "Error casting to type(ClassDecl) or not found");
			System.exit(4);
		}
		
		// check if the class table has a member "id.spelling"
		if(!cd.memberTable.containsKey(subid.spelling)){
			reporter.reportError("" + subid.posn.toString()+"Declaration of member is not found");
			System.exit(4);
		}
		
		// ref.decl is supposed to be a member declaration
		ref.decl = cd.memberTable.get(subid.spelling);
		/*if(ref.decl.type == null) {
			System.out.println("error");
		}*/
		
		// Visibility check
		// first cast to md to access isStatic
		MemberDecl md = (MemberDecl) ref.decl;
		if(md == null) {
			reporter.reportError("" + ref.decl.posn.toString() + "Error casting to type(MemberDecl)");
			System.exit(4);
		}
		// Visibility should be checked except for this reference
		if(!(subref instanceof ThisRef)) {
			if(!(subref.decl instanceof VarDecl && ((VarDecl) subref.decl).thisRef)) {
				if(md.isPrivate) {
					reporter.reportError("" + ref.decl.posn.toString() +"Error: member variable is private");
					System.exit(4);
				}
			}
		}
		
		// Static check
		if(!(subref instanceof ThisRef) && subref.decl instanceof ClassDecl) {
			if(!(subref.decl instanceof VarDecl && ((VarDecl) subref.decl).thisRef)) {
				if(!md.isStatic) {
					reporter.reportError("" + ref.decl.posn.toString() +"Error: try to access a member using class but member variable is not static");
					System.exit(4);
				}
			}
		}
		
		// subid.decl assignment
		TypeDenoter t = ref.decl.type;
		if(t == null) {
			reporter.reportError("" + ref.decl.posn.toString() +"Error: type is null");
			System.exit(4);
		}
		// ClassType
		if(t instanceof ClassType) {
			ClassType ct = (ClassType) t;
			Declaration d = table.retrieve(ct.className.spelling);
			if(d == null) {
				reporter.reportError(""+ct.posn.toString()+" " + ct.className.spelling + " is not declared or found");
				System.exit(4);
			}
			subid.decl = (ClassDecl) table.retrieve(ct.className.spelling);
		}
		else {
			// BaseType
			subid.decl = cd.memberTable.get(ref.decl.name);
			//subid.decl = cd.memberTable.get()
		}	
		return null;
	}

	@Override
	public Object visitIdentifier(Identifier id, Object arg) {
		// TODO Auto-generated method stub
		Declaration temp = table.retrieve(id.spelling);
		if(temp == null) {
			reporter.reportError(""+id.posn.toString()+" " + id.spelling + " is not declared or found");
			System.exit(4);
		}
		id.decl = table.retrieve(id.spelling);
		return null;
	}

	@Override
	public Object visitOperator(Operator op, Object arg) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visitIntLiteral(IntLiteral num, Object arg) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visitBooleanLiteral(BooleanLiteral bool, Object arg) {
		// TODO Auto-generated method stub
		return null;
	}
	
	public IdentificationTable getTable() {
		return table;
	}

	@Override
	public Object visitNullLiteral(NullLiteral nullLiteral, Object arg) {
		// TODO Auto-generated method stub
		return null;
	}
}
