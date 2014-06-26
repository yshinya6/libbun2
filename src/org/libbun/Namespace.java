package org.libbun;

public class Namespace extends SymbolTable {
	public UniMap<PegRuleSet> ruleMap;
	public UniArray<String>   exportSymbolList;
	public BunDriver  driver;

	public Namespace(BunDriver driver) {
		super(null);
		this.root = this;
		this.ruleMap = new UniMap<PegRuleSet>();
		PegRuleSet pegRule = new PegRuleSet();
		pegRule.loadPegRule();
		this.ruleMap.put("peg", pegRule);
//		this.ruleMap.put("main", ruleSet);
		this.driver = driver;
	}
	
	public final String toString() {
		return "root";
	}

	public void importFrom(Namespace ns) {
		for(int i = 0; i < ns.exportSymbolList.size(); i++) {
			String symbol = ns.exportSymbolList.ArrayValues[i];
			this.setSymbol(symbol, ns.getSymbol(symbol));
		}
	}

	public void initParserRuleSet(ParserContext context, String lang) {
		if(lang == null) {
			lang = this.guessLang(context.source.fileName, "bun");
		}
		PegRuleSet ruleSet = this.getRuleSet(lang);
		context.setRuleSet(ruleSet);
	}

	private String guessLang(String fileName, String defaultValue) {
		if(fileName != null) {
			int loc = fileName.lastIndexOf(".");
			if(loc > 0) {
				return fileName.substring(loc+1);
			}
		}
		return defaultValue;
	}

	public final PegRuleSet getRuleSet(String lang) {
		PegRuleSet p = this.ruleMap.get(lang);
		if(p == null) {
			p = new PegRuleSet();
			p.loadPegFile("lib/peg/" + lang + ".peg");
			this.ruleMap.put(lang, p);
		}
		return p;
	}



}
