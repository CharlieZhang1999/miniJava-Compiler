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
	private ErrorReporter reporter;
	private int static_offset;
	private int localVariableOffset;
	private int patchAddr_Call_main;
	private MethodDecl currentMethodDecl;
	private HashMap<MethodDecl, ArrayList<Integer>> map;
	
	public CodeGenerator(Package ast, ErrorReporter reporter){
		this.reporter = reporter;
		this.map = new HashMap<MethodDecl, ArrayList<Integer>>();
		ast.visit(this, null);
		
	}
	
	@Override
	public Object visitPackage(Package prog, Object arg) {
		// TODO Auto-generated method stub
		/*for(ClassDecl cd: prog.classDeclList) {
			for(FieldDecl fd: cd.fieldDeclList) {
				if(fd.isStatic) {
					Machine.emit(Machine.Op.PUSH, 1);
					fd.runtimeentity = new Address(Machine.characterSize, static_offset);
					static_offset++;
				}
			}
		}*/
		
		
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
				
				if(cd.fieldDeclList.get(fieldCount).isStatic) {
					KnownAddress fieldAddress = new KnownAddress(Machine.addressSize, staticfieldOffset);
					staticfieldOffset++;
					cd.fieldDeclList.get(fieldCount).visit(this, fieldAddress);
				}
				else {
					KnownAddress fieldAddress = new KnownAddress(Machine.addressSize, fieldOffset);
					fieldOffset++;
					cd.fieldDeclList.get(fieldCount).visit(this, fieldAddress);
				}
				
			}
			// Debug: IDK what i'm doing rn
			cd.runtimeentity = new KnownAddress(cd.fieldDeclList.size()-staticfieldOffset, staticfieldOffset+1);
		}
		
		// visit all classses
		for (ClassDecl classDecl : prog.classDeclList) {
            classDecl.visit(this, arg);
        }
		
		// patch done here
		for(MethodDecl md: map.keySet()) {
			for(int callStmtAddress: map.get(md)) {
				Machine.patch(callStmtAddress, ((KnownAddress) md.runtimeentity).address);
			}
		}
		
		
		// copy and paste debugger here 
		/*
		 * write code to object code file (.mJAM)
		 */
		String objectCodeFileName = "Counter.mJAM";
		ObjectFile objF = new ObjectFile(objectCodeFileName);
		System.out.print("Writing object code file " + objectCodeFileName + " ... ");
		if (objF.write()) {
			System.out.println("FAILED!");
			return null;
		}
		else
			System.out.println("SUCCEEDED");	

		 // create asm file corresponding to object code using disassembler 
        String asmCodeFileName = objectCodeFileName.replace(".mJAM",".asm");
        System.out.print("Writing assembly file " + asmCodeFileName + " ... ");
        Disassembler d = new Disassembler(objectCodeFileName);
        if (d.disassemble()) {
                System.out.println("FAILED!");
                return null;
        }
        else
                System.out.println("SUCCEEDED");

	/* 
	 * run code using debugger
	 * 
	 */
        System.out.println("Running code in debugger ... ");
        Interpreter.debug(objectCodeFileName, asmCodeFileName);

        System.out.println("*** mJAM execution completed");

		
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
		md.runtimeentity = new KnownAddress(Machine.addressSize, Machine.nextInstrAddr());
		this.currentMethodDecl = md;
		// edge case: main method
		if(md.name.equals("main")) {
			Machine.patch(patchAddr_Call_main, Machine.nextInstrAddr());
		}
		
		// initialize paramOffset
		int paramOffset = -1 * md.parameterDeclList.size();
		for(ParameterDecl pd: md.parameterDeclList) {
			KnownAddress paramAddress = new KnownAddress(Machine.addressSize, paramOffset);
			pd.visit(this, paramAddress);
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
		for(Statement s: stmt.sl) {
			s.visit(this, null);
		}
		return null;
	}

	@Override
	public Object visitVardeclStmt(VarDeclStmt stmt, Object arg) {
		// TODO Auto-generated method stub
		KnownAddress localVarAddress = new KnownAddress(Machine.addressSize, this.localVariableOffset);
		stmt.varDecl.visit(this, localVarAddress);
		stmt.initExp.visit(this, null);
		// BaseType Decl
		if(stmt.varDecl.type instanceof BaseType) {
			if(stmt.varDecl.type.typeKind == TypeKind.INT || stmt.varDecl.type.typeKind == TypeKind.BOOLEAN ) {
				Machine.emit(Op.STORE, Reg.LB, this.localVariableOffset);
			}
		}
		this.localVariableOffset++;
		
		return null;
	}

	@Override
	public Object visitAssignStmt(AssignStmt stmt, Object arg) {
		// TODO Auto-generated method stub
		// RHS
		stmt.val.visit(this, null);
		
		// LHS
		stmt.ref.visit(this, null);
		// Case 1: IdRef or QualRef
		if(stmt.ref instanceof IdRef || stmt.ref instanceof QualRef) {
			KnownAddress refEntity = (KnownAddress) stmt.ref.id.decl.runtimeentity;
			int offset = refEntity.address;
			// Machine.emit(Machine.Op.LOAD, Reg.OB, refEntity.address);
			
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
			// should we check if it's a ParameterDecl? 
			// Debug
		}
		// Case 2: ThisRef
		else if(stmt.ref instanceof ThisRef) {
			KnownAddress refEntity = (KnownAddress) stmt.ref.id.decl.runtimeentity;
			int offset = refEntity.address;
			Machine.emit(Op.STORE, Reg.OB, offset);
		}
		// Case 3: unkown? 
		// Debug
		else {
			
		}
		
		
		return null;
	}

	@Override
	public Object visitIxAssignStmt(IxAssignStmt stmt, Object arg) {
		// TODO Auto-generated method stub
		// LHS
		// ArrayRef
		if(stmt.ref instanceof IdRef && stmt.ref.decl.type instanceof ArrayType) {
			KnownAddress refEntity = (KnownAddress) stmt.ref.visit(this, null);
			int offset = refEntity.address;
			// Debug: isStatic 
			if(stmt.ref.decl instanceof FieldDecl && ((FieldDecl) stmt.ref.decl).isStatic) {
				Machine.emit(Op.LOAD, Reg.SB, offset);
			}
			else if(stmt.ref.decl instanceof FieldDecl) {
				Machine.emit(Op.LOAD, Reg.OB, offset);
			}
			else if(stmt.ref.decl instanceof VarDecl) {
				Machine.emit(Op.LOAD, Reg.LB, offset);
			}
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
		}
		
		// Case 1: QualRef
		if(stmt.methodRef instanceof QualRef) {
			Reference subref = ((QualRef) mr).ref;
			int callStmtAddress = 0;
			// Case : Non-static
			if(!(subref.decl instanceof ClassDecl) && !md.isStatic) {
				// Edge Case: System.out.println
				if(md.name.equals("println") && subref instanceof QualRef && ((QualRef) subref).decl.name.equals("out")) {
					Reference subsubRef = ((QualRef) subref).ref;
					if(subsubRef.id.decl.name.equals("System")) {
						Machine.emit(Prim.putintnl);
						return null;
					}
				}
				stmt.methodRef.visit(this, null);
				/* Ommitted */
				//int instanceOffset = ((KnownAddress)subref.decl.runtimeentity).address;
				//Machine.emit(Op.LOAD, Reg.LB, instanceOffset);				
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
		
		return null;
	}

	@Override
	public Object visitReturnStmt(ReturnStmt stmt, Object arg) {
		if(stmt.returnExpr == null) {
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
		// TODO Auto-generated method stub
		expr.left.visit(this, null);
		expr.right.visit(this, null);
		String op = expr.operator.spelling;
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
		expr.ref.visit(this, null);
		
		// KnownAddress refAddress = (KnownAddress) expr.ref.visit(this, null);
		// int offset = refAddress.address;
		
		
		/* Omitted
		// Case 1: IdRef
		if(expr.ref instanceof IdRef) {
			/*if(expr.ref.decl instanceof FieldDecl && ((FieldDecl) expr.ref.decl).isStatic) {
				Machine.emit(Op.LOAD, Reg.SB, offset);
			}
			else if(expr.ref.decl instanceof FieldDecl) {
				Machine.emit(Op.LOAD, Reg.OB, offset);
			}
			else if(expr.ref.decl instanceof VarDecl) {
				Machine.emit(Op.LOAD, Reg.LB, offset);
			}
			else if(expr.ref.decl instanceof ParameterDecl) {
				// here offset should be negative
				Machine.emit(Op.LOAD, Reg.LB, offset);
			}
			expr.ref.visit(this, null);
		}
		// Case 2: QualRef
		else if(expr.ref instanceof QualRef) {

		}
		// Case 3: ThisRef
		*/
		return null;
	}

	@Override
	public Object visitIxExpr(IxExpr expr, Object arg) {
		// TODO Auto-generated method stub
		// ArrayRef
		if(expr.ref instanceof IdRef && expr.ref.decl.type instanceof ArrayType) {
			KnownAddress refEntity = (KnownAddress) expr.ref.visit(this, null);
			int offset = refEntity.address;
			// Debug: isStatic 
			if(expr.ref.decl instanceof FieldDecl && ((FieldDecl) expr.ref.decl).isStatic) {
				Machine.emit(Op.LOAD, Reg.SB, offset);
			}
			else if(expr.ref.decl instanceof FieldDecl) {
				Machine.emit(Op.LOAD, Reg.OB, offset);
			}
			else if(expr.ref.decl instanceof VarDecl) {
				Machine.emit(Op.LOAD, Reg.LB, offset);
			}
		}
		// Ix
		expr.ixExpr.visit(this, null);
		
		return null;
	}

	// Debug visitCallExpr
	@Override
	public Object visitCallExpr(CallExpr expr, Object arg) {
		// TODO Auto-generated method stub
		for(Expression e: expr.argList) {
			e.visit(this, null);
		}
		
		MethodDecl md = (MethodDecl) expr.functionRef.decl;
		if(md == null) {
			reporter.reportError(md.posn.toString() + " Codegen Error: not method declaration");
		}
		
		// Case 1: QualRef
		if(expr.functionRef instanceof QualRef) {
			Reference subref = ((QualRef) expr.functionRef).ref;
			int callStmtAddress = 0;
			// Case : Non-static
			if(!(subref.decl instanceof ClassDecl) && !md.isStatic) {
				
				expr.functionRef.visit(this, null);
				/* Ommitted */
				//int instanceOffset = ((KnownAddress)subref.decl.runtimeentity).address;
				//Machine.emit(Op.LOAD, Reg.LB, instanceOffset);				
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
		// TODO Auto-generated method stub
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
		res = (KnownAddress) ref.decl.runtimeentity;
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
		res = (KnownAddress) temp.runtimeentity;
		
		int offset = ((KnownAddress) temp.runtimeentity).address;
		if(ref.decl instanceof FieldDecl && ((FieldDecl) ref.decl).isStatic) {
			Machine.emit(Op.LOAD, Reg.SB, offset);
		}
		else if(ref.decl instanceof FieldDecl) {
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
		RuntimeEntity res = null;
		res = (KnownAddress) ref.decl.runtimeentity;
		// Case 1: FieldDecl && static
		if(ref.id.decl instanceof FieldDecl && ref.id.decl.name.equals("length")) {
			if(ref.ref.decl.type instanceof ArrayType) {
				ref.ref.visit(this, null);
				Machine.emit(Prim.arraylen);
			}
		}
		else if(ref.id.decl instanceof FieldDecl && ((FieldDecl) ref.decl).isStatic) {
			Machine.emit(Op.LOAD, Reg.SB, ((KnownAddress) ref.id.decl.runtimeentity).address);
		}
		// Case 2: FieldDecl 
		else if(ref.id.decl instanceof FieldDecl) {
			ref.ref.visit(this, null);
			Machine.emit(Op.LOADL, ((KnownAddress) ref.id.decl.runtimeentity).address);
			Machine.emit(Prim.fieldref);
		}
		// Case 3: MethodDecl eg: counter.increse(3)
		else if(ref.id.decl instanceof MethodDecl) {
			ref.ref.visit(this, null);
		}
		
		return null;
		// return res;
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
		}
		else {
			Machine.emit(Op.LOADL, 0);
		}
		return null;
	}

	@Override
	public Object visitNullLiteral(NullLiteral nullLiteral, Object arg) {
		// TODO Auto-generated method stub
		return null;
	}
	
	// helper method
	public static int valuation(String spelling) {
		return Integer.parseInt(spelling);
	}

}
