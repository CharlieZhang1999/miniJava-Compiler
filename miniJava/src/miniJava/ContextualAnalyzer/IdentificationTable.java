package miniJava.ContextualAnalyzer;
import miniJava.ErrorReporter;
import miniJava.AbstractSyntaxTrees.*;
import java.util.*;
public class IdentificationTable {
	
	private Stack<HashMap<String, Declaration>> idStack;
	private ErrorReporter reporter;
	//private ArrayList<ClassDecl> arr;
	//private HashMap<String, Declaration> memberTable;
	
	public IdentificationTable(ErrorReporter reporter) {
		// TODO Auto-generated constructor stub
		idStack = new Stack<>();
		this.reporter = reporter;
	}
	
	public void enter(Declaration d) {
		HashMap<String, Declaration> idTable = idStack.peek();
		if(d.name == null) {
			reporter.reportError("Declaration's name is not found");
		}
		idTable.put(d.name, d);
		
	}
	public void enter(String name, Declaration d) {
		HashMap<String, Declaration> idTable = idStack.peek();
		idTable.put(name, d);
	}
	
	public boolean nameExistsCurrentScope(String name) {
		HashMap<String, Declaration> idTable = idStack.peek();
		if(idTable.containsKey(name)) {
			return true;
		}
		return false;
	}
	
	public boolean nameExistsLocalScope(String name) {
		int index = idStack.size() - 1;
		while(index >= 2) {
			if(idStack.get(index).containsKey(name)) {
				return true;
			}
			index--;
		}
		return false;
	}
	
	
	public Declaration retrieve(String s) {
		int index = idStack.size() - 1;
		// which level should we trace back to 
		while(index >= 0) {
			if(idStack.get(index).containsKey(s)) {
				return idStack.get(index).get(s);
			}
			index--;
		}
		return null;
	}
	
	public void openScope() {
		HashMap<String, Declaration> idTable = new HashMap<>();
		idStack.push(idTable);
	}
	public void closeScope() {
		idStack.pop();
	}
}
