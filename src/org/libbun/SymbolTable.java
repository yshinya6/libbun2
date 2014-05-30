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

	public final boolean check(PegObject node, PegDriver driver) {
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

	public final PegObject checkType(PegObject node, MetaType type, MetaType[] greekContext, boolean hasNextChoice, PegDriver driver) {
		this.check(node, driver);
		return type.match(node, true, greekContext);
	}

	class DefinedTypeFunctor extends Functor {
		public DefinedTypeFunctor(String name, MetaType type) {
			super(name, MetaType._LookupFuncType2(type));
		}

		@Override
		protected void matchSubNode(PegObject node, boolean hasNextChoice, PegDriver driver) {
			node.matched = this;
		}

		@Override
		public void build(PegObject node, PegDriver driver) {
			driver.pushType(node.getType(MetaType.UntypedType));
		}
	}

	public Functor setType(MetaType type) {
		String name = type.getName();
		Functor defined = this.getSymbol(type.getName());
		if(defined != null) {
			System.out.println("duplicated name:" + name);
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
	
	public void load(String fileName, PegDriver driver) {
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
				driver.report(node, "error", "unmatched functor for " + node.name);
			}
		}
	}

	// ======================================================================
	
	// #type

	class TypeFunctor extends Functor {
		public TypeFunctor(String name) {
			super(name, null);
		}

		@Override
		protected void matchSubNode(PegObject node, boolean hasNextChoice, PegDriver driver) {
			SymbolTable gamma = node.getSymbolTable();
			String typeName = node.getText();
//			System.out.println("type name " + typeName);
			node.matched = this;
			node.typed = gamma.getType(typeName, null);
		}

		@Override
		public void build(PegObject node, PegDriver driver) {
			driver.pushType(node.getType(MetaType.UntypedType));
		}
	}

//	class DefinedNameFunctor extends Functor {
//		private final int nameIndex;
//		public DefinedNameFunctor(String name, int nameIndex, MetaType type) {
//			super(name, MetaType._LookupFuncType2(type));
//			this.nameIndex = nameIndex;
//		}
//
//		@Override
//		protected void matchSubNode(PegObject node, boolean hasNextChoice, PegDriver driver) {
//			node.matched = this;
//		}
//
//		@Override
//		public void build(PegObject node, PegDriver driver) {
//			driver.pushName(this.name, this.nameIndex);
//		}
//	}

	class NameFunctor extends Functor {
		public NameFunctor(String name) {
			super(name, null);
		}

		@Override
		protected void matchSubNode(PegObject node, boolean hasNextChoice, PegDriver driver) {
			node.matched = this;
		}

		@Override
		public void build(PegObject node, PegDriver driver) {
			driver.pushGlobalName(node, node.getText());
		}
	}

	class FunctionFunctor extends Functor {
		public FunctionFunctor(String name) {
			super(name, null);
		}

		@Override
		protected void matchSubNode(PegObject node, boolean hasNextChoice, PegDriver driver) {
			System.out.println("node: " + node);
			node.matched = null;
		}

		@Override
		public void build(PegObject node, PegDriver driver) {
			//driver.pushLiteral(node, node.getText(), node.getType(MetaType.UntypedType));
		}
	}

	public void loadBunModel() {
		this.addFunctor(new TypeFunctor("#type"));
		this.addFunctor(new NameFunctor("#name"));

		this.addFunctor(new FunctionFunctor("#function"));
		this.addFunctor(new BunFunctor("#bun"));  // #bun
		this.addFunctor(new ErrorFunctor());

	}
	
	class LiteralFunctor extends Functor {
		public LiteralFunctor(String name, SymbolTable gamma, String typeName) {
			super(name, gamma.getFuncType(typeName));
		}

		@Override
		protected void matchSubNode(PegObject node, boolean hasNextChoice, PegDriver driver) {
			node.matched = this;
			node.typed = this.getReturnType(MetaType.UntypedType);
		}

		@Override
		public void build(PegObject node, PegDriver driver) {
			driver.pushRawLiteral(node, node.getText(), node.getType(MetaType.UntypedType));
		}
	}

	public void addRawLiteralFunctor(String name, String typeName) {
		this.addFunctor(new LiteralFunctor(name, this, typeName));
	}

	class NullFunctor extends LiteralFunctor {
		public NullFunctor(String name, SymbolTable gamma, String typeName) {
			super(name, gamma, typeName);
		}

		@Override
		public void build(PegObject node, PegDriver driver) {
			driver.pushNull(node);
		}
	}

	public void addNullFunctor(String name, String typeName) {
		this.addFunctor(new NullFunctor(name, this, typeName));
	}
	
	class TrueFunctor extends LiteralFunctor {
		public TrueFunctor(String name, SymbolTable gamma, String typeName) {
			super(name, gamma, typeName);
		}

		@Override
		public void build(PegObject node, PegDriver driver) {
			driver.pushTrue(node);
		}
	}

	public void addTrueFunctor(String name, String typeName) {
		this.addFunctor(new TrueFunctor(name, this, typeName));
	}

	class FalseFunctor extends LiteralFunctor {
		public FalseFunctor(String name, SymbolTable gamma, String typeName) {
			super(name, gamma, typeName);
		}

		@Override
		public void build(PegObject node, PegDriver driver) {
			driver.pushFalse(node);
		}
	}

	public void addFalseFunctor(String name, String typeName) {
		this.addFunctor(new FalseFunctor(name, this, typeName));
	}

	class IntegerFunctor extends LiteralFunctor {
		public IntegerFunctor(String name, SymbolTable gamma, String typeName) {
			super(name, gamma, typeName);
		}

		@Override
		public void build(PegObject node, PegDriver driver) {
			long n = UniCharset._ParseInt(node.getText());
			driver.pushInteger(node, n);
		}
	}

	public void addIntegerFunctor(String name, String typeName) {
		this.addFunctor(new IntegerFunctor(name, this, typeName));
	}

	class FloatFunctor extends LiteralFunctor {
		public FloatFunctor(String name, SymbolTable gamma, String typeName) {
			super(name, gamma, typeName);
		}

		@Override
		public void build(PegObject node, PegDriver driver) {
			double n = UniCharset._ParseFloat(node.getText());
			driver.pushFloat(node, n);
		}
	}

	public void addFloatFunctor(String name, String typeName) {
		this.addFunctor(new FloatFunctor(name, this, typeName));
	}

	class CharacterFunctor extends LiteralFunctor {
		boolean needsUnquote;
		public CharacterFunctor(String name, SymbolTable gamma, String typeName, boolean needsUnquote) {
			super(name, gamma, typeName);
			this.needsUnquote = needsUnquote;
		}

		@Override
		public void build(PegObject node, PegDriver driver) {
			String s = node.getText();
			if(this.needsUnquote) {
				s = UniCharset._UnquoteString(s);
			}
			driver.pushCharacter(node, s.charAt(0));
		}
	}

	public void addCharacterFunctor(String name, String typeName, boolean needsUnquote) {
		this.addFunctor(new CharacterFunctor(name, this, typeName, needsUnquote));
	}

	class StringFunctor extends LiteralFunctor {
		boolean needsUnquote;
		public StringFunctor(String name, SymbolTable gamma, String typeName, boolean needsUnquote) {
			super(name, gamma, typeName);
			this.needsUnquote = needsUnquote;
		}

		@Override
		public void build(PegObject node, PegDriver driver) {
			String s = node.getText();
			if(this.needsUnquote) {
				s = UniCharset._UnquoteString(s);
			}
			driver.pushString(node, s);
		}
	}

	public void addStringFunctor(String name, String typeName, boolean needsUnquote) {
		this.addFunctor(new StringFunctor(name, this, typeName, needsUnquote));
	}

}

