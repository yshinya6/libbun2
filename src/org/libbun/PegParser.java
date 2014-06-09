package org.libbun;

public final class PegParser {
	UniMap<Peg>           pegMap;
	UniMap<Peg>           pegCache = null;
	UniMap<String>        objectLabelMap = null;
	boolean enableMemo = false;

	public PegParser() {
		this.initParser();
	}
	public void initParser() {
		this.pegMap = new UniMap<Peg>();
	}

	public PegParserContext newContext(PegSource source, int startIndex, int endIndex) {
		return new PegParserContext(this, source, startIndex, endIndex);
	}

	public PegParserContext newContext(PegSource source) {
		return new PegParserContext(this, source, 0, source.sourceText.length());
	}

	public final boolean loadPegFile(String fileName) {
		PegSource source = Main.loadSource(fileName);
		PegParserParser p = new PegParserParser(this, source);
		while(p.hasRule()) {
			p.parseRule();
		}
		this.resetCache();
		return true;
	}
	
	void importPeg(String label, String fileName) {
		if(Main.PegDebuggerMode) {
			System.out.println("importing " + fileName);
		}
		PegParser p = new PegParser();
		p.loadPegFile(fileName);
		UniArray<String> list = p.makeList(label);
		String prefix = "";
		int loc = label.indexOf(":");
		if(loc > 0) {
			prefix = label.substring(0, loc+1);
			label = label.substring(loc+1);
			this.pegMap.put(label, new PegLabel(label, prefix+label));
		}
		for(int i = 0; i < list.size(); i++) {
			String l = list.ArrayValues[i];
			Peg e = p.getDefinedPeg(l);
			this.pegMap.put(prefix + l, e.clone(prefix));
		}
	}
	
	public void setPegRule(String name, Peg e) {
		Peg checked = this.checkPegRule(name, e);
		if(checked != null) {
			this.pegMap.put(name, checked);
			this.pegCache = null;
		}
	}

	private Peg checkPegRule(String name, Peg e) {
		if(e instanceof PegChoice) {
			PegChoice newnode = new PegChoice();
			for(int i = 0; i < e.size(); i++) {
				newnode.add( this.checkPegRule(name, e.get(i)));
			}
			if(newnode.size() == 1) {
				return newnode.get(0);
			}
			return newnode;
		}
		if(e instanceof PegLabel) {  // self reference
			if(name.equals(((PegLabel) e).symbol)) {
				Peg defined = this.pegMap.get(name, null);
				if(defined == null) {
					e.warning("undefined self reference: " + name);
				}
//				System.out.println("name " + name + ", " + ((PegLabel) e).symbol + " " + defined);
				return defined;
			}
		}
		return e;
	}

	public final void resetCache() {
		this.initCache();
		boolean noerror = true;
		UniArray<String> list = this.pegMap.keys();
		for(int i = 0; i < list.size(); i++) {
			String key = list.ArrayValues[i];
			Peg e = this.pegMap.get(key, null);
			if(!e.verify(this)) {
				noerror = false;
			}
			this.removeLeftRecursion(key, e);
//			if(Main.pegDebugger) {
//				System.out.println(e.toPrintableString(key));
//			}
		}
		list = this.pegCache.keys();
		for(int i = 0; i < list.size(); i++) {
			String key = list.ArrayValues[i];
			Peg e = this.pegCache.get(key, null);
			if(Main.PegDebuggerMode) {
				System.out.println(e.toPrintableString(key, "\n  = ", "\n  / ", "\n  ;", true));
			}
			if(!e.verify(this)) {
				noerror = false;
			}
		}
		if(!noerror) {
			Main._Exit(1, "peg error found");
		}
		//		for(int i = 0; i < list.size(); i++) {
		//			String key = list.ArrayValues[i];
		//			Peg e = this.pegMap.GetValue(key, null);
		//			this.setFirstCharCache(key, e);
		//		}
		//		list = this.keywordCache.keys();
		//		for(int i = 0; i < list.size(); i++) {
		//			String key = list.ArrayValues[i];
		//			//System.out.println("keyword: " + key);
		//		}
		//		list = this.firstCharCache.keys();
		//		for(int i = 0; i < list.size(); i++) {
		//			String key = list.ArrayValues[i];
		//			//System.out.println("cache: '" + key + "'");
		//		}
		//		list = this.pegCache.keys();
		//		for(int i = 0; i < list.size(); i++) {
		//			String key = list.ArrayValues[i];
		//			Peg e = this.pegCache.GetValue(key, null);
		//			//System.out.println("" + key + " <- " + e);
		//		}
	}
	
	private void initCache() {
		this.pegCache = new UniMap<Peg>();
		this.objectLabelMap = new UniMap<String>();
	}

	public void addObjectLabel(String objectLabel) {
		this.objectLabelMap.put(objectLabel, objectLabel);
	}

	private void removeLeftRecursion(String name, Peg e) {
		if(e instanceof PegChoice) {
			for(int i = 0; i < e.size(); i++) {
				this.removeLeftRecursion(name, e.get(i));
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
						String key = this.nameRightJoinName(name);  // left recursion
						this.appendPegCache(key, seq.cdr());
						return;
					}
					else {
						Peg left = this.pegMap.get(label, null);
						if(this.hasLabel(name, left)) {
							String key = this.nameRightJoinName(label);  // indirect left recursion
							this.appendPegCache(key, seq.cdr());
							return;
						}
					}
				}
			}
		}
		this.appendPegCache(name, e);
	}

	String nameRightJoinName(String key) {
		return "+" + key;
	}

	private boolean hasLabel(String name, Peg e) {
		if(e instanceof PegChoice) {
			for(int i = 0; i < e.size(); i++) {
				if(this.hasLabel(name, e.get(i))) {
					return true;
				}
			}
			return false;
		}
		if(e instanceof PegLabel) {
			String label = ((PegLabel) e).symbol;
			if(name.equals(label)) {
				return true;
			}
			e = this.pegMap.get(label, null);
			return this.hasLabel(name, e);
		}
		return false;
	}
	
	private void appendPegCache(String name, Peg e) {
		Peg defined = this.pegCache.get(name, null);
		if(defined != null) {
			e = defined.appendAsChoice(e);
		}
		this.pegCache.put(name, e);
	}

	public final boolean hasPattern(String name) {
		return this.pegMap.get(name, null) != null;
	}

	public Peg getDefinedPeg(String name) {
		return this.pegMap.get(name, null);
	}
	
	public final Peg getPattern(String name, char firstChar) {
		if(this.pegCache == null) {
			this.resetCache();
		}
		return this.pegCache.get(name, null);
	}

	public final Peg getRightPattern(String name, char firstChar) {
		if(this.pegCache == null) {
			this.resetCache();
		}
		return this.getPattern(this.nameRightJoinName(name), firstChar);
	}
	
	public final UniArray<String> makeList(String startPoint) {
		UniArray<String> list = new UniArray<String>(new String[100]);
		UniMap<String> set = new UniMap<String>();
		Peg e = this.getDefinedPeg(startPoint);
		if(e != null) {
			list.add(startPoint);
			set.put(startPoint, startPoint);
			e.makeList(this, list, set);
		}
		return list;
	}
	
	public final void show(String startPoint) {
		UniArray<String> list = makeList(startPoint);
		for(int i = 0; i < list.size(); i++) {
			String name = list.ArrayValues[i];
			Peg e = this.getDefinedPeg(name);
			String rule = e.toPrintableString(name, "\n  = ", "\n  / ", "\n  ;", true);
			System.out.println(rule);
		}
	}
	

}


