package miniJava.CodeGenerator;
import miniJava.ErrorReporter;
import java.util.ArrayList;
import java.util.HashMap;
import miniJava.AbstractSyntaxTrees.*;
import miniJava.AbstractSyntaxTrees.Package;
import mJAM.Disassembler;
import mJAM.Interpreter;
import mJAM.Machine;
import mJAM.ObjectFile;
import mJAM.Machine.Op;
import mJAM.Machine.Prim;
import mJAM.Machine.Reg;
public class CodeGenerator implements Visitor<Object, Object>{
	public ErrorReporter reporter;
	public int static_offset;
	public int localVariableOffset;
	public int patchAddr_Call_main;
	public MethodDecl currentMethodDecl;
	public HashMap<MethodDecl, ArrayList<Integer>> map;
	public boolean visitLHS;
	public int qrefLayer;
	public Declaration printlndecl = null;
	
	public CodeGenerator(Package ast, ErrorReporter reporter){
		this.reporter = reporter;
		this.map = new HashMap<MethodDecl, ArrayList<Integer>>();
		ast.visit(this, null);
		
	}
	
	@Override
	public Object visitPackage(Package prog, Object arg) {
		// TODO Auto-generated method stub
		this.static_offset = 0;
		for(ClassDecl cd: prog.classDeclList) {
			if (cd.name.equals("_PrintStream")) {
				for (MethodDecl md: cd.methodDeclList) {
					if (md.name.equals("println")) {
						this.printlndecl = md;
					}
				}
			}
			for(FieldDecl fd: cd.fieldDeclList) {
				if(fd.isStatic) {
					Machine.emit(Machine.Op.PUSH, 1);
					fd.runtimeentity = new KnownOffset(Machine.characterSize, static_offset);
					//System.out.println(fd.name+" is stored at offset "+static_offset);
					static_offset++;
				}
			}
		}
		
		
		// visit main 
		Machine.emit(Op.LOADL,0);            // array length 0
		Machine.emit(Prim.newarr);           // empty String array argument
		patchAddr_Call_main = Machine.nextInstrAddr();  // record instr addr where main is called                                                
		Machine.emit(Op.CALL,Reg.CB,-1);     // static call main (address to be patched)
		Machine.emit(Op.HALT,0,0,0); 
		
		// 
		
		for(ClassDecl cd: prog.classDeclList) {
			// Debug
			int staticfieldOffset = 0;
			int fieldOffset = 0;
			for(int fieldCount = 0; fieldCount < cd.fieldDeclList.size(); fieldCount++) {
				
				if(!cd.fieldDeclList.get(fieldCount).isStatic) {
					KnownOffset fieldAddress = new KnownOffset(Machine.addressSize, fieldOffset);
					fieldOffset++;
					cd.fieldDeclList.get(fieldCount).visit(this, fieldAddress);
				}
				
			}
			// Debug: IDK what i'm doing rn
			cd.runtimeentity = new KnownOffset(cd.fieldDeclList.size()-staticfieldOffset, staticfieldOffset+1);
		}
		
		// visit all classses
		for (ClassDecl classDecl : prog.classDeclList) {
            classDecl.visit(this, arg);
        }
		
		// patch done here
		for(MethodDecl md: map.keySet()) {
			for(int callStmtAddress: map.get(md)) {
				Machine.patch(callStmtAddress, ((KnownOffset) md.runtimeentity).offset);
			}
		}
		
		
		// copy and paste debugger here 
		return null;
	}

	@Override
	public Object visitClassDecl(ClassDecl cd, Object arg) {
		// TODO Auto-generated method stub
		
		for(MethodDecl md: cd.methodDeclList) {
			md.visit(this, null);
		}
		return null;
	}

	@Override
	public Object visitFieldDecl(FieldDecl fd, Object arg) {
		// TODO Auto-generated method stub
		fd.runtimeentity = (RuntimeEntity) arg;
		return null;
	}

	@Override
	public Object visitMethodDecl(MethodDecl md, Object arg) {
		// TODO Auto-generated method stub
		md.runtimeentity = new KnownOffset(Machine.addressSize, Machine.nextInstrAddr());
		this.currentMethodDecl = md;
		// edge case: main method
		if(md.name.equals("main")) {
			Machine.patch(patchAddr_Call_main, Machine.nextInstrAddr());
		}
		
		// initialize paramOffset
		int paramOffset = -1 * md.parameterDeclList.size();
		for(ParameterDecl pd: md.parameterDeclList) {
			KnownOffset paramAddress = new KnownOffset(Machine.addressSize, paramOffset);
			pd.visit(this, paramAddress);
			paramOffset++;
		}
		
		// reinitialize loalVariableOffset
		this.localVariableOffset = 3;
		for(Statement s: md.statementList) {
			s.visit(this, null);
		}
		
		if(md.type.typeKind == TypeKind.VOID) {
			Machine.emit(Op.RETURN, 0, 0, md.parameterDeclList.size());
		}else {
			Machine.emit(Op.RETURN, 1, 0, md.parameterDeclList.size());
		}
		return null;
	}

	@Override
	public Object visitParameterDecl(ParameterDecl pd, Object arg) {
		// TODO Auto-generated method stub
		pd.runtimeentity = (RuntimeEntity) arg;
		return null;
	}

	@Override
	public Object visitVarDecl(VarDecl decl, Object arg) {
		// TODO Auto-generated method stub
		// decl.
		decl.runtimeentity = (RuntimeEntity) arg;
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
		return null;
	}

	@Override
	public Object visitArrayType(ArrayType type, Object arg) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visitBlockStmt(BlockStmt stmt, Object arg) {
		// TODO Auto-generated method stub
		int currentLocalOffset = this.localVariableOffset;
		for(Statement s: stmt.sl) {
			s.visit(this, null);
		}
		int afterLocalOffset = this.localVariableOffset;
		
		this.localVariableOffset = currentLocalOffset;
		Machine.emit(Op.POP,afterLocalOffset - currentLocalOffset);
		return null;
				
		
		/*int numVar = 0;
		for(Statement s: stmt.sl) {
			if(s instanceof VarDeclStmt) {
				numVar++;
			}
			s.visit(this, null);
		}
		Machine.emit(Op.POP, 0, 0, numVar);
		this.localVariableOffset -= numVar;
		return null;*/
	}

	@Override
	public Object visitVardeclStmt(VarDeclStmt stmt, Object arg) {
		// TODO Auto-generated method stub
		KnownOffset localVarAddress = new KnownOffset(Machine.addressSize, this.localVariableOffset);
		stmt.varDecl.visit(this, localVarAddress);
		stmt.initExp.visit(this, null);
		// BaseType Decl
		if(stmt.varDecl.type instanceof BaseType) {
			if(stmt.varDecl.type.typeKind == TypeKind.INT || stmt.varDecl.type.typeKind == TypeKind.BOOLEAN ) {
				Machine.emit(Op.STORE, Reg.LB, this.localVariableOffset);
				Machine.emit(Op.LOAD, Reg.LB, this.localVariableOffset);
			}
		}
		this.localVariableOffset++;
		
		return null;
	}

	@Override
	public Object visitAssignStmt(AssignStmt stmt, Object arg) {
		// TODO Auto-generated method stub
		// Case 1: IdRef
		if(stmt.ref instanceof IdRef) {
			// RHS
			stmt.val.visit(this, null);
			// start LHS
			this.visitLHS = true;
			// cstmt.ref.visit(this, null);
			
			KnownOffset refEntity = (KnownOffset) stmt.ref.decl.runtimeentity;
			int offset = refEntity.offset;
			
			// Debug: isStatic 
			if(stmt.ref.decl instanceof FieldDecl && ((FieldDecl) stmt.ref.decl).isStatic) {
					Machine.emit(Op.STORE, Reg.SB, offset);
			}
			else if(stmt.ref.decl instanceof FieldDecl) {
				Machine.emit(Op.STORE, Reg.OB, offset);
			}
			else if(stmt.ref.decl instanceof VarDecl) {
				Machine.emit(Op.STORE, Reg.LB, offset);
			}
		}
		// Case 2: QualRef
		else if(stmt.ref instanceof QualRef) {
			// start LHS
			this.visitLHS = true;
			this.qrefLayer = 0;
			stmt.ref.visit(this, null);
			// // RHS
			stmt.val.visit(this, null);
			KnownOffset refEntity = (KnownOffset) stmt.ref.decl.runtimeentity;
			int offset = refEntity.offset;
			if(stmt.ref.decl instanceof FieldDecl && ((FieldDecl) stmt.ref.decl).isStatic) {
				Machine.emit(Op.STORE, Reg.SB, offset);
			}
			else if(stmt.ref.decl instanceof FieldDecl) {
				// Debug
				/*if(stmt.val instanceof NewObjectExpr || stmt.val instanceof NewArrayExpr) {
					stmt.ref.decl.runtimeentity = new KnownAddress(refEntity.size, refEntity.offset);
				}*/
					Machine.emit(Prim.fieldupd);
			}
			
		}
		// Case 3: Unknown
		else {
			reporter.reportError(stmt.ref.posn.toString()+" CodeGen Error -- Ref other than IdRef and QRef can't be at the left hand side of the assignstmt");
			System.exit(4);
		}
		
		this.visitLHS = false;
		
		return null;
	}

	@Override
	public Object visitIxAssignStmt(IxAssignStmt stmt, Object arg) {
		// TODO Auto-generated method stub
		// LHS
		// ArrayRef
		if(stmt.ref.decl.type instanceof ArrayType) {
			if(stmt.ref instanceof IdRef) {
				stmt.ref.visit(this, null);
			}
			else if(stmt.ref instanceof QualRef) {
				this.qrefLayer = 0;
				stmt.ref.visit(this, null);
			}
			else {
				reporter.reportError(stmt.ref.posn.toString() + " CodeGen Error -- ref must be type of IdRef or QRef");
				System.exit(4);
			}
		}
		else {
			reporter.reportError(stmt.ref.posn.toString() + " CodeGen Error -- ref type must be type of ArrayType");
			System.exit(4);
		}
		
		// IxExpression
		stmt.ix.visit(this, null);
		
		// RHS
		stmt.exp.visit(this, null);
		
		// Emit
		Machine.emit(Prim.arrayupd);
		return null;
	}

	@Override
	public Object visitCallStmt(CallStmt stmt, Object arg) {
		// TODO Auto-generated method stub
		for(Expression e: stmt.argList) {
			e.visit(this, null);
		}
		
		
		Reference mr = stmt.methodRef;
		MethodDecl md = (MethodDecl) mr.decl;
		if(md == null) {
			reporter.reportError(md.posn.toString() + " Codegen Error: not method declaration");
			System.exit(4);
		}
		
		// Case 1: QualRef
		if(stmt.methodRef instanceof QualRef) {
			this.qrefLayer = 0;
			Reference subref = ((QualRef) mr).ref;
			int callStmtAddress = 0;
			// Case : Non-static
			if(!md.isStatic) {
				// Edge Case: System.out.println
				if(stmt.methodRef.decl.equals(this.printlndecl)) {
					Machine.emit(Prim.putintnl);
					return null;
				}
				/*if(md.name.equals("println") && subref instanceof QualRef && ((QualRef) subref).decl.name.equals("out")) {
					Reference subsubRef = ((QualRef) subref).ref;
					if(subsubRef.id.decl.name.equals("System")) {
						Machine.emit(Prim.putintnl);
						return null;
					}
				}*/
				
				stmt.methodRef.visit(this, null);
				callStmtAddress = Machine.nextInstrAddr();
				Machine.emit(Op.CALLI, Reg.CB, -1);
			}
			
			// Case: Static
			else {
				callStmtAddress = Machine.nextInstrAddr();
				Machine.emit(Op.CALL, Reg.CB, -1);
			}
			
			
			// Case 1: first call to md
			if(!map.containsKey(md)) {
				map.put(md, new ArrayList<Integer>());
				// Case 2: Not first call, already have arraylist initialized
			}
			map.get(md).add(callStmtAddress);
			
			
		}
		
		// Case 2: IdRef
		// Example: class A{ callB(){callC();} callC(){...}}
		else if(stmt.methodRef instanceof IdRef) {
			int callStmtAddress = 0;
			// Case: Non-static 
			if(!md.isStatic) {
				Machine.emit(Op.LOADA, Reg.OB, 0);
				callStmtAddress = Machine.nextInstrAddr();
				Machine.emit(Op.CALLI, Reg.CB, -1);
			}
			// Case: static
			else {
				callStmtAddress = Machine.nextInstrAddr();
				Machine.emit(Op.CALL, Reg.CB, -1);
			}
			
			// Case 1: first call to md
			if(!map.containsKey(md)) {
				map.put(md, new ArrayList<Integer>());
				// Case 2: Not first call, already have arraylist initialized
			}
			map.get(md).add(callStmtAddress);
			
		}
		
		// If this is a callStmt, then the return value is meaningless since it won't be assigned to other references
		// So we should pop it
		if (md.type.typeKind != TypeKind.VOID) {
			Machine.emit(Machine.Op.POP, 1);
		}
		return null;
	}

	@Override
	public Object visitReturnStmt(ReturnStmt stmt, Object arg) {
		if(stmt.returnExpr != null) {
			stmt.returnExpr.visit(this, null);
		}
		
		return null;
	}

	@Override
	public Object visitIfStmt(IfStmt stmt, Object arg) {
		// TODO Auto-generated method stub
		stmt.cond.visit(this, null);
		int i = Machine.nextInstrAddr();
		Machine.emit(Op.JUMPIF, 0, Reg.CB, -1);
		stmt.thenStmt.visit(this, null);
		int j = Machine.nextInstrAddr();
		Machine.emit(Op.JUMP, 0, Reg.CB, -1);
		int g = Machine.nextInstrAddr();
		Machine.patch(i, g);
		if(stmt.elseStmt != null) {
			stmt.elseStmt.visit(this, null);
		}
		int h = Machine.nextInstrAddr();
		Machine.patch(j, h);
		return null;
	}

	@Override
	public Object visitWhileStmt(WhileStmt stmt, Object arg) {
		// TODO Auto-generated method stub
		int j = Machine.nextInstrAddr();
		Machine.emit(Op.JUMP, Reg.CB, -1);
		int g = Machine.nextInstrAddr();
		stmt.body.visit(this, null);
		int h = Machine.nextInstrAddr();
		Machine.patch(j, h);
		stmt.cond.visit(this, null);
		Machine.emit(Op.JUMPIF, 1, Reg.CB, g);
		
		return null;
	}

	@Override
	public Object visitUnaryExpr(UnaryExpr expr, Object arg) {
		// TODO Auto-generated method stub
		expr.expr.visit(this, null);
		String op = expr.operator.spelling;
		if(op.equals("-")) {
			Machine.emit(Prim.neg);
		}
		else if(op.equals("!")) {
			Machine.emit(Prim.not);
		}
		return null;
	}

	@Override
	public Object visitBinaryExpr(BinaryExpr expr, Object arg) {
		String op = expr.operator.spelling;
		// TODO Auto-generated method stub
		// short circuit (before visiting the second expression)
		if(op.equals("&&") || op.equals("||")) {
			int left = (int) expr.left.visit(this, null);
			if(left == 0 && op.equals("&&")) {
				return null;
			}
			if(left == 1 && op.equals("||")) {
				return null;
			}
		}
		expr.left.visit(this, null);
		expr.right.visit(this, null);
		
		if(op.equals("+")) {
			Machine.emit(Prim.add);
		}
		else if(op.equals("-")) {
			Machine.emit(Prim.sub);
		}
		else if(op.equals("*")) {
			Machine.emit(Prim.mult);
		}
		else if(op.equals("/")) {
			Machine.emit(Prim.div);
		}
		else if(op.equals("&&")) {
			Machine.emit(Prim.and);
		}
		else if(op.equals("||")) {
			Machine.emit(Prim.or);
		}
		else if(op.equals("==")) {
			Machine.emit(Prim.eq);
		}
		else if(op.contentEquals("!=")) {
			Machine.emit(Prim.ne);
		}
		else if(op.equals("<=")) {
			Machine.emit(Prim.le);
		}
		else if(op.equals(">=")) {
			Machine.emit(Prim.ge);
		}
		else if(op.equals(">")) {
			Machine.emit(Prim.gt);
		}
		else if(op.equals("<")) {
			Machine.emit(Prim.lt);
		}
		return null;
	}

	@Override
	public Object visitRefExpr(RefExpr expr, Object arg) {
		// TODO Auto-generated method stub
		if(expr.ref instanceof QualRef) {
			this.qrefLayer = 0;
		}
		expr.ref.visit(this, null);
		return null;
	}

	@Override
	public Object visitIxExpr(IxExpr expr, Object arg) {
		// TODO Auto-generated method stub
		// ArrayRef
		if(expr.ref.decl.type instanceof ArrayType) {
			if(expr.ref instanceof IdRef) {
				expr.ref.visit(this, null);
			}
			else if(expr.ref instanceof QualRef) {
				this.qrefLayer = 0;
				expr.ref.visit(this, null);
			}
			else {
				reporter.reportError(expr.ref.posn.toString() + " CodeGen Error -- ref must be type of IdRef or QRef");
				System.exit(4);
			}
		}
		else {
			reporter.reportError(expr.ref.posn.toString() + " CodeGen Error -- ref type must be type of ArrayType");
			System.exit(4);
		}
		// Ix
		expr.ixExpr.visit(this, null);
		
		//ArrayRef
		Machine.emit(Prim.arrayref);
		return null;
	}

	// Debug visitCallExpr
	@Override
	public Object visitCallExpr(CallExpr expr, Object arg) {
		// TODO Auto-generated method stub
		for(Expression e: expr.argList) {
			e.visit(this, null);
		}
		
		
		Reference mr = expr.functionRef;
		MethodDecl md = (MethodDecl) mr.decl;
		if(md == null) {
			reporter.reportError(md.posn.toString() + " Codegen Error: not method declaration");
			System.exit(4);
		}
		
		// Case 1: QualRef
		if(expr.functionRef instanceof QualRef) {
			this.qrefLayer = 0;
			Reference subref = ((QualRef) mr).ref;
			int callStmtAddress = 0;
			// Case : Non-static
			if(!md.isStatic) {
				// Edge Case: System.out.println
				if(md.name.equals("println") && subref instanceof QualRef && ((QualRef) subref).decl.name.equals("out")) {
					Reference subsubRef = ((QualRef) subref).ref;
					if(subsubRef.id.decl.name.equals("System")) {
						reporter.reportError(expr.functionRef.posn.toString()+" System.out.println() can't be expression ");
						System.exit(4);
					}
				}
				expr.functionRef.visit(this, null);
			
				callStmtAddress = Machine.nextInstrAddr();
				Machine.emit(Op.CALLI, Reg.CB, -1);
			}
			
			// Case: Static
			else {
				callStmtAddress = Machine.nextInstrAddr();
				Machine.emit(Op.CALL, Reg.CB, -1);
			}
			
			
			// Case 1: first call to md
			if(!map.containsKey(md)) {
				map.put(md, new ArrayList<Integer>());
				// Case 2: Not first call, already have arraylist initialized
			}
			map.get(md).add(callStmtAddress);
			
			
		}
		
		// Case 2: IdRef
		// Example: class A{ callB(){callC();} callC(){...}}
		else if(expr.functionRef instanceof IdRef) {
			int callStmtAddress = 0;
			// Case: Non-static 
			if(!md.isStatic) {
				Machine.emit(Op.LOADA, Reg.OB, 0);
				callStmtAddress = Machine.nextInstrAddr();
				Machine.emit(Op.CALLI, Reg.CB, -1);
			}
			// Case: static
			else {
				callStmtAddress = Machine.nextInstrAddr();
				Machine.emit(Op.CALL, Reg.CB, -1);
			}
			
			// Case 1: first call to md
			if(!map.containsKey(md)) {
				map.put(md, new ArrayList<Integer>());
				// Case 2: Not first call, already have arraylist initialized
			}
			map.get(md).add(callStmtAddress);
			
		}
		
		return null;
	}

	@Override
	public Object visitLiteralExpr(LiteralExpr expr, Object arg) {
		// TODO Auto-generated method stu
		if(expr.lit instanceof BooleanLiteral) {
			int res = (int) expr.lit.visit(this, null);
			return res;
		}
		expr.lit.visit(this, null);
		return null;
	}

	@Override
	public Object visitNewObjectExpr(NewObjectExpr expr, Object arg) {
		// TODO Auto-generated method stub
		Machine.emit(Op.LOADL, -1);
		if(expr.classtype.className.decl != null && expr.classtype.className.decl instanceof ClassDecl) {
			int size = ((ClassDecl) expr.classtype.className.decl).runtimeentity.size;
			Machine.emit(Op.LOADL, size);
			Machine.emit(Prim.newobj);
		}else {
			reporter.reportError(expr.posn.toString() + " Codegen Error: Can't create new instance of non-class type");
			System.exit(4);
		}
		
		return null;
	}

	@Override
	public Object visitNewArrayExpr(NewArrayExpr expr, Object arg) {
		// TODO Auto-generated method stub
		expr.sizeExpr.visit(this, null);
		Machine.emit(Prim.newarr);
		return null;
	}

	@Override
	public Object visitThisRef(ThisRef ref, Object arg) {
		// TODO Auto-generated method stub
		RuntimeEntity res = null;
		res = (KnownOffset) ref.decl.runtimeentity;
		// Debug: 
		Machine.emit(Op.LOADA, Reg.OB, 0);
		return null;
		// return res;
	}

	@Override
	public Object visitIdRef(IdRef ref, Object arg) {
		// TODO Auto-generated method stub
		RuntimeEntity res = null;
		ref.id.visit(this, null);
		Declaration temp = ref.id.decl; // or Declaration temp = ref.decl?
		res = (KnownOffset) temp.runtimeentity;
		
		int offset = ((KnownOffset) temp.runtimeentity).offset;
		if(ref.decl instanceof FieldDecl && ((FieldDecl) ref.decl).isStatic) {
			Machine.emit(Op.LOAD, Reg.SB, offset);
		}
		else if(ref.decl instanceof FieldDecl) {
			//Debug
			Machine.emit(Op.LOAD, Reg.OB, offset);
		}
		else if(ref.decl instanceof VarDecl) {
			Machine.emit(Op.LOAD, Reg.LB, offset);
		}
		else if(ref.decl instanceof ParameterDecl) {
			// here offset should be negative
			Machine.emit(Op.LOAD, Reg.LB, offset);
		}
		
		return null;
		//return res;
	}

	@Override
	public Object visitQRef(QualRef ref, Object arg) {
		// TODO Auto-generated method stub
		this.qrefLayer--;
		RuntimeEntity res = null;
		res = (KnownOffset) ref.decl.runtimeentity;
		
		// Edge Case: 
		if(this.visitLHS) {
			if(ref.decl instanceof FieldDecl && ref.decl.name.equals("length")) {
				if(ref.ref != null && ref.ref.decl.type instanceof ArrayType) {
					reporter.reportError(ref.posn.toString()+" Codegen Error -- array.length can't be at the left hand side of assign statement");
					System.exit(4);
				}
			}
		}
		
		
		// Case 0: a = array.length
		if(ref.decl instanceof FieldDecl && ref.decl.name.equals("length")) {
			if(ref.ref != null && ref.ref.decl.type instanceof ArrayType) {
				ref.ref.visit(this, null);
				Machine.emit(Prim.arraylen);
			}
		}
		else if(ref.decl instanceof FieldDecl && ((FieldDecl) ref.decl).isStatic) {
			// Debug
			Machine.emit(Op.LOAD, Reg.SB, ((KnownOffset) ref.decl.runtimeentity).offset);
			// Machine.emit(Op.LOAD, Reg.SB, ((KnownOffset) ref.id.decl.runtimeentity).offset);
		}
		else if(ref.decl instanceof FieldDecl || ref.decl instanceof MethodDecl) {
			// Edge Case: The start of the QualRef 
			if(ref.ref instanceof IdRef) {
				// Case: Implicit THIS. So manually load a's Address using LOADA and fieldref (and then load b)
				if(ref.ref.decl instanceof FieldDecl && ((FieldDecl) ref.ref.decl).isStatic) {
					Machine.emit(Op.LOADA, Reg.SB, 0);
					Machine.emit(Op.LOADL, ((KnownOffset)ref.ref.decl.runtimeentity).offset);
					Machine.emit(Prim.fieldref);
				}
				else if(ref.ref.decl instanceof FieldDecl) {
					Machine.emit(Op.LOADA, Reg.OB, 0);
					Machine.emit(Op.LOADL, ((KnownOffset)ref.ref.decl.runtimeentity).offset);
					Machine.emit(Prim.fieldref);
				}
				else if(ref.ref.decl instanceof VarDecl) {
					// Load a's address directly if a is a local variable
					Machine.emit(Op.LOAD, Reg.LB, ((KnownOffset)ref.ref.decl.runtimeentity).offset);
					//Machine.emit(Op.LOADL, ((KnownOffset)ref.ref.decl.runtimeentity).offset);
					//Machine.emit(Prim.fieldref);
				}
				else if(ref.ref.decl instanceof ParameterDecl) {
					// here offset should be negative
					Machine.emit(Op.LOAD, Reg.LB, ((KnownOffset)ref.ref.decl.runtimeentity).offset);
					//Machine.emit(Op.LOADA, Reg.LB, 0);
					//Machine.emit(Op.LOADL, ((KnownOffset)ref.ref.decl.runtimeentity).offset);
					//Machine.emit(Prim.fieldref);
				}
				// Other cases: 
				else {
					reporter.reportError(ref.ref.posn.toString()+" CodeGen Error -- Other decl can't be use as the head of the QRef");
					System.exit(4);
				}
				
			}
			else {
				ref.ref.visit(this, null);
			}
			
			// Step 2:
			// load the field offset

			if(ref.decl instanceof FieldDecl) {
				Machine.emit(Op.LOADL, ((KnownOffset) ref.decl.runtimeentity).offset);
			}
			
			
			// Step 3:
			// if field is not the last element in the QualRef, use fieldRef to go to the field's address
			this.qrefLayer++;
			
			// If at the LHS of an assignstmt, then at the last element/layer we can't use fieldref
			if(!(ref.decl instanceof MethodDecl)) {
				if(this.visitLHS) {
					if(this.qrefLayer<0) {
						Machine.emit(Prim.fieldref);
					}
				}
				// But if at the RHS or in a callstmt, then we should call fieldref
				else {
					Machine.emit(Prim.fieldref);
				}
			}
			
			
		}
		return null;
	}

	@Override
	public Object visitIdentifier(Identifier id, Object arg) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visitOperator(Operator op, Object arg) {
		// TODO Auto-generated method stub
		//if(op.)
		return null;
	}

	@Override
	public Object visitIntLiteral(IntLiteral num, Object arg) {
		// TODO Auto-generated method stub
		int v = valuation (num.spelling);
		Machine.emit(Op.LOADL, v);
		return null;
	}

	@Override
	public Object visitBooleanLiteral(BooleanLiteral bool, Object arg) {
		// TODO Auto-generated method stub
		if(bool.spelling.equals("true")) {
			Machine.emit(Op.LOADL, 1);
			return 1;
		}
		else {
			Machine.emit(Op.LOADL, 0);
			return 0;
		}
	}

	@Override
	public Object visitNullLiteral(NullLiteral nullLiteral, Object arg) {
		// TODO Auto-generated method stub
		Machine.emit(Op.LOADL, 0);
		return null;
	}
	
	// helper method
	public static int valuation(String spelling) {
		return Integer.parseInt(spelling);
	}

}
