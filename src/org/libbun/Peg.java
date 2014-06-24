package org.libbun;

public abstract class Peg {
	public final static boolean _BackTrack = true;
	int       flag     = 0;
	String    ruleName     = null;
	boolean   debug    = false;
	boolean   hasLeftRecursion = false;

	PegSource source = null;
	int       sourcePosition = 0;
	
	Peg(String ruleName) {
		this.ruleName = ruleName;
	}

	protected abstract Peg clone(String ns);
	protected abstract void stringfy(UniStringBuilder sb, boolean debugMode);
	protected abstract void makeList(PegParser parser, UniArray<String> list, UniMap<String> set);
	public abstract boolean simpleMatch(UniMap<Peg> pegMap, SourceContext source);
	protected abstract PegObject lazyMatch(PegObject inNode, ParserContext context);
	protected abstract boolean verify(PegParser parser);
	public abstract void accept(PegVisitor visitor);

	public int size() {
		return 0;
	}
	public Peg get(int index) {
		return this;  // to avoid NullPointerException
	}

	@Override public String toString() {
		UniStringBuilder sb = new UniStringBuilder();
		this.stringfy(sb, false);
		if(this.ruleName != null) {
			sb.append(" defined in ");
			sb.append(this.ruleName);
		}
		return sb.toString();
	}
	
	public boolean hasLeftRecursion() {
		return this.hasLeftRecursion;
	}
	
	public void setLeftRecursion(boolean lrExistense) {
		this.hasLeftRecursion = lrExistense;
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
	protected PegObject debugMatch(PegObject inNode, ParserContext context) {
		if(this.debug) {
			PegObject node2 = this.lazyMatch(inNode, context);
			String msg = "matched";
			if(node2.isFailure()) {
				msg = "failed";
			}
			String line = context.formatErrorMessage(msg, this.toString());
			System.out.println(line + "\n\tnode #" + inNode + "# => #" + node2 + "#");
			return node2;
		}
		return this.lazyMatch(inNode, context);
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

	void setSource(PegSource source, int sourcePosition) {
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
	
	public final static Peg newString(String token) {
		return new PegString(null, token);
	}
	public final static Peg newAny() {
		return new PegAny(null);
	}
	public final static Peg newCharacter(String charSet) {
		return new PegCharacter(null, charSet);
	}
	public final static Peg newOptional(Peg e) {
		return new PegOptional(null, e);
	}
	public final static Peg newZeroMore(Peg e) {
		return new PegZeroMore(null, e);
	}
	public final static Peg newOneMore(Peg e) {
		return new PegOneMore(null, e);
	}
	public final static Peg newSequence(Peg ... elist) {
		PegSequence l = new PegSequence();
		for(Peg e : elist) {
			l.list.add(e);
		}
		return l;
	}
	public final static Peg newChoice(Peg ... elist) {
		PegSequence l = new PegSequence();
		for(Peg e : elist) {
			l.list.add(e);
		}
		return l;
	}
}

abstract class PegAtom extends Peg {
	String symbol;
	public PegAtom (String ruleName, String symbol) {
		super(ruleName);
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
	}

}

class PegString extends PegAtom {
	public PegString(String ruleName, String symbol) {
		super(ruleName, symbol);
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
	public boolean simpleMatch(UniMap<Peg> pegMap, SourceContext source) {
		if(source.match(this.symbol)) {
			return true;
		}
		return false;
	}
	@Override
	public PegObject lazyMatch(PegObject inNode, ParserContext context) {
		if(context.match(this.symbol)) {
			return inNode;
		}
		return context.foundFailure(this);
	}
}

class PegAny extends PegAtom {
	public PegAny(String ruleName) {
		super(ruleName, ".");
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
	public boolean simpleMatch(UniMap<Peg> pegMap, SourceContext source) {
		if(source.hasChar()) {
			source.consume(1);
			return true;
		}
		return false;
	}
	@Override
	public PegObject lazyMatch(PegObject inNode, ParserContext context) {
		if(context.hasChar()) {
			context.consume(1);
			return inNode;
		}
		return context.foundFailure(this);
	}
}

class PegCharacter extends PegAtom {
	UniCharset charset;
	public PegCharacter(String ruleName, String token) {
		super(ruleName, token);
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
	public boolean simpleMatch(UniMap<Peg> pegMap, SourceContext context) {
		char ch = context.getChar();
		if(this.charset.match(ch)) {
			context.consume(1);
			return true;
		}
		return false;
	}
	@Override
	public PegObject lazyMatch(PegObject inNode, ParserContext context) {
		char ch = context.getChar();
		if(!this.charset.match(ch)) {
			return context.foundFailure(this);
		}
		context.consume(1);
		return inNode;
	}
}

class PegLabel extends PegAtom {
	public PegLabel(String ruleName, String token) {
		super(ruleName, token);
	}
	@Override
	protected Peg clone(String ns) {
		if(ns != null && ns.length() > 0) {
			return new PegLabel(this.ruleName, ns + this.symbol);
		}
		return this;
	}
	@Override
	public boolean simpleMatch(UniMap<Peg> pegMap, SourceContext source) {
		Peg e = pegMap.get(this.symbol, null);
		if(e != null) {
			return e.simpleMatch(pegMap, source);
		}
		return false;
	}
	@Override protected PegObject lazyMatch(PegObject parentNode, ParserContext context) {
		PegObject left = context.parsePegNode(parentNode, this.symbol);
		if(left.isFailure()) {
			return left;
		}
		return context.parseRightPegNode(left, this.symbol);
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
	public PegUnary(String ruleName, Peg e, boolean prefix) {
		super(ruleName);
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

//abstract class PegSuffixed extends PegUnary {
//	public PegSuffixed(String ruleName, Peg e) {
//		super(ruleName, e, false);
//	}
//}

class PegOptional extends PegUnary {
	public PegOptional(String ruleName, Peg e) {
		super(ruleName, e, false);
	}
	@Override
	protected Peg clone(String ns) {
		Peg e = this.innerExpr.clone(ns);
		if(e != this) {
			return new PegOptional(this.ruleName, e);
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
	@Override
	public boolean simpleMatch(UniMap<Peg> pegMap, SourceContext source) {
		this.innerExpr.simpleMatch(pegMap, source);
		return true;
	}
	@Override protected PegObject lazyMatch(PegObject parentNode, ParserContext context) {
		PegObject node = parentNode;
		int stackPosition = context.getStackPosition(this);
		Peg errorPeg = context.storeFailurePeg();
		int errorPosition = context.storeFailurePosition();
		node = this.innerExpr.debugMatch(node, context);
		if(node.isFailure()) {
			context.popBack(stackPosition, Peg._BackTrack);
			context.restoreFailure(errorPeg, errorPosition);
			node = parentNode;
		}
		return node;
	}
}

class PegOneMore extends PegUnary {
	public int atleast = 0; 
	protected PegOneMore(String ruleName, Peg e, int atLeast) {
		super(ruleName, e, false);
		this.atleast = atLeast;
	}
	public PegOneMore(String ruleName, Peg e) {
		this(ruleName, e, 1);
	}
	@Override
	protected Peg clone(String ns) {
		Peg e = this.innerExpr.clone(ns);
		if(e != this) {
			return new PegOneMore(this.ruleName, e);
		}
		return this;
	}
	@Override
	protected String getOperator() {
		return "+";
	}
	@Override
	public boolean simpleMatch(UniMap<Peg> pegMap, SourceContext source) {
		int count = 0;
		while(source.hasChar()) {
			int startPosition = source.getPosition();
			if(!this.innerExpr.simpleMatch(pegMap, source)) {
				break;
			}
			if(!(startPosition < source.getPosition())) {
				break;
			}
			count = count + 1;
		}
		if(count < this.atleast) {
			return false;
		}
		return true;
	}
	@Override
	public PegObject lazyMatch(PegObject parentNode, ParserContext context) {
		PegObject prevNode = parentNode;
		Peg errorPeg = context.storeFailurePeg();
		int errorPosition = context.storeFailurePosition();
		int count = 0;
		while(context.hasChar()) {
			int startPosition = context.getPosition();
			PegObject node = this.innerExpr.debugMatch(prevNode, context);
			if(node.isFailure()) {
				break;
			}
			prevNode = node;
			if(!(startPosition < context.getPosition())) {
				break;
			}
			count = count + 1;
		}
		context.restoreFailure(errorPeg, errorPosition);
		if(count < this.atleast) {
			return context.foundFailure(this);
		}
		return prevNode;
	}

	@Override
	public void accept(PegVisitor visitor) {
		visitor.visitOneMore(this);
	}
}

class PegZeroMore extends PegOneMore {
	public PegZeroMore(String ruleName, Peg e) {
		super(ruleName, e, 0);
	}
	@Override
	protected Peg clone(String ns) {
		Peg e = this.innerExpr.clone(ns);
		if(e != this) {
			return new PegZeroMore(this.ruleName, e);
		}
		return this;
	}
	@Override
	protected String getOperator() {
		return "*";
	}
	@Override
	public void accept(PegVisitor visitor) {
		visitor.visitZeroMore(this);
	}
}

class PegAnd extends PegUnary {
	PegAnd(String ruleName, Peg e) {
		super(ruleName, e, true);
	}
	@Override
	protected Peg clone(String ns) {
		Peg e = this.innerExpr.clone(ns);
		if(e != this) {
			return new PegAnd(this.ruleName, e);
		}
		return this;
	}
	@Override
	protected String getOperator() {
		return "&";
	}
	@Override
	public boolean simpleMatch(UniMap<Peg> pegMap, SourceContext source) {
		int pos = source.getPosition();
		boolean res = this.innerExpr.simpleMatch(pegMap, source);
		source.rollback(pos);
		return res;
	}
	@Override
	protected PegObject lazyMatch(PegObject parentNode, ParserContext context) {
		PegObject node = parentNode;
		int stackPosition = context.getStackPosition(this);
		node = this.innerExpr.debugMatch(node, context);
		context.popBack(stackPosition, Peg._BackTrack);
		return node;
	}
	@Override
	public void accept(PegVisitor visitor) {
		visitor.visitPegAnd(this);
	}
}

class PegNot extends PegUnary {
	PegNot(String ruleName, Peg e) {
		super(ruleName, e, true);
	}
	@Override
	protected Peg clone(String ns) {
		Peg e = this.innerExpr.clone(ns);
		if(e != this) {
			return new PegNot(this.ruleName, e);
		}
		return this;
	}
	@Override
	protected String getOperator() {
		return "!";
	}
	@Override
	public boolean simpleMatch(UniMap<Peg> pegMap, SourceContext source) {
		int pos = source.getPosition();
		boolean res = this.innerExpr.simpleMatch(pegMap, source);
		source.rollback(pos);
		return !res;
	}
	@Override
	protected PegObject lazyMatch(PegObject parentNode, ParserContext context) {
		PegObject node = parentNode;
		int stackPosition = context.getStackPosition(this);
		node = this.innerExpr.debugMatch(node, context);
		context.popBack(stackPosition, Peg._BackTrack);
		if(node.isFailure()) {
			return parentNode;
		}
		return context.foundFailure(this);
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
		super(first.ruleName);
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
	public boolean simpleMatch(UniMap<Peg> pegMap, SourceContext source) {
		int pos = source.getPosition();
		for(int i = 0; i < this.size(); i++) {
			Peg e  = this.get(i);
			if(!e.simpleMatch(pegMap, source)) {
				source.rollback(pos);
				return false;
			}
		}
		return true;
	}
	@Override
	protected PegObject lazyMatch(PegObject inNode, ParserContext context) {
		int stackPosition = context.getStackPosition(this);
		for(int i = 0; i < this.list.size(); i++) {
			Peg e  = this.list.ArrayValues[i];
			inNode = e.debugMatch(inNode, context);
			if(inNode.isFailure()) {
				context.popBack(stackPosition, Peg._BackTrack);
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
			if(this.ruleName == null) {
				this.ruleName = e.ruleName;
			}
			this.list.add(e);
			if(e.hasLeftRecursion() == true) {
				this.setLeftRecursion(true);
			}
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
	public boolean simpleMatch(UniMap<Peg> pegMap, SourceContext source) {
		for(int i = 0; i < this.list.size(); i++) {
			Peg e  = this.list.ArrayValues[i];
			if(e.simpleMatch(pegMap, source)) {
				return true;
			}
		}
		return false;
	}
	@Override
	protected PegObject lazyMatch(PegObject inNode, ParserContext context) {
		int stackPosition = context.getStackPosition(this);
		PegObject node = inNode;
		Peg errorPeg = context.storeFailurePeg();
		int errorPosition = context.storeFailurePosition();
		for(int i = 0; i < this.size(); i++) {
			Peg e  = this.get(i);
			if(e instanceof PegCatch) {
				node = context.newPegObject("#error");
				node.createdPeg = context.storeFailurePeg();
				node.startIndex = context.storeFailurePosition();
				node.endIndex = context.storeFailurePosition();
				if(Main.PegDebuggerMode) {
					Main._PrintLine(node.formatSourceMessage("error: " + this.ruleName, " by " + node.createdPeg));
				}
				context.restoreFailure(errorPeg, errorPosition);
				return e.debugMatch(node, context);
			}
			node = e.debugMatch(inNode, context);
			if(!node.isFailure()) {
				break;
			}
			context.popBack(stackPosition, Peg._BackTrack);
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

class PegSetter extends PegUnary {
	public int index;
	public PegSetter(String ruleName, Peg e, int index) {
		super(ruleName, e, false);
		this.innerExpr = e;
		this.index = index;
	}
	@Override
	protected Peg clone(String ns) {
		Peg e = this.innerExpr.clone(ns);
		if(e != this) {
			return new PegSetter(this.ruleName, e, this.index);
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
	public boolean simpleMatch(UniMap<Peg> pegMap, SourceContext source) {
		return this.simpleMatch(pegMap, source);
	}
	@Override
	public PegObject lazyMatch(PegObject parentNode, ParserContext context) {
		PegObject node = this.innerExpr.debugMatch(parentNode, context);
		if(node.isFailure()) {
			return node;
		}
		if(parentNode == node) {
			return parentNode;
		}
		context.push(this, parentNode, this.index, node);
		return parentNode;
	}
}

class PegObjectLabel extends PegAtom {
	public PegObjectLabel(String ruleName, String objectLabel) {
		super(ruleName, objectLabel);
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
	public boolean simpleMatch(UniMap<Peg> pegMap, SourceContext source) {
		return true;
	}
	@Override
	public PegObject lazyMatch(PegObject inNode, ParserContext context) {
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
		visitor.visitObjectLabel(this);
	}
}

class PegNewObject extends PegList {
	boolean leftJoin = false;
	String nodeName = "";
	public PegNewObject(String ruleName, boolean leftJoin) {
		super();
	}
	public PegNewObject(String ruleName, boolean leftJoin, Peg e) {
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
			PegList l = new PegNewObject(this.ruleName, this.leftJoin);
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
	public boolean simpleMatch(UniMap<Peg> pegMap, SourceContext source) {
		int pos = source.getPosition();
		for(int i = 0; i < this.size(); i++) {
			Peg e = this.get(i);
			if(!e.simpleMatch(pegMap, source)) {
				source.rollback(pos);
				return false;
			}
		}
		return true;
	}

	@Override
	public PegObject lazyMatch(PegObject inNode, ParserContext context) {
		PegObject leftNode = inNode;
		int pos = context.getPosition();
		int stack = context.getStackPosition(this);
		int i = 0;
		PegObject newnode = context.newPegObject(this.nodeName);
		newnode.setSource(this, context.source, pos, context.getPosition());
		if(this.leftJoin) {
			context.push(this, newnode, 0, leftNode);
		}
		for(; i < this.size(); i++) {
			Peg e = this.get(i);
			PegObject node = e.debugMatch(newnode, context);
			if(node.isFailure()) {
				return node;
			}
			if(node != newnode) {
				this.warning("dropping " + node);
			}
		}
		int top = context.getStackPosition(this);
		context.addSubObject(newnode, stack, top);
//		if(newnode.name == null || newnode.name.length() == 0) {
//			newnode.name = context.source.substring(pos, context.getPosition());
//		}
		newnode.setSource(this, context.source, pos, context.getPosition());
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
	PegCatch (String ruleName, Peg first) {
		super(ruleName, first, true);
	}
	@Override
	protected Peg clone(String ns) {
		Peg e = this.innerExpr.clone(ns);
		if(e != this) {
			return new PegCatch(this.ruleName, e);
		}
		return this;
	}
	@Override
	protected String getOperator() {
		return "catch ";
	}
	@Override
	public boolean simpleMatch(UniMap<Peg> pegMap, SourceContext source) {
		this.innerExpr.simpleMatch(pegMap, source);
		return true;
	}
	@Override
	protected PegObject lazyMatch(PegObject inNode, ParserContext context) {
		this.innerExpr.debugMatch(inNode, context);
		return inNode;
	}
	@Override
	public void accept(PegVisitor visitor) {
		visitor.visitCatch(this);
	}
}

class PegIndent extends PegAtom {
	PegIndent(String ruleName) {
		super(ruleName, "indent");
	}
	@Override
	protected Peg clone(String ns) {
		return this;
	}
	@Override
	public boolean simpleMatch(UniMap<Peg> pegMap, SourceContext source) {
		return false;
	}
	@Override
	protected PegObject lazyMatch(PegObject inNode, ParserContext context) {
		if(inNode.source != null) {
			String indent = inNode.source.getIndentText(inNode.startIndex);
			if(context.matchIndentSize(indent)) {
				return inNode;
			}
			return new PegObject(null); //not match
		}
		return inNode;
	}
	@Override
	protected boolean verify(PegParser parser) {
		return true;
	}
	@Override
	public void accept(PegVisitor visitor) {
		visitor.visitIndent(this);
	}
}
