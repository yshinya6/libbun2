package org.libbun;

public class Functor {
	public SymbolTable storedTable = null;
	public String      name;
	public boolean     isSymbol;
	public BunType     funcType;
	TemplateEngine     template;
	public Functor     nextChoice = null;

	public Functor(String name, boolean isSymbol, BunType funcType) {
		this.name = name;
		this.isSymbol = isSymbol;
		this.funcType = funcType;
	}
	public String key() {
		if(this.funcType == null) {
			return this.name + "*";
		}
		else {
			if(this.name.equals("#coercion") || this.name.equals("#conv")) {
				return this.keyTypeRel(this.name);
			}
			if(this.isSymbol) {
				return this.name;
			}
			return this.name + ":" + this.funcType.getFuncParamSize();
		}
	}
	private String keyTypeRel(String head) {
		BunType fromType = this.funcType.getFuncParamType(0);
		BunType toType = this.funcType.getReturnType();
		return BunType.keyTypeRel(head, fromType, toType);
	}
	@Override 
	public String toString() {
		return this.key();
	}
	
	public BunType getReturnType(BunType defaultType) {
		if(this.funcType != null) {
			return this.funcType.getReturnType();
		}
		return defaultType;
	}
	protected void matchSubNode(PegObject node, boolean hasNextChoice) {
		SymbolTable gamma = node.getSymbolTable();
		BunType varFuncType = this.getVarFuncType();
		for(int i = 0; i < node.size(); i++) {
			BunType type = this.paramTypeAt(varFuncType, i);
			if(!gamma.checkTypeAt(node, i, type, hasNextChoice)) {
				//System.out.println("mismatched varFuncType: " + varFuncType + " at i = " + i);
				node.matched = null;
				return;
			}
		}
		node.typed = this.returnType(varFuncType);
		node.matched = this;
		//System.out.println("matched varFuncType: " + varFuncType + " returnType = " + node.typed);
		if(this.template != null) {
			this.template.check(node, gamma.root.driver);
		}
	}
	private BunType getVarFuncType() {
		if(this.funcType != null) {
			return this.funcType.newVarGreekType(null, null);
		}
		return null;
	}
	private BunType paramTypeAt(BunType varFuncType, int index) {
		if(varFuncType != null && index < varFuncType.getFuncParamSize()) {
			return varFuncType.getFuncParamType(index).getRealType();
		}
		return BunType.UntypedType;
	}
	private BunType returnType(BunType varFuncType) {
		if(varFuncType != null) {
			return varFuncType.getReturnType().getRealType();
		}
		return BunType.UntypedType;
	}
	public void build(PegObject node, BunDriver driver) {
		if(this.template != null) {
			this.template.build(node, driver);
		}
	}
	public void add(TemplateEngine section) {
		this.template = section;
	}
	
}


class ErrorFunctor extends Functor {
	public ErrorFunctor() {
		super(BunSymbol.PerrorFunctor, false, null);
	}
	@Override
	protected void matchSubNode(PegObject node, boolean hasNextChoice) {
		node.matched = this;
	}
	@Override
	public void build(PegObject node, BunDriver driver) {
//		PegObject msgNode = node.get(0, null);
//		if(msgNode != null) {
//			String errorMessage = node.getTextAt(0, "*error*");
//			driver.report(node, "error", errorMessage);
//		}
//		else {
		driver.report(node, "error", "syntax error");
//		}
	}
}

class BunTypeDefFunctor extends Functor {
	public BunTypeDefFunctor(String name) {
		super(name, false, null);
	}
	@Override 
	protected void matchSubNode(PegObject node, boolean hasNextChoice) {
		SymbolTable gamma = node.getSymbolTable();
		String name = node.getText();
		gamma.setType(BunType.newValueType(name, null));
		node.matched = this;
	}
	@Override
	public void build(PegObject node, BunDriver driver) {
	}
}

class BunTemplateFunctor extends Functor {
	public BunTemplateFunctor(String name) {
		super(name, false, null);
	}

	@Override 
	protected void matchSubNode(PegObject node, boolean hasNextChoice) {
		SymbolTable gamma = node.getSymbolTable();
		Functor f = this.parseFunctor(gamma, node);
		if(f != null) {
			gamma.addFunctor(f);
		}
		node.matched = this;
	}

	@Override
	public void build(PegObject node, BunDriver driver) {
		// TODO Auto-generated method stub
	}

	private Functor parseFunctor(SymbolTable gamma, PegObject bunNode) {
		boolean isSymbol = false;
		String name = bunNode.textAt(0, null);
		GreekList greekList = this.parseGreekList(gamma, bunNode.get(5, null));
		UniMap<Integer> nameMap = new UniMap<Integer>();
		if(bunNode.get(1).isEmptyToken()) {
			isSymbol = true;
		}
		BunType funcType = this.parseFuncType(gamma, greekList, bunNode.get(1), bunNode.get(2), nameMap);
		Functor functor = new Functor(name, isSymbol, funcType);
		TemplateEngine section = this.parseSection(bunNode.get(3), nameMap);
		functor.add(section);
		return functor;
	}

	private GreekList parseGreekList(SymbolTable gamma, PegObject paramNode) {
		if(paramNode != null) {
			GreekList listed = null;
			for(int i = 0; i < paramNode.size(); i++) {
				PegObject p = paramNode.get(i);
				String name = p.textAt(0, null);
				listed = this.appendGreekList(listed, name, this.parseType(gamma, null, p.get(1, null)));
			}
			return listed;
		}
		return null;
	}
		
	private GreekList appendGreekList(GreekList listed, String name, BunType parsedType) {
		GreekList entry = new GreekList(name, parsedType);
		if(listed == null) {
			return entry;
		}
		listed.append(entry);
		return listed;
	}

	private BunType parseFuncType(SymbolTable gamma, GreekList greekList, PegObject paramNode, PegObject returnTypeNode, UniMap<Integer> nameMap) {
		if(paramNode.size() == 0 && paramNode.getText().equals("(*)")) {
			return null; // 
		}
		UniArray<BunType> typeList = new UniArray<BunType>(new BunType[paramNode.size()+1]);
		for(int i = 0; i < paramNode.size(); i++) {
			PegObject p = paramNode.get(i);
			String name = p.textAt(0, null);
			typeList.add(this.parseType(gamma, greekList, p.get(1, null)));
			nameMap.put(name, i);
		}
		typeList.add(this.parseType(gamma, greekList, returnTypeNode));
		BunType t = BunType.newFuncType(typeList);
		if(greekList != null) {
			t = new GreekContainerType(greekList, t);
		}
		return t;
	}

	private BunType parseType(SymbolTable gamma, GreekList greekList, PegObject typeNode) {
		if(typeNode != null) {
			if(greekList != null) {
				String name = typeNode.getText();
				BunType t = greekList.getGreekType(name);
				if(t != null) {
					return t;
				}
			}
			gamma.tryMatch(typeNode);
			BunType t = typeNode.getType(BunType.UntypedType);
			if(t instanceof VarType) {
				t = BunType.UntypedType;
				typeNode.typed = BunType.UntypedType;
			}
			return t;
		}
		return BunType.UntypedType;
	}

	private TemplateEngine parseSection(PegObject sectionNode, UniMap<Integer> nameMap) {
		TemplateEngine section = new TemplateEngine();
		int line = 0;
		for(int i = 0; i < sectionNode.size(); i++) {
			PegObject subNode = sectionNode.get(i);
			//			System.out.println(subNode);
			if(subNode.is("#bun.header")) {
				this.parseLine(section, true, subNode, nameMap);
			}
			else if(subNode.is("#bun.line")) {
				if(line > 0) {
					section.addNewLine();
				}
				this.parseLine(section, false, subNode, nameMap);
				line = line + 1;
			}
		}
		return section;
	}

	private final static Integer NoneOfName = -2;
	private int indexOfName(String name, UniMap<Integer> nameMap) {
		if(name.equals("this")) {
			return -1;
		}
		return nameMap.get(name, NoneOfName);
	}
	
	private void parseLine(TemplateEngine sec, boolean headerOption, PegObject lineNode, UniMap<Integer> nameMap) {
		for(int j = 0; j < lineNode.size(); j++) {
			PegObject chunkNode = lineNode.get(j);
			if(chunkNode.is("#bun.chunk")) {
				if(!headerOption) {
					String s = chunkNode.getText();
					sec.addCodeChunk(s);
				}
			}
			else if(chunkNode.is("#bun.cmd1")) {
				String name = chunkNode.textAt(0, null);
				int index = this.indexOfName(name, nameMap);
				if(index != -2) {
					if(!headerOption) {
						sec.addNodeChunk(index);
					}
				}
				else {
					SymbolTable gamma = lineNode.getSymbolTable();
					if(!this.checkCommand(gamma, name)) {
						gamma.report(chunkNode, "warning", "undefined command: " + name);
						return;
					}
					else {
						sec.addCommand(headerOption, name, -1, chunkNode, 1);
					}
				}
			}
			else if(chunkNode.is("#bun.cmd2")){
				SymbolTable gamma = lineNode.getSymbolTable();
				String cmd = chunkNode.textAt(0, null);
				if(!this.checkCommand(gamma, cmd)) {
					gamma.report(chunkNode, "warning", "undefined command: " + cmd);
					return;
				}
				String name = chunkNode.textAt(1, null);
				int index = this.indexOfName(name, nameMap);
				if(index != -2) {
					sec.addCommand(headerOption, cmd, index, chunkNode, 2);
				}
				else {
					sec.addCommand(headerOption, cmd, -1, chunkNode, 1);
				}
			}
		}
	}

	private boolean checkCommand(SymbolTable gamma, String name) {
		return gamma.root.driver.hasCommand(name);
	}

}

class TemplateEngine {
	TempalteChunk chunks = null;

	public TemplateEngine() {

	}

	void add(TempalteChunk chunk) {
		if(this.chunks == null) {
			this.chunks = chunk;
		}
		else {
			TempalteChunk cur = this.chunks;
			while(cur.next != null) {
				cur = cur.next;
			}
			cur.next = chunk;
		}
	}
	
	void addNewLine() {
		this.add(new NewLineChunk());
	}
	void addCodeChunk(String text) {
		if(text.length() > 0) {
			this.add(new CodeChunk(text));
		}
	}
	void addNodeChunk(int index) {
		this.add(new NodeChunk(null, index));
	}
	void addCommand(boolean headerOption, String cmd, int index, PegObject aNode, int aStart) {		
		String[] a = null;
		if(aStart < aNode.size()) {
			a = new String[aNode.size()-aStart];
			for(int i = 0; i < a.length; i++) {
				a[i] = aNode.textAt(aStart +i, "");
			}
		}
		this.add(new NodeCommandChunk(headerOption, cmd, index, a));
	}
	
	void addCommand(boolean headerOption, String cmd, String name) {
		this.add(new CommandChunk(headerOption, cmd, name));
	}

	public void check(PegObject node, BunDriver driver) {
		TempalteChunk cur = this.chunks;
		while(cur != null) {
			if(cur.headerOption) {
				cur.push(node, driver);
			}
			cur = cur.next;
		}
	}

	public void build(PegObject node, BunDriver driver) {
		TempalteChunk cur = this.chunks;
		while(cur != null) {
			if(!cur.headerOption) {
				cur.push(node, driver);
			}
			cur = cur.next;
		}
	}
	
	abstract class TempalteChunk {
		boolean headerOption = false;
		TempalteChunk next = null;
		public abstract void push(PegObject node, BunDriver d);
	}

	class CodeChunk extends TempalteChunk {
		String text;
		CodeChunk(String text) {
			this.text = text;
		}
		@Override
		public void push(PegObject node, BunDriver d) {
			d.pushCode(this.text);
		}
	}

	class NewLineChunk extends TempalteChunk {
		@Override
		public void push(PegObject node, BunDriver d) {
			d.pushNewLine();
		}
	}

	class NodeChunk extends TempalteChunk {
		int index;
		NodeChunk(String name, int index) {
			this.index = index;
		}
		@Override
		public void push(PegObject node, BunDriver d) {
			d.pushNode(node.get(this.index));
		}
	}
	private static final String[] Null = new String[0];

	class NodeCommandChunk extends TempalteChunk {
		String name;
		int index;
		String[] arguments;
		NodeCommandChunk(boolean headerOption, String name, int index, String[] a) {
			this.name = name;
			this.index = index;
			this.headerOption = headerOption;
			this.arguments = a;
			if(a == null) {
				this.arguments = Null;
			}
		}
		@Override
		public void push(PegObject node, BunDriver d) {
			if(this.index != -1) {
				node = node.get(this.index);
			}
			d.pushCommand(this.name, node, this.arguments);
		}
	}

	class CommandChunk extends TempalteChunk {
		String name;
		String param1;
		CommandChunk(boolean headerOption, String name, String param1) {
			this.name = name;
			this.param1 = param1;
			this.headerOption = headerOption;
		}
		@Override
		public void push(PegObject node, BunDriver d) {
			String[] a = new String[1];
			a[0] = this.param1;
			d.pushCommand(this.name, node, a);
		}
	}

}


