package org.libbun;


public class SymbolTable {
	Namespace          namespace = null;
	PegObject          scope     = null;
	UniMap<Functor>    symbolTable = null;

	public SymbolTable(Namespace namespace) {
		this.namespace = namespace;
	}

	@Override public String toString() {
		return "NS[" + this.scope + "]";
	}

	public final Namespace getNamespace() {
		return this.namespace;
	}
	
	public final void report(PegObject node, String errorType, String msg) {
		if(this.namespace.driver != null) {
			this.namespace.driver.report(node, errorType, msg);
		}
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

	public final Functor getLocalSymbol(String name) {
		if(this.symbolTable != null) {
			return this.symbolTable.get(name, null);
		}
		return null;
	}
	
	public final Functor getSymbol(String name) {
		SymbolTable table = this;
		while(table != null) {
			if(table.symbolTable != null) {
				Functor f = table.symbolTable.get(name, null);
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

	public final boolean tryMatch(PegObject node) {
		if(node.matched == null) {
			Functor cur = this.getFunctor(node);
			while(cur != null) {
				boolean hasNextChoice = false;
				if(cur.nextChoice != null) {
					hasNextChoice = true;
				}
				cur.matchSubNode(node, hasNextChoice);
				if(node.matched != null) {
					return true;
				}
				cur = cur.nextChoice;
			}
		}
		return false;
	}

	public final boolean checkTypeAt(PegObject node, int index, MetaType type, MetaType[] greekContext, boolean hasNextChoice) {
		PegObject subnode = node.get(index);
		if(!this.tryMatch(subnode)) {
			if(hasNextChoice) {
				return false;
			}
			return false;
		}
		MetaType valueType = node.getType(MetaType.UntypedType);
		if(type.is(valueType, greekContext)) {
			return true;
		}
		subnode.typed = this.getTypeCoersion(valueType, type, hasNextChoice);
		if(subnode.typed != null) {
			return true;
		}
		return false;
	}
	
	private MetaType getTypeCoersion(MetaType sourceType, MetaType targetType, boolean hasNextChoice) {
		if(hasNextChoice) {
			return null;
		}
		return null;
	}



	class DefinedTypeFunctor extends Functor {
		public DefinedTypeFunctor(String name, MetaType type) {
			super(name, MetaType.newFuncType(type));
		}

		@Override
		protected void matchSubNode(PegObject node, boolean hasNextChoice) {
			node.matched = this;
		}

		@Override
		public void build(PegObject node, PegDriver driver) {
			driver.pushType(node.getType(MetaType.UntypedType));
		}
	}

	class DefinedNameFunctor extends DefinedTypeFunctor {
		public PegObject initValue;
		public DefinedNameFunctor(String name, MetaType type, PegObject initValue) {
			super(name, type);
			this.initValue = initValue;
		}
		@Override
		public void build(PegObject node, PegDriver driver) {
			driver.pushLocalName(node, this.name);
		}
	}

	class DefinedGlobalNameFunctor extends DefinedNameFunctor {
		public DefinedGlobalNameFunctor(String name, MetaType type, PegObject initValue) {
			super(name, type, initValue);
		}
		@Override
		public void build(PegObject node, PegDriver driver) {
			driver.pushGlobalName(node, this.name);
		}
	}

	private Functor setName(boolean GlobalName, String name, PegObject nameNode, MetaType type, PegObject initValue) {
		Functor defined = this.getLocalSymbol(name);
		if(defined != null && nameNode != null) {
			this.report(nameNode, "notice", "duplicated name: " + name);
		}
		if(GlobalName) {
			defined = new DefinedGlobalNameFunctor(name, type, initValue);
			if(Main.EnableVerbose) {
				Main._PrintLine(name + " :" + type.getName() + " initValue=" + initValue);
			}
		}
		else {
			defined = new DefinedNameFunctor(name, type, initValue);
		}
		this.setSymbol(name, defined);
		return defined;
	}

	public Functor setName(String name, MetaType type, PegObject initValue) {
		return this.setName(false, name, null, type, initValue);
	}

	public Functor setName(PegObject nameNode, MetaType type, PegObject initValue) {
		return this.setName(false, nameNode.getText(), nameNode, type, initValue);
	}

	public Functor setGlobalName(PegObject nameNode, MetaType type, PegObject initValue, PegDriver driver) {
		return this.setName(true, nameNode.getText(), nameNode, type, initValue);
	}
	
	public DefinedNameFunctor getName(String name) {
		Functor f = this.getSymbol(name);
		if(f instanceof DefinedNameFunctor) {
			return (DefinedNameFunctor)f;
		}
		return null;
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
		return MetaType.newFuncType(r);
	}
	
	public void load(String fileName, PegDriver driver) {
		BunSource source = Main.loadSource(fileName);
		this.namespace.newParserContext(null, source);
		PegParserContext context =  this.namespace.newParserContext(null, source);
		while(context.hasNode()) {
			PegObject node = context.parsePegNode(new PegObject(BunSymbol.TopLevelFunctor), "TopLevel", false/*hasNextChoice*/);
			node.gamma = this;
			if(this.tryMatch(node)) {
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
		protected void matchSubNode(PegObject node, boolean hasNextChoice) {
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

	class NameFunctor extends Functor {
		public NameFunctor(String name) {
			super(name, null);
		}

		@Override
		protected void matchSubNode(PegObject node, boolean hasNextChoice) {
			SymbolTable gamma = node.getSymbolTable();
			String name = node.getText();
			node.matched = gamma.getName(name);
			if(node.matched == null) {
				node.matched = this;
			}
		}

		@Override
		public void build(PegObject node, PegDriver driver) {
			driver.pushUndefinedName(node, node.getText());
		}
	}

	
	class VarFunctor extends Functor {
		public VarFunctor(String name) {
			super(name, null);
		}

		@Override
		protected void matchSubNode(PegObject node, boolean hasNextChoice) {
			System.out.println("node: " + node);
			SymbolTable gamma = node.getSymbolTable();
			String name = node.getTextAt(0, null);
			MetaType type = node.getTypeAt(gamma, 1, MetaType.UntypedType);
			gamma.checkTypeAt(node, 2, type, null, hasNextChoice);
			type = node.getTypeAt(gamma, 2, null);
			node.matched = this;
		}

		@Override
		public void build(PegObject node, PegDriver driver) {
			
		}
	}
	

	class FunctionFunctor extends Functor {
		public FunctionFunctor(String name) {
			super(name, null);
		}

		@Override
		protected void matchSubNode(PegObject node, boolean hasNextChoice) {
			System.out.println("node: " + node);
			this.setFuncType(node);
			node.matched = this;
		}

		private void setFuncType(PegObject node) {
			SymbolTable gamma = node.getSymbolTable();
			if(node.typed != null) {
				VarType	inf = new VarType("Func");
				PegObject params = node.get(1, null);
				PegObject block = node.get(3, null);
				for(int i = 0; i < params.size(); i++) {
					PegObject p = params.get(i);
					MetaType ptype = p.getTypeAt(gamma, 1, null);
					ptype = inf.newVarType(p.getTypeAt(gamma, 1, null));
					if(block != null) {
						block.setName(p.get(0), ptype, null);
					}
				}
				MetaType returnType = inf.newVarType(node.getTypeAt(gamma, 2, null));
				node.typed = inf.getRealType();
				if(block != null) {
					block.setName(node.get(0), node.typed, node);
					block.setName("return", returnType, null);
				}
			}
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
		protected void matchSubNode(PegObject node, boolean hasNextChoice) {
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

