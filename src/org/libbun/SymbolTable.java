package org.libbun;


public class SymbolTable {
	Namespace          namespace = null;
	PegObject          scope     = null;
	UniMap<Functor>  symbolTable = null;

	public SymbolTable(Namespace namespace) {
		this.namespace = namespace;
	}

	@Override public String toString() {
		return "NS[" + this.scope + "]";
	}

	public final Namespace getNamespace() {
		return this.namespace;
	}

	public final SymbolTable getParentTable() {
		if(this.scope != null) {
			PegObject Node = this.scope.parent;
			while(Node != null) {
				if(Node.gamma != null) {
					return Node.gamma;
				}
				Node = Node.parent;
			}
		}
		return null;
	}

	public final void setSymbol(String key, Functor f) {
		if(this.symbolTable == null) {
			this.symbolTable = new UniMap<Functor>();
		}
		//		System.out.println("set key: " + key + "");
		this.symbolTable.put(key, f);
	}

	public final Functor getSymbol(String key) {
		SymbolTable table = this;
		while(table != null) {
			if(table.symbolTable != null) {
				Functor f = table.symbolTable.get(key, null);
				if(f != null) {
					return f;
				}
			}
			table = table.getParentTable();
		}
		//		System.out.println("unknown key: " + key);
		return null;
	}

	public void addFunctor(Functor f) {
		String key = f.key();
		Functor parent = this.getSymbol(key);
		f.nextChoice = parent;
		this.setSymbol(key, f);
		if(Main.EnableVerbose) {
			System.err.println("defined functor: " + f.name + ": " + f.funcType + " as " + key);
		}
	}
	
	public Functor getFunctor(PegObject node) {
		String key = node.name + ":" + node.size();
		Functor f = this.getSymbol(key);
		if(f != null) {
			return f;
		}
		key = node.name + "*";
		f = this.getSymbol(key);
		if(f == null && Main.EnableVerbose) {
			System.err.println("undefined functor: " + node.name + ":" + node.size());
		}
		return f;
	}

	public final boolean check(PegObject node, BunDriver driver) {
		if(node.matched == null) {
			Functor cur = this.getFunctor(node);
			while(cur != null) {
				boolean hasNextChoice = false;
				if(cur.nextChoice != null) {
					hasNextChoice = true;
				}
				cur.matchSubNode(node, hasNextChoice, driver);
				if(node.matched != null) {
					return true;
				}
				cur = cur.nextChoice;
			}
		}
		return false;
	}

	public final PegObject checkType(PegObject node, MetaType type, MetaType[] greekContext, boolean hasNextChoice, BunDriver driver) {
		this.check(node, driver);
		return type.match(node, true, greekContext);
	}

	class DefinedTypeFunctor extends Functor {
		public DefinedTypeFunctor(String name, MetaType type) {
			super(name, MetaType._LookupFuncType2(type));
		}

		@Override
		protected void matchSubNode(PegObject node, boolean hasNextChoice, BunDriver driver) {
			node.matched = this;
		}

		@Override
		public void build(PegObject node, BunDriver driver) {
			driver.pushTypeOf(node);
		}
	}

	public Functor setType(String name, MetaType type) {
		Functor defined = this.getSymbol(name);
		if(defined != null) {
			System.out.println("duplicated name:" + type);
		}
		defined = new DefinedTypeFunctor(name, type);
		this.setSymbol(name, defined);
		return defined;
	}

	public MetaType getType(String name, MetaType defaultType) {
		Functor f = this.getSymbol(name);
		if(f instanceof DefinedTypeFunctor) {
			return f.getReturnType(defaultType);
		}
		if(Main.EnableVerbose) {
			Main._PrintLine("FIXME: undefined type: " + name);
		}
		return defaultType;
	}

	public FuncType getFuncType(String returnTypeName) {
		MetaType r = this.getType(returnTypeName, MetaType.UntypedType);
		return MetaType._LookupFuncType2(r);
	}
	
	public void load(String fileName, BunDriver driver) {
		BunSource source = Main.loadSource(fileName);
		this.namespace.newParserContext(null, source);
		PegParserContext context =  this.namespace.newParserContext(null, source);
		while(context.hasNode()) {
			PegObject node = context.parsePegNode(new PegObject(BunSymbol.TopLevelFunctor), "TopLevel", false/*hasNextChoice*/);
			node.gamma = this;
			if(this.check(node, driver)) {
				node.build(driver);
			}
			else {
				driver.pushErrorMessage(node.source, "unmatched functor for " + node.name);
				Main._PrintLine(node.toString());
			}
		}
	}



}

