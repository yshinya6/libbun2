package org.libbun;

public abstract class Functor {
	public String    name;
	public FuncType funcType;
	public Functor   nextChoice = null;

	public Functor(String name, FuncType funcType) {
		this.name = name;
		this.funcType = funcType;
	}

	public String key() {
		if(this.funcType == null) {
			return this.name + "*";
		}
		else {
			return this.name + ":" + this.funcType.getFuncParamSize();
		}
	}

	@Override
	public String toString() {
		return this.key();
	}

	protected abstract void matchSubNode(PegObject node, boolean hasNextChoice, PegDriver driver);

	public abstract void build(PegObject node, PegDriver driver);

	public MetaType getReturnType(MetaType defaultType) {
		if(this.funcType != null) {
			return this.funcType.getReturnType();
		}
		return defaultType;
	}

}

class ErrorFunctor extends Functor {
	public ErrorFunctor() {
		super(BunSymbol.PerrorFunctor, null);
	}

	@Override
	protected void matchSubNode(PegObject node, boolean hasNextChoice, PegDriver driver) {
		node.matched = this;
	}

	@Override
	public void build(PegObject node, PegDriver driver) {
		PegObject msgNode = node.get(0, null);
		if(msgNode != null) {
			String errorMessage = node.getTextAt(0, "*error*");
			driver.report(node, "error", errorMessage);
		}
		else {
			driver.report(node, "error", "syntax error");
		}
	}
}

class BunFunctor extends Functor {
	public BunFunctor(String name) {
		super(name, null);
	}

	@Override 
	protected void matchSubNode(PegObject node, boolean hasNextChoice, PegDriver driver) {
		SymbolTable gamma = node.getSymbolTable();
		TemplateFunctor f = this.newFunctor(gamma, node, driver);
		if(f != null) {
			gamma.addFunctor(f);
		}
		node.matched = this;
	}

	@Override
	public void build(PegObject node, PegDriver driver) {
		// TODO Auto-generated method stub
	}

	private TemplateFunctor newFunctor(SymbolTable gamma, PegObject defineNode, PegDriver driver) {
		PegObject sig = defineNode.get(0);
		String name = sig.getTextAt(0, null);
		UniMap<Integer> nameMap = new UniMap<Integer>();
		FuncType funcType = this.newFuncType(gamma, sig.get(1), sig.get(2,null), nameMap, driver);
		TemplateFunctor functor = new TemplateFunctor(name, funcType);
		for(int i = 1; i < defineNode.size(); i++) {
			Section section = this.newSection(defineNode.get(i), nameMap, driver);
			functor.add(section);
		}
		return functor;
	}

	private MetaType newType(SymbolTable gamma, PegObject typeNode, PegDriver driver) {
		if(typeNode != null) {
			gamma.check(typeNode, driver);
			return typeNode.getType(MetaType.UntypedType);
		}
		return MetaType.UntypedType;
	}

	private FuncType newFuncType(SymbolTable gamma, PegObject paramNode, PegObject returnTypeNode, UniMap<Integer> nameMap, PegDriver driver) {
		UniArray<MetaType> typeList = new UniArray<MetaType>(new MetaType[paramNode.size()+1]);
		for(int i = 0; i < paramNode.size(); i++) {
			PegObject p = paramNode.get(i);
			String name = p.getTextAt(0, null);
			typeList.add(this.newType(gamma, p.get(1, null), driver));
			nameMap.put(name, i);
		}
		typeList.add(this.newType(gamma, returnTypeNode, driver));
		return MetaType.newFuncType(typeList);
	}

	private Section newSection(PegObject sectionNode, UniMap<Integer> nameMap, PegDriver driver) {
		Section section = new Section();
		int line = 0;
		for(int i = 0; i < sectionNode.size(); i++) {
			PegObject subNode = sectionNode.get(i);
			//			System.out.println(subNode);
			if(subNode.is("#bun.label")) {
				System.out.println("TODO: section.label");
			}
			if(subNode.is("#bun.line")) {
				if(line > 0) {
					section.addNewLine();
				}
				section.addLineNode(subNode, nameMap, driver);
				line = line + 1;
			}
		}
		return section;
	}
}

class TemplateFunctor extends Functor {
	Section section;

	public TemplateFunctor(String name, FuncType funcType) {
		super(name, funcType);
		this.section = null;
	}

	@Override
	protected void matchSubNode(PegObject node, boolean hasNextChoice, PegDriver driver) {
		MetaType[] greekContext = getGreekContext();
		SymbolTable gamma = node.getSymbolTable();
		for(int i = 0; i < node.size(); i++) {
			MetaType type = this.getParamTypeAt(i);
			PegObject subNode = node.get(i);
			PegObject checkedNode = gamma.checkType(subNode, type, greekContext, hasNextChoice, driver);
			if(checkedNode == null) {
				//System.out.println("** untyped node: " + node);
				return;
			}
		}
		node.matched = this;
		node.typed = this.getReturnType(MetaType.UntypedType).getRealType(greekContext);
		if(node.typed == null) {  // unresolved greek type
			node.matched = null;
		}
		//System.out.println("**typed " + node);
	}
	
	private MetaType[] getGreekContext() {
		if(this.funcType != null && this.funcType.hasGreekType()) {
			return GreekType._NewGreekContext(null);
		}
		return null;
	}
	
	private MetaType getParamTypeAt(int index) {
		if(this.funcType != null && index < this.funcType.getFuncParamSize()) {
			return this.funcType.getFuncParamType(index);
		}
		return MetaType.UntypedType;
	}

	@Override
	public void build(PegObject node, PegDriver driver) {
		Section cur = this.section;
		while(cur != null) {
			cur.build(node, driver);
			cur = cur.nextChoice;
		}
	}

	public void add(Section section) {
		Section sec = this.section;
		if(sec == null) {
			this.section = section;
		}
		else {
			while(sec.nextChoice != null) {
				sec = sec.nextChoice;
			}
			sec.nextChoice = section;
		}
	}
}

class Section {
	String label;
	UniArray<String> requirements;
	ChunkCommand chunks = null;
	Section nextChoice = null;

	public Section() {

	}

	void add(ChunkCommand chunk) {
		if(this.chunks == null) {
			this.chunks = chunk;
		}
		else {
			ChunkCommand cur = this.chunks;
			while(cur.next != null) {
				cur = cur.next;
			}
			cur.next = chunk;
		}
	}

	public void build(PegObject node, PegDriver driver) {
		ChunkCommand cur = this.chunks;
		//System.out.println("debug command: " + cur);
		while(cur != null) {
			cur.push(node, driver);
			cur = cur.next;
			//System.out.println("debug command: " + cur);
		}
	}

	boolean addLineNode(PegObject lineNode, UniMap<Integer> nameMap, PegDriver driver) {
		for(int j = 0; j < lineNode.size(); j++) {
			PegObject chunkNode = lineNode.get(j);
			//System.out.println("debug: chunk: " + chunkNode);
			if(chunkNode.is("#bun.chunk")) {
				String s = chunkNode.getText();
				if(s.equals("$$")) {
					s = "$";
				}
				this.addChunk(s);
			}
			else if(chunkNode.is("#bun.cmd")) {
				if(chunkNode.size() == 1) {
					String name = chunkNode.getTextAt(0, null);
					Integer index = nameMap.get(name, null);
					if(index != null) {
						this.addNode(index);
					}
					else {
						driver.report(chunkNode, "warning", "undefined name: " + name);
					}
				}
				else {
					String cmd = chunkNode.getTextAt(0, null);
					String name = chunkNode.getTextAt(1, null);
					Integer index = nameMap.get(name, null);
					if(index != null) {
						this.addCommand(cmd, index);
					}
					else {
						driver.report(chunkNode, "warning", "undefined name: " + name);
					}
				}
			}
		}
		return true;
	}
	void addNewLine() {
		this.add(new NewLineChunk());
	}
	void addChunk(String text) {
		this.add(new Chunk(text));
	}
	void addNode(int index) {
		this.add(new NodeCommand(null, index));
	}
	void addCommand(String cmd, int index) {
		if(cmd.equals("typeof")) {
			this.add(new TypeOfNodeCommand(cmd, index));
			return;
		}
		this.add(new Command(cmd, index));
	}
}


abstract class ChunkCommand {
	ChunkCommand next = null;
	public abstract void push(PegObject node, PegDriver d);
}

class Chunk extends ChunkCommand {
	String text;
	Chunk(String text) {
		this.text = text;
	}
	@Override
	public void push(PegObject node, PegDriver d) {
		d.pushCode(this.text);
	}
}

class NewLineChunk extends ChunkCommand {
	@Override
	public void push(PegObject node, PegDriver d) {
		d.pushNewLine();
	}
}

class Command extends ChunkCommand {
	String name;
	int index;
	Command(String name, int index) {
		this.name = name;
		this.index = index;
	}
	@Override
	public void push(PegObject node, PegDriver d) {
		d.pushCommand(this.name, node.get(this.index));
	}


}

class NodeCommand extends Command {
	NodeCommand(String name, int index) {
		super(name, index);
	}
	@Override
	public void push(PegObject node, PegDriver d) {
		d.pushNode(node.get(this.index));
	}
}

class TypeOfNodeCommand extends Command {
	TypeOfNodeCommand(String name, int index) {
		super(name, index);
	}
	@Override
	public void push(PegObject node, PegDriver d) {
		d.pushType(node.get(this.index).getType(MetaType.UntypedType));
	}
}

