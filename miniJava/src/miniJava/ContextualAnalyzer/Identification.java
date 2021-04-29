package miniJava.ContextualAnalyzer;
import miniJava.AbstractSyntaxTrees.*;
// Since Package is a library in Java, we have to load the specific Package from AST so that Java knows which one we are using
import miniJava.AbstractSyntaxTrees.Package;
import miniJava.ErrorReporter;

public class Identification implements Visitor<Object, Object> {
	public IdentificationTable table;
	private ErrorReporter reporter;
	public ClassDecl currentClassDecl;
	private boolean inStatic; // a boolean that judges if in a static method
	public Statement currentStatement; // a variable that serves for the VarDecl initializing check
	public boolean hasMain;
	private boolean visitLHS;
	
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
		
		if(!hasMain) {
			reporter.reportError(prog.posn.toString()+ " sourcefile lacks a suitable main method");
			System.exit(4);
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
				reporter.reportError(""+md.posn.toString()+" Duplicate method: method name already exists.");
				System.exit(4);
			}
			if(!md.isPrivate && md.isStatic && md.name.equals("main") && md.type.typeKind == TypeKind.VOID) {
				if(hasMain) {
					reporter.reportError(""+md.posn.toString()+" One program can only have one unique public static void main method");
					System.exit(4);
				}
				else {
					if(md.parameterDeclList != null && md.parameterDeclList.size() == 1 && md.parameterDeclList.get(0).type instanceof ArrayType
							&& ((ArrayType) md.parameterDeclList.get(0).type).eltType instanceof ClassType 
							&& ((ClassType) ((ArrayType) md.parameterDeclList.get(0).type).eltType).className.spelling.equals("String")) {
						this.hasMain = true;
					}
					else {
						reporter.reportError(""+md.posn.toString()+" Incorrect format of public static void main method");
						System.exit(4);
					}
				}
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
		inStatic = md.isStatic;
		md.type.visit(this, null);
		// level 3
		table.openScope();
		for(ParameterDecl pd: md.parameterDeclList) {
			pd.visit(this, null);
		}
		
		// level 4
		table.openScope();
		for(Statement st: md.statementList) {
			this.currentStatement = st;
			st.visit(this, null);
		}
		table.closeScope();
		table.closeScope();
		return null;
	}

	@Override
	public Object visitParameterDecl(ParameterDecl pd, Object arg) {
		// TODO Auto-generated method stub
		pd.type.visit(this, null);
		if(table.nameExistsLocalScope(pd.name)) {
			reporter.reportError(""+pd.posn.toString()+" Variable name already exists in this local scope.");
			System.exit(4);
		}
		table.enter(pd);	
		return null;
	}

	@Override
	public Object visitVarDecl(VarDecl decl, Object arg) {
		decl.type.visit(this, null);
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
		
		/*type.className.visit(this, null);
		if(type.className.decl == null || !(type.className.decl instanceof ClassDecl)) {
			reporter.reportError(type.posn + " Expecting an already declared class.");
			System.exit(4);
		}
		
		return null;*/
		ClassDecl classDecl = (ClassDecl) this.table.findClass(type.className.spelling);
        if (classDecl == null) {
            reporter.reportError(type.posn.toString() + ": Expected a class name");
        } else {
            type.className.decl = classDecl;
        }
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
			this.currentStatement = s;
			s.visit(this, null);
		}
		table.closeScope();
		return null;
	}

	@Override
	public Object visitVardeclStmt(VarDeclStmt stmt, Object arg) {
		// TODO Auto-generated method stub
		stmt.varDecl.visit(this, null);
		stmt.initExp.visit(this, null);
		if(stmt.initExp instanceof RefExpr && ((RefExpr)stmt.initExp).ref instanceof ThisRef) {
			stmt.varDecl.thisRef = true;
		}
		
		
		return null;
	}

	@Override
	public Object visitAssignStmt(AssignStmt stmt, Object arg) {
		// TODO Auto-generated method stub
		this.visitLHS = true;
		stmt.ref.visit(this, null);
		this.visitLHS = false;
		stmt.val.visit(this, null);
		return null;
	}

	@Override
	public Object visitIxAssignStmt(IxAssignStmt stmt, Object arg) {
		// TODO Auto-generated method stub
		this.visitLHS = true;
		stmt.ref.visit(this, null);
		stmt.ix.visit(this, null);
		this.visitLHS = false;
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
		if(stmt.thenStmt instanceof VarDeclStmt) {
			reporter.reportError(stmt.thenStmt.posn+" A variable declaration cannot be the solitary statement in a branch of a conditional statement.");
			System.exit(4);
		}
		table.openScope();
		stmt.thenStmt.visit(this, null);
		table.closeScope();
		// elsestmt
		if(stmt.elseStmt != null) {
			if(stmt.elseStmt instanceof VarDeclStmt) {
				reporter.reportError(stmt.elseStmt.posn+" A variable declaration cannot be the solitary statement in a branch of a conditional statement.");
				System.exit(4);
			}
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
		if(stmt.body instanceof VarDeclStmt) {
			reporter.reportError(stmt.body.posn+" A variable declaration cannot be the solitary statement in a branch of a conditional statement.");
			System.exit(4);
		}
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
		if(expr.ref instanceof IdRef && (expr.ref.decl instanceof ClassDecl || expr.ref.decl instanceof MethodDecl)) {
			reporter.reportError(expr.posn.toString()+" Can't reference a class or method only(in the form of IdRef) in RefExpr");
			System.exit(4);
		}
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
		if(inStatic) {
			reporter.reportError(ref.posn+" Cannot reference \"this\" within a static context");
			System.exit(4);
		}
		ref.decl = currentClassDecl;
		
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
		if(temp instanceof MemberDecl) {
			if(((MemberDecl) temp).isStatic != inStatic) {
				reporter.reportError(""+ref.id.posn.toString() + "Cannot reference non-static ref " + ref.id.spelling +" in static context");
				System.exit(4);
			}
		}
		if(temp instanceof VarDecl && this.currentStatement instanceof VarDeclStmt && temp == ((VarDeclStmt) this.currentStatement).varDecl) {
			reporter.reportError("" +ref.id.posn.toString() + "Cannot reference " + ref.id.spelling +" within the initializing expression of the declaration for " + ref.id.spelling);
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
		
		if(subref.decl == null) {
			reporter.reportError("" + subref.posn.toString() + "Can't find the corresponding field or field not declared");
			System.exit(4);
		}
		
		// Can't have method in the middle of the QualRef
		if(subref.decl instanceof MethodDecl) {
			reporter.reportError("" + subref.posn.toString() + "Can't have method" + subref.id.spelling + "in the middle of the QualRef");
			System.exit(4);
		}
		
		
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
		// Edge case 2: ArrayRef
		else if(subref.decl.type instanceof ArrayType && subid.spelling.equals("length")){
			// array.length can't show up at the LHS of assignstmt
			if(this.visitLHS) {
				reporter.reportError("" + subid.posn.toString() + " Array.length can't show up at the LHS of assignstmt");
			}
			subid.decl = new FieldDecl(false, true, new BaseType(TypeKind.INT, null), "length", null);
			ref.decl = new FieldDecl(false, true, new BaseType(TypeKind.INT, null), "length", null);
			return null;
		}
		else {
			// If it's a class, then find its class name directly
			if(subref.id.decl instanceof ClassDecl) {
				name = subref.id.decl.name;
			}
			// If its' an instance of a class, find its class name via ClassType.className
			else if(subref.id.decl instanceof VarDecl || subref.id.decl instanceof FieldDecl || subref.id.decl instanceof ParameterDecl ) {
				// Case 1: subref is a ClassType
				if(subref.id.decl.type instanceof ClassType) {
					ClassType c = (ClassType) subref.id.decl.type;
					if(c == null) {
						reporter.reportError("" + subref.id.posn.toString() + "Error casting to type(ClassType) or not found");
						System.exit(4);
					}
					name = c.className.spelling;
				}
				// Case 2: subref is other type. eg: BaseType "Int"
				else {
					reporter.reportError("" + subref.id.posn.toString() + "Reference other than ClassType can't be qualified");
					System.exit(4);
				}
				
			}
			// edge case: not a class nor an instance of the class
			else {
				reporter.reportError("" + subref.id.posn.toString() + "Error finding the classname of the variable");
				System.exit(4);
			}
		}
		
		
		Declaration temp = table.retrieve(name);
		if(temp == null) {
			reporter.reportError(""+subref.posn.toString()+" " + "Class or instance is not declared or found");
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
		
		// ref.decl is supposed to be a field declaration
		ref.decl = cd.memberTable.get(subid.spelling);
		
		// Visibility check
		// first cast to md to access isStatic
		MemberDecl md = (MemberDecl) ref.decl;
		if(md == null) {
			reporter.reportError("" + ref.decl.posn.toString() + "Error casting to type(MemberDecl)");
			System.exit(4);
		}
		// Visibility Check
		// Private is only allowed to be accessed if the private member is a member of the currentClass
		if(md.isPrivate) {
			// Case 1: ThisRef
			if(subref instanceof ThisRef && subref.decl == this.currentClassDecl) {
				// All good, nothing happens
			}
			// Case 2: a class itself
			else if(subref.id.decl instanceof ClassDecl && subref.id.decl == this.currentClassDecl) {
				if(subref.id.decl == this.currentClassDecl || subref.id.decl.name.equals(this.currentClassDecl.name)) {
					// All good, nothing happens
				}
				else {
					reporter.reportError("" + ref.decl.posn.toString() +"Error: member variable is private");
					System.exit(4);
				}
				
			}
			else if(subref.id.decl instanceof Declaration) {
				// Case 3: a parameter, a variable declared, or a field of type currentClass
				if(subref.id.decl.type != null && ((ClassType) subref.id.decl.type).className.spelling.equals(this.currentClassDecl.name)) {
					// All good, nothing happens
				}
				else {
					reporter.reportError("" + ref.decl.posn.toString() +"Error: member variable is private");
					System.exit(4);
				}
			}
			else {
				reporter.reportError("" + ref.decl.posn.toString() +"Error: member variable is private");
				System.exit(4);
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
		if(temp instanceof MemberDecl) {
			if(((MemberDecl) temp).isStatic != inStatic) {
				reporter.reportError("" +id.posn.toString() + "Cannot reference non-static id " + id.spelling +" in static context");
				System.exit(4);
			}
		}
		if(temp instanceof VarDecl && this.currentStatement instanceof VarDeclStmt && temp == ((VarDeclStmt) this.currentStatement).varDecl) {
			reporter.reportError("" +id.posn.toString() + "Cannot reference " + id.spelling +" within the initializing expression of the declaration for " + id.spelling);
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
