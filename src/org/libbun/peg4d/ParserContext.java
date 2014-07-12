package org.libbun.peg4d;

import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.libbun.Functor;
import org.libbun.Main;
import org.libbun.UCharset;
import org.libbun.UList;

public abstract class ParserContext {
	public final          ParserSource source;
	protected long        sourcePosition = 0;
	public    long        endPosition;
	public    PegRuleSet ruleSet = null;
	
	long backtrackCount = 0;
	long backtrackSize = 0;
	int  objectCount = 0;

	public ParserContext(ParserSource source, long startIndex, long endIndex) {
		this.source = source;
		this.sourcePosition = startIndex;
		this.endPosition = endIndex;
	}
	
	public abstract void setRuleSet(PegRuleSet ruleSet);
	public abstract PegObject parsePegObject(PegObject left, String key);

	protected final long getPosition() {
		return this.sourcePosition;
	}
	protected final void setPosition(long pos) {
		this.sourcePosition = pos;
	}
	protected final void rollback(long pos) {
		if(this.sourcePosition > pos) {
			this.backtrackCount = this.backtrackCount + 1;
			this.backtrackSize = this.backtrackSize + (this.sourcePosition - pos);
		}
		this.sourcePosition = pos;
	}

	@Override
	public final String toString() {
		if(this.endPosition > this.sourcePosition) {
			return this.source.substring(this.sourcePosition, this.endPosition);
		}
		return "";
	}

	public final boolean hasChar() {
		return this.sourcePosition < this.endPosition;
	}

	protected final char getChar() {
		if(this.hasChar()) {
			return this.source.charAt(this.sourcePosition);
		}
		return '\0';
	}

	protected final void consume(long plus) {
		this.sourcePosition = this.sourcePosition + plus;
	}

	protected final boolean match(char ch) {
		if(ch == this.getChar()) {
			this.consume(1);
			return true;
		}
		return false;
	}

	protected final boolean match(String text) {
		if(this.endPosition - this.sourcePosition >= text.length()) {
			for(int i = 0; i < text.length(); i++) {
				if(text.charAt(i) != this.source.charAt(this.sourcePosition + i)) {
					return false;
				}
			}
			this.consume(text.length());
			return true;
		}
		return false;
	}

	protected final boolean match(UCharset charset) {
		if(charset.match(this.getChar())) {
			this.consume(1);
			return true;
		}
		return false;
	}
	
	protected final long matchZeroMore(UCharset charset) {
		for(;this.hasChar(); this.consume(1)) {
			char ch = this.source.charAt(this.sourcePosition);
			if(!charset.match(ch)) {
				break;
			}
		}
		return this.sourcePosition;
	}
	
	public final String formatErrorMessage(String msg1, String msg2) {
		return this.source.formatErrorMessage(msg1, this.sourcePosition, msg2);
	}

	public final void showPosition(String msg) {
		showPosition(msg, this.getPosition());
	}

	public final void showPosition(String msg, long pos) {
		System.out.println(this.source.formatErrorMessage("debug", pos, msg));
	}

	public final void showErrorMessage(String msg) {
		System.out.println(this.source.formatErrorMessage("error", this.sourcePosition, msg));
		Main._Exit(1, msg);
	}

	
	
	public boolean hasNode() {
		this.matchZeroMore(UCharset.WhiteSpaceNewLine);
		return this.sourcePosition < this.endPosition;
	}

	public PegObject parseNode(String key) {
		PegObject o = this.parsePegObject(new PegObject("#toplevel"), key);
		if(o.isFailure()) {
			o = this.newErrorObject();
		}
		return o;
	}
	
	public final PegObject newPegObject(String name) {
		PegObject node = new PegObject(name, this.source, null, this.sourcePosition);
		this.objectCount = this.objectCount + 1;
		return node;
	}
	
	protected final PegObject foundFailureNode = new PegObject(null, this.source, null, 0);

	public final PegObject newErrorObject() {
		PegObject node = newPegObject("#error");
		node.createdPeg = this.foundFailureNode.createdPeg;
		node.startIndex = this.foundFailureNode.startIndex;
		node.matched = Functor.ErrorFunctor;
		return node;
	}
	
	public final PegObject foundFailure(Peg created) {
		if(this.sourcePosition >= this.foundFailureNode.startIndex) {  // adding error location
			this.foundFailureNode.startIndex = this.sourcePosition;
			this.foundFailureNode.createdPeg = created;
		}
		return this.foundFailureNode;
	}

	public final PegObject refoundFailure(Peg created, long pos) {
		this.foundFailureNode.startIndex = pos;
		this.foundFailureNode.createdPeg = created;
		return this.foundFailureNode;
	}

	public final PegObject matchString(PegObject left, PegString e) {
		if(this.match(e.symbol)) {
			return left;
		}
		return this.foundFailure(e);
	}

	public final PegObject matchCharacter(PegObject left, PegCharacter e) {
		char ch = this.getChar();
		if(!e.charset.match(ch)) {
			return this.foundFailure(e);
		}
		this.consume(1);
		return left;
	}

	public final PegObject matchAny(PegObject left, PegAny e) {
		if(this.hasChar()) {
			this.consume(1);
			return left;
		}
		return this.foundFailure(e);
	}

	public PegObject matchLabel(PegObject left, PegLabel e) {
		return this.parsePegObject(left, e.symbol);
	}

	public PegObject matchOptional(PegObject left, PegOptional e) {
		ObjectLog marker = this.pushNewMarker();
		PegObject parsedNode = e.innerExpr.performMatch(left, this);
		if(parsedNode.isFailure()) {
			this.disposeObjectAndEraseFailure(marker);
			return left;
		}
		this.popBack(marker);
		return parsedNode;
	}

	public PegObject matchRepeat(PegObject left, PegRepeat e) {
		PegObject prevNode = left;
		int count = 0;
		while(this.hasChar()) {
			ObjectLog marker = this.pushNewMarker();
			PegObject node = e.innerExpr.performMatch(prevNode, this);
			if(node.isFailure()) {
				if(count < e.atleast) {					
					this.popBack(marker);			
					return node;
				}
				this.disposeObjectAndEraseFailure(marker);
				break;
			}
			prevNode = node;
			this.popBack(marker);			
			//System.out.println("startPostion=" + startPosition + ", current=" + this.getPosition() + ", count = " + count);
			if(!(marker.pos < this.getPosition())) {
				if(count < e.atleast) {
					return this.foundFailure(e);
				}
				break;
			}
			count = count + 1;
		}
		return prevNode;
	}

	public PegObject matchAnd(PegObject left, PegAnd e) {
		PegObject node = left;
		ObjectLog marker = this.pushNewMarker();
		node = e.innerExpr.performMatch(node, this);
		this.disposeObject(marker);
		return node;
	}

	public PegObject matchNot(PegObject left, PegNot e) {
		PegObject node = left;
		ObjectLog marker = this.pushNewMarker();
		node = e.innerExpr.performMatch(node, this);
		this.disposeObject(marker);
		if(node.isFailure()) {
			return left;
		}
		return this.foundFailure(e);
	}

	public PegObject matchSequence(PegObject left, PegSequence e) {
		ObjectLog marker = this.pushNewMarker();
		for(int i = 0; i < e.size(); i++) {
			PegObject parsedNode = e.get(i).performMatch(left, this);
			if(parsedNode.isFailure()) {
				this.disposeObject(marker);
				return parsedNode;
			}
			left = parsedNode;
		}
		this.popBack(marker);
		return left;
	}

	public PegObject matchChoice(PegObject left, PegChoice e) {
		PegObject node = left;
		for(int i = 0; i < e.size(); i++) {
			ObjectLog marker = this.pushNewMarker();
			node = e.get(i).performMatch(left, this);
			if(!node.isFailure()) {
				this.popBack(marker);
				break;
			}
			this.disposeObject(marker);
		}
		return node;
	}
	
	private class ObjectLog {
		ObjectLog next;
		long pos;
		Object leftNodeOrErrorPeg;
		int  index;
		PegObject childNode;
	}

	ObjectLog logStack = null;
	ObjectLog unusedLog = null;
	int usedLog = 0;
	int maxLog  = 0;
	
	private ObjectLog newLog() {
		if(this.unusedLog == null) {
			maxLog = maxLog + 1;
			return new ObjectLog();
		}
		ObjectLog l = this.unusedLog;
		this.unusedLog = l.next;
		l.next = null;
		return l;
	}
	
	private void push(ObjectLog l) {
		l.next = this.logStack;
		this.logStack = l;
	}
	
	protected final ObjectLog pushNewMarker() {
		ObjectLog l = this.newLog();
		l.pos = this.sourcePosition;
		l.leftNodeOrErrorPeg = this.foundFailureNode.createdPeg;
		l.index = (int)(this.foundFailureNode.startIndex - l.pos);
		l.childNode  = null;
		this.push(l);
		return l;
	}
	
	protected final void pushSetter(PegObject parentNode, int index, PegObject childNode) {
		ObjectLog l = this.newLog();
		l.pos = this.sourcePosition;
		l.index = index;
		l.leftNodeOrErrorPeg = parentNode;
		l.childNode  = childNode;
		this.push(l);
	}

	protected final void popBack(ObjectLog marker, boolean isDispose) {
		ObjectLog unused = this.logStack;
		ObjectLog cur = this.logStack;
		while(cur != null) {
			if(cur == marker) {
				this.logStack = marker.next; 
				marker.next = this.unusedLog;
				this.unusedLog = unused;
				break;
			}
			if(!isDispose && cur.childNode != null) {
				return;
			}
			cur.leftNodeOrErrorPeg = null;
			cur.childNode = null;
			cur = cur.next;
		}
	}

	protected final void popBack(ObjectLog marker) {
		popBack(marker, false);
	}

	protected final void disposeObject(ObjectLog marker) {
		popBack(marker, true);
		this.rollback(marker.pos);
	}

	protected final void disposeObjectAndEraseFailure(ObjectLog marker) {
		popBack(marker, true);
		this.rollback(marker.pos);
		this.foundFailureNode.createdPeg = (Peg)marker.leftNodeOrErrorPeg;
		this.foundFailureNode.startIndex = marker.pos + marker.index;
	}

	protected final void popNewObject(PegObject newnode, ObjectLog marker) {
		UList<ObjectLog> entryList = new UList<ObjectLog>(new ObjectLog[8]);
		ObjectLog unused = this.logStack;
		ObjectLog cur = this.logStack;
		while(cur != null) {
			if(cur == marker) {
				this.logStack = marker.next; 
				marker.next = this.unusedLog;
				this.unusedLog = unused;
				break;
			}
			if(cur.leftNodeOrErrorPeg == newnode) {
				entryList.add(cur);
			}
			cur = cur.next;
		}
		for(int i = entryList.size() - 1; i >= 0; i--) {
			ObjectLog l = entryList.ArrayValues[i];
			if(l.index == -1) {
				newnode.append(l.childNode);
			}
			else {
				newnode.set((int)l.pos, l.childNode);
			}
			if(l.childNode.is("#lazy")) {
				this.parseLazyObject(l.childNode);
			}
			l.childNode = null;
		}
		newnode.setSource(marker.pos, this.getPosition());
		newnode.checkNullEntry();
		entryList = null;
	}
	
	public PegObject matchNewObject(PegObject left, PegNewObject e) {
		PegObject leftNode = left;
		ObjectLog marker = this.pushNewMarker();
		PegObject newnode = this.newPegObject(e.nodeName);
		newnode.setSource(e, this.source, marker.pos);
		if(e.leftJoin) {
			this.pushSetter(newnode, -1, leftNode);
		}
		for(int i = 0; i < e.size(); i++) {
			PegObject node = e.get(i).performMatch(newnode, this);
			if(node.isFailure()) {
				this.disposeObject(marker);
				return node;
			}
			//			if(node != newnode) {
			//				e.warning("dropping @" + newnode.name + " " + node);
			//			}
		}
		this.popNewObject(newnode, marker);
		return newnode;
	}

	public PegObject matchSetter(PegObject left, PegSetter e) {
		PegObject node = e.innerExpr.performMatch(left, this);
		if(node.isFailure() || left == node) {
			return node;
		}
		this.pushSetter(left, e.index, node);
		return left;
	}

	public PegObject matchTag(PegObject left, PegTag e) {
		left.tag = e.symbol;
		return left;
	}

	public PegObject matchMessage(PegObject left, PegMessage e) {
		left.optionalToken = e.symbol;
		left.startIndex = this.getPosition();
		return left;
	}

	public PegObject matchPipe(PegObject left, PegPipe e) {
		left.tag = "#lazy";
		left.optionalToken = e.symbol;
		if(left.source instanceof FileSource) {
			left.source = left.source.trim(left.startIndex, left.startIndex + left.length);
			left.startIndex = 0;
			left.length = (int)left.source.length();
		}
		return left;
	}

	public final ParserContext newParserContext(ParserSource source, long startIndex, long endIndex) {
		ParserContext p = new SimpleParserContext(source, startIndex, endIndex);
		p.setRuleSet(this.ruleSet);
		return p;
	}
	
	private ExecutorService parserPool = null;
	private int taskCount = 0;
	public void parseLazyObject(PegObject left) {
		if(parserPool == null) {
			parserPool = Executors.newFixedThreadPool(4);
		}
		parserPool.execute(new Task(taskCount, this, left));
		taskCount += 1;
	}
	
	class Task implements Runnable {
		int taskId;
		ParserContext parser;
		PegObject left;
		Task(int taskId, ParserContext parser, PegObject left) {
			this.taskId = taskId;
			this.parser = parser;
			this.left = left;
		}
		public void run(){
			try {
				PegObject parent = left.parent;
				ParserContext sub = parser.newParserContext(left.source, left.startIndex, left.startIndex + left.length);
				if(Main.VerbosePegMode) {
					//sub.beginStatInfo();
					//System.out.println("[" + this.taskId + "] start parsing: " + left);
				}
				PegObject newone = sub.parseNode(left.optionalToken);
				parent.replace(left, newone);
				if(Main.VerbosePegMode) {
					//System.out.println("[" + this.taskId + "] end parsing: " + newone);
					//sub.endStatInfo(newone);
				}
				sub = null;
			}
			catch (RuntimeException e) {
				e.printStackTrace();
			}
			this.parser = null;
			this.left = null;
	   }
	}
	
	public PegObject matchIndent(PegObject left, PegIndent e) {
		if(left.source != null) {
			String indent = left.source.getIndentText(left.startIndex);
			//System.out.println("###" + indent + "###");
			if(this.match(indent)) {
				return left;
			}
		}
		return left;
	}

	public PegObject matchCatch(PegObject left, PegCatch e) {
		e.innerExpr.performMatch(left, this);
		return left;
	}

	public void initMemo() {
		// TODO Auto-generated method stub
		
	}
	
	protected int memoHit = 0;
	protected int memoMiss = 0;
	protected int memoSize = 0;

	long timer = 0;
	long usedMemory;
	
	public void beginStatInfo() {
		System.gc(); // meaningless ?
		this.backtrackSize = 0;
		this.backtrackCount = 0;
		long total = Runtime.getRuntime().totalMemory() / 1024;
		long free =  Runtime.getRuntime().freeMemory() / 1024;
		usedMemory =  total - free;
		timer = System.currentTimeMillis();
	}

	public void endStatInfo(PegObject parsedObject) {
		long awaitTime = 0;
		if(this.parserPool != null) {
			this.parserPool.shutdown();
			awaitTime = System.currentTimeMillis();
			try {
				this.parserPool.awaitTermination(60000, TimeUnit.MILLISECONDS);
				awaitTime = (System.currentTimeMillis() - awaitTime);
				this.parserPool = null;
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		timer = (System.currentTimeMillis() - timer);
		System.gc(); // meaningless ?
		if(Main.VerbosePegMode) {
//			System.out.println("parsed:\n" + parsedObject);
//			if(this.hasChar()) {
//				System.out.println("** uncosumed: '" + this.source + "' **");
//			}
			System.gc(); // meaningless ?
			long length = this.getPosition();
			System.out.println();
			System.out.println("erapsed time: " + timer + " msec" + " awaitTime: " + awaitTime + " msec IO: " + this.source.statIOCount);
			System.out.println("length: " + length + ", consumed: " + this.getPosition() + ", length/backtrack: " + (double)this.backtrackSize / length);
			System.out.println("backtrack: size= " + this.backtrackSize + " count=" + this.backtrackCount + " average=" + (double)this.backtrackSize / this.backtrackCount);
			System.out.println("created_object: " + this.objectCount + ", used_object: " + parsedObject.count() + " object_logs: " + maxLog);
			System.out.println("hit: " + this.memoHit + ", miss: " + this.memoMiss + ", consumed memo:" + this.memoSize);
			long total = Runtime.getRuntime().totalMemory() / 1024;
			long free =  Runtime.getRuntime().freeMemory() / 1024;
			long used =  total - free;
			System.out.println("heap: " + used + "KiB/ " + (used/1024) + "MiB");
			used =  used - usedMemory;
			length = length / 1024;
			System.out.println("used: " + used + "KiB/ " + (used/1024) + "MiB,  heap/length: " + (double) used/ length);
			System.out.println();
		}
	}
	
}

