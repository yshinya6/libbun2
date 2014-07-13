package org.libbun.peg4d;

import java.util.HashMap;

public class PackratParserContext extends SimpleParserContext {

	public PackratParserContext(ParserSource source) {
		super(source);
	}

	private class Memo2 {
		Memo2 next;
		Peg  keypeg;
		int  consumed;
	}
	
	private HashMap<Long, Memo2> memoMap2;

	public void initMemo() {
		this.memoMap2 = new HashMap<Long, Memo2>();
	}
	
	public void clearMemo() {}

	private Memo2 UnusedMemo = null;

	private Memo2 newMemo() {
		if(UnusedMemo != null) {
			Memo2 m = this.UnusedMemo;
			this.UnusedMemo = m.next;
			return m;
		}
		else {
			Memo2 m = new Memo2();
			this.memoSize += 1;
			return m;
		}
	}

	private void freeMemo(Memo2 m) {
		m.next = this.UnusedMemo;
		this.UnusedMemo = m;
	}
	
	protected void setMemo(long keypos, Peg keypeg, int consumed) {
		Memo2 m = null;
		m = newMemo();
		m.keypeg = keypeg;
		m.consumed = consumed;
		m.next = this.memoMap2.get(keypos);
		this.memoMap2.put(keypos, m);
		this.memoMiss += 1;
//		if(keypeg == peg) {
//			System.out.println("cache " + keypos + ", " + keypeg);
//		}
	}

	protected Memo2 getMemo(Peg keypeg, long keypos) {
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
	
	

}
