package org.libbun;

public class SimpleParserContext extends ParserContext {
	private UniMap<Peg>        pegCache;
	private UniMap<SimpleMemo> memoMap = new UniMap<SimpleMemo>();
	
	class SimpleMemo {
		PegObject result;
		int nextPosition;
	}

	final UniArray<SimpleLog> logStack = new UniArray<SimpleLog>(new SimpleLog[128]);
	private int stackTop = 0;

	class SimpleLog {
		int sourcePosition;
		Peg trace;
		char type;
		PegObject parentNode;
		int index;
		PegObject childNode;
	}
	
	int memoHit = 0;
	int memoMiss = 0;
	int memoSize = 0;

	public SimpleParserContext(PegSource source) {
		this(source, 0, source.sourceText.length());
	}

	public SimpleParserContext(PegSource source, int startIndex, int endIndex) {
		super(source, startIndex, endIndex);
	}
	
	@Override
	public void setRuleSet(PegRuleSet ruleSet) {
		this.loadPegDefinition(ruleSet.pegMap);
	}

	public final void loadPegDefinition(UniMap<Peg> pegMap) {
		this.pegCache = new UniMap<Peg>();	
		UniArray<String> list = pegMap.keys();
		for(int i = 0; i < list.size(); i++) {
			String key = list.ArrayValues[i];
			Peg e = pegMap.get(key, null);
			this.checkLeftRecursion(key, e);
		}
		list = this.pegCache.keys();
		for(int i = 0; i < list.size(); i++) {
			String key = list.ArrayValues[i];
			Peg e = this.pegCache.get(key, null);
//			if(Main.PegDebuggerMode) {
//				System.out.println(e.toPrintableString(key, "\n  = ", "\n  / ", "\n  ;", true));
//			}
		}
	}
	
	private void checkLeftRecursion(String name, Peg e) {
		if(e instanceof PegChoice) {
			for(int i = 0; i < e.size(); i++) {
				this.checkLeftRecursion(name, e.get(i));
			}
			return;
		}
		if(e instanceof PegSequence) {
			PegSequence seq = (PegSequence)e;
			if(seq.size() > 1) {
				Peg first = seq.get(0);
				if(first instanceof PegLabel) {
					String label = ((PegLabel) first).symbol;
					if(label.equals(name)) {
						//this.lrExistence = true;
						String key = this.nameRightJoinName(name);  // left recursion
						this.appendPegCache(key, seq.cdr());
						return;
					}
					// indirect left recursion has not supported
//					else {
//						Peg left = this.pegMap.get(label, null);
//						if(this.hasLabel(name, left)) {
//							String key = this.nameRightJoinName(label);  // indirect left recursion
//							this.appendPegCache(key, seq.cdr());
//							return;
//						}
//					}
				}
			}
		}
		this.appendPegCache(name, e);
	}
	
	private String nameRightJoinName(String name) {
		return name + "+";
	}

	private void appendPegCache(String name, Peg e) {
		Peg defined = this.pegCache.get(name, null);
		if(defined != null) {
			e = defined.appendAsChoice(e);
		}
		this.pegCache.put(name, e);
	}
		
	@Override
	public SourceContext subContext(int startIndex, int endIndex) {
		return new SimpleParserContext(this.source, startIndex, endIndex);
	}
		
	public void initMemo() {
		this.memoMap = new UniMap<SimpleMemo>();
	}
	
	public UniMap<SimpleMemo> getMemoMap() {
		return memoMap;
	}
	
	public final Peg getRule(String name) {
		return this.pegCache.get(name, null);
	}

	private final Peg getRightJoinRule(String name) {
		return this.pegCache.get(this.nameRightJoinName(name), null);
	}

	public final PegObject parsePegObject(PegObject parentNode, String ruleName) {
		Peg e = this.getRule(ruleName);
		PegObject left = e.debugMatch(parentNode, this);
		if(left.isFailure()) {
			return left;
		}
		e = this.getRightJoinRule(ruleName);
		if(e != null) {
			return e.debugMatch(left, this);
		}
		return left;
	}

	// memo has bugs
	public final PegObject parsePegNode2(PegObject parentNode, String pattern) {
		int pos = this.getPosition();
		String key = pattern + ":" + pos;
		SimpleMemo m = this.memoMap.get(key, null);
		if(m == null) {
			m = new SimpleMemo();
			m.nextPosition = this.getPosition();
			m.result = foundFailureNode;
			this.memoMap.put(key, m);
			this.memoMiss = this.memoMiss + 1;
			Peg e = this.getRule(pattern);
			PegObject ans = e.debugMatch(parentNode, this);
			if(pos == this.getPosition() && !ans.isFailure()) {
				this.memoMap.remove(key);
				return ans;
			}
			else {
				m.result = ans;
				m.nextPosition = this.getPosition();
//				if(getLrExistence(pattern)) {
//					return growLR(pattern, pos, m, parentNode);
//				}
//				else {
				return ans;
//				}
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
	
	public final int getStackPosition(Peg trace) {
		this.pushImpl(trace, null, '\0', null, 0, null);
		return this.stackTop;
	}

	private void pushImpl(Peg trace, String msg, char type, PegObject parentNode, int index, PegObject childNode) {
		SimpleLog log = null;
		if(this.stackTop < this.logStack.size()) {
			if(this.logStack.ArrayValues[this.stackTop] == null) {
				this.logStack.ArrayValues[this.stackTop] = new SimpleLog();
			}
			log = this.logStack.ArrayValues[this.stackTop];
		}
		else {
			log = new SimpleLog();
			this.logStack.add(log);
		}
		log.trace = trace;
		log.sourcePosition = this.sourcePosition;
		log.type = type;
		log.parentNode = parentNode;
		log.index = index;
		log.childNode = childNode;
		this.stackTop = this.stackTop + 1;
	}

	void pushLog(Peg trace, String msg) {
		this.pushImpl(trace, msg, 'm', null, 0, null);
	}

	@Override
	public void addSubObject(PegObject newnode, int stack, int top) {
		for(int i = stack; i < top; i++) {
			SimpleLog log = this.logStack.ArrayValues[i];
			if(log.type == 'p' && log.parentNode == newnode) {
				if(log.index == -1) {
					newnode.append(log.childNode);
				}
				else {
					newnode.set(log.index, log.childNode);
				}
			}
		}
	}

	public void popBack(int stackPostion, boolean backtrack) {
		this.stackTop = stackPostion-1;
		SimpleLog log = this.logStack.ArrayValues[stackPostion-1];
		if(backtrack) {
			this.rollback(log.sourcePosition);
		}
	}

	public void push(Peg trace, PegObject parentNode, int index, PegObject node) {
		this.pushImpl(trace, "", 'p', parentNode, index, node);
	}
	
	public void showStatInfo(PegObject parsedObject) {
		System.out.println("hit: " + this.memoHit + ", miss: " + this.memoMiss);
		System.out.println("created_object=" + this.objectCount + ", used_object=" + parsedObject.count());
		System.out.println("backtrackCount: " + this.backtrackCount + ", backtrackLength: " + this.backtrackSize);
		System.out.println();
	}

}
