package org.libbun.peg4d;

import org.libbun.Functor;
import org.libbun.UCharset;

public abstract class ParserContext extends SourceContext {
	public PegRuleSet ruleSet = null;
	public int objectCount = 0;

	public ParserContext(ParserSource source, long startIndex, long endIndex) {
		super(source, startIndex, endIndex);
	}
	
	public abstract void setRuleSet(PegRuleSet ruleSet);
	public boolean hasNode() {
		this.matchZeroMore(UCharset.WhiteSpaceNewLine);
		return this.sourcePosition < this.endPosition;
	}

	public PegObject parseNode(String key) {
		return this.parsePegObject(new PegObject("#toplevel"), key);
	}

	public abstract void initMemo();
	public abstract void clearMemo();
	public void removeMemo(long startIndex, long endIndex) {
	}

	public abstract PegObject parsePegObject(PegObject inNode, String key);
	
	public final PegObject newPegObject(String name) {
		PegObject node = new PegObject(name, this.source, null, this.sourcePosition);
		this.objectCount = this.objectCount + 1;
		return node;
	}
	
	public final PegObject newErrorObject() {
		PegObject node = newPegObject("#error");
		node.createdPeg = this.storeFailurePeg();
		node.startIndex = this.storeFailurePosition();
		node.matched = Functor.ErrorFunctor;
		return node;
	}

	protected final PegObject foundFailureNode = new PegObject(null, this.source, null, 0);
	
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

	public final Peg storeFailurePeg() {
		return this.foundFailureNode.createdPeg;
	}
	public final long storeFailurePosition() {
		return this.foundFailureNode.startIndex;
	}
	public final void restoreFailure(Peg created, long pos) {
		this.foundFailureNode.createdPeg = created;
		this.foundFailureNode.startIndex   = pos;
	}

	public abstract int getStackPosition(Peg peg);
	public abstract void popBack(int stackPosition, boolean backtrack);
	public abstract void push(Peg peg, PegObject parentNode, int index, PegObject node);
	public abstract void addSubObject(PegObject newnode, int stack, int top);
	
	public abstract void showStatInfo(PegObject node);

	public boolean isVerifyMode() {
		return false;
	}

	public PegObject precheck(PegNewObject peg, PegObject in) {
		return null;  // if prechecked
	}
}
