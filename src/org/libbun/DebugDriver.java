package org.libbun;

public class DebugDriver extends BunDriver {

	@Override
	public void initTable(Namespace gamma) {
		if(!Main.PegDebuggerMode) {
			gamma.setType("void",    new VoidType("void"));
			gamma.setType("boolean", new BooleanType("bool"));
			gamma.setType("int",     new IntType("int_t"));
			gamma.setType("float",   new FloatType("float"));
			gamma.setType("String",  new IntType("str"));
			gamma.setType("any",     new AnyType("var"));
			gamma.setType("alpha",   new GreekType(0));
			
			gamma.addFunctor(new TypeFunctor("#type"));
			gamma.addFunctor(new NameFunctor("#name"));
			gamma.addFunctor(new LiteralFunctor("#integer", gamma, "int"));
			gamma.addFunctor(new LiteralFunctor("#string", gamma, "String"));
	
			gamma.addFunctor(new FunctionFunctor("#function"));
	
			gamma.addFunctor(new BunFunctor("#bun"));  // #bun
			gamma.addFunctor(new ErrorFunctor());
			gamma.load("lib/python/konoha.bun", this);
		}
	}

	@Override
	public void pushName(String name, int nameIndex) {
		System.out.print(name+"_"+nameIndex);
	}

	@Override
	public void pushLiteral(PegObject node, String number, MetaType type) {
		System.out.print(number);
	}

	@Override
	public void pushNewLine() {
		System.out.println();
	}

	@Override
	public void pushCode(String text) {
		System.out.print(text);
	}

	@Override
	public void pushNode(PegObject node) {
		node.build(this);
	}

	@Override
	public void pushTypeOf(PegObject node) {
		if(node.typed != null) {
			System.out.println(node.typed.getName());
		}
		else {
			System.out.println("*untyped "+ node.name + "*");
		}
	}

	@Override
	public void pushCommand(String name, PegObject node) {
		System.out.print("${" + name + " ");
		this.pushNode(node);
		System.out.print("}");
	}

	@Override
	public void pushErrorMessage(SourceToken source, String msg) {
		System.out.println(source.source.formatErrorMessage("error", source.startIndex, msg));
	}


}
