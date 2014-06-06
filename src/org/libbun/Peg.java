package org.libbun;

public abstract class Peg {
	public final static boolean _BackTrack = true;
	
	int       flag     = 0;
	String    name     = null;
	boolean   debug    = false;

	BunSource source = null;
	int       sourcePosition = 0;

	Peg(String leftLabel) {
		this.name = leftLabel;
	}

	protected abstract Peg clone(String ns);
	protected abstract void stringfy(UniStringBuilder sb, boolean debugMode);
	protected abstract void makeList(PegParser parser, UniArray<String> list, UniMap<String> set);
	protected abstract PegObject lazyMatch(PegObject inNode, PegParserContext source);
	protected abstract boolean verify(PegParser parser);
	public abstract void accept(PegVisitor visitor);

	public int size() {
		return 0;
	}
	public Peg get(int index) {
		return this;  // to avoid NullPointerException
	}

//	protected boolean hasPossibleSetter() {
//		return false;
//	}

	@Override public String toString() {
		UniStringBuilder sb = new UniStringBuilder();
		this.stringfy(sb, false);
		if(this.name != null) {
			sb.append(" defined in ");
			sb.append(this.name);
		}
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
	
	protected PegObject debugMatch(PegObject inNode, PegParserContext source) {
		if(this.debug) {
			PegObject node2 = this.lazyMatch(inNode, source);
			String msg = "matched";
			if(node2.isFailure()) {
				msg = "failed";
			}
			String line = source.formatErrorMessage(msg, this.toString());
			System.out.println(line + "\n\tnode #" + inNode + "# => #" + node2 + "#");
			return node2;
		}
		return this.lazyMatch(inNode, source);
	}

	public final String toPrintableString(String name, String Setter, String Choice, String SemiColon, boolean debugMode) {
		UniStringBuilder sb = new UniStringBuilder();
		sb.append(name);
		sb.append(Setter);
		if(this instanceof PegChoice) {
			for(int i = 0; i < this.size(); i++) {
				if(i > 0) {
					sb.append(Choice);
				}
				this.get(i).stringfy(sb, debugMode);
			}
		}
		else {
			this.stringfy(sb, debugMode);
		}
		sb.append(SemiColon);
		return sb.toString();
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
	protected void stringfy(UniStringBuilder sb, boolean debugMode) {
		sb.append(this.symbol);
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
	
	@Override
	protected void makeList(PegParser parser, UniArray<String> list, UniMap<String> set) {
		// TODO Auto-generated method stub
	}

}

class PegString extends PegAtom {
	public PegString(String leftLabel, String symbol) {
		super(leftLabel, symbol);
	}

	@Override
	protected Peg clone(String ns) {
		return this;
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
	public PegObject lazyMatch(PegObject inNode, PegParserContext source) {
		if(source.match(this.symbol)) {
			return inNode;
		}
		return source.foundFailure(this);
	}

}

class PegAny extends PegAtom {
	public PegAny(String leftLabel) {
		super(leftLabel, ".");
	}

	@Override
	protected Peg clone(String ns) {
		return this;
	}

	@Override
	public void accept(PegVisitor visitor) {
		visitor.visitPegAny(this);
	}

	@Override
	public PegObject lazyMatch(PegObject inNode, PegParserContext source) {
		if(source.hasChar()) {
			source.consume(1);
			return inNode;
		}
		return source.foundFailure(this);
	}

}

class PegCharacter extends PegAtom {
	UniCharset charset;
	public PegCharacter(String leftLabel, String token) {
		super(leftLabel, token);
		this.charset = new UniCharset(token);
	}

	@Override
	protected Peg clone(String ns) {
		return this;
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
	public PegObject lazyMatch(PegObject inNode, PegParserContext source) {
		char ch = source.getChar();
		if(!this.charset.match(ch)) {
			return source.foundFailure(this);
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
	protected Peg clone(String ns) {
		if(ns != null && ns.length() > 0) {
			return new PegLabel(this.name, ns + this.symbol);
		}
		return this;
	}

	@Override protected PegObject lazyMatch(PegObject parentNode, PegParserContext source) {
		PegObject left = source.parsePegNode(parentNode, this.symbol);
		if(left.isFailure()) {
			return left;
		}
		return source.parseRightPegNode(left, this.symbol);
	}

	@Override
	protected void makeList(PegParser parser, UniArray<String> list, UniMap<String> set) {
		if(!set.hasKey(this.symbol)) {
			Peg next = parser.getDefinedPeg(this.symbol);
			list.add(this.symbol);
			set.put(this.symbol, this.symbol);
			next.makeList(parser, list, set);
		}
	}
	
	@Override
	protected boolean verify(PegParser parser) {
		if(!parser.hasPattern(this.symbol)) {
			Main._PrintLine(this.source.formatErrorMessage("error", this.sourcePosition, "undefined label: " + this.symbol));
			return false;
		}
		return true;
	}
	
	@Override
	public void accept(PegVisitor visitor) {
		visitor.visitPegLabel(this);
	}

}

abstract class PegUnary extends Peg {
	Peg innerExpr;
	boolean prefix;
	public PegUnary(String leftLabel, Peg e, boolean prefix) {
		super(leftLabel);
		this.innerExpr = e;
		this.prefix = prefix;
	}

	@Override
	public final int size() {
		return 1;
	}

	@Override
	public final Peg get(int index) {
		return this.innerExpr;
	}
	
	protected abstract String getOperator();
	
	@Override
	protected final void stringfy(UniStringBuilder sb, boolean debugMode) {
		if(this.prefix) {
			sb.append(this.getOperator());
		}
		if(this.innerExpr instanceof PegAtom || this.innerExpr instanceof PegNewObject) {
			this.innerExpr.stringfy(sb, debugMode);
		}
		else {
			sb.append("(");
			this.innerExpr.stringfy(sb, debugMode);
			sb.append(")");
		}
		if(!this.prefix) {
			sb.append(this.getOperator());
		}
	}
	
	@Override
	protected void makeList(PegParser parser, UniArray<String> list, UniMap<String> set) {
		this.innerExpr.makeList(parser, list, set);
	}

	@Override
	protected boolean verify(PegParser parser) {
		return this.innerExpr.verify(parser);
	}
}

abstract class PegSuffixed extends PegUnary {
	public PegSuffixed(String leftLabel, Peg e) {
		super(leftLabel, e, false);
	}
}

class PegOptional extends PegSuffixed {
	public PegOptional(String leftLabel, Peg e) {
		super(leftLabel, e);
	}

	@Override
	protected Peg clone(String ns) {
		Peg e = this.innerExpr.clone(ns);
		if(e != this) {
			return new PegOptional(this.name, e);
		}
		return this;
	}

	@Override
	protected String getOperator() {
		return "?";
	}

	@Override
	public void accept(PegVisitor visitor) {
		visitor.visitPegOptional(this);
	}

	@Override protected PegObject lazyMatch(PegObject parentNode, PegParserContext source) {
		PegObject node = parentNode;
		int stackPosition = source.getStackPosition(this);
		Peg errorPeg = source.storeFailurePeg();
		int errorPosition = source.storeFailurePosition();
		node = this.innerExpr.debugMatch(node, source);
		if(node.isFailure()) {
			source.popBack(stackPosition, Peg._BackTrack);
			source.restoreFailure(errorPeg, errorPosition);
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
	protected Peg clone(String ns) {
		Peg e = this.innerExpr.clone(ns);
		if(e != this) {
			return new PegOneMore(this.name, e);
		}
		return this;
	}

	@Override
	protected String getOperator() {
		return "+";
	}

	@Override
	public PegObject lazyMatch(PegObject parentNode, PegParserContext source) {
		PegObject prevNode = parentNode;
		Peg errorPeg = source.storeFailurePeg();
		int errorPosition = source.storeFailurePosition();
		int count = 0;
		while(source.hasChar()) {
			int startPosition = source.getPosition();
			PegObject node = this.innerExpr.debugMatch(prevNode, source);
			if(node.isFailure()) {
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
		source.restoreFailure(errorPeg, errorPosition);
		if(count < 1) {
			return source.foundFailure(this);
		}
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
	protected Peg clone(String ns) {
		Peg e = this.innerExpr.clone(ns);
		if(e != this) {
			return new PegZeroMore(this.name, e);
		}
		return this;
	}

	@Override
	protected String getOperator() {
		return "*";
	}

	@Override
	public PegObject lazyMatch(PegObject parentNode, PegParserContext source) {
		PegObject prevNode = parentNode;
		Peg errorPeg = source.storeFailurePeg();
		int errorPosition = source.storeFailurePosition();
		int count = 0;
		while(source.hasChar()) {
			int startPosition = source.getPosition();
			PegObject node = this.innerExpr.debugMatch(prevNode, source);
			if(node.isFailure()) {
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
		source.restoreFailure(errorPeg, errorPosition);
		return prevNode;
	}

	@Override
	public void accept(PegVisitor visitor) {
		visitor.visitZeroMore(this);
	}
}

abstract class PegPredicate extends PegUnary {
	public PegPredicate(String leftLabel, Peg e) {
		super(leftLabel, e, true);
		this.innerExpr = e;
	}
}

class PegAndPredicate extends PegPredicate {
	PegAndPredicate(String leftLabel, Peg e) {
		super(leftLabel, e);
	}

	@Override
	protected Peg clone(String ns) {
		Peg e = this.innerExpr.clone(ns);
		if(e != this) {
			return new PegAndPredicate(this.name, e);
		}
		return this;
	}

	@Override
	protected String getOperator() {
		return "&";
	}

	@Override
	protected PegObject lazyMatch(PegObject parentNode, PegParserContext source) {
		PegObject node = parentNode;
		int stackPosition = source.getStackPosition(this);
		node = this.innerExpr.debugMatch(node, source);
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
	protected Peg clone(String ns) {
		Peg e = this.innerExpr.clone(ns);
		if(e != this) {
			return new PegNotPredicate(this.name, e);
		}
		return this;
	}

	@Override
	protected String getOperator() {
		return "!";
	}

	@Override
	protected PegObject lazyMatch(PegObject parentNode, PegParserContext source) {
		PegObject node = parentNode;
		int stackPosition = source.getStackPosition(this);
		node = this.innerExpr.debugMatch(node, source);
		source.popBack(stackPosition, Peg._BackTrack);
		if(node.isFailure()) {
			return parentNode;
		}
		return source.foundFailure(this);
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
	
	@Override protected void stringfy(UniStringBuilder sb, boolean debugMode) {
		for(int i = 0; i < this.size(); i++) {
			if(i > 0) {
				sb.append(" ");
			}
			Peg e = this.get(i);
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
	protected void makeList(PegParser parser, UniArray<String> list, UniMap<String> set) {
		for(int i = 0; i < this.size(); i++) {
			this.get(i).makeList(parser, list, set);
		}
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

}

class PegSequence extends PegList {
	PegSequence() {
		super();
	}
	
	PegSequence(Peg first) {
		super(first);
	}

	@Override
	protected Peg clone(String ns) {
		boolean hasClone = false;
		for(int i = 0; i < this.list.size(); i++) {
			Peg e  = this.list.ArrayValues[i].clone(ns);
			if(e != this.list.ArrayValues[i]) {
				hasClone = true;
			}
		}
		if(hasClone) {
			PegList l = new PegSequence();
			for(int i = 0; i < this.list.size(); i++) {
				l.list.add(this.get(i).clone(ns));
			}
			return l;
		}
		return this;
	}

	@Override
	protected PegObject lazyMatch(PegObject inNode, PegParserContext source) {
		for(int i = 0; i < this.list.size(); i++) {
			Peg e  = this.list.ArrayValues[i];
			inNode = e.debugMatch(inNode, source);
			if(inNode.isFailure()) {
				return inNode;
			}
		}
		return inNode;
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
	boolean hasCatch = false;
	PegChoice() {
		super();
	}
	
	PegChoice(Peg first) {
		super(first);
	}

	@Override
	protected Peg clone(String ns) {
		boolean hasClone = false;
		for(int i = 0; i < this.list.size(); i++) {
			Peg e  = this.list.ArrayValues[i].clone(ns);
			if(e != this.list.ArrayValues[i]) {
				hasClone = true;
			}
		}
		if(hasClone) {
			PegChoice l = new PegChoice();
			l.hasCatch = this.hasCatch;
			for(int i = 0; i < this.list.size(); i++) {
				l.list.add(this.get(i).clone(ns));
			}
			return l;
		}
		return this;
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
	protected PegObject lazyMatch(PegObject inNode, PegParserContext source) {
		int stackPosition = source.getStackPosition(this);
		PegObject node = inNode;
		SourceToken stackedLocation = null;
		if(this.hasCatch) {
			stackedLocation = source.stackFailureLocation(null);
		}
		for(int i = 0; i < this.size(); i++) {
			Peg e  = this.get(i);
			if(e instanceof PegCatch) {
				node = source.newPegObject("#error");
				node.source = source.stackFailureLocation(stackedLocation);
				if(Main.PegDebuggerMode) {
					Main._PrintLine(node.source.formatErrorMessage("error: " + this.name, " by " + node.source.createdPeg));
				}
//				System.out.println("@" + this);
//				System.out.println("node.source=" + node.source.startIndex + ", " + node.source.endIndex);
//				System.out.println("node.source.peg=" + node.source.createdPeg);
//				System.out.println("node.source: " + node.source.getText());
				return e.debugMatch(node, source);
			}
			node = e.debugMatch(inNode, source);
			if(!node.isFailure()) {
				break;
			}
			source.popBack(stackPosition, Peg._BackTrack);
		}
		if(stackedLocation != null) {
			source.stackFailureLocation(stackedLocation);
		}
		return node;
	}

//	@Override
//	protected PegObject lazyMatch2(PegObject inNode, PegParserContext source, boolean hasNextChoice) {
//		int stackPosition = source.getStackPosition(this);
//		PegObject node = inNode;
//		for(int i = 0; i < this.size(); i++) {
//			Peg e  = this.get(i);
//			boolean nextChoice = true;
//			if(i + 1 == this.size()) {
//				nextChoice = hasNextChoice;
//			}
//			node = e.debugMatch(inNode, source, nextChoice);
//			if(!node.isErrorNode()) {
//				return node;
//			}
//			source.popBack(stackPosition, Peg._BackTrack);
//		}
//		return node;
//	}

	@Override
	protected boolean verify(PegParser parser) {
		boolean noerror = true;
		for(int i = 0; i < this.list.size(); i++) {
			Peg e  = this.list.ArrayValues[i];
			if(!e.verify(parser)) {
				noerror = false;
			}
			if(e instanceof PegCatch) {
				this.hasCatch = true;
			}
		}
		return noerror;
	}

	@Override
	public void accept(PegVisitor visitor) {
		visitor.visitChoice(this);
	}
}

class PegSetter extends PegSuffixed {
	public int index;
	public PegSetter(String leftLabel, Peg e, int index) {
		super(leftLabel, e);
		this.innerExpr = e;
		this.index = index;
	}

	@Override
	protected Peg clone(String ns) {
		Peg e = this.innerExpr.clone(ns);
		if(e != this) {
			return new PegSetter(this.name, e, this.index);
		}
		return this;
	}

	@Override
	protected String getOperator() {
		if(this.index != -1) {
			return "@" + this.index;
		}
		return "@";
	}
	
	@Override
	public void accept(PegVisitor visitor) {
		visitor.visitSetter(this);
	}

	@Override
	public PegObject lazyMatch(PegObject parentNode, PegParserContext source) {
		PegObject node = this.innerExpr.debugMatch(parentNode, source);
		if(node.isFailure()) {
			return node;
		}
		if(parentNode == node) {
			return parentNode;
		}
		source.push(this, parentNode, this.index, node);
		return parentNode;
	}
}

class PegObjectLabel extends PegAtom {

	public PegObjectLabel(String leftLabel, String objectLabel) {
		super(leftLabel, objectLabel);
	}
	
	@Override
	protected Peg clone(String ns) {
		return this;
	}

	@Override
	protected final void stringfy(UniStringBuilder sb, boolean debugMode) {
		if(debugMode) {
			sb.append(this.symbol);
		}
	}

	@Override
	public PegObject lazyMatch(PegObject inNode, PegParserContext source) {
		inNode.name = this.symbol;
		return inNode;
	}

	@Override
	protected boolean verify(PegParser parser) {
		parser.addObjectLabel(this.symbol);
		return true;
	}

	@Override
	public void accept(PegVisitor visitor) {
		//visitor.visitNewObject(this);
	}

}

class PegNewObject extends PegList {
	boolean leftJoin = false;
	String nodeName = "";

	public PegNewObject(String leftLabel, boolean leftJoin) {
		super();
	}

	public PegNewObject(String leftLabel, boolean leftJoin, Peg e) {
		super(e);
		this.leftJoin = leftJoin;
	}
	
	@Override
	protected Peg clone(String ns) {
		boolean hasClone = false;
		for(int i = 0; i < this.list.size(); i++) {
			Peg e  = this.list.ArrayValues[i].clone(ns);
			if(e != this.list.ArrayValues[i]) {
				hasClone = true;
			}
		}
		if(hasClone) {
			PegList l = new PegNewObject(this.name, this.leftJoin);
			for(int i = 0; i < this.list.size(); i++) {
				l.list.add(this.get(i).clone(ns));
			}
			return l;
		}
		return this;
	}

	public void add(Peg e) {
		if(e instanceof PegSequence) {
			for(int i =0; i < e.size(); i++) {
				this.list.add(e.get(i));
			}
		}
		else {
			this.list.add(e);
		}
	}

	@Override
	protected final void stringfy(UniStringBuilder sb, boolean debugMode) {
		if(debugMode) {
			if(this.leftJoin) {
				sb.append("<<@ ");
			}
			else {
				sb.append("<< ");
			}
		}
		else {
			sb.append("( ");
		}
		super.stringfy(sb, debugMode);
		if(debugMode) {
			sb.append(" >>");
		}
		else {
			sb.append(" )");
		}
	}

	@Override
	public PegObject lazyMatch(PegObject inNode, PegParserContext source) {
		PegObject leftNode = inNode;
		int pos = source.getPosition();
		int stack = source.getStackPosition(this);
		int i = 0;
		PegObject newnode = source.newPegObject(this.nodeName);
		newnode.setSource(this, source.source, pos, source.getPosition());
		if(this.leftJoin) {
			source.push(this, newnode, 0, leftNode);
		}
		for(; i < this.size(); i++) {
			Peg e = this.get(i);
			PegObject node = e.debugMatch(newnode, source);
			if(node.isFailure()) {
				return node;
			}
			if(node != newnode) {
				this.warning("dropping " + node);
			}
		}
		int top = source.getStackPosition(this);
		for(i = stack; i < top; i++) {
			Log log = source.logStack.ArrayValues[i];
			if(log.type == 'p' && log.parentNode == newnode) {
				if(log.index == -1) {
					newnode.append(log.childNode);
				}
				else {
					newnode.set(log.index, log.childNode);
				}
			}
		}
		if(newnode.name == null || newnode.name.length() == 0) {
			newnode.name = source.source.substring(pos, source.getPosition());
		}
		newnode.setSource(this, source.source, pos, source.getPosition());
		newnode.checkNullEntry();
		return newnode;
	}

	@Override
	public void accept(PegVisitor visitor) {
		visitor.visitNewObject(this);

	}
}

// (e / catch e)

class PegCatch extends PegUnary {
	PegCatch (String leftLabel, Peg first) {
		super(leftLabel, first, true);
	}
	@Override
	protected Peg clone(String ns) {
		Peg e = this.innerExpr.clone(ns);
		if(e != this) {
			return new PegCatch(this.name, e);
		}
		return this;
	}

	
	@Override
	protected String getOperator() {
		return "catch ";
	}

	@Override
	protected PegObject lazyMatch(PegObject inNode, PegParserContext source) {
		this.innerExpr.debugMatch(inNode, source);
		return inNode;
	}

	@Override
	public void accept(PegVisitor visitor) {
		// TODO Auto-generated method stub
		
	}
	
}

class PegIndent extends PegAtom {

	PegIndent(String leftLabel) {
		super(leftLabel, "indent");
		// TODO Auto-generated constructor stub
	}

	@Override
	protected Peg clone(String ns) {
		return this;
	}

	@Override
	protected PegObject lazyMatch(PegObject inNode, PegParserContext source) {
		if(inNode.source != null) {
			String indent = inNode.source.getIndentText();
			System.out.println("###" + indent + "###");
			if(source.match(indent)) {
				return inNode;
			}
		}
		return inNode;
		//return source.newErrorNode(this, "mismatched indent", hasNextChoice);
	}

	@Override
	protected boolean verify(PegParser parser) {
		return true;
	}

	@Override
	public void accept(PegVisitor visitor) {
		// TODO Auto-generated method stub
	}

}

