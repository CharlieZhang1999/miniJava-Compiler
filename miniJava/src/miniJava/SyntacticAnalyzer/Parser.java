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
		ClassDeclList cdl = new ClassDeclList();
		while(token.kind == TokenKind.CLASS) {
			ClassDecl cd  = parseClassDeclaration();
			cdl.add(cd);
		}
		accept(TokenKind.EOT);
		pkg = new Package(cdl, null);
		return pkg;
	}
	
	// ClassDeclaration ::= class id { ( FieldDeclaration | MethodDeclaration )* } 
	public ClassDecl parseClassDeclaration() throws SyntaxError {
		ClassDecl cd = null;
		
		FieldDeclList fdl = new FieldDeclList();
		MethodDeclList mtdl = new MethodDeclList(); 
		
		
		accept(TokenKind.CLASS);
		
		//classname
		String cn =  token.spelling;
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
				md = new MethodDecl(fd, pdl, sl, null);
				mtdl.add(md);
				
			}
			else if(token.kind == TokenKind.INT || token.kind == TokenKind.BOOLEAN || token.kind == TokenKind.ID) {
				td = parseType();
				
				name = token.spelling;
				accept(TokenKind.ID);
				
				// Field Declaration
				// done
				if(token.kind == TokenKind.SEMICOLON) {
					acceptIt();
					
					// add FieldDecl to FieldDeclList
					fd = new FieldDecl(isPrivate, isStatic, td, name, null);
					fdl.add(fd);
				}
				
				// Method Declaration
				else if(token.kind == TokenKind.LPAREN) {
					accept(TokenKind.LPAREN);
					
					fd = new FieldDecl(isPrivate, isStatic, td, name, null);
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
					md = new MethodDecl(fd, pdl, sl, null); 
					mtdl.add(md);
				}
				else parseError("Neither field declaration nor method declaration");
			}
			else parseError("Neither field declaration nor method declaration");
		}
		accept(TokenKind.RCURBRACK);
		cd  =  new ClassDecl(cn, fdl, mtdl, null);
		return cd;
	}
	
	public TypeDenoter parseType() throws SyntaxError {
		TypeDenoter td = null;
		switch (token.kind) {
		
		case INT:
			acceptIt();
			if(token.kind == TokenKind.LSQBRACK) {
				acceptIt();
				accept(TokenKind.RSQBRACK);
				TypeDenoter bt = new BaseType(TypeKind.INT, null);
				td =  new ArrayType(bt, null);
			}
			else {
				td = new BaseType(TypeKind.INT, null);
			}
			break;
			
		case BOOLEAN:
			acceptIt();
			td = new BaseType(TypeKind.BOOLEAN, null);
			break;
			
		case ID:
			Identifier id = new Identifier(token);
			
			acceptIt();
			if(token.kind == TokenKind.LSQBRACK) {
				acceptIt();
				accept(TokenKind.RSQBRACK);
				ClassType ct = new ClassType(id, null);
				td =  new ArrayType(ct, null);
			}
			else {
				td = new ClassType(id, null);
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
		
		TypeDenoter td1 = parseType();
		ParameterDecl pd1 = new ParameterDecl(td1, token.spelling, null);
		accept(TokenKind.ID);
		pdl.add(pd1);
		
		while(token.kind == TokenKind.COMMA) {
			acceptIt();
			TypeDenoter td2 = parseType();
			ParameterDecl pd2 = new ParameterDecl(td2, token.spelling, null);
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
		if(token.kind == TokenKind.ID) {
			Identifier id = new Identifier(token);
			rf = new IdRef(id, null);
			acceptIt();
		}
		else if(token.kind == TokenKind.THIS) {
			rf = new ThisRef(null);
			acceptIt();
		}
		else parseError("Invalid Term - expecting Reference but found" + token.kind);
			
		while(token.kind == TokenKind.DOT) {
			acceptIt();
			Identifier id2 = new Identifier(token);
			rf = new QualRef(rf, id2, null);
			accept(TokenKind.ID);
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
			acceptIt();
			// {Statement *}
			StatementList sl = new StatementList();
			while(token.kind != TokenKind.RCURBRACK) {
				Statement s = parseStatement();
				sl.add(s);
			}
			accept(TokenKind.RCURBRACK);
			return new BlockStmt(sl, null);
		
		// VarDeclStmt
		// done
		case INT: case BOOLEAN: 
			TypeDenoter type = parseType();
			VarDecl vd = new VarDecl(type, token.spelling, null);
			accept(TokenKind.ID);
			
			accept(TokenKind.ASSIGNMENT);
			Expression e1 = parseExpression();
			accept(TokenKind.SEMICOLON);
			return new VarDeclStmt(vd, e1, null);
			
		case THIS:
			Reference rf = parseReference();
			switch(token.kind) {
			
			// AssignStmt
			// done
			case ASSIGNMENT:
				acceptIt();
				Expression e2 = parseExpression();
				accept(TokenKind.SEMICOLON);
				return new AssignStmt(rf, e2, null);
				
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
				return new IxAssignStmt(rf, e3, e4, null);
			
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
				return new CallStmt(rf, el, null);
				
			default:
				parseError("Invalid Term - expecting brackets or assignment after Reference but found " + token.kind);
				return null;
			}
			
		case ID:
			Identifier id1 = new Identifier(token);
			Reference rf1 = new IdRef(id1, null);
			acceptIt();
			if(token.kind == TokenKind.LSQBRACK) {
				acceptIt();
				
				// VarDeclStmt
				// ID[] id = Expression
				// done
				if(token.kind == TokenKind.RSQBRACK) {
					acceptIt();
					TypeDenoter td1 = new ClassType(id1, null);
					ArrayType at1 = new ArrayType(td1, null);
					VarDecl vd1 = new VarDecl(at1, token.spelling, null);
					accept(TokenKind.ID);
					accept(TokenKind.ASSIGNMENT);
					Expression e2 = parseExpression();
					accept(TokenKind.SEMICOLON);
					return new VarDeclStmt(vd1, e2, null);
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
					return new IxAssignStmt(rf1, e2, e3, null);
				}
			}
			
			// VarDeclStmt 
			// ID ID = Expression
			// done
			else if(token.kind == TokenKind.ID) {
				TypeDenoter td1 = new ClassType(id1, null);
				VarDecl vd1 = new VarDecl(td1, token.spelling, null);
				accept(TokenKind.ID);
				accept(TokenKind.ASSIGNMENT);
				Expression e2 = parseExpression();
				accept(TokenKind.SEMICOLON);
				return new VarDeclStmt(vd1, e2, null);
			}
			else {
				/* the followings are in the form 
				 Reference = Expression ;
				| Reference (ArgumentList?);*/
				
				while(token.kind == TokenKind.DOT) {
					acceptIt();
					// No 'this' is allowed in the middle of the Reference
					if(token.kind == TokenKind.ID) {
						// I use QualRef here recursively 
						rf1 = new QualRef(rf1, new Identifier(token), null);
						acceptIt();
						
					}
					else parseError("Invalid Term - expecting ID after dot");
				}
				switch(token.kind) {
				
				// AssignmentStmt
				// done
				case ASSIGNMENT:
					acceptIt();
					Expression e2 = parseExpression();
					accept(TokenKind.SEMICOLON);
					return new AssignStmt(rf1, e2, null);

				// IxAssignmentStmt
				// done
				case LSQBRACK:
					acceptIt();
					Expression e3 = parseExpression();
					accept(TokenKind.RSQBRACK);
					accept(TokenKind.ASSIGNMENT);
					Expression e4 = parseExpression();
					accept(TokenKind.SEMICOLON);
					return new IxAssignStmt(rf1, e3, e4, null);
					
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
					return new CallStmt(rf1, el, null);
					
				default:
					parseError("Invalid Term - expecting brackets or assignment after Reference but found " + token.kind);
					return null;
				}
			}
			
		// ReturnStmt
		// done
		case RETURN: 
			acceptIt();
			Expression e2 = null;
			if(token.kind != TokenKind.SEMICOLON) {
				e2 = parseExpression();
			}
			accept(TokenKind.SEMICOLON);
			return new ReturnStmt(e2, null);
			
		// IfStmt
		// done
		case IF:
			acceptIt();
			accept(TokenKind.LPAREN);
			Expression e3 = parseExpression();
			accept(TokenKind.RPAREN);
			Statement s1 = parseStatement();
			if(token.kind == TokenKind.ELSE) {
				acceptIt();
				Statement s2 = parseStatement();
				return new IfStmt(e3, s1, s2, null);
			}
			return new IfStmt(e3, s1, null);
			
		// WhileStmt
		// done
		case WHILE:
			acceptIt();
			accept(TokenKind.LPAREN);
			Expression e4 = parseExpression();
			accept(TokenKind.RPAREN);
			Statement s4 = parseStatement();
			return new WhileStmt(e4, s4, null);
			
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
				accept(TokenKind.RSQBRACK);
				e1 = new IxExpr(rf1, e2, null);
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
				e1 = new CallExpr(rf1, el, null);
			}
			// RefExpr
			// Reference
			// done
			else {
				e1 = new RefExpr(rf1, null);
			}
			break; 
			
		// unop Expression	
		// done
		case UNOP: case ADDITIVEUNARY:
			Operator op = new Operator(token);
			acceptIt();
			Expression e2 = parseExpression();
			e1 =  new UnaryExpr(op, e2, null);
			break;
			
		// ( Expression )
		// done
		case LPAREN:
			acceptIt();
			e1 = parseExpression();
			accept(TokenKind.RPAREN);
			break;
				
		// num 
		// done
		case NUM: 
			e1 = new LiteralExpr(new IntLiteral(token), null);
			acceptIt();
			break;
		// true | false
		// done
		case TRUE: case FALSE:
			e1 = new LiteralExpr(new BooleanLiteral(token), null);
			acceptIt();
			break;
			
		// new (id() | int[Expression] | id[Expression])
		// done
		case NEW:
			acceptIt();
			switch (token.kind) {
			case ID:
				Identifier id1 = new Identifier(token);
				ClassType ct1 = new ClassType(id1, null);
				acceptIt();
				switch(token.kind) {
				// done
				case LPAREN:
					acceptIt();
					accept(TokenKind.RPAREN);
					e1 = new NewObjectExpr(ct1, null);
					break;
				// done
				case LSQBRACK:
					acceptIt();
					Expression e5 = parseExpression();
					accept(TokenKind.RSQBRACK);
					e1 = new NewArrayExpr(ct1, e5, null);
					break;
					
				default: 
					parseError("Invalid Term - expecting '(' or '[' after ID but found" + token.kind);
					return null;
				}
				break;
			
			// int[Expression]
			// done
			case INT:
				BaseType bt1 = new BaseType(TypeKind.INT, null);
				ArrayType at1 = new ArrayType(bt1, null);
				acceptIt();
				accept(TokenKind.LSQBRACK);
				Expression e6 = parseExpression();
				accept(TokenKind.RSQBRACK);
				e1 = new NewArrayExpr(at1, e6, null);
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
	
	
	
	public void acceptIt() {
		accept(token.kind);
	}
	
	public void accept(TokenKind expectedTokenKind) {
		if(token.kind == expectedTokenKind) {
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
		Expression e1 = parseConjunctionLevel();
		while(token.kind == TokenKind.DISJUNCTION) {
			Operator op = new Operator(token);
			acceptIt();
			Expression e2 = parseConjunctionLevel();
			e1 = new BinaryExpr(op, e1, e2, null);
			
		}
		return e1;
	}
	
	 public Expression parseConjunctionLevel() {
		 Expression e1 = parseEqualityLevel();
		 while(token.kind == TokenKind.CONJUNCTION) {
			 Operator op = new Operator(token);
			 acceptIt();
			 Expression e2 = parseEqualityLevel();
			 e1 = new BinaryExpr(op, e1, e2, null);
		 }
		 return e1;
	 }
	 
	 public Expression parseEqualityLevel() {
		 Expression e1 = parseRelationalLevel();
		 while(token.kind == TokenKind.EQUALITY) {
			 Operator op = new Operator(token);
			 acceptIt();
			 Expression e2 = parseRelationalLevel();
			 e1 = new BinaryExpr(op, e1, e2, null);
		 }
		 return e1;
	 }
	 
	 
	 public Expression parseRelationalLevel() {
		 Expression e1 = parseAdditiveLevel();
		 while(token.kind == TokenKind.RELATIONAL) {
			 Operator op = new Operator(token);
			 acceptIt();
			 Expression e2 = parseAdditiveLevel();
			 e1 = new BinaryExpr(op, e1, e2, null);
		 }
		 return e1;
	 }
	 
	 public Expression parseAdditiveLevel() {
		 Expression e1 = parseMultiplicativeLevel();
		 while(token.kind == TokenKind.ADDITIVE || token.kind == TokenKind.ADDITIVEUNARY) {
			 Operator op = new Operator(token);
			 acceptIt();
			 Expression e2 = parseMultiplicativeLevel();
			 e1 = new BinaryExpr(op, e1, e2, null);
		 }
		 return e1;
	 }
	 
	 public Expression parseMultiplicativeLevel() {
		 Expression e1 = parseUnaryLevel();
		 while(token.kind == TokenKind.MULTIPLICATIVE) {
			 Operator op = new Operator(token);
			 acceptIt();
			 Expression e2 = parseUnaryLevel();
			 e1 = new BinaryExpr(op, e1, e2, null);
		 }
		 return e1;
	 }
	
	 public Expression parseUnaryLevel() {
		 boolean hasUnary = false;
		 Operator op = null;
		 if(token.kind == TokenKind.UNARY) {
			 op = new Operator(token);
			 acceptIt();
			 hasUnary = true;
		 }
		 Expression e1 = parseBaseLevel();
		 if(hasUnary) {
			 e1 = new UnaryExpr(op, e1, null);
		 }
		 return e1;
	 }
		
	 
}
