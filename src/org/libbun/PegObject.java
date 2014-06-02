package org.libbun;


public class PegObject {
	SourceToken    source;
	String      name;
	PegObject   AST[] = null;
	PegObject   parent;
	SymbolTable gamma = null;
	Functor     matched = null;
	MetaType    typed   = null;

	PegObject(String functor) {
		this.name = functor;
	}

	PegObject(String functor, BunSource source) {
		this.name = functor;
		  // created to store error position
		this.source = source.newToken(null, 0, 0);
	}

	public boolean is(String functor) {
		return this.name.startsWith(functor);
	}

	final boolean isErrorNode() {
		return this.name.equals(BunSymbol.PerrorFunctor);
	}

	final void setSource(Peg createdPeg, BunSource source, int startIndex, int endIndex) {
		this.source = source.newToken(createdPeg, startIndex, endIndex);
	}

	final void setMessage(Peg createdPeg, BunSource source, int startIndex, String message) {
		this.source = source.newToken(createdPeg, startIndex, startIndex, message);
	}

	final String getText() {
		if(this.source != null) {
			return this.source.getText();
		}
		return "NULL";
	}

	// AST[]
	
	public final int size() {
		if(this.AST == null) {
			return 0;
		}
		return this.AST.length;
	}

	public final PegObject get(int index) {
		return this.AST[index];
	}

	public final PegObject get(int index, PegObject defaultValue) {
		if(index < this.size()) {
			return this.AST[index];
		}
		return defaultValue;
	}

	public final void set(int index, PegObject node) {
		if(!(index < this.size())){
			this.expandAstToSize(index+1);
		}
		this.AST[index] = node;
	}

	//	public final void swap(int i, int j) {
	//		PegObject node = this.AST[i];
	//		this.AST[i] = this.AST[j];
	//		this.AST[j] = node;
	//	}

	public final void resizeAst(int size) {
		if(this.AST == null && size > 0) {
			this.AST = Main._NewPegObjectArray(size);
		}
		else if(this.AST.length != size) {
			PegObject[] newast = Main._NewPegObjectArray(size);
			if(size > this.AST.length) {
				Main._ArrayCopy(this.AST, 0, newast, 0, this.AST.length);
			}
			else {
				Main._ArrayCopy(this.AST, 0, newast, 0, size);
			}
			this.AST = newast;
		}
	}

	public final void expandAstToSize(int newSize) {
		if(newSize > this.size()) {
			this.resizeAst(newSize);
		}
	}

	void append(PegObject childNode) {
		int size = this.size();
		this.expandAstToSize(size+1);
		this.AST[size] = childNode;
		childNode.parent = this;
	}

	public final String getTextAt(int index, String defaultValue) {
		if(index < this.size()) {
			return this.AST[index].getText();
		}
		return defaultValue;
	}
	
	public MetaType getTypeAt(SymbolTable gamma, int index, MetaType defaultType) {
		if(index < this.size()) {
			PegObject node = this.AST[index];
			if(node.typed != null) {
				return node.typed;
			}
			if(node.matched == null && gamma != null) {
				gamma.tryMatch(node);
			}
			if(node.matched != null) {
				return node.getType(defaultType);
			}
		}
		return defaultType;
	}


	@Override
	public String toString() {
		SourceBuilder sb = new SourceBuilder(null);
		this.stringfy(sb);
		return sb.toString();
	}

	final void stringfy(SourceBuilder sb) {
		if(this.AST == null) {
			sb.appendNewLine(this.name+ ": ", this.getText(), "   " + this.info());
		}
		else {
			sb.appendNewLine(this.name);
			sb.openIndent(" {            " + this.info());
			for(int i = 0; i < this.size(); i++) {
				if(this.AST[i] != null) {
					this.AST[i].stringfy(sb);
				}
				else {
					sb.appendNewLine("@missing subnode at " + i);
				}
			}
			sb.closeIndent("}");
		}
	}

	private String info() {
		if(this.matched == null) {
			if(this.source != null) {
				return "## by peg : " + this.source.createdPeg;
			}
			return "";
		}
		else {
			return ":" + this.getType(null) + " by " + this.matched;
		}
	}
		
	public final SymbolTable getSymbolTable() {
		PegObject node = this;
		while(node.gamma == null) {
			node = node.parent;
		}
		return node.gamma;
	}

	public final PegObject findParentNode(String name) {
		PegObject node = this;
		while(node != null) {
			if(node.is(name)) {
				return node;
			}
			node = node.parent;
		}
		return null;
	}

	public void setName(String name, MetaType type, PegObject initValue) {
		if(this.gamma == null) {
			SymbolTable parent = this.getSymbolTable();
			this.gamma = new SymbolTable(parent.namespace);
		}
		this.gamma.setName(name, type, initValue);
	}

	public void setName(PegObject nameNode, MetaType type, PegObject initValue) {
		if(this.gamma == null) {
			SymbolTable parent = this.getSymbolTable();
			this.gamma = new SymbolTable(parent.namespace);
		}
		this.gamma.setName(nameNode, type, initValue);
	}
	
	public final MetaType getType(MetaType defaultType) {
		if(this.typed == null) {
			if(this.matched != null) {
				this.typed = this.matched.getReturnType(defaultType);
			}
			if(this.typed == null) {
				return defaultType;
			}
		}
		return this.typed;
	}

	public final void build(PegDriver driver) {
		if(this.matched != null) {
			this.matched.build(this, driver);
		}
		else {
			driver.pushUnknownNode(this);
		}
	}



}
