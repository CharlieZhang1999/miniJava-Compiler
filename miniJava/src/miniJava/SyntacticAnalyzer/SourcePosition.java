package miniJava.SyntacticAnalyzer;

public class SourcePosition {
	public int start;
	public int finish;
	
	public SourcePosition(int start, int finish) {
		this.start = start;
		this.finish = finish;
	}
	public SourcePosition() {
		this.start = 0;
		this.finish = 0;
	}
	public SourcePosition(int start) {
		this.start = start;
	}
	public String toString() {
		return "*** line "+this.start+": ";
	}
}
