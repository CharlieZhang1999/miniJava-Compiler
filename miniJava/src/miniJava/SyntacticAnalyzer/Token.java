package miniJava.SyntacticAnalyzer;

import miniJava.SyntacticAnalyzer.TokenKind;

public class Token {
	public TokenKind kind;
	public String spelling;
	public SourcePosition posn;

	public Token(TokenKind kind, String spelling) {
		this.kind = kind;
		this.spelling = spelling;
	}
}
