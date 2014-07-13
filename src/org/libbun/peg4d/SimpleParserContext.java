package org.libbun.peg4d;

import java.util.HashMap;

import org.libbun.Main;
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
			this.checkLeftRecursion(key, e);
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

	public final Peg getRule(String name) {
		return this.pegCache.get(name, null);
	}

	private final Peg getRightJoinRule(String name) {
		return this.pegCache.get(this.nameRightJoinName(name), null);
	}

	public final PegObject parsePegObject(PegObject parentNode, String ruleName) {
		Peg e = this.getRule(ruleName);
		PegObject left = e.performMatch(parentNode, this);
		if(left.isFailure()) {
			return left;
		}
		e = this.getRightJoinRule(ruleName);
		if(e != null) {
			return e.performMatch(left, this);
		}
		return left;
	}


}
