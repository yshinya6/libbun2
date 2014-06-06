package org.libbun;


public class SymbolTable {
	Namespace          root = null;
	PegObject          scope     = null;
	UniMap<Functor>    symbolTable = null;

	public SymbolTable(Namespace namespace) {
		this.root = namespace;
	}

	SymbolTable(Namespace root, PegObject node) {
		this.root = root;
		this.set(node);
	}
	
	public void set(PegObject node) {
		node.gamma = this;
		this.scope = node;
	}
	
	public final Namespace getNamespace() {
		return this.root;
	}
	
	public final void report(PegObject node, String errorType, String msg) {
		if(this.root.driver != null) {
			this.root.driver.report(node, errorType, msg);
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
		this.symbolTable.put(key, f);
		f.storedTable = this;
		//System.out.println("SET gamma " + this + ", name = " + key);
	}

	public final Functor getLocalSymbol(String name) {
		if(this.symbolTable != null) {
			return this.symbolTable.get(name, null);
		}
		return null;
	}
	
	public final Functor getSymbol(String name) {
		SymbolTable gamma = this;
		while(gamma != null) {
			Functor f = gamma.getLocalSymbol(name);
			//System.out.println("GET gamma " + gamma + ", name=" + name + " f=" + f);
			if(f != null) {
				return f;
			}
			gamma = gamma.getParentTable();
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
			System.err.println("defined functor: " + f.name + ": " + f.funcType + " as " + key + " in " + this);
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
//		if(f == null && Main.EnableVerbose) {
//			Main._PrintLine("SymbolTable.getFunctor(): not found " + node.name + ":" + node.size());
//		}
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
				//System.out.println("trying " + cur.funcType);
				cur.matchSubNode(node, hasNextChoice);
				if(node.matched != null) {
					return true;
				}
				cur = cur.nextChoice;
			}
			return false;
		}
		return true;
	}

	public final boolean checkTypeAt(PegObject node, int index, MetaType type, MetaType[] greekContext, boolean hasNextChoice) {
		if(type == MetaType.UntypedType) {  // UntypedType does not invoke tryMatch()
			return true;
		}
		if(index < node.size()) {
			PegObject subnode = node.get(index);
			if(!this.tryMatch(subnode)) {
				if(Main.EnableVerbose) {
					Main._PrintLine("mismatched: " + subnode.name + ":" + node.size() + " as " + type.getName());
				}
				return false;
			}
			MetaType valueType = subnode.getType(MetaType.UntypedType);
			//System.out.println("index="+index + ", type="+type + ", valueType="+valueType + "node=" + node);
			if(type.is(valueType, greekContext)) {
				return true;
			}
			subnode.typed = this.getTypeCoersion(valueType, type, hasNextChoice);
			if(subnode.typed != null) {
				return true;
			}
			return false;
		}
		else {
			MetaType valueType = this.getVoidType();
			return type.is(valueType, greekContext);
		}
	}
		
	private MetaType getTypeCoersion(MetaType sourceType, MetaType targetType, boolean hasNextChoice) {
		String key = MetaType.keyTypeRel("#coercion", sourceType, targetType);
		Functor f = this.getSymbol(key);
		if(f != null) {
			if(Main.EnableVerbose) {
				Main._PrintLine("found type coercion from " + sourceType + " to " + targetType);
			}
			return MetaType.newTransType(key, sourceType, targetType, f);
		}
		if(hasNextChoice) {
			return null;
		}
		return null;
	}

	class DefinedTypeFunctor extends Functor {
		public DefinedTypeFunctor(String name, MetaType type) {
			super(name, false, type);
		}

		@Override
		protected void matchSubNode(PegObject node, boolean hasNextChoice) {
			node.matched = this;
		}

		@Override
		public void build(PegObject node, BunDriver driver) {
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
		public void build(PegObject node, BunDriver driver) {
			driver.pushLocalName(node, this.name);
		}
	}

	class DefinedGlobalNameFunctor extends DefinedNameFunctor {
		public DefinedGlobalNameFunctor(String name, MetaType type, PegObject initValue) {
			super(name, type, initValue);
		}
		@Override
		public void build(PegObject node, BunDriver driver) {
			driver.pushGlobalName(node, this.name);
		}
	}

	class DefinedFunctionFunctor extends DefinedNameFunctor {
		public DefinedFunctionFunctor(String name, MetaType type, PegObject funcNode, Functor defined) {
			super(name, type, funcNode);
			this.nextChoice = defined;
		}
		@Override
		public void build(PegObject node, BunDriver driver) {
			driver.pushApplyNode(node, name);
		}
	}
	
	private Functor setName(boolean GlobalName, String name, PegObject nameNode, MetaType type, PegObject initValue) {
		Functor defined = this.getLocalSymbol(name);
		if(defined != null && nameNode != null) {
			this.report(nameNode, "notice", "duplicated name: " + name);
		}
		type = type.getRealType();
		if(type.hasVarType() && nameNode != null) {
			this.report(nameNode, "notice", "ambigious variable: " + name + " :" + type.getName());
		}
		if(GlobalName) {
			defined = new DefinedGlobalNameFunctor(name, type.getRealType(), initValue);
			if(Main.EnableVerbose) {
				Main._PrintLine("global name: " + name + " :" + type.getName() + " initValue=" + initValue);
			}
		}
		else {
			defined = new DefinedNameFunctor(name, type.getRealType(), initValue);
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

	public Functor setGlobalName(PegObject nameNode, MetaType type, PegObject initValue) {
		return this.setName(true, nameNode.getText(), nameNode, type, initValue);
	}

	public Functor setFunctionName(String name, MetaType type, PegObject node) {
		Functor defined = this.getSymbol(name);
		defined = new DefinedFunctionFunctor(name, type, node, defined);
		if(Main.EnableVerbose) {
			Main._PrintLine(name + " :" + type.getName() + " initValue=" + node);
		}
		this.setSymbol(name, defined);
		return defined;
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
		if(f != null) {
			return f.getReturnType(defaultType);
		}
//		if(f instanceof DefinedTypeFunctor) {
//			return f.getReturnType(defaultType);
//		}
		if(Main.EnableVerbose) {
			Main._PrintLine("FIXME: undefined type: " + name);
		}
		return defaultType;
	}

	public MetaType getVoidType() {
		return this.getType("void", null);  //FIXME
	}

	public MetaType getAnyType() {
		return this.getType("any", null);   //FIXME
	}
	
	
	public void load(String fileName, BunDriver driver) {
		BunSource source = Main.loadSource(fileName);
		this.root.newParserContext(null, source);
		PegParserContext context =  this.root.newParserContext(null, source);
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
			super(name, false, null);
		}

		@Override
		protected void matchSubNode(PegObject node, boolean hasNextChoice) {
			if(node.isEmptyToken()) {
				node.typed = MetaType.newVarType(node, null);
			}
			else {
				String typeName = node.getText();
				SymbolTable gamma = node.getSymbolTable();
				node.typed = gamma.getType(typeName, null);
			}
			node.matched = this;
		}

		@Override
		public void build(PegObject node, BunDriver driver) {
			driver.pushType(node.getType(MetaType.UntypedType));
		}
	}

	class NameFunctor extends Functor {
		public NameFunctor(String name) {
			super(name, false, null);
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
		public void build(PegObject node, BunDriver driver) {
			driver.pushUndefinedName(node, node.getText());
		}
	}

	public void loadBunModel(String fileName, BunDriver driver) {
		this.addFunctor(new TypeFunctor("#type"));
		this.addFunctor(new NameFunctor("#name"));
		this.addFunctor(new BunFunctor("#bun"));  // #bun
		this.addFunctor(new ErrorFunctor());
		this.load(fileName, driver);
	}

}

