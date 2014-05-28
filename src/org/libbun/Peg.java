package org.libbun;

public abstract class Peg {
	public final static boolean _BackTrack = true;
	String    name     = null;
	int       priority = 0;
	boolean   debug    = false;

	BunSource source = null;
	int       sourcePosition = 0;

	Peg(String leftLabel) {
		this.name = leftLabel;
	}

	protected abstract void stringfy(UniStringBuilder sb, boolean debugMode);
	protected abstract PegObject lazyMatch(PegObject inNode, PegParserContext source, boolean hasNextChoice);
	protected abstract boolean verify(PegParser parser);
	public abstract void accept(PegVisitor visitor);
	public abstract int size();
	public abstract Peg get(int index);

	@Override public String toString() {
		UniStringBuilder sb = new UniStringBuilder();
		this.stringfy(sb, false);
		return sb.toString();
	}

	public final PegSequence appendAsSequence(Peg e) {
		if(e instanceof PegSequence) {
			((PegSequence)e).list.add(0, this);
			return ((PegSequence)e);
		}
		if(this instanceof PegSequence) {
			((PegSequence)this).list.add(e);
			return ((PegSequence)this);
		}
		else {
			PegSequence seq = new PegSequence(this);
			seq.appendAsSequence(e);
			return seq;
		}
	}

	public final PegChoice appendAsChoice(Peg e) {
		if(this instanceof PegChoice) {
			((PegChoice)this).add(e);
			return ((PegChoice)this);
		}
		else {
			PegChoice choice = new PegChoice(this);
			choice.add(e);
			return choice;
		}
	}
	
	protected PegObject debugMatch(PegObject inNode, PegParserContext source, boolean hasNextChoice) {
		if(this.debug) {
			PegObject node2 = this.lazyMatch(inNode, source, false);
			String msg = "matched";
			if(node2.isErrorNode()) {
				msg = "failed";
			}
			String line = source.formatErrorMessage(msg, this.toString());
			System.out.println(line + "\n\tnode #" + inNode + "# => #" + node2 + "#");
			return node2;
		}
		return this.lazyMatch(inNode, source, hasNextChoice);
	}

	public final String toPrintableString(String name) {
		UniStringBuilder sb = new UniStringBuilder();
		sb.append(name);
		sb.append(" <- ");
		this.joinPrintableString(sb, this);
		return sb.toString();
	}

	private void joinPrintableString(UniStringBuilder sb, Peg e) {
		if(e instanceof PegChoice) {
			for(int i = 0; i < e.size(); i++) {
				if(i > 0) {
					sb.append("\n\t/ ");
				}
				e.get(i).stringfy(sb, true);
			}
		}
		else {
			e.stringfy(sb, true);
		}
	}

	void setSource(BunSource source, int sourcePosition) {
		this.source = source;
		this.sourcePosition = sourcePosition;
	}

	protected void dump(String msg) {
		if(this.source != null) {
			System.out.println(this.source.formatErrorMessage("*", this.sourcePosition, msg));
		}
		else {
			System.out.println("unknown source: " + msg);
		}
	}

	protected void warning(String msg) {
		if(Main.PegDebuggerMode) {
			Main._PrintLine("PEG warning: " + msg);
		}
	}

}

abstract class PegAtom extends Peg {
	String symbol;
	public PegAtom (String leftLabel, String symbol) {
		super(leftLabel);
		this.symbol = symbol;
	}
	@Override
	protected boolean verify(PegParser parser) {
		return true;
	}
	
	@Override
	public final int size() {
		return 0;
	}

	@Override
	public final Peg get(int index) {
		return this;  // just avoid NullPointerException
	}
}

class PegString extends PegAtom {
	public PegString(String leftLabel, String symbol) {
		super(leftLabel, symbol);
	}

	@Override
	protected void stringfy(UniStringBuilder sb, boolean debugMode) {
		char Quote = '\'';
		if(this.symbol.indexOf("'") != -1) {
			Quote = '"';
		}
		sb.append(UniCharset._QuoteString(Quote, this.symbol, Quote));
	}

	@Override
	public void accept(PegVisitor visitor) {
		visitor.visitPegString(this);
	}

	@Override
	public PegObject lazyMatch(PegObject inNode, PegParserContext source, boolean hasNextChoice) {
		if(source.match(this.symbol)) {
			return inNode;
		}
		return source.newExpectedErrorNode(this, this, hasNextChoice);
	}


}

class PegAny extends PegAtom {
	public PegAny(String leftLabel) {
		super(leftLabel, ".");
	}

	@Override
	protected void stringfy(UniStringBuilder sb, boolean debugMode) {
		sb.append(".");
	}

	@Override
	public void accept(PegVisitor visitor) {
		visitor.visitPegAny(this);
	}

	@Override
	public PegObject lazyMatch(PegObject inNode, PegParserContext source, boolean hasNextChoice) {
		if(source.hasChar()) {
			source.consume(1);
			return inNode;
		}
		return source.newExpectedErrorNode(this, this, hasNextChoice);
	}

}

class PegCharacter extends PegAtom {
	UniCharset charset;
	public PegCharacter(String leftLabel, String token) {
		super(leftLabel, token);
		this.charset = new UniCharset(token);
	}

	@Override
	protected void stringfy(UniStringBuilder sb, boolean debugMode) {
		sb.append("[" + this.symbol, "]");
	}

	@Override
	public void accept(PegVisitor visitor) {
		visitor.visitPegCharacter(this);
	}

	@Override
	public PegObject lazyMatch(PegObject inNode, PegParserContext source, boolean hasNextChoice) {
		char ch = source.getChar();
		if(!this.charset.match(ch)) {
			return source.newExpectedErrorNode(this, this, hasNextChoice);
		}
		source.consume(1);
		return inNode;
	}

}

class PegLabel extends PegAtom {
	public PegLabel(String leftLabel, String token) {
		super(leftLabel, token);
	}

	@Override
	protected void stringfy(UniStringBuilder sb, boolean debugMode) {
		sb.append(this.symbol);
	}

	@Override protected PegObject lazyMatch(PegObject parentNode, PegParserContext source, boolean hasNextChoice) {
		PegObject left = source.parsePegNode(parentNode, this.symbol, hasNextChoice);
		if(left.isErrorNode()) {
			return left;
		}
		return source.parseRightPegNode(left, this.symbol);
	}

	@Override
	public void accept(PegVisitor visitor) {
		visitor.visitPegLabel(this);
	}

	@Override
	protected boolean verify(PegParser parser) {
		if(!parser.hasPattern(this.symbol)) {
			System.out.println(this.source.formatErrorMessage("error", this.sourcePosition, "undefined label: " + this.symbol));
			return false;
		}
		return true;
	}
}

abstract class PegSuffixed extends Peg {
	Peg innerExpr;
	public PegSuffixed(String leftLabel, Peg e) {
		super(leftLabel);
		this.innerExpr = e;
	}

	@Override
	public final int size() {
		return 1;
	}

	@Override
	public final Peg get(int index) {
		return this.innerExpr;
	}

	@Override
	protected final void stringfy(UniStringBuilder sb, boolean debugMode) {
		if(this.innerExpr instanceof PegAtom) {
			this.innerExpr.stringfy(sb, debugMode);
		}
		else {
			sb.append("(");
			this.innerExpr.stringfy(sb, debugMode);
			sb.append(")");
		}
		sb.append(this.getOperator());
	}

	@Override
	protected boolean verify(PegParser parser) {
		return this.innerExpr.verify(parser);
	}

	protected abstract String getOperator();

}

class PegOptional extends PegSuffixed {
	public PegOptional(String leftLabel, Peg e) {
		super(leftLabel, e);
	}

	@Override
	protected String getOperator() {
		return "?";
	}

	@Override
	public void accept(PegVisitor visitor) {
		visitor.visitPegOptional(this);
	}

	@Override protected PegObject lazyMatch(PegObject parentNode, PegParserContext source, boolean hasNextChoice) {
		PegObject node = parentNode;
		int stackPosition = source.getStackPosition(this);
		node = this.innerExpr.debugMatch(node, source, true);
		if(node.isErrorNode()) {
			source.popBack(stackPosition, Peg._BackTrack);
			node = parentNode;
		}
		return node;
	}
}

class PegOneMore extends PegSuffixed {
	public PegOneMore(String leftLabel, Peg e) {
		super(leftLabel, e);
	}

	@Override
	protected String getOperator() {
		return "+";
	}

	@Override
	public PegObject lazyMatch(PegObject parentNode, PegParserContext source, boolean hasNextChoice) {
		PegObject prevNode = parentNode;
		int count = 0;
		while(source.hasChar()) {
			boolean aChoice = true;
			if(count < 1) {
				aChoice = hasNextChoice;
			}
			int startPosition = source.getPosition();
			PegObject node = this.innerExpr.debugMatch(prevNode, source, aChoice);
			if(node.isErrorNode()) {
				break;
			}
			if(node != prevNode) {
				this.warning("ignored result of " + this.innerExpr);
			}
			prevNode = node;
			if(!(startPosition < source.getPosition())) {
				this.warning("avoid infinite loop " + this);
				break;
			}
			count = count + 1;
		}
		if(count < 1) {
			return source.newExpectedErrorNode(this, this.innerExpr, hasNextChoice);
		}
		//System.out.println("prevNode: " + prevNode + "s,e=" + prevNode.sourcePosition + ", " + prevNode.endIndex);
		return prevNode;
	}

	@Override
	public void accept(PegVisitor visitor) {
		visitor.visitOneMore(this);
	}
}

class PegZeroMore extends PegSuffixed {
	public PegZeroMore(String leftLabel, Peg e) {
		super(leftLabel, e);
	}

	@Override
	protected String getOperator() {
		return "*";
	}

	@Override
	public PegObject lazyMatch(PegObject parentNode, PegParserContext source, boolean hasNextChoice) {
		PegObject prevNode = parentNode;
		int count = 0;
		while(source.hasChar()) {
			int startPosition = source.getPosition();
			PegObject node = this.innerExpr.debugMatch(prevNode, source, true);
			if(node.isErrorNode()) {
				break;
			}
			if(node != prevNode) {
				this.warning("ignored result of " + this.innerExpr);
			}
			prevNode = node;
			if(!(startPosition < source.getPosition())) {
				this.warning("avoid infinite loop " + this);
				break;
			}
			count = count + 1;
		}
		return prevNode;
	}

	@Override
	public void accept(PegVisitor visitor) {
		visitor.visitZeroMore(this);
	}
}

abstract class PegPredicate extends Peg {
	Peg innerExpr;
	public PegPredicate(String leftLabel, Peg e) {
		super(leftLabel);
		this.innerExpr = e;
	}

	@Override
	public final int size() {
		return 1;
	}

	@Override
	public final Peg get(int index) {
		return this.innerExpr;
	}

	@Override
	protected final void stringfy(UniStringBuilder sb, boolean debugMode) {
		sb.append(this.getOperator());
		if(this.innerExpr instanceof PegAtom) {
			this.innerExpr.stringfy(sb, debugMode);
		}
		else {
			sb.append("(");
			this.innerExpr.stringfy(sb, debugMode);
			sb.append(")");
		}
	}
	
	@Override
	protected boolean verify(PegParser parser) {
		return this.innerExpr.verify(parser);
	}
	
	protected abstract String getOperator();
}

class PegAndPredicate extends PegPredicate {
	PegAndPredicate(String leftLabel, Peg e) {
		super(leftLabel, e);
	}

	@Override
	protected String getOperator() {
		return "&";
	}

	@Override
	protected PegObject lazyMatch(PegObject parentNode, PegParserContext source, boolean hasNextChoice) {
		PegObject node = parentNode;
		int stackPosition = source.getStackPosition(this);
		node = this.innerExpr.debugMatch(node, source, true);
		source.popBack(stackPosition, Peg._BackTrack);
		return node;
	}

	@Override
	public void accept(PegVisitor visitor) {
		visitor.visitPegAnd(this);
	}

}

class PegNotPredicate extends PegPredicate {
	PegNotPredicate(String leftLabel, Peg e) {
		super(leftLabel, e);
	}

	@Override
	protected String getOperator() {
		return "!";
	}

	@Override
	protected PegObject lazyMatch(PegObject parentNode, PegParserContext source, boolean hasNextChoice) {
		PegObject node = parentNode;
		int stackPosition = source.getStackPosition(this);
		node = this.innerExpr.debugMatch(node, source, hasNextChoice);
		source.popBack(stackPosition, Peg._BackTrack);
		if(node.isErrorNode()) {
			return parentNode;
		}
		return source.newUnexpectedErrorNode(this, this.innerExpr, hasNextChoice);
	}

	@Override
	public void accept(PegVisitor visitor) {
		visitor.visitPegNot(this);
	}
}

abstract class PegList extends Peg {
	protected UniArray<Peg> list;
	PegList() {
		super(null);
		this.list = new UniArray<Peg>(new Peg[2]);
	}

	PegList(Peg first) {
		super(first.name);
		this.list = new UniArray<Peg>(new Peg[2]);
		this.add(first);
	}

	public final int size() {
		return this.list.size();
	}
	
	public final Peg get(int index) {
		return this.list.ArrayValues[index];
	}

	public void add(Peg e) {
		this.list.add(e);
	}
}

class PegSequence extends PegList {

	PegSequence(Peg first) {
		super(first);
	}

	@Override protected void stringfy(UniStringBuilder sb, boolean debugMode) {
		for(int i = 0; i < this.list.size(); i++) {
			if(i > 0) {
				sb.append(" ");
			}
			Peg e = this.list.ArrayValues[i];
			if(e instanceof PegChoice || e instanceof PegSequence) {
				sb.append("(");
				e.stringfy(sb, debugMode);
				sb.append(")");
			}
			else {
				e.stringfy(sb, debugMode);
			}
		}
	}

	@Override
	protected PegObject lazyMatch(PegObject inNode, PegParserContext source, boolean hasNextChoice) {
		for(int i = 0; i < this.list.size(); i++) {
			Peg e  = this.list.ArrayValues[i];
			inNode = e.debugMatch(inNode, source, hasNextChoice);
			if(inNode.isErrorNode()) {
				return inNode;
			}
		}
		return inNode;
	}
	
	@Override
	protected boolean verify(PegParser parser) {
		boolean noerror = true;
		for(int i = 0; i < this.list.size(); i++) {
			Peg e  = this.list.ArrayValues[i];
			if(!e.verify(parser)) {
				noerror = false;
			}
		}
		return noerror;
	}

	@Override
	public void accept(PegVisitor visitor) {
		visitor.visitSequence(this);
	}
	
	public Peg cdr() {
		PegSequence seq = new PegSequence(this.get(1)); 
		for(int i = 2; i < this.size(); i++) {
			Peg e  = this.list.ArrayValues[i];
			seq.list.add(e);
		}
		return seq;
	}
}

class PegChoice extends PegList {
	PegChoice() {
		super();
	}
	
	PegChoice(Peg first) {
		super(first);
	}

	public void add(Peg e) {
		if(e instanceof PegChoice) {
			for(int i = 0; i < e.size(); i++) {
				this.add(e.get(i));
			}
		}
		else if(e != null) {
			if(this.name == null) {
				this.name = e.name;
			}
			this.list.add(e);
		}
	}

	
	@Override protected void stringfy(UniStringBuilder sb, boolean debugMode) {
		for(int i = 0; i < this.size(); i++) {
			if(i > 0) {
				sb.append(" / ");
			}
			Peg e = this.list.ArrayValues[i];
			e.stringfy(sb, debugMode);
		}
	}

	@Override
	protected PegObject lazyMatch(PegObject inNode, PegParserContext source, boolean hasNextChoice) {
		int stackPosition = source.getStackPosition(this);
		PegObject node = inNode;
		for(int i = 0; i < this.size(); i++) {
			Peg e  = this.list.ArrayValues[i];
			boolean nextChoice = true;
			if(i + 1 == this.size()) {
				nextChoice = hasNextChoice;
			}
			node = e.debugMatch(inNode, source, nextChoice);
			if(!node.isErrorNode()) {
				return node;
			}
			source.popBack(stackPosition, Peg._BackTrack);
		}
		return node;
	}
	
	@Override
	protected boolean verify(PegParser parser) {
		boolean noerror = true;
		for(int i = 0; i < this.list.size(); i++) {
			Peg e  = this.list.ArrayValues[i];
			if(!e.verify(parser)) {
				noerror = false;
			}
		}
		return noerror;
	}

	@Override
	public void accept(PegVisitor visitor) {
		visitor.visitChoice(this);
	}
}

class PegSetter extends PegPredicate {
	boolean allowError = false;
	public PegSetter(String leftLabel, Peg e, boolean allowError) {
		super(leftLabel, e);
		this.innerExpr = e;
		this.allowError = allowError;
	}
	@Override
	protected String getOperator() {
		if(this.allowError) {
			return "$$";
		}
		else {
			return "$";
		}
	}

	@Override
	public void accept(PegVisitor visitor) {
		visitor.visitSetter(this);
	}

	@Override
	public PegObject lazyMatch(PegObject parentNode, PegParserContext source, boolean hasNextChoice) {
		if(this.allowError) {
			int pos = source.getPosition();
			PegObject node = this.innerExpr.debugMatch(parentNode, source, false);
			source.push(this, parentNode, -1, node);
			if(node.isErrorNode()) {
				source.showPosition("syntax error: " + node, pos);
				int indent = source.source.getIndentSize(pos);
				source.skipIndent(indent);
			}
			return parentNode;
		}
		PegObject node = this.innerExpr.debugMatch(parentNode, source, hasNextChoice);
		if(node.isErrorNode()) {
			return node;
		}
		if(parentNode == node) {
			//			this.warning("node was not created: nothing to set " + node + " ## created by " + this.innerExpr);
			return parentNode;
		}
		source.push(this, parentNode, -1, node);
		return parentNode;
	}
}

class PegObjectLabel extends Peg {
	String objectLabel = null;

	public PegObjectLabel(String leftLabel, String objectLabel) {
		super(leftLabel);
		this.objectLabel = objectLabel;
	}

	@Override
	public final int size() {
		return 0;
	}

	@Override
	public final Peg get(int index) {
		return this;
	}

	@Override
	protected final void stringfy(UniStringBuilder sb, boolean debugMode) {
		if(debugMode) {
			sb.append(this.objectLabel);
		}
	}

	@Override
	public PegObject lazyMatch(PegObject inNode, PegParserContext source, boolean hasNextChoice) {
		inNode.name = this.objectLabel;
		return inNode;
	}

	@Override
	protected boolean verify(PegParser parser) {
		parser.addObjectLabel(this.objectLabel);
		return true;
	}

	@Override
	public void accept(PegVisitor visitor) {
		//visitor.visitNewObject(this);
	}

}

class PegNewObject extends Peg {
	Peg innerExpr;
	boolean leftJoin = false;
	String nodeName = "";

	public PegNewObject(String leftLabel, boolean leftJoin, Peg e) {
		super(leftLabel);
		this.innerExpr = e;
		this.leftJoin = leftJoin;
	}
	
	@Override
	public final int size() {
		return 1;
	}

	@Override
	public final Peg get(int index) {
		return this.innerExpr;
	}


	@Override
	protected final void stringfy(UniStringBuilder sb, boolean debugMode) {
		if(debugMode) {
			sb.append("{");
			if(this.leftJoin) {
				sb.append("$ ");
			}
		}
		this.innerExpr.stringfy(sb, debugMode);
		if(debugMode) {
			sb.append("}");
		}
	}

	@Override
	public PegObject lazyMatch(PegObject inNode, PegParserContext source, boolean hasNextChoice) {
		// prefetch first node..
		int pos = source.getPosition();
		int stack = source.getStackPosition(this);
		PegObject newnode = source.newPegObject(this.nodeName);
		if(this.leftJoin) {
			source.push(this, newnode, 0, inNode);
		}
		PegObject node = this.innerExpr.debugMatch(newnode, source, false);
		if(node.isErrorNode()) {
			//System.out.println("disposing... object pos=" + pos + ", by " + this + "error="+node);
			//source.popBack(stack, Peg._BackTrack);
			return node;
		}
		int top = source.getStackPosition(this);
		for(int i = stack; i < top; i++) {
			Log log = source.logStack.ArrayValues[i];
			if(log.type == 'p' && log.parentNode == newnode) {
				newnode.append((PegObject)log.childNode);
			}
		}
		if(newnode.name == null || newnode.name.length() == 0) {
			newnode.name = source.source.substring(pos, source.getPosition());
		}
		newnode.setSource(this, source.source, pos, source.getPosition());
		return newnode;
	}

	@Override
	protected boolean verify(PegParser parser) {
		return this.innerExpr.verify(parser);
	}

	@Override
	public void accept(PegVisitor visitor) {
		visitor.visitNewObject(this);

	}


	//	private boolean containSetter(Peg e, boolean checkSequence) {
	//		if(e instanceof PegSetter) {
	//			return true;
	//		}
	//		if(e instanceof PegPredicate) {
	//			return this.containSetter(((PegPredicate) e).innerExpr, true);
	//		}
	//		if(e instanceof PegChoice) {
	//			if(this.containSetter(((PegChoice) e).firstExpr, true)) {
	//				return true;
	//			}
	//			if(this.containSetter(((PegChoice) e).secondExpr, true)) {
	//				return true;
	//			}
	//		}
	//		if(checkSequence && e.nextExpr != null) {
	//			return this.containSetter(e.nextExpr, true);
	//		}
	//		return false;
	//	}

}

//class PegSemanticAction extends Peg {
//	String name;
//	SemanticFunction f;
//	PegSemanticAction(String leftLabel, BunSource source, int sourcePosition, String name, SemanticFunction f) {
//		super(leftLabel, source);
//		this.name = name;
//		this.f = f;
//	}
//	@Override protected String stringfy() {
//		return "    :" + this.name;
//	}
//	@Override protected PegObject lazyMatch(PegObject parentNode, PegParserContext source, boolean hasNextChoice) {
//		parentNode.setSemanticAction(this.f);
//		return parentNode;
//	}
//	@Override boolean check(PegParser p, String leftName, int order) {
//		return true;
//	}
//}
