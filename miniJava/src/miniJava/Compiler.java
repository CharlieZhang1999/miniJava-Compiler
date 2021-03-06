package miniJava;
import java.io.File;

import java.io.FileInputStream;  
import java.io.FileNotFoundException;
import java.io.InputStream;

import miniJava.ErrorReporter;
import miniJava.SyntacticAnalyzer.Parser;
import miniJava.SyntacticAnalyzer.Scanner;

import miniJava.AbstractSyntaxTrees.*;
public class Compiler {	
	private static char currentChar;
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		InputStream inputStream = null;
		try {
			inputStream = new FileInputStream(args[0]);
		}catch(FileNotFoundException e) {
			System.out.println("Input file" + args[0] + "not found");
			// Can't open file, return 1
			System.exit(1);
		}
		ErrorReporter reporter = new ErrorReporter();
		Scanner scanner = new Scanner(inputStream, reporter);
		Parser parser = new Parser(scanner, reporter);
		
		System.out.println("Syntactic analysis ... ");
		AST ast = parser.parse();
		if(reporter.hasErrors()) {
			System.out.println("Syntactic Analysis Failed! Invalid program.");
			// Error parsing the file, return 4
			System.exit(4);
		}

		System.out.println("Syntactic Analysis Succeeded! Valid program.");
		ASTDisplay td = new ASTDisplay();
		td.showTree(ast);
		// Successfully parsing the file, return 0
		System.exit(0);

		
	}
}
