package miniJava;
import java.io.File;



import java.io.FileInputStream;  
import java.io.FileNotFoundException;
import java.io.InputStream;

import mJAM.Disassembler;
import mJAM.Interpreter;
import mJAM.ObjectFile;
import miniJava.ErrorReporter;
import miniJava.SyntacticAnalyzer.Parser;
import miniJava.SyntacticAnalyzer.Scanner;
import miniJava.*;

import miniJava.AbstractSyntaxTrees.*;
import miniJava.ContextualAnalyzer.*;
import miniJava.AbstractSyntaxTrees.Package;
import miniJava.CodeGenerator.*;
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
		
		
		System.out.println("Contextual analysis starts... ");
		Identification id = new Identification((Package)ast, reporter);
		IdentificationTable table = id.getTable();
		new TypeChecking((Package) ast, reporter, table);
		if(reporter.hasErrors()) {
			System.out.println("Contextual Analysis Failed! Invalid program.");
			// Error parsing the file, return 4
			System.exit(4);
		}
		System.out.println("Contextual Analysis Succeeded! Valid program.");
		
		System.out.println("Code Generation starts... ");
		CodeGenerator cg = new CodeGenerator((Package)ast, reporter);
		System.out.println("Code Generation Succeeded! Valid program.");
		
		// ASTDisplay td = new ASTDisplay();
		// td.showTree(ast);
		/*
		 * write code to object code file (.mJAM)
		 */
		
		String objectCodeFileName = args[0].substring(0, args[0].lastIndexOf("."));
		ObjectFile objF = new ObjectFile(objectCodeFileName+".mJAM");
		System.out.print("Writing object code file " + objectCodeFileName  + " ... ");
		if (objF.write()) {
			System.out.println("FAILED!");
		}
		else
			System.out.println("SUCCEEDED");	
		
		
		/*
		 * write code to object code file (.mJAM)
		 */
		/*
		String objectCodeFileName = "Counter.mJAM";
		ObjectFile objF = new ObjectFile(objectCodeFileName);
		System.out.print("Writing object code file " + objectCodeFileName + " ... ");
		if (objF.write()) {
			System.out.println("FAILED!");
			return;
		}
		else
			System.out.println("SUCCEEDED");	
		
		 // create asm file corresponding to object code using disassembler 
        String asmCodeFileName = objectCodeFileName.replace(".mJAM",".asm");
        System.out.print("Writing assembly file " + asmCodeFileName + " ... ");
        Disassembler d = new Disassembler(objectCodeFileName);
        if (d.disassemble()) {
                System.out.println("FAILED!");
                return;
        }
        else
                System.out.println("SUCCEEDED");
        
        /* 
         * run code using debugger
         * 
         */
		/*
        System.out.println("Running code in debugger ... ");
        Interpreter.debug(objectCodeFileName, asmCodeFileName);

        System.out.println("*** mJAM execution completed");*/
		
	}
}
