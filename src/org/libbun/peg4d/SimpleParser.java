package org.libbun.peg4d;

import java.util.LinkedHashMap;
import java.util.Map;

import org.libbun.Main;
import org.libbun.UCharset;
import org.libbun.UList;
import org.libbun.UMap;

public class SimpleParser extends ParserContext {
	protected UList<Peg>       pegList;
	private UMap<Peg>        pegCache;
	
	public SimpleParser(ParserSource source) {
		this(source, 0, source.length(), null);
		this.initMemo();
	}

	public SimpleParser(ParserSource source, long startIndex, long endIndex, UList<Peg> pegList) {
		super(source, startIndex, endIndex);
		if(pegList != null) {
			this.pegList = pegList;
			this.pegCache = new UMap<Peg>();
			for(int i = 0; i < pegList.size(); i++) {
				Peg e = pegList.ArrayValues[i];
				this.pegCache.put(e.ruleName, e);
			}
		}
		this.initMemo();
	}
	
	public ParserContext newParserContext(ParserSource source, long startIndex, long endIndex) {
		return new SimpleParser(source, startIndex, endIndex, this.pegList);
	}

	
	@Override
	public void setRuleSet(PegRuleSet ruleSet) {
		this.ruleSet = ruleSet;
		this.pegCache = new UMap<Peg>();
		this.pegList = new UList<Peg>(new Peg[this.ruleSet.pegMap.size()]);
		UList<String> list = ruleSet.pegMap.keys();
		for(int i = 0; i < list.size(); i++) {
			String key = list.ArrayValues[i];
			Peg e = ruleSet.pegMap.get(key, null);
			e.ruleName = key;
			//this.pegCache.put(key, e);
			this.pegCache.put(key, e.clone(new PegOptimizer()));
			this.pegList.add(e);
		}
	}
	
	class PegOptimizer extends PegTransformer {
		public Peg transform(Peg e) {
			Peg ne = this.transformImpl(e);
			if(ne != null) {
//				if(Main.VerbosePegMode) {
//					System.out.println("optimized: " + ne);
//				}
				ne.ruleName = e.ruleName;
			}
			return ne;
		}
		public Peg transformImpl(Peg e) {
			if(e instanceof PegString) {
				String symbol = ((PegString) e).symbol;
				if(symbol.length() == 1) {
					return new PegString1(e, symbol);
				}
				if(symbol.length() == 0) {
					return new PegEmpty(e);
				}
				return null;
			}
			if(e instanceof PegNot) {
				Peg inner = ((PegNot) e).innerExpr;
				if(inner instanceof PegString) {
					return this.newNotString(e, ((PegString) inner).symbol);
				}
				if(inner instanceof PegCharacter) {
					return new PegNotCharacter(e, ((PegCharacter) inner).charset);
				}
			}
			if(e instanceof PegOptional) {
				Peg inner = ((PegOptional) e).innerExpr;
				if(inner instanceof PegString) {
					return new PegOptionalString(e, ((PegString) inner).symbol);
				}
				if(inner instanceof PegCharacter) {
					return new PegOptionalCharacter(e, ((PegCharacter) inner).charset);
				}
			}
			if(e instanceof PegRepeat) {
				PegRepeat re = (PegRepeat)e;
				Peg inner = re.innerExpr;
				if(inner instanceof PegCharacter) {
					UCharset charset = ((PegCharacter) inner).charset;
					Peg ne = null;
					if(re.atleast == 1) {
						ne = new PegOneMoreCharacter(e, charset);
					}
					else {
						ne = new PegZeroMoreCharacter(e, charset);
					}
					return ne;
				}
			}
			if(e instanceof PegSequence) {
				PegSequence seq = (PegSequence)e;
				if(seq.size() == 2 && seq.get(1) instanceof PegAny) {
					Peg ne = this.transform(seq.get(0));
					if(ne instanceof PegNotAtom) {
						((PegNotAtom) ne).setNextAny(e);
						return ne;
					}
					if(ne instanceof PegNotCharacter) {
						((PegNotCharacter) ne).setNextAny(e);
						return ne;
					}
				}
			}
			return null;
		}
		private final Peg newNotString(Peg orig, String token) {
			if(token.length() == 1) {
				return new PegNotString1(orig, token);
			}
			return new PegNotString(orig, token);
		}
	}
	
	private boolean isCreation(Peg e) {
		if(e instanceof PegNewObject) {
			return true;
		}
		if(e instanceof PegOptional || e instanceof PegRepeat) {
			return this.isCreation(((PegUnary)e).innerExpr);
		}
		if(e instanceof PegAnd || e instanceof PegNot) {
			return this.isCreation(((PegUnary)e).innerExpr);
		}
		if(e instanceof PegList) {
			PegList l = ((PegList)e);
			boolean hasCreation = false;
			for(int i = 0; i < l.size(); i++) {
				if(isCreation(l.get(i))) {
					hasCreation = true;
				}
			}
			if(hasCreation) {
				
			}
			return hasCreation;
		}
		return false;
//		if(e instanceof PegString || e instanceof PegCharacter || e instanceof PegAny) {
//			return true;
//		}
//		return false;
	}
	
	
	
//	private void appendPegCache(String name, Peg e) {
//		Peg defined = this.pegCache.get(name, null);
//		if(defined != null) {
//			e = defined.appendAsChoice(e);
//		}
//		this.pegCache.put(name, e);
//	}

	public final Peg getRule(String name) {
		return this.pegCache.get(name, null);
	}

//	private final Peg getRightJoinRule(String name) {
//		return this.pegCache.get(this.nameRightJoinName(name), null);
//	}

	public final PegObject parsePegObject(PegObject parentNode, String ruleName) {
		Peg e = this.getRule(ruleName);
		PegObject left = e.performMatch(parentNode, this);
//		if(left.isFailure()) {
//			return left;
//		}
//		e = this.getRightJoinRule(ruleName);
//		if(e != null) {
//			return e.performMatch(left, this);
//		}
		return left;
	}
	
	class PegEmpty extends PegOptimized {
		public PegEmpty(Peg orig) {
			super(orig);
		}
		@Override
		public PegObject simpleMatch(PegObject left, ParserContext context) {
			return left;
		}
	}

	class PegString1 extends PegOptimized {
		char symbol;
		public PegString1(Peg orig, String token) {
			super(orig);
			this.symbol = token.charAt(0);
		}
		@Override
		public PegObject simpleMatch(PegObject left, ParserContext context) {
			long pos = context.getPosition();
			if(context.charAt(pos) == this.symbol) {
				context.consume(1);
				return left;
			}
			return context.foundFailure(this);
		}
	}
	
	abstract class PegNotAtom extends PegOptimized {
		boolean nextAny = false;
		public PegNotAtom(Peg orig) {
			super(orig);
		}
		public final void setNextAny(Peg orig) {
			this.orig = orig;
			this.nextAny = true;
		}
		protected final PegObject matchNextAny(PegObject left, ParserContext context) {
			if(context.hasChar()) {
				context.consume(1);
				return left;
			}
			else {
				return context.foundFailure(this);
			}
		}
	}

	class PegNotString extends PegNotAtom {
		String symbol;
		public PegNotString(Peg orig, String token) {
			super(orig);
			this.symbol = token;
		}
		@Override
		public PegObject simpleMatch(PegObject left, ParserContext context) {
			long pos = context.getPosition();
			if(context.match(this.symbol)) {
				context.setPosition(pos);
				return context.foundFailure(this);
			}
			if(this.nextAny) {
				return this.matchNextAny(left, context);
			}
			return left;
		}
	}

	class PegNotString1 extends PegNotAtom {
		char symbol;
		public PegNotString1(Peg orig, String token) {
			super(orig);
			this.symbol = token.charAt(0);
		}
		@Override
		public PegObject simpleMatch(PegObject left, ParserContext context) {
			if(this.symbol == context.getChar()) {
				return context.foundFailure(this);
			}
			if(this.nextAny) {
				return this.matchNextAny(left, context);
			}
			return left;
		}
	}	
	
	class PegNotCharacter extends PegNotAtom {
		UCharset charset;
		public PegNotCharacter(Peg orig, UCharset charset) {
			super(orig);
			this.charset = charset;
		}
		@Override
		public PegObject simpleMatch(PegObject left, ParserContext context) {
			long pos = context.getPosition();
			if(context.match(this.charset)) {
				context.setPosition(pos);
				return context.foundFailure(this);
			}
			if(this.nextAny) {
				return this.matchNextAny(left, context);
			}
			return left;
		}
	}

	class PegOptionalString extends PegOptimized {
		String symbol;
		public PegOptionalString(Peg orig, String token) {
			super(orig);
			this.symbol = token;
		}
		@Override
		public PegObject simpleMatch(PegObject left, ParserContext context) {
			context.match(this.symbol);
			return left;
		}
	}

	class PegOptionalCharacter extends PegOptimized {
		UCharset charset;
		public PegOptionalCharacter(Peg orig, UCharset charset) {
			super(orig);
			this.charset = charset;
		}
		@Override
		public PegObject simpleMatch(PegObject left, ParserContext context) {
			context.match(this.charset);
			return left;
		}
	}
	
	class PegOneMoreStream extends PegOptimized {
		Peg repeated;
		public PegOneMoreStream(Peg orig, Peg repeated) {
			super(orig);
			this.repeated = repeated;
		}
		@Override
		public PegObject simpleMatch(PegObject left, ParserContext context) {
			long pos = context.getPosition();
			PegObject node = this.repeated.simpleMatch(left, context);
			if(node.isFailure()) {
				context.setPosition(pos);
				return node;
			}
			left = node;
			while(context.hasChar()) {
				pos = context.getPosition();
				node = this.repeated.simpleMatch(left, context);
				if(node.isFailure() || pos == context.getPosition()) {
					context.setPosition(pos);
					break;
				}
				left = node;
			}
			return left;
		}
	}

	class PegZeroMoreStream extends PegOptimized {
		Peg repeated;
		public PegZeroMoreStream(Peg orig, Peg repeated) {
			super(orig);
			this.repeated = repeated;
		}
		@Override
		public PegObject simpleMatch(PegObject left, ParserContext context) {
			while(context.hasChar()) {
				long pos = context.getPosition();
				PegObject node = this.repeated.simpleMatch(left, context);
				if(node.isFailure() || pos == context.getPosition()) {
					context.setPosition(pos);
					break;
				}
				left = node;
			}
			return left;
		}
	}

	
	class PegOneMoreCharacter extends PegOptimized {
		UCharset charset;
		public PegOneMoreCharacter(Peg orig, UCharset charset) {
			super(orig);
			this.charset = charset;
		}
		@Override
		public PegObject simpleMatch(PegObject left, ParserContext context) {
			long pos = context.getPosition();
			char ch = context.charAt(pos);
			if(!this.charset.match(ch)) {
				return context.foundFailure(this);
			}
			pos++;
			for(;context.hasChar();pos++) {
				ch = context.charAt(pos);
				if(!this.charset.match(ch)) {
					break;
				}
			}
			context.setPosition(pos);
			return left;
		}
	}

	class PegZeroMoreCharacter extends PegOptimized {
		UCharset charset;
		public PegZeroMoreCharacter(Peg orig, UCharset charset) {
			super(orig);
			this.charset = charset;
		}
		@Override
		public PegObject simpleMatch(PegObject left, ParserContext context) {
			long pos = context.getPosition();
			for(;context.hasChar();pos++) {
				char ch = context.charAt(pos);
				if(!this.charset.match(ch)) {
					break;
				}
			}
			context.setPosition(pos);
			return left;
		}
	}
	
	protected final static int FifoSize = 100;

	public final class ObjectMemo {
		ObjectMemo next;
		Peg  keypeg;
		PegObject generated;
		int  consumed;
	}

	protected Map<Long, ObjectMemo> memoMap;
	private ObjectMemo UnusedMemo = null;

	public void initMemo() {
	}

	private final ObjectMemo newMemo() {
		if(UnusedMemo != null) {
			ObjectMemo m = this.UnusedMemo;
			this.UnusedMemo = m.next;
			return m;
		}
		else {
			ObjectMemo m = new ObjectMemo();
			this.memoSize += 1;
			return m;
		}
	}
	
	protected final void setMemo(long keypos, Peg keypeg, PegObject generated, int consumed) {
		ObjectMemo m = null;
		m = newMemo();
		m.keypeg = keypeg;
		m.generated = generated;
		m.consumed = consumed;
		m.next = this.memoMap.get(keypos);
		this.memoMap.put(keypos, m);
		this.memoMiss += 1;
//		if(keypeg == peg) {
//			System.out.println("cache " + keypos + ", " + keypeg);
//		}
	}

	protected final ObjectMemo getMemo(Peg keypeg, long keypos) {
		ObjectMemo m = this.memoMap.get(keypos);
		while(m != null) {
			if(m.keypeg == keypeg) {
				this.memoHit += 1;
				return m;
			}
			m = m.next;
		}
		return m;
	}
	
	protected final void unusedMemo(ObjectMemo m) {
		this.appendMemo2(m, UnusedMemo);
		UnusedMemo = m;
	}
	
	private void appendMemo2(ObjectMemo m, ObjectMemo n) {
		while(m.next != null) {
			m = m.next;
		}
		m.next = n;
	}			
	
}

