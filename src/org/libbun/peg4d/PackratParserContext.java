package org.libbun.peg4d;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class PackratParserContext extends SimpleParserContext {

	public PackratParserContext(ParserSource source) {
		super(source);
	}

	private class Memo2 {
		Memo2 next;
		Peg  keypeg;
		PegObject generated;
		int  consumed;
	}
	private final static int FifoSize = 100;
	private Map<Long, Memo2> memoMap2;

	public void initMemo() {
		this.memoMap2 = new LinkedHashMap<Long, Memo2>(FifoSize) {  //FIFO
			private static final long serialVersionUID = 6725894996600788028L;
			@Override
	        protected boolean removeEldestEntry(Map.Entry<Long, Memo2> eldest)  {
				if(this.size() > FifoSize) {
					//System.out.println("removed pos="+eldest.getKey());
					Memo2 m = eldest.getValue();
					this.appendMemo2(m, UnusedMemo);
					UnusedMemo = m;
					return true;			
				}
	            return false;
	        }
			private void appendMemo2(Memo2 m, Memo2 n) {
				while(m.next != null) {
					m = m.next;
				}
				m.next = n;
			}			
	    };
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
	
	protected void setMemo(long keypos, Peg keypeg, PegObject generated, int consumed) {
		Memo2 m = null;
		m = newMemo();
		m.keypeg = keypeg;
		m.generated = generated;
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
				this.memoHit += 1;
				return m;
			}
			m = m.next;
		}
		return m;
	}

//	public void removeMemo(long startIndex, long endIndex) {
//		if(this.memoMap2 != null) {
//			//System.out.println("remove = " + startIndex + ", " + endIndex);
//			for(long i = startIndex; i < endIndex; i++) {
//				Long key = i;
//				Memo2 m = this.memoMap2.get(key);
//				if(m != null) {
//					appendMemo2(m, this.UnusedMemo);
//					this.UnusedMemo = m;
//					this.memoMap2.remove(key);
//					//System.out.println("recycling pos=" + key);				
//				}
//			}
//		}
//	}
//
	
	public PegObject matchNewObject(PegObject left, PegNewObject e) {
		long pos = this.getPosition();
		Memo2 m = this.getMemo(e, pos);
		if(m != null) {
			if(m.generated == null) {
				return this.refoundFailure(e, pos+m.consumed);
			}
			setPosition(pos + m.consumed);
			return m.generated;
		}
		PegObject generated = super.matchNewObject(left, e);
		if(generated.isFailure()) {
			this.setMemo(pos, e, null, (int)(generated.startIndex - pos));
		}
		else {
			this.setMemo(pos, e, generated, (int)(this.getPosition() - pos));
		}
		return generated;
	}


}
