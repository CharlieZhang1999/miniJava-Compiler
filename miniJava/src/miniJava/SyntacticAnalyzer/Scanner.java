package miniJava.SyntacticAnalyzer;

import java.io.*;
import java.util.HashMap;

import miniJava.ErrorReporter;
import miniJava.SyntacticAnalyzer.Token;
import miniJava.SyntacticAnalyzer.TokenKind;

public class Scanner {
	
	private InputStream inputStream;
	private ErrorReporter reporter;
	private BufferedReader br;
	private HashMap kwdHashmap;
	private HashMap idHashmap;
	private HashMap punctHashmap;
	
	private char currentChar;
	private StringBuilder currentSpelling;
	private int currentLine;
	
	// true when end of line is found
	private boolean eot = false; 
	
	// some whitespace that should be ignored
	private static char tabs = '\t';
	private static char newlines = '\n';
	private static char returns = '\r';
	
	
	public Scanner(/*BufferedReader myReader*/InputStream inputStream, ErrorReporter reporter) {
		//this.br = myReader;
		this.reporter = reporter;
		this.inputStream = inputStream;
		this.reporter = reporter;
		
		// initialize keyword hashmap
		this.kwdHashmap = new HashMap<String, TokenKind>(){{
			put("class", TokenKind.CLASS);
			put("void", TokenKind.VOID);
			put("public", TokenKind.PUBLIC);
			put("private", TokenKind.PRIVATE);
			put("static", TokenKind.STATIC);
			put("int", TokenKind.INT);
			put("boolean", TokenKind.BOOLEAN);
			put("this", TokenKind.THIS);
			put("return", TokenKind.RETURN);
			put("if", TokenKind.IF);
			put("else", TokenKind.ELSE);
			put("while", TokenKind.WHILE);
			put("true", TokenKind.TRUE);
			put("false", TokenKind.FALSE);
			put("new", TokenKind.NEW);
			
			// null literal
			put("null", TokenKind.NULL);
		}};
		
		this.punctHashmap = new HashMap<String, TokenKind>(){{
			put(",", TokenKind.COMMA);
			put(".", TokenKind.DOT);
			put(";", TokenKind.SEMICOLON);
		}};
		
		this.currentLine = 1;
		// initialize scanner state
		readChar();
	}
	
	public Token scan() {

		// skip whitespace
		while (!eot && (currentChar == ' '|| currentChar == '\t' || currentChar == '\n' || currentChar == '\r'))
			skipIt();

		// start of a token: collect spelling and identify token kind
		currentSpelling = new StringBuilder();
		TokenKind kind = scanToken();
		String spelling = currentSpelling.toString();
		
		// initialize the token's start position and end position with current line number
		SourcePosition sp = new SourcePosition(currentLine, currentLine);
		
		// return new token
		return new Token(kind, spelling, sp);
	}
	
	/**
	 * determine token kind
	 */
	public TokenKind scanToken() {
		if (eot)
			return(TokenKind.EOT); 

		// scan Token
		switch (currentChar) {
		
		// Group 1: operators
		case '+': 
			takeIt();
			return(TokenKind.ADDITIVE);
		case '*': 
			takeIt();
			return(TokenKind.MULTIPLICATIVE);
			
		// since '-' is BINOP and also UNOP, we make a new cateogory called BUNOP for '-'
		case '-': 
			takeIt();
			return(TokenKind.ADDITIVEUNARY);
			
		case'/':
			takeIt();
			// case '//'
			if(currentChar == '/' ) {
				while(currentChar != '\n' && !eot) {
					takeIt();
				}
				// currentChar is 'eot'
				if(eot) {
					return(TokenKind.COMMENT);
				}
				else {
					// currentChar is '\n'
					takeIt();
					return(TokenKind.COMMENT);
				}
			}
			// case '/*'
			else if(currentChar == '*') {
				takeIt();
				while(true) {
					while(currentChar != '*') {
						if(!eot) {
							takeIt();
						}
						// edge case: the comment starts with /* but is unterminated 
						else {
							scanError("The comment is unterminated");
							return(TokenKind.ERROR);
						}
					}
					takeIt();
					if(currentChar == '/') {
						takeIt();
						break;
					}
				}
				return(TokenKind.COMMENT);
			}
			// case '/' (divider)
			else{
				//return(TokenKind.BINOP);
				return(TokenKind.MULTIPLICATIVE);
			}
			
			
		case '=':
			takeIt();
			if(currentChar != '=') {
				return(TokenKind.ASSIGNMENT);
			}
			else{
				takeIt();
				//return(TokenKind.BINOP);
				return(TokenKind.EQUALITY);
			}
			
		case '<':
			takeIt();
			if(currentChar == '=') {
				takeIt();
				//return(TokenKind.BINOP);
				return(TokenKind.RELATIONAL);
			}
			//else return(TokenKind.BINOP);
			else return(TokenKind.RELATIONAL);
			
		case '>':
			takeIt();
			if(currentChar == '=') {
				takeIt();
				//return(TokenKind.BINOP);
				return(TokenKind.RELATIONAL);
			}
			//else return(TokenKind.BINOP);
			else return(TokenKind.RELATIONAL);
			
		case '&':
			takeIt();
			if(currentChar != '&') {
				return(TokenKind.ERROR);
			}
			else{
				takeIt();
				//return(TokenKind.BINOP);
				return(TokenKind.CONJUNCTION);
			}

		case '|':
			takeIt();
			if(currentChar != '|') {
				return(TokenKind.ERROR);
			}
			else{
				takeIt();
				//return(TokenKind.BINOP);
				return(TokenKind.DISJUNCTION);
			}
			
		case '!':
			takeIt();
			if(currentChar == '=') {
				takeIt();
				//return(TokenKind.BINOP);
				return(TokenKind.EQUALITY);
			}
			else return(TokenKind.UNOP);
			
			
		case '(': 
			takeIt();
			return(TokenKind.LPAREN);

		case ')':
			takeIt();
			return(TokenKind.RPAREN);
		
		case '{':
			takeIt();
			return(TokenKind.LCURBRACK);
			
		case '}':
			takeIt();
			return(TokenKind.RCURBRACK);
			
		case '[':
			takeIt();
			return(TokenKind.LSQBRACK);
			
		case ']':
			takeIt();
			return(TokenKind.RSQBRACK);
			
		// Group 2: NUM
		case '0': case '1': case '2': case '3': case '4':
		case '5': case '6': case '7': case '8': case '9':
			while (isDigit(currentChar))
				takeIt();
			return(TokenKind.NUM);
		
		
		case 'a': case 'b': case 'c': case 'd': case 'e':
		case 'f': case 'g': case 'h': case 'i': case 'j':
		case 'k': case 'l': case 'm': case 'n': case 'o':
		case 'p': case 'q': case 'r': case 's': case 't':
		case 'u': case 'v': case 'w': case 'x': case 'y': 
		case 'z':
		case 'A': case 'B': case 'C': case 'D': case 'E':
		case 'F': case 'G': case 'H': case 'I': case 'J':
		case 'K': case 'L': case 'M': case 'N': case 'O':
		case 'P': case 'Q': case 'R': case 'S': case 'T':
		case 'U': case 'V': case 'W': case 'X': case 'Y': 
		case 'Z':
			while(isLetterOrDigit(currentChar) | currentChar == '_') {
				takeIt(); 
			}
			// Group 3: KEYWORD
			if(this.kwdHashmap.containsKey(currentSpelling.toString())) {
				// if(currentSpelling)
				return (TokenKind) this.kwdHashmap.get(currentSpelling.toString());
					
			}// GROUP 4: IDENTIFIER
			else return(TokenKind.ID);
		
		// GROUP 5: PUNCTUATION
		case ',': case ';': case '.':
			takeIt();
			// not sure whether to get currentChar or get currentSpelling
			return (TokenKind)this.punctHashmap.get(currentSpelling.toString());
			
			
		
		default:
			scanError("Unrecognized character '" + currentChar + "' in input");
			return(TokenKind.ERROR);
		}
	}
	
	private void skipIt() {
		nextChar();
	}
	
	private void skipLine() {
		
	}
	
	private void takeIt() {
		currentSpelling.append(currentChar);
		nextChar();
	}
	
	
	private boolean isDigit(char c) {
		return (c >= '0') && (c <= '9');
	}
	
	/* @ check if the character is a letter or a digit */
	private boolean isLetterOrDigit(char c) {
		return (c >= 'a' && c <= 'z') ||
		           (c >= 'A' && c <= 'Z') ||
		           (c >= '0' && c <= '9');
	}
	
	/*private boolean isPunctuation(char c) {
		return (c == ',') || (c == ';') || (c == '.');
	}*/
	
	private void scanError(String m) {
		reporter.reportError("Scan Error:  " + m);
	}
	
	
	
	
	private void nextChar() {
		if(!eot) {
			readChar();
		}
	}
	
	private void readChar() {
		try {
			int c = inputStream.read();
			// int c = br.read();
			currentChar = (char) c;
			if (c == -1) {
				eot = true;
			}
			if(c == newlines) {
				currentLine = currentLine + 1;
			}
		} catch (IOException e) {
			scanError("I/O Exception!");
			eot = true;
		}
	}
	

}
