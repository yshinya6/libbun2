package org.libbun.peg4d;

import java.util.HashMap;

import org.libbun.Main;
import org.libbun.UList;
import org.libbun.UMap;

public class ObjectParserContext extends SimpleParserContext {
	
	public ObjectParserContext(ParserSource source) {
		this(source, 0, source.length());
		this.initMemo();
	}

	public ObjectParserContext(ParserSource source, long startIndex, long endIndex) {
		super(source, startIndex, endIndex);
	}

	private boolean verifyMode = false;
	public final boolean isVerifyMode() {
		return this.verifyMode;
	}

	public final boolean startVerifyMode() {
		boolean verifyMode = this.verifyMode;
		this.verifyMode = true;
		return verifyMode;
	}
	
	public final void endVerifyMode(boolean verifyMode) {
		this.verifyMode = verifyMode;
	}

	private class Memo2 {
		Peg  keypeg;
		long pos;
		Peg createdPeg;
		Memo2 next;
	}
	
	private HashMap<Long, Memo2> memoMap2;

	public void initMemo() {
		this.memoMap2 = new HashMap<Long, Memo2>();
	}
	
	public void clearMemo() {}

	private Memo2 UnusedMemo = null;
	
	private void setMemo(Peg keypeg, long keypos, Peg peg, long pos) {
		Memo2 m = null;
		if(UnusedMemo != null) {
			m = this.UnusedMemo;
			this.UnusedMemo = m.next;
		}
		else {
			m = new Memo2();
			this.memoSize += 1;
		}
		m.keypeg = keypeg;
		m.pos = pos;
		m.createdPeg = peg;
		m.next = this.memoMap2.get(keypos);
		this.memoMap2.put(keypos, m);
		this.memoMiss += 1;
//		if(keypeg == peg) {
//			System.out.println("cache " + keypos + ", " + keypeg);
//		}
	}

	private Memo2 getMemo(Peg keypeg, long keypos) {
		Memo2 m = this.memoMap2.get(keypos);
		while(m != null) {
			if(m.keypeg == keypeg) {
				return m;
			}
			m = m.next;
		}
		return m;
	}

	public void removeMemo(long startIndex, long endIndex) {
		if(this.memoMap2 != null) {
			//System.out.println("remove = " + startIndex + ", " + endIndex);
			for(long i = startIndex; i < endIndex; i++) {
				Long key = i;
				Memo2 m = this.memoMap2.get(key);
				if(m != null) {
					appendMemo2(m, this.UnusedMemo);
					this.UnusedMemo = m;
					this.memoMap2.remove(key);
					//System.out.println("recycling pos=" + key);				
				}
			}
		}
	}

	private void appendMemo2(Memo2 m, Memo2 n) {
		while(m.next != null) {
			m = m.next;
		}
		m.next = n;
	}

	@Override
	public PegObject matchNewObject(PegObject left, PegNewObject e) {
		long startIndex = this.getPosition();
		Memo2 m = this.getMemo(e, startIndex);
		if(m != null) {
			this.memoHit += 1;
			if(m.createdPeg != e) {
				return this.refoundFailure(m.createdPeg, m.pos);
			}
			if(this.verifyMode) {
				left.startIndex = startIndex;
				this.setPosition(m.pos);  // comsume
				return left;
			}
		}
		else {
			boolean verifyMode = this.startVerifyMode();
			PegObject vnode = left;
			for(int i = 0; i < e.size(); i++) {
				vnode = e.get(i).performMatch(vnode, this);
				if(vnode.isFailure()) {
					this.rollback(startIndex);
					this.setMemo(e, startIndex, vnode.createdPeg, vnode.startIndex);
					this.endVerifyMode(verifyMode);
					return vnode;
				}
			}
			this.endVerifyMode(verifyMode);
			if(verifyMode) {
				this.setMemo(e, startIndex, e, this.getPosition());
				return vnode;
			}
			this.rollback(startIndex);
		}
		return super.matchNewObject(left, e);
	}

	@Override
	public PegObject matchSetter(PegObject left, PegSetter e) {
		if(!this.isVerifyMode()) {
			return super.matchSetter(left, e);
		}
		long startIndex = left.startIndex;
		PegObject node = e.innerExpr.performMatch(left, this);
		node.startIndex = startIndex;
		return node;
	}

	@Override
	public PegObject matchTag(PegObject left, PegTag e) {
		if(!this.isVerifyMode()) {
			return super.matchTag(left, e);
		}
		return left;
	}

	@Override
	public PegObject matchMessage(PegObject left, PegMessage e) {
		if(!this.isVerifyMode()) {
			return super.matchMessage(left, e);
		}
		return left;
	}

	@Override
	public PegObject matchPipe(PegObject left, PegPipe e) {
		if(!this.isVerifyMode()) {
			return super.matchPipe(left, e);
		}
		return left;
	}

}
