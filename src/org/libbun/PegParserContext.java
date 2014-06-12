package org.libbun;

public class PegParserContext extends SourceContext {
	public  PegParser    parser;

	final UniArray<Log> logStack = new UniArray<Log>(new Log[128]);
	private int stackTop = 0;

	private final UniMap<PegObject> memoMap = new UniMap<PegObject>();
	private UniMap<Memo> memoMap2 = new UniMap<Memo>();
	
	private final UniMap<Boolean> lrExistenceMap = new UniMap<Boolean>();
	
	public Boolean getLrExistence(String key) {
		return this.lrExistenceMap.get(key);
	}
	
	public void setLrExistence(String key, Boolean value) {
		lrExistenceMap.put(key, value);
	}
	
	public void initMemo() {
		this.memoMap2 = new UniMap<Memo>();
	}
	
	public UniMap<Memo> getMemoMap() {
		return memoMap2;
	}
	
	int memoHit = 0;
	int memoMiss = 0;
	int memoSize = 0;
	int objectCount = 0;
	int errorCount = 0;


	public PegParserContext(PegParser parser, PegSource source, int startIndex, int endIndex) {
		super(source, startIndex, endIndex);
		this.parser = parser;
	}

	@Override
	public SourceContext subContext(int startIndex, int endIndex) {
		return new PegParserContext(this.parser, this.source, startIndex, endIndex);
	}


	@Override
	public void skipIndent(int indentSize) {
		int pos = this.sourcePosition;
		//		this.showPosition("skip characters until indent="+indentSize + ", pos=" + pos, pos);
		for(;pos < this.endPosition; pos = pos + 1) {
			char ch = this.charAt(pos);
			if(ch == '\n' && pos > this.sourcePosition) {
				int posIndent = this.source.getIndentSize(pos+1);
				if(posIndent <= indentSize) {
					this.sourcePosition = pos + 1;
					//					System.out.println("skip characters until indent="+indentSize + ", pos=" + this.sourcePosition);
					return ;
				}
			}
		}
		//		System.out.println("skip characters until indent="+indentSize + ", pos = endPosition");
		this.sourcePosition = this.endPosition;
	}

	public boolean hasNode() {
		this.matchZeroMore(UniCharset.WhiteSpaceNewLine);
		return this.sourcePosition < this.endPosition;
	}

	public PegObject parseNode(String key) {
		PegObject po = this.parsePegNode(new PegObject(BunSymbol.TopLevelFunctor), key);
		//return po.eval(this.source, parentNode);
		return po;
	}

	public char getFirstChar() {
		return this.getChar();
	}

	public boolean isLeftRecursion(String PatternName) {
		Peg e = this.parser.getRightPattern(PatternName, this.getFirstChar());
		return e != null;
	}

	public final PegObject parsePegNode(PegObject parentNode, String pattern) {
		int pos = this.getPosition();
		String key = pattern + ":" + pos;
		Memo m = this.memoMap2.get(key, null);
		if(m == null) {
			m = new Memo();
			m.nextPosition = this.getPosition();
			m.result = foundFailureNode;
			this.memoMap2.put(key, m);
			this.memoMiss = this.memoMiss + 1;
			Peg e = this.parser.getPattern(pattern, this.getFirstChar());
			PegObject ans = e.debugMatch(parentNode, this);
			if(pos == this.getPosition() && !ans.isFailure()) {
				this.memoMap2.remove(key);
				return ans;
			}
			else {
				m.result = ans;
				m.nextPosition = this.getPosition();
				if(getLrExistence(pattern)) {
					return growLR(pattern, pos, m, parentNode);
				}
				else {
					return ans;
				}
			}
		}
		else {
			this.memoHit = this.memoHit + 1;
			this.sourcePosition = m.nextPosition;
			if(m.result == null) {
				return parentNode;
			}
			return m.result;
		}
	}
	
	public final PegObject growLR(String pattern, int pos, Memo m, PegObject parentNode) {
		while(true) {
			this.setPosition(pos);
			Peg e = this.parser.getPattern(pattern, this.getFirstChar());
			PegObject ans = e.debugMatch(parentNode, this);
			if(ans.isFailure() || this.getPosition() <= m.nextPosition){
				break;
			}
			m.result = ans;
			m.nextPosition = this.getPosition();
		}
		this.setPosition(m.nextPosition);
		return m.result;
	}

	public final PegObject parsePegNode2(PegObject parentNode, String pattern, boolean hasNextChoice) {
		int pos = this.getPosition();
		String key = pattern + ":" + pos;
		PegObject node = this.memoMap.get(key, null);
		if(node != null) {
			this.memoHit = this.memoHit + 1;
			return node;
		}
		Peg e = this.parser.getPattern(pattern, this.getFirstChar());
		if(e != null) {
			node = e.debugMatch(parentNode, this);
			if(node.isFailure() && hasNextChoice) {
				this.memoMiss = this.memoMiss + 1;
				this.memoMap.put(key, node);
				return node;
			}
			if(node != parentNode && node.isFailure()) {
				this.memoMiss = this.memoMiss + 1;
				this.memoMap.put(key, node);
				return node;
			}
			return node;
		}
		Main._Exit(1, "undefined label " + pattern + " '" + this.getFirstChar() + "'");
		return this.foundFailureNode;
	}

	public final PegObject parsePegNodeNon(PegObject parentNode, String pattern, boolean hasNextChoice) {
		Peg e = this.parser.getPattern(pattern, this.getFirstChar());
		if(e != null) {
			return e.debugMatch(parentNode, this);
		}
		Main._Exit(1, "undefined label " + pattern + " '" + this.getFirstChar() + "'");
		return this.foundFailureNode;
	}

	public final PegObject parseRightPegNode(PegObject left, String symbol) {
		String key = this.parser.nameRightJoinName(symbol);
		Peg e = this.parser.getPattern(key, this.getFirstChar());
		if(e != null) {
			PegObject right = e.debugMatch(left, this);
			if(!right.isFailure()) {
				left = right;
			}
		}
		return left;
	}

	final int getStackPosition(Peg trace) {
		this.pushImpl(trace, null, '\0', null, 0, null);
		return this.stackTop;
	}

	private void pushImpl(Peg trace, String msg, char type, PegObject parentNode, int index, PegObject childNode) {
		Log log = null;
		if(this.stackTop < this.logStack.size()) {
			if(this.logStack.ArrayValues[this.stackTop] == null) {
				this.logStack.ArrayValues[this.stackTop] = new Log();
			}
			log = this.logStack.ArrayValues[this.stackTop];
		}
		else {
			log = new Log();
			this.logStack.add(log);
		}
		log.trace = trace;
		log.sourcePosition = this.sourcePosition;
		log.msg = msg;
		log.type = type;
		log.parentNode = parentNode;
		log.index = index;
		log.childNode = childNode;
		this.stackTop = this.stackTop + 1;
	}

	void pushLog(Peg trace, String msg) {
		this.pushImpl(trace, msg, 'm', null, 0, null);
	}

	void popBack(int stackPostion, boolean backtrack) {
		this.stackTop = stackPostion-1;
		Log log = this.logStack.ArrayValues[stackPostion-1];
		if(backtrack) {
			this.rollback(log.sourcePosition);
		}
	}

	public void push(Peg trace, PegObject parentNode, int index, PegObject node) {
		this.pushImpl(trace, "", 'p', parentNode, index, node);
	}

	public PegObject newPegObject(String name) {
		PegObject node = new PegObject(name, this.source, null, this.sourcePosition);
		this.objectCount = this.objectCount + 1;
		return node;
	}

	private final PegObject foundFailureNode = new PegObject(null, this.source, null, 0);
	
	public final PegObject foundFailure(Peg created) {
		if(this.sourcePosition >= this.foundFailureNode.endIndex) {  // adding error location
			//System.out.println("failure found?: " + this.sourcePosition + " > " + token.endIndex);
			this.foundFailureNode.startIndex = this.sourcePosition;
			this.foundFailureNode.endIndex = this.sourcePosition;
			this.foundFailureNode.createdPeg = created;
		}
		return this.foundFailureNode;
	}
	
	public final Peg storeFailurePeg() {
		return this.foundFailureNode.createdPeg;
	}

	public final int storeFailurePosition() {
		return this.foundFailureNode.endIndex;
	}

	public final void restoreFailure(Peg created, int pos) {
		this.foundFailureNode.createdPeg = created;
		this.foundFailureNode.startIndex   = pos;
		this.foundFailureNode.endIndex   = pos;
	}
}

class Log {
	int sourcePosition;
	Peg trace;
	String msg;
	char type;
	PegObject parentNode;
	int index;
	PegObject childNode;

	@Override public String toString() {
		return "" + this.sourcePosition + " " + this.msg;
	}
}

class Memo {
	PegObject result;
	int nextPosition;
}
