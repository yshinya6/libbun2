package org.libbun;

import org.libbun.peg4d.Peg;

public class BunTag {
	public  Peg    createdPeg = null;
	public  int    tagId;
	public  String tagName;
	public  String optionalToken = null;
	Functor matched = null;
	BunType typed   = null;

	private BunTag(int tagId, String tagName) {
		this.tagId = tagId;
		this.tagName = tagName;
	}
	
	private static UMap<BunTag> pooled = new UMap<BunTag>();
	
	static {
		newBunTag("!");       // tagId = 0;   Failure
		newBunTag("|");       // tagId = 1;   Pipe
	}
	
	public final static int id(String tagName) {
		BunTag t = pooled.get(tagName);
		if(t == null) {
			return -1;
		}
		return t.tagId;
	}
	
	public final static BunTag newBunTag(String tagName) {
		BunTag t = pooled.get(tagName);
		if(t == null) {
			t = new BunTag(pooled.size(), tagName);
			pooled.put(tagName, t);
		}
		return new BunTag(t.tagId, tagName);
	}
	
	// method 

	public final boolean isFailure() {
		return this.tagId == 0;
	}

	public final boolean isPipe() {
		return this.tagId == 1;
	}
	
	public final String getName() {
		return this.tagName;
	}
	
	public final boolean isMatched() {
		return this.matched != null;
	}
	
	public final boolean isUntyped() {
		return this.typed == null || this.typed == BunType.UntypedType;
	}

	public final BunType getType() {
		if(this.typed == null) {
			if(this.matched != null) {
				this.typed = this.matched.getReturnType(BunType.UntypedType);
			}
			if(this.typed == null) {
				return BunType.UntypedType;
			}
		}
		return this.typed;
	}

	
	
}
