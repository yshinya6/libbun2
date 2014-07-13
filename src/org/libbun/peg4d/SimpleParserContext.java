package org.libbun.peg4d;

import org.libbun.Main;
import org.libbun.UCharset;
import org.libbun.UList;
import org.libbun.UMap;

public class SimpleParserContext extends ParserContext {
	private UMap<Peg>        pegCache;
	
	public SimpleParserContext(ParserSource source) {
		this(source, 0, source.length());
		this.initMemo();
	}

	public SimpleParserContext(ParserSource source, long startIndex, long endIndex) {
		super(source, startIndex, endIndex);
	}
	
	@Override
	public void setRuleSet(PegRuleSet ruleSet) {
		this.ruleSet = ruleSet;
		this.loadPegDefinition(ruleSet.pegMap);
	}

	public final void loadPegDefinition(UMap<Peg> pegMap) {
		this.pegCache = new UMap<Peg>();	
		UList<String> list = pegMap.keys();
		for(int i = 0; i < list.size(); i++) {
			String key = list.ArrayValues[i];
			Peg e = pegMap.get(key, null);
//			this.pegCache.put(key, e); // e.clone(new PegOptimizer()));
			this.pegCache.put(key, e.clone(new PegOptimizer()));
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
			if(e instanceof PegNot) {
				Peg inner = ((PegNot) e).innerExpr;
				if(inner instanceof PegString) {
					return new PegNotString(e, ((PegString) inner).symbol);
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
					if(ne instanceof PegNotString) {
						((PegNotString) ne).setNextAny(e);
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
		
	class PegNotString extends PegOptimized {
		String symbol;
		boolean nextAny = false;
		public PegNotString(Peg orig, String token) {
			super(orig);
			this.symbol = token;
		}
		public void setNextAny(Peg orig) {
			this.orig = orig;
			this.nextAny = true;
		}
		@Override
		public PegObject simpleMatch(PegObject left, ParserContext context) {
			long pos = context.getPosition();
			if(context.match(this.symbol)) {
				context.setPosition(pos);
				return context.foundFailure(this);
			}
			if(this.nextAny) {
				if(context.hasChar()) {
					context.consume(1);
				}
				else {
					return context.foundFailure(this);
				}
			}
			return left;
		}
	}

	class PegNotCharacter extends PegOptimized {
		UCharset charset;
		boolean nextAny = false;
		public PegNotCharacter(Peg orig, UCharset charset) {
			super(orig);
			this.charset = charset;
		}
		public void setNextAny(Peg orig) {
			this.orig = orig;
			this.nextAny = true;
		}
		@Override
		public PegObject simpleMatch(PegObject left, ParserContext context) {
			long pos = context.getPosition();
			if(context.match(this.charset)) {
				context.setPosition(pos);
				return context.foundFailure(this);
			}
			if(this.nextAny) {
				if(context.hasChar()) {
					context.consume(1);
				}
				else {
					return context.foundFailure(this);
				}
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
}

