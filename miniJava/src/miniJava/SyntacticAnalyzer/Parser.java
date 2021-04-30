package miniJava.SyntacticAnalyzer;

import miniJava.ErrorReporter;
import miniJava.AbstractSyntaxTrees.*;
import miniJava.AbstractSyntaxTrees.Package;
import miniJava.SyntacticAnalyzer.*;

public class Parser {
	private Scanner scanner;
	private ErrorReporter reporter;
	private Token token;
	private boolean trace = false;
	private SourcePosition prevPos;
	
	public Parser(Scanner scanner, ErrorReporter reporter) {
		this.scanner = scanner;
		this.reporter = reporter;
	}
	
	/**
	 * SyntaxError is used to unwind parse stack when parse fails
	 *
	 */
	class SyntaxError extends Error {
		private static final long serialVersionUID = 1L;	
	}
	
	
	/**
	 *  parse input, catch possible parse error
	 */
	public AST parse() {
		token = scanner.scan();
		// Skip comments
		while(token.kind == TokenKind.COMMENT) {
			acceptIt();
;		}
		try {
			Package pkg = parseProgram();
			return pkg;
		}
		catch (SyntaxError e) { }
		return null;
	}
	
	// Program ::= (ClassDeclaration)*eot
	public Package parseProgram() throws SyntaxError{
		Package pkg = null;
		SourcePosition packageSp = new SourcePosition();
		startPos(packageSp);
		
		ClassDeclList cdl = new ClassDeclList();
		// add predefined classes
		cdl.add(addPredefinedSystem());
		cdl.add(addPrintStream());
		cdl.add(addPredefinedString());
		while(token.kind == TokenKind.CLASS) {
			ClassDecl cd  = parseClassDeclaration();
			cdl.add(cd);
		}
		accept(TokenKind.EOT);
		endPos(packageSp);
		pkg = new Package(cdl, packageSp);
		return pkg;
	}
	
	public ClassDecl addPredefinedSystem() {
		ClassDecl cd = null;
		FieldDeclList fdl = new FieldDeclList();
		MethodDeclList mtdl = new MethodDeclList(); 
		FieldDecl fd = null;
		
		Token new_token = new Token(TokenKind.PREDEFINED, "_PrintStream", null);
		// Also serves as the Class ID
		Identifier id = new Identifier(new_token);
		TypeDenoter td = new ClassType(id, null);
		fd = new FieldDecl(false, true, td, "out", null);
		fdl.add(fd);
		cd = new ClassDecl("System", id, fdl, mtdl, null);
		return cd;
	}
	
	public ClassDecl addPrintStream() {
		ClassDecl cd = null;
		FieldDeclList fdl = new FieldDeclList();
		MethodDeclList mtdl = new MethodDeclList(); 
		ParameterDeclList pdl = new ParameterDeclList();
		StatementList sl = new StatementList();
		
		FieldDecl fd = null;
		TypeDenoter td = new BaseType(TypeKind.VOID, null);
		fd = new FieldDecl(false, false, td, "println", null);		
		
		TypeDenoter td1 = new BaseType(TypeKind.INT, null);
		ParameterDecl pd1 = new ParameterDecl(td1, "n", null);
		pdl.add(pd1);
		
		MethodDecl md = new MethodDecl(fd, pdl, sl, null);
		mtdl.add(md);
		
		// Class Id
		Token new_token = new Token(TokenKind.ID, "_PrintStream", null);
		Identifier id = new Identifier(new_token);
		cd = new ClassDecl("_PrintStream",id, fdl, mtdl, null);
		return cd;
	}
	
	public ClassDecl addPredefinedString() {
		ClassDecl cd = null;
		FieldDeclList fdl = new FieldDeclList();
		MethodDeclList mtdl = new MethodDeclList();

		// Class Id
		Token new_token = new Token(TokenKind.ID, "String", null);
		Identifier id = new Identifier(new_token);
		cd = new ClassDecl("String", id, fdl, mtdl, null);
		return cd;
	}
	
	// ClassDeclaration ::= class id { ( FieldDeclaration | MethodDeclaration )* } 
	public ClassDecl parseClassDeclaration() throws SyntaxError {
		ClassDecl cd = null;
		SourcePosition cdSp = new SourcePosition();
		startPos(cdSp);
		
		FieldDeclList fdl = new FieldDeclList();
		MethodDeclList mtdl = new MethodDeclList(); 
		
		
		accept(TokenKind.CLASS);
		
		//classname
		String cn =  token.spelling;
		//Class ID
		Identifier classid = new Identifier(token);
		
		accept(TokenKind.ID);
		accept(TokenKind.LCURBRACK);
		while(token.kind != TokenKind.RCURBRACK) {	
			
			// initialization
			boolean isPrivate = false;
			boolean isStatic = false;
			TypeDenoter td = null;
			String name = null;
			FieldDecl fd = null;
			MethodDecl md = null;
			SourcePosition memberSp = new SourcePosition();
			startPos(memberSp);
			
			//Possible field soucePosition in the form MethodDecl(FieldDecl, ParamList..)
			SourcePosition memberSp2 = new SourcePosition();
			startPos(memberSp2);
			
			
			if(token.kind == TokenKind.PUBLIC || token.kind == TokenKind.PRIVATE) {
				if(token.kind == TokenKind.PRIVATE) {
					isPrivate = true;
				}
				acceptIt();
			}
			if(token.kind == TokenKind.STATIC) {
				isStatic = true;
				acceptIt();
			}
			
			// Method Declaration
			if(token.kind == TokenKind.VOID) {
				// typedenoter
				td = new BaseType(TypeKind.VOID, null);
				acceptIt();
				
				//string name 
				name = token.spelling;
				accept(TokenKind.ID);
				
				//unchecked
				fd = new FieldDecl(isPrivate, isStatic, td, name, null);
				
				accept(TokenKind.LPAREN);
				ParameterDeclList pdl = new ParameterDeclList();
				StatementList sl = new StatementList();
				if(token.kind == TokenKind.INT || token.kind == TokenKind.BOOLEAN || token.kind == TokenKind.ID) {
					pdl = parseParameterList();
				}
				accept(TokenKind.RPAREN);
				accept(TokenKind.LCURBRACK);
				
				// {Statement *}
				while(token.kind != TokenKind.RCURBRACK) {
					Statement s = parseStatement();
					sl.add(s);
				}
				accept(TokenKind.RCURBRACK);
				
				// add MethodDecl to MethodDeclList
				endPos(memberSp);
				md = new MethodDecl(fd, pdl, sl, memberSp);
				mtdl.add(md);
				
			}
			else if(token.kind == TokenKind.INT || token.kind == TokenKind.BOOLEAN || token.kind == TokenKind.ID) {
				td = parseType();
				
				// PA5
				// SourcePosition potentialIdRefSp = new SourcePosition();
				// Token cur = token;
				name = token.spelling;
				accept(TokenKind.ID);
				// endPos(potentialIdRefSp);
				
				// Field Declaration
				// done
				if(token.kind == TokenKind.SEMICOLON) {
					acceptIt();
					
					// add FieldDecl to FieldDeclList
					endPos(memberSp);
					fd = new FieldDecl(isPrivate, isStatic, td, name, memberSp);
					fdl.add(fd);
				}
				else if(token.kind == TokenKind.ASSIGNMENT && isStatic) {
					acceptIt();
					Expression e2 = parseExpression();
					accept(TokenKind.SEMICOLON);
					endPos(memberSp);
					fd = new FieldInitialization(isPrivate, isStatic, td, name, memberSp, e2);
					fdl.add(fd);
					
				}
				// Method Declaration
				else if(token.kind == TokenKind.LPAREN) {
					accept(TokenKind.LPAREN);
					endPos(memberSp2);
					fd = new FieldDecl(isPrivate, isStatic, td, name, memberSp2);
					ParameterDeclList pdl = new ParameterDeclList();
					StatementList sl = new StatementList();
					
					if(token.kind == TokenKind.INT || token.kind == TokenKind.BOOLEAN || token.kind == TokenKind.ID) {
						pdl = parseParameterList();
					}					
					accept(TokenKind.RPAREN);
					accept(TokenKind.LCURBRACK);
					// {Statement *}
					while(token.kind != TokenKind.RCURBRACK) {
						Statement s  = parseStatement();
						sl.add(s);
					}
					accept(TokenKind.RCURBRACK);
					
					// add MethodDecl to MethodDeclList
					endPos(memberSp);
					md = new MethodDecl(fd, pdl, sl, memberSp); 
					mtdl.add(md);
				}
				else parseError("Neither field declaration nor static field initialization nor method declaration");
			}
			else parseError("Neither field declaration nor method declaration");
		}
		accept(TokenKind.RCURBRACK);
		endPos(cdSp);
		cd  =  new ClassDecl(cn, classid, fdl, mtdl, cdSp);
		return cd;
	}
	
	public TypeDenoter parseType() throws SyntaxError {
		TypeDenoter td = null;
		// for array type
		SourcePosition arrSp = new SourcePosition();
		startPos(arrSp);
		
		// for base and class type
		SourcePosition typeSp = new SourcePosition();
		startPos(typeSp);
		
		switch (token.kind) {
		
		case INT:
			acceptIt();
			if(token.kind == TokenKind.LSQBRACK) {
				acceptIt();
				accept(TokenKind.RSQBRACK);
				endPos(typeSp);
				TypeDenoter bt = new BaseType(TypeKind.INT, typeSp);
				endPos(arrSp);
				td =  new ArrayType(bt, arrSp);
			}
			else {
				endPos(typeSp);
				td = new BaseType(TypeKind.INT, typeSp);
			}
			break;
			
		case BOOLEAN:
			acceptIt();
			endPos(typeSp);
			td = new BaseType(TypeKind.BOOLEAN, typeSp);
			break;
			
		case ID:
			Identifier id = new Identifier(token);
			
			acceptIt();
			if(token.kind == TokenKind.LSQBRACK) {
				acceptIt();
				accept(TokenKind.RSQBRACK);
				endPos(typeSp);
				ClassType ct = new ClassType(id, typeSp);
				endPos(arrSp);
				td =  new ArrayType(ct, arrSp);
			}
			else {
				endPos(typeSp);
				td = new ClassType(id, typeSp);
			}
			break;
			
		default:
			parseError("Invalid Term - expecting INT or or INT[] OR BOOLEAN OR IDENTIFIER OR IDENTIFIER[] but found " + token.kind);
		}
		return td;

	}
	
	// ParameterList ::= Type id ( , Type id )*
	public ParameterDeclList parseParameterList() throws SyntaxError {
		ParameterDeclList pdl = new ParameterDeclList();
		
		SourcePosition paramsp1 = new SourcePosition();
		startPos(paramsp1);
		TypeDenoter td1 = parseType();
		endPos(paramsp1);
		ParameterDecl pd1 = new ParameterDecl(td1, token.spelling, paramsp1);
		accept(TokenKind.ID);
		pdl.add(pd1);
		
		while(token.kind == TokenKind.COMMA) {
			acceptIt();
			SourcePosition paramsp2 = new SourcePosition();
			startPos(paramsp2);
			TypeDenoter td2 = parseType();
			endPos(paramsp2);
			ParameterDecl pd2 = new ParameterDecl(td2, token.spelling, paramsp2);
			accept(TokenKind.ID);
			pdl.add(pd2);
		}
		return pdl;
	}
	
	// ArgumentList ::= Expression(,Expression)* 
	public ExprList parseArgumentList() {
		ExprList el = new ExprList();
		Expression e1 = parseExpression();
		el.add(e1);
		while(token.kind == TokenKind.COMMA) {
			acceptIt();
			Expression e2 = parseExpression();
			el.add(e2);
		}
		return el;
	}
	
	// Reference ::= id | this | Reference.id
	public Reference parseReference() {
		Reference rf = null;
		int startPosition = token.posn.start;
		
		if(token.kind == TokenKind.ID) {
			SourcePosition idRefSp = new SourcePosition();
			// manually 
			idRefSp.start = startPosition;
			Identifier id = new Identifier(token);
			acceptIt();
			endPos(idRefSp);
			rf = new IdRef(id, idRefSp);
			
		}
		else if(token.kind == TokenKind.THIS) {
			SourcePosition thisRefSp = new SourcePosition();
			// manually 
			thisRefSp.start = startPosition;
			acceptIt();
			endPos(thisRefSp);
			rf = new ThisRef(thisRefSp);
		}
		else parseError("Invalid Term - expecting Reference but found" + token.kind);
			
		while(token.kind == TokenKind.DOT) {
			acceptIt();
			// Manually
			SourcePosition qualRefSp = new SourcePosition();
			qualRefSp.start = startPosition;
			Identifier id2 = new Identifier(token);
			accept(TokenKind.ID);
			endPos(qualRefSp);
			rf = new QualRef(rf, id2, qualRefSp);
			
		}
		return rf;	
		
	}
	
	/*
	 * Statement ::=
					{ Statement* }
					| Typeid=Expression;
					| Reference = Expression ;
					| Reference [Expression]=Expression;
					| Reference (ArgumentList?);
					| return Expression? ;
					| if ( Expression ) Statement (else Statement)? | while ( Expression ) Statement
	 */
	
	public Statement parseStatement() {
		switch(token.kind) {
		
		// BlockStmt
		// done
		case LCURBRACK:
			SourcePosition bsp = new SourcePosition();
			startPos(bsp);
			acceptIt();
			// {Statement *}
			StatementList sl = new StatementList();
			while(token.kind != TokenKind.RCURBRACK) {
				Statement s = parseStatement();
				sl.add(s);
			}
			accept(TokenKind.RCURBRACK);
			endPos(bsp);
			return new BlockStmt(sl, bsp);
		
		// VarDeclStmt
		// done
		case INT: case BOOLEAN: 
			//
			SourcePosition vardeclstmtsp = new SourcePosition();
			startPos(vardeclstmtsp);
			
			//
			SourcePosition vardeclsp = new SourcePosition();
			startPos(vardeclsp);
			TypeDenoter type = parseType();
			String spelling = token.spelling;
			accept(TokenKind.ID);
			endPos(vardeclsp);
			VarDecl vd = new VarDecl(type, spelling, vardeclsp);
			
			
			accept(TokenKind.ASSIGNMENT);
			Expression e1 = parseExpression();
			accept(TokenKind.SEMICOLON);
			endPos(vardeclstmtsp);
			return new VarDeclStmt(vd, e1, vardeclstmtsp);
			
		case THIS:
			SourcePosition thisstmtsp = new SourcePosition();
			startPos(thisstmtsp);
			Reference rf = parseReference();
			switch(token.kind) {
			
			// AssignStmt
			// done
			case ASSIGNMENT:
				acceptIt();
				Expression e2 = parseExpression();
				accept(TokenKind.SEMICOLON);
				endPos(thisstmtsp);
				return new AssignStmt(rf, e2, thisstmtsp);
				
			// IxAssignStmt
			// done
			case LSQBRACK:
				acceptIt();
				Expression e3 = parseExpression();
				accept(TokenKind.RSQBRACK);
				accept(TokenKind.ASSIGNMENT);
				Expression e4 = parseExpression();
				//Expression e4 = parseExpression();
				accept(TokenKind.SEMICOLON);
				endPos(thisstmtsp);
				return new IxAssignStmt(rf, e3, e4, thisstmtsp);
			
			// CallStmt
			// basically done
			case LPAREN:
				acceptIt();
				ExprList el = new ExprList();
				if(token.kind != TokenKind.RPAREN) {
					el = parseArgumentList();
				}
				accept(TokenKind.RPAREN);
				accept(TokenKind.SEMICOLON);
				endPos(thisstmtsp);
				return new CallStmt(rf, el, thisstmtsp);
				
			default:
				parseError("Invalid Term - expecting brackets or assignment after Reference but found " + token.kind);
				return null;
			}
			
		case ID:
			SourcePosition level1sp = new SourcePosition();
			startPos(level1sp);
			SourcePosition level2sp = new SourcePosition();
			startPos(level2sp);
			SourcePosition level3sp = new SourcePosition();
			startPos(level3sp);
			SourcePosition level4sp = new SourcePosition();
			startPos(level4sp);
			SourcePosition level5sp = new SourcePosition();
			startPos(level5sp);
			int idstartPosition = token.posn.start;
			Token t = token;
			acceptIt();
			Identifier id1 = new Identifier(t);
			endPos(level5sp);
			Reference rf1 = new IdRef(id1, level5sp);
			
			
			if(token.kind == TokenKind.LSQBRACK) {
				acceptIt();
				
				// VarDeclStmt
				// ID[] id = Expression
				// done
				if(token.kind == TokenKind.RSQBRACK) {
					acceptIt();
					endPos(level4sp);
					TypeDenoter td1 = new ClassType(id1, level4sp);
					endPos(level3sp);
					ArrayType at1 = new ArrayType(td1, level3sp);
					String spelling1 = token.spelling;
					accept(TokenKind.ID);
					endPos(level2sp);
					VarDecl vd1 = new VarDecl(at1, spelling1, level2sp);
					accept(TokenKind.ASSIGNMENT);
					Expression e2 = parseExpression();
					accept(TokenKind.SEMICOLON);
					endPos(level1sp);
					return new VarDeclStmt(vd1, e2, level1sp);
				}
				
				// IxAssignStmt
				// REFERENCE[Expression]=Expression
				// done
				else {
					
					Expression e2 = parseExpression();
					accept(TokenKind.RSQBRACK);
					accept(TokenKind.ASSIGNMENT);
					Expression e3 = parseExpression();
					accept(TokenKind.SEMICOLON);
					endPos(level1sp);
					return new IxAssignStmt(rf1, e2, e3, level1sp);
				}
			}
			
			// VarDeclStmt 
			// ID ID = Expression
			// done
			else if(token.kind == TokenKind.ID) {
				String spelling2 = token.spelling;
				accept(TokenKind.ID);
				endPos(level3sp);
				TypeDenoter td1 = new ClassType(id1, level3sp);
				endPos(level2sp);
				VarDecl vd1 = new VarDecl(td1, spelling2, level2sp);
				
				accept(TokenKind.ASSIGNMENT);
				Expression e2 = parseExpression();
				accept(TokenKind.SEMICOLON);
				endPos(level1sp);
				return new VarDeclStmt(vd1, e2, level1sp);
			}
			else {
				/* the followings are in the form 
				 Reference = Expression ;
				| Reference (ArgumentList?);*/
				SourcePosition qrefsp = new SourcePosition(idstartPosition);
				while(token.kind == TokenKind.DOT) {
					acceptIt();
					// No 'this' is allowed in the middle of the Reference
					if(token.kind == TokenKind.ID) {
						// I use QualRef here recursively 
						Token temp = token;
						acceptIt();
						endPos(qrefsp);
						rf1 = new QualRef(rf1, new Identifier(temp), qrefsp);
						
						
					}
					else parseError("Invalid Term - expecting ID after dot");
				}
				switch(token.kind) {
				
				// AssignmentStmt
				// done
				case ASSIGNMENT:
					SourcePosition asp = new SourcePosition(idstartPosition);
					acceptIt();
					Expression e2 = parseExpression();
					accept(TokenKind.SEMICOLON);
					endPos(asp);
					return new AssignStmt(rf1, e2, asp);

				// IxAssignmentStmt
				// done
				case LSQBRACK:
					SourcePosition iasp = new SourcePosition(idstartPosition);
					acceptIt();
					Expression e3 = parseExpression();
					accept(TokenKind.RSQBRACK);
					accept(TokenKind.ASSIGNMENT);
					Expression e4 = parseExpression();
					accept(TokenKind.SEMICOLON);
					endPos(iasp);
					return new IxAssignStmt(rf1, e3, e4, iasp);
					
				// CallStmt
				// basically done
				case LPAREN:
					SourcePosition csp = new SourcePosition(idstartPosition);
					acceptIt();
					ExprList el = new ExprList();
					if(token.kind != TokenKind.RPAREN) {
						el = parseArgumentList();
					}
					accept(TokenKind.RPAREN);
					accept(TokenKind.SEMICOLON);
					endPos(csp);
					return new CallStmt(rf1, el, csp);
					
				default:
					parseError("Invalid Term - expecting brackets or assignment after Reference but found " + token.kind);
					return null;
				}
			}
			
		// ReturnStmt
		// done
		case RETURN: 
			SourcePosition rsp = new SourcePosition();
			startPos(rsp);
			acceptIt();
			Expression e2 = null;
			if(token.kind != TokenKind.SEMICOLON) {
				e2 = parseExpression();
			}
			accept(TokenKind.SEMICOLON);
			endPos(rsp);
			return new ReturnStmt(e2, rsp);
			
		// IfStmt
		// done
		case IF:
			SourcePosition isp = new SourcePosition();
			startPos(isp);
			acceptIt();
			accept(TokenKind.LPAREN);
			Expression e3 = parseExpression();
			accept(TokenKind.RPAREN);
			Statement s1 = parseStatement();
			if(token.kind == TokenKind.ELSE) {
				acceptIt();
				Statement s2 = parseStatement();
				endPos(isp);
				return new IfStmt(e3, s1, s2, isp);
			}
			endPos(isp);
			return new IfStmt(e3, s1, isp);
			
		// WhileStmt
		// done
		case WHILE:
			SourcePosition wsp = new SourcePosition();
			startPos(wsp);
			acceptIt();
			accept(TokenKind.LPAREN);
			Expression e4 = parseExpression();
			accept(TokenKind.RPAREN);
			Statement s4 = parseStatement();
			endPos(wsp);
			return new WhileStmt(e4, s4, wsp);
			
		default:
			parseError("Invalid Term -  found " + token.kind);
			return null;
			
		}
		
	}
	
	/*
	 * Expression ::= Reference
						| Reference [ Expression ]
						| Reference(ArgumentList?)
						| unop Expression
						| Expression binop Expression
						| ( Expression )
						| num | true | false
						| new (id() | int[Expression] | id[Expression])
	 */
	public Expression parseBaseLevel() {
		SourcePosition esp = new SourcePosition();
		startPos(esp);
		Expression e1 = null;
		switch(token.kind) {
		
		/* Reference
		| Reference [ Expression ]
		| Reference(ArgumentList?)*/
		case THIS: case ID:
			Reference rf1 = parseReference();
			
			// IxExpr
			// Reference [ Expression ]
			// done
			if(token.kind == TokenKind.LSQBRACK) {
				
				acceptIt();
				Expression e2 = parseExpression();
				//Expression e2 = parseBaseLevel();
				accept(TokenKind.RSQBRACK);
				endPos(esp);
				e1 = new IxExpr(rf1, e2, esp);
			}
			
			// CallExpr
			// Reference(ArgumentList?
			// done
			else if(token.kind == TokenKind.LPAREN) {
				acceptIt();
				ExprList el = new ExprList();
				if(token.kind != TokenKind.RPAREN) {
					el = parseArgumentList();
				}
				accept(TokenKind.RPAREN);
				endPos(esp);
				e1 = new CallExpr(rf1, el, esp);
			}
			// RefExpr
			// Reference
			// done
			else {
				endPos(esp);
				e1 = new RefExpr(rf1, esp);
			}
			break; 
			
		// unop Expression	
		// done
		case UNOP: case ADDITIVEUNARY:
			Operator op = new Operator(token);
			acceptIt();
			Expression e2 = parseBaseLevel();
			// Expression e2 = parseBaseLevel();
			endPos(esp);
			e1 =  new UnaryExpr(op, e2, esp);
			break;
			
		// ( Expression )
		// done
		case LPAREN:
			acceptIt();
			e1 = parseExpression();
			//e1 = parseBaseLevel();
			accept(TokenKind.RPAREN);
			break;
				
		// num 
		// done
		case NUM: 
			Token t = token;
			acceptIt();
			endPos(esp);
			e1 = new LiteralExpr(new IntLiteral(t), esp);
			break;
		// true | false
		// done
		case TRUE: case FALSE:
			Token t1 = token;
			acceptIt();
			endPos(esp);
			e1 = new LiteralExpr(new BooleanLiteral(t1), esp);
			break;
			
		// null
		case NULL:
			Token t2 = token;
			acceptIt();
			endPos(esp);
			e1 = new LiteralExpr(new NullLiteral(t2), esp);
			break;
			
		// new (id() | int[Expression] | id[Expression])
		// done
		case NEW:
			acceptIt();
			switch (token.kind) {
			case ID:
				SourcePosition typesp = new SourcePosition();
				startPos(typesp);
				Identifier id1 = new Identifier(token);
				acceptIt();
				endPos(typesp);
				ClassType ct1 = new ClassType(id1, typesp);
				
				switch(token.kind) {
				// done
				case LPAREN:
					acceptIt();
					accept(TokenKind.RPAREN);
					endPos(esp);
					e1 = new NewObjectExpr(ct1, esp);
					break;
				// done
				case LSQBRACK:
					acceptIt();
					Expression e5 = parseExpression();
					// Expression e5 = parseBaseLevel();
					accept(TokenKind.RSQBRACK);
					endPos(esp);
					e1 = new NewArrayExpr(ct1, e5, esp);
					break;
					
				default: 
					parseError("Invalid Term - expecting '(' or '[' after ID but found" + token.kind);
					return null;
				}
				break;
			
			// int[Expression]
			// done
			case INT:
				SourcePosition basetypesp = new SourcePosition();
				startPos(basetypesp);
				acceptIt();
				endPos(basetypesp);
				BaseType bt1 = new BaseType(TypeKind.INT, basetypesp);
				// ArrayType at1 = new ArrayType(bt1, null);
				
				accept(TokenKind.LSQBRACK);
				Expression e6 = parseExpression();
				//	Expression e6 = parseBaseLevel();
				accept(TokenKind.RSQBRACK);
				endPos(esp);
				e1 = new NewArrayExpr(bt1, e6, esp);
				break;
				
			default: 
				parseError("Invalid Term - expecting ID or INT after inside new() but found" + token.kind);
				return null;
			}
			break;
			
		default: 
			parseError("Invalid Term - expecting Expression but found " + token.kind);	
			return null;
		}
		
		/*// Expression binop Expression
		if(token.kind == TokenKind.BINOP || token.kind == TokenKind.BUNOP) {
			acceptIt();
			parseExpression();
		}*/
		return e1;
	}
	
	/**
	 * We are marking the start and end of ast nodes by the token at there
	 * assign the start and end of ast nodes
	 * @param a SourcePosition instance of an ast node
	 */
	public void startPos(SourcePosition sp) {
		sp.start = token.posn.start;
		// which is same as token.posn.finish
	}
	
	public void endPos(SourcePosition sp) {
		sp.finish = prevPos.finish;
		// which is same as prevPos.start
	}
	
	
	public void acceptIt() {
		accept(token.kind);
	}
	
	public void accept(TokenKind expectedTokenKind) {
		if(token.kind == expectedTokenKind) {
			// update prevPos
			prevPos = token.posn;
			if(trace) {
				pTrace();
			}
			token = scanner.scan();
			// Skip comments
			while(token.kind == TokenKind.COMMENT) {
				acceptIt();
			}
		}
		else parseError("expecting '" + expectedTokenKind +
				"' but found '" + token.kind + "'");
		
		
	}
	/**
	 * report parse error and unwind call stack to start of parse
	 * @param e  string with error detail
	 * @throws SyntaxError
	 */
	private void parseError(String e) throws SyntaxError {
		reporter.reportError("Parse error: " + e);
		throw new SyntaxError();
	}
	
	
	private void pTrace() {
		StackTraceElement [] stl = Thread.currentThread().getStackTrace();
		for (int i = stl.length - 1; i > 0 ; i--) {
			if(stl[i].toString().contains("parse"))
				System.out.println(stl[i]);
		}
		System.out.println("accepting: " + token.kind + " (\"" + token.spelling + "\")");
		System.out.println();
	}
	
	
	// PA2
	// DisjunctionLevel
	public Expression parseExpression() {
		int startPosition = token.posn.start;
		
		Expression e1 = parseConjunctionLevel();
		while(token.kind == TokenKind.DISJUNCTION) {
			SourcePosition expsp = new SourcePosition(startPosition);
			Operator op = new Operator(token);
			acceptIt();
			Expression e2 = parseConjunctionLevel();
			endPos(expsp);
			e1 = new BinaryExpr(op, e1, e2, expsp);
			
		}
		return e1;
	}
	
	 public Expression parseConjunctionLevel() {
		 int startPosition = token.posn.start;
		 Expression e1 = parseEqualityLevel();
		 while(token.kind == TokenKind.CONJUNCTION) {
			 SourcePosition expsp = new SourcePosition(startPosition);
			 Operator op = new Operator(token);
			 acceptIt();
			 Expression e2 = parseEqualityLevel();
			 endPos(expsp);
			 e1 = new BinaryExpr(op, e1, e2, expsp);
		 }
		 return e1;
	 }
	 
	 public Expression parseEqualityLevel() {
		 int startPosition = token.posn.start;
		 Expression e1 = parseRelationalLevel();
		 while(token.kind == TokenKind.EQUALITY) {
			 SourcePosition expsp = new SourcePosition(startPosition);
			 Operator op = new Operator(token);
			 acceptIt();
			 Expression e2 = parseRelationalLevel();
			 endPos(expsp);
			 e1 = new BinaryExpr(op, e1, e2, expsp);
		 }
		 return e1;
	 }
	 
	 
	 public Expression parseRelationalLevel() {
		 int startPosition = token.posn.start;
		 Expression e1 = parseAdditiveLevel();
		 while(token.kind == TokenKind.RELATIONAL) {
			 SourcePosition expsp = new SourcePosition(startPosition);
			 Operator op = new Operator(token);
			 acceptIt();
			 Expression e2 = parseAdditiveLevel();
			 endPos(expsp);
			 e1 = new BinaryExpr(op, e1, e2, expsp);
		 }
		 return e1;
	 }
	 
	 public Expression parseAdditiveLevel() {
		 int startPosition = token.posn.start;
		 Expression e1 = parseMultiplicativeLevel();
		 while(token.kind == TokenKind.ADDITIVE || token.kind == TokenKind.ADDITIVEUNARY) {
			 SourcePosition expsp = new SourcePosition(startPosition);
			 Operator op = new Operator(token);
			 acceptIt();
			 Expression e2 = parseMultiplicativeLevel();
			 endPos(expsp);
			 e1 = new BinaryExpr(op, e1, e2, expsp);
		 }
		 return e1;
	 }
	 
	 public Expression parseMultiplicativeLevel() {
		 int startPosition = token.posn.start;
		 Expression e1 = parseBaseLevel();
		 while(token.kind == TokenKind.MULTIPLICATIVE) {
			 SourcePosition expsp = new SourcePosition(startPosition);
			 Operator op = new Operator(token);
			 acceptIt();
			 Expression e2 = parseBaseLevel();
			 endPos(expsp);
			 e1 = new BinaryExpr(op, e1, e2, expsp);
		 }
		 return e1;
	 }
	
	 /*public Expression parseUnaryLevel() {
		 boolean hasUnary = false;
		 Operator op = null;
		 if(token.kind == TokenKind.UNOP || token.kind == TokenKind.ADDITIVEUNARY) {
			 op = new Operator(token);
			 acceptIt();
			 hasUnary = true;
		 }
		 Expression e1 = parseBaseLevel();
		 if(hasUnary) {
			 e1 = new UnaryExpr(op, e1, null);
		 }
		 return e1;
	 }*/
		
	 
}
