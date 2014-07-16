package org.libbun.peg4d;

import java.util.HashMap;
import java.util.LinkedHashMap;

import org.libbun.UList;

public class PackratParser extends RecursiveDecentParser {

	public PackratParser(ParserSource source) {
		super(source);
	}
	
	private PackratParser(ParserSource source, long startIndex, long endIndex, UList<Peg> pegList) {
		super(source, startIndex, endIndex, pegList);
	}

	public ParserContext newParserContext(ParserSource source, long startIndex, long endIndex) {
		return new PackratParser(source, startIndex, endIndex, this.pegList);
	}


	public void initMemo() {
//		this.memoMap = new LinkedHashMap<Long, ObjectMemo>(FifoSize) {  //FIFO
//			private static final long serialVersionUID = 6725894996600788028L;
//			@Override
//	        protected boolean removeEldestEntry(Map.Entry<Long, ObjectMemo> eldest)  {
//				if(this.size() > FifoSize) {
//					//System.out.println("removed pos="+eldest.getKey());
//					unusedMemo(eldest.getValue());
//					return true;			
//				}
//	            return false;
//	        }
//	    };
		this.memoMap = new HashMap<Long, ObjectMemo>();
	}
	
	public PegObject matchLabel(PegObject left, PegLabel e) {
		long pos = this.getPosition();
		ObjectMemo m = this.getMemo(e, pos);
		if(m != null) {
			if(m.generated == null) {
				return this.refoundFailure(e, pos+m.consumed);
			}
			setPosition(pos + m.consumed);
			return m.generated;
		}
		PegObject generated = super.matchLabel(left, e);
		if(generated.isFailure()) {
			this.setMemo(pos, e, null, (int)(generated.startIndex - pos));
		}
		else {
			this.setMemo(pos, e, generated, (int)(this.getPosition() - pos));
		}
		return generated;
	}

}
